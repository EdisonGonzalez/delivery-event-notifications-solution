# Refactorización del Proyecto de Notificaciones - Cambios Implementados

## Resumen de Cambios

Se han implementado con éxito todas las mejoras arquitectónicas solicitadas en el proyecto de notificaciones de entrega.

---

## 1. ✅ Objeto de Paginación (en lugar de Lista)

Se implementó una clase genérica `Page<T>` para retornar datos paginados:

```java
public record Page<T>(
    List<T> content,
    int pageNumber,
    int pageSize,
    long totalElements,
    int totalPages,
    boolean hasNextPage,
    boolean hasPreviousPage)
```

**Archivo:** `src/main/java/com/delivery/event/notification/infrastructure/web/dto/Page.java`

### Cambios en la API:

**Antes:**
```json
GET /notification_events
Response: List<NotificationEvent>
```

**Después:**
```json
GET /notification_events
Response: PageDto<NotificationEventResponseDto> {
  "content": [...],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNextPage": true,
  "hasPreviousPage": false
}
```

---

## 2. ✅ Separación de DTOs por Capas

### Capa Web (Web Layer)

- **`NotificationEventResponseDto`** - DTO de respuesta para consultas
- **`CreateNotificationEventRequestDto`** - DTO de solicitud para crear eventos
- **`PageDto<T>`** - DTO genérico para respuestas paginadas

**Archivos:**
- `src/main/java/com/delivery/event/notification/infrastructure/web/dto/NotificationEventResponseDto.java`
- `src/main/java/com/delivery/event/notification/infrastructure/web/dto/CreateNotificationEventRequestDto.java`
- `src/main/java/com/delivery/event/notification/infrastructure/web/dto/PageDto.java`

### Capa de Dominio (Domain Layer)

- **`NotificationEvent`** - Objeto de dominio con lógica de negocio
- **`Audit`** - Clase base para auditoría

### Capa de Persistencia (Repository Layer)

- **`NotificationEventEntity`** - Entidad JPA mapeada a la base de datos
- **`AuditEntity`** - Clase base JPA para auditoría

---

## 3. ✅ Uso de Records y Lombok

### Records utilizados (sin boilerplate):

```java
// Web DTO Response
public record NotificationEventResponseDto(
    UUID id,
    String eventId,
    String eventType,
    // ... otros campos
)

// Web DTO Request
public record CreateNotificationEventRequestDto(
    @NotBlank String eventId,
    @NotBlank String eventType,
    @NotBlank String content,
    @NotBlank String webhookUrl
)

// Pagination DTO
public record PageDto<T>(
    List<T> content,
    int pageNumber,
    // ... otros campos
)
```

### Entidades con Lombok y SuperBuilder:

```java
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "notification_events")
public class NotificationEventEntity extends AuditEntity {
    // Heredará los campos de AuditEntity
}
```

**Beneficios:**
- Eliminación de boilerplate (getters/setters)
- Construcción fluida con SuperBuilder para herencia
- Código más limpio y mantenible

---

## 4. ✅ Lombok SuperBuilder para Clases Heredadas

Se actualizaron todas las clases base y derivadas:

### Clase Base Audit (Dominio):
```java
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Audit {
    private UUID id;
    private String createdBy;
    private Instant createdAt;
    // ...
}
```

### Clase Base AuditEntity (JPA):
```java
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@MappedSuperclass
public abstract class AuditEntity {
    // ...
}
```

### Clases Derivadas:
```java
@SuperBuilder
public class NotificationEvent extends Audit {
    // Hereda todos los campos y usa SuperBuilder
}

@SuperBuilder
public class NotificationEventEntity extends AuditEntity {
    // Hereda todos los campos y usa SuperBuilder
}
```

**Uso:**
```java
NotificationEvent event = NotificationEvent.builder()
    .id(UUID.randomUUID())
    .eventId("evt-123")
    .createdBy("system")
    .deliveryStatus(DeliveryStatus.PENDING)
    .build();
```

---

## 5. ✅ MapStruct para Conversión entre Capas

Se implementó un mapper con MapStruct para convertir entre capas:

**Archivo:** `src/main/java/com/delivery/event/notification/infrastructure/web/mapper/NotificationEventMapper.java`

```java
@Mapper(componentModel = "spring")
public interface NotificationEventMapper {
    
    NotificationEventResponseDto toResponseDto(NotificationEvent event);
    
    NotificationEvent toDomain(NotificationEventResponseDto dto);
}
```

### Flujo de Conversión:

```
NotificationEventEntity (BD)
        ↓
   [Manual Mapper]
        ↓
NotificationEvent (Dominio)
        ↓
   [MapStruct]
        ↓
NotificationEventResponseDto (Web)
```

---

## 6. ✅ Endpoint POST para Simular Eventos (Desarrollo)

Se añadió un nuevo endpoint POST para simular la llegada de eventos (solo desarrollo):

```http
POST /notification_events
Content-Type: application/json

{
  "eventId": "ORDER-001",
  "eventType": "ORDER_CREATED",
  "content": "{\"orderId\":\"ORD-123\"}",
  "webhookUrl": "https://client.example.com/webhook"
}
```

**Respuesta:**
```http
HTTP/1.1 201 Created
Content-Type: application/json

{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "eventId": "ORDER-001",
  "eventType": "ORDER_CREATED",
  "clientId": "client-a",
  "content": "{\"orderId\":\"ORD-123\"}",
  "webhookUrl": "https://client.example.com/webhook",
  "deliveryStatus": "PENDING",
  "retryCount": 0,
  "reason": null,
  "nextAttemptAt": "2026-06-07T11:35:02.702-05:00",
  "createdBy": "client-a",
  "createdAt": "2026-06-07T11:35:02.702-05:00",
  "updatedBy": "client-a",
  "updatedAt": "2026-06-07T11:35:02.702-05:00"
}
```

### Componentes:
- **Puerto:** `CreateNotificationEventUseCase` - Define el contrato
- **Servicio:** `CreateNotificationEventService` - Implementa la lógica
- **Controlador:** Endpoint `POST /notification_events` en `NotificationEventController`

---

## Cambios en Dependencias

Se agregó MapStruct al `pom.xml`:

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.6.3</version>
</dependency>
```

Y se configuró el plugin del compilador para procesar anotaciones de MapStruct:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <annotationProcessorPaths>
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>1.6.3</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

---

## API Endpoints Actualizados

### GET /notification_events
**Retorna paginación con DTOs de respuesta**

```bash
# Sin parámetros (default: page=0, size=20)
curl -H "Authorization: Bearer {token}" \
  http://localhost:8080/notification_events

# Con paginación
curl -H "Authorization: Bearer {token}" \
  "http://localhost:8080/notification_events?page=1&size=10"

# Con filtros
curl -H "Authorization: Bearer {token}" \
  "http://localhost:8080/notification_events?delivery_status=FAILED&page=0&size=20"
```

### GET /notification_events/{id}
**Retorna DTO de respuesta individual**

```bash
curl -H "Authorization: Bearer {token}" \
  http://localhost:8080/notification_events/{eventId}
```

### POST /notification_events
**Simula la creación de un evento (desarrollo)**

```bash
curl -X POST \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{
    "eventId": "ORDER-001",
    "eventType": "ORDER_CREATED",
    "content": "{\"orderId\":\"ORD-123\"}",
    "webhookUrl": "https://client.example.com/webhook"
  }' \
  http://localhost:8080/notification_events
```

### POST /notification_events/{id}/replay
**Replays a failed event**

```bash
curl -X POST \
  -H "Authorization: Bearer {token}" \
  http://localhost:8080/notification_events/{eventId}/replay
```

---

## Archivos Creados

1. `src/main/java/com/delivery/event/notification/infrastructure/web/dto/Page.java`
2. `src/main/java/com/delivery/event/notification/infrastructure/web/dto/PageDto.java`
3. `src/main/java/com/delivery/event/notification/infrastructure/web/dto/NotificationEventResponseDto.java`
4. `src/main/java/com/delivery/event/notification/infrastructure/web/dto/CreateNotificationEventRequestDto.java`
5. `src/main/java/com/delivery/event/notification/infrastructure/web/mapper/NotificationEventMapper.java`
6. `src/main/java/com/delivery/event/notification/application/port/in/CreateNotificationEventUseCase.java`
7. `src/main/java/com/delivery/event/notification/application/service/CreateNotificationEventService.java`

---

## Archivos Modificados

1. `pom.xml` - Agregadas dependencias de MapStruct
2. `src/main/java/com/delivery/event/notification/domain/model/Audit.java` - Agregado SuperBuilder
3. `src/main/java/com/delivery/event/notification/domain/model/NotificationEvent.java` - Agregado SuperBuilder
4. `src/main/java/com/delivery/event/notification/infrastructure/persistence/jpa/AuditEntity.java` - Agregado SuperBuilder
5. `src/main/java/com/delivery/event/notification/infrastructure/persistence/jpa/NotificationEventEntity.java` - Agregado SuperBuilder
6. `src/main/java/com/delivery/event/notification/application/port/in/NotificationQueryUseCase.java` - Cambiar retorno a Page
7. `src/main/java/com/delivery/event/notification/application/port/out/NotificationRepositoryPort.java` - Cambiar retorno a Page
8. `src/main/java/com/delivery/event/notification/application/service/NotificationQueryService.java` - Implementar Page
9. `src/main/java/com/delivery/event/notification/infrastructure/persistence/adapter/NotificationRepositoryAdapter.java` - Implementar Page
10. `src/main/java/com/delivery/event/notification/infrastructure/web/NotificationEventController.java` - Agregar mappers y nuevo endpoint
11. `src/main/java/com/delivery/event/notification/application/service/ApplicationServiceConfiguration.java` - Registrar nuevo servicio
12. `src/test/java/com/delivery/event/notification/application/service/NotificationQueryServiceTest.java` - Actualizar tests
13. `src/test/java/com/delivery/event/notification/infrastructure/web/NotificationEventControllerIntegrationTest.java` - Actualizar tests

---

## Testing

Todos los tests unitarios e integración han sido actualizados y pasan correctamente:

```bash
mvn test -q
# Build SUCCESS
```

---

## Ventajas de la Refactorización

✅ **Separación clara de responsabilidades** - DTOs separados por capa
✅ **Reducción de boilerplate** - Records y Lombok reducen líneas de código
✅ **Paginación estándar** - Implementación genérica reutilizable
✅ **Mapeo automático** - MapStruct genera código de conversión
✅ **Herencia simplificada** - SuperBuilder facilita construcción de subclases
✅ **API más robusta** - Endpoint para simular eventos facilita testing
✅ **Mejor mantenimiento** - Estructura más clara y escalable
✅ **Cumplimiento de arquitectura hexagonal** - Separación clara entre capas

---

## Notas Importantes

1. El endpoint `POST /notification_events` está diseñado para desarrollo y testing
2. Los DTOs separados permiten evolución independiente de cada capa
3. MapStruct genera el código de mapeo en tiempo de compilación
4. La paginación incluye información útil: `hasNextPage`, `hasPreviousPage`, `totalPages`
5. SuperBuilder requiere `@NoArgsConstructor` y `@AllArgsConstructor` en clases base

---

## Próximos Pasos Recomendados

- Agregar validaciones adicionales en los DTOs de request
- Implementar querydsl para filtros más complejos
- Agregar caché en la capa de persistencia
- Implementar metrics/monitoring para los endpoints
- Documentación OpenAPI/Swagger para la API

