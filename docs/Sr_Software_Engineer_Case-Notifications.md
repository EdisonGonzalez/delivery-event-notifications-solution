# Senior Software Engineer Case - Notification Platform

## Context

The platform is:

- Cloud-native
- Event-driven
- Microservices-based
- Transactional

The platform manages client resources such as:

- Accounts
- Payments
- Transactions

Operations supported:

- Create
- Update
- Delete

---

# Problem Statement

Implement a notification system capable of delivering platform events to client-owned webhook endpoints.

Examples of events:

- Balance updated
- Resource created
- Resource updated
- Resource deleted

---

# Functional Requirements

## Event Notification Delivery

### FR-1 Subscription Validation

Before delivering an event:

- Verify that a subscription exists.
- Verify that the subscription is active.
- Verify that the event belongs to the same client that owns the subscription.

Constraint:

- A client must never receive events belonging to another client.

---

### FR-2 Webhook Delivery

The system must:

- Deliver notifications via HTTPS.
- Send notifications to the URL configured by the client.
- Support different webhook endpoints per client.

---

### FR-3 Retry Strategy

When delivery fails:

- Retry automatically.
- Use an efficient retry strategy.
- Avoid infinite retries.

Examples of acceptable strategies:

- Exponential backoff
- Fixed retry intervals
- Retry with jitter

---

### FR-4 Delivery Persistence

Store delivery information including:

- Notification ID
- Client ID
- Event ID
- Delivery status
- Number of attempts
- Error information
- Final result
- Delivery timestamps

---

### FR-5 Observability

The system must provide near real-time observability.

Required capabilities:

- Monitor delivery success rate.
- Monitor delivery failures.
- Monitor retry activity.
- Detect abnormal behavior.
- Support investigation of client complaints.

Possible implementations:

- Metrics
- Logs
- Traces
- Dashboards
- Alerts

---

# Self-Service Notification API

Clients must be able to manage notification deliveries through a REST API.

---

## API Requirement 1

### GET /notification_events

Return notification events for the authenticated client.

Supported filters:

- from
- to
- delivery_status

Pagination:

- page
- size

Requirements:

- A client can only access its own notifications.

---

## API Requirement 2

### GET /notification_events/{notification_event_id}

Return detailed information for a specific notification event.

Requirements:

- A client can only access its own notification.

---

## API Requirement 3

### POST /notification_events/{notification_event_id}/replay

Replay a notification that previously failed.

Requirements:

- Only failed notifications can be replayed.
- Generate a new delivery attempt.
- Preserve audit history.

---

# Candidate Tasks

## Task 1 - System Design

Design a scalable and resilient architecture supporting:

1. Notification delivery
2. Self-service API

The design should include:

- Components
- Communication flows
- Storage
- Retry mechanism
- Observability
- Security considerations

Suggested diagrams:

- C4 Context
- C4 Container
- Sequence diagrams

---

## Task 2 - Implementation

### Architectural Style

Use:

- Java
- Spring Boot
- Hexagonal Architecture

---

### Notification Delivery

Implement:

1. Retrieve notification events.
2. Resolve webhook subscription.
3. Deliver notification via HTTPS.
4. Persist delivery results.
5. Handle retries.

---

### Self-Service API

Implement:

#### GET /notification_events

Query notifications.

Supported query parameters:

- delivery_status
- from
- to
- page
- size

---

#### GET /notification_events/{notification_event_id}

Retrieve notification details.

---

#### POST /notification_events/{notification_event_id}/replay

Replay a failed notification.

---

### Data Source

Use:

`notification_events.json`

as the source of notification events.

---

## Task 3 - Security

Identify at least three OWASP Top 10 vulnerabilities that could affect the solution.

For each vulnerability:

1. Describe the risk.
2. Explain the attack scenario.
3. Propose mitigations.

Examples:

- Broken Access Control (BOLA/IDOR)
- SSRF
- Security Misconfiguration
- Injection
- Vulnerable Components
- Authentication Failures

---

# Non-Functional Requirements

## Scalability

The solution should:

- Support high event volume.
- Support horizontal scaling.
- Avoid single points of failure.

---

## Reliability

The solution should:

- Handle temporary webhook outages.
- Guarantee eventual delivery when possible.
- Prevent duplicate processing.

---

## Auditability

All delivery attempts must be traceable.

Required audit information:

- Attempt number
- Timestamp
- Request payload
- Response status
- Failure reason

---

## Multi-Tenancy

The solution must enforce strict tenant isolation.

Requirements:

- Client A cannot access Client B data.
- Client A cannot receive Client B notifications.

---

## Security

Webhook delivery must:

- Use HTTPS.
- Validate URLs.
- Prevent SSRF attacks.
- Prevent DNS rebinding attacks.
- Protect against replay attacks if signatures are used.

---

# Deliverables

## Design

Provide:

- Architecture diagrams
- Design rationale
- Trade-off analysis

---

## Source Code

Provide:

- GitHub repository
- Last commit before submission deadline

---

## AI Usage Documentation

Document:

- Tools used
- Prompts used
- Screenshots (optional)
- Areas where AI assisted implementation

---

# Discussion Preparation

Be prepared to justify:

- Architecture decisions
- Retry strategy
- Database design
- Security choices
- Observability approach
- Scaling strategy