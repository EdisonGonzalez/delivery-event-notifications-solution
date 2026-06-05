# delivery-event-notifications-solution

Este repositorio define una solución para notificaciones por webhook con autoservicio REST, aplicando Arquitectura Hexagonal, patrón Outbox, seguridad OWASP y observabilidad.

## Decisiones confirmadas para la solución

- Paquete raíz: `com.delivery.event.notification`
- `eventId`: generado por el productor; **no** es lo mismo que `correlationId`
- Todas las entidades de persistencia extienden de `Audit`
- Auditoría con `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `channel`, `correlationId` y `version`
- Reintentos: `Spring Retry`
- Entrega HTTP: `WebClient` + Reactor Netty
- Mitigación SSRF: validación de URL, IP privada/local, localhost y DNS rebinding
- Rate limiting: `Bucket4j` embebido en el microservicio
- Base de datos: PostgreSQL en Docker para local, desarrollo y una ejecución lo más idéntica posible a producción; no se usa H2
- Estrategia de concurrencia: `SKIP LOCKED`
- Estados de auditoría: conservar eventos con estado `IGNORED` y un campo `reason`

## Puntos críticos que deben resolverse antes de codificar

1. **Límites de capa**: el dominio no debe tener anotaciones Spring/JPA.
2. **Outbox y reintentos**: el scheduler debe ser idempotente y respetar `nextAttemptAt`.
3. **Autorización por cliente**: cada consulta debe filtrar por `clientId` de la seguridad.
4. **Seguridad del webhook**: validar destino y bloquear SSRF/DNS rebinding.
5. **Concurrencia multi-instancia**: evitar doble entrega con `SKIP LOCKED`.
6. **Observabilidad**: incluir `event_id` y `client_id` en MDC y métricas.
7. **Rate limiting**: proteger `POST /notification_events/{id}/replay` con Bucket4j.

## Arquitectura de alto nivel

```mermaid
flowchart TB
	subgraph Clients[Consumidores]
		RESTClient[Cliente REST autenticado]
		TargetWebhook[Destino Webhook del cliente]
	end

	subgraph Infra[Infrastructure Layer]
		Security[Spring Security + JWT]
		RateLimit[Bucket4j Rate Limiter]
		RESTAPI[REST Controllers]
		Scheduler[@Scheduled Outbox Poller]
		WebClientAdapter[Webhook Adapter\nWebClient + Reactor Netty]
		SSRF[SSRF + DNS Rebinding Guard]
		JPA[JPA Repositories]
		Flyway[Flyway Migrations]
		Metrics[Micrometer + Actuator]
		Logging[SLF4J + MDC]
	end

	subgraph App[Application Layer]
		QueryUC[NotificationQueryUseCase]
		ReplayUC[NotificationReplayUseCase]
		ProcessUC[ProcessNotificationUseCase]
		DeliverPort[WebhookDeliveryPort]
		RepositoryPort[NotificationRepositoryPort]
		SubscriptionPort[SubscriptionPort]
	end

	subgraph Domain[Domain Layer]
		NotificationEvent[NotificationEvent]
		Subscription[Subscription]
		Audit[Audit]
		Status[DeliveryStatus\nPENDING / COMPLETED / FAILED / IGNORED]
	end

	RESTClient --> Security --> RateLimit --> RESTAPI
	RESTAPI --> QueryUC
	RESTAPI --> ReplayUC
	Scheduler --> ProcessUC
	ProcessUC --> SubscriptionPort
	ProcessUC --> RepositoryPort
	ProcessUC --> DeliverPort
	QueryUC --> RepositoryPort
	ReplayUC --> RepositoryPort

	ProcessUC --> NotificationEvent
	ProcessUC --> Subscription
	NotificationEvent --> Audit
	Subscription --> Audit

	DeliverPort --> WebClientAdapter --> SSRF --> TargetWebhook
	RepositoryPort --> JPA
	JPA --> Flyway

	RESTAPI --> Metrics
	Scheduler --> Metrics
	WebClientAdapter --> Logging
	RESTAPI --> Logging
```

## Lifecycle de un evento de notificación

```mermaid
flowchart LR
	A[Evento producido con eventId] --> B[Persistencia Outbox]
	B --> C{¿Existe Subscription activa?}
	C -- No --> I[Marcar IGNORED + reason]
	C -- Sí --> D[Estado PENDING]
	D --> E[Scheduler toma lote]
	E --> F[Claim by Update / SKIP LOCKED]
	F --> G[Enviar webhook con WebClient]
	G --> H{¿Éxito?}
	H -- Sí --> J[Marcar COMPLETED]
	H -- No --> K[Incrementar retryCount]
	K --> L{¿Quedan reintentos?}
	L -- Sí --> M[Calcular backoff]
	M --> D
	L -- No --> N[Marcar FAILED]

	subgraph SelfService[Self-Service REST API]
		Q1[GET /notification_events]
		Q2[GET /notification_events/{id}]
		Q3[POST /notification_events/{id}/replay]
	end

	Q1 --> D
	Q2 --> D
	Q3 --> D
```

## Modelo de discusión para afinar el diseño

### 1. Concurrencia y locking

La estrategia preferida es PostgreSQL con `SELECT ... FOR UPDATE SKIP LOCKED` para el procesamiento concurrente.

Esto evita optimistic locking y permite múltiples instancias del procesador sin duplicar envíos.

En la implementación actual, el `claim` de lote se ejecuta dentro de una transacción en `ProcessNotificationService.processBatch()` para mantener el bloqueo coherente mientras se obtiene el lote pendiente.

### 2. Seguridad

- `BOLA / IDOR`: todas las queries deben incluir `clientId`
- `SSRF`: bloqueo de redes privadas, loopback, metadata endpoints y rebinding
- `Rate limiting`: aplicar Bucket4j sobre replay para suficiencia y resiliencia

### 3. Auditoría

Los eventos ignorados no se eliminan. Se conservan como `IGNORED` con `reason` para trazabilidad y soporte.

La auditoría JPA queda habilitada con `PersistenceConfig` y `AuditContextConfig`, usando `system` como auditor por defecto.

## Ejecucion local con Docker Compose

Se incluye `docker-compose.yml` para levantar un entorno local completo con:

- `postgres` con volumen persistente (`postgres_data`)
- `healthcheck` en base de datos para esperar estado healthy
- `app` (microservicio Spring Boot) construido desde `Dockerfile`

### Levantar entorno local

```bash
docker compose up --build
```

### Levantar entorno local con perfil dev

Se incluye `docker-compose.override.yml` que aplica automáticamente cuando ejecutas `docker compose`:

```bash
docker compose up --build
```

El override proporciona:
- Puerto PostgreSQL `5433` (en lugar de `5432`) para no conflictuar con instancias locales
- Puerto app `8081` (en lugar de `8080`)
- Perfil Spring `dev` activado con logs en `DEBUG` para `com.delivery.*`
- Logs en archivo con rotación automática

Para usar solo la configuración base sin el override:

```bash
docker compose -f docker-compose.yml up --build
```

### Detener entorno local

```bash
docker compose down
```

### Reiniciar desde cero (incluyendo datos)

```bash
docker compose down -v
docker compose up --build
```

## Migraciones Flyway y datos mock

La aplicacion ejecuta Flyway al arrancar:

- `V1__init.sql`: crea tablas e indices
- `V2__mock_data.sql`: inserta eventos de ejemplo en `notification_events`

Los registros de `V2` incluyen JSON en `content` y no dejan eventos en estado `PENDING`.

## Comportamiento esperado al arrancar

- PostgreSQL queda accesible en `localhost:5432` (o `5433` con override)
- La app queda accesible en `localhost:8080` (o `8081` con override)
- Flyway aplica `V1` y `V2` automaticamente
- El scheduler usa estrategia de concurrencia con `SKIP LOCKED`
- Los reintentos de entrega se ejecutan con `Spring Retry`

## CI/CD

### Flujo de desarrollo

1. **CI en cada PR** es ejecutado por `.github/workflows/ci-test.yml`:
   - Ejecuta `mvn clean test`
   - Los reportes de Surefire se guardan como artifacts por 30 días
   - Se ejecuta en triggers: `pull_request` y `push` a `main`/`develop`

2. **Publicacion en GHCR** se ejecuta por `.github/workflows/publish-ghcr.yml`:
   - Trigger: PR cerrado sobre `main` con merge exitoso
   - Build multi-stage con Docker Buildx
   - Tags: `latest` y `sha` en `ghcr.io/${{ github.repository }}`
   - Cache en GitHub Actions para acelerar builds

## Estado de avance (12 puntos del blueprint)

1. [x] Arquitectura Hexagonal (dominio, aplicacion, infraestructura)
2. [x] Outbox transaccional con scheduler y procesamiento por lote
3. [x] API self-service (`GET /notification_events`, `GET /notification_events/{id}`, `POST /notification_events/{id}/replay`)
4. [x] Reintentos con backoff y transiciones de estado (`PENDING`, `COMPLETED`, `FAILED`, `IGNORED`)
5. [x] Seguridad BOLA/IDOR por `clientId` autenticado
6. [x] Rate limiting en endpoint de replay
7. [x] Mitigaciones SSRF/DNS rebinding para webhooks salientes
8. [x] PostgreSQL + Flyway (`V1__init.sql`, `V2__mock_data.sql`)
9. [x] Docker local (`Dockerfile`, `docker-compose.yml`, `docker-compose.override.yml`)
10. [x] CI/CD en GitHub Actions (tests + publicacion GHCR)
11. [x] Pruebas: unitarias, integración REST y escenario end-to-end de entrega webhook asíncrona
12. [~] Documentacion/JavaDoc: README y `docs/agent-history.md` completos; pendiente ampliar JavaDoc en clases restantes

## Cobertura de pruebas (JaCoCo)

Métricas verificadas en la última ejecución:

- Instruction: `86.67%`
- Branch: `82.50%`
- Line: `87.66%`
- Method: `86.23%`

Comando usado:

```bash
mvn org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report
```

Reporte HTML disponible en `target/site/jacoco/index.html`.

Configuración y uso local

- El `pom.xml` incluye ahora el plugin `org.jacoco:jacoco-maven-plugin` configurado para:
  - preparar el agente antes de ejecutar los tests (fase initialize),
  - generar el informe durante la fase `verify`, y
  - ejecutar una comprobación (`check`) que hace fallar la build si la cobertura de líneas es inferior al 85%.

- Para ejecutar la verificación completa (tests + informe + chequeo de cobertura):

```bash
mvn clean verify
```

- Si quieres generar sólo el informe sin ejecutar la comprobación, puedes ejecutar explícitamente las metas del plugin:

```bash
mvn org.jacoco:jacoco-maven-plugin:prepare-agent test org.jacoco:jacoco-maven-plugin:report
```

- Para ejecutar sólo el test del adaptador HttpServer (local):

```bash
mvn -Dtest=com.delivery.event.notification.infrastructure.web.WebhookDeliveryAdapterHttpServerTest test
```

- El informe HTML se genera en `target/site/jacoco/index.html` y el XML en `target/site/jacoco/jacoco.xml`.

Notas de CI

- Cuando subas la rama y ejecutes CI (GitHub Actions), la pipeline debe ejecutar `mvn clean verify` o, como mínimo, ejecutar las metas del plugin para asegurar que se genera el informe y se aplica la regla de cobertura.
- Recomiendo que el workflow de CI archive `target/site/jacoco` como artifact para poder descargar y revisar el HTML desde la ejecución de la pipeline.

## Publicacion automatica en GHCR

Se agrego el workflow `.github/workflows/publish-ghcr.yml` para publicar imagen Docker en GHCR cuando un PR se mergea a `main`.

- Trigger: `pull_request` cerrado sobre `main` con `merged == true`
- Registry: `ghcr.io`
- Tags: `latest` y `sha`
