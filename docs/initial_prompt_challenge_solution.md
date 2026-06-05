# Prompt Blueprint for Claude: Webhook & Notification Self-Service System

You are an expert software engineer specialized in Java Spring Boot, Hexagonal Architecture, and secure distributed
systems design. Your task is to implement a resilient, scalable, and observable event notification platform based on the
following architectural requirements.

There are two reference files available for context: `notification_events.json` (containing mock event data) and
`Sr_Software_Engineer_Case_-_Notifications.pdf` (containing the core problem statement).

---

## 1. Architectural Strategy & Design Patterns

### Hexagonal Architecture Boundaries

The application must be strictly structured into clear layers to isolate business logic from infrastructure details:

* **Domain Layer (Core):** Pure Java. No Spring annotations, no JPA dependencies. Contains domain entities (
  `NotificationEvent`, `Subscription`) and core business invariants.
* **Application Layer (Ports & Use Cases):** Implements business features via Inbound Ports (
  `ProcessNotificationUseCase`, `NotificationQueryUseCase`, `NotificationReplayUseCase`). Defines Outbound Ports (
  interfaces) for data access and network communication.
* **Infrastructure Layer (Adapters):** Driving Adapters (REST Controllers, `@Scheduled` tasks) and Driven Adapters (
  Spring Data JPA repositories, HTTP clients).

### The Transactional Outbox Pattern

Instead of implementing a distributed transaction (Saga Pattern), we use the **Transactional Outbox Pattern** to ensure
reliable *at-least-once* delivery.

* Events are initially received and stored in a database table acting as an Outbox queue with a `PENDING` delivery
  status.


* An asynchronous, background scheduling mechanism (Spring `@Scheduled`) polls `PENDING` notifications in batches and
  invokes the outbound HTTP delivery port.

---

## 2. Component Implementation Details (Java Spring Boot)

### Task 1: Webhook Delivery & Subscription Verification

* **Subscription Gatekeeper:** Before delivering an event, verify that an active subscription exists for the specific
  `client_id` and `event_type`. If no active subscription matches, the event should be safely ignored.


* **Asynchronous Webhook Client:** Implement the delivery using Spring 3.x+ `RestClient` or `WebClient`. The network
  call must run on an independent thread pool using Spring's `@Async` capabilities.


* **Efficient Retry Strategy:** Implement an exponential backoff retry mechanism (e.g., maximum 3 attempts, initial
  delay of 2 seconds, multiplier of 2.0) using `@Retryable` or an explicit application-level counter. If all retries are
  exhausted, transition the event status to `FAILED`.

### Task 2: Self-Service REST API

Implement a Spring Boot `@RestController` mapping exactly to these endpoints:

1. `GET /notification_events`: Fetches all event notifications for an authenticated client, supporting dynamic query
   filters for `delivery_status` and creation date.


2. `GET /notification_events/{notification_event_id}`: Retrieves the explicit details of a single notification. Returns
   a standard HTTP `404 Not Found` if the resource does not exist.


3. `POST /notification_events/{notification_event_id}/replay`: Allows re-triggering a definitely `FAILED` delivery. To
   keep the API responsive, this endpoint must not invoke the webhook synchronously; it must update the event status
   back to `PENDING` and reset its retry counter, letting the background processor pick it up immediately. Respond with
   HTTP `202 Accepted`.

---

## 3. Near Real-Time Observability

The implementation must integrate cross-cutting observability mechanisms to catch behavior deviations instantly and
facilitate rapid support troubleshooting:

* **Metrics (Micrometer & Actuator):** Expose custom metrics to track queue health in near real-time. Specifically
  expose:


* A gauge tracking current `PENDING` queue size (to detect processing bottlenecks).


* Counters tracking cumulative delivery successes (`COMPLETED`) and failures (`FAILED`).


* **Structured Logging & Tracing (SLF4J MDC):** Every log statement covering an individual notification's lifecycle must
  include the unique `event_id` within the Mapped Diagnostic Context (MDC). This enables immediate log-aggregation
  searches when responding to client complaints.

---

## 4. OWASP Top 10 Security & Mitigations

The public API and outbound webhook infrastructure must incorporate strict security controls against known
vulnerabilities:

1. **Broken Object Level Authorization (BOLA / IDOR):** Prevent users from viewing other clients' notifications. Ensure
   that database queries for specific resource IDs always bind the authenticated `client_id` extracted securely from the
   security context (e.g., `WHERE id = :eventId AND client_id = :authenticatedClientId`).


2. **Broken User Authentication & Rate Limiting:** Enforce a secure Spring Security filter chain. Protect the
   resource-heavy `/replay` endpoint from Denial of Service (DoS) attacks by implementing a rate-limiting filter (e.g.,
   Token Bucket via Bucket4j or Gateway configuration).


3. **Server-Side Request Forgery (SSRF) via Webhooks:** Secure the outbound webhook delivery client. Before dispatching
   an HTTP request, parse the target URL and reject connections pointing to private or local loopback IP spaces (e.g.,
   RFC 1918 networks, `localhost`, cloud metadata endpoints).

---

## Your Deliverable Instructions

Please generate the production-grade code structure for this solution.

* Organize code strictly into directories matching Hexagonal Architecture layers (`domain`, `application`,
  `infrastructure`).
* Provide the core Java files, including the Domain entities, Inbound/Outbound Ports, Application Use Cases, Spring Boot
  REST Controllers, the Asynchronous Webhook Sender Adapter, and the Database Repositories.
* Include standard configurations for Spring Boot metrics and security context handling.
* Ensure that all code is well-documented with JavaDoc comments and follows best practices for clean code and SOLID
  principles.
* Do not include any external dependencies beyond what is necessary for Spring Boot, Spring Data JPA, and the chosen
  HTTP client library.
* Focus on the core logic and structure; you do not need to implement the actual database schema or external service
  integrations, but your code should be structured to allow for easy extension in those areas.
* Make sure to include error handling and logging as per the observability requirements, and ensure that all security
  controls are clearly implemented in the code.
* Provide a README.md file that explains how to set up and run the application, as well as how to test the implemented
  features.
* Ensure that the code is clean, maintainable, and adheres to industry best practices for Java Spring Boot development.
* Include unit tests for the core business logic in the Domain and Application layers, as well as integration tests for
  the REST API endpoints and the asynchronous webhook delivery mechanism. Use JUnit and Mockito for testing, and ensure
  that tests are comprehensive and cover edge cases.
* Create a Dockerfile for containerizing the application, and provide instructions in the README.md for building and
  running
  the Docker container. Ensure that the application can be easily deployed in a cloud environment or on-premises.
* Use PostgreSQL for development, testing, and production-oriented local execution. Avoid H2 so the runtime behavior,
  SQL dialect, locking semantics, and migrations remain consistent across environments. Ensure that the database schema
  is defined using JPA entities and that migrations can be managed using a tool like Flyway or Liquibase.
* Ensure that the application is designed for scalability, with considerations for load balancing, horizontal scaling,
  and
  fault tolerance. Use Spring Boot's features to support these requirements, such as configuring connection pools,
  implementing health checks, and designing stateless components where possible. Provide documentation on how to scale
  the application in a production environment.
* Include a comprehensive logging strategy that captures key events and errors in the application. Use SLF4J with
  Logback
  for logging, and ensure that logs are structured and include relevant context information (e.g., `event_id`,
  `client_id`)
  to facilitate troubleshooting and monitoring. Provide examples of log messages in the documentation, and explain how
  to
  configure log levels and log output formats for different environments (development, staging, production).
* Ensure that the application is designed with security best practices in mind, including secure coding practices,
  proper
  handling of sensitive data, and regular security audits. Provide documentation on the security measures implemented in
  the application, and explain how to perform security testing and vulnerability assessments to ensure the application
  remains secure over time.
* Create using mermaid a high-level architecture diagram that illustrates the components of the application and their
  interactions. Include
  this diagram in the README.md to provide a visual overview of the system design and architecture. Ensure that the
  diagram is clear, well-labeled, and accurately represents the structure and flow of the application. Besides, create a
  flow diagram that illustrates the lifecycle of a notification event from reception to delivery, including the retry
  mechanism and the self-service API interactions. Include this diagram in the documentation to provide a clear
  understanding of the event processing flow.
* Ensure that the codebase is well-organized, with a clear package structure that reflects the Hexagonal
  Architecture. Use meaningful class and method names, and include comments where necessary to explain complex logic or
  design decisions. Adhere to Java coding standards and best practices to ensure that the code is clean, readable, and
  maintainable. Provide a comprehensive README.md that includes setup instructions, usage examples, testing guidelines,
  and any other relevant information for developers who will be working with the codebase.
* Remember to include proper error handling and exception management throughout the application. Use custom exceptions
  where appropriate, and ensure that all exceptions are logged with sufficient context to facilitate debugging. Provide
  a global exception handler for the REST API to return meaningful error responses to clients while avoiding exposure of
  sensitive information. Include examples of error handling in the documentation, and explain how to extend the error
  handling strategy as the application evolves.
* Ensure that the application is designed for maintainability and extensibility. Use design patterns where
  appropriate to promote code reuse and separation of concerns. Provide clear documentation on how to extend the
  application with new features or integrations, and ensure that the codebase is structured in a way that allows for
  easy modifications without introducing bugs or breaking existing functionality. Encourage best practices for code
  reviews and collaborative development to maintain a high-quality codebase over time.
* Ensure that the application is thoroughly tested and that all tests pass successfully before finalizing the
  implementation. Use continuous integration (CI) tools to automate the testing process and ensure that any new code
  changes are properly validated before being merged into the main codebase. Provide documentation on how to run the
  tests and interpret the results, and encourage a culture of testing and quality assurance within the development team.
* Finally, create a file with agent history chat interaction with the prompt and the generated code snippets, including
  any iterations or refinements made during the development process. This file should provide a clear record of the
  decision-making process and the evolution of the codebase, and can be used for future reference or as a learning
  resource for other developers. Include this file in the documentation to provide transparency and insight into the
  development process.