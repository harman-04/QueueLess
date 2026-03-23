# QueueLess – Complete Project Documentation

## Table of Contents

1. [Project Overview & Architecture](#1-project-overview--architecture)
2. [Core Technologies & Why Chosen](#2-core-technologies--why-chosen)
3. [Backend Package Structure & Key Classes](#3-backend-package-structure--key-classes)
4. [Authentication & Authorization Flow](#4-authentication--authorization-flow)
5. [Queue Management](#5-queue-management)
6. [Notifications & Schedulers](#6-notifications--schedulers)
7. [Payments & Token Generation](#7-payments--token-generation)
8. [Search & Discovery](#8-search--discovery)
9. [Frontend Architecture & Key Components](#9-frontend-architecture--key-components)
10. [Testing Strategy](#10-testing-strategy)
11. [Deployment & DevOps](#11-deployment--devops)
12. [Future Roadmap & Possible Enhancements](#12-future-roadmap--possible-enhancements)
13. [Security Deep Dive](#13-security-deep-dive)
14. [Database Design & Indexes](#14-database-design--indexes)
15. [Caching Strategy](#15-caching-strategy)
16. [Conclusion](#16-conclusion)
17. [Technology Learning Guide](#17-technology-learning-guide)
18. [Interview Questions & Answers](#18-interview-questions--answers)
19. [Data Structures & Algorithms in QueueLess](#19-data-structures--algorithms-in-queueless)
20. [How to Present QueueLess in an Interview](#20-how-to-present-queueless-in-an-interview)

---

## 1. Project Overview & Architecture

### 1.1 Problem Statement
Waiting in queues is an inevitable part of daily life – at hospitals, banks, restaurants, shops, and more. Customers waste time standing in line, providers struggle to manage crowd flow, and administrators lack visibility into service efficiency. **QueueLess** addresses these issues by offering a digital queue management system that allows users to join queues remotely, track their position in real time, and receive notifications when it’s their turn. Providers can manage multiple queues, handle emergency requests, and view analytics. Administrators can onboard places and services, manage providers, and monitor overall performance.

### 1.2 High‑Level Features
- **Multi‑role System**: Supports three roles – USER, PROVIDER, ADMIN – each with tailored dashboards and permissions.
- **Place & Service Management**: Admins create places (e.g., hospitals, shops) and define services under them (e.g., cardiology, haircut). Providers are assigned to one or more places.
- **Queue Operations**: Users can join queues as regular, group, or emergency tokens. Providers serve tokens, complete them, cancel them, and approve/reject emergency tokens.
- **Real‑time Updates**: WebSocket (STOMP) broadcasts queue state changes to all connected clients (users, providers, admins) instantly.
- **Notifications**: Email and push notifications (FCM) remind users before their turn and alert them when queues become short.
- **Feedback & Ratings**: Users rate their experience (overall, staff, service, wait time) after token completion.
- **Analytics & Reports**: Charts for token volume, busiest hours, average wait time; PDF/Excel exports for queue data and admin reports.
- **Payment Integration**: Admins and providers purchase access tokens via Razorpay.
- **Search & Discovery**: Comprehensive search with filters (location, rating, wait time, features) using MongoDB geospatial queries.

### 1.3 System Architecture
QueueLess follows a **client‑server architecture** with a clear separation between frontend and backend.

- **Frontend** (React SPA) communicates with the backend via REST API and WebSocket.
- **Backend** (Spring Boot) exposes REST endpoints and WebSocket endpoints. It handles business logic, database interactions, and external integrations.
- **Database**: MongoDB (primary storage) and Redis (caching, OTP storage).
- **External Services**: Razorpay (payments), Firebase Cloud Messaging (push notifications), SMTP server (email).
- **Monitoring**: Prometheus scrapes metrics, Grafana visualizes them, Loki collects logs, Promtail ships logs.

```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'primaryTextColor': '#FFFFFF', 'lineColor': '#B0BEC5', 'edgeLabelBackground':'#111827'}}}%%
graph TD
    %% Global Diagram Title - No literal ** or other formatting
    mainTitle["APPLICATION ARCHITECTURE DIAGRAM (DARK MODE)"]:::titleNode
    style mainTitle fill:none,stroke:none

    %% --- Node Definitions - Clean Multiline Text, No Markdown ---
    Browser_D["User Browser\nApplication Interface"]:::browserNodeD
    Backend_D["Spring Boot Backend\nAPI & Business Logic"]:::backendNodeD
    MongoDB_D["MongoDB\nNoSQL Database"]:::dbNodeD
    Redis_D["Redis\nCaching & Messaging"]:::dbNodeD
    Razorpay_D["Razorpay"]:::serviceNodeD
    Firebase_D["Firebase FCM"]:::serviceNodeD
    SMTP_D["SMTP (Email)"]:::serviceNodeD

    %% --- Defining Tiered Styles with Specific Colors ---
    classDef titleNode font-size:22px,font-weight:bold,color:#E1F5FE,stroke:none;
    %% Neon Blue for Client
    classDef browserNodeD fill:#0D47A1,stroke:#40C4FF,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;
    %% Light Green for Backend
    classDef backendNodeD fill:#1B5E20,stroke:#A5D6A7,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;
    %% Bright Orange for Databases
    classDef dbNodeD fill:#FF6F00,stroke:#FFE082,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;
    %% Bright Red for Third-Party Services
    classDef serviceNodeD fill:#B71C1C,stroke:#EF9A9A,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;

    %% --- Logical Connections ---
    Browser_D -.->|REST / WebSocket| Backend_D
    Browser_D --> Razorpay_D
    Backend_D --> MongoDB_D
    Backend_D --> Razorpay_D
    Backend_D --> Firebase_D
    Backend_D --> SMTP_D
    MongoDB_D -.-> Redis_D
    Razorpay_D --> SMTP_D
```

**Data Flow Example (Join Queue):**
1. User clicks “Join Queue” on a queue page.
2. Frontend sends POST request to `/api/queues/{queueId}/add-token` with JWT in header.
3. Backend validates token, checks if user already has an active token (if not, creates a new token).
4. Backend saves the token to MongoDB, updates queue statistics.
5. Backend broadcasts queue update via WebSocket to all clients subscribed to that queue.
6. Frontend receives update and re‑renders the queue view.
7. (Optional) If user has enabled push notifications, backend schedules a notification before their turn.

### 1.4 Technology Stack & Rationale

| Component          | Technology          | Why Chosen                                                                 |
|--------------------|---------------------|----------------------------------------------------------------------------|
| Backend Framework  | Spring Boot 3.5     | Mature, feature‑rich, strong security, WebSocket support, huge community. |
| Database           | MongoDB             | Document model fits queue tokens; geospatial queries for location search; flexible schema. |
| Caching            | Redis               | Fast, in‑memory store for OTPs and frequently accessed data.               |
| Authentication     | JWT + Spring Security | Stateless, scalable; integrates easily with Spring Security.               |
| Real‑time          | WebSocket (STOMP)   | Lightweight, bi‑directional, perfect for live queue updates.               |
| Payments           | Razorpay            | Trusted in India, easy integration, supports test mode.                    |
| Push Notifications | Firebase Cloud Messaging | Cross‑platform, free, reliable for web push.                             |
| Email              | JavaMail + SMTP     | Standard, works with any SMTP provider.                                    |
| Monitoring         | Prometheus + Grafana + Loki | Industry‑standard stack for metrics and logs.                            |
| Frontend           | React + Vite        | Component‑based, fast development, HMR.                                    |
| State Management   | Redux Toolkit       | Predictable state, middleware support (e.g., for WebSocket).               |
| Styling            | React Bootstrap + CSS | Consistent components, responsive, easy to customize.                    |

### 1.5 Deployment & DevOps
- **Containerization**: Docker images for backend and frontend; Docker Compose orchestrates the whole stack (backend, frontend, MongoDB, Redis, Prometheus, Grafana, Loki, Promtail).
- **SSL**: Self‑signed certificates for development; production would use proper certificates (e.g., Let’s Encrypt).
- **CI/CD**: Not implemented yet but could be added using GitHub Actions to run tests and deploy on push.

### 1.6 Scalability Considerations
- **Horizontal scaling**: Backend is stateless (JWT) so multiple instances can run; MongoDB can be scaled with replica sets.
- **Caching**: Redis reduces database load for frequently accessed data (e.g., place lists).
- **Rate limiting**: Prevents abuse by limiting requests per user/IP.

---


## 2. Core Technologies & Why Chosen

### 2.1 Java & Spring Boot
**Why Java?**  
Java is a mature, platform‑independent language with strong ecosystem support. It provides static typing, which reduces runtime errors and makes refactoring safer. For a project like QueueLess that involves complex business logic, security, and concurrency, Java is a reliable choice.

**Why Spring Boot?**  
Spring Boot eliminates boilerplate configuration and provides production‑ready features out of the box. Key reasons:
- **Auto‑configuration** – reduces setup time.
- **Spring Data MongoDB** – integrates seamlessly with MongoDB.
- **Spring Security** – comprehensive authentication/authorization.
- **Spring WebSocket** – easy STOMP support.
- **Spring Scheduling** – for background jobs (e.g., notifications).
- **Actuator & Micrometer** – built‑in monitoring.

**Configuration Highlights**
- `application.properties` centralizes environment‑specific settings.
- `@Configuration` classes define beans (e.g., `FirebaseConfig`, `WebSocketConfig`).
- `@EnableScheduling` activates scheduled tasks.

**Interview Q&A**
- *Why use Spring Boot over plain Java?*  
  It accelerates development by providing pre‑built modules, reduces configuration, and integrates easily with modern tools.
- *How does auto‑configuration work?*  
  Spring Boot scans classpath for dependencies (e.g., `spring‑data‑mongodb`) and configures beans accordingly; can be overridden with custom `@Configuration`.
- *What are the advantages of using `@RequiredArgsConstructor` (Lombok)?*  
  It generates a constructor with required final fields, reducing boilerplate and promoting dependency injection.

---

### 2.2 MongoDB
**Why MongoDB?**
- **Document model** matches the queue token structure (tokens are nested within queue documents, avoiding joins).
- **Geospatial queries** enable location‑based search (used in `/places/nearby`).
- **Flexible schema** allows adding new fields without migrations (e.g., `isActive` added later).
- **Horizontal scalability** through sharding (if needed in future).

**Key Collections & Indexes**
- `users`: unique index on `email`.
- `places`: `2dsphere` index on `location`, compound index on `adminId` and `location`.
- `queues`: indexes on `providerId`, `placeId`, `serviceId`, `tokens.userId`.
- `feedback`: indexes on `tokenId` (unique), `placeId`, `providerId`.
- `audit_logs`: indexes on `userId`, `action`, `timestamp`.
- `notification_preferences`: unique compound index on `userId` and `queueId`.

**Interview Q&A**
- *Why MongoDB instead of SQL?*  
  The queue token data is naturally nested; MongoDB’s document model avoids complex joins and allows atomic updates on the entire queue.
- *How do you handle transactions?*  
  MongoDB supports multi‑document transactions, but we use atomic updates on a single document (e.g., adding a token) which is sufficient for most cases. For operations that modify multiple documents (e.g., resetting a queue and clearing user tokens), we use a service‑level transaction.
- *What indexes do you have and why?*  
  I explained above; they are crucial for query performance (e.g., `tokens.userId` to find queues a user is in).

---

### 2.3 Redis
**Why Redis?**
- **OTP storage** – OTPs are short‑lived; Redis TTL automatically expires them.
- **Caching** – reduces database load for frequently read data (e.g., place lists).
- **Rate limiting** – `Bucket4j` stores buckets in memory (no external Redis needed in current implementation, but Redis could be added for distributed rate limiting).

**Configuration**
- `RedisCacheConfig` sets up `RedisCacheManager` with JSON serialization for Java 8 dates and GeoJSON types.
- Cache annotations: `@Cacheable`, `@CacheEvict` on service methods.

**Interview Q&A**
- *Why use Redis for OTPs instead of MongoDB with TTL?*  
  Redis TTL is more efficient and ensures automatic cleanup without needing a scheduled job. It’s also faster for key‑value lookups.
- *How does the caching work?*  
  Spring’s cache abstraction wraps Redis. Methods annotated with `@Cacheable` first check Redis; if data exists, it’s returned without hitting MongoDB. `@CacheEvict` invalidates entries after updates.

---

### 2.4 Spring Security & JWT
**Why JWT?**
- Stateless – no session storage, scales easily.
- Contains user details (role, email) in the token, avoiding extra database calls.
- Can be validated on each request.

**Flow**
1. User registers/logs in → server validates credentials, creates JWT, returns it.
2. Client stores JWT (localStorage) and sends it in `Authorization: Bearer <token>` header.
3. `JwtAuthenticationFilter` intercepts each request, validates the token, and sets `SecurityContext`.

**Role‑Based Access**  
Custom annotations (`@AdminOnly`, `@ProviderOnly`) use `@PreAuthorize` to enforce method‑level security.

**Interview Q&A**
- *How do you secure WebSocket endpoints?*  
  A `StompJwtChannelInterceptor` validates the JWT during CONNECT and sets the user in the `StompHeaderAccessor`.
- *What happens if a token expires?*  
  The filter returns 401, and the frontend redirects to login.
- *Why use `@PreAuthorize` over URL‑based security?*  
  It allows fine‑grained control directly in the controller, making security rules explicit and close to the business logic.

---

### 2.5 WebSocket (STOMP)
**Why WebSocket?**
- Real‑time updates are essential for queue status (position, wait time, token served).
- STOMP provides a simple messaging protocol over WebSocket, with built‑in support for user‑specific destinations (`/user/queue/...`).

**Implementation**
- `WebSocketConfig` enables STOMP, sets endpoint `/ws`, configures message broker (`/topic`, `/queue`).
- `QueueWebSocketController` handles incoming messages (e.g., `/app/queue/serve-next`) and broadcasts to subscribers.

**Interview Q&A**
- *How do you ensure only the right user receives private messages?*  
  Spring automatically translates `/user/queue/...` to `/queue/...-user...` based on the authenticated user.
- *What happens if WebSocket connection drops?*  
  The client attempts reconnection via SockJS (fallback). The server doesn’t store messages; the client re‑subscribes on reconnect.

---

### 2.6 Payment Integration (Razorpay)
**Why Razorpay?**
- Popular in India, easy to integrate, provides test mode.
- Handles payment confirmation via webhook (or we directly confirm on frontend callback).

**Flow**
1. Frontend requests order creation (`/create-order`) with user email and token type.
2. Backend calls Razorpay API to create order, stores order info in `Payment` collection.
3. Frontend calls Razorpay checkout, user pays.
4. On success, frontend calls `/confirm` endpoint with orderId and paymentId.
5. Backend verifies payment, generates a token (`Token` entity), and marks it as used on registration.

**Interview Q&A**
- *How do you prevent duplicate payments?*  
  The `orderId` is stored in the database with a unique constraint; if the same order is confirmed twice, it will fail.
- *What if the payment succeeds but the token generation fails?*  
  The payment is already marked as paid; we could retry token generation or log an alert. We store the payment details so admin can manually issue a token.

---

### 2.7 Push Notifications (Firebase Cloud Messaging)
**Why FCM?**
- Free, cross‑platform (web, Android, iOS).
- Allows sending messages to multiple devices (`multicast`).

**Implementation**
- Frontend registers FCM token via `/user/fcm-token` endpoint.
- `TokenNotificationScheduler` runs every minute, checks upcoming tokens, and sends push notifications to the user’s registered devices (if enabled).
- `BestTimeNotificationScheduler` sends notifications when queue length drops below threshold.

**Interview Q&A**
- *How do you handle notification permissions?*  
  The frontend requests permission, and if granted, it calls the backend to store the token. If permission is denied, no token is sent.
- *What if a user has multiple devices?*  
  We store a list of FCM tokens in the user document, and `sendMulticast` delivers to all.

---

### 2.8 Email Service (JavaMail)
**Why JavaMail?**
- Standard, integrates with any SMTP server.
- Used for OTP verification, password reset links, and alerts.

**Template Management**
- HTML templates are stored in `src/main/resources/templates/` and read as strings (since we’re not using Thymeleaf).
- Placeholders are replaced with dynamic content.

**Interview Q&A**
- *How do you prevent spam?*  
  We only send emails on user‑triggered events (registration, password reset) or admin‑initiated actions. Rate limits prevent abuse.
- *What if the email server is down?*  
  The email sending is wrapped in try‑catch; errors are logged but the user is informed (e.g., “OTP could not be sent”); they can retry.

---

### 2.9 Monitoring (Prometheus, Grafana, Loki)
**Why this stack?**
- Prometheus scrapes metrics from Spring Boot Actuator (exposed at `/actuator/prometheus`).
- Grafana visualizes metrics (e.g., request latency, token creation rate).
- Loki collects logs from Docker containers, enabling centralised log search.

**Configuration**
- Spring Boot Actuator is enabled with Prometheus endpoint.
- Docker Compose includes Prometheus, Grafana, Loki, and Promtail services.
- Grafana dashboards are pre‑configured (not in code, but can be exported/imported).

**Interview Q&A**
- *What metrics do you track?*  
  Custom metrics like `queue.waiting.tokens`, `queue.in.service.tokens`, `queue.total.served` via Micrometer `Gauge`. Also HTTP request duration, error rate, and database connection pool.
- *How do you handle logs?*  
  Logs are written to stdout; Promtail scrapes Docker container logs and sends them to Loki. You can search logs in Grafana.

---

### 2.10 Frontend: React & Redux Toolkit
**Why React?**
- Component‑based, reusable, large ecosystem.
- Vite for fast development builds.

**State Management**
- Redux Toolkit for global state (auth, places, services, search, user, etc.).
- WebSocket updates are dispatched to Redux, which updates components.

**Why React Bootstrap?**
- Provides accessible, responsive components out of the box.
- Easy to customise with CSS.

**Interview Q&A**
- *How do you handle authentication state?*  
  We store JWT in localStorage and Redux. On app load, we check localStorage and restore state. Axios interceptor attaches token to every request.
- *How does WebSocket integration work?*  
  We have a singleton `WebSocketService` that connects on login and disconnects on logout. It subscribes to queue topics and dispatches Redux actions on messages.
- *Why use Redux Toolkit over plain Redux?*  
  Reduces boilerplate, includes Immer for immutable updates, and provides `createAsyncThunk` for async actions.

---


## 3. Backend Package Structure & Key Classes

### 3.1 Package Overview

The backend follows a standard layered architecture:

```
src/main/java/com/queueless/backend/
├── config/               – Spring configuration classes
├── controller/           – REST controllers and WebSocket controller
├── dto/                  – Data Transfer Objects (request/response)
├── enums/                – Enumerations (Role, TokenStatus)
├── exception/            – Custom exceptions and global handler
├── model/                – MongoDB document entities
├── repository/           – Spring Data MongoDB repositories
├── scheduler/            – Scheduled tasks (notifications, metrics)
├── security/             – JWT filters, authentication, annotations
└── service/              – Business logic
```

### 3.2 Key Classes & Their Responsibilities

#### Config Package
- `FirebaseConfig.java` – initializes Firebase Admin SDK for FCM.
- `OpenAPIConfig.java` – configures Swagger/OpenAPI documentation.
- `RateLimitConfig.java` – creates `Bucket` instances for rate limiting (per user/IP + endpoint group).
- `RedisCacheConfig.java` – configures Redis cache manager with JSON serialization.
- `WebSocketConfig.java` – enables STOMP, registers endpoint `/ws`, configures message broker.
- `SecurityConfig.java` – the main security configuration: CORS, CSRF (disabled), session management (stateless), URL‑based authorization, and adding custom filters.

#### Controller Package
- `AuthController.java` – handles registration, login, email verification, OTP resend.
- `PlaceController.java` – CRUD for places; geospatial nearby search.
- `ServiceController.java` – CRUD for services under a place.
- `QueueController.java` – all queue operations (create, join, serve, complete, cancel, reorder, reset, etc.).
- `FeedbackController.java` – submit feedback, retrieve by place/provider, average ratings.
- `AdminController.java` – admin dashboard stats, provider management, payments, reports.
- `ProviderController.java` – provider‑specific data (managed places, services, analytics).
- `UserController.java` – user profile, favorites, token history, FCM token registration.
- `SearchController.java` – comprehensive search across places, services, queues.
- `PaymentController.java` – Razorpay order creation and confirmation.
- `ExportController.java` – exports queue data to PDF/Excel.
- `PublicController.java` – public statistics for home page.
- `QueueWebSocketController.java` – WebSocket message handlers (`/queue/connect`, `/queue/serve-next`, `/queue/add-token`, `/queue/status`, etc.).
- `PasswordResetController.java` – forgot password OTP flow.
- `PasswordResetTokenController.java` – admin‑initiated password reset (token‑based).
- `NotificationPreferenceController.java` – manage per‑queue notification settings.

#### DTO Package
Each DTO is used to transfer data between client and server, often with validation annotations (`@NotBlank`, `@Size`, `@Min`). Examples:
- `LoginRequest`, `RegisterRequest`, `PlaceDTO`, `ServiceDTO`, `QueueResetRequestDTO`, `TokenRequestDTO`, `FeedbackDTO`, `NotificationPreferenceDTO`, etc.

#### Model Package
MongoDB documents (entities) annotated with `@Document`:
- `User` – stores all user data (including `isActive`, `managedPlaceIds`, `fcmTokens`).
- `Place` – geospatial field `location` as `GeoJsonPoint`.
- `Service` – linked to a place.
- `Queue` – contains a list of `QueueToken` (embedded).
- `Feedback` – linked to token, queue, user, provider, place.
- `Payment`, `Token` – for paid tokens.
- `AuditLog` – audit trail.
- `NotificationPreference` – per‑queue user preferences.
- `PasswordResetToken` – for admin‑initiated reset.

#### Repository Package
Spring Data MongoDB repositories extend `MongoRepository<T, String>` and define custom query methods (e.g., `findByProviderId`, `findByPlaceIdIn`, `findByUserIdAndQueueId`). Also includes custom queries with `@Query` for geospatial search.

#### Security Package
- `JwtTokenProvider.java` – generates and validates JWT; extracts claims.
- `JwtAuthenticationFilter.java` – intercepts requests, validates JWT, sets `SecurityContext`.
- `JwtAuthenticationEntryPoint` – handles 401 errors.
- `JwtAccessDeniedHandler` – handles 403 errors.
- `StompJwtChannelInterceptor` – validates JWT for WebSocket CONNECT frames.
- Custom annotations (`@AdminOnly`, `@ProviderOnly`, etc.) – used with `@PreAuthorize`.
- `RateLimitFilter.java` – applies rate limiting based on user/IP and endpoint group.

#### Service Package
All business logic resides here. Key services:
- `AuthService` – registration, login, email verification.
- `UserService` – profile update, password change, favorites, token history.
- `PlaceService` – CRUD, nearby search, caching.
- `ServiceService` – CRUD.
- `QueueService` – queue operations, token management, reset, position calculation, best time to join.
- `FeedbackService` – submission, rating aggregation.
- `AdminService` – dashboard stats, provider details, reports.
- `ProviderAnalyticsService` – provider‑specific analytics.
- `SearchService` – comprehensive search (places, services, queues) using MongoDB aggregation and geospatial queries.
- `PaymentService` – Razorpay integration, token generation.
- `ExportService` – PDF/Excel generation.
- `EmailService` – email sending with templates.
- `FcmService` – push notifications.
- `FileStorageService` – profile image uploads.
- `AuditLogService` – logging business events.
- `NotificationPreferenceService` – per‑queue user preferences.
- `PasswordResetService` – OTP‑based reset.
- `PasswordResetTokenService` – token‑based reset.
- `QueueMetricsService` – exposes custom metrics (waiting tokens, in‑service tokens, total served) via Micrometer.
- `QRCodeService` – generates QR codes for queue join links.

#### Scheduler Package
- `TokenNotificationScheduler.java` – runs every minute, checks for upcoming tokens, sends email/push notifications.
- `BestTimeNotificationScheduler.java` – runs hourly, checks queues with short waiting times, sends notifications to opted‑in users.
- `MetricsScheduler.java` – updates queue metrics every 15 seconds.
- `QueueAnalyticsScheduler.java` – takes hourly snapshots of waiting counts for analytics.
- `AlertScheduler.java` – checks for queues exceeding wait time threshold and sends alerts to admins.

#### Exception Package
- Custom exceptions (`ResourceNotFoundException`, `QueueInactiveException`, `UserAlreadyInQueueException`, `AccessDeniedException`, etc.) and `GlobalExceptionHandler` to return consistent error responses.

### 3.3 Design Patterns Used

| Pattern           | Where it appears |
|-------------------|------------------|
| **Repository**    | All `*Repository` interfaces (Spring Data) |
| **DTO**           | `*DTO` classes separate internal entity from API contracts. |
| **Service Layer** | All `*Service` classes encapsulate business logic. |
| **Builder**       | Used in entities with Lombok `@Builder` (e.g., `User`, `Payment`). |
| **Factory**       | `RateLimitConfig.resolveBucket` returns appropriate bucket based on endpoint group. |
| **Interceptor**   | `JwtAuthenticationFilter`, `StompJwtChannelInterceptor` – intercept HTTP/WebSocket requests. |
| **Singleton**     | `WebSocketService` in frontend is a singleton; backend beans are singletons by default. |
| **Observer**      | WebSocket broadcasts act as an observable pattern; clients observe queue state changes. |
| **Aspect**        | `@PreAuthorize` is a form of AOP; custom aspects could be added (not used yet). |

### 3.4 Diagram Prompts (for Mermaid/PlantUML)

**Backend Package Diagram (Mermaid)**
```mermaid
graph TD
    subgraph "Controller Layer"
        AuthController
        PlaceController
        QueueController
        AdminController
    end
    subgraph "Service Layer"
        AuthService
        PlaceService
        QueueService
        AdminService
    end
    subgraph "Repository Layer"
        UserRepository
        PlaceRepository
        QueueRepository
    end
    AuthController --> AuthService
    PlaceController --> PlaceService
    QueueController --> QueueService
    AdminController --> AdminService
    AuthService --> UserRepository
    PlaceService --> PlaceRepository
    QueueService --> QueueRepository
    AdminService --> PlaceRepository
    AdminService --> QueueRepository
    AdminService --> UserRepository
```

**Queue Reset Flow Diagram (Mermaid)**
```mermaid
sequenceDiagram
    participant Admin as Admin
    participant Frontend
    participant Backend
    participant QueueService
    participant ExportService
    participant Cache

    Admin->>Frontend: Clicks "Reset Queue"
    Frontend->>Backend: POST /queues/{id}/reset-with-options
    Backend->>QueueService: resetQueueWithOptions()
    alt preserveData == true
        QueueService->>ExportService: exportQueueToPdf()
        ExportService-->>Cache: store export
        Cache-->>QueueService: exportId
    end
    QueueService->>QueueService: clear tokens, reset counter
    QueueService->>QueueRepository: save queue
    QueueService-->>Backend: response (success, exportFileUrl)
    Backend-->>Frontend: 200 OK
    Frontend-->>Admin: show success
```

**Notification Scheduler Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant Scheduler as TokenNotificationScheduler
    participant QueueRepository
    participant UserRepository
    participant EmailService
    participant FcmService

    loop every minute
        Scheduler->>QueueRepository: findAll()
        loop each queue
            Scheduler->>QueueService: calculate waiting tokens
            loop each waiting token
                Scheduler->>UserRepository: find user
                alt user has preferences
                    Scheduler->>NotificationPreferenceService: get per-queue pref
                end
                Scheduler->>EmailService: send email (if enabled)
                Scheduler->>FcmService: send push (if enabled)
                Scheduler->>QueueRepository: mark token as notified
            end
        end
    end
```

---

## 4. Authentication & Authorization Flow

QueueLess uses JWT (JSON Web Tokens) for stateless authentication and Spring Security’s method‑level annotations for authorization.

### 4.1 Registration Flow

**User Registration (USER role)**
1. **Frontend** sends `POST /api/auth/register` with user details (name, email, password, phone, role=USER).
2. `AuthController.register()` calls `AuthService.register()`.
3. `AuthService` checks if email already exists. If not, creates a `User` entity with `isVerified = false` and persists it.
4. A 6‑digit OTP is generated and stored in Redis (using `OtpRepository`, which uses a collection with TTL).
5. An email is sent to the user containing the OTP (using `EmailService`).
6. The user receives an email and enters the OTP on the frontend.

**Admin / Provider Registration**
- They must first purchase a token via Razorpay (admin buys an admin token for themselves, or buys provider tokens for others).
- The token is stored in the `tokens` collection with a `tokenValue`, `role`, `expiryDate`, and `used=false`.
- During registration, the user supplies the token in the request.
- `AuthService` validates the token (checks existence, expiry, not used, role match, and email match).
- If valid, the token is marked as used, the user is created with `isVerified = true` (since they purchased a token), and an OTP is still sent? Wait – in our implementation we set `isVerified = false` for all roles, but then we also call `sendVerificationOtp` for all roles. That means admins/providers also have to verify their email via OTP. That’s a security trade‑off (email ownership). In the code we saw, after registration, `sendVerificationOtp` is called for all roles. So they will get an OTP and must verify. That’s fine.

**Email Verification**
- Frontend sends `POST /api/auth/verify-email` with email and OTP.
- `AuthService` checks OTP in Redis, marks user as verified, deletes OTP record.

**Resend OTP**
- `POST /api/auth/resend-verification?email=...` triggers a new OTP.

### 4.2 Login Flow

1. Frontend sends `POST /api/auth/login` with email and password.
2. `AuthService.login()` validates email and password, checks `isVerified` and `isActive` (if false, rejects).
3. Generates JWT using `JwtTokenProvider.generateToken()`, which includes user ID, email, role, etc.
4. Returns JWT to frontend.
5. Frontend stores JWT (usually in localStorage) and includes it in subsequent requests as `Authorization: Bearer <token>`.

### 4.3 JWT Validation

- Every request (except public endpoints) passes through `JwtAuthenticationFilter`.
- The filter extracts the token from the `Authorization` header, validates it using `JwtTokenProvider.validateToken()`.
- If valid, it sets the `SecurityContext` with `UsernamePasswordAuthenticationToken` containing the user ID as principal and authorities (role).
- Spring Security then uses this context for method‑level authorization (`@PreAuthorize`).

### 4.4 Role‑Based Access Control

- Custom annotations like `@AdminOnly`, `@ProviderOnly`, `@UserOnly`, `@Authenticated` are defined in `security.annotations` package.
- They are used with `@PreAuthorize` (e.g., `@PreAuthorize("hasRole('ADMIN')")`).
- Spring Security’s method security is enabled with `@EnableMethodSecurity(prePostEnabled = true)`.

### 4.5 WebSocket Authentication

- During WebSocket connection (STOMP), the client sends a token in the CONNECT frame (e.g., in the `Authorization` header).
- `StompJwtChannelInterceptor` intercepts the CONNECT message, validates the token, and sets the user in the `StompHeaderAccessor`.
- This user is then used for subscriptions and message handling (e.g., `/user/queue/emergency-approved`).

### 4.6 Security Configuration Highlights

- **`SecurityConfig.java`**:
    - `cors()` – allows frontend origins (e.g., `https://localhost:5173`).
    - `csrf().disable()` – because we are stateless and use JWT.
    - `sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)`
    - `authorizeHttpRequests()` – defines public endpoints (`/api/auth/**`, `/api/password/**`, `/api/public/**`, Swagger, etc.) and role‑based endpoints.
    - Adds `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`.

### 4.7 Code References

**`AuthService.register()`** – core registration logic, includes token validation for admin/provider roles.

```java
public String register(RegisterRequest request) {
    // ... validation and token handling ...
    User user = User.builder()
            .name(request.getName())
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .phoneNumber(request.getPhoneNumber())
            .role(request.getRole())
            .isVerified(false)
            .preferences(userPreferences)
            .build();
    userRepository.save(user);
    sendVerificationOtp(request.getEmail());
    auditLogService.logEvent("USER_REGISTERED", ...);
    return "User registered successfully! Please verify your email.";
}
```

**`AuthService.login()`** – includes `isActive` check.

```java
public JwtResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid credentials");
    }
    if (!user.getIsVerified()) {
        throw new RuntimeException("Account not verified");
    }
    if (user.getIsActive() != null && !user.getIsActive()) {
        throw new RuntimeException("Your account has been disabled");
    }
    String jwtToken = jwtProvider.generateToken(user);
    return new JwtResponse(jwtToken, ...);
}
```

**`JwtTokenProvider.generateToken()`** – adds claims.

```java
public String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", user.getId());
    claims.put("role", user.getRole().name());
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());
    claims.put("name", user.getName());
    // ...
    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}
```

### 4.8 Potential Interview Questions & Answers

1. **How do you handle user registration for different roles?**
    - For USER, they register normally, verify email via OTP.
    - For ADMIN/PROVIDER, they must have a pre‑purchased token (via Razorpay). The token is validated during registration; after validation, they still need to verify email.

2. **Why do you send OTP for admin/provider even though they already have a token?**
    - To ensure the email address is owned by the person registering, adding an extra security layer.

3. **How do you prevent brute‑force attacks on login?**
    - Rate limiting (e.g., 10 attempts per minute per IP) via `RateLimitFilter`.

4. **How do you store passwords?**
    - Using BCrypt with `BCryptPasswordEncoder` from Spring Security.

5. **How do you handle token refresh?**
    - We don’t implement refresh tokens currently; JWT expires after 24 hours, and users must log in again. This is acceptable for now but could be improved with refresh tokens.

6. **What happens if a user logs out?**
    - Frontend removes token from localStorage and Redux; backend doesn’t store sessions, so logout is purely client‑side.

7. **How do you ensure that a user cannot join a queue if already in another queue?**
    - The `User` entity has an `activeTokenId` field. When a user joins a queue, we set it; when token is completed/cancelled, we clear it. `canUserJoinQueue()` checks if `activeTokenId` is null.

8. **How do you secure WebSocket endpoints?**
    - `StompJwtChannelInterceptor` validates token during CONNECT. Subscriptions are also secured via `@PreAuthorize` in `WebSocketSecurityConfig`.

9. **What is the purpose of `auditLogService.logEvent()` in authentication?**
    - To log successful logins and registrations for auditing and debugging.

10. **How do you handle email sending in a non‑blocking way?**
    - The `sendVerificationOtpEmail` is synchronous, but it’s a lightweight operation; if needed, we could make it asynchronous using `@Async`.

### 4.9 Diagram Prompts

**JWT Authentication Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant Client
    participant Backend
    participant JwtAuthenticationFilter
    participant Controller
    Client->>Backend: POST /login (email, password)
    Backend->>Backend: validate credentials
    Backend-->>Client: JWT token
    Client->>Backend: Request with Authorization header
    Backend->>JwtAuthenticationFilter: filter
    JwtAuthenticationFilter->>JwtTokenProvider: validate token
    JwtTokenProvider-->>JwtAuthenticationFilter: valid
    JwtAuthenticationFilter->>SecurityContext: set authentication
    JwtAuthenticationFilter->>Controller: proceed
    Controller-->>Client: response
```

**Registration Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant UserRepository
    participant OtpRepository
    participant EmailService

    Client->>AuthController: POST /register
    AuthController->>AuthService: register()
    AuthService->>UserRepository: existsByEmail?
    alt email exists
        AuthService-->>Client: error
    else
        AuthService->>UserRepository: save user (verified=false)
        AuthService->>OtpRepository: save OTP with TTL
        AuthService->>EmailService: send OTP email
        AuthService-->>Client: success, need verification
    end
    Client->>AuthController: POST /verify-email
    AuthController->>AuthService: verifyEmail()
    AuthService->>OtpRepository: find OTP
    alt OTP valid
        AuthService->>UserRepository: update user verified=true
        AuthService->>OtpRepository: delete OTP
        AuthService-->>Client: success
    else
        AuthService-->>Client: error
    end
```

---

## 5. Queue Management – Core Business Logic

Queue management is the heart of QueueLess. This section explains how queues are created, how tokens are processed, and how real‑time updates work.

### 5.1 Queue Data Model

**`Queue` entity** (MongoDB document)
- Contains embedded list of `QueueToken` objects.
- Each `QueueToken` includes `tokenId`, `userId`, `userName`, `status`, `issuedAt`, `servedAt`, `completedAt`, `isEmergency`, `emergencyDetails`, `priority`, `isGroup`, `groupMembers`, etc.
- Queue statistics are stored in embedded `QueueStatistics` (average wait time, daily users served, etc.).
- Supports `maxCapacity`, `isActive`, `supportsGroupToken`, `emergencySupport`, `emergencyPriorityWeight`, `requiresEmergencyApproval`, `autoApproveEmergency`.

### 5.2 Creating a Queue

- **Endpoint**: `POST /api/queues/create` (provider or admin).
- **Controller**: `QueueController.createNewQueue()` – extracts provider ID from authentication, calls `QueueService.createNewQueue()`.
- **Service**:
    - Validates that the provider has access to the place and service (through `managedPlaceIds` or admin relation).
    - Creates a new `Queue` instance, sets defaults, and saves to MongoDB.
    - Logs audit event (`QUEUE_CREATED`).

**Code Snippet** (simplified)
```java
public Queue createNewQueue(String providerId, String serviceName, String placeId,
                            String serviceId, Integer maxCapacity, Boolean supportsGroupToken,
                            Boolean emergencySupport, Integer emergencyPriorityWeight,
                            Boolean requiresEmergencyApproval, Boolean autoApproveEmergency) {
    // Validation and place/service checks
    Queue newQueue = new Queue(providerId, serviceName, placeId, serviceId);
    newQueue.setMaxCapacity(maxCapacity);
    newQueue.setSupportsGroupToken(supportsGroupToken);
    newQueue.setEmergencySupport(emergencySupport);
    newQueue.setEmergencyPriorityWeight(emergencyPriorityWeight);
    newQueue.setRequiresEmergencyApproval(requiresEmergencyApproval);
    newQueue.setAutoApproveEmergency(autoApproveEmergency);
    newQueue = queueRepository.save(newQueue);
    auditLogService.logEvent("QUEUE_CREATED", ...);
    return newQueue;
}
```

### 5.3 Adding a Token

**Regular token**: `POST /api/queues/{queueId}/add-token` (user only)
- Checks:
    - Queue is active.
    - User does not already have an active token in any queue (via `user.getActiveTokenId()`).
    - User does not already have a waiting/in‑service token in this queue.
    - Max capacity not exceeded.
- Generates a unique token ID (e.g., `queueId-T-001`).
- Creates `QueueToken` with status `WAITING`, adds to queue's token list.
- Updates user's `activeTokenId`.
- Saves queue and user.
- Broadcasts update via WebSocket.

**Group token**: `POST /api/queues/{queueId}/add-group-token`
- Requires queue to support group tokens.
- Validates that group members list is provided (at least 2 members).
- Creates token with `isGroup=true`, stores group members.

**Emergency token**: `POST /api/queues/{queueId}/add-emergency-token`
- Requires queue to support emergency tokens.
- If `autoApproveEmergency` is true, token is immediately added as `WAITING` with high priority.
- Otherwise, token is added to `pendingEmergencyTokens` list with status `PENDING`.
- Provider later approves/rejects via `/api/queues/{queueId}/approve-emergency/{tokenId}`.

**Token with details**: `POST /api/queues/{queueId}/add-token-with-details`
- Allows user to provide purpose, condition, notes, and privacy settings.
- User details are stored in `UserQueueDetails` object inside the token.

### 5.4 Serving Next Token

- **Endpoint**: `POST /api/queues/{queueId}/serve-next` (provider/admin).
- **Service method**: `QueueService.serveNextToken()`
- Steps:
    1. Find any existing `IN_SERVICE` token and mark it as `COMPLETED` (if present). Clear user's active token.
    2. Find the next waiting token with highest priority (emergency tokens have higher priority) and earliest issue time.
    3. Change its status to `IN_SERVICE`, set `servedAt`, and save.
    4. Broadcast update.

### 5.5 Completing a Token

- **Endpoint**: `POST /api/queues/{queueId}/complete-token` (provider/admin)
- **Service**: `QueueService.completeToken()`
- Marks token as `COMPLETED`, sets `completedAt`, calculates service duration (if servedAt present), clears user's active token, saves.

### 5.6 Canceling a Token

- **Endpoint**: `DELETE /api/queues/{queueId}/cancel-token/{tokenId}?reason=...`
- **Service**: `QueueService.cancelToken()`
- For regular tokens: sets status `CANCELLED`, clears user's active token, sends WebSocket notification to user.
- For pending emergency tokens: removes from pending list (no user state change).
- Logs cancellation reason.

### 5.7 Queue Reset with Data Preservation

- **Endpoint**: `POST /api/queues/{queueId}/reset-with-options` (provider/admin)
- **Request body**: `QueueResetRequestDTO` with `preserveData`, `reportType`, `includeUserDetails`.
- **Service**:
    - If `preserveData` true, calls `ExportService` to generate PDF/Excel of current queue data and stores it in `ExportCacheService` (returns a download URL).
    - Clears all tokens, resets counters, clears user active tokens.
    - Saves queue, broadcasts update.
- Returns `QueueResetResponseDTO` with success status, export file URL, and number of tokens cleared.

### 5.8 Reordering Queue

- **Endpoint**: `PUT /api/queues/{queueId}/reorder` (provider/admin)
- **Body**: new order of `QueueToken` objects.
- **Service**: `QueueService.reorderQueue()` – replaces the tokens list, resets notification flags for waiting tokens, saves, broadcasts.

### 5.9 Real‑time Updates via WebSocket

- When a queue changes (token added, served, completed, cancelled, reordered), `QueueService` calls `broadcastQueueUpdate()`.
- This sends the updated queue object to `/topic/queues/{queueId}`.
- All clients subscribed to that topic (e.g., the queue page, provider dashboard) receive the update instantly.
- Additionally, when a token is served/completed, a private message is sent to the user (`/user/queue/provider-updates`) if they are the provider.

### 5.10 Schedulers Related to Queues

- **`updateAllQueueWaitTimes`** (every 30 seconds): recalculates estimated wait time based on waiting tokens * average service time.
- **`cleanupExpiredTokens`** (every hour): removes tokens older than 24 hours and clears user active token references.
- **`TokenNotificationScheduler`** (every minute): checks waiting tokens and sends email/push notifications before the user’s turn.
- **`BestTimeNotificationScheduler`** (hourly): checks queues with low waiting count and sends notifications to opted‑in users.

### 5.11 Key Code Snippets

**Adding a regular token** (`QueueService.addNewToken` excerpt)
```java
public QueueToken addNewToken(String queueId, String userId) {
    Queue queue = getQueueOrThrow(queueId);
    // ... validation ...
    int nextToken = queue.getTokenCounter() + 1;
    queue.setTokenCounter(nextToken);
    String tokenId = queueId + "-T-" + String.format("%03d", nextToken);
    QueueToken token = new QueueToken(tokenId, userId, TokenStatus.WAITING.toString(), LocalDateTime.now());
    queue.getTokens().add(token);
    User user = getUserOrThrow(userId);
    user.setActiveTokenId(tokenId);
    user.setLastQueueJoinTime(LocalDateTime.now());
    userRepository.save(user);
    Queue updatedQueue = queueRepository.save(queue);
    broadcastQueueUpdate(queueId, updatedQueue);
    return token;
}
```

**Serving next token** (`QueueService.serveNextToken` excerpt)
```java
public Queue serveNextToken(String queueId) {
    Queue queue = getQueueOrThrow(queueId);
    // Complete previous in‑service token
    Optional<QueueToken> previousInService = queue.getTokens().stream()
            .filter(t -> TokenStatus.IN_SERVICE.toString().equals(t.getStatus()))
            .findFirst();
    previousInService.ifPresent(inServiceToken -> {
        inServiceToken.setStatus(TokenStatus.COMPLETED.toString());
        inServiceToken.setCompletedAt(LocalDateTime.now());
        User user = getUserOrThrow(inServiceToken.getUserId());
        user.setActiveTokenId(null);
        userRepository.save(user);
    });
    // Find next waiting token (highest priority, then earliest issued)
    Optional<QueueToken> nextToken = queue.getTokens().stream()
            .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
            .max((t1, t2) -> {
                if (!t1.getPriority().equals(t2.getPriority()))
                    return t2.getPriority() - t1.getPriority();
                return t1.getIssuedAt().compareTo(t2.getIssuedAt());
            });
    if (nextToken.isPresent()) {
        QueueToken token = nextToken.get();
        token.setStatus(TokenStatus.IN_SERVICE.toString());
        token.setServedAt(LocalDateTime.now());
        Queue updatedQueue = queueRepository.save(queue);
        broadcastQueueUpdate(queueId, updatedQueue);
        return updatedQueue;
    }
    return queue;
}
```

### 5.12 Interview Questions & Answers

1. **How do you ensure that a user cannot join multiple queues at once?**  
   The `User` entity has an `activeTokenId` field. When joining a queue, we set it; when a token is completed/cancelled, we clear it. `canUserJoinQueue()` checks if `activeTokenId == null`. This prevents simultaneous participation.

2. **How do you handle race conditions when two users try to join the same queue at the same time?**  
   MongoDB’s atomic updates on a single document (the queue) prevent corruption. The `Queue` document is updated in one operation (e.g., adding a token). If two updates happen concurrently, MongoDB’s document‑level locking ensures one completes before the other.

3. **How do you calculate the estimated wait time?**  
   We multiply the number of waiting tokens by the average service time of the associated service. This is recalculated every 30 seconds.

4. **How do emergency tokens get priority?**  
   Emergency tokens are assigned a higher `priority` (e.g., 10) than regular tokens (0). When serving the next token, we sort by priority descending before issue time.

5. **How do you manage group tokens?**  
   A group token is a single token that represents multiple people. It has `isGroup = true` and a list of `groupMembers`. When served, all members are served together. The token is treated as one entry in the queue.

6. **What happens if a provider resets a queue and chooses to preserve data?**  
   The system exports the current queue data (tokens, statistics) to a PDF/Excel file, stores it in a cache with a unique ID, and returns a download URL in the response. The actual reset clears all tokens and resets counters.

7. **How do you broadcast queue updates to clients?**  
   After any modification, we publish the updated `Queue` object to the WebSocket topic `/topic/queues/{queueId}`. All clients subscribed to that topic receive it instantly. Private messages (e.g., to a specific user) are sent to `/user/queue/...`.

8. **How do you handle token cancellation?**  
   The user can cancel their own waiting token via `DELETE /api/queues/{queueId}/cancel-token/{tokenId}`. The service marks the token as `CANCELLED`, clears the user's `activeTokenId`, and broadcasts an update. If a provider cancels a token, they can provide a reason, which is sent to the user via WebSocket.

9. **How do you ensure that the queue order is maintained after a reset?**  
   The reset operation completely clears all tokens, so the order is lost. If the admin needs to preserve order, they can export the data before reset.

10. **How does the “best time to join” feature work?**  
    We collect hourly snapshots of waiting counts (via `QueueAnalyticsScheduler`). For a given queue, we calculate the average waiting count for each hour of the day over the last 30 days. The hours with the lowest average waiting count are recommended as the best time to join.

11. **How do you prevent a user from joining a queue that is full?**  
    The `maxCapacity` field is checked before adding a token. If the number of waiting + in‑service tokens >= maxCapacity, we throw a `QueueFullException`.

12. **How do you implement queue reordering?**  
    The frontend allows drag‑and‑drop of waiting tokens. On drop, it sends the new order of tokens (a full list) to the backend. The backend replaces the tokens list, resets notification flags, saves, and broadcasts.

13. **How do you handle the case where a provider serves a token and there is already an in‑service token?**  
    The `serveNextToken()` method first completes any existing in‑service token (marks as completed, clears user active token) before moving the next waiting token to in‑service. This ensures only one token is in‑service at a time.

### 5.13 Diagram Prompts

**Queue Reset Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant Admin
    participant Frontend
    participant Backend
    participant QueueService
    participant ExportService
    participant Cache

    Admin->>Frontend: Click "Reset Queue"
    Frontend->>Backend: POST /queues/{id}/reset-with-options
    Backend->>QueueService: resetQueueWithOptions()
    alt preserveData == true
        QueueService->>ExportService: exportQueueToPdf()
        ExportService-->>Cache: store export
        Cache-->>QueueService: exportId
    end
    QueueService->>QueueService: clear tokens, reset counter
    QueueService->>QueueRepository: save queue
    QueueService->>UserRepository: clear activeTokenId for affected users
    QueueService-->>Backend: response (success, exportFileUrl)
    Backend-->>Frontend: 200 OK
    Frontend-->>Admin: success message
    opt if exportFileUrl
        Frontend->>Backend: GET /export/exports/{exportId}
        Backend-->>Frontend: file download
    end
```

**Emergency Token Approval Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant User
    participant Frontend
    participant Backend
    participant QueueService
    participant Provider

    User->>Frontend: Click "Request Emergency Token"
    Frontend->>Backend: POST /queues/{id}/add-emergency-token
    alt queue.autoApproveEmergency == false
        Backend->>QueueService: addEmergencyToken()
        QueueService->>Queue: add token to pending list
        QueueService-->>Frontend: token created (PENDING)
        Frontend-->>User: "Emergency request sent, waiting approval"
        Provider->>Frontend: Open approval modal
        Frontend->>Backend: GET /queues/{id}/pending-emergency
        Backend-->>Frontend: list of pending tokens
        Provider->>Frontend: Click "Approve"
        Frontend->>Backend: POST /queues/{id}/approve-emergency/{tokenId}?approve=true
        Backend->>QueueService: approveEmergencyToken()
        QueueService->>Queue: move token to waiting, set high priority
        QueueService-->>User: WebSocket notification "Emergency approved"
        QueueService-->>Provider: success
    else autoApprove == true
        Backend->>QueueService: addEmergencyToken()
        QueueService->>Queue: add token as WAITING with high priority
        QueueService-->>Frontend: token created (WAITING)
        Frontend-->>User: "Emergency token added to queue"
    end
```

---
## 6. Notifications & Schedulers

QueueLess uses several background schedulers to handle notifications, metrics, and analytics. This section explains each scheduler, how they integrate with the rest of the system, and how notifications (email and push) are sent.

### 6.1 Scheduler Overview

All schedulers are annotated with `@Component` and `@Scheduled` (or `@Scheduled` inside a component). They run at fixed intervals and are managed by Spring’s task execution.

**Key Schedulers**

| Scheduler                   | Frequency        | Purpose                                                                 |
|-----------------------------|------------------|-------------------------------------------------------------------------|
| `TokenNotificationScheduler`| every minute     | Checks for tokens that are about to be served and sends email/push notifications. |
| `BestTimeNotificationScheduler` | every hour   | Checks queues with waiting count below threshold and sends “best time” notifications to opted‑in users. |
| `MetricsScheduler`          | every 15 seconds | Updates Prometheus metrics (waiting tokens, in‑service tokens, total served) for each queue. |
| `QueueAnalyticsScheduler`   | every hour       | Takes hourly snapshots of waiting counts for later “best time” analysis. |
| `AlertScheduler`            | every 5 minutes  | Checks if any queue’s wait time exceeds the admin’s configured threshold and sends email alerts. |
| `QueueService.cleanupExpiredTokens` | every hour | Removes tokens older than 24 hours and clears user active tokens. |
| `QueueService.updateAllQueueWaitTimes` | every 30 seconds | Recalculates estimated wait time for all active queues. |

### 6.2 Token Notification Scheduler (`TokenNotificationScheduler`)

This is the most critical notification scheduler. It runs every minute and checks all waiting tokens across all queues to see if the user should be notified.

**Flow**
1. Fetch all queues (`queueRepository.findAll()`).
2. For each queue, get the list of waiting tokens (status `WAITING`).
3. For each waiting token, estimate the time until it will be served:  
   `minutesUntilTurn = (position in waiting list) * averageServiceTime`
   (The average service time comes from the associated `Service` entity; defaults to 5 if not set.)
4. Compare against a threshold (global default `notifyBeforeMinutes`, which can be overridden by per‑queue user preferences).
5. If `minutesUntilTurn <= threshold` and the token has not yet been notified (`notificationSent = false`), send notifications:
    - Email: if user has email notifications enabled.
    - Push: if user has push notifications enabled and has registered FCM tokens.
6. Mark token as notified to avoid duplicate messages.

**Respecting Per‑Queue Preferences**
- The scheduler looks up the user’s `NotificationPreference` for that queue (if any).
- If found and `enabled = true`, it uses the custom `notifyBeforeMinutes` value instead of the global default.
- It also respects the `notifyOnStatusChange` and `notifyOnEmergencyApproval` fields for other types of notifications (though those are handled elsewhere).

**Code Snippet (simplified)**
```java
@Scheduled(fixedRate = 60000) // every minute
public void checkUpcomingTokens() {
    List<Queue> allQueues = queueRepository.findAll();
    for (Queue queue : allQueues) {
        Service service = serviceService.getServiceById(queue.getServiceId());
        int avgServiceTime = service != null ? service.getAverageServiceTime() : 5;
        List<QueueToken> waitingTokens = queue.getTokens().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .sorted(...) // by priority then issue time
                .collect(Collectors.toList());

        Map<String, NotificationPreference> prefMap = preferenceService.getPreferencesForQueue(queue.getId())
                .stream().collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity()));

        for (int i = 0; i < waitingTokens.size(); i++) {
            QueueToken token = waitingTokens.get(i);
            if (token.getNotificationSent()) continue;

            int estimatedMinutes = i * avgServiceTime;
            NotificationPreference pref = prefMap.get(token.getUserId());
            int notifyThreshold = (pref != null && pref.getNotifyBeforeMinutes() != null) ? pref.getNotifyBeforeMinutes() : notifyBeforeMinutes;

            if (estimatedMinutes <= notifyThreshold) {
                User user = userRepository.findById(token.getUserId()).orElse(null);
                if (user != null) {
                    // Send email if enabled
                    if (user.getPreferences() != null && user.getPreferences().getEmailNotifications()) {
                        emailService.sendUpcomingTokenEmail(user.getEmail(), token.getTokenId(),
                                queue.getServiceName(), estimatedMinutes);
                    }
                    // Send push if enabled
                    if (user.getPreferences() != null && user.getPreferences().getPushNotifications()
                            && user.getFcmTokens() != null && !user.getFcmTokens().isEmpty()) {
                        fcmService.sendMulticast(user.getFcmTokens(),
                                "Your turn is coming up!",
                                String.format("Token %s for %s is about to be served (approx. %d min).",
                                        token.getTokenId(), queue.getServiceName(), estimatedMinutes),
                                queue.getId());
                    }
                    token.setNotificationSent(true);
                }
            }
        }
        queueRepository.save(queue);
    }
}
```

### 6.3 Best Time Notification Scheduler (`BestTimeNotificationScheduler`)

This scheduler runs every hour and notifies users when a queue they follow becomes short (i.e., waiting count drops below a threshold). It respects per‑queue `notifyOnBestTime` preferences.

**Flow**
1. Fetch all active queues (`queueRepository.findByIsActive(true)`).
2. For each queue, calculate current waiting count.
3. If waiting count < `BEST_TIME_THRESHOLD` (currently 3), find all `NotificationPreference` records for that queue where `notifyOnBestTime = true` and `enabled = true`.
4. For each user, check if the last notification for that queue was sent more than 24 hours ago (to avoid spamming).
5. If cooldown passed, send a push notification (only push; email optional) to the user’s FCM tokens.
6. Update `lastBestTimeNotificationSent` in the preference.

**Code Snippet (simplified)**
```java
@Scheduled(cron = "0 0 * * * *") // every hour at minute 0
public void checkBestTimeNotifications() {
    List<Queue> activeQueues = queueRepository.findByIsActive(true);
    for (Queue queue : activeQueues) {
        long waitingCount = queue.getTokens().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .count();
        if (waitingCount >= BEST_TIME_THRESHOLD) continue;

        List<NotificationPreference> prefs = preferenceRepository.findByQueueId(queue.getId()).stream()
                .filter(p -> Boolean.TRUE.equals(p.getNotifyOnBestTime()))
                .filter(p -> Boolean.TRUE.equals(p.getEnabled()))
                .collect(Collectors.toList());

        for (NotificationPreference pref : prefs) {
            LocalDateTime lastSent = pref.getLastBestTimeNotificationSent();
            if (lastSent != null && lastSent.isAfter(LocalDateTime.now().minusHours(24))) continue;

            User user = userRepository.findById(pref.getUserId()).orElse(null);
            if (user != null && user.getFcmTokens() != null && !user.getFcmTokens().isEmpty()) {
                fcmService.sendMulticast(user.getFcmTokens(),
                        "Queue is now short!",
                        String.format("The queue for %s currently has only %d people waiting. Great time to join!",
                                queue.getServiceName(), waitingCount),
                        queue.getId());
                pref.setLastBestTimeNotificationSent(LocalDateTime.now());
                preferenceRepository.save(pref);
            }
        }
    }
}
```

### 6.4 Metrics Scheduler (`MetricsScheduler`)

This scheduler updates Prometheus metrics for each queue. It uses Micrometer’s `Gauge` to expose metrics that can be scraped by Prometheus.

**Metrics exposed:**
- `queue.waiting.tokens` – number of waiting tokens.
- `queue.in.service.tokens` – number of tokens being served.
- `queue.total.served` – total completed tokens (since queue creation).

These metrics are tagged with `queueId`, `placeId`, and `service` for fine‑grained monitoring.

**Code Snippet**
```java
@Service
@RequiredArgsConstructor
public class QueueMetricsService {
    private final QueueRepository queueRepository;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    public void init() {
        updateAllMetrics();
    }

    public void updateAllMetrics() {
        queueRepository.findAll().forEach(queue -> {
            String queueId = queue.getId();
            String placeId = queue.getPlaceId();
            String serviceName = queue.getServiceName();
            Tags tags = Tags.of("queueId", queueId, "placeId", placeId, "service", serviceName);

            long waiting = queue.getTokens().stream().filter(t -> "WAITING".equals(t.getStatus())).count();
            long inService = queue.getTokens().stream().filter(t -> "IN_SERVICE".equals(t.getStatus())).count();
            long totalServed = queue.getTokens().stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();

            // Gauges are created once and then their value is updated.
            Gauge.builder("queue.waiting.tokens", () -> waiting)
                    .tags(tags).register(meterRegistry);
            Gauge.builder("queue.in.service.tokens", () -> inService)
                    .tags(tags).register(meterRegistry);
            Gauge.builder("queue.total.served", () -> totalServed)
                    .tags(tags).register(meterRegistry);
        });
    }
}
```

### 6.5 Queue Analytics Scheduler (`QueueAnalyticsScheduler`)

This scheduler takes an hourly snapshot of waiting counts and stores them in a separate collection (`queue_hourly_stats`). This data is used for the “best time to join” feature and for historical analytics.

**Flow**
1. At the start of each hour, get all queues.
2. For each queue, count waiting tokens.
3. Save a `QueueHourlyStats` document with `queueId`, `hour` (the start of the hour), and `waitingCount`.
4. Delete records older than 60 days to keep the collection manageable.

**Code Snippet**
```java
@Scheduled(cron = "0 0 * * * *") // every hour at minute 0
public void snapshotQueueWaitCounts() {
    LocalDateTime hourStart = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
    List<Queue> allQueues = queueRepository.findAll();
    for (Queue queue : allQueues) {
        long waitingCount = queue.getTokens().stream()
                .filter(t -> "WAITING".equals(t.getStatus()))
                .count();
        QueueHourlyStats stats = new QueueHourlyStats();
        stats.setQueueId(queue.getId());
        stats.setHour(hourStart);
        stats.setWaitingCount((int) waitingCount);
        statsRepository.save(stats);
    }
    // Clean old records
    LocalDateTime cutoff = LocalDateTime.now().minusDays(60);
    statsRepository.deleteByHourBefore(cutoff);
}
```

### 6.6 Alert Scheduler (`AlertScheduler`)

This scheduler runs every 5 minutes and checks for queues whose estimated wait time exceeds the threshold set by the admin. It sends an email alert to the admin’s configured notification email.

**Flow**
1. Fetch all `AlertConfig` documents (each belongs to an admin).
2. For each admin, get all places they own.
3. For each place, get all queues.
4. For each queue, if `estimatedWaitTime > thresholdWaitTime`, compile a message.
5. If any queue exceeded threshold, send an email to the admin’s notification email.

**Code Snippet**
```java
@Scheduled(fixedRate = 300000) // 5 minutes
public void checkThresholds() {
    List<AlertConfig> allConfigs = alertConfigRepository.findAll();
    for (AlertConfig config : allConfigs) {
        if (!config.isEnabled()) continue;
        List<Place> adminPlaces = placeRepository.findByAdminId(config.getAdminId());
        List<String> placeIds = adminPlaces.stream().map(Place::getId).toList();
        List<Queue> queues = queueRepository.findByPlaceIdIn(placeIds);
        StringBuilder alertMessage = new StringBuilder();
        boolean anyExceed = false;
        for (Queue queue : queues) {
            if (queue.getEstimatedWaitTime() > config.getThresholdWaitTime()) {
                anyExceed = true;
                alertMessage.append("Queue: ").append(queue.getServiceName())
                        .append(" – Wait time: ").append(queue.getEstimatedWaitTime()).append(" min\n");
            }
        }
        if (anyExceed) {
            emailService.sendAlertEmail(config.getNotificationEmail(), alertMessage.toString());
        }
    }
}
```

### 6.7 Push Notifications (FCM)

**Frontend Registration**
- On login (or when permissions are granted), the frontend requests an FCM token using `getToken(messaging, { vapidKey })`.
- It then sends the token to the backend via `POST /api/user/fcm-token?token=...`.
- The backend stores the token in the user’s `fcmTokens` list.

**Sending Push Notifications**
- `FcmService.sendMulticast(List<String> tokens, String title, String body, String queueId)` builds a `MulticastMessage` and sends it via Firebase Admin SDK.
- It handles errors (e.g., invalid tokens) by logging but not removing them (could be improved).

**Notification Handling in Frontend**
- The service worker (`firebase-messaging-sw.js`) listens for background messages and shows a system notification.
- When the user clicks the notification, the app opens and navigates to the relevant queue page (if `queueId` is provided in the data payload).

### 6.8 Email Notifications

- `EmailService` uses JavaMail with a Gmail SMTP server (or any other).
- Templates (HTML) are stored in `src/main/resources/templates/`.
- Methods: `sendVerificationOtpEmail`, `sendOtpEmail` (for password reset), `sendUpcomingTokenEmail`, `sendAlertEmail`, `sendPasswordResetLink` (for admin‑initiated reset).

### 6.9 Interview Questions & Answers

1. **How do you avoid sending duplicate notifications for the same token?**  
   Each token has a `notificationSent` flag. Once a notification is sent, we set it to `true`. The scheduler checks this flag and skips already notified tokens.

2. **What happens if a user joins a queue and then the token is served before the notification scheduler runs?**  
   The scheduler checks only waiting tokens. If the token is already served, it’s no longer waiting, so no notification is sent. That’s fine.

3. **How do you handle FCM token invalidation?**  
   If the FCM service returns an error indicating an invalid token, we could remove it from the user’s list. Currently, we just log the error, but it could be improved.

4. **Why do you send notifications from a scheduled job instead of immediately when the token is added?**  
   We need to calculate the position and wait time. The position changes as other tokens are served or added, so we compute it just before the notification time.

5. **How do you ensure that the best time notification doesn’t spam the user?**  
   We check `lastBestTimeNotificationSent` and only send if it was more than 24 hours ago.

6. **How do you handle a large number of queues and tokens in the notification scheduler?**  
   The scheduler iterates over all queues and all waiting tokens. If the system grows, we might need to optimise by only checking queues with waiting tokens and using pagination. But currently it’s acceptable.

7. **What is the purpose of the metrics scheduler?**  
   It exposes custom metrics for monitoring. For example, we can graph the waiting token count over time in Grafana.

8. **How do you test the schedulers?**  
   We can mock the repositories and services and use `@SpringBootTest` with a test profile that runs the schedulers. We can also test the logic by calling the methods directly in unit tests.

9. **How do you configure the schedule intervals?**  
   Using `@Scheduled` with `cron` expressions or `fixedRate`. The values are hardcoded but can be moved to `application.properties` if needed.

10. **How does the alert scheduler know which email to send alerts to?**  
    Each admin can configure their own notification email (or use their own email) via the `/api/admin/alert-config` endpoint. The configuration is stored in the `alert_configs` collection.

### 6.10 Diagram 

**Token Notification Scheduler Flow (Mermaid)**
```mermaid
graph TD
    A[Start every minute] --> B[Get all queues]
    B --> C[For each queue, get waiting tokens]
    C --> D[Sort by priority + issue time]
    D --> E[For each token, estimate minutes until turn]
    E --> F{minutes <= threshold?}
    F -->|Yes| G[Check user preferences]
    G --> H{Email enabled?}
    H -->|Yes| I[Send email]
    H -->|No| J{Push enabled?}
    J -->|Yes| K[Send push notification]
    J -->|No| L[Mark token as notified]
    F -->|No| M[Skip]
    L --> N[Save queue]
    M --> N
```

**Notification Preferences Lookup**
```mermaid
sequenceDiagram
    participant Scheduler as TokenNotificationScheduler
    participant PrefRepo as NotificationPreferenceRepository
    participant UserRepo as UserRepository
    participant EmailService
    participant FcmService

    Scheduler->>PrefRepo: findByQueueId(queueId)
    PrefRepo-->>Scheduler: Map<userId, NotificationPreference>
    Scheduler->>UserRepo: findById(userId)
    UserRepo-->>Scheduler: User (with FCM tokens)
    alt email enabled
        Scheduler->>EmailService: sendUpcomingTokenEmail()
    end
    alt push enabled and tokens exist
        Scheduler->>FcmService: sendMulticast()
    end
```

---

## 7. Payments & Token Generation

QueueLess uses Razorpay to handle payments for admin and provider tokens. This section explains the payment flow, token generation, and how tokens are validated during registration.

### 7.1 Payment Flow Overview

**Admin Token Purchase (Self)**
1. Admin selects a plan (1 month, 1 year, lifetime) on the pricing page.
2. Frontend sends `POST /api/payment/create-order` with email, role=ADMIN, tokenType.
3. Backend creates a Razorpay order, stores a `Payment` record with `isPaid=false`, and returns order details.
4. Frontend opens Razorpay checkout, user completes payment.
5. On success, frontend calls `POST /api/payment/confirm` with orderId, paymentId, email, tokenType.
6. Backend verifies payment (by checking the order exists and is unpaid, then marks as paid), generates an admin token (a random string like `ADMIN-xxxx`), stores it in the `tokens` collection with expiry date, and returns the token value.
7. User receives the token and uses it during registration.

**Provider Token Purchase (Admin buys for others)**
- Similar flow, but the admin buys a provider token for a specific email (or multiple emails in bulk).
- The order creation includes `adminId` (the buyer) in the notes.
- After payment confirmation, the backend extracts `adminId` from the order notes and generates a provider token with `createdByAdminId` set.
- The provider later uses the token during registration.

### 7.2 Payment Service (`PaymentService`)

**Order Creation** (`createOrder`)
- Validates the token type and role.
- Calls Razorpay API to create an order.
- Saves a `Payment` document with order ID, amount, email, role, and `isPaid=false`.
- If role is PROVIDER and adminId is provided, stores `createdByAdminId`.
- Returns the Razorpay order object (containing `id`, `amount`, `currency`).

**Payment Confirmation** (`confirmPayment`)
- Finds the `Payment` by `razorpayOrderId`.
- Updates `razorpayPaymentId` and sets `isPaid=true`.
- Logs an audit event (`PAYMENT_COMPLETED`).

**Token Generation** (`generateToken`)
- Based on tokenType, calculates expiry (1 month, 1 year, or 100 years).
- Creates a unique token string: `ADMIN-<random>` or `PROVIDER-<random>`.
- Saves a `Token` entity with token value, role, createdForEmail, expiry, and optionally `createdByAdminId` (for provider tokens).
- Returns the `Token`.

**Extract Admin ID from Order** (`getAdminIdFromOrder`)
- Fetches the Razorpay order and reads the `notes.adminId` (set during order creation).
- Used in the provider token confirmation flow to know which admin purchased the token.

### 7.3 Payment Controller (`PaymentController`)

- `POST /create-order` – calls `paymentService.createOrder`, returns `OrderResponse`.
- `POST /confirm` – for admin tokens, confirms payment, generates token.
- `POST /confirm-provider` – for a single provider token, extracts adminId from order, generates token.
- `POST /confirm-provider-bulk` – for multiple provider tokens, takes a list of emails, generates a token for each (adminId taken from authenticated user).

### 7.4 Token Storage (`Token` entity)

```java
@Document("tokens")
public class Token {
    @Id
    private String id;
    private String tokenValue;       // e.g., ADMIN-abc123
    private Role role;               // ADMIN or PROVIDER
    private boolean isUsed;          // used during registration
    private LocalDateTime expiryDate;
    private String createdForEmail;  // the email that can use this token
    private boolean isProviderToken;
    private String providerEmail;    // if isProviderToken, the email it belongs to
    private LocalDateTime createdAt;
    private String createdByAdminId; // which admin purchased this provider token
}
```

### 7.5 Token Validation in Registration (`AuthService.register`)

When registering as ADMIN or PROVIDER, the user must supply a token.

- `AuthService` checks if the token exists in the `Token` collection.
- Verifies it hasn't been used (`isUsed == false`), not expired, matches the requested role, and matches the email (if applicable).
- If valid, marks token as `used` and proceeds with user creation.

**Code Snippet (simplified)**
```java
if (request.getRole() == Role.ADMIN || request.getRole() == Role.PROVIDER) {
    Token token = tokenRepository.findByTokenValue(request.getToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid token"));
    if (token.isUsed()) throw new InvalidTokenException("Token already used");
    if (token.getExpiryDate().isBefore(LocalDateTime.now()))
        throw new InvalidTokenException("Token expired");
    if (!token.getRole().equals(request.getRole()))
        throw new InvalidTokenException("Token role mismatch");
    if (!token.getCreatedForEmail().equals(request.getEmail()))
        throw new InvalidTokenException("Token tied to different email");
    token.setUsed(true);
    tokenRepository.save(token);
}
// After token validation, user is created (with isVerified = false)
```

### 7.6 Security Considerations

- **Order Notes**: Admin ID is stored in Razorpay order notes; during confirmation, we fetch it and use it to set `createdByAdminId` on the provider token. This prevents a malicious user from claiming a token purchased by someone else.
- **Payment Verification**: We don’t rely solely on frontend callback; we also verify that the payment is actually completed by checking the payment ID and ensuring it's not already used. However, we do not call Razorpay to verify the payment again (the frontend callback is trusted within our system). In production, you might want to add a webhook to verify payments.
- **Token Expiry**: Tokens have an expiry date; after that, they cannot be used.
- **Used Token Prevention**: Once used, a token cannot be used again.

### 7.7 Error Handling

- **Payment not found**: `confirmPayment` throws `RuntimeException`, caught by global handler, returns 404.
- **Invalid token type**: `createOrder` throws `IllegalArgumentException` → 400.
- **Razorpay API error**: `createOrder` throws `RazorpayException` → 500.
- **Admin ID extraction failure**: `getAdminIdFromOrder` throws `RuntimeException` → 500.

### 7.8 Frontend Integration

- Uses Razorpay's checkout.js.
- On order creation, the frontend opens a Razorpay modal with the order details.
- After successful payment, the frontend calls the `/confirm` endpoint with the payment details.
- The frontend stores the returned token (or displays it) and redirects to registration.

### 7.9 Interview Questions & Answers

1. **How do you prevent a user from using the same token twice?**  
   Each token has an `isUsed` flag. After it is used during registration, we set it to `true`. Any subsequent attempt will fail.

2. **What happens if the payment is successful but the token generation fails?**  
   The payment record is already marked as paid. We would need to manually intervene or retry token generation. In practice, this is unlikely because token generation is simple and does not involve external calls. If it did, we could use a transaction or a retry mechanism.

3. **How do you ensure that the admin ID stored in the order is correct?**  
   During order creation, the authenticated admin’s ID is passed as `adminId` and stored in the Razorpay order notes. When confirming the payment, we fetch the order and extract that adminId. This prevents another user from hijacking the token.

4. **Why do you need to store `createdByAdminId` on the provider token?**  
   It allows the admin to see which providers they have purchased tokens for (visible in the admin dashboard). It also adds accountability.

5. **How do you handle the case where a user tries to register with a token that is not yet paid?**  
   The token is only generated after payment confirmation, so it cannot exist before payment. The registration flow only checks for the token; if it's not there, registration fails.

6. **Do you support refunds?**  
   Currently, no. This would require integrating Razorpay’s refund API and updating the token status. It could be added later.

7. **How do you generate the token string?**  
   `ADMIN-` + `UUID.randomUUID().toString().replace("-", "").substring(0,12)`. This ensures uniqueness.

8. **How do you test the payment flow without real money?**  
   Razorpay provides a test mode with test API keys. We also mock the Razorpay client in unit tests to avoid making real HTTP calls.

9. **What happens if the Razorpay order creation fails?**  
   The error is caught and returned as a 500. The frontend shows an error to the user.

10. **How do you ensure that the email used for the token matches the registration email?**  
    The token is created for a specific email (`createdForEmail`). During registration, we check that the email in the request matches this field. This prevents a token from being used by a different person.

### 7.10 Diagram Prompts

**Payment Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant Admin
    participant Frontend
    participant Backend
    participant Razorpay
    participant DB

    Admin->>Frontend: Choose plan, enter email
    Frontend->>Backend: POST /create-order (email, role, tokenType)
    Backend->>Razorpay: orders.create()
    Razorpay-->>Backend: order details
    Backend->>DB: save Payment (isPaid=false)
    Backend-->>Frontend: orderId, amount, currency
    Frontend->>Razorpay: open checkout
    Admin->>Razorpay: complete payment
    Razorpay-->>Frontend: payment success
    Frontend->>Backend: POST /confirm (orderId, paymentId, email, tokenType)
    Backend->>DB: find Payment by orderId
    Backend->>DB: update Payment isPaid=true
    Backend->>DB: generate and save Token
    Backend-->>Frontend: tokenValue
    Frontend-->>Admin: token displayed
    Admin->>Frontend: register with token
```

**Token Validation During Registration (Mermaid)**
```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'primaryTextColor': '#FFFFFF', 'lineColor': '#B0BEC5', 'edgeLabelBackground':'#111827'}}}%%
graph TD
    %% Global Diagram Title - Clean text, no markdown
    titleD("USER REGISTRATION FLOW WITH TOKEN VALIDATION (DARK MODE)"):::titleNodeD
    style titleD fill:none,stroke:none

    %% --- Node Definitions - Clean Multiline Text, No Special Characters ---
    %% Start/Final Action T1 (Bright Blue)
    User_A["User submits\nregistration request"]:::actionNodeD
    Final_C["Create user\nsend OTP"]:::actionNodeD
    TokenFinal_N["Mark token used\ncreate user, set isVerified to false"]:::actionNodeD

    %% Decisions T2 (Bright Orange, Diamond)
    RoleDecis_B{"role ==\nADMIN/PROVIDER?"}:::decisionNodeD
    IsUsedDecis_F{"isUsed?"}:::decisionNodeD
    ExpiredDecis_H{"expired?"}:::decisionNodeD
    RoleMatchDecis_J{"role matches?"}:::decisionNodeD
    EmailMatchDecis_L{"email matches?"}:::decisionNodeD

    %% Lookup T3 (Bright Green)
    FindToken_D["Find Token\nby tokenValue"]:::lookupNodeD

    %% Errors/Exceptions T4 (High-Visibility Red)
    Error_E["InvalidTokenException"]:::errorNodeD
    Error_G["Token already used"]:::errorNodeD
    Error_I["Token expired"]:::errorNodeD
    Error_K["Role mismatch"]:::errorNodeD
    Error_M["Email mismatch"]:::errorNodeD

    %% --- Defining High-Visibility Dark-Mode Styles ---
    classDef titleNodeD font-size:20px,font-weight:bold,color:#E1F5FE,fill:none,stroke:none;
    %% Bright Blue for Start/Action T1
    classDef actionNodeD fill:#0D47A1,stroke:#40C4FF,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;
    %% Bright Orange for Decision T2 (Diamond)
    classDef decisionNodeD fill:#FF8F00,stroke:#FFCA28,stroke-width:2px,color:#FFFFFF,font-weight:600;
    %% Bright Green for Lookup T3
    classDef lookupNodeD fill:#1B5E20,stroke:#A5D6A7,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;
    %% High-Visibility Red for Error/Exception T4
    classDef errorNodeD fill:#B71C1C,stroke:#EF9A9A,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;

    %% --- Logical Connections ---
    User_A --> RoleDecis_B
    RoleDecis_B -->|No| Final_C
    RoleDecis_B -->|Yes| FindToken_D
    FindToken_D -->|Not found| Error_E
    FindToken_D -->|Found| IsUsedDecis_F
    IsUsedDecis_F -->|Yes| Error_G
    IsUsedDecis_F -->|No| ExpiredDecis_H
    ExpiredDecis_H -->|Yes| Error_I
    ExpiredDecis_H -->|No| RoleMatchDecis_J
    RoleMatchDecis_J -->|No| Error_K
    RoleMatchDecis_J -->|Yes| EmailMatchDecis_L
    EmailMatchDecis_L -->|No| Error_M
    EmailMatchDecis_L -->|Yes| TokenFinal_N
    %% Connect the admin flow back to the final user creation
    TokenFinal_N --> Final_C
```

---
## 8. Search & Discovery

QueueLess provides a powerful search engine that allows users to find places, services, and queues based on various criteria. This section explains how the search works, from the frontend filters to the MongoDB queries that power it.

### 8.1 Overview of Search Capabilities

Users can search for:
- **Places** – by name, type, rating, location (nearby), active status.
- **Services** – by name, description, features (supports group token, emergency support), active status.
- **Queues** – by service name, wait time, features, active status.

Search results are paginated and can be sorted by name, rating, or wait time. Filters can be combined to narrow down results.

### 8.2 Backend Implementation

#### 8.2.1 `SearchController`

The controller exposes three main endpoints:

- `POST /api/search/comprehensive` – performs a full search across places, services, and queues with pagination and sorting.
- `POST /api/search/nearby` – searches for places near a given location, with filters.
- `GET /api/search/filter-options` – returns available filter values (place types, wait time ranges).
- `POST /api/search/statistics` – returns aggregate statistics (e.g., total places, average ratings) for the search query.
- `GET /api/search/quick/{query}` – a simple quick search with a limit.

**Comprehensive Search Request (example)**
```json
{
  "query": "coffee",
  "placeType": "CAFE",
  "minRating": 4.0,
  "maxWaitTime": 15,
  "supportsGroupToken": true,
  "emergencySupport": false,
  "isActive": true,
  "longitude": 77.5946,
  "latitude": 12.9716,
  "radius": 5,
  "searchPlaces": true,
  "searchServices": true,
  "searchQueues": true,
  "sortBy": "rating",
  "sortDirection": "desc"
}
```

#### 8.2.2 `SearchService` – Core Logic

The service uses MongoDB’s aggregation framework and geospatial queries to fetch results. It returns a `SearchResultDTO` containing separate lists for places, services, and queues, along with pagination metadata.

**Key Steps in Comprehensive Search**
1. **Place Search** – Build a `Criteria` based on filters (name regex, type, minRating, isActive, location if provided). Execute a `Query` with pagination and sorting. Also perform a separate count query for total places.
2. **Service Search** – Use the place IDs obtained from the place search (if place search was performed) to restrict services to those places, or search by name/description if no place filter. Apply other filters (supportsGroupToken, emergencySupport, isActive). Paginate and count.
3. **Queue Search** – Use place IDs from place search (if available) or search by serviceName. Apply filters (maxWaitTime, supportsGroupToken, emergencySupport, isActive). Paginate and count.
4. **Enrich Queue Results** – For each queue, fetch the associated place details (name, address, rating) to populate `EnhancedQueueDTO`.

**Geospatial Query for Nearby Places**
```java
@Query("{ 'location' : { $near : { $geometry : { type : 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
List<Place> findByLocationNear(Double longitude, Double latitude, Double maxDistance);
```

**Search with Filters (Example)**
```java
Query query = new Query();
List<Criteria> criteriaList = new ArrayList<>();
if (request.getQuery() != null) {
    criteriaList.add(Criteria.where("name").regex(request.getQuery(), "i"));
}
if (request.getPlaceType() != null) {
    criteriaList.add(Criteria.where("type").is(request.getPlaceType()));
}
if (request.getMinRating() != null && request.getMinRating() > 0) {
    criteriaList.add(Criteria.where("rating").gte(request.getMinRating()));
}
if (request.getIsActive() != null) {
    criteriaList.add(Criteria.where("isActive").is(request.getIsActive()));
}
if (request.getLongitude() != null && request.getLatitude() != null) {
    criteriaList.add(Criteria.where("location").nearSphere(
        new Point(request.getLongitude(), request.getLatitude()))
        .maxDistance(request.getRadius() * 1000)); // convert km to meters
}
if (!criteriaList.isEmpty()) {
    query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
}
// Add pagination and sorting
query.with(pageable);
List<Place> places = mongoTemplate.find(query, Place.class);
long total = mongoTemplate.count(query.skip(-1).limit(-1), Place.class); // count without pagination
```

**Service Search Using Aggregation (to join with places)**
```java
Aggregation agg = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("placeId").in(placeIds)),
    Aggregation.match(Criteria.where("name").regex(request.getQuery(), "i")),
    Aggregation.lookup("places", "placeId", "_id", "place"),
    Aggregation.unwind("place"),
    Aggregation.project()
        .and("_id").as("id")
        .and("name").as("name")
        .and("description").as("description")
        .and("averageServiceTime").as("averageServiceTime")
        .and("supportsGroupToken").as("supportsGroupToken")
        .and("emergencySupport").as("emergencySupport")
        .and("isActive").as("isActive")
        .and("place.name").as("placeName")
        .and("place.address").as("placeAddress")
        .and("place.rating").as("placeRating"),
    Aggregation.sort(Sort.by(Sort.Direction.ASC, "name")),
    Aggregation.skip(pageable.getOffset()),
    Aggregation.limit(pageable.getPageSize())
);
AggregationResults<ServiceSearchResult> results = mongoTemplate.aggregate(agg, "services", ServiceSearchResult.class);
```

**Queue Search with Enrichment**
```java
List<Queue> queues = mongoTemplate.find(query, Queue.class);
List<String> placeIds = queues.stream().map(Queue::getPlaceId).distinct().collect(Collectors.toList());
Map<String, Place> placeMap = placeService.getPlacesByIds(placeIds).stream()
        .collect(Collectors.toMap(Place::getId, Function.identity()));
List<EnhancedQueueDTO> enhancedQueues = queues.stream().map(queue -> {
    EnhancedQueueDTO dto = EnhancedQueueDTO.fromQueue(queue);
    Place place = placeMap.get(queue.getPlaceId());
    if (place != null) {
        dto.setPlaceName(place.getName());
        dto.setPlaceAddress(place.getAddress());
        dto.setPlaceRating(place.getRating());
    }
    return dto;
}).collect(Collectors.toList());
```

### 8.3 Filters & Sorting

- **Place Types** – Dynamic list from MongoDB (distinct types of existing places).
- **Wait Time Ranges** – Predefined ranges (under 15 min, 15–30 min, 30–60 min, over 60 min).
- **Rating** – Minimum rating (0–5, step 0.5).
- **Features** – Supports group tokens, emergency support.
- **Active Only** – Filter out inactive places/services/queues.
- **Sorting** – By name, rating, or wait time, each ascending or descending.

### 8.4 Pagination

All search results are paginated using Spring’s `Pageable`. The frontend can load more results as the user scrolls or clicks a “Load More” button.

### 8.5 Performance Considerations

- **Indexes**: Critical indexes are in place (e.g., `placeId` on services, `placeId` on queues, `location` 2dsphere on places, `rating` on places).
- **Caching**: Place lists and service lists are cached via Redis to reduce database load.
- **Geospatial Queries**: Use 2dsphere indexes for efficient location‑based searches.
- **Aggregation**: The service search uses `$lookup` to join with places, which can be expensive; we cache the result or limit the number of results.

### 8.6 Code Snippets

**Comprehensive Search (simplified)**
```java
public SearchResultDTO comprehensiveSearch(SearchRequestDTO request, Pageable pageable) {
    SearchResultDTO result = new SearchResultDTO();
    List<String> placeIds = new ArrayList<>();

    // Places
    if (request.isSearchPlaces()) {
        Query placeQuery = buildPlaceQuery(request);
        List<Place> places = mongoTemplate.find(placeQuery.with(pageable), Place.class);
        long total = mongoTemplate.count(placeQuery, Place.class);
        result.setPlaces(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
        result.setTotalPlaces(total);
        placeIds = places.stream().map(Place::getId).collect(Collectors.toList());
    }

    // Services
    if (request.isSearchServices()) {
        Query serviceQuery = buildServiceQuery(request, placeIds);
        List<Service> services = mongoTemplate.find(serviceQuery.with(pageable), Service.class);
        long total = mongoTemplate.count(serviceQuery, Service.class);
        result.setServices(services.stream().map(ServiceDTO::fromEntity).collect(Collectors.toList()));
        result.setTotalServices(total);
    }

    // Queues
    if (request.isSearchQueues()) {
        Query queueQuery = buildQueueQuery(request, placeIds);
        List<Queue> queues = mongoTemplate.find(queueQuery.with(pageable), Queue.class);
        long total = mongoTemplate.count(queueQuery, Queue.class);
        // Enrich with place details
        List<EnhancedQueueDTO> enhanced = enrichQueues(queues);
        result.setQueues(enhanced);
        result.setTotalQueues(total);
    }

    return result;
}
```

### 8.7 Interview Questions & Answers

1. **How do you implement the “nearby” search?**  
   We use MongoDB’s `$near` geospatial query with a 2dsphere index on the `location` field. The frontend passes longitude, latitude, and radius (in km). We convert radius to meters and use `$maxDistance` to restrict results.

2. **How do you handle searching across three different collections (places, services, queues)?**  
   We execute separate queries for each type, using common filters (e.g., query string, location, rating). This allows pagination per type. The results are combined in a single DTO.

3. **How do you ensure that the service search only returns services from places that match the user’s search?**  
   If the user performed a place search, we collect the `placeIds` from the place results and use them to filter services. If no place search was performed, we search services by name/description directly.

4. **What is the purpose of the `$lookup` in the service search?**  
   `$lookup` joins service documents with place documents so we can include place name and address in the service results without extra queries. However, we also do a separate place lookup for queues.

5. **How do you handle performance when searching large datasets?**  
   We rely on indexes (e.g., `name` text index, `location` 2dsphere, `rating`). We also use pagination to limit results. For the comprehensive search, we execute queries in parallel using `CompletableFuture` (though not implemented, it could be).

6. **How do you handle the “quick search” endpoint?**  
   It’s a simple wrapper around the comprehensive search with a small page size and default filters, returning only the first few results.

7. **What are the available filter options and how do you populate them?**  
   Place types are retrieved by querying distinct `type` values from the places collection. Wait time ranges are hardcoded in the frontend (but could be dynamic). The frontend calls `/api/search/filter-options` to get these lists.

8. **How do you handle sorting by wait time for queues?**  
   The queue documents already have an `estimatedWaitTime` field, which is updated periodically. We sort by that field in the query.

9. **How do you handle the case where a place has a high rating but no feedback?**  
   Rating is stored on the place document and updated when new feedback is submitted. If there are no feedbacks, the rating remains 0.0. The user can still filter by rating, but places with 0 rating will not appear if `minRating` > 0.

10. **How do you handle location‑based searches when the user doesn’t provide coordinates?**  
    If `longitude` and `latitude` are not provided, the location filter is omitted from the query. The search then uses other filters (name, type, rating) only.

11. **What is the `SearchStatisticsDTO` used for?**  
    It provides aggregated information about the search results, such as total number of places, services, queues, average wait time, and average rating. This can be displayed on the frontend as a summary.

12. **How do you test the search functionality?**  
    Unit tests for the service mock MongoDB templates and verify the generated queries. Integration tests with embedded MongoDB test actual queries.

### 8.8 Diagram Prompts

**Comprehensive Search Flow (Mermaid)**
```mermaid
graph TD
    A[User submits search request] --> B[Controller]
    B --> C[SearchService.comprehensiveSearch]
    C --> D{searchPlaces?}
    D -->|Yes| E[Build place query with filters]
    E --> F[Execute place query + count]
    F --> G[Collect placeIds]
    D -->|No| G
    G --> H{searchServices?}
    H -->|Yes| I[Build service query using placeIds]
    I --> J[Execute service query + count]
    H -->|No| K{searchQueues?}
    K -->|Yes| L[Build queue query using placeIds]
    L --> M[Execute queue query + count]
    M --> N[Enrich queues with place details]
    K -->|No| O[Combine results into SearchResultDTO]
    N --> O
    O --> P[Return to controller]
    P --> Q[Return JSON to frontend]
```

**Geospatial Search (Mermaid)**
```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'primaryTextColor': '#FFFFFF', 'lineColor': '#B0BEC5', 'edgeLabelBackground':'#111827', 'noteBkgColor': '#FF8F00', 'noteTextColor': '#FFFFFF', 'noteBorderColor': '#FFCA28'}}}%%
sequenceDiagram
    autonumber
    
    %% Standard Participants without complex styling
    participant F as Frontend
    participant B as Backend API
    participant M as MongoDB

    %% Clear flow messages using simple text
    F->>B: POST /search/nearby (lon, lat, radius)
    activate B
    
    %% Use a standard Note to handle the complex calc logic
    Note right of B: Calculate maxDistance =\nradius * 1000 meters
    
    %% Clear, single-line message summarizing the query
    B->>M: Execute geospatial query: db.places.find({$near})
    activate M
    M-->>B: Return list of places
    deactivate M
    
    B-->>F: Return places as JSON list
    deactivate B
```

---
## 9. Frontend Architecture & Key Components

The QueueLess frontend is a single‑page application built with React, Vite, Redux Toolkit, and React Bootstrap. It communicates with the backend via REST API and WebSocket (STOMP). This section explains the frontend structure, state management, and key components.

### 9.1 Project Structure

```
src/
├── assets/               – static assets (images, fonts)
├── components/           – reusable UI components
│   ├── AdminPlaces.jsx
│   ├── AdvancedSearch.jsx
│   ├── AuthFormWrapper.jsx
│   ├── BestTimeToJoin.jsx
│   ├── CancelTokenModal.jsx
│   ├── CompletedTokensList.jsx
│   ├── CustomerQueue.jsx
│   ├── EmergencyApprovalModal.jsx
│   ├── ErrorBoundary.jsx
│   ├── ExportHistory.jsx
│   ├── ExportReportModal.jsx
│   ├── FavoriteButton.jsx
│   ├── FeedbackForm.jsx
│   ├── FeedbackHistory.jsx
│   ├── FeedbackPrompt.jsx
│   ├── Footer.jsx
│   ├── Navbar.jsx
│   ├── NotificationModal.jsx
│   ├── OtpInput.jsx
│   ├── PlaceDetail.jsx
│   ├── PlaceForm.jsx
│   ├── PlaceList.jsx
│   ├── PlaceListSkeleton.jsx
│   ├── PlaceMap.jsx
│   ├── PlaceRatingSummary.jsx
│   ├── ProtectedRoute.jsx
│   ├── ProviderLeaderboard.jsx
│   ├── ProviderQueueManagement.jsx
│   ├── QRCodeModal.jsx
│   ├── QRScannerModal.jsx
│   ├── QueueList.jsx
│   ├── QueuesTableSkeleton.jsx
│   ├── RatingDisplay.jsx
│   ├── ResetQueueModal.jsx
│   ├── SearchResults.jsx
│   ├── SearchResultsSkeleton.jsx
│   ├── ServiceManagement.jsx
│   ├── TokenHistorySkeleton.jsx
│   ├── TokenVolumeChart.jsx
│   ├── UserDashboardSkeleton.jsx
│   ├── UserDetailsModal.jsx
│   ├── UserQueueRestriction.jsx
│   ├── UserTokenHistoryChart.jsx
│   ├── UserTokenHistoryList.jsx
│   └── ...
├── hooks/                – custom hooks (useCountUp, useFcmToken, useInView)
├── pages/                – top‑level pages (AdminDashboard, Home, Login, etc.)
├── redux/                – Redux slices
│   ├── adminAnalyticsSlice.js
│   ├── authSlice.js
│   ├── placeSlice.js
│   ├── providerAnalyticsSlice.js
│   ├── queue/queueSlice.js
│   ├── searchSlice.js
│   ├── serviceSlice.js
│   ├── userAnalyticsSlice.js
│   ├── userSlice.js
│   └── userTokenHistorySlice.js
├── services/             – API service modules (authService, adminService, etc.)
├── utils/                – helpers (axiosInstance, loadScript, normalizeQueue, tokenUtils)
├── firebase.js           – Firebase configuration and messaging setup
├── App.jsx               – main app component with routing
├── main.jsx              – entry point
└── index.css             – global styles
```

### 9.2 Routing

Routing is handled by React Router v6. Public routes are accessible without authentication, while protected routes use the `ProtectedRoute` component that checks `token` and `role`.

**Key routes** (from `App.jsx`):
- `/` – Home
- `/login`, `/register`, `/forgot-password`, `/verify-otp`, `/reset-password`, `/reset-password-token`
- `/places` – list of places
- `/places/:id` – place details
- `/customer/queue/:queueId` – queue page for users
- `/search` – advanced search
- `/favorites` – user’s favorite places (protected)
- `/profile` – user profile (protected)
- `/user/dashboard` – user dashboard (protected)
- `/user/notifications` – notification preferences (protected)
- `/provider/queues` – provider queue list (protected)
- `/provider/dashboard/:queueId` – provider dashboard (protected)
- `/admin/dashboard` – admin dashboard (protected)
- `/admin/places` – manage places (protected)
- `/admin/places/:placeId/services` – manage services (protected)
- `/admin/providers/:providerId` – provider details (protected)
- `/provider-pricing` – buy provider tokens (protected for admins)
- `/pricing` – buy admin tokens (public)
- `*` – NotFound page

### 9.3 State Management (Redux Toolkit)

Redux is used for global state that is shared across many components. The main slices:

- **auth** – stores token, role, user ID, name, phone, profile image, preferences, etc. Actions: `loginSuccess`, `logout`, `updateProfile`, `updatePreferences`, `toggleDarkMode`.
- **queue** – holds the currently viewed queue (from WebSocket updates) and connection status.
- **places** – list of places, current place, pagination state.
- **services** – list of services, grouped by placeId.
- **search** – search filters, results, pagination metadata.
- **user** – favorite places and other user‑specific data.
- **adminAnalytics** – token volume and busiest hours data.
- **providerAnalytics** – analytics for providers.
- **userAnalytics** – user token history chart data.
- **userTokenHistory** – paginated list of user’s past tokens.

**Example: `authSlice`**
```javascript
const authSlice = createSlice({
  name: 'auth',
  initialState: getInitialState(),
  reducers: {
    loginSuccess: (state, action) => {
      state.token = action.payload.token;
      state.role = action.payload.role;
      state.id = action.payload.userId;
      state.name = action.payload.name;
      state.phoneNumber = action.payload.phoneNumber;
      state.profileImageUrl = action.payload.profileImageUrl;
      // ... store in localStorage
    },
    logout: (state) => {
      // clear all auth data
    },
    updateProfile: (state, action) => {
      // update name, phone, profileImageUrl
    },
    updatePreferences: (state, action) => {
      state.preferences = { ...state.preferences, ...action.payload };
      localStorage.setItem('preferences', JSON.stringify(state.preferences));
    },
    toggleDarkMode: (state) => {
      state.preferences.darkMode = !state.preferences.darkMode;
      localStorage.setItem('preferences', JSON.stringify(state.preferences));
    },
  },
});
```

### 9.4 WebSocket Integration

The `WebSocketService` singleton (in `services/websocketService.js`) manages the STOMP connection, subscriptions, and message sending.

**Connection**
- On login, `WebSocketService.connect()` is called (from `App.jsx` effect).
- It uses `SockJS` and STOMP, sending the JWT in the CONNECT frame.
- On connection success, it subscribes to user‑specific topics (`/user/queue/emergency-approved`, `/user/queue/token-cancelled`) and optionally to queue topics.

**Subscribing to a queue** (e.g., in `CustomerQueue.jsx`)
- When the component mounts, it calls `WebSocketService.subscribeToQueue(queueId)`.
- The subscription receives queue updates and dispatches `updateQueue` Redux action.
- On unmount, it calls `WebSocketService.unsubscribeFromQueue(queueId)`.

**Sending commands**
- `WebSocketService.sendMessage(destination, body)` – used to serve next token, add token, etc.

**Handling user notifications**
- When an emergency approval or token cancellation is received, a custom event is dispatched, and a modal is shown (via `NotificationModal`).

### 9.5 Key Components

**Navbar** – shows different buttons based on role, includes profile dropdown with image, logout, dark mode toggle. Uses `profileImageUrl` from Redux and constructs full URL for uploaded images.

**CustomerQueue** – the main page for users to join a queue, see their status, position, estimated wait time. It uses WebSocket to receive real‑time updates. It also includes forms for regular/group/emergency tokens and the user details modal.

**ProviderDashboard** – displays the queue for a specific provider, with tabs for dashboard and analytics. It includes the `QueueList` component (with drag‑and‑drop reordering), completed tokens list, and emergency approval modal.

**AdminDashboard** – shows stats cards, tabs for queues, payments, providers, analytics, and a map. It fetches data from `/admin/stats`, `/admin/providers`, `/admin/queues/enhanced`, etc.

**SearchResults** – displays search results (places, services, queues) with infinite scroll (load more). It uses the Redux search state and dispatches `performSearch` actions.

**PlaceForm** – used for creating/editing places; includes fields for name, type, address, location (longitude/latitude), images, business hours, etc.

**UserProfile** – allows users to update name, phone, change password, upload profile image, manage preferences. Includes a file input for image upload and a modal for selecting from premium avatars.

### 9.6 API Calls

All API calls go through `axiosInstance` (in `utils/axiosInstance.js`), which automatically adds the JWT token to the `Authorization` header and handles 401 responses (redirect to login). Specific service modules (e.g., `authService`, `placeService`) use this instance.

### 9.7 Notifications & FCM

- **FCM setup**: `firebase.js` initialises Firebase and exports `messaging`. `useFcmToken` hook requests permission, gets the token, and registers it with the backend via `/user/fcm-token`.
- **Service worker**: `firebase-messaging-sw.js` listens for background messages and shows notifications. When the user clicks the notification, it opens the queue page.

### 9.8 Styling & Dark Mode

- Global CSS variables in `index.css` define light and dark mode styles. The `dark-mode` class is applied to `body` based on `preferences.darkMode` in Redux.
- React Bootstrap components are used for layout and basic styling; custom CSS files override or extend them.

### 9.9 Interview Questions & Answers

1. **Why did you choose React for the frontend?**  
   React’s component‑based architecture, large ecosystem, and excellent performance for real‑time updates made it a good fit. Vite provides fast builds and hot module replacement.

2. **How do you manage global state?**  
   We use Redux Toolkit for global state (authentication, search results, user preferences). For local component state, we use React hooks (`useState`, `useReducer`). Redux helps avoid prop drilling and keeps state consistent across components.

3. **How does WebSocket integration work with Redux?**  
   The `WebSocketService` subscribes to queue topics and dispatches Redux actions when messages arrive. The Redux store is updated, and components connected via `useSelector` re‑render automatically.

4. **How do you handle authentication state persistence?**  
   On login, we store the JWT, user details, and preferences in localStorage. On app load, the `getInitialState` function reads from localStorage and restores the Redux state. The JWT is also attached to API requests via the axios interceptor.

5. **How do you protect routes?**  
   The `ProtectedRoute` component checks if the user is authenticated (token exists) and if the role is allowed. If not, it redirects to login or the appropriate dashboard.

6. **How do you upload profile images?**  
   The frontend uses a file input, builds a `FormData` object, and sends a POST request to `/user/profile/image` with `multipart/form-data`. The backend saves the file and returns the URL, which is then stored in Redux and displayed.

7. **How do you implement infinite scroll / load more?**  
   For search results, the `SearchResults` component receives pagination metadata from the backend. When the user clicks “Load More”, it dispatches a `performSearch` action with the next page number. The results are appended to the existing list.

8. **How do you handle form validation?**  
   We use `Formik` and `Yup` for form validation. For example, the registration form validates email format, password strength, and phone number pattern. Error messages are displayed inline.

9. **How do you ensure that the UI updates in real time?**  
   By subscribing to WebSocket topics, the frontend receives updates as soon as they happen on the backend. The Redux store is updated, causing components to re‑render with the new data.

10. **What is the purpose of the `OtpInput` component?**  
    It provides a 6‑digit OTP input with automatic focus movement and submission when all digits are filled. It is used for email verification and password reset.

11. **How do you manage dark mode?**  
    The `darkMode` preference is stored in Redux and localStorage. When toggled, the `dark-mode` class is added/removed from the `body`, and CSS variables switch to dark mode styles.

12. **How do you handle file uploads (profile images)?**  
    The `UserProfile` component contains a hidden file input. When the user selects a file, it is validated (type, size) and uploaded via `authService.uploadProfileImage`. The returned image URL is stored in Redux and displayed immediately.

### 9.10 Diagram Prompts

**Redux State Flow (Mermaid)**
```mermaid
graph TD
    A[Component] -->|dispatch action| B[Redux Slice]
    B -->|update state| C[Store]
    C -->|useSelector| A
    D[WebSocket Service] -->|dispatch| B
    E[API Call] -->|dispatch| B
```

**WebSocket Subscription Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant CustomerQueue
    participant WebSocketService
    participant Backend
    participant Redux

    CustomerQueue->>WebSocketService: subscribeToQueue(queueId)
    WebSocketService->>Backend: STOMP SUBSCRIBE
    Backend-->>WebSocketService: initial queue state
    WebSocketService->>Redux: dispatch(updateQueue)
    Redux-->>CustomerQueue: re‑render
    Note over Backend: queue changes
    Backend-->>WebSocketService: queue update message
    WebSocketService->>Redux: dispatch(updateQueue)
    Redux-->>CustomerQueue: re‑render
```

---

## 10. Testing Strategy

QueueLess includes a comprehensive suite of tests to ensure correctness, prevent regressions, and document expected behaviour. This section explains the testing approach, tools used, and how to run tests.

### 10.1 Backend Unit Tests

**Tools**: JUnit 5, Mockito, MockMvc

**Service Tests** (e.g., `AuthServiceTest`, `QueueServiceTest`, `AdminServiceTest`)

- **Purpose**: Test business logic in isolation.
- **Approach**: Mock all repository dependencies using `@Mock` and `@InjectMocks`. Use `Mockito.when()` to stub database calls, and verify interactions.
- **Example** (from `AuthServiceTest`):
  ```java
  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @InjectMocks private AuthService authService;

  @Test
  void loginSuccess() {
      User user = User.builder().email("test@example.com").password("encoded").build();
      when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
      when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
      JwtResponse response = authService.login(new LoginRequest("test@example.com", "pass"));
      assertNotNull(response);
      assertEquals("USER", response.getRole());
  }
  ```

**Controller Tests** (e.g., `AuthControllerTest`, `QueueControllerTest`)

- **Purpose**: Test REST endpoints, security, and request/response mapping.
- **Approach**: Use `@WebMvcTest` or `@SpringBootTest` with `@AutoConfigureMockMvc`. Mock service layer with `@MockBean`. Use `MockMvc` to perform requests and assert status, headers, and JSON.
- **Example** (from `AuthControllerTest`):
  ```java
  @MockBean private AuthService authService;
  @Test
  void loginSuccess() throws Exception {
      when(authService.login(any(LoginRequest.class))).thenReturn(new JwtResponse("token", "USER", ...));
      mockMvc.perform(post("/api/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(new LoginRequest("test@example.com", "pass"))))
              .andExpect(status().isOk())
              .andExpect(jsonPath("$.token").value("token"));
  }
  ```

**Repository Tests** – Not explicitly written; they are covered by integration tests that use embedded MongoDB.

### 10.2 Backend Integration Tests

**Tools**: `@SpringBootTest` with embedded MongoDB (Flapdoodle), `TestRestTemplate`.

**Purpose**: Test the entire stack, from controller to database, including real database interactions.

**Setup**:
- `BaseIntegrationTest` starts an embedded MongoDB instance (via Flapdoodle) before any tests run.
- It sets `spring.data.mongodb.uri` to the embedded instance and disables SSL and Redis for tests.
- Example: `QueueFlowIntegrationTest` simulates a complete flow: register user, create place, create service, register provider, create queue, join, serve, complete, and check history.

**Challenges**:
- Embedded MongoDB may not support all features (e.g., transactions, but we don’t use them).
- We must manually verify users (set `isVerified=true`) because email OTP is not sent in tests.

**Code snippet** (from `QueueFlowIntegrationTest`):
```java
@Test
void fullQueueFlow() throws Exception {
    // 1. Register user, verify manually
    // 2. Create admin token, register admin
    // 3. Create place, service
    // 4. Create provider token, register provider
    // 5. Provider creates queue
    // 6. User joins queue
    // 7. Provider serves next, completes token
    // 8. User token history contains the token
}
```

### 10.3 Frontend Tests

**Current Status**: Not yet implemented, but planned.

**Tools**: Jest, React Testing Library.

**Planned coverage**:
- **Component tests**: Ensure components render correctly with mock data.
- **Redux tests**: Test slice reducers and async thunks.
- **Integration tests**: Test pages with mocked API responses.
- **E2E tests** (Cypress) – could be added later.

### 10.4 Mocking External Services

- **Razorpay**: We mock the `RazorpayClient` in `PaymentServiceTest` using Mockito and a `MockedConstruction`. This allows us to simulate successful and failing order creation without real network calls.
- **Email**: We mock `JavaMailSender` or the whole `EmailService` to avoid sending actual emails.
- **Firebase**: `FcmService` is mocked in tests that require it.

### 10.5 Test Coverage

We aim for high coverage on critical services and controllers. The build currently runs all tests and fails if coverage drops below a threshold (not enforced yet but can be added with JaCoCo).

To run tests:
```bash
cd backend
./mvnw test
```
This will run all unit and integration tests (the latter use embedded MongoDB and may take a while).

### 10.6 Best Practices Followed

- **Isolation**: Each test runs in a clean environment (embedded DB resets).
- **Readable**: Tests use `assertThat` and meaningful names.
- **Fast**: Unit tests are fast; integration tests are separate and can be skipped with a profile.
- **Test data**: Built‑in test data (e.g., `createTestQueue()`) to avoid duplication.

### 10.7 Interview Questions & Answers

1. **How do you test the service layer?**  
   We mock repositories using Mockito and verify that service methods call the correct repository methods with the expected parameters. We also assert the returned values.

2. **How do you test controllers?**  
   Using `@WebMvcTest` or `@SpringBootTest` with `MockMvc`. We mock the service layer and test that the controller returns the correct HTTP status and response body.

3. **How do you test the queue flow end‑to‑end?**  
   We have an integration test (`QueueFlowIntegrationTest`) that uses an embedded MongoDB and `TestRestTemplate` to simulate a full user journey. It covers registration, place/service creation, queue creation, joining, serving, and completion.

4. **How do you handle test data that requires unique constraints (e.g., email)?**  
   We use unique values like `user@example.com` or generate random strings. The embedded database is cleared between tests, so there is no conflict.

5. **How do you test that the email OTP works?**  
   In integration tests, we manually set `isVerified=true` to bypass the OTP step. In unit tests, we mock `EmailService` and verify it was called with the correct email.

6. **How do you test the WebSocket endpoints?**  
   We have unit tests for the `QueueWebSocketController` using `Mockito`. For integration, we would need to use a test client that connects to the WebSocket, which is more complex; we rely on unit tests for now.

7. **How do you mock Razorpay in tests?**  
   We use `MockedConstruction` to create a mock `RazorpayClient` and stub its `orders.create()` method to return a simulated order. This avoids real HTTP calls.

8. **What is the role of `BaseIntegrationTest`?**  
   It sets up the embedded MongoDB and disables Redis and SSL for all integration tests. It ensures that the test environment is consistent.

9. **How do you ensure that the embedded MongoDB is cleaned up after tests?**  
   The Flapdoodle library automatically stops the MongoDB process after the JVM exits. We also delete the temporary data directory.

10. **What improvements could be made to the test suite?**
    - Add integration tests for WebSocket connections.
    - Add frontend tests.
    - Add more negative test cases (e.g., invalid input, permission errors).
    - Use test containers for a more realistic database environment.

### 10.8 Diagram 

**Test Layers (Mermaid)**
```mermaid
%%{init: {'theme': 'dark', 'themeVariables': { 'primaryTextColor': '#FFFFFF', 'lineColor': '#B0BEC5', 'edgeLabelBackground':'#111827'}}}%%
graph TD
    %% Global Diagram Title - Clean text
    titleD("SOFTWARE TESTING STRATEGY (DARK MODE)"):::titleNodeD
    style titleD fill:none,stroke:none

    %% --- Unit Tests Tier ---
    %% Corrected: Explicit single-word ID [UnitTests] is used for styling, 
    %% with the full title defined inside brackets.
    subgraph UnitTests["UNIT TESTS"]
        direction TB
        A[Service Tests]:::testNodeD --> B(Mock Repositories):::toolNodeD
        C[Controller Tests]:::testNodeD --> D(Mock Service):::toolNodeD
    end

    %% --- Integration Tests Tier ---
    subgraph IntegrationTests["INTEGRATION TESTS"]
        direction TB
        E[Full Flow Test]:::testNodeD --> F(Embedded MongoDB):::toolNodeD
        E --> G(Real HTTP Calls):::toolNodeD
    end

    %% --- Frontend Tests Tier ---
    %% The fix for the original parentheses error is maintained by 
    %% defining the full title within the brackets.
    subgraph FrontendTests_Planned["FRONTEND TESTS (PLANNED)"]
        direction TB
        H[Component Tests]:::testNodeD --> I(Jest + RTL):::toolNodeD
        J[Redux Tests]:::testNodeD --> K(Mock API):::toolNodeD
    end

    %% --- Defining Tiered Styles for Dark Mode ---
    classDef titleNodeD font-size:20px,font-weight:bold,color:#E1F5FE,stroke:none;
    
    %% Tier 1: Tests (Bright Blue)
    classDef testNodeD fill:#0D47A1,stroke:#40C4FF,stroke-width:2px,color:#FFFFFF,rx:10,ry:10,font-weight:600;
    
    %% Tier 2: Testing Tools/Mocks (Bright Green)
    classDef toolNodeD fill:#1B5E20,stroke:#A5D6A7,stroke-width:2px,color:#FFFFFF,rx:5,ry:5,font-weight:600,font-style:italic;

    %% --- Subgraph Box Styling (Dark Gray) ---
    %% The styles are now applied to the explicit IDs, resolving the parse error.
    style UnitTests fill:#111827,stroke:#D1D5DB,stroke-width:1.5px,rx:5,ry:5;
    style IntegrationTests fill:#111827,stroke:#D1D5DB,stroke-width:1.5px,rx:5,ry:5;
    style FrontendTests_Planned fill:#111827,stroke:#D1D5DB,stroke-width:1.5px,rx:5,ry:5;
```

**Integration Test Flow (Mermaid)**
```mermaid
sequenceDiagram
    participant Test
    participant Backend
    participant EmbeddedMongo

    Test->>Backend: HTTP POST /api/auth/register
    Backend->>EmbeddedMongo: insert user
    Test->>Backend: HTTP POST /api/auth/verify-email
    Backend->>EmbeddedMongo: update user verified=true
    Test->>Backend: ... continue flow
    Backend->>EmbeddedMongo: various operations
    Test->>Backend: final assertion
```

---

## 11. Deployment & DevOps

QueueLess is containerised with Docker and orchestrated via Docker Compose, making it easy to run locally or deploy to a server. This section covers the build process, environment configuration, monitoring stack, and the CI/CD pipeline.

### 11.1 Dockerfiles

**Backend Dockerfile** (located at `backend/backend/Dockerfile`)
```dockerfile
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- Multi‑stage build: first stage compiles the code, second stage only contains the JRE and the final JAR.
- SSL is handled by the application (server.ssl.* properties), so no extra configuration in Docker.

**Frontend Dockerfile** (located at `frontend/queue-less-frontend/Dockerfile`)
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
COPY ssl/cert.pem /etc/nginx/ssl/cert.pem
COPY ssl/key.pem /etc/nginx/ssl/key.pem
EXPOSE 443
```
- Builds the React app, then serves it with Nginx over HTTPS using self‑signed certificates (for development). In production, replace certificates with real ones.

### 11.2 Docker Compose

The `docker-compose.yml` file orchestrates all services:
- **backend** – Spring Boot app
- **frontend** – Nginx serving the React app
- **mongodb** – MongoDB database
- **redis** – Redis cache
- **prometheus** – Metrics scraper
- **grafana** – Metrics visualisation
- **loki** – Log aggregation
- **promtail** – Log collector

**Key points**:
- All services are on a custom network `queueless-net`.
- Backend depends on MongoDB and Redis.
- Environment variables are passed from the host environment (or a `.env` file) to the backend container.
- SSL certificates are mounted from the host.

**Example snippet**:
```yaml
services:
  backend:
    build: ./backend/backend
    container_name: queueless-backend
    environment:
      - REDIS_HOST=redis
      - MONGODB_URI=${MONGODB_URI}
      - JWT_SECRET=${JWT_SECRET}
      # ... other vars
    ports:
      - "8443:8443"
    networks:
      - queueless-net
```

### 11.3 Environment Variables

All sensitive values (database URI, JWT secret, API keys) should be stored in a `.env` file (never committed). Example `.env`:

```properties
MONGODB_URI=mongodb://mongodb:27017/queueless
JWT_SECRET=your-secret-here
JWT_EXPIRATION=86400000
RAZORPAY_KEY=rzp_test_xxxx
RAZORPAY_SECRET=xxxx
MAIL_USERNAME=your@gmail.com
MAIL_PASSWORD=your-app-password
SSL_KEY_STORE_PASSWORD=changeit
APP_FRONTEND_URL=https://localhost:5173
```

### 11.4 Monitoring Stack

- **Prometheus** scrapes metrics from the backend’s `/actuator/prometheus` endpoint every 15 seconds.
- **Grafana** connects to Prometheus and provides dashboards. Default login is `admin/admin`. Pre‑configured dashboards can be imported.
- **Loki** stores logs from all containers. **Promtail** collects logs from Docker and sends them to Loki.
- To view logs, use Grafana’s Explore tab with Loki data source.

### 11.5 CI/CD Pipeline (GitHub Actions)

The workflow file (`.github/workflows/ci-cd.yml`) automates testing, building, and deployment.

**Workflow trigger**:
- On push to `main` or `develop`
- On pull request to `main`

**Jobs**:
1. **test-backend** – runs `mvn clean verify` with environment secrets (MongoDB URI, JWT, etc.). Uses setup‑java action.
2. **test-frontend** – installs dependencies and runs `npm run build` (no actual tests yet, but verifies that the build succeeds).
3. **build-and-push** – only runs on push to `main`. Logs into GitHub Container Registry (GHCR), builds and pushes Docker images for backend and frontend, tagged with `latest` and commit SHA.

**Secrets** must be set in the GitHub repository:
- `MONGODB_URI`, `JWT_SECRET`, `JWT_EXPIRATION`, `RAZORPAY_KEY`, `RAZORPAY_SECRET`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `SSL_KEY_STORE_PASSWORD`, `VITE_API_BASE_URL`

### 11.6 Running Locally with Docker Compose

1. Clone the repository.
2. Create a `.env` file in the root with the required environment variables (see example above).
3. Build and start all services:
   ```bash
   docker-compose up --build
   ```
4. Access:
    - Frontend: `https://localhost:5173`
    - Backend API: `https://localhost:8443`
    - Grafana: `http://localhost:3000`
    - Prometheus: `http://localhost:9090`

### 11.7 Production Considerations

- **SSL certificates**: Replace self‑signed certificates with certificates from a trusted CA (e.g., Let’s Encrypt). Update `nginx.conf` and backend `application.properties` accordingly.
- **Secrets management**: Use a secrets manager (e.g., HashiCorp Vault) or environment variables injected at runtime (Kubernetes secrets).
- **Database backups**: Regularly backup MongoDB data. Use `mongodump` or a managed database service.
- **Scaling**: The backend is stateless, so multiple replicas can be run behind a load balancer. MongoDB should be scaled with replica sets.
- **Monitoring**: Set up alerts in Grafana for critical metrics (e.g., high error rate, high queue wait times).
- **Logging**: Consider using a centralised logging service like Loki in production, with retention policies.

### 11.8 Diagram 

**Docker Compose Architecture (Mermaid)**
```mermaid
graph TD
    subgraph Docker Host
        B[Backend Container]
        F[Frontend Container]
        M[MongoDB Container]
        R[Redis Container]
        P[Prometheus Container]
        G[Grafana Container]
        L[Loki Container]
        T[Promtail Container]
    end
    B --> M
    B --> R
    F --> B
    P --> B
    G --> P
    T --> L
    L --> G
```

**CI/CD Pipeline (Mermaid)**
```mermaid
graph LR
    A[Push to main/develop] --> B[Run tests]
    B --> C{Tests pass?}
    C -->|Yes| D[Build Docker images]
    D --> E[Push to GHCR]
    C -->|No| F[Fail build]
```

---

## 12. Future Roadmap & Possible Enhancements

QueueLess is already a feature‑rich platform, but there are many directions in which it could be extended. This section outlines potential future improvements, both technical and functional.

### 12.1 Short‑Term Enhancements

1. **Mobile App**
    - Develop native Android and iOS apps (Flutter or React Native) to provide a smoother mobile experience and enable push notifications without browser permissions.
    - Leverage FCM for both platforms.

2. **Real‑Time Analytics Dashboard**
    - Add live graphs of token volume, average wait time, and queue loads on the admin/provider dashboards using WebSocket updates.
    - Show active users per queue, historical trends.

3. **Multi‑Language Support (i18n)**
    - Internationalise the frontend using i18next, supporting English, Hindi, and other regional languages.
    - Store user language preference and serve translations.

4. **Advanced Reporting**
    - Scheduled email reports (daily/weekly) for admins and providers.
    - More export formats (CSV, JSON) and custom report templates.

5. **Improved Notification Preferences**
    - Allow users to set notification time per queue (already done) and also enable/disable specific notification types (e.g., only turn notifications, not emergency approvals).
    - Option to receive notifications via SMS (using Twilio or similar).

6. **Two‑Factor Authentication (2FA)**
    - Add an extra layer of security for admin and provider accounts using TOTP (Google Authenticator) or email/SMS OTP.

### 12.2 Medium‑Term Enhancements

1. **Queue Analytics & Machine Learning**
    - Predict wait times based on historical data using time series forecasting (e.g., ARIMA, Prophet).
    - Recommend best time to join dynamically, personalised to the user’s preferences.

2. **Integration with Third‑Party Systems**
    - Calendar integration (Google Calendar, Outlook) to automatically schedule appointments when a token is served.
    - Integration with hospital management systems (HMS) or point‑of‑sale (POS) systems for automatic ticket generation.

3. **Payment Subscriptions**
    - Allow admins to subscribe to monthly/yearly plans instead of buying tokens manually.
    - Use Razorpay subscriptions with recurring billing.

4. **Provider Scheduling**
    - Allow providers to set availability hours per queue (e.g., open only from 9 AM to 5 PM) and automatically deactivate the queue outside those hours.
    - Support for recurring breaks (lunch breaks).

5. **Advanced User Profiles**
    - Let users add multiple addresses, preferred places, and receive notifications about new services.
    - Social login (Google, Facebook) for quicker registration.

6. **Audit & Compliance**
    - Enhance audit logging with IP addresses, user agent, and geolocation.
    - Provide compliance reports (GDPR, HIPAA) if needed for medical deployments.

### 12.3 Long‑Term / Architectural Improvements

1. **Microservices Architecture**
    - Split the monolith into separate services (e.g., auth service, queue service, notification service) to scale independently.
    - Use message brokers (RabbitMQ, Kafka) for inter‑service communication.

2. **GraphQL API**
    - Replace REST with GraphQL to give clients more flexibility and reduce over‑fetching.
    - Could be introduced alongside REST.

3. **Serverless Functions**
    - Move scheduled jobs (notifications, metrics) to serverless functions (AWS Lambda) to reduce server costs and improve scalability.

4. **Database Sharding**
    - As data grows, shard MongoDB collections by tenant (admin) or by time (e.g., queue data older than 6 months moved to cold storage).

5. **Kubernetes Deployment**
    - Container orchestration with Kubernetes for better scalability, rolling updates, and self‑healing.
    - Use Helm charts to manage deployments.

6. **Disaster Recovery**
    - Implement multi‑region replication for MongoDB and Redis.
    - Automated backups with point‑in‑time recovery.

### 12.4 Community & Ecosystem

- **Open Source Contribution** – Invite external contributors by providing clear contribution guidelines, issue templates, and a code of conduct.
- **Plugins / Extensions** – Allow third‑party developers to create plugins for custom integrations (e.g., custom notification channels, payment gateways).
- **Demo & Documentation** – Maintain a live demo instance and improve the existing documentation (user guides, API tutorials).

---

## 13. Conclusion

QueueLess is a full‑stack queue management system that successfully addresses the needs of multiple stakeholders – users, providers, and administrators. Its architecture is built on modern, well‑supported technologies (Spring Boot, MongoDB, React) and includes essential features such as real‑time updates, role‑based access, payment integration, push notifications, and comprehensive search.

The project demonstrates:

- **Strong separation of concerns** with a layered backend architecture.
- **Responsive, user‑friendly frontend** built with React and Redux.
- **Robust authentication and authorisation** using JWT and method‑level security.
- **Real‑time capabilities** via WebSocket (STOMP) for live queue updates.
- **Scalability** through stateless design, caching (Redis), and containerisation.
- **Observability** with Prometheus, Grafana, and Loki.
- **Testing** with unit and integration tests covering critical flows.
- **CI/CD** pipeline using GitHub Actions and container registry.

The project is production‑ready and can be deployed using Docker Compose or scaled further with Kubernetes. The roadmap outlines exciting possibilities for future growth.

### 13.1 Interview Preparation Summary

When discussing QueueLess in an interview, you can highlight:

- **Why you chose the tech stack** – explain trade‑offs (e.g., MongoDB for document flexibility, Spring Boot for productivity, React for component reusability).
- **Key challenges** – handling real‑time updates, preventing double token joining, securing WebSocket connections, ensuring idempotent operations in payments.
- **How you solved them** – using WebSocket with JWT, `activeTokenId` field, atomic MongoDB updates, Razorpay order notes.
- **Design patterns** – Repository, DTO, Service layer, Observer (WebSocket), Builder (Lombok), etc.
- **Testing strategy** – unit tests with mocks, integration tests with embedded MongoDB, how you test asynchronous code (schedulers) and external services (Razorpay).
- **Scalability considerations** – stateless backend, Redis caching, indexes in MongoDB, potential for microservices.
- **Future improvements** – mobile app, AI‑based wait time predictions, GraphQL.

**Potential tricky questions**:
- *How do you ensure a user doesn’t join two queues at once?* – `activeTokenId` field in `User`; check before adding.
- *What if two users try to join the same queue at the same time?* – MongoDB document‑level atomic update; only one will succeed.
- *How do you handle token expiry?* – JWT expires after 24 hours; refresh tokens not implemented, but could be added.
- *How do you test WebSocket functionality?* – Unit tests with mocked messaging template; integration tests would require a test client.
- *How do you keep the wait time calculation accurate?* – Updated every 30 seconds based on waiting tokens and average service time; can be improved with actual service durations.

By studying this documentation thoroughly, you’ll be able to explain every line of code, justify design decisions, and confidently answer interview questions.

---

## 14. Glossary

| Term                | Description                                                                                                                                                              |
|---------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **JWT**             | JSON Web Token – used for stateless authentication.                                                                                                                      |
| **STOMP**           | Simple Text Oriented Messaging Protocol – used over WebSocket for messaging.                                                                                             |
| **FCM**             | Firebase Cloud Messaging – push notification service.                                                                                                                    |
| **Razorpay**        | Payment gateway used for purchasing admin/provider tokens.                                                                                                               |
| **GeoJsonPoint**    | MongoDB’s representation of a geographic point (longitude, latitude).                                                                                                   |
| **MongoDB Aggregation** | A pipeline for data processing, used for advanced search queries.                                                                                                     |
| **SockJS**          | A library that provides a WebSocket‑like API and falls back to other transports when WebSocket is unavailable.                                                          |
| **Audit Log**       | A record of important business actions (e.g., user login, queue creation) for traceability.                                                                             |
| **Provider**        | A user who manages queues; can serve tokens, approve emergencies, etc.                                                                                                   |
| **Admin**           | A user who manages places, services, and providers; has full system access.                                                                                              |
| **Token**           | A unique identifier for a user’s spot in a queue; also used for payment (admin/provider tokens).                                                                         |
| **Queue**           | A collection of tokens for a specific service.                                                                                                                           |
| **Place**           | A physical location (e.g., hospital, shop) that offers services.                                                                                                         |
| **Service**         | A specific offering at a place (e.g., cardiology, haircut) that has an average service time.                                                                            |
| **WebSocket**       | A protocol for full‑duplex communication over a single TCP connection; used for real‑time updates.                                                                      |
| **Prometheus**      | A metrics‑based monitoring system.                                                                                                                                       |
| **Grafana**         | A visualisation tool for metrics.                                                                                                                                        |
| **Loki**            | A log aggregation system.                                                                                                                                                |
| **Promtail**        | A log collector that ships logs to Loki.                                                                                                                                 |

---
## 14. Security Deep Dive

QueueLess implements multiple layers of security to protect user data, prevent unauthorised access, and ensure system integrity. This section explores each security measure in detail.

### 14.1 Authentication (JWT)

**How JWT is generated**  
`JwtTokenProvider.generateToken(User user)` creates a JWT with the following claims:
- `sub` – user ID (subject)
- `role` – user role (USER, PROVIDER, ADMIN)
- `userId` – duplicate of subject (for convenience)
- `email` – user email
- `name` – user name
- `placeId` – if the user is a provider with a specific place
- `isVerified` – whether email is verified
- `ownedPlaceIds` – list of place IDs owned (for admins)

The token is signed with HMAC‑SHA256 using a secret key from `jwt.secret`. It has an expiry (`jwt.expiration`) – default 24 hours.

**Validation**  
`JwtAuthenticationFilter` intercepts every request, extracts the token from the `Authorization: Bearer <token>` header, and calls `JwtTokenProvider.validateToken()`. This method checks:
- Signature validity (using the same secret)
- Expiry (not expired)
- Malformed structure

If valid, it creates a `UsernamePasswordAuthenticationToken` with the user ID as principal and the role as granted authority, and sets it in `SecurityContext`. This enables `@PreAuthorize` annotations in controllers.

**Why JWT over sessions?**  
Stateless – no need for server‑side session storage, making horizontal scaling trivial. The token contains all necessary information, so we don’t need to query the database for each request (except for the `activeTokenId` check, which we do in service layer anyway). However, we still fetch user for some operations, but authentication itself is stateless.

### 14.2 Role‑Based Access Control

Spring Security’s `@PreAuthorize` is used at the method level. We’ve created custom annotations:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface AdminOnly {}
```

Similarly `@ProviderOnly`, `@UserOnly`, `@Authenticated`. These are applied to controller methods.

**Why method‑level?**  
It keeps security rules close to the business logic, making it easier to see which roles are allowed for each endpoint. URL‑based security in `SecurityConfig` is also used for public endpoints, but method‑level is more granular.

### 14.3 Password Storage

Passwords are encoded with `BCryptPasswordEncoder` (Spring Security) with strength 10. BCrypt is adaptive, making brute‑force attacks slower. We never store plain text passwords.

### 14.4 OTP Storage & Verification

OTPs are stored in MongoDB (collection `otp`) with an expiry of 5 minutes. The document has a TTL index that automatically deletes expired OTPs.  
Alternative: could use Redis, but MongoDB TTL is simpler and consistent with the rest of the data. The `OtpDocument` also has a unique index on `email` (ensuring only one OTP per email at a time).

### 14.5 CORS Configuration

`SecurityConfig.corsConfigurationSource()` sets allowed origins (e.g., `https://localhost:5173`), allowed methods (GET, POST, PUT, DELETE, OPTIONS, PATCH), and allowed headers. Credentials (cookies) are allowed, though we don’t use cookies. This is necessary because the frontend runs on a different origin (5173) while backend is on 8443.

### 14.6 CSRF Protection

CSRF is disabled because we use JWT and stateless sessions, and the application is an API that doesn’t rely on cookies for authentication. The token is sent in the `Authorization` header, which is not automatically included by browsers, so CSRF attacks are not possible. However, if we ever add cookie‑based authentication, we would re‑enable CSRF protection.

### 14.7 XSS & Security Headers

We rely on Spring Security’s default headers (X‑XSS‑Protection, X‑Content‑Type‑Options, etc.) and have customised Content‑Security‑Policy in `SecurityConfig` to restrict sources of scripts, styles, images, and fonts. This helps mitigate XSS and clickjacking.

### 14.8 Rate Limiting

`RateLimitFilter` applies limits based on endpoint groups:

- **Default**: 100 requests per minute per user/IP
- **Token creation endpoints**: 10 per minute (stricter)
- **Search endpoints**: 50 per minute (higher allowance)

The key is built from user ID (if authenticated) or IP address, combined with the URI. This prevents abuse, such as brute‑force login attempts or flooding token creation.

### 14.9 WebSocket Security

`StompJwtChannelInterceptor` intercepts the STOMP CONNECT frame, validates the JWT, and sets the user in the `StompHeaderAccessor`. The `WebSocketSecurityConfig` further restricts subscriptions and message destinations:

- Public endpoints (CONNECT, HEARTBEAT) are permitted.
- Subscriptions to `/user/**` and `/topic/queues/*` require authentication.
- Subscriptions to `/topic/admin/**` require ADMIN role.
- All other messages are denied by default.

User destinations (`/user/queue/*`) are automatically mapped to the authenticated user, ensuring that private messages are only sent to the intended user.

### 14.10 Input Validation

All DTOs are annotated with `@NotNull`, `@NotBlank`, `@Size`, `@Pattern`, etc. Controllers use `@Valid` to trigger validation. The `GlobalExceptionHandler` catches `MethodArgumentNotValidException` and returns a structured error response with field‑specific messages.

### 14.11 Audit Logging

Important actions (login, registration, queue creation, token served, payment completed, etc.) are logged in the `audit_logs` collection. This provides traceability for security incidents.

### 14.12 Database Security

- MongoDB credentials are stored in environment variables, not hard‑coded.
- The database is accessed only by the backend (firewall rules would restrict in production).
- Data validation is performed at the application level to prevent injection (though MongoDB drivers are safe from SQL injection).

### 14.13 File Upload Security

- Only image files are allowed (content‑type validation).
- File size limited to 2 MB.
- Files are stored outside the web root (under a specific directory) and served via a resource handler.
- The filename is generated by the server (UUID + user ID) to prevent directory traversal attacks.

### 14.14 Interview Questions & Answers

1. **How do you prevent a user from accessing another user’s data?**  
   Most endpoints that return data for a specific user (e.g., `/api/user/profile`) use the authenticated user ID from the security context. For endpoints that take a user ID as a path variable (e.g., `/api/user/favorites/{placeId}`), we check that the authenticated user matches the path variable. For admin endpoints, we check that the admin owns the resource (e.g., the place belongs to the admin).

2. **What is the purpose of the `isActive` field in the User entity?**  
   It allows admins to disable provider accounts. When disabled, the user cannot log in. This is useful for temporarily suspending a provider without deleting their data.

3. **How do you protect against brute‑force login attacks?**  
   We use rate limiting on the login endpoint (part of the authentication endpoints). Additionally, after multiple failed attempts, we could lock the account or require CAPTCHA – not yet implemented but could be added.

4. **How do you ensure that the JWT secret is kept secure?**  
   It is stored in environment variables (`.env` file) and never committed. In production, it is injected as a secret in the container orchestration (e.g., Docker secrets, Kubernetes secrets). The secret should be at least 256 bits.

5. **How do you handle token refresh?**  
   Currently, we don’t implement refresh tokens. If a token expires, the user must log in again. This is acceptable for a web application, but for mobile apps, we could add refresh tokens.

6. **What measures are in place to prevent SQL/NoSQL injection?**  
   We use Spring Data MongoDB’s `Query` and `Criteria` objects, which safely parameterise queries. User input is never concatenated directly into query strings. For geospatial queries, coordinates are passed as numbers.

7. **How do you handle CORS?**  
   We have a CORS configuration that allows requests only from the frontend origin (`https://localhost:5173`). This is set in `SecurityConfig`. Allowed methods include OPTIONS, and credentials are allowed (though not used). This prevents unauthorised domains from making API calls.

8. **How do you secure WebSocket connections?**  
   We validate JWT during the CONNECT frame. Without a valid token, the connection is rejected. Once connected, subscriptions are also secured: only authenticated users can subscribe to `/user/**` and `/topic/queues/*`.

9. **How do you handle file uploads safely?**  
   The file is stored with a generated name (no user‑supplied name) to prevent directory traversal. The file is validated for content type and size. It is served from a directory that is not exposed via the main application root; we have a dedicated resource handler.

10. **What is the purpose of the `RateLimitFilter` and how does it work?**  
    It limits the number of requests per user/IP per endpoint group. It uses Bucket4j to implement a token bucket algorithm. For each request, it consumes a token; if the bucket is empty, it returns 429 Too Many Requests. The limit is configured per group (e.g., token creation 10/min, search 50/min, default 100/min).

---
## 15. Database Design & Indexes

QueueLess uses MongoDB as its primary database. This section explains the schema design for each collection, the relationships between documents, the indexing strategy, and the reasoning behind key decisions.

### 15.1 Collection Overview

| Collection               | Purpose                                                                 |
|--------------------------|-------------------------------------------------------------------------|
| `users`                  | All user accounts (USER, PROVIDER, ADMIN).                             |
| `places`                 | Physical locations (hospitals, shops, banks, etc.) with geospatial data. |
| `services`               | Services offered at a place (e.g., cardiology, haircut).                |
| `queues`                 | Active and historical queues for a service. Contains embedded tokens.   |
| `feedback`               | User feedback on completed tokens.                                      |
| `tokens`                 | Admin and provider registration tokens (purchased via Razorpay).        |
| `payments`               | Razorpay order records.                                                 |
| `otp`                    | OTPs for email verification and password reset.                         |
| `audit_logs`             | Business activity log.                                                  |
| `notification_preferences` | Per‑queue notification settings for users.                              |
| `password_reset_tokens`  | Tokens for admin‑initiated password reset.                              |
| `queue_hourly_stats`     | Hourly snapshots of waiting counts for analytics.                       |
| `alert_configs`          | Admin alert thresholds.                                                 |

### 15.2 Schema Details & Indexes

#### 15.2.1 `users`

```json
{
  "_id": ObjectId,
  "name": "John Doe",
  "email": "john@example.com",
  "password": "$2a$10$...",
  "phoneNumber": "+1234567890",
  "role": "USER",               // USER, PROVIDER, ADMIN
  "profileImageUrl": "/uploads/user123_abc.jpg",
  "placeId": null,
  "createdAt": ISODate,
  "isVerified": true,
  "preferences": { ... },
  "ownedPlaceIds": [ ... ],     // for admins
  "activeTokenId": "queue123-T-001",
  "lastQueueJoinTime": ISODate,
  "adminId": ObjectId,          // for providers, points to the admin who created them
  "managedPlaceIds": [ ... ],   // for providers
  "fcmTokens": [ ... ],
  "isActive": true
}
```

**Indexes**
- Unique index on `email` – essential for fast login and uniqueness.
- Index on `role` – used when filtering users by role (e.g., providers under an admin).
- Index on `adminId` – to fetch all providers belonging to an admin.
- Index on `activeTokenId` – to quickly find users with active tokens (not a unique index because many users have null).

**Why embedded preferences?**  
Preferences are only ever accessed with the user document, so embedding avoids a separate collection and join.

#### 15.2.2 `places`

```json
{
  "_id": ObjectId,
  "name": "City Hospital",
  "type": "HOSPITAL",
  "address": "123 Main St",
  "location": { "type": "Point", "coordinates": [77.5946, 12.9716] },
  "adminId": ObjectId,
  "imageUrls": [ "https://..." ],
  "description": "A leading hospital...",
  "rating": 4.5,
  "totalRatings": 120,
  "contactInfo": { "phone": "...", "email": "...", "website": "..." },
  "businessHours": [ ... ],
  "isActive": true
}
```

**Indexes**
- `2dsphere` index on `location` – enables geospatial queries (nearby search).
- Compound index on `adminId` and `location` – used when an admin fetches their places with location (though not used heavily).
- Index on `type` – for filtering by place type.
- Index on `rating` – for sorting top‑rated places.

**Why GeoJsonPoint?**  
MongoDB’s geospatial support requires documents to store coordinates in GeoJSON format. We use `GeoJsonPoint` (Spring Data) which maps to the correct structure.

#### 15.2.3 `services`

```json
{
  "_id": ObjectId,
  "placeId": ObjectId,
  "name": "Cardiology Consultation",
  "description": "Heart specialist",
  "averageServiceTime": 15,
  "supportsGroupToken": true,
  "emergencySupport": true,
  "isActive": true
}
```

**Indexes**
- Index on `placeId` – to fetch all services of a place.
- Index on `name` – for text search (not full‑text, but regex is used).

#### 15.2.4 `queues`

```json
{
  "_id": ObjectId,
  "placeId": ObjectId,
  "serviceId": ObjectId,
  "providerId": ObjectId,
  "serviceName": "Cardiology Consultation",
  "maxCapacity": 50,
  "currentPosition": 0,
  "estimatedWaitTime": 10,
  "isActive": true,
  "startTime": ISODate,
  "endTime": ISODate,
  "tokens": [ { ... } ],               // embedded array
  "pendingEmergencyTokens": [ { ... } ],
  "tokenCounter": 42,
  "statistics": { ... },
  "supportsGroupToken": true,
  "emergencySupport": true,
  "emergencyPriorityWeight": 10,
  "requiresEmergencyApproval": false,
  "autoApproveEmergency": true
}
```

**Indexes**
- `providerId` – to list queues for a provider.
- `placeId` – to list queues in a place.
- `serviceId` – to list queues for a service.
- `isActive` – to filter active queues.
- Compound index on `tokens.userId` – to quickly find queues a user is in (used in `getQueuesByUserId`). This is a multikey index.

**Why embed tokens?**  
Tokens are always accessed in the context of their queue. Embedding avoids a separate collection and reduces the number of queries. The embedded array can grow large, but we limit each queue to a maximum capacity (e.g., 100) and have a cleanup job for old tokens.

#### 15.2.5 `feedback`

```json
{
  "_id": ObjectId,
  "tokenId": "queue123-T-001",
  "queueId": ObjectId,
  "userId": ObjectId,
  "providerId": ObjectId,
  "placeId": ObjectId,
  "serviceId": ObjectId,
  "rating": 4,
  "comment": "Great service",
  "staffRating": 5,
  "serviceRating": 4,
  "waitTimeRating": 3,
  "createdAt": ISODate
}
```

**Indexes**
- Unique index on `tokenId` – ensures each token is rated only once.
- Index on `placeId` – to fetch all feedback for a place.
- Index on `providerId` – to fetch all feedback for a provider.

**Why separate collection?**  
Feedback is not part of the queue document because it’s not needed when displaying a queue. It can grow large; separate collection allows pagination and independent indexing.

#### 15.2.6 `tokens` (payment tokens)

```json
{
  "_id": ObjectId,
  "tokenValue": "ADMIN-abc123",
  "role": "ADMIN",
  "isUsed": false,
  "expiryDate": ISODate,
  "createdForEmail": "admin@example.com",
  "isProviderToken": false,
  "providerEmail": null,
  "createdAt": ISODate,
  "createdByAdminId": null
}
```

**Indexes**
- Unique index on `tokenValue` – ensures tokens are not duplicated.
- Index on `createdForEmail` – used when checking if a token exists for a given email (during registration).
- Index on `createdByAdminId` – for admin to see which provider tokens they purchased.

#### 15.2.7 `payments`

```json
{
  "_id": ObjectId,
  "razorpayOrderId": "order_...",
  "razorpayPaymentId": "pay_...",
  "amount": 10000,
  "isPaid": true,
  "createdForEmail": "admin@example.com",
  "role": "ADMIN",
  "createdAt": ISODate,
  "createdByAdminId": null
}
```

**Indexes**
- Unique index on `razorpayOrderId` – prevents duplicate order confirmation.
- Index on `createdForEmail` – for admin payment history.
- Index on `createdByAdminId` – for admin to see payments they made for providers.

#### 15.2.8 `otp`

```json
{
  "_id": ObjectId,
  "email": "user@example.com",
  "otp": "123456",
  "expiryTime": ISODate
}
```

**Indexes**
- Unique index on `email` – only one OTP per email at a time.
- TTL index on `expiryTime` – automatically deletes documents after expiry (5 minutes).

#### 15.2.9 `audit_logs`

```json
{
  "_id": ObjectId,
  "userId": ObjectId,
  "action": "USER_LOGIN",
  "description": "User logged in",
  "details": { "email": "..." },
  "timestamp": ISODate
}
```

**Indexes**
- Index on `userId` – to retrieve logs for a specific user.
- Index on `action` – to filter by event type.
- Index on `timestamp` – for time‑based queries.

#### 15.2.10 `notification_preferences`

```json
{
  "_id": ObjectId,
  "userId": ObjectId,
  "queueId": ObjectId,
  "notifyBeforeMinutes": 5,
  "notifyOnStatusChange": true,
  "notifyOnEmergencyApproval": true,
  "enabled": true,
  "notifyOnBestTime": false,
  "lastBestTimeNotificationSent": ISODate,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

**Indexes**
- Compound unique index on `userId` and `queueId` – ensures one preference per user per queue.
- Index on `queueId` – used by the best‑time scheduler to find all users who have opted in.

#### 15.2.11 `password_reset_tokens`

```json
{
  "_id": ObjectId,
  "token": "RESET-abc123",
  "userId": ObjectId,
  "email": "provider@example.com",
  "expiryDate": ISODate,
  "used": false
}
```

**Indexes**
- Unique index on `token` – fast lookup.
- TTL index on `expiryDate` – auto‑deletes expired tokens.
- Index on `userId` – to delete old tokens for a user before creating a new one.

#### 15.2.12 `queue_hourly_stats`

```json
{
  "_id": ObjectId,
  "queueId": ObjectId,
  "hour": ISODate,
  "waitingCount": 5,
  "createdAt": ISODate
}
```

**Indexes**
- Compound index on `queueId` and `hour` – for fetching stats for a queue over a date range.
- TTL index on `hour` – deletes records older than 60 days (implemented in scheduler, but could be TTL index).

#### 15.2.13 `alert_configs`

```json
{
  "_id": ObjectId,
  "adminId": ObjectId,
  "thresholdWaitTime": 15,
  "notificationEmail": "admin@example.com",
  "enabled": true,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

**Indexes**
- Index on `adminId` – to fetch config for an admin.

### 15.3 Relationships (NoSQL Style)

- **One‑to‑Many**: `place` → `services` – references (`placeId` in `services`).
- **One‑to‑Many**: `service` → `queue` – references (`serviceId` in `queues`).
- **Many‑to‑One**: `queue` → `provider` – reference (`providerId` in `queues`).
- **Many‑to‑One**: `queue` → `place` – reference (`placeId` in `queues`).
- **One‑to‑Many**: `queue` → `tokens` – embedded array (`tokens`).
- **Many‑to‑One**: `feedback` → `user`, `provider`, `place` – references.

We use references (object IDs) instead of embedding for collections that are independent and can grow large, or where we need to query independently.

### 15.4 Query Patterns & Index Usage

- **Login**: `findOne({ email })` – uses unique index.
- **Places nearby**: `find({ location: { $near: ... } })` – uses 2dsphere index.
- **User queues**: `find({ "tokens.userId": userId })` – uses multikey index on `tokens.userId`.
- **Place feedback**: `find({ placeId })` – uses index on `placeId`.
- **Provider queues**: `find({ providerId })` – uses index on `providerId`.
- **Provider analytics** (tokens over time): aggregates over `queues.tokens.completedAt` – requires a scan; we rely on indexing `completedAt` inside the array (cannot index nested fields unless we create a compound index, but for analytics we tolerate a full scan because it’s not real‑time).

### 15.5 Performance Considerations

- **Token array growth**: Each queue has a max capacity, and tokens are removed after completion (or expired). The array size stays within bounds.
- **Aggregation queries**: For search, we use aggregation with `$lookup` to join services with places. This can be heavy; we mitigate by caching place data and using in‑memory joining when possible.
- **Index coverage**: We ensure that common queries are covered by indexes (e.g., `providerId` for provider dashboards).
- **TTL indexes**: Used for OTP and password reset tokens to automatically clean up expired data.

### 15.6 Interview Questions & Answers

1. **Why did you choose MongoDB over a relational database?**  
   The data model is document‑oriented: tokens are naturally nested inside a queue, feedback references multiple entities, and services are linked to places. MongoDB’s flexible schema allowed us to iterate quickly and add fields like `isActive` later without migrations. Geospatial support for nearby search was also a key factor.

2. **How do you handle the relationship between a queue and its tokens?**  
   Tokens are embedded as an array in the queue document. This ensures that when we fetch a queue, we get all its tokens without a join. Updates are atomic at the document level.

3. **How do you prevent the token array from growing indefinitely?**  
   We have a scheduled job that removes tokens older than 24 hours. Also, each queue has a `maxCapacity` that limits the number of waiting+in‑service tokens. Completed tokens are kept for a limited time for history.

4. **Why do you have a separate `feedback` collection instead of embedding feedback in tokens?**  
   Feedback is not needed when viewing the queue; it’s only displayed on the place page. Also, feedback can be rated separately and aggregated. A separate collection makes it easier to paginate and index.

5. **What indexes do you have on the `queues` collection?**  
   We have indexes on `providerId`, `placeId`, `serviceId`, `isActive`, and a multikey index on `tokens.userId`. These support the most common queries.

6. **How do you handle geospatial queries?**  
   We use a 2dsphere index on the `location` field and MongoDB’s `$near` operator. The radius is converted from kilometers to meters.

7. **How do you ensure that OTPs expire?**  
   We set a TTL index on `expiryTime` in the `otp` collection. MongoDB automatically deletes expired documents after the TTL value.

8. **How do you ensure that a token is not used twice?**  
   The `tokenValue` field has a unique index, and we check `isUsed` flag.

9. **What is the purpose of the compound index on `userId` and `queueId` in `notification_preferences`?**  
   It ensures that a user can have only one preference per queue. It also speeds up lookups for user‑specific preferences.

10. **How do you handle performance when the `tokens` array becomes large?**  
    We limit the array size via `maxCapacity` and clean up old tokens. If a queue is extremely busy, the provider can reset the queue, which clears the array.

11. **What would you change if the system had millions of users?**
    - Shard MongoDB by `_id` or by `placeId` (for queues).
    - Use Redis for session‑like data and OTPs.
    - Move analytics to a separate reporting database (e.g., Elasticsearch).
    - Implement read replicas for reporting queries.

---

## 16. Caching Strategy

QueueLess uses Redis to cache frequently accessed data and store short‑lived values like OTPs. This reduces database load and improves response times.

### 16.1 Redis Use Cases

| Use Case               | Description                                                                               | Data Type   | TTL          |
|------------------------|-------------------------------------------------------------------------------------------|-------------|--------------|
| **OTP Storage**        | Temporary OTPs for email verification and password reset.                                 | String      | 5 minutes    |
| **Place Caching**      | Caching place documents by ID to reduce repeated reads.                                   | String (JSON) | 10 minutes   |
| **Places by Admin**    | Caching the list of places owned by an admin.                                             | String (JSON) | 10 minutes   |
| **Services by Place**  | Caching services under a place to avoid DB lookups on place detail page.                 | String (JSON) | 10 minutes   |
| **Queues by Place**    | Caching queues for a place (used in place detail).                                        | String (JSON) | 10 minutes   |
| **Queue by ID**        | Caching individual queue documents to speed up real‑time page loads.                      | String (JSON) | 10 minutes   |

### 16.2 Configuration

`RedisCacheConfig` sets up a `RedisCacheManager` with:

- Default TTL: 10 minutes
- JSON serialization for values (using `GenericJackson2JsonRedisSerializer`)
- Support for Java 8 dates and GeoJson types (via `JavaTimeModule` and `GeoJsonModule`)

We also disable caching of null values to avoid storing empty results.

### 16.3 Cache Annotations

Spring’s cache abstraction is used in service methods:

- `@Cacheable(value = "places", key = "#id")` – on `PlaceService.getPlaceById`
- `@Cacheable(value = "placesByAdmin", key = "#adminId")` – on `PlaceService.getPlacesByAdminId`
- `@Cacheable(value = "queuesByPlace", key = "#placeId")` – on `QueueService.getQueuesByPlaceId`
- `@Cacheable(value = "queues", key = "#queueId")` – on `QueueService.getQueueById`

### 16.4 Cache Eviction

Whenever a document is updated or deleted, the relevant cache entries are evicted:

- `@CacheEvict(value = {"places", "placesByAdmin"}, allEntries = true)` – on `createPlace`, `updatePlace`, `deletePlace`
- `@CacheEvict(value = {"queues", "queuesByPlace"}, allEntries = true)` – on `createQueue`, `serveNextToken`, `completeToken`, etc.

This ensures that stale data is not served.

### 16.5 OTP Storage via MongoDB TTL (Alternative)

While we could have used Redis for OTPs, we opted for MongoDB with a TTL index because it’s simpler (no additional service) and consistent with the rest of the data. Redis is not yet used for OTPs in the current codebase, but it could be added later. The `OtpDocument` uses a TTL index on `expiryTime` to auto‑delete.

### 16.6 Performance Considerations

- **Cache hit rate**: Places, services, and queues are read‑heavy, so caching improves performance significantly.
- **Cache invalidation**: We evict all entries in a cache when any change occurs. For `placesByAdmin`, evicting all is fine because an admin typically doesn’t have many places (fewer than 100). For `places`, evicting all on any place update is also acceptable because the number of places is limited.
- **Memory usage**: Redis is configured with a maxmemory policy (e.g., `volatile‑lru`) to avoid out‑of‑memory issues in production.

### 16.7 Future Improvements

- **Distributed caching**: In a clustered environment, we can use Redis as a central cache.
- **Partial eviction**: Instead of evicting all entries, we could evict only the affected keys (e.g., `places` cache evict only the specific place ID). However, `@CacheEvict` doesn’t support that easily for multiple caches; we can use `CacheManager` programmatically.
- **Caching of search results**: Could be added to reduce database load for common search queries.

### 16.8 Interview Questions & Answers

1. **Why do you need caching?**  
   To reduce database load and improve response times for frequently accessed data (e.g., place details when users browse the homepage).

2. **How do you decide what to cache?**  
   We cache data that is read frequently and changes infrequently. Places, services, and queues fall into this category. We also cache by ID and by admin ID to speed up admin dashboards.

3. **How do you handle cache invalidation?**  
   We use `@CacheEvict` to remove entries when data is updated. For places, we evict all entries because the number of places is small; for larger collections, we would evict only the specific key.

4. **Why do you use Redis instead of an in‑memory cache like Caffeine?**  
   Redis is distributed and can be shared across multiple instances of the backend, which is important for horizontal scaling. Caffeine is local to each instance and would cause inconsistency.

5. **What TTL do you use and why?**  
   10 minutes. This balances freshness and performance. Data does not change so often that we need immediate consistency.

6. **How do you test that caching works?**  
   In unit tests, we mock the cache manager. In integration tests, we can run Redis in a Docker container and verify that repeated calls hit the cache (e.g., by measuring time or checking cache statistics). We also have tests that verify eviction after updates.

7. **What would happen if Redis goes down?**  
   The application would fall back to database queries (caching is not critical). We would log the error and continue serving requests. We could also implement a fallback to a local cache (e.g., Caffeine) but currently we don’t.

8. **How do you ensure that serialised JSON is readable by all services?**  
   We use `GenericJackson2JsonRedisSerializer` with type information (e.g., `@class` property) so that any service can deserialize the objects correctly. This is important if multiple microservices share the same cache.

---

## 17. Conclusion

QueueLess is a complete, production‑ready queue management system that demonstrates a wide range of modern software development practices. Throughout this documentation, we have explored the architecture, design decisions, and implementation details that make the system robust, scalable, and user‑friendly.

### 17.1 Achievements

- **Full‑featured application**: Supports three distinct user roles (USER, PROVIDER, ADMIN) with tailored dashboards and permissions.
- **Real‑time experience**: WebSocket (STOMP) delivers live updates, making queue management feel instantaneous.
- **Multi‑channel notifications**: Email and push notifications (FCM) keep users informed without constant polling.
- **Flexible queue types**: Regular, group, and emergency tokens cater to different scenarios.
- **Geolocation and search**: MongoDB’s geospatial queries power location‑based search and discovery.
- **Monetisation**: Integrated Razorpay payments allow admins and providers to purchase access tokens.
- **Observability**: Prometheus, Grafana, and Loki provide metrics and logs for monitoring and troubleshooting.
- **Comprehensive testing**: Unit and integration tests cover critical flows, ensuring reliability.
- **DevOps readiness**: Docker and Docker Compose make deployment easy, and a CI/CD pipeline automates builds and tests.

### 17.2 Key Technical Decisions & Their Justifications

| Decision                     | Justification                                                                 |
|------------------------------|-------------------------------------------------------------------------------|
| **MongoDB**                  | Document model suits nested tokens; geospatial support; flexible schema.      |
| **Spring Boot**              | Rapid development, built‑in security, WebSocket, and scheduling.              |
| **JWT + Spring Security**    | Stateless authentication; scalable; integrates seamlessly with WebSocket.     |
| **Redis**                    | Fast caching and OTP storage; distributed across instances.                   |
| **WebSocket (STOMP)**        | Low‑overhead real‑time updates; built‑in user destination support.            |
| **Razorpay**                 | Trusted payment gateway with easy integration and test mode.                  |
| **React + Redux**            | Component‑based UI, predictable state management, and WebSocket integration.  |
| **Docker & Compose**         | Ensures consistent development and production environments.                   |

### 17.3 Lessons Learned

- **Embedding vs. referencing**: Embedding tokens inside the queue document simplifies reads but requires careful handling of array growth. The solution was to impose a `maxCapacity` and clean up old tokens.
- **WebSocket security**: Securing the WebSocket connection required a custom `StompJwtChannelInterceptor` to validate tokens during CONNECT; standard HTTP security doesn’t apply.
- **Rate limiting per user**: Initially we limited by IP, but adding user‑based limits prevented authenticated users from bypassing quotas by switching networks.
- **Testing external APIs**: Mocking Razorpay with `MockedConstruction` allowed us to test payment flows without real network calls.
- **Caching invalidation**: Evicting all entries on update works for small datasets but will need refinement as data grows.

### 17.4 Future Enhancements

- **Mobile apps**: Build native apps for Android and iOS to reach a wider audience.
- **Advanced analytics**: Use machine learning to predict wait times and recommend optimal joining times.
- **Multi‑language support**: Add i18n to serve users in different regions.
- **Two‑factor authentication**: Enhance security for admin and provider accounts.
- **Scheduled reports**: Email reports to admins and providers on a daily or weekly basis.
- **Kubernetes deployment**: Orchestrate the containers with Kubernetes for better scalability and self‑healing.
- **GraphQL API**: Give clients more flexibility and reduce over‑fetching.

### 17.5 Interview Preparation & Confidence

This documentation serves as a complete reference. By studying it, you can:

- Explain the purpose and functionality of every class, method, and configuration.
- Justify technology choices with trade‑offs.
- Describe how features work end‑to‑end (e.g., a user joining a queue triggers a sequence of backend, database, and WebSocket actions).
- Discuss scalability, security, and performance considerations.
- Answer potential interview questions with confidence.

When presenting QueueLess in an interview:

- **Start with the problem** – waiting in lines is inefficient; QueueLess digitises the process.
- **Highlight the architecture** – Spring Boot backend, React frontend, MongoDB, WebSocket, Redis.
- **Focus on a key feature** – real‑time updates, group tokens, or the payment integration – and explain the design choices.
- **Mention challenges** – preventing double queue joins, securing WebSocket, rate limiting – and how you solved them.
- **Show your testing approach** – unit tests, integration tests, and how you mock external services.
- **Discuss future improvements** – mobile apps, AI wait‑time predictions, Kubernetes.

### 17.6 Final Words

QueueLess is a testament to modern software engineering: it combines robust backend architecture, responsive frontend design, and thoughtful user experience into a cohesive system. It demonstrates proficiency in Java, Spring Boot, MongoDB, React, and a host of supporting technologies. The project is not only functional but also well‑documented, tested, and ready for deployment.

With this documentation, you are well‑equipped to discuss every aspect of the project in detail, impress interviewers, and confidently claim ownership of the codebase.

---

**End of Complete Project Documentation**
---
We'll now start building the **Technology Learning Guide**. We'll deliver each sub‑section in a separate response, so you can add them to your documentation progressively.

---

## 18. Technology Learning Guide

This section is designed for developers who want to understand the technologies used in QueueLess from scratch. Each sub‑section explains a core technology, its key concepts, and how it is applied in the project. You can read them in order or jump to the topics you need.

---

### 18.1 Java & Spring Boot

#### 18.1.1 Java Basics for Spring Boot

**What is Java?**  
Java is a statically‑typed, object‑oriented programming language that runs on the Java Virtual Machine (JVM). It is known for its “write once, run anywhere” capability, strong ecosystem, and excellent tooling.

**Why Java for QueueLess?**
- **Maturity**: Vast libraries, frameworks, and community support.
- **Performance**: Compiled to bytecode, JIT optimises at runtime.
- **Security**: Built‑in security features and strong typing reduce vulnerabilities.
- **Concurrency**: Easy to handle multiple threads (useful for WebSocket and scheduled tasks).

**Key Java Concepts Used in QueueLess**

1. **Classes & Objects**  
   Every file in `src/main/java/com/queueless/backend/` is a class. Example: `User.java` defines a blueprint for user objects.

   ```java
   @Document("users")
   @Data
   @Builder
   public class User {
       @Id
       private String id;
       private String name;
       // ...
   }
   ```

2. **Annotations**  
   Java annotations (starting with `@`) provide metadata for the compiler and frameworks. Spring heavily uses annotations to reduce XML configuration.  
   Examples in QueueLess:
    - `@SpringBootApplication` – marks the main class.
    - `@RestController` – marks a class as a REST controller.
    - `@Autowired` / `@RequiredArgsConstructor` – for dependency injection.
    - `@Document` – tells Spring Data MongoDB that this class maps to a collection.

3. **Dependency Injection (DI)**  
   Instead of creating objects manually, Spring creates them and injects them where needed. This is done via constructors (preferred) or field injection.  
   In `QueueService.java`:

   ```java
   @Service
   @RequiredArgsConstructor
   public class QueueService {
       private final QueueRepository queueRepository;
       private final SimpMessagingTemplate messagingTemplate;
       // ...
   }
   ```
   Lombok’s `@RequiredArgsConstructor` generates a constructor with all `final` fields, and Spring injects the dependencies automatically.

4. **Lambda Expressions & Streams**  
   Used extensively for processing collections (e.g., filtering tokens). Example from `QueueService`:

   ```java
   long waitingCount = queue.getTokens().stream()
           .filter(t -> "WAITING".equals(t.getStatus()))
           .count();
   ```

5. **Optional**  
   Used to avoid `null` checks. Example from `UserRepository`:

   ```java
   Optional<User> findByEmail(String email);
   ```

6. **Records (Java 14+)**  
   Not used heavily in QueueLess, but can be used for simple DTOs. The project uses Java 25, so newer features are available.

**How to Get Started with Java**
- Install JDK 25 (or later) from [Oracle](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/).
- Use an IDE like IntelliJ IDEA, Eclipse, or VS Code with Java extensions.
- Understand the basic syntax: classes, methods, variables, control structures.

**Practice with QueueLess**
- Open `BackendApplication.java` – this is the entry point.
- Explore `User.java` to see a simple entity.
- Look at `QueueService.java` to see business logic with streams and optionals.

---

## 18.1.2 Introduction to Spring Boot

**What is Spring Boot?**  
Spring Boot is a framework built on top of the Spring Framework that simplifies the development of production‑ready applications. It provides:

- **Auto‑configuration** – automatically configures Spring based on the dependencies on the classpath.
- **Starter dependencies** – pre‑defined dependency sets (e.g., `spring-boot-starter-web` includes Tomcat, Jackson, etc.).
- **Embedded servers** – Tomcat, Jetty, or Undertow are embedded, so no need to deploy a WAR file.
- **Production‑ready features** – metrics, health checks, externalised configuration.

**Why Spring Boot for QueueLess?**
- **Rapid development**: We could focus on business logic instead of boilerplate.
- **Integration**: Works seamlessly with Spring Data MongoDB, Spring Security, WebSocket, and other projects.
- **Testing**: Built‑in support for integration tests with `@SpringBootTest`.
- **Configuration**: `application.properties` centralises all settings.

**How QueueLess Uses Spring Boot**

1. **Main Application Class** – `BackendApplication.java`:
   ```java
   @SpringBootApplication
   @EnableScheduling
   public class BackendApplication {
       public static void main(String[] args) {
           SpringApplication.run(BackendApplication.class, args);
       }
   }
   ```
    - `@SpringBootApplication` combines `@Configuration`, `@EnableAutoConfiguration`, and `@ComponentScan`.
    - `@EnableScheduling` enables scheduled tasks (e.g., `@Scheduled` methods).

2. **Starter Dependencies** in `pom.xml`:
    - `spring-boot-starter-web` – REST API.
    - `spring-boot-starter-data-mongodb` – MongoDB integration.
    - `spring-boot-starter-security` – Spring Security.
    - `spring-boot-starter-websocket` – WebSocket support.
    - `spring-boot-starter-mail` – JavaMail.
    - `spring-boot-starter-cache` – caching abstraction.
    - `spring-boot-starter-actuator` – monitoring endpoints.
    - `spring-boot-starter-test` – testing.

3. **Auto‑configuration**  
   Because we have `spring-boot-starter-data-mongodb` on the classpath, Spring Boot automatically creates a `MongoTemplate` bean and a `MongoClient` using the `spring.data.mongodb.uri` property. Similarly, it sets up `RedisCacheManager` when `spring-boot-starter-data-redis` is present.

4. **Properties** in `application.properties`:
   ```properties
   spring.data.mongodb.uri=${MONGODB_URI}
   server.ssl.key-store=classpath:localhost.p12
   jwt.secret=${JWT_SECRET}
   ```
    - Properties are externalised, making the application environment‑aware.

5. **Profiles** – Not extensively used, but can be added for different environments (dev, test, prod). For example, we could have `application-dev.properties` with different database URIs.

**How Spring Boot Starts**
1. `SpringApplication.run()` creates an `ApplicationContext`.
2. It scans the classpath for auto‑configuration candidates.
3. It creates beans defined in configuration classes (e.g., `FirebaseConfig`, `WebSocketConfig`).
4. It starts the embedded Tomcat server (or other) on the configured port.

**Important Spring Boot Concepts in QueueLess**

- **Configuration Classes** (`@Configuration`): Used to define custom beans (e.g., `RedisCacheConfig`, `WebSocketConfig`).
- **Properties Binding**: `@Value` injects values from properties. In `RateLimitFilter`, we inject `rate.limit.paths` as a list.
- **Actuator Endpoints**: Enabled by `spring-boot-starter-actuator`. The `/actuator/prometheus` endpoint is used by Prometheus to scrape metrics.
- **Logging**: Spring Boot configures logging (Logback) out of the box; we use `@Slf4j` and `log.info()`.

**Practice with QueueLess**
- Open `BackendApplication` and run the main method (the backend starts).
- Modify `application.properties` and observe how the behaviour changes (e.g., change the server port).
- Explore `pom.xml` to see all starter dependencies.

---

## 18.1.3 Dependency Injection and Annotations

**What is Dependency Injection?**  
Dependency Injection (DI) is a design pattern where objects receive their dependencies from an external source rather than creating them internally. In Spring, the IoC (Inversion of Control) container manages beans and injects them where needed.

**Why DI?**
- **Loose coupling**: Classes depend on abstractions, not concrete implementations.
- **Testability**: Dependencies can be mocked easily.
- **Maintainability**: Changing a dependency only requires updating the configuration, not the consuming code.

**How Spring Manages Beans**  
Spring scans the classpath for classes annotated with `@Component`, `@Service`, `@Repository`, `@Controller`, etc., and registers them as beans in the application context. It then resolves dependencies using constructor injection, setter injection, or field injection.

**Annotations Used in QueueLess**

1. **`@Service`** – Marks a class as a service bean (business logic). Examples: `QueueService`, `AuthService`, `AdminService`.

2. **`@Repository`** – Marks a class as a repository (data access). In QueueLess, we use Spring Data repositories, which are automatically implemented; the annotation is not needed on the interface, but we could use it on custom repository implementations (none).

3. **`@Controller`** – Marks a class as a Spring MVC controller. Examples: `AuthController`, `QueueController`, `AdminController`.

4. **`@RestController`** – A combination of `@Controller` and `@ResponseBody`; all methods return JSON by default. Used for all REST controllers.

5. **`@Configuration`** – Marks a class as a source of bean definitions. Examples: `SecurityConfig`, `WebSocketConfig`, `RedisCacheConfig`.

6. **`@Bean`** – Used inside a `@Configuration` class to explicitly define a bean. Example: `SecurityConfig` defines `PasswordEncoder`, `CorsConfigurationSource`, etc.

7. **`@Autowired`** – Tells Spring to inject a dependency. In QueueLess, we avoid field injection and use constructor injection via Lombok’s `@RequiredArgsConstructor`.

8. **`@RequiredArgsConstructor`** (Lombok) – Generates a constructor with required (final) fields. Spring automatically uses this constructor for injection. This is cleaner than `@Autowired` on fields.

**Example from `QueueService.java`**
```java
@Service
@RequiredArgsConstructor
public class QueueService {
    private final QueueRepository queueRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    // ...
}
```
- `@Service` tells Spring this is a service bean.
- `@RequiredArgsConstructor` creates a constructor with parameters for all final fields.
- Spring will create an instance of `QueueService` and inject `queueRepository`, `messagingTemplate`, etc., from the application context.

**Bean Scope**  
By default, Spring beans are singletons (one instance per application context). This is ideal for stateless services like `QueueService`. If we needed a new instance for each request, we could use `@Scope("prototype")`.

**Lombok’s Role**  
Lombok is a compile‑time library that generates boilerplate code (constructors, getters, setters, etc.) via annotations. In QueueLess, we use:

- `@Data` – generates getters, setters, `toString()`, `equals()`, `hashCode()`.
- `@Builder` – provides a builder pattern for constructing objects.
- `@NoArgsConstructor` / `@AllArgsConstructor` – generate empty/all‑args constructors.
- `@RequiredArgsConstructor` – as above.
- `@Slf4j` – adds a `log` field for logging.

**How Spring Injects Dependencies**  
When the application starts, Spring:

1. Scans the base package (configured by `@ComponentScan` on `@SpringBootApplication`).
2. Creates beans for all classes with `@Service`, `@Repository`, `@Controller`, etc.
3. For each bean, it looks for a constructor with parameters. If found, it resolves those parameters by looking up beans of matching types.
4. If a bean cannot be found, the application fails to start (this is good – we catch missing dependencies early).

**Why Constructor Injection?**
- **Immutability**: Dependencies can be `final`.
- **Testability**: Easy to pass mocks in unit tests.
- **No need for `@Autowired`**: Spring automatically uses the constructor.

**Example from `AuthService`** (uses `@RequiredArgsConstructor`):
```java
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    // ...
}
```
Spring injects all three repositories/services.

**Field Injection (Not Used)** – If we had used `@Autowired` on fields, we would write:
```java
@Autowired
private UserRepository userRepository;
```
This is less testable and makes the class harder to understand.

**Practice with QueueLess**
- Open `QueueService` and look at its constructor (implicitly generated).
- In `QueueController`, note that `QueueService` is injected via constructor.
- Check `SecurityConfig` for `@Bean` definitions (e.g., `PasswordEncoder`).

---

## 18.1.4 Creating REST Controllers

**What is a REST Controller?**  
In Spring Boot, a REST controller is a class that handles HTTP requests and returns data (usually JSON) directly to the client, without rendering a view. It is the entry point for the frontend to interact with the backend.

**Key Annotations**

- `@RestController` – Marks a class as a REST controller. It combines `@Controller` and `@ResponseBody`, meaning that the return value of each handler method is automatically written to the HTTP response body.
- `@RequestMapping` – Maps HTTP requests to handler methods. It can be used at class level (to define a base path) and at method level.
- `@GetMapping`, `@PostMapping`, `@PutMapping`, `@DeleteMapping`, `@PatchMapping` – Shortcuts for `@RequestMapping` with specific HTTP methods.
- `@PathVariable` – Binds a URI template variable to a method parameter.
- `@RequestParam` – Binds a query parameter to a method parameter.
- `@RequestBody` – Binds the HTTP request body to a method parameter (JSON to Java object).
- `@Valid` – Triggers validation on a request body (using Bean Validation annotations).

**Example: `QueueController`** (simplified)

```java
@RestController
@RequestMapping("/api/queues")
@RequiredArgsConstructor
@Tag(name = "Queues", description = "Queue management endpoints")
public class QueueController {
    private final QueueService queueService;

    @PostMapping("/create")
    @AdminOrProviderOnly
    public ResponseEntity<Queue> createNewQueue(@Valid @RequestBody CreateQueueRequest request) {
        String providerId = SecurityContextHolder.getContext().getAuthentication().getName();
        Queue newQueue = queueService.createNewQueue(providerId, request.getServiceName(), ...);
        return ResponseEntity.status(HttpStatus.CREATED).body(newQueue);
    }

    @GetMapping("/{queueId}")
    public ResponseEntity<Queue> getQueueById(@PathVariable String queueId) {
        Queue queue = queueService.getQueueById(queueId);
        return ResponseEntity.ok(queue);
    }

    @PostMapping("/{queueId}/add-token")
    @UserOnly
    public ResponseEntity<QueueToken> addTokenToQueue(@PathVariable String queueId) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        QueueToken token = queueService.addNewToken(queueId, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(token);
    }
}
```

**How It Works**

1. **Class‑level `@RequestMapping("/api/queues")`** – All methods in this controller have paths relative to `/api/queues`.
2. **`createNewQueue`** – Maps to `POST /api/queues/create`. The `@Valid` ensures the `CreateQueueRequest` is validated (e.g., `@NotBlank` fields). The `@RequestBody` tells Spring to read the JSON body and deserialize it into a `CreateQueueRequest` object.
3. **`getQueueById`** – Maps to `GET /api/queues/{queueId}`. The `{queueId}` placeholder is bound to the `queueId` method parameter via `@PathVariable`.
4. **`addTokenToQueue`** – Maps to `POST /api/queues/{queueId}/add-token`. The `{queueId}` is again a path variable. No request body is needed.
5. **Return values** – The method returns a `ResponseEntity` which allows setting status code, headers, and body. Alternatively, you can return the object directly, and Spring defaults to HTTP 200 OK.

**Path Variables vs Query Parameters**

- **Path variable** – Part of the URL, used to identify a resource (e.g., `/users/{id}`).
- **Query parameter** – After `?`, used for filtering, pagination, etc. (e.g., `/users?page=0&size=20`).

In QueueLess, path variables are used for IDs, and query parameters are used for optional parameters (e.g., `days` in analytics endpoints).

**JSON to Java Conversion (Jackson)**

Spring Boot includes Jackson, which automatically converts:
- Incoming JSON to Java objects (when `@RequestBody` is used).
- Java objects to JSON in the response (when the method returns an object or `ResponseEntity`).

Example: A `CreateQueueRequest` sent as JSON:
```json
{
  "serviceName": "Test Queue",
  "placeId": "place123",
  "serviceId": "service123",
  "maxCapacity": 10
}
```
Jackson will map this to a `CreateQueueRequest` instance with fields populated.

**Validation**

When `@Valid` is used, Spring checks the constraints defined in the DTO (e.g., `@NotBlank`, `@Min`). If validation fails, a `MethodArgumentNotValidException` is thrown. Our `GlobalExceptionHandler` catches it and returns a structured error response:

```json
{
  "status": 400,
  "error": "Validation Failed",
  "message": "Validation failed for one or more fields",
  "fieldErrors": {
    "serviceName": "Service name is required"
  }
}
```

**Security Annotations**

The controller uses custom annotations like `@AdminOrProviderOnly` and `@UserOnly`. These are `@PreAuthorize` annotations that restrict access based on the authenticated user’s role. For example:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN') or hasRole('PROVIDER')")
public @interface AdminOrProviderOnly {}
```

**Exception Handling**

The `GlobalExceptionHandler` (`@RestControllerAdvice`) catches exceptions thrown in controllers and maps them to appropriate HTTP responses. It handles:
- `ResourceNotFoundException` → 404
- `UserAlreadyInQueueException` → 409
- `MethodArgumentNotValidException` → 400 with field errors
- Other runtime exceptions → 400 or 500

**Practice with QueueLess**

1. Open `QueueController.java` and identify each method’s mapping, path variables, and request bodies.
2. In `PlaceController.java`, see how `@Valid` is used on `PlaceDTO` and how validation errors are handled.
3. Create a new request in Postman to `POST /api/queues/create` and see the response when required fields are missing.
4. Try to access a protected endpoint without authentication – you should receive a 401 or 403.

---

## 18.1.5 Spring Data MongoDB

**What is Spring Data MongoDB?**  
Spring Data MongoDB is a module that simplifies working with MongoDB in a Spring application. It provides a high‑level abstraction over the MongoDB Java driver, allowing you to interact with the database using repository interfaces and POJOs (Plain Old Java Objects) mapped to collections.

**Key Concepts**

- **Entities**: Classes annotated with `@Document` that map to MongoDB collections.
- **Repositories**: Interfaces that extend `MongoRepository<T, ID>` and provide CRUD operations automatically.
- **Query methods**: Methods named according to conventions (e.g., `findByProviderId`) that Spring implements automatically.
- **Custom queries**: Using `@Query` to write MongoDB JSON queries.
- **Aggregation**: Using `MongoTemplate` and `Aggregation` classes for advanced data processing.

**How QueueLess Uses Spring Data MongoDB**

1. **Entities (`@Document`)**  
   Each entity corresponds to a MongoDB collection. For example, `User.java`:

   ```java
   @Document("users")
   @Data
   @Builder
   public class User {
       @Id
       private String id;
       @Indexed(unique = true)
       private String email;
       // ...
   }
   ```
    - `@Document("users")` tells Spring this class maps to the `users` collection.
    - `@Id` marks the primary key.
    - `@Indexed` creates database indexes (e.g., unique on email).

2. **Repositories**  
   Each entity has a repository interface that extends `MongoRepository`. For example, `QueueRepository`:

   ```java
   public interface QueueRepository extends MongoRepository<Queue, String> {
       List<Queue> findByProviderId(String providerId);
       List<Queue> findByPlaceId(String placeId);
       List<Queue> findByServiceId(String serviceId);
       List<Queue> findByIsActive(boolean isActive);
       List<Queue> findByPlaceIdIn(List<String> placeIds);
   }
   ```
    - `MongoRepository` provides methods like `findById`, `save`, `delete`, `count`, etc.
    - Spring automatically implements the custom query methods based on the method name (e.g., `findByProviderId` translates to a query on the `providerId` field).
    - `findByPlaceIdIn` takes a list of place IDs and returns all queues whose `placeId` is in the list.

3. **Custom Queries with `@Query`**  
   For complex queries, we can write MongoDB JSON queries directly. Example from `PlaceRepository`:

   ```java
   @Query("{ 'location' : { $near : { $geometry : { type : 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
   List<Place> findByLocationNear(Double longitude, Double latitude, Double maxDistance);
   ```
    - The `?0`, `?1`, `?2` are placeholders for method parameters.
    - This uses MongoDB’s `$near` geospatial operator.

4. **Aggregation Framework**  
   For advanced queries (e.g., search across services and places), we use `MongoTemplate` and `Aggregation`. Example from `SearchService`:

   ```java
   Aggregation agg = Aggregation.newAggregation(
       Aggregation.match(Criteria.where("placeId").in(placeIds)),
       Aggregation.match(Criteria.where("name").regex(request.getQuery(), "i")),
       Aggregation.lookup("places", "placeId", "_id", "place"),
       Aggregation.unwind("place"),
       Aggregation.project()
           .and("_id").as("id")
           .and("name").as("name")
           .and("place.name").as("placeName"),
       Aggregation.sort(Sort.by(Sort.Direction.ASC, "name")),
       Aggregation.skip(pageable.getOffset()),
       Aggregation.limit(pageable.getPageSize())
   );
   AggregationResults<ServiceSearchResult> results = mongoTemplate.aggregate(agg, "services", ServiceSearchResult.class);
   ```
    - This performs a `$lookup` to join services with places, then projects only the needed fields.

5. **Transactions**  
   MongoDB supports multi‑document transactions, but we don’t use them because our operations are mostly atomic at the document level. For resetting a queue and clearing user tokens, we perform multiple saves, but if one fails, we don’t roll back; this is acceptable because the operation can be retried.

**How Spring Data MongoDB Works Under the Hood**

- **Repository proxy**: When you define an interface extending `MongoRepository`, Spring creates a proxy at runtime that implements the interface using a `MongoTemplate` instance.
- **Query derivation**: For methods like `findByProviderId`, Spring parses the method name, builds a `Criteria` object, and executes a query.
- **Conversion**: Spring uses a `MappingMongoConverter` to convert between Java objects and MongoDB documents. It handles nested objects, `@Field` renaming, etc.

**Important Annotations in QueueLess**

- `@Document` – Marks an entity.
- `@Id` – Marks the ID field.
- `@Indexed` – Creates an index for faster queries.
- `@CompoundIndex` – Creates a compound index (e.g., in `NotificationPreference`, we have a unique compound index on `userId` and `queueId`).
- `@Field` – Specifies a different field name in MongoDB (e.g., `@Field("tokens.userId")` in `Queue`).
- `@GeoSpatialIndexed` – Creates a 2dsphere index for geospatial queries (used on `Place.location`).

**Best Practices**

- **Use indexes wisely**: The `@Indexed` annotations ensure that queries are fast. We have indexes on frequently queried fields (email, placeId, providerId, etc.).
- **Embedded documents**: When data is always accessed together (tokens inside a queue), embed it. When it grows large or is accessed independently, use references.
- **Batch operations**: For bulk inserts (e.g., seeding data), use `MongoTemplate.insert` with a list.
- **Aggregation**: Use for complex reporting; ensure it doesn’t become a performance bottleneck by using `$match` early.

**Practice with QueueLess**

1. Open `QueueRepository.java` and note the custom methods.
2. In `QueueService`, observe how `queueRepository.save()` is used to persist a queue.
3. In `PlaceService`, see how `placeRepository.searchNearbyWithFilters` uses `@Query` for geospatial search.
4. Run the application and use MongoDB Compass to view the collections and indexes created automatically.

---

## 18.1.6 Spring Security & JWT

**What is Spring Security?**  
Spring Security is a powerful framework for authentication (who you are) and authorisation (what you can do) in Spring applications. It provides comprehensive protection against common attacks (CSRF, session fixation, etc.) and integrates seamlessly with Spring Boot.

**What is JWT?**  
JSON Web Token (JWT) is a compact, URL‑safe token format used to securely transmit information between parties. It consists of three parts: header, payload, and signature. The payload contains claims (e.g., user ID, role, email). The token is signed with a secret key, ensuring it cannot be tampered with.

**Why JWT for QueueLess?**
- **Stateless**: No server‑side session storage, making horizontal scaling easy.
- **Self‑contained**: The token holds the user’s identity and roles, so the database doesn’t need to be queried for every request (though we still query for some data).
- **Mobile/Web friendly**: Works well with SPAs and mobile apps.

**How Spring Security is Configured**

1. **Dependencies** – `spring-boot-starter-security` brings in everything.
2. **SecurityConfig.java** – The main configuration class:
   ```java
   @Configuration
   @EnableWebSecurity
   @EnableMethodSecurity(prePostEnabled = true)
   public class SecurityConfig {
       @Bean
       public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
           http
               .cors(cors -> cors.configurationSource(corsConfigurationSource()))
               .csrf(csrf -> csrf.disable())
               .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
               .authorizeHttpRequests(auth -> auth
                   .requestMatchers("/api/auth/**", "/api/password/**", "/api/public/**", "/ws/**").permitAll()
                   .requestMatchers(HttpMethod.GET, "/api/places/**").permitAll()
                   .requestMatchers("/api/admin/**").hasRole("ADMIN")
                   .anyRequest().authenticated()
               )
               .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
           return http.build();
       }
   }
   ```
    - **CORS**: Allows requests from the frontend origin.
    - **CSRF**: Disabled because we use stateless JWT.
    - **Session management**: Stateless – no sessions.
    - **Authorisation**: Public endpoints are permitted; others require authentication; admin endpoints require `ROLE_ADMIN`.
    - **Filter**: Adds `JwtAuthenticationFilter` before the standard `UsernamePasswordAuthenticationFilter`.

3. **JwtTokenProvider.java** – Handles token generation and validation:
   ```java
   public String generateToken(User user) {
       Map<String, Object> claims = new HashMap<>();
       claims.put("sub", user.getId());
       claims.put("role", user.getRole().name());
       claims.put("email", user.getEmail());
       // ...
       return Jwts.builder()
           .setClaims(claims)
           .setIssuedAt(new Date())
           .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
           .signWith(getSigningKey(), SignatureAlgorithm.HS256)
           .compact();
   }
   ```

4. **JwtAuthenticationFilter.java** – Intercepts each request, extracts the token, validates it, and sets the `SecurityContext`:
   ```java
   protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
       String authHeader = request.getHeader("Authorization");
       if (authHeader != null && authHeader.startsWith("Bearer ")) {
           String token = authHeader.substring(7);
           if (jwtProvider.validateToken(token)) {
               String userId = jwtProvider.getUserIdFromToken(token);
               Collection<GrantedAuthority> authorities = jwtProvider.getAuthoritiesFromToken(token);
               UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
               SecurityContextHolder.getContext().setAuthentication(auth);
           }
       }
       chain.doFilter(request, response);
   }
   ```

5. **Method‑level security** – Enabled with `@EnableMethodSecurity`. Custom annotations like `@AdminOnly` are defined:
   ```java
   @Target(ElementType.METHOD)
   @Retention(RetentionPolicy.RUNTIME)
   @PreAuthorize("hasRole('ADMIN')")
   public @interface AdminOnly {}
   ```
   These are used in controllers:
   ```java
   @PostMapping("/places")
   @AdminOnly
   public ResponseEntity<PlaceDTO> createPlace(@Valid @RequestBody PlaceDTO placeDTO) {
       // only admins can access
   }
   ```

**WebSocket Security**

WebSocket connections require a separate security interceptor because they don’t go through the normal HTTP filter chain.

- **StompJwtChannelInterceptor.java** – Intercepts the CONNECT frame, extracts the JWT from the `Authorization` header, and sets the user in the `StompHeaderAccessor`. This user is then used for user‑specific subscriptions (`/user/queue/...`).
- **WebSocketSecurityConfig.java** – Configures authorisation for subscriptions and messages:
  ```java
  @Override
  protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
      messages
          .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.HEARTBEAT).permitAll()
          .simpSubscribeDestMatchers("/user/**", "/topic/queues/*").authenticated()
          .anyMessage().denyAll();
  }
  ```

**Password Encoding**

- `PasswordEncoder` is a `BCryptPasswordEncoder` with strength 10. It hashes passwords before storing them and verifies them during login.

**Role Hierarchy**

Not implemented, but could be added if needed (e.g., `ADMIN > PROVIDER > USER`). Currently, we use explicit role checks.

**Important Points to Remember**

- JWT secret is stored in environment variables, never hard‑coded.
- Tokens have a short expiration (24 hours) to limit exposure.
- `SecurityContext` is thread‑local; cleared after request.
- When a user logs out, the frontend discards the token; the backend does nothing.

**How to Test Security in Integration Tests**

We use `@WithMockUser` to simulate authenticated users with specific roles. Example:

```java
@Test
@WithMockUser(username = "admin", roles = {"ADMIN"})
void createPlace_Success() throws Exception {
    // ...
}
```

**Practice with QueueLess**

1. Open `SecurityConfig` and examine the `filterChain` bean.
2. In `JwtTokenProvider`, see how the token is built and signed.
3. In `AuthService.login()`, note that we generate the token after validating credentials.
4. Add a temporary test endpoint with `@AdminOnly` and try to access it without authentication – you should get a 403.

---

## 18.1.7 Spring WebSocket

**What is WebSocket?**  
WebSocket is a communication protocol that provides full‑duplex, persistent connections between a client and a server. Unlike HTTP (which is request‑response), WebSocket allows the server to push data to the client at any time, enabling real‑time updates.

**Why WebSocket for QueueLess?**  
Queue status changes frequently (tokens added, served, cancelled). Using WebSocket, the server can immediately push the updated queue to all connected clients (users, providers, admins) without polling. This results in a smooth, live experience.

**STOMP (Simple Text Oriented Messaging Protocol)**  
STOMP is a messaging protocol that works over WebSocket. It provides a simple frame‑based format (similar to HTTP) and includes concepts like destinations (topics), subscriptions, and messages. Spring uses STOMP to build a message‑broker abstraction.

**Spring WebSocket Configuration** (`WebSocketConfig.java`)

```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("https://localhost:5173")
                .withSockJS();
    }
}
```
- **`@EnableWebSocketMessageBroker`** – Enables WebSocket message handling.
- **`configureMessageBroker`** – Sets up a simple in‑memory message broker for `/topic` (broadcast) and `/queue` (point‑to‑point). Messages sent to `/app` are routed to controller methods. `/user` is used for user‑specific destinations.
- **`registerStompEndpoints`** – Registers the `/ws` endpoint. SockJS provides fallback for browsers that don’t support WebSocket.

**Handling Messages** (`QueueWebSocketController.java`)

```java
@Controller
public class QueueWebSocketController {
    private final SimpMessagingTemplate messagingTemplate;
    private final QueueService queueService;

    @MessageMapping("/queue/serve-next")
    @AdminOrProviderOnly
    public void serveNext(@Payload ServeNextRequest request, Authentication authentication) {
        Queue updatedQueue = queueService.serveNextToken(request.getQueueId());
        if (updatedQueue != null) {
            messagingTemplate.convertAndSend("/topic/queues/" + request.getQueueId(), updatedQueue);
            messagingTemplate.convertAndSendToUser(authentication.getName(), "/queue/provider-updates", updatedQueue);
        }
    }
}
```
- **`@MessageMapping`** – Maps a destination like `/app/queue/serve-next` (the `/app` prefix is stripped). The payload is automatically deserialised into a `ServeNextRequest`.
- **`SimpMessagingTemplate`** – Used to send messages to clients. `convertAndSend` sends to a topic, `convertAndSendToUser` sends to a specific user’s queue.

**Subscriptions in Frontend**  
The frontend connects to `/ws` and subscribes to a topic, e.g., `/topic/queues/123`. When the backend sends a message to that topic, the frontend receives it and updates the UI.

**User Destinations**  
Destinations starting with `/user/` are translated to a user‑specific queue. For example, a message sent to `/user/queue/emergency-approved` will be delivered only to the user with that ID.

**Security** (`StompJwtChannelInterceptor.java`)  
During the STOMP CONNECT frame, the client includes an `Authorization` header with the JWT. The interceptor validates it and sets the user in the `StompHeaderAccessor`. Spring then uses this user for subscriptions and user‑specific messaging.

**WebSocket Security Config** (`WebSocketSecurityConfig.java`)
- Uses `AbstractSecurityWebSocketMessageBrokerConfigurer` to define access rules for inbound messages.
- Permits CONNECT, HEARTBEAT, UNSUBSCRIBE, etc., but requires authentication for subscriptions to `/user/**` and `/topic/queues/*`.

**Why Use a Simple Broker Instead of a Full Message Broker (e.g., RabbitMQ)?**  
For the current scale, an in‑memory broker is sufficient. It’s lightweight and easy to configure. As the system grows, we could switch to a full broker like RabbitMQ or ActiveMQ by changing the configuration.

**Practice with QueueLess**

1. Run the backend and frontend. Open the browser’s Developer Tools (Network tab) and filter for WebSocket connections. Observe the connection to `ws://localhost:8443/ws`.
2. Join a queue as a user. In the Network tab, you should see WebSocket frames arriving with queue updates.
3. In the backend, add a log statement in `QueueWebSocketController.serveNext` to see when messages are sent.
4. Open multiple browser windows (incognito) to simulate multiple users and see live updates.

---

## 18.1.8 Spring Scheduling & Caching

**Scheduling**  
Spring’s `@Scheduled` annotation allows you to run methods at fixed intervals or cron expressions. In QueueLess, several background tasks are scheduled to keep data fresh, send notifications, and maintain analytics.

**Enabling Scheduling**  
Add `@EnableScheduling` to the main application class (or any `@Configuration` class). In QueueLess, `BackendApplication` has it.

**Example Scheduler: TokenNotificationScheduler**
```java
@Component
@RequiredArgsConstructor
public class TokenNotificationScheduler {
    private final QueueRepository queueRepository;
    private final EmailService emailService;
    private final FcmService fcmService;

    @Scheduled(fixedRate = 60000) // runs every minute
    public void checkUpcomingTokens() {
        List<Queue> allQueues = queueRepository.findAll();
        for (Queue queue : allQueues) {
            // process tokens
        }
    }
}
```
- `@Scheduled(fixedRate = 60000)` – executes the method every 60 seconds, regardless of how long the method takes (if you want to wait for completion, use `fixedDelay`).
- Other options: `@Scheduled(cron = "0 0 * * * *")` – runs at the start of every hour.

**Other Schedulers in QueueLess**
- `BestTimeNotificationScheduler` – hourly, checks for short queues.
- `MetricsScheduler` – every 15 seconds, updates Prometheus metrics.
- `QueueAnalyticsScheduler` – hourly, takes snapshots of waiting counts.
- `AlertScheduler` – every 5 minutes, checks wait time thresholds.
- `QueueService.cleanupExpiredTokens` – every hour, removes old tokens.
- `QueueService.updateAllQueueWaitTimes` – every 30 seconds, recalculates wait times.

**Caching**  
Caching stores frequently accessed data in memory (or Redis) to reduce database load. Spring provides a cache abstraction that works with different providers (Redis, Caffeine, etc.).

**Enabling Caching**  
Add `@EnableCaching` to a configuration class. In QueueLess, `RedisCacheConfig` has `@EnableCaching`.

**Cache Annotations**
- `@Cacheable` – caches the result of a method. Subsequent calls with the same arguments return the cached value.
- `@CacheEvict` – removes entries from the cache (usually after updates).
- `@CachePut` – updates the cache with the method result (rarely used).

**Example from `PlaceService`**
```java
@Cacheable(value = "places", key = "#id")
public Place getPlaceById(String id) {
    return placeRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Place not found"));
}

@CacheEvict(value = {"places", "placesByAdmin"}, allEntries = true)
public Place updatePlace(String id, PlaceDTO placeDTO) {
    // update logic
}
```
- `getPlaceById` is cached. The first call hits the database; subsequent calls use the cache.
- `updatePlace` evicts all entries from `places` and `placesByAdmin` caches, ensuring stale data is cleared.

**Redis Configuration** (`RedisCacheConfig.java`)
```java
@Bean
public RedisCacheConfiguration cacheConfiguration() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(new GeoJsonModule());
    objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    objectMapper.activateDefaultTyping(...);

    return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
}
```
- Sets a default TTL of 10 minutes.
- Uses JSON serialisation so that objects can be stored and retrieved with type information (important for polymorphic types like `GeoJsonPoint`).

**Cache Manager** – Spring creates a `RedisCacheManager` bean automatically when `spring-boot-starter-data-redis` is present. We customise it with our `RedisCacheConfig`.

**Why Use Redis?**
- Distributed – if we have multiple backend instances, they all share the same cache.
- Persistent – Redis can persist to disk, though we don’t need it for caching.
- Fast – in‑memory storage.

**Practice with QueueLess**
1. Enable debug logging for cache: add `logging.level.org.springframework.cache=DEBUG` in `application.properties`.
2. Call `GET /api/places/{id}` twice in a row and observe the logs – the first call hits the database, the second hits the cache.
3. Update a place (PUT) and then call `GET /api/places/{id}` again – the cache should be evicted and the new value fetched from the database.

**Interview Questions**
- *How do you decide what to cache?* – Data that is read often and changes infrequently, like place details and lists.
- *How do you handle cache invalidation?* – Use `@CacheEvict` after updates. For some caches, we evict all entries (`allEntries = true`) because the number of entries is small.
- *What if Redis goes down?* – The application continues to work; it simply won’t cache. We can fall back to an in‑memory cache like Caffeine, but we haven’t implemented that.

---

## 18.1.9 Configuration & Profiles

**What is Spring Boot Configuration?**  
Spring Boot externalises configuration using properties files, YAML, environment variables, or command‑line arguments. This allows you to run the same application in different environments (development, test, production) without changing code.

**Configuration Sources** (in order of precedence, highest first)
1. Command‑line arguments (`--server.port=8080`)
2. Environment variables (`MONGODB_URI`)
3. `application-{profile}.properties` (e.g., `application-prod.properties`)
4. `application.properties` (or `application.yml`)
5. Default values

**Properties in QueueLess**  
All properties are stored in `application.properties`. Sensitive values (database URIs, secrets) are injected via environment variables using `${...}` syntax:

```properties
spring.data.mongodb.uri=${MONGODB_URI}
jwt.secret=${JWT_SECRET}
jwt.expiration=${JWT_EXPIRATION}
```

This keeps secrets out of version control.

**Profiles**  
A profile is a named set of configuration. You can activate profiles via `spring.profiles.active`. For example, to run with the `dev` profile: `java -jar app.jar --spring.profiles.active=dev`. Then Spring will load `application-dev.properties` on top of `application.properties`.

QueueLess does not currently define custom profiles, but it could easily be extended. For example:

- `application-dev.properties` – local MongoDB, Redis, debug logging.
- `application-prod.properties` – production database, reduced logging, different SSL settings.

**Using `@Value`**  
Properties can be injected into beans using `@Value`. Example from `RateLimitFilter`:

```java
@Value("${rate.limit.capacity:100}")
private int capacity;
```

The `:100` is a default value if the property is not set.

**`@ConfigurationProperties`**  
A type‑safe alternative to `@Value`. You can bind a group of properties to a POJO. Example (not used in QueueLess, but could be added):

```java
@ConfigurationProperties(prefix = "rate.limit")
@Data
public class RateLimitProperties {
    private int capacity = 100;
    private int refill = 100;
    private Duration duration = Duration.ofMinutes(1);
}
```

Then in a configuration class, add `@EnableConfigurationProperties(RateLimitProperties.class)`.

**How QueueLess Uses Configuration**

- **`application.properties`** – Holds all settings, with placeholders for secrets.
- **`@Value`** – Used in `RateLimitConfig`, `RateLimitFilter`, `FileStorageService`, etc.
- **`@ConfigurationProperties`** – Not used, but could improve readability.
- **Environment Variables** – The `.env` file (or Docker environment) sets `MONGODB_URI`, `JWT_SECRET`, etc. These are passed to the Spring Boot process.

**External Configuration in Docker**  
In `docker-compose.yml`, environment variables are passed to the backend container:

```yaml
environment:
  - REDIS_HOST=redis
  - MONGODB_URI=${MONGODB_URI}
  - JWT_SECRET=${JWT_SECRET}
```

The values are read from the host’s environment (or a `.env` file) and injected.

**Best Practices**
- **Never commit secrets** – Use environment variables or a secrets management service.
- **Use profiles for environment‑specific settings** – For example, different database URIs, log levels, cache TTLs.
- **Keep default values in `application.properties`** – So the app can run without environment variables (useful for local development).
- **Use `@ConfigurationProperties` for groups of related properties** – It makes the code cleaner and easier to test.

**Practice with QueueLess**

1. Run the backend with a different port: `java -jar backend.jar --server.port=8080`. Observe that it now runs on 8080.
2. Create an `application-dev.properties` file in `src/main/resources` with `server.port=8081`. Run with `--spring.profiles.active=dev` and see that it picks up the new port.
3. In `RateLimitConfig`, add a `@Value` property and change its value in `application.properties`. See the effect on rate limiting.

---

## 18.2 MongoDB

### 18.2.1 Document Model & Collections

**What is MongoDB?**  
MongoDB is a NoSQL document database that stores data in flexible, JSON‑like documents (BSON). Unlike relational databases, it does not require a fixed schema, and documents can contain nested structures.

**Why Document Model for QueueLess?**
- **Hierarchical data**: Tokens are naturally nested inside a queue. Embedding them avoids separate tables and joins.
- **Flexible schema**: Fields like `isActive` can be added later without migrations.
- **Geospatial support**: Built‑in 2dsphere indexes for location‑based search.
- **Performance**: Queries on a single document are very fast.

**Collections in QueueLess**  
MongoDB stores documents in collections (like tables in SQL). Each collection holds documents that represent a similar entity. In QueueLess, we have:

- `users` – user accounts (USER, PROVIDER, ADMIN)
- `places` – physical locations
- `services` – services offered at a place
- `queues` – queues with embedded tokens
- `feedback` – user reviews
- `tokens` – payment tokens for registration
- `payments` – Razorpay order records
- `otp` – temporary OTPs
- `audit_logs` – business audit trail
- `notification_preferences` – per‑queue user settings
- `password_reset_tokens` – admin‑initiated reset tokens
- `queue_hourly_stats` – analytics snapshots
- `alert_configs` – admin alert thresholds

**Document Structure Example: `queues` Collection**  
A queue document might look like this:

```json
{
  "_id": ObjectId("..."),
  "providerId": ObjectId("..."),
  "placeId": ObjectId("..."),
  "serviceId": ObjectId("..."),
  "serviceName": "Cardiology",
  "isActive": true,
  "estimatedWaitTime": 12,
  "maxCapacity": 50,
  "tokens": [
    {
      "tokenId": "queue123-T-001",
      "userId": ObjectId("..."),
      "userName": "John Doe",
      "status": "WAITING",
      "issuedAt": ISODate("..."),
      "priority": 0,
      "isEmergency": false
    },
    {
      "tokenId": "queue123-T-002",
      "userId": ObjectId("..."),
      "userName": "Jane Smith",
      "status": "IN_SERVICE",
      "issuedAt": ISODate("..."),
      "servedAt": ISODate("...")
    }
  ]
}
```

**Embedding vs. Referencing**
- **Embedding** (used for `tokens` inside `queues`) – tokens are only meaningful in the context of their queue; they are always accessed together.
- **Referencing** (used for `placeId` in `queues`) – places are independent entities and are queried separately. A reference (ObjectId) keeps them linked.

**Why embed tokens?**
- Tokens are never accessed without their queue.
- Updating a queue (adding a token, serving a token) is atomic.
- No need for a separate collection and joins.

**Why reference places in queues?**
- Places are independent (they have their own details like address, images).
- Many queues can reference the same place.
- We often fetch place details separately (e.g., on the place page).

**Unique Constraints**
- `users.email` is unique – ensured by `@Indexed(unique = true)`.
- `tokens.tokenValue` is unique – to prevent duplicate tokens.
- `feedback.tokenId` is unique – one feedback per token.

**Practice with QueueLess**
1. Open MongoDB Compass and connect to the running database. Browse the `queues` collection to see embedded tokens.
2. Run the application and create a place, service, and queue. Check how the document is stored.
3. In `User.java`, note the `@Indexed(unique = true)` on `email`. This creates a unique index in MongoDB.

---

## 18.2.2 CRUD Operations with Spring Data

**Repository Abstraction**  
Spring Data MongoDB provides a repository abstraction that greatly simplifies data access. You define an interface that extends `MongoRepository<T, ID>`, and Spring automatically provides implementations for common CRUD operations (save, findById, findAll, delete, etc.). You can also define custom query methods by following naming conventions.

**Basic Repository Interface**  
In QueueLess, each entity has a repository interface. For example, `QueueRepository`:

```java
public interface QueueRepository extends MongoRepository<Queue, String> {
    List<Queue> findByProviderId(String providerId);
    List<Queue> findByPlaceId(String placeId);
    List<Queue> findByServiceId(String serviceId);
    List<Queue> findByIsActive(boolean isActive);
    List<Queue> findByPlaceIdIn(List<String> placeIds);
}
```
- `MongoRepository<Queue, String>` gives us methods like `save(Queue)`, `findById(String)`, `findAll()`, `deleteById(String)`, etc.
- Custom methods like `findByProviderId` are automatically implemented based on the method name. Spring parses the name, derives the query (e.g., `providerId` field), and executes it.

**Naming Conventions**  
The method name must follow a pattern: `findBy` + property name + optional keyword (`And`, `Or`, `In`, `Like`, etc.). Examples:
- `findByProviderId(String providerId)` – finds all queues where `providerId` matches.
- `findByPlaceIdIn(List<String> placeIds)` – finds all queues whose `placeId` is in the given list.

**Saving Documents**  
Saving is done via `repository.save(entity)`. This inserts if the entity has no `@Id` or updates if the ID exists. Example from `QueueService`:

```java
Queue newQueue = new Queue(providerId, serviceName, placeId, serviceId);
newQueue = queueRepository.save(newQueue);
```

**Updating Documents**  
To update, you modify the entity and call `save` again. Since `save` is an upsert (update if exists, insert otherwise), you must ensure the entity has the correct ID. Example from `QueueService.completeToken`:

```java
QueueToken token = ...; // fetched from queue
token.setStatus(TokenStatus.COMPLETED.toString());
token.setCompletedAt(LocalDateTime.now());
queueRepository.save(queue); // saves the whole queue document with the updated token
```

**Deleting Documents**  
`repository.deleteById(id)` or `repository.delete(entity)`. Example from `PlaceService.deletePlace`:

```java
Place place = getPlaceById(id);
placeRepository.delete(place);
```

**Custom Queries with `@Query`**  
When the method name is insufficient, you can write a MongoDB JSON query using `@Query`. Example from `PlaceRepository`:

```java
@Query("{ 'location' : { $near : { $geometry : { type : 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
List<Place> findByLocationNear(Double longitude, Double latitude, Double maxDistance);
```
- The placeholders `?0`, `?1`, `?2` correspond to method parameters.

**Using `MongoTemplate`**  
For complex operations (like aggregations) that aren't covered by repositories, you can inject `MongoTemplate`. It provides low‑level access to MongoDB. Example from `SearchService`:

```java
Aggregation agg = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("placeId").in(placeIds)),
    Aggregation.match(Criteria.where("name").regex(request.getQuery(), "i")),
    Aggregation.lookup("places", "placeId", "_id", "place"),
    Aggregation.unwind("place"),
    Aggregation.project().and("_id").as("id").and("name").as("name"),
    Aggregation.skip(pageable.getOffset()),
    Aggregation.limit(pageable.getPageSize())
);
AggregationResults<ServiceSearchResult> results = mongoTemplate.aggregate(agg, "services", ServiceSearchResult.class);
```

**Transactions**  
MongoDB supports multi‑document transactions, but QueueLess does not use them because most operations are atomic at the document level. For operations that span multiple documents (e.g., resetting a queue and clearing user tokens), we perform them in sequence; if one fails, the operation can be retried. In a future version, we could use `@Transactional` with MongoDB transactions.

**Practice with QueueLess**
1. In `QueueService.createNewQueue`, observe how a new queue is saved using `queueRepository.save`.
2. In `QueueService.completeToken`, see that we modify the queue and then save it.
3. Open `PlaceRepository` and examine the `@Query` annotation for nearby search.
4. In `SearchService`, see how `MongoTemplate` is used for complex aggregations.

---

## 18.2.3 Indexing & Performance

**Why Indexes?**  
Indexes are data structures that improve the speed of read operations. Without indexes, MongoDB must perform a collection scan (examine every document) to find matches. With indexes, it can locate documents quickly, similar to using an index in a book.

**Cost of Indexes**
- **Write performance**: Each insert, update, or delete also updates the indexes.
- **Storage space**: Indexes consume disk space.
- **Maintenance**: Over‑indexing can hurt performance.

**Indexes in QueueLess**

We define indexes using annotations in the entity classes. Spring Data MongoDB creates these indexes automatically when the application starts (if they don’t already exist).

**1. Unique Indexes**
- `@Indexed(unique = true)` on `User.email` – ensures no duplicate emails and speeds up login.

**2. Single‑Field Indexes**
- `@Indexed` on `Place.type`, `Place.rating`, `Place.adminId`, `Queue.providerId`, `Queue.placeId`, `Queue.serviceId`, `Queue.isActive`, `Feedback.placeId`, `Feedback.providerId`, etc. These speed up filtering and sorting.

**3. Compound Indexes**
- `@CompoundIndex(name = "place_admin_location", def = "{'adminId': 1, 'location': '2dsphere'}")` on `Place` – supports queries that filter by admin and then by location.
- `@CompoundIndex(name = "user_queue_idx", def = "{'userId': 1, 'queueId': 1}", unique = true)` on `NotificationPreference` – ensures one preference per user per queue and speeds up lookups.

**4. Geospatial Index**
- `@GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)` on `Place.location` – enables efficient $near queries.

**5. Multikey Index**
- Compound index on `tokens.userId` in the `Queue` collection – this is automatically created by the `@CompoundIndex(name = "tokens_userId_idx", def = "{'tokens.userId': 1}")` in `Queue.java`. It allows fast lookups for `getQueuesByUserId`.

**6. TTL Indexes**
- `@Indexed(expireAfter = "300s")` on `OtpDocument.expiryTime` – automatically deletes expired OTPs.

**How Indexes Are Used in Queries**

- **Login**: `db.users.find({ email: "user@example.com" })` uses the unique index on `email`.
- **Nearby places**: `db.places.find({ location: { $near: ... } })` uses the 2dsphere index.
- **Provider queues**: `db.queues.find({ providerId: ObjectId(...) })` uses the index on `providerId`.
- **User queues**: `db.queues.find({ "tokens.userId": ObjectId(...) })` uses the multikey index on `tokens.userId`.

**Checking Index Usage**  
You can use MongoDB’s `explain()` to see whether an index was used. For example, in MongoDB Compass or the mongo shell:

```javascript
db.queues.find({ providerId: ObjectId("...") }).explain("executionStats")
```
Look for `"stage": "IXSCAN"` (index scan) vs. `"COLLSCAN"` (collection scan).

**Best Practices**

- **Index fields that appear in `where` clauses, `sort`, and `group`**.
- **Avoid over‑indexing** – each index adds overhead.
- **Use compound indexes when multiple fields are frequently used together** (e.g., `adminId` + `location`).
- **Consider index size** – large indexes may not fit in RAM.
- **Use `explain()` to verify queries use indexes**.

**Practice with QueueLess**

1. Start MongoDB Compass and connect to your local database.
2. Run a query like `db.queues.find({ providerId: ObjectId("...") })` and click the “Explain” tab to see the execution stats.
3. In the code, examine the `@Indexed` annotations in `User.java`, `Queue.java`, `Place.java`, etc.
4. Try to create a query that forces a collection scan (e.g., query on a non‑indexed field) and compare the performance with `explain()`.

---

## 18.2.4 Geospatial Queries

**What Are Geospatial Queries?**  
Geospatial queries in MongoDB allow you to search for documents based on geographic location. This is essential for features like “find places near me” or “show all clinics within 5 km”. MongoDB supports several geospatial operators, including `$near`, `$geoWithin`, and `$geoIntersects`.

**Why Geospatial in QueueLess?**  
Users need to find places (hospitals, shops, banks) near their current location. The `Places` collection stores the latitude and longitude of each place, and we use MongoDB’s geospatial capabilities to perform efficient proximity searches.

**Setting Up Geospatial Data**  
In the `Place` entity, we store the location as a `GeoJsonPoint`:

```java
@Document(collection = "places")
public class Place {
    // ...
    @GeoSpatialIndexed(type = GeoSpatialIndexType.GEO_2DSPHERE)
    private GeoJsonPoint location;
    // ...
}
```

- `GeoJsonPoint` is a Spring Data MongoDB class that represents a GeoJSON point with coordinates `[longitude, latitude]`.
- The `@GeoSpatialIndexed` annotation creates a 2dsphere index on the `location` field. This index is required for efficient geospatial queries.

**Repository Method for Nearby Places**  
In `PlaceRepository`, we define a custom query using `@Query`:

```java
@Query("{ 'location' : { $near : { $geometry : { type : 'Point', coordinates: [?0, ?1] }, $maxDistance: ?2 } } }")
List<Place> findByLocationNear(Double longitude, Double latitude, Double maxDistance);
```

- `?0` and `?1` are placeholders for longitude and latitude.
- `?2` is the maximum distance in meters.
- The `$near` operator returns places sorted by distance, starting from the nearest.

**Service Method**  
`PlaceService.getNearbyPlaces` calls this repository method:

```java
public List<Place> getNearbyPlaces(double longitude, double latitude, double radius) {
    double maxDistance = radius * 1000; // convert km to meters
    return placeRepository.findByLocationNear(longitude, latitude, maxDistance);
}
```

**Controller Endpoint**  
`PlaceController.getNearbyPlaces` exposes this to the frontend:

```java
@GetMapping("/nearby")
public ResponseEntity<List<PlaceDTO>> getNearbyPlaces(
        @RequestParam double longitude,
        @RequestParam double latitude,
        @RequestParam(defaultValue = "5") double radius) {
    List<Place> places = placeService.getNearbyPlaces(longitude, latitude, radius);
    return ResponseEntity.ok(places.stream().map(PlaceDTO::fromEntity).collect(Collectors.toList()));
}
```

**How It Works Internally**  
MongoDB uses the 2dsphere index to quickly find documents whose location is within the specified radius. The `$near` operator also sorts the results by distance. The query returns all places within `maxDistance` meters of the given point.

**Example Query**  
If a user is at (77.5946, 12.9716) and wants places within 5 km, the generated MongoDB query is:

```javascript
db.places.find({
    location: {
        $near: {
            $geometry: { type: "Point", coordinates: [77.5946, 12.9716] },
            $maxDistance: 5000
        }
    }
})
```

**Other Geospatial Operators**
- `$geoWithin` – returns documents within a specified polygon or circle (without sorting).
- `$geoIntersects` – returns documents that intersect a specified geometry (e.g., a line or polygon).

**Indexing Considerations**
- The 2dsphere index is mandatory for `$near` queries. Without it, MongoDB will return an error.
- The index supports both GeoJSON points and legacy coordinate pairs. We use GeoJSON (`GeoJsonPoint`) for consistency.

**Testing Geospatial Queries**  
In MongoDB Compass, you can run a query manually to verify the index is used. The “Explain” tab will show `"stage": "GEO_NEAR"` or `"IXSCAN"`.

**Practice with QueueLess**

1. Insert a few places with different coordinates into the `places` collection.
2. Call `GET /api/places/nearby?longitude=...&latitude=...&radius=5` and verify that only places within that radius are returned.
3. In MongoDB Compass, run the same query using the `$near` operator and use the Explain tab to see the index usage.

---

## 18.2.5 Aggregation Framework

**What is MongoDB Aggregation?**  
Aggregation is a way to process documents and return computed results. It’s like SQL’s `GROUP BY` but much more powerful. The aggregation pipeline consists of stages, each transforming the documents as they pass through.

**Why Use Aggregation in QueueLess?**
- **Search across collections**: The comprehensive search needs to join services with places to show place details alongside service results.
- **Analytics**: Aggregations are used to compute token volume over time, busiest hours, and average wait times.
- **Reporting**: Some reports require grouping and counting documents.

**Basic Aggregation Stages**

| Stage        | Purpose                                                                 | Example                                           |
|--------------|-------------------------------------------------------------------------|---------------------------------------------------|
| `$match`     | Filters documents (like `WHERE` in SQL)                                 | `{ $match: { isActive: true } }`                  |
| `$group`     | Groups documents by a key and applies accumulators (sum, avg, etc.)     | `{ $group: { _id: "$userId", total: { $sum: 1 } } }` |
| `$project`   | Selects fields, computes new fields, or renames                         | `{ $project: { name: 1, fullAddress: { $concat: ["$address", ", ", "$city"] } } }` |
| `$lookup`    | Performs a left outer join with another collection                      | `{ $lookup: { from: "places", localField: "placeId", foreignField: "_id", as: "place" } }` |
| `$unwind`    | Deconstructs an array field into multiple documents                     | `{ $unwind: "$tokens" }`                          |
| `$sort`      | Sorts documents                                                         | `{ $sort: { createdAt: -1 } }`                    |
| `$limit`     | Limits the number of documents                                          | `{ $limit: 20 }`                                  |
| `$skip`      | Skips a number of documents (for pagination)                            | `{ $skip: 0 }`                                    |

**Example: Service Search with Place Details**  
In `SearchService`, we need to return services along with the name and address of the place they belong to. This is done with a `$lookup`:

```java
Aggregation agg = Aggregation.newAggregation(
    Aggregation.match(Criteria.where("placeId").in(placeIds)),
    Aggregation.match(Criteria.where("name").regex(request.getQuery(), "i")),
    Aggregation.lookup("places", "placeId", "_id", "place"),
    Aggregation.unwind("place"),
    Aggregation.project()
        .and("_id").as("id")
        .and("name").as("name")
        .and("description").as("description")
        .and("averageServiceTime").as("averageServiceTime")
        .and("supportsGroupToken").as("supportsGroupToken")
        .and("emergencySupport").as("emergencySupport")
        .and("isActive").as("isActive")
        .and("place.name").as("placeName")
        .and("place.address").as("placeAddress")
        .and("place.rating").as("placeRating"),
    Aggregation.sort(Sort.by(Sort.Direction.ASC, "name")),
    Aggregation.skip(pageable.getOffset()),
    Aggregation.limit(pageable.getPageSize())
);
AggregationResults<ServiceSearchResult> results = mongoTemplate.aggregate(agg, "services", ServiceSearchResult.class);
```

**How It Works**
1. `$match` – filters services by `placeId` (from place search results) and by name (regex).
2. `$lookup` – joins with the `places` collection. For each service, it adds a new array field `place` containing the matching place documents (usually one).
3. `$unwind` – turns the array `place` into a single embedded object.
4. `$project` – selects the fields we want, renaming some (e.g., `place.name` becomes `placeName`).
5. `$sort` and pagination – sorts by name and limits the result.

**Example: Token Volume Over Time**  
In `AdminService.getTokensOverTime`, we aggregate completed tokens by date:

```java
Map<LocalDate, Long> dailyCounts = queues.stream()
        .flatMap(q -> q.getTokens().stream())
        .filter(t -> TokenStatus.COMPLETED.toString().equals(t.getStatus()))
        .filter(t -> t.getCompletedAt() != null && t.getCompletedAt().isAfter(start))
        .collect(Collectors.groupingBy(
                t -> t.getCompletedAt().toLocalDate(),
                Collectors.counting()
        ));
```

This uses Java streams, not MongoDB aggregation. For large datasets, it would be more efficient to do this in the database, but currently the data is small enough to process in‑memory.

**Example: Busiest Hours**  
`AdminService.getBusiestHours` uses `QueueHourlyStats` documents, which are snapshots taken every hour. The service groups them by hour and averages the waiting count:

```java
Map<Integer, Double> avgByHour = stats.stream()
        .collect(Collectors.groupingBy(
                s -> s.getHour().getHour(),
                Collectors.averagingInt(QueueHourlyStats::getWaitingCount)
        ));
```

Again, this is Java‑side. For larger datasets, we would use MongoDB aggregation with `$group` and `$avg`.

**Aggregation in MongoDB Shell**  
The equivalent aggregation for busiest hours would be:

```javascript
db.queue_hourly_stats.aggregate([
    { $match: { hour: { $gte: ISODate("...") } } },
    { $group: { _id: { $hour: "$hour" }, avgWaiting: { $avg: "$waitingCount" } } },
    { $sort: { _id: 1 } }
])
```

**Performance Considerations**
- `$lookup` can be expensive if the joined collection is large. Indexes on the foreign field (`_id` in places) help.
- Use `$match` as early as possible to reduce the number of documents flowing through the pipeline.
- Pagination (`$skip` + `$limit`) should be done after filtering, not before.
- Avoid `$lookup` if the data can be denormalised or cached.

**Practice with QueueLess**
1. In `SearchService.comprehensiveSearch`, find the aggregation code for services. Run the query in MongoDB Compass to see the output.
2. Write a small aggregation to compute the total number of tokens served per provider (hint: use `$unwind` on `tokens`, then `$group` by `providerId`).
3. Test the performance with `explain("executionStats")`.

---

## 18.3 Redis

### 18.3.1 What is Redis?

**Overview**  
Redis (REmote DIctionary Server) is an open‑source, in‑memory key‑value store. It supports various data structures: strings, hashes, lists, sets, sorted sets, and more. It can also persist data to disk (RDB snapshots or append‑only logs) for durability, but its primary strength is speed – all data is held in RAM.

**Why Redis for QueueLess?**
- **Caching**: Frequently accessed data (places, services, queues) can be cached to reduce database load and improve response times.
- **TTL (Time‑to‑Live)**: Redis can automatically expire keys after a set time, which is perfect for temporary data like OTPs.
- **Distributed**: In a clustered environment, Redis can be shared across multiple backend instances, keeping caches consistent.

**Use Cases in QueueLess** (actual and potential)
- **Caching**: Place details, service lists, queue objects – currently implemented with Redis.
- **Rate limiting**: Not using Redis, but could be used to share bucket state across instances.
- **OTP storage**: Currently stored in MongoDB with TTL index; could be moved to Redis for better performance.

### 18.3.2 Using Redis with Spring Cache

**Dependencies**  
`spring-boot-starter-data-redis` brings in Lettuce (the Redis client) and auto‑configuration.

**Configuration**  
`RedisCacheConfig.java` customises the `RedisCacheManager`:

```java
@Configuration
@EnableCaching
public class RedisCacheConfig {
    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.registerModule(new GeoJsonModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(...);

        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper)));
    }
}
```

- **`@EnableCaching`** enables Spring’s cache abstraction.
- **`RedisCacheConfiguration`** sets:
    - Default TTL of 10 minutes.
    - JSON serialisation for values (so objects are stored as JSON, not Java serialised).
    - Type information (`activateDefaultTyping`) so that deserialisation works correctly with polymorphic types (like `GeoJsonPoint`).
    - Keys are strings, values are JSON.

**Cache Annotations in Services**  
Spring’s `@Cacheable`, `@CacheEvict`, `@CachePut` are used in service methods. Example from `PlaceService`:

```java
@Cacheable(value = "places", key = "#id")
public Place getPlaceById(String id) { ... }

@CacheEvict(value = {"places", "placesByAdmin"}, allEntries = true)
public Place updatePlace(String id, PlaceDTO placeDTO) { ... }
```

- **`@Cacheable`**: When `getPlaceById` is called, Spring checks Redis for a key `places::<id>`. If found, returns the cached value; otherwise, executes the method and stores the result.
- **`@CacheEvict`**: After updating a place, all entries in the `places` and `placesByAdmin` caches are removed, forcing fresh reads.

**Cache Manager**  
Spring Boot auto‑creates a `RedisCacheManager` using the `RedisConnectionFactory`. Our `RedisCacheConfiguration` bean customises it.

**Caching in Practice**
- Place details are cached because they are read frequently and change infrequently.
- Queue objects are also cached, but because they change often (tokens added/served), we evict the cache on every update. This may still be beneficial because reads still happen frequently.

**Potential Pitfalls**
- **Cache stampede**: When a cache expires, many requests may hit the database simultaneously. We mitigate by using TTL of 10 minutes, and the application can handle moderate load.
- **Serialisation**: Type information must be preserved; otherwise, deserialisation may fail. `activateDefaultTyping` adds `@class` property to the JSON.

**Practice with QueueLess**
1. Run the application and call `GET /api/places/{id}`. Check Redis (e.g., with `redis-cli`) for a key `places::<id>`.
2. After updating the place, call the same endpoint again – the cache should be empty and the new value stored.
3. Use `spring.cache.redis.time‑to‑live` property to change the default TTL.

---

## 18.3.3 TTL and Expiration

**What is TTL?**  
TTL (Time‑to‑Live) is a mechanism that automatically removes a key from Redis after a specified duration. This is essential for temporary data like OTPs, session tokens, or cached items that should not stay forever.

**How TTL Works in Redis**  
When you set a key with a TTL, Redis tracks its expiration time. After the TTL passes, the key is deleted automatically. You can also set a key without TTL (persistent) or update the TTL later.

**TTL in Spring Cache**  
In `RedisCacheConfig`, we set a default TTL for all cached entries:

```java
.entryTtl(Duration.ofMinutes(10))
```

This means any key stored by Spring’s cache abstraction will expire after 10 minutes. You can also set per‑cache TTL by creating multiple `RedisCacheConfiguration` beans and using `@Cacheable(cacheNames = "places", key = "#id")` with a custom `CacheResolver`.

**Setting TTL Manually with `RedisTemplate`**  
If you need to store temporary data outside the cache abstraction (e.g., OTPs), you can use `RedisTemplate`:

```java
@Autowired
private RedisTemplate<String, String> redisTemplate;

public void storeOtp(String email, String otp) {
    String key = "otp:" + email;
    redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5));
}
```

This sets a key with a 5‑minute TTL. The key will be automatically deleted after 5 minutes.

**OTP Storage in QueueLess**  
Currently, QueueLess stores OTPs in MongoDB with a TTL index. However, Redis would be a more natural fit because:
- **Performance**: In‑memory storage is faster than disk.
- **Built‑in TTL**: No need for a background cleanup job.
- **Simplicity**: Less schema overhead.

If you wanted to switch to Redis for OTPs, you would:
1. Add `RedisTemplate` to `AuthService`.
2. In `sendVerificationOtp`, store the OTP with a 5‑minute TTL.
3. In `verifyEmail`, retrieve the OTP from Redis; if not present, it’s expired.
4. Remove the `otp` collection from MongoDB.

**Expiration of Other Data**
- **Cached objects**: Automatically expire after 10 minutes to keep data fresh.
- **Password reset tokens** (in `password_reset_tokens` collection) use MongoDB TTL index (expires after 24 hours).
- **Queue hourly stats** are deleted via a scheduled job (or could use TTL index).

**Monitoring Expirations**  
You can use `redis-cli` to check TTL of a key:

```bash
redis-cli TTL places::123
```

If the key does not exist, it returns `-2`. If it exists but has no TTL, it returns `-1`.

**Practice with QueueLess**
1. Start Redis and the backend.
2. Call `GET /api/places/{id}` to cache a place.
3. Use `redis-cli` to inspect the key: `KEYS places::*` and `TTL places::<id>`.
4. Wait 10 minutes and call the endpoint again – the TTL should have expired and the cache will be refilled.

---

## 18.3.4 OTP Storage (Alternative)

**Why Use Redis for OTPs?**  
OTPs are short‑lived and need to be accessible only for a few minutes. Redis is ideal because:
- **Automatic expiration**: Set a TTL (e.g., 5 minutes) and Redis deletes the key automatically.
- **High performance**: In‑memory access is faster than querying MongoDB.
- **No need for a separate collection or background cleanup job**.

**How QueueLess Currently Stores OTPs**  
Currently, QueueLess stores OTPs in a MongoDB collection `otp`. Each document has an `expiryTime` field, and a TTL index automatically removes expired documents. This works, but it’s less efficient than Redis.

**Switching to Redis for OTPs**  
If we wanted to switch, we would:

1. **Add Redis dependency** – already present.
2. **Inject `RedisTemplate<String, String>` into `AuthService`**.
3. **When sending OTP**:
   ```java
   String key = "otp:" + email;
   redisTemplate.opsForValue().set(key, otp, Duration.ofMinutes(5));
   ```
4. **When verifying OTP**:
   ```java
   String storedOtp = redisTemplate.opsForValue().get(key);
   if (storedOtp == null) {
       // OTP expired or not found
   } else if (!storedOtp.equals(otp)) {
       // Invalid OTP
   } else {
       // Verified – delete the key
       redisTemplate.delete(key);
   }
   ```
5. **Remove the `otp` collection** from MongoDB.

**Benefits**
- Faster lookups (no database round‑trip).
- Reduced load on MongoDB.
- Simpler code – no need to handle TTL indices or background deletion.

**Practice**  
If you have Redis running, you can test this manually by using `redis-cli` to set a key with TTL and retrieve it.

---

## 18.4 JWT & Authentication

### 18.4.1 JWT Structure & Claims

**What is JWT?**  
A JSON Web Token (JWT) is a compact, URL‑safe token format used to transmit information between parties. It consists of three parts: **header**, **payload**, and **signature**, each base64‑encoded and separated by dots.

- **Header**: Contains the signing algorithm (e.g., HS256) and token type.
- **Payload**: Contains claims (user data, roles, expiry). This is where we store user‑specific information.
- **Signature**: Verifies the token was not tampered with.

**JWT Structure Example**
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```

**Claims in QueueLess**  
In `JwtTokenProvider.generateToken()`, we add the following claims:

```java
claims.put("sub", user.getId());          // subject = user ID
claims.put("role", user.getRole().name());
claims.put("userId", user.getId());
claims.put("email", user.getEmail());
claims.put("name", user.getName());
claims.put("placeId", user.getPlaceId());
claims.put("isVerified", user.getIsVerified());
claims.put("ownedPlaceIds", user.getOwnedPlaceIds());
```

These claims are used by the frontend to display user information and by the backend to enforce permissions.

**Why Include `userId` Both as `sub` and as a custom claim?**  
`sub` is the standard claim for subject, but we also include a separate `userId` for convenience in the frontend and for logging.

**Expiration**  
We set an expiration time (e.g., 24 hours) in the `exp` claim. After that, the token is considered invalid.

**Practice**  
Use [jwt.io](https://jwt.io) to decode a token generated by QueueLess and inspect the claims.

---

## 18.4.2 Token Generation & Validation

**Generating a JWT**  
The `JwtTokenProvider` class is responsible for creating and validating tokens. The `generateToken` method:

```java
public String generateToken(User user) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("sub", user.getId());
    claims.put("role", user.getRole().name());
    claims.put("userId", user.getId());
    claims.put("email", user.getEmail());
    claims.put("name", user.getName());
    claims.put("placeId", user.getPlaceId());
    claims.put("isVerified", user.getIsVerified());
    claims.put("ownedPlaceIds", user.getOwnedPlaceIds());

    return Jwts.builder()
            .setClaims(claims)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
}
```

- `getSigningKey()` returns a `Key` object created from the `jwt.secret` property (must be at least 256 bits for HS256).
- The token is built with the claims, issued at, expiration, and signed with HMAC‑SHA256.

**Validating a JWT**  
The `validateToken` method checks:

```java
public boolean validateToken(String token) {
    try {
        Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
        return true;
    } catch (ExpiredJwtException e) {
        log.error("JWT token is expired: {}", e.getMessage());
        throw new RuntimeException("JWT token is expired");
    } catch (MalformedJwtException | SecurityException | IllegalArgumentException e) {
        log.error("Invalid JWT token: {}", e.getMessage());
        throw new RuntimeException("Invalid JWT token");
    }
}
```

- It attempts to parse the token using the same signing key. If the signature is invalid, the token is malformed, or it has expired, an exception is thrown.

**Extracting Information**  
The provider also has methods to extract the user ID, email, and role from the token:

```java
public String getUserIdFromToken(String token) {
    Claims claims = Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    return claims.getSubject();
}
```

**JWT Authentication Filter**  
The `JwtAuthenticationFilter` is a `OncePerRequestFilter` that runs on every request. It:

1. Extracts the `Authorization` header.
2. If it starts with `Bearer `, extracts the token.
3. Validates the token using `jwtProvider.validateToken(token)`.
4. If valid, creates an `UsernamePasswordAuthenticationToken` with the user ID as principal and the role as authority.
5. Sets it in the `SecurityContext`.

```java
UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(userId, null, authorities);
authentication.setDetails(Map.of("userId", userId, "email", email));
SecurityContextHolder.getContext().setAuthentication(authentication);
```

**Why set the `userId` as the principal?**  
The principal is used by Spring Security to identify the current user. In our controllers, we can retrieve it via `Authentication.getName()`.

**Role Extraction**  
The role is stored as a single claim. The filter creates a `SimpleGrantedAuthority` with prefix `ROLE_`. For example, if the claim `role` is `ADMIN`, the authority becomes `ROLE_ADMIN`, which matches `hasRole('ADMIN')` in `@PreAuthorize`.

**WebSocket Authentication**  
WebSocket connections use a similar mechanism but in the `StompJwtChannelInterceptor`. It intercepts the CONNECT frame, extracts the token, validates it, and sets the user in the `StompHeaderAccessor`. This user is then used for user‑specific destinations.

**Error Handling**  
If the token is invalid or missing, the filter does not set the authentication. The `SecurityContext` remains anonymous. The `JwtAuthenticationEntryPoint` then returns a 401 Unauthorized response for requests that require authentication.

**Practice**
1. Log in to the application and copy the token from the browser’s localStorage.
2. Use an online JWT debugger (e.g., jwt.io) to view the claims.
3. In the backend, add a breakpoint in `JwtAuthenticationFilter` and observe the token being processed.

---

## 18.4.3 Integrating with Spring Security

**Spring Security Configuration**  
The `SecurityConfig` class is the central place for security setup. It’s annotated with `@Configuration`, `@EnableWebSecurity`, and `@EnableMethodSecurity(prePostEnabled = true)`.

**Key Beans**
- **`SecurityFilterChain`** – defines which requests are secured and how.
- **`PasswordEncoder`** – a `BCryptPasswordEncoder` bean for hashing passwords.
- **`JwtAuthenticationFilter`** – our custom filter that validates JWT and sets the `SecurityContext`.
- **CORS configuration** – defines allowed origins, methods, headers.

**Filter Chain Order**  
The filter chain is built in the `filterChain` method:

```java
http
    .cors(cors -> cors.configurationSource(corsConfigurationSource()))
    .csrf(csrf -> csrf.disable())
    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
    .authorizeHttpRequests(auth -> auth
        .requestMatchers("/api/auth/**", "/api/password/**", "/api/public/**", "/ws/**").permitAll()
        .requestMatchers(HttpMethod.GET, "/api/places/**").permitAll()
        .requestMatchers("/api/admin/**").hasRole("ADMIN")
        .anyRequest().authenticated()
    )
    .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
```

- **CORS**: allows the frontend origin.
- **CSRF**: disabled because we use stateless JWT.
- **Session**: stateless – no sessions.
- **Authorisation**: public endpoints are permitted; admin endpoints require `ROLE_ADMIN`; all others require authentication.
- **Filter**: adds our `JwtAuthenticationFilter` before Spring’s default `UsernamePasswordAuthenticationFilter`.

**Password Encoding**  
The `PasswordEncoder` bean is injected into `AuthService` and used when registering and logging in:

```java
user.setPassword(passwordEncoder.encode(request.getPassword()));
```

During login, we verify with `passwordEncoder.matches(request.getPassword(), user.getPassword())`. BCrypt is one‑way, salted, and adaptive, making it resistant to brute‑force attacks.

**Method‑Level Security**  
With `@EnableMethodSecurity`, we can use `@PreAuthorize` on controller methods. We defined custom annotations for readability:

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasRole('ADMIN')")
public @interface AdminOnly {}
```

Then in controllers:

```java
@PostMapping("/places")
@AdminOnly
public ResponseEntity<PlaceDTO> createPlace(...) { ... }
```

This ensures only users with `ROLE_ADMIN` can access the method. Spring Security checks the `GrantedAuthority` list in the `Authentication` object.

**AccessDeniedHandler & AuthenticationEntryPoint**
- `JwtAccessDeniedHandler` – handles 403 responses when a user is authenticated but lacks the required role.
- `JwtAuthenticationEntryPoint` – handles 401 responses when a request requires authentication but the token is missing or invalid.

**How Spring Security Works Internally**
- When a request arrives, the filter chain processes it.
- `JwtAuthenticationFilter` (if the token is valid) sets an `Authentication` object in `SecurityContextHolder`.
- Later in the chain, `AuthorizationFilter` checks the `@PreAuthorize` annotations (if any) and either allows or denies access.
- If access is denied, the `AccessDeniedHandler` is called.
- If authentication is required but missing, the `AuthenticationEntryPoint` is called.

**Practice with QueueLess**
1. In `SecurityConfig`, comment out the `authorizeHttpRequests` rule for `/api/places/**` and observe that you can no longer access places without login.
2. Try to access a protected endpoint with a valid token but without the required role – you’ll get a 403.
3. Add a new role and custom annotation to experiment.

---

## 18.5 WebSocket & STOMP

### 18.5.1 WebSocket vs HTTP

**What is WebSocket?**  
WebSocket is a communication protocol that provides full‑duplex, persistent connections between a client and a server. Unlike HTTP (which is request‑response), WebSocket allows the server to push data to the client at any time without the client needing to poll. This makes it ideal for real‑time applications like chat, gaming, and live queue updates.

**Why Not Just HTTP Polling?**
- **Polling**: The client repeatedly asks “has anything changed?”. This wastes bandwidth and increases latency.
- **Long polling**: The server holds the request until an event occurs, but it still requires a new HTTP connection each time.
- **WebSocket**: One persistent connection, messages can flow both ways instantly.

**WebSocket in QueueLess**  
Queue status changes frequently (tokens added, served, cancelled). Using WebSocket, the server immediately pushes the updated queue to all connected clients, providing a seamless live experience.

---

### 18.5.2 STOMP Protocol

**What is STOMP?**  
STOMP (Simple Text Oriented Messaging Protocol) is a messaging protocol that works over WebSocket. It defines a simple frame‑based format (similar to HTTP) and includes concepts like **destinations** (topics/queues), **subscriptions**, and **messages**. Spring uses STOMP to build a message‑broker abstraction on top of WebSocket.

**STOMP Frames Example**
```
CONNECT
accept-version:1.2
heart-beat:4000,4000

^@
```

```
SUBSCRIBE
id:sub-0
destination:/topic/queues/123

^@
```

```
MESSAGE
destination:/topic/queues/123
content-type:application/json
content-length:...

{"id":"queue123","tokens":[...]}
```

**Destinations**
- **`/topic`** – broadcast to all subscribers (e.g., `/topic/queues/{queueId}`).
- **`/queue`** – point‑to‑point (e.g., `/queue/provider-updates`).
- **`/user`** – a special prefix that Spring translates to a user‑specific queue (e.g., `/user/queue/emergency-approved`).

**Why STOMP?**
- Standardised messaging over WebSocket.
- Spring provides built‑in support (message broker, controller annotations).
- Easy to secure and integrate with Spring Security.

---

### 18.5.3 Spring WebSocket Configuration

**`WebSocketConfig.java`**
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("https://localhost:5173")
                .withSockJS();
    }
}
```
- **`@EnableWebSocketMessageBroker`** – enables WebSocket message handling.
- **`configureMessageBroker`** – sets up an in‑memory simple broker for `/topic` and `/queue`. Messages sent to `/app` are routed to controller methods (e.g., `/app/queue/serve-next`). `/user` is used for user‑specific destinations.
- **`registerStompEndpoints`** – registers the `/ws` endpoint. SockJS provides fallback for browsers that don’t support WebSocket.

**Why SockJS?**  
SockJS simulates WebSocket when it’s not available, falling back to other transports (e.g., HTTP long polling). This ensures the application works across all browsers.

---

### 18.5.4 Sending and Receiving Messages

**Message Handling Controller**  
`QueueWebSocketController` is annotated with `@Controller` (not `@RestController`). It handles incoming STOMP messages.

```java
@MessageMapping("/queue/serve-next")
@AdminOrProviderOnly
public void serveNext(@Payload ServeNextRequest request, Authentication authentication) {
    Queue updatedQueue = queueService.serveNextToken(request.getQueueId());
    if (updatedQueue != null) {
        messagingTemplate.convertAndSend("/topic/queues/" + request.getQueueId(), updatedQueue);
        messagingTemplate.convertAndSendToUser(authentication.getName(), "/queue/provider-updates", updatedQueue);
    }
}
```
- `@MessageMapping` maps the destination `/app/queue/serve-next` to this method.
- `@Payload` binds the message body to a Java object.
- `SimpMessagingTemplate` is used to send messages to clients.

**Broadcast vs. User‑Specific**
- `convertAndSend("/topic/queues/123", updatedQueue)` sends to all subscribers of that topic (e.g., all users viewing that queue).
- `convertAndSendToUser(providerId, "/queue/provider-updates", updatedQueue)` sends a private message to the provider’s user‑specific queue.

**Frontend Subscription**  
The frontend (using STOMP.js) connects and subscribes:

```javascript
client.subscribe('/topic/queues/' + queueId, (message) => {
    const queue = JSON.parse(message.body);
    // update UI
});
```

---

### 18.5.5 Securing WebSocket Endpoints

**JWT Authentication for WebSocket**  
HTTP authentication doesn’t apply to WebSocket, so we need to intercept the CONNECT frame. `StompJwtChannelInterceptor` does this:

```java
public class StompJwtChannelInterceptor implements ChannelInterceptor {
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);
            if (jwtProvider.validateToken(token)) {
                String userId = jwtProvider.getUserIdFromToken(token);
                Collection<GrantedAuthority> authorities = jwtProvider.getAuthoritiesFromToken(token);
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                accessor.setUser(auth);
            }
        }
        return message;
    }
}
```
- It extracts the `Authorization` header from the CONNECT frame.
- Validates the token and sets the user in the `StompHeaderAccessor`. Spring then uses this user for subsequent subscriptions.

**Security Configuration for WebSocket**  
`WebSocketSecurityConfig` extends `AbstractSecurityWebSocketMessageBrokerConfigurer`:

```java
@Configuration
public class WebSocketSecurityConfig extends AbstractSecurityWebSocketMessageBrokerConfigurer {
    @Override
    protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
        messages
            .simpTypeMatchers(SimpMessageType.CONNECT, SimpMessageType.HEARTBEAT).permitAll()
            .simpSubscribeDestMatchers("/user/**", "/topic/queues/*").authenticated()
            .anyMessage().denyAll();
    }
}
```
- Permits CONNECT and HEARTBEAT (required for handshake).
- Requires authentication for subscriptions to `/user/**` and `/topic/queues/*`.
- All other messages are denied.

**Why Separate Security Config?**  
Spring Security’s HTTP filter chain does not apply to WebSocket frames. This config ensures that even if a client connects, they cannot subscribe to topics without proper authentication.

---

### 18.5.6 Practice with QueueLess

1. Open the browser’s Developer Tools (Network tab) and filter for WebSocket connections. Connect to the app and observe the CONNECT frame and subsequent messages.
2. In `QueueWebSocketController`, add a log statement to see when messages are processed.
3. Simulate multiple users (incognito windows) to see how updates are broadcast to all subscribers.
4. Try to subscribe to a queue topic without being authenticated – you should receive an error or be denied.

---

## 18.6 React & Redux

### 18.6.1 React Basics (Components, Props, State)

**What is React?**  
React is a JavaScript library for building user interfaces. It uses a component‑based architecture, where each piece of the UI is a self‑contained component that manages its own state and renders based on that state. React updates the DOM efficiently using a virtual DOM.

**Components in QueueLess**  
Every page and reusable UI piece is a React component. For example, `CustomerQueue.jsx` is a functional component that displays a queue, allows users to join, and shows their token status.

**Props**  
Props (short for properties) are read‑only data passed from a parent component to a child. Example in `QueueList.jsx`:

```jsx
<QueueList queue={queue} onServeNext={handleServeNext} />
```

Here, `queue` and `onServeNext` are props passed to `QueueList`. Inside `QueueList`, they are accessed via `props.queue` or by destructuring: `const { queue, onServeNext } = props;`.

**State**  
State is data that can change over time and causes the component to re‑render when updated. In functional components, we use the `useState` hook.

Example from `CustomerQueue.jsx`:

```jsx
const [queue, setQueue] = useState(null);
const [loading, setLoading] = useState(true);
```

**Hooks**
- `useState` – adds state to functional components.
- `useEffect` – runs side effects (e.g., fetching data, setting up subscriptions). It runs after the component renders.
- `useRef` – holds a mutable value that doesn’t cause re‑renders; used for DOM references (e.g., focusing an input).
- `useCallback` / `useMemo` – optimise performance by memoising functions or values.

**Example: Fetching a Queue**  
In `CustomerQueue.jsx`:

```jsx
useEffect(() => {
    const fetchQueue = async () => {
        const response = await axiosInstance.get(`/queues/${queueId}`);
        setQueue(normalizeQueue(response.data));
    };
    fetchQueue();
    const interval = setInterval(fetchQueue, 5000); // refresh every 5 seconds
    return () => clearInterval(interval);
}, [queueId]);
```

- The effect runs when `queueId` changes.
- It fetches the queue data and sets state.
- It sets up a polling interval (though WebSocket would be better, this is a fallback).
- Cleans up the interval on unmount.

**Conditional Rendering**  
React components can return different JSX based on state:

```jsx
if (loading) {
    return <Spinner animation="border" />;
}
if (error) {
    return <Alert variant="danger">{error}</Alert>;
}
return <div>{/* queue UI */}</div>;
```

**Practice with QueueLess**
1. Open `CustomerQueue.jsx` and identify where `useState` and `useEffect` are used.
2. Modify the component to display a different message when the queue is inactive.
3. Try adding a new state variable and use it in the render.

---

### 18.6.2 Hooks (useState, useEffect, useRef)

We already covered `useState` and `useEffect`. Let’s look at `useRef` and `useCallback`.

**useRef**  
`useRef` creates a mutable object that persists for the lifetime of the component. It does not cause re‑renders. Common uses:
- Accessing DOM elements directly (e.g., focusing an input).
- Storing previous values.
- Keeping a reference to a timer ID.

Example from `Navbar.jsx`:

```jsx
const logoRef = useRef(null);
// Later: logoRef.current.focus();
```

**useCallback**  
`useCallback` returns a memoised version of a function that only changes if one of its dependencies changes. It’s used to prevent unnecessary re‑creation of functions that are passed as props to child components (optimisation).

Example from `CustomerQueue.jsx` (simplified):

```jsx
const handleAddToken = useCallback(async () => {
    // ... token logic
}, [queueId, userId]);
```

Now `handleAddToken` is stable across renders unless `queueId` or `userId` change.

**useMemo**  
`useMemo` memoises a computed value. It’s useful for expensive calculations.

**Practice**  
In `QueueList.jsx`, find where `onServeNext` is passed. Wrap it with `useCallback` to see the effect (though not necessary in this example).

---

### 18.6.3 Routing with React Router

**What is React Router?**  
React Router is a library that enables navigation between different components (pages) in a React application. It keeps the UI in sync with the URL.

**Routes in QueueLess**  
The `App.jsx` defines all routes using `BrowserRouter`, `Routes`, and `Route`:

```jsx
<Route path="/customer/queue/:queueId" element={<CustomerQueue />} />
<Route path="/login" element={<Login />} />
<Route path="/profile" element={<ProtectedRoute><UserProfile /></ProtectedRoute>} />
```

- **`:queueId`** – a dynamic segment; accessible via `useParams()` inside the component.
- **`element`** – the component to render when the path matches.

**Protected Routes**  
The `ProtectedRoute` component checks if the user is authenticated (token exists) and has the required role. If not, it redirects to login.

```jsx
const ProtectedRoute = ({ children, allowedRoles = [] }) => {
    const { token, role } = useSelector((state) => state.auth);
    if (!token) return <Navigate to="/login" replace />;
    if (allowedRoles.length && !allowedRoles.includes(role)) {
        return <Navigate to="/" replace />;
    }
    return children;
};
```

**Navigation**  
To navigate programmatically, we use `useNavigate`:

```jsx
const navigate = useNavigate();
navigate('/profile');
```

**Accessing URL Parameters**  
Inside `CustomerQueue`, we use `useParams()` to get the `queueId`:

```jsx
const { queueId } = useParams();
```

**Practice**
1. Add a new route to the app (e.g., `/about`) and a simple component.
2. In `Navbar`, add a link to that route using `Link` from `react-router-dom`.
3. Use `useNavigate` in a button to go back to home.

---

### 18.6.4 Redux Toolkit (Slices, Thunks, Selectors)

**What is Redux?**  
Redux is a state management library that centralises application state. It helps manage state that is shared across many components, avoiding prop drilling. **Redux Toolkit** is the modern, recommended way to write Redux logic.

**Core Concepts**
- **Store**: Holds the global state.
- **Slice**: A collection of reducers and actions for a specific part of the state.
- **Action**: A plain object describing what happened.
- **Reducer**: A function that takes the current state and an action, and returns a new state.
- **Thunk**: A function that can dispatch actions asynchronously (e.g., fetching data).

**Example Slice: `authSlice`**
```javascript
const authSlice = createSlice({
    name: 'auth',
    initialState: {
        token: null,
        role: null,
        id: null,
        name: null,
        // ...
    },
    reducers: {
        loginSuccess: (state, action) => {
            state.token = action.payload.token;
            state.role = action.payload.role;
            // ...
        },
        logout: (state) => {
            state.token = null;
            state.role = null;
            // ...
        },
        updateProfile: (state, action) => {
            state.name = action.payload.name;
            // ...
        },
    },
});

export const { loginSuccess, logout, updateProfile } = authSlice.actions;
export default authSlice.reducer;
```

- `createSlice` automatically generates action creators and handles immutability.
- The reducer is exported and combined in the store.

**Using Redux in Components**
- `useSelector` – reads state from the store.
- `useDispatch` – gets the dispatch function to send actions.

Example from `Navbar`:

```jsx
const { token, role, name, profileImageUrl } = useSelector((state) => state.auth);
const dispatch = useDispatch();

const handleLogout = () => {
    dispatch(logout());
    navigate('/login');
};
```

**Async Thunks**  
For asynchronous operations (like API calls), we use `createAsyncThunk`. Example from `placeSlice`:

```javascript
export const fetchPlaces = createAsyncThunk(
    'places/fetchAll',
    async (_, { rejectWithValue }) => {
        try {
            const response = await placeService.getAll();
            return response.data;
        } catch (error) {
            return rejectWithValue(error);
        }
    }
);
```

The thunk automatically dispatches `pending`, `fulfilled`, and `rejected` actions. We handle them in the slice’s `extraReducers`.

**Selectors**  
Selectors are functions that extract specific pieces of state. They can be defined in the slice or in components. Example:

```javascript
const selectUser = (state) => state.auth;
const selectName = (state) => state.auth.name;
```

**Practice with QueueLess**
1. Open `authSlice.js` and see how the state is managed.
2. In `Login.jsx`, see how `loginSuccess` is dispatched after a successful login.
3. Create a new slice for a feature (e.g., `theme`), add it to the store, and use it in a component.

---

### 18.6.5 Making API Calls with Axios

**What is Axios?**  
Axios is a promise‑based HTTP client for the browser and Node.js. It provides a simple API for making HTTP requests and automatically transforms JSON.

**Axios Instance**  
In `utils/axiosInstance.js`, we create a configured instance:

```javascript
const axiosInstance = axios.create({
    baseURL: import.meta.env.VITE_API_BASE_URL || 'https://localhost:8443/api',
});

axiosInstance.interceptors.request.use((config) => {
    const token = store.getState().auth.token;
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

axiosInstance.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401) {
            store.dispatch(logout());
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);
```

- The **request interceptor** adds the JWT to every request.
- The **response interceptor** handles 401 (token expired) by logging out and redirecting.

**Using the Instance**  
Services (like `authService`) use this instance:

```javascript
export const authService = {
    login: (credentials) => axiosInstance.post('/auth/login', credentials),
    register: (userData) => axiosInstance.post('/auth/register', userData),
    // ...
};
```

**Practice**
1. In `Login.jsx`, see how `authService.login` is called.
2. Add a new API call to a test endpoint and log the response.

---

### 18.6.6 WebSocket Client with STOMP.js

**STOMP.js Client**  
The frontend uses `@stomp/stompjs` and `sockjs-client` to connect to the backend WebSocket.

**WebSocketService Singleton**  
`services/websocketService.js` implements a singleton that manages the connection:

```javascript
class WebSocketService {
    constructor() {
        this.client = null;
        this.subscriptions = new Map();
    }

    connect() {
        const token = localStorage.getItem('token');
        this.client = new Client({
            webSocketFactory: () => new SockJS('https://localhost:8443/ws'),
            connectHeaders: { Authorization: `Bearer ${token}` },
            onConnect: () => {
                this.subscribeToUserNotifications();
            },
        });
        this.client.activate();
    }

    subscribeToQueue(queueId) {
        const subscription = this.client.subscribe(`/topic/queues/${queueId}`, (message) => {
            const queue = JSON.parse(message.body);
            store.dispatch(updateQueue(queue));
        });
        this.subscriptions.set(`queue-${queueId}`, subscription);
    }

    sendMessage(destination, body) {
        this.client.publish({ destination, body: JSON.stringify(body) });
    }
}
```

- The connection is established with the JWT in the `connectHeaders`.
- On connection, we subscribe to user‑specific notifications.
- `subscribeToQueue` stores the subscription so we can unsubscribe later.
- `sendMessage` sends commands to the backend (e.g., `/app/queue/serve-next`).

**Usage in Components**  
In `CustomerQueue.jsx`, we subscribe when the component mounts:

```jsx
useEffect(() => {
    if (queueId) {
        WebSocketService.subscribeToQueue(queueId);
        return () => WebSocketService.unsubscribeFromQueue(queueId);
    }
}, [queueId]);
```

**Practice**
1. Open `CustomerQueue.jsx` and see how it uses `WebSocketService`.
2. Add a new WebSocket subscription to a different topic and log the messages.

---

### 18.6.7 Styling with React Bootstrap & CSS

**React Bootstrap**  
React Bootstrap provides Bootstrap components as React components. It includes layout, forms, buttons, modals, etc. Example from `Navbar.jsx`:

```jsx
<NavDropdown.Item onClick={() => navigate('/profile')}>
    <i className="bi bi-person-circle me-2"></i>Profile
</NavDropdown.Item>
```

**CSS Modules**  
QueueLess uses regular CSS files (e.g., `Navbar.css`, `UserProfile.css`) imported into components. Dark mode is implemented via CSS variables in `index.css`:

```css
:root {
    --bg-primary: #ffffff;
    --text-primary: #212529;
}
.dark-mode {
    --bg-primary: #1a1a1a;
    --text-primary: #f8f9fa;
}
body {
    background-color: var(--bg-primary);
    color: var(--text-primary);
}
```

The `dark-mode` class is added to `body` based on Redux state.

**Icons**  
We use Bootstrap Icons (`bi-*`) and React Icons (`FaSearch`, etc.).

**Practice**
1. In `UserProfile.css`, modify a style and see it take effect.
2. Add a new Bootstrap component to a page (e.g., a `ProgressBar`).

---

## 18.7 Docker & Docker Compose

### 18.7.1 Docker Images & Containers

**What is Docker?**  
Docker is a platform for developing, shipping, and running applications inside lightweight containers. A container packages an application with its dependencies, ensuring it runs consistently across environments.

**Why Docker for QueueLess?**
- **Consistency**: The same image runs on a developer’s laptop, test server, and production.
- **Isolation**: Services (backend, frontend, MongoDB, Redis, Prometheus) run in separate containers.
- **Portability**: The entire stack can be started with a single command (`docker-compose up`).

**Docker Images in QueueLess**  
We have two custom images:
- **Backend** – built from `backend/backend/Dockerfile`
- **Frontend** – built from `frontend/queue-less-frontend/Dockerfile`

---

### 18.7.2 Multi‑stage Builds

**Backend Dockerfile**
```dockerfile
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=builder /app/target/backend-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- **First stage** (`builder`) uses a Maven image to compile the code.
- **Second stage** uses a slim JRE image and copies only the JAR file, resulting in a much smaller final image.
- The JAR includes all dependencies, so no extra installation is needed.

**Frontend Dockerfile**
```dockerfile
FROM node:20-alpine AS builder
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:alpine
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/nginx.conf
COPY ssl/cert.pem /etc/nginx/ssl/cert.pem
COPY ssl/key.pem /etc/nginx/ssl/key.pem
EXPOSE 443
```
- Builds the React app with `npm run build`.
- Copies the static files to an Nginx image.
- Copies a custom `nginx.conf` (which proxies API requests to the backend) and SSL certificates.

---

### 18.7.3 Networking & Volumes

**Docker Compose Network**  
All services are connected to a custom bridge network `queueless-net`. This allows containers to communicate using service names (e.g., `backend` can reach `mongodb` via `mongodb:27017`).

**Volumes**  
Volumes persist data beyond container life. In `docker-compose.yml`, we define volumes for:
- **MongoDB** – stores database files.
- **Redis** – persists cache (optional, but useful).
- **Prometheus** – stores metrics.
- **Grafana** – stores dashboards and data sources.
- **Redis data** – optional.

Example volume definition:
```yaml
volumes:
  prometheus-data:
  grafana-data:
  redis-data:
```

---

### 18.7.4 Using Docker Compose for Orchestration

**docker-compose.yml Highlights**  
The file defines services with build contexts, ports, environment variables, networks, and volumes.

- **Backend service**: builds from `./backend/backend`, exposes port 8443, passes environment variables.
- **Frontend service**: builds from `./frontend/queue-less-frontend`, exposes port 5173, depends on backend.
- **MongoDB**: uses official image, exposes 27017.
- **Redis**: official image, exposes 6379.
- **Prometheus, Grafana, Loki, Promtail** – configured with mounted config files.

**Running the Stack**
```bash
docker-compose up --build
```
This builds images (if needed) and starts all containers.

**Environment Variables**  
Sensitive values are passed via environment variables (e.g., `MONGODB_URI`, `JWT_SECRET`). These can be set in a `.env` file or directly in the shell.

**Practice**
1. Ensure Docker and Docker Compose are installed.
2. Create a `.env` file in the root with all required variables (see sample in documentation).
3. Run `docker-compose up` and access the frontend at `https://localhost:5173`.
4. Check logs with `docker-compose logs -f backend` to see what’s happening.

---

## 18.8 Monitoring Stack (Prometheus, Grafana, Loki)

### 18.8.1 Prometheus Metrics & Scraping

**What is Prometheus?**  
Prometheus is an open‑source monitoring system that collects metrics from configured targets at regular intervals. It stores time‑series data and provides a powerful query language (PromQL).

**How QueueLess Exposes Metrics**  
Spring Boot Actuator exposes a `/actuator/prometheus` endpoint when `micrometer-registry-prometheus` is added. This endpoint outputs metrics in a format Prometheus can scrape.

**Prometheus Configuration**  
The `prometheus.yml` file (mounted in the container) defines scrape jobs:

```yaml
scrape_configs:
  - job_name: 'spring-boot'
    metrics_path: '/actuator/prometheus'
    scheme: https
    tls_config:
      insecure_skip_verify: true
    static_configs:
      - targets: ['backend:8443']
        labels:
          application: 'queueless-backend'
```

- It scrapes the backend every 15 seconds (global scrape interval).
- `tls_config.insecure_skip_verify` is set because the backend uses a self‑signed certificate.

**Custom Metrics**  
`QueueMetricsService` uses Micrometer to expose custom gauges:

```java
Gauge.builder("queue.waiting.tokens", () -> waiting)
        .tags("queueId", queueId, "placeId", placeId)
        .register(meterRegistry);
```

These appear in the `/actuator/prometheus` output and can be queried in Prometheus.

---

### 18.8.2 Custom Metrics with Micrometer

**Micrometer** provides a facade for metrics. Spring Boot auto‑configures it with Prometheus.

**Creating a Gauge**  
In `QueueMetricsService`, we create gauges for waiting tokens, in‑service tokens, and total served. The gauge’s value is computed on‑the‑fly by the lambda each time Prometheus scrapes.

**Why Custom Metrics?**  
Standard metrics (CPU, memory, HTTP requests) are useful, but business metrics like queue length help monitor system health and usage patterns.

**Practice**
1. Open Prometheus UI at `http://localhost:9090`.
2. Try a query like `queue_waiting_tokens{queueId="..."}`.
3. Create a new custom metric in code and verify it appears in the `/actuator/prometheus` endpoint.

---

### 18.8.3 Grafana Dashboards

**What is Grafana?**  
Grafana is a visualisation tool that connects to data sources like Prometheus and allows you to create dashboards with graphs, tables, and alerts.

**Grafana in QueueLess**  
Grafana is included in the Docker Compose stack. It’s configured with a default admin password (`admin`). You can connect it to the Prometheus data source (automatically discovered because they are on the same network).

**Creating a Dashboard**
1. Log into Grafana at `http://localhost:3000` (admin/admin).
2. Add Prometheus as a data source (URL: `http://prometheus:9090`).
3. Create a new dashboard with a graph panel, using a PromQL query like `rate(http_server_requests_seconds_count[5m])` to see request rate.
4. Save the dashboard.

**Pre‑configured Dashboards**  
While not provided, you can export/import dashboards. A good starting point is the Spring Boot 2.1 dashboard from the Grafana community.

**Practice**
- Explore the default dashboards or create one for queue metrics.
- Set up an alert when a queue’s waiting tokens exceed a threshold.

---

### 18.8.4 Loki Log Aggregation

**What is Loki?**  
Loki is a horizontally scalable, highly available log aggregation system inspired by Prometheus. It does not index log content; instead, it labels logs, making it cheap and efficient.

**How It Works in QueueLess**
- **Promtail** (installed in Docker) collects logs from all containers and sends them to Loki.
- Loki stores the logs and exposes a query API.
- Grafana can connect to Loki as a data source, allowing you to search logs.

**Configuration**
- `loki/loki-config.yaml` – configures Loki storage and retention.
- `promtail/promtail-config.yaml` – tells Promtail where to find Docker logs and how to label them (e.g., `container`, `job`).

**Using Grafana with Loki**
1. In Grafana, add Loki as a data source (URL: `http://loki:3100`).
2. Go to Explore, select Loki, and run a query like `{container="queueless-backend"}`.
3. You can also combine with metrics: for example, view logs for requests that had a high latency.

**Practice**
- Use the Explore view to tail logs from the backend container.
- Add a label to logs in the application (e.g., using MDC) and see it appear in Loki.

---

## 18.9 Testing

### 18.9.1 Unit Testing with JUnit 5 & Mockito

**JUnit 5** is the standard testing framework for Java. **Mockito** is used to create mock objects, isolating the code under test.

**Backend Unit Tests**
- **Service tests** (e.g., `AuthServiceTest`) mock the repository and other dependencies.
- **Controller tests** (e.g., `AuthControllerTest`) use `MockMvc` to simulate HTTP requests and verify responses.

**Example: Service Test**
```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private AuthService authService;

    @Test
    void loginSuccess() {
        User user = User.builder().email("test@example.com").password("encoded").build();
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        JwtResponse response = authService.login(new LoginRequest("test@example.com", "pass"));
        assertNotNull(response);
    }
}
```

**Example: Controller Test**
```java
@WebMvcTest(AuthController.class)
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockBean private AuthService authService;

    @Test
    void loginSuccess() throws Exception {
        JwtResponse response = new JwtResponse("token", "USER", "id", "name", ...);
        when(authService.login(any())).thenReturn(response);
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("..."))
                .andExpect(status().isOk());
    }
}
```

**Why Use Mocks?**
- **Speed**: Tests run quickly without hitting the database.
- **Isolation**: We test only the logic in the service, not the database.

---

### 18.9.2 Integration Testing with Embedded MongoDB

**What is Embedded MongoDB?**  
Flapdoodle’s embedded MongoDB library starts a real MongoDB process in memory for tests. This allows testing repository methods and complex queries without a separate database instance.

**BaseIntegrationTest**  
`BaseIntegrationTest` sets up the embedded MongoDB and disables SSL and Redis for tests:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class BaseIntegrationTest {
    private static TransitionWalker.ReachedState<RunningMongodProcess> mongod;

    @BeforeAll
    static void startMongo() {
        mongod = Mongod.builder()
                .net(Start.to(Net.class).initializedWith(Net.defaults().withPort(27017)))
                .build()
                .start(Version.Main.V6_0);
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:27017/queueless");
        registry.add("server.ssl.enabled", () -> "false");
        registry.add("spring.cache.type", () -> "none");
    }
}
```

**Integration Test Example**  
`QueueFlowIntegrationTest` simulates a complete flow: register user, create place, create service, register provider, create queue, join queue, serve, complete, and verify history.

**Benefits**
- Tests the entire stack (controllers, services, repositories) together.
- Ensures that the database schema and indexes work correctly.

---

### 18.9.3 Controller Testing with MockMvc

**What is MockMvc?**  
`MockMvc` is a Spring Testing module that allows testing controllers without starting a full HTTP server. It simulates requests and verifies responses.

**Example from `AdminControllerTest`**
```java
@Test
@WithMockUser(username = adminId, roles = {"ADMIN"})
void getAdminStats_Success() throws Exception {
    Map<String, Object> stats = Map.of("totalPlaces", 5);
    when(adminService.getDashboardStats(adminId)).thenReturn(stats);
    mockMvc.perform(get("/api/admin/stats"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalPlaces").value(5));
}
```

- `@WithMockUser` simulates an authenticated user with a given role.
- `jsonPath` verifies parts of the JSON response.

**Why Not Full HTTP?**  
MockMvc is faster and doesn’t require the server to start. It also allows easy injection of mocked services.

---

### 18.9.4 Testing WebSocket & Schedulers

**WebSocket Testing**  
We have unit tests for `QueueWebSocketController` that mock `SimpMessagingTemplate`. Integration tests for WebSocket would require a client, which we haven’t implemented yet.

**Scheduler Testing**  
We can test schedulers by calling the method directly in a test (with mocked repositories). For example, in `TokenNotificationSchedulerTest`, we mock `queueRepository`, `userRepository`, etc., and verify that `emailService.sendUpcomingTokenEmail` is called under the right conditions.

**Frontend Testing (Planned)**  
Future plans include:
- **Jest** for unit testing components.
- **React Testing Library** for rendering components and simulating user interactions.
- **Cypress** for end‑to‑end testing.

---

### 18.9.5 Best Practices & Coverage

- **Aim for high coverage** on critical services and controllers (e.g., >80%).
- **Write tests for edge cases**: empty lists, null inputs, exceptions.
- **Use `@Test` lifecycle**: `@BeforeEach` to set up common test data.
- **Keep tests independent**; each test should be able to run alone.
- **Run tests in CI/CD**: The GitHub Actions workflow runs all tests on every push.

**Practice**
1. Run `mvn test` and observe the output. Look at the `target/surefire-reports` directory for detailed reports.
2. Write a new unit test for a service method that is not yet covered.
3. Add an integration test for a new endpoint.

---

## Conclusion of Technology Learning Guide

This guide has covered the key technologies used in QueueLess, from the basics of Java and Spring Boot to advanced topics like Redis, WebSocket, and monitoring. By studying the code examples and practicing with the application, you can gain a deep understanding of how each component fits together.

Now, armed with this knowledge, you can confidently discuss the architecture, make contributions, and even extend the system with new features.

---

**End of Technology Learning Guide**

---
## 19. Interview Questions & Answers

This section contains a curated list of interview questions that you may be asked about QueueLess, along with detailed answers that demonstrate your understanding of the system. Use these to prepare for technical interviews.

---

### 19.1 General & Project Overview

**Q1: What problem does QueueLess solve?**  
QueueLess addresses the inefficiency of physical waiting lines. It allows users to join queues remotely, track their position in real time, and receive notifications when it’s their turn. For businesses, it provides digital queue management, analytics, and a way to handle emergencies or group visits.

**Q2: Who are the target users?**  
There are three main roles:
- **Users**: join queues, rate services, manage favourites.
- **Providers**: manage their own queues, serve tokens, approve emergency requests.
- **Admins**: create places and services, onboard providers, view overall statistics.

**Q3: What are the key features?**
- Real‑time queue updates via WebSocket.
- Multi‑role system with JWT authentication.
- Place/service management with geolocation.
- Token types: regular, group, emergency (with approval flow).
- Push and email notifications.
- Payment integration (Razorpay) for admin/provider tokens.
- Search with filters (location, rating, wait time).
- Feedback and ratings.
- Admin dashboard with analytics and reports.
- Audit logging and alert configuration.

---

### 19.2 Architecture & Technology Stack

**Q4: Why did you choose MongoDB over a relational database?**
- **Document model**: Tokens are naturally nested inside a queue. Embedding avoids joins and reduces query complexity.
- **Geospatial support**: Built‑in 2dsphere indexes enable efficient nearby search.
- **Flexible schema**: Fields like `isActive` can be added later without migrations.
- **Scalability**: MongoDB shards horizontally, which may be needed as data grows.

**Q5: Why did you choose Spring Boot?**
- **Rapid development**: Auto‑configuration, starter dependencies, and embedded servers.
- **Comprehensive ecosystem**: Spring Security, Spring Data, WebSocket, Scheduling.
- **Testability**: Built‑in testing support with `@SpringBootTest` and `MockMvc`.
- **Production‑ready**: Actuator, metrics, and externalised configuration.

**Q6: Why did you choose React for the frontend?**
- **Component‑based architecture**: Reusable UI components.
- **Virtual DOM**: Efficient updates.
- **Large ecosystem**: Redux for state management, React Router for routing, React Bootstrap for styling.
- **Vite** for fast development builds.

**Q7: How does WebSocket help with real‑time updates?**  
WebSocket maintains a persistent connection, allowing the server to push queue changes immediately. This avoids polling and provides a live experience. We use STOMP over WebSocket for messaging and Spring’s `SimpMessagingTemplate` to broadcast updates.

---

### 19.3 Backend – Spring Boot & Data Access

**Q8: How do you handle authentication and authorisation?**  
We use JWT (JSON Web Tokens). On login, the server validates credentials and returns a signed JWT containing the user ID, role, email, etc. Each request includes the token in the `Authorization` header. A custom `JwtAuthenticationFilter` validates the token and sets the `SecurityContext`. Method‑level security with `@PreAuthorize` (via custom annotations like `@AdminOnly`) enforces role‑based access.

**Q9: How do you prevent a user from joining multiple queues at once?**  
The `User` entity has an `activeTokenId` field. When a user joins a queue, we set this field to the token ID; when the token is completed/cancelled, we clear it. Before adding a new token, we check if `activeTokenId` is `null`. This ensures only one active token per user.

**Q10: How do you calculate the estimated wait time?**  
We multiply the number of waiting tokens by the average service time of the associated service. The average service time is stored in the `Service` entity and can be configured by the admin. This calculation is run every 30 seconds by a scheduled task.

**Q11: How do you handle concurrency when multiple users join a queue simultaneously?**  
MongoDB’s document‑level atomicity ensures that updates to a single queue document are serialised. When two requests try to add a token at the same time, the second one will see the updated document (the first one already incremented `tokenCounter` and added the token) and will succeed or fail based on capacity. This is safe without explicit locking.

**Q12: How do you implement group tokens?**  
A group token is a single token with a list of group members. When the token is served, all members are served together. The queue’s `supportsGroupToken` flag must be true. The frontend collects member details and sends them in a request to `/add-group-token`. The backend stores the members in the token’s `groupMembers` array.

**Q13: How do emergency tokens work?**  
If the queue has `emergencySupport` enabled, users can request an emergency token. If `autoApproveEmergency` is true, the token is added directly as `WAITING` with a higher priority. Otherwise, it goes to a `pendingEmergencyTokens` list. Providers see pending emergency tokens in their dashboard and can approve or reject them. Upon approval, the token moves to `WAITING` with high priority; upon rejection, the user is notified.

**Q14: How do you reset a queue while preserving data?**  
The `resetQueueWithOptions` endpoint accepts a `preserveData` flag. If true, we call `ExportService` to generate a PDF/Excel report of the current queue data and store it in a cache (`ExportCacheService`). The export URL is returned in the response. Then we clear all tokens, reset counters, and clear users’ `activeTokenId`. The export can be downloaded later.

**Q15: How do you handle feedback and ratings?**  
After a token is completed, the user is prompted to submit feedback. The `Feedback` entity stores ratings for overall, staff, service, and wait time, plus a comment. When feedback is submitted, we update the `Place` document’s `rating` and `totalRatings` fields by recalculating the average. Similarly, we can compute average ratings for providers.

**Q16: How do you manage audit logs?**  
We have an `AuditLog` collection. In key service methods (e.g., `QueueService.addNewToken`, `AuthService.login`), we call `auditLogService.logEvent(action, description, details)`. This stores the user ID (if authenticated), action, description, and a details map (e.g., tokenId, queueId). This helps with debugging and compliance.

**Q17: How do you ensure email delivery?**  
We use JavaMail with Gmail SMTP. In development, we use a test email account with an app password. For production, we would use a dedicated email service (e.g., SendGrid) or a custom domain. We also have retry logic (not shown) and logs to catch failures.

**Q18: How do you integrate Razorpay payments?**  
When an admin buys a token, the frontend requests an order. The backend creates a Razorpay order and stores a `Payment` document with `isPaid=false`. After the user pays, the frontend calls `/confirm` with the orderId and paymentId. The backend updates the payment and generates a unique token (e.g., `ADMIN-xxx`) stored in the `tokens` collection. The token is then used during registration.

**Q19: How do you handle token validation during registration?**  
In `AuthService.register`, if the role is ADMIN or PROVIDER, we look up the token in the `tokens` collection by its value. We check that it is not used, not expired, matches the role, and matches the email. If valid, we mark it as used and proceed with user creation.

---

### 19.4 MongoDB Design & Indexing

**Q20: Why did you embed tokens inside the queue document?**  
Tokens are never accessed without their queue. Embedding allows atomic updates (e.g., adding a token) and avoids a separate collection. It also makes fetching a queue with its tokens a single query.

**Q21: What indexes do you have and why?**
- Unique index on `users.email` – for fast login and uniqueness.
- 2dsphere index on `places.location` – for geospatial nearby search.
- Index on `queues.providerId`, `queues.placeId`, `queues.serviceId` – for filtering queues.
- Multikey index on `queues.tokens.userId` – for finding all queues a user is in.
- Unique compound index on `notification_preferences.userId` and `queueId` – ensures one preference per user per queue.
- TTL index on `otp.expiryTime` – automatically deletes expired OTPs.

**Q22: How do you handle geospatial queries?**  
We use MongoDB’s `$near` operator with a 2dsphere index on `places.location`. The repository method `findByLocationNear` accepts longitude, latitude, and radius (in meters). The controller exposes an endpoint `/api/places/nearby`. The frontend passes the user’s current location.

**Q23: How do you search across places, services, and queues?**  
We have a `SearchService` that builds separate queries for each type. For places, we use criteria on name, type, rating, and optionally location. For services, we use the place IDs from the place search (if any) and filter by name, features, etc. For queues, we use place IDs and filter by wait time, features. Results are combined in a `SearchResultDTO`. Pagination is handled with Spring’s `Pageable`.

**Q24: How do you aggregate data for analytics?**  
We store hourly snapshots of queue waiting counts in the `queue_hourly_stats` collection. For token volume over time, we query the `queues` collection and count completed tokens by day (in‑memory aggregation). For busiest hours, we group the hourly stats by hour and average the waiting count.

---

### 19.5 Security

**Q25: How do you secure WebSocket connections?**  
We use a `StompJwtChannelInterceptor` that intercepts the CONNECT frame, extracts the JWT from the `Authorization` header, validates it, and sets the user in the `StompHeaderAccessor`. Then `WebSocketSecurityConfig` ensures that only authenticated users can subscribe to `/user/**` and `/topic/queues/*` topics.

**Q26: How do you prevent CSRF attacks?**  
We disable CSRF because we use stateless JWT authentication. The token is sent in the `Authorization` header, which is not automatically included by browsers, so CSRF attacks are not possible.

**Q27: How do you store passwords?**  
We use `BCryptPasswordEncoder` with strength 10. Passwords are never stored in plain text; only the hash is saved. During login, we compare the plain text password with the stored hash using `matches()`.

**Q28: How do you handle rate limiting?**  
We have a `RateLimitFilter` that uses Bucket4j. For each request, we build a key based on the user ID (if authenticated) or IP, plus the URI. We also categorise endpoints (e.g., token creation, search) and apply different limits (e.g., 10/min for token creation, 50/min for search, 100/min default). If the bucket is empty, we return 429 Too Many Requests.

---

### 19.6 Frontend – React & Redux

**Q29: How do you manage global state?**  
We use Redux Toolkit. Slices for auth, places, services, search, user, etc. Each slice defines initial state, reducers, and async thunks. The store is configured in `store.js`. Components access state via `useSelector` and dispatch actions via `useDispatch`.

**Q30: How do you handle authentication in the frontend?**  
After a successful login, we store the JWT in `localStorage` and in Redux. Axios interceptors add the token to every request. If a 401 response is received, we log the user out and redirect to login. The `ProtectedRoute` component checks if the user is authenticated and has the required role before rendering a page.

**Q31: How do you manage real‑time updates?**  
We have a `WebSocketService` singleton that connects to the backend using STOMP. It subscribes to queue topics and dispatches Redux actions when messages arrive. Components subscribe/unsubscribe in `useEffect`. The service also handles reconnection and token renewal.

**Q32: How do you handle file uploads (profile images)?**  
We use a file input and `FormData`. The `authService.uploadProfileImage` sends a `multipart/form‑data` request to `/user/profile/image`. On success, we update the Redux store with the new image URL. The image is displayed with a cache‑busting query parameter.

**Q33: How do you implement dark mode?**  
The user preference is stored in Redux and `localStorage`. When toggled, we add/remove a `dark-mode` class on the `body`. CSS variables define light and dark colors; components use these variables for styling.

**Q34: How do you test frontend components?**  
Currently, we have no frontend tests, but we plan to use Jest and React Testing Library. We would mock API calls and Redux state, render components, and assert that the expected elements appear and interactions work.

---

### 19.7 DevOps & Deployment

**Q35: How do you deploy the application?**  
We use Docker and Docker Compose. The `docker-compose.yml` defines services for backend, frontend, MongoDB, Redis, Prometheus, Grafana, Loki, and Promtail. Environment variables are passed from a `.env` file. The backend image uses a multi‑stage build to keep the image small. The frontend is built and served by Nginx.

**Q36: How do you handle SSL?**  
In development, we use self‑signed certificates (included in the repository). The frontend Nginx serves HTTPS using these certificates. The backend also runs on HTTPS with the same certificates. In production, we would replace them with certificates from a trusted CA (e.g., Let’s Encrypt).

**Q37: How do you monitor the application?**  
Spring Boot Actuator exposes metrics at `/actuator/prometheus`. Prometheus scrapes these metrics and stores them. Grafana connects to Prometheus to visualise metrics (e.g., request rate, error rate, custom queue metrics). Loki collects logs from Docker containers via Promtail, and Grafana can query logs.

**Q38: How do you handle CI/CD?**  
We have a GitHub Actions workflow that runs on push to `main` or `develop`. It first runs tests (backend and frontend). If tests pass, it builds Docker images and pushes them to GitHub Container Registry. This could be extended to deploy to a server via SSH or Kubernetes.

---

### 19.8 Advanced Topics & Troubleshooting

**Q39: How would you scale the system?**
- **Backend**: Stateless (JWT) so we can add more instances behind a load balancer.
- **MongoDB**: Use replica sets for high availability and sharding for horizontal scaling.
- **Redis**: Use a Redis cluster for distributed caching and rate limiting.
- **WebSocket**: With multiple backend instances, we need a shared message broker (e.g., RabbitMQ) instead of the in‑memory broker. Spring supports that with `@EnableWebSocketMessageBroker` and a broker relay.
- **Database queries**: Optimise indexes and use read replicas for reporting.

**Q40: How do you handle token expiration in WebSocket?**  
When a token expires, the next HTTP request will return 401, and the frontend redirects to login. For WebSocket, the connection will eventually fail when trying to send a message; the client can detect this and reconnect with a new token.

**Q41: How do you ensure that only one token is in‑service at a time?**  
`serveNextToken` first completes any existing in‑service token before moving the next waiting token to in‑service. This is done atomically within a single transaction (MongoDB document update). It also clears the user’s `activeTokenId` for the completed token.

**Q42: How do you handle a situation where a user closes the browser while they have an active token?**  
The token remains in the queue; it will eventually be served or cancelled by the provider. The user can still receive notifications via email or push (if they enabled them). If they come back, they can see their token status from the dashboard.

**Q43: How do you handle a large number of concurrent WebSocket connections?**  
The in‑memory broker is suitable for moderate loads. For high concurrency, we would switch to a full‑featured broker like RabbitMQ or ActiveMQ, which can handle thousands of connections. We would also ensure that the backend instances are scaled horizontally, with a load balancer distributing WebSocket connections.

**Q44: How do you test the system’s performance?**  
We could use JMeter or Gatling to simulate many users joining queues, serving tokens, etc., while monitoring metrics in Prometheus/Grafana. We would check response times, error rates, and resource usage. Load testing would help identify bottlenecks (e.g., database indexes, cache usage).

**Q45: What improvements would you make next?**
- **Mobile app** (React Native or Flutter).
- **Machine learning** to predict wait times and recommend best joining times.
- **GraphQL API** for more flexible queries.
- **Kubernetes** for orchestration.
- **Two‑factor authentication** for admins and providers.
- **More detailed analytics** (e.g., user retention, provider performance trends).

---

## 20. General Technical Interview Questions

This section contains common interview questions that may be asked about Java, Spring, databases, design patterns, testing, and other technologies used in the project. Prepare these answers to demonstrate your overall technical expertise.

---

### 20.1 Core Java

**Q1: Explain the difference between `==` and `.equals()` in Java.**  
`==` compares object references (memory addresses). `.equals()` compares the content of objects. For primitive types, `==` compares values. For custom objects, you should override `equals()` to define logical equality. In QueueLess, we use Lombok’s `@Data` which generates `equals()` and `hashCode()` based on fields.

**Q2: What are the main features of Java 8?**
- **Lambda expressions**: enable functional programming.
- **Streams API**: process collections declaratively.
- **Optional**: avoid `NullPointerException`.
- **New Date/Time API**: `LocalDateTime`, `Instant`, etc.
- **Default methods** in interfaces.
- **Method references**.

In QueueLess, we use streams to filter tokens (`queue.getTokens().stream().filter(...).count()`) and `Optional` in repository methods.

**Q3: What is the difference between `ArrayList` and `LinkedList`?**
- `ArrayList` uses a dynamic array; random access is O(1), insertion/deletion is O(n) (except at the end).
- `LinkedList` uses a doubly‑linked list; insertion/deletion is O(1) at the ends, but random access is O(n).
- In QueueLess, we rarely need large lists; we use `ArrayList` for simplicity.

**Q4: Explain `final`, `finally`, and `finalize`.**
- `final` – variable cannot be reassigned, method cannot be overridden, class cannot be extended.
- `finally` – block in `try‑catch` that always executes (for cleanup).
- `finalize` – deprecated method called by GC before object is reclaimed (not used in QueueLess).

**Q5: What is a `ConcurrentModificationException` and how to avoid it?**  
It occurs when a collection is modified while iterating over it (except via the iterator’s own `remove()`). To avoid, use `ConcurrentHashMap`, copy the collection, or use `removeIf()` (which uses iterator internally).

---

### 20.2 Spring Framework

**Q6: What is Dependency Injection? Why use it?**  
Dependency Injection (DI) is a pattern where objects receive their dependencies from an external source rather than creating them internally. It promotes loose coupling, testability, and maintainability. Spring implements DI via constructor injection (preferred) or field injection.

**Q7: What is the difference between `@Component`, `@Service`, `@Repository`, and `@Controller`?**  
All are stereotypes for Spring beans.
- `@Component` – generic stereotype.
- `@Service` – for business logic.
- `@Repository` – for DAO/Repository; translates persistence exceptions.
- `@Controller` – for web controllers (MVC).  
  `@RestController` is a combination of `@Controller` and `@ResponseBody`.

**Q8: Explain the Spring bean lifecycle.**
1. Instantiation.
2. Populate properties (DI).
3. `BeanNameAware` / `BeanFactoryAware` (if implemented).
4. `BeanPostProcessor` before initialisation.
5. `@PostConstruct` init method.
6. `InitializingBean.afterPropertiesSet()`.
7. Custom init method (`@Bean(initMethod="...")`).
8. `BeanPostProcessor` after initialisation.
9. Bean ready.
10. On shutdown, `@PreDestroy` → `DisposableBean.destroy()` → custom destroy method.

**Q9: What is AOP? Where do you use it in Spring?**  
Aspect‑Oriented Programming separates cross‑cutting concerns (e.g., logging, transactions, security). In Spring, you can use `@Aspect` to define aspects. In QueueLess, we use method‑level security (`@PreAuthorize`), which is implemented via Spring AOP. Also, `@Transactional` (not used) is another example.

**Q10: What are the different scopes of a Spring bean?**
- **singleton** (default): one instance per container.
- **prototype**: new instance each time requested.
- **request**: one per HTTP request (web context).
- **session**: one per HTTP session.
- **application**: one per ServletContext.
- **websocket**: one per WebSocket session.

In QueueLess, all beans are singleton (stateless services).

**Q11: Explain Spring’s transaction management.**  
Spring provides declarative transaction management via `@Transactional`. It uses AOP to wrap methods. The `PlatformTransactionManager` (e.g., `MongoTransactionManager` for MongoDB) handles commit/rollback. MongoDB supports transactions in replica sets. We don’t use transactions in QueueLess because our operations are atomic at the document level, but we could add them for multi‑document operations.

---

### 20.3 Spring Boot

**Q12: What is Spring Boot? How does it differ from Spring?**  
Spring Boot simplifies Spring application development by providing:
- Auto‑configuration (based on classpath dependencies).
- Starter dependencies (pre‑defined sets).
- Embedded servers (Tomcat, Jetty).
- Production‑ready features (actuator, metrics).
- No XML configuration.

**Q13: How does auto‑configuration work?**  
Spring Boot scans the classpath for `META-INF/spring.factories` files, which list configuration classes. It then evaluates conditions (e.g., `@ConditionalOnClass`, `@ConditionalOnMissingBean`) to decide whether to apply a configuration. For example, if `spring‑data‑mongodb` is on the classpath, it auto‑configures a `MongoTemplate`.

**Q14: What is Spring Boot Actuator?**  
Actuator provides production‑ready endpoints (e.g., `/health`, `/info`, `/metrics`, `/prometheus`). In QueueLess, we expose the `/actuator/prometheus` endpoint for Prometheus scraping.

**Q15: How do you externalise configuration?**  
Use `application.properties` or `application.yml`, environment variables, command‑line arguments, or `@Value`. QueueLess uses a mix: `application.properties` with placeholders like `${JWT_SECRET}` that are replaced by environment variables.

---

### 20.4 Databases & JPA / MongoDB

**Q16: What is the difference between SQL and NoSQL?**
- SQL: relational, predefined schema, ACID transactions, joins.
- NoSQL: non‑relational, flexible schema, horizontal scaling, eventually consistent.  
  MongoDB (document store) is used in QueueLess because it matches the nested data model (tokens inside queues) and provides geospatial queries.

**Q17: What are the advantages of using MongoDB over a relational database for QueueLess?**
- Document model fits the queue‑token hierarchy.
- Geospatial indexes for nearby search.
- Flexible schema allows adding fields (e.g., `isActive`) without migrations.
- Horizontal scaling with sharding.

**Q18: What are indexes? Why are they important?**  
Indexes speed up data retrieval. They are data structures that store a small portion of the collection’s data in an easy‑to‑traverse form. Without indexes, MongoDB must scan every document (collection scan). In QueueLess, we have indexes on `users.email`, `places.location`, `queues.providerId`, etc.

**Q19: What is a TTL index?**  
A TTL index automatically deletes documents after a certain time. Used for OTPs in QueueLess (the `otp` collection has a TTL index on `expiryTime`). This avoids manual cleanup.

**Q20: What is the difference between `save` and `insert` in MongoDB?**  
`save` upserts (insert if not exists, update if exists) based on the `_id` field. `insert` only inserts, throwing an error if the document already exists. Spring Data’s `MongoRepository.save` uses upsert logic.

---

### 20.5 Security

**Q21: What is JWT? How does it work?**  
JSON Web Token (JWT) is a compact, URL‑safe token format consisting of three parts: header, payload, signature. The payload contains claims (user ID, role, expiration). The token is signed with a secret key, ensuring integrity. The client sends the token in the `Authorization` header. The server validates the signature and extracts claims without needing a session.

**Q22: How do you implement authentication in a stateless application?**  
Use JWT. On login, generate a token and return it. The client stores it and sends it with every request. The server validates the token on each request (without storing session state). This scales horizontally because any instance can validate the token using the shared secret.

**Q23: What is CSRF? How does Spring Security protect against it?**  
Cross‑Site Request Forgery (CSRF) tricks a user into executing unwanted actions on a web application where they are authenticated. Spring Security protects by generating a CSRF token that must be included in state‑changing requests. Since we use JWT and the token is sent in the header (not automatically included by browsers), we disable CSRF protection.

**Q24: What is the difference between `hasRole` and `hasAuthority`?**  
`hasRole` automatically adds the `ROLE_` prefix (e.g., `hasRole('ADMIN')` becomes `ROLE_ADMIN`). `hasAuthority` checks the exact authority string. In QueueLess, we use `hasRole` with custom annotations for readability.

---

### 20.6 WebSocket & Messaging

**Q25: What is WebSocket? How does it differ from HTTP?**  
WebSocket provides full‑duplex, persistent communication over a single TCP connection. HTTP is request‑response; each request opens a new connection (or uses keep‑alive). WebSocket allows the server to push data to the client without polling, making it ideal for real‑time apps.

**Q26: What is STOMP? Why use it with WebSocket?**  
STOMP (Simple Text Oriented Messaging Protocol) is a messaging protocol that defines frames (CONNECT, SUBSCRIBE, SEND) over WebSocket. It provides a higher‑level abstraction with destinations, subscriptions, and message headers. Spring uses STOMP to map messages to controller methods and to manage user‑specific destinations.

**Q27: How do you handle user‑specific messages in WebSocket?**  
Spring translates `/user/queue/...` destinations to a user‑specific queue (e.g., `/queue/...-user`). When sending to `convertAndSendToUser`, Spring adds the user’s session ID. The `StompJwtChannelInterceptor` sets the user in the `StompHeaderAccessor`, which Spring uses to route messages.

---

### 20.7 Microservices & Cloud (Optional)

**Q28: What are microservices? How does QueueLess align with microservices?**  
Microservices are an architectural style where an application is composed of small, independent services. QueueLess is a monolith but could be split into services (auth, queue, notification, payment). This would improve scalability and development independence.

**Q29: How would you scale the backend horizontally?**
- Ensure statelessness (JWT, no session affinity).
- Use a shared database (MongoDB replica set) and Redis cluster.
- Use a load balancer to distribute traffic.
- For WebSocket, use a message broker (e.g., RabbitMQ) to broadcast updates across instances.

---

### 20.8 Design Patterns

**Q30: What design patterns are used in QueueLess?**
- **Repository pattern**: Spring Data repositories abstract data access.
- **DTO pattern**: Separate DTOs from entities (e.g., `PlaceDTO` vs `Place`).
- **Builder pattern**: Lombok `@Builder` for constructing objects (e.g., `User.builder()`).
- **Singleton pattern**: Spring beans are singletons by default.
- **Observer pattern**: WebSocket broadcasts (publish‑subscribe).
- **Factory pattern**: `RateLimitConfig` creates different `Bucket` instances based on endpoint group.
- **Interceptor pattern**: `JwtAuthenticationFilter`, `StompJwtChannelInterceptor`.
- **AOP**: `@PreAuthorize` for security.

**Q31: What is the difference between DTO and Entity?**  
An entity is a Java object that maps to a database table/collection. It may contain sensitive data and business logic. A DTO is a data transfer object used to send data over the wire, often containing only a subset of fields and with validation annotations. In QueueLess, we use DTOs in controllers and entities in the service/repository layers.

---

### 20.9 Testing

**Q32: What is the difference between unit tests and integration tests?**
- **Unit tests**: test a single component (e.g., a service) in isolation, mocking its dependencies. Fast and focused.
- **Integration tests**: test how components interact (e.g., controller → service → database). They use real dependencies (embedded database, test containers) and are slower.

**Q33: How do you test Spring controllers?**  
Using `@WebMvcTest` and `MockMvc`. You mock the service layer and perform HTTP requests, verifying status codes and response bodies.

**Q34: How do you test a method that uses `@Scheduled`?**  
You can call the method directly in a test, or use `@SpringBootTest` with `TestExecutionListeners` to trigger the scheduler. Mocking the dependencies allows you to verify that the scheduler calls the expected methods.

**Q35: How do you test WebSocket endpoints?**  
You can write unit tests for the `@MessageMapping` methods using `MockMvc` (for the message handling), or use an embedded STOMP client in integration tests. In QueueLess, we have unit tests for `QueueWebSocketController` that mock the messaging template.

---

### 20.10 DevOps & CI/CD

**Q36: What is Docker? Why do you use it?**  
Docker is a containerisation platform that packages an application and its dependencies into a lightweight, portable image. It ensures consistency across environments. In QueueLess, we use Docker to run the backend, frontend, MongoDB, Redis, and monitoring tools together.

**Q37: What is a multi‑stage build?**  
A multi‑stage build uses multiple `FROM` instructions in a Dockerfile to create smaller final images. The first stage compiles the code, the second stage copies only the compiled artifacts. This reduces image size (e.g., our backend image doesn’t include Maven or source code).

**Q38: What is Docker Compose?**  
Docker Compose defines and runs multi‑container Docker applications using a YAML file. It sets up networks, volumes, and service dependencies. Our `docker-compose.yml` starts all services (backend, frontend, MongoDB, Redis, Prometheus, Grafana, Loki, Promtail) with one command.

**Q39: What is CI/CD? How do you implement it?**  
CI (Continuous Integration) automates building and testing on every push. CD (Continuous Delivery/Deployment) automates deployment. We use GitHub Actions: on push to `main`, it runs tests, builds Docker images, and pushes them to GHCR (GitHub Container Registry). Deployment could be automated to a server via SSH or Kubernetes.

---

### 20.11 Soft Skills & Problem Solving

**Q40: Describe a challenging bug you encountered and how you solved it.**  
*(Example)*: A problem where the estimated wait time was not updating correctly. I traced the issue to the scheduler that updates wait times; it was using an old value of average service time because the service object was cached. I fixed it by evicting the service cache after updates. This taught me to be careful with caching and to test cache invalidation.

**Q41: How do you ensure code quality?**
- Write unit and integration tests.
- Use static analysis tools (e.g., SonarQube, Checkstyle).
- Conduct code reviews.
- Follow consistent coding standards (e.g., Java naming, formatting).
- Use `@Builder` and DTOs to maintain clear contracts.

**Q42: How do you handle technical debt?**
- Prioritise refactoring when adding new features.
- Keep a backlog of known issues.
- Use metrics (e.g., cyclomatic complexity, test coverage) to identify problem areas.
- Schedule dedicated refactoring sprints.

**Q43: How do you keep up with new technologies?**
- Follow blogs (Baeldung, Spring blog, InfoQ).
- Watch conference talks (SpringOne, Devoxx).
- Experiment with small side projects.
- Read official documentation.

---

## 21. Data Structures & Algorithms in QueueLess

This section explains the data structures used throughout the QueueLess codebase and why they were chosen, along with relevant algorithms. Understanding these choices demonstrates your ability to select appropriate data structures for real‑world problems.

### 21.1 Embedded List for Tokens (`List<QueueToken>`)

**Where it’s used:**  
In the `Queue` entity, tokens are stored as a `List<QueueToken>` embedded directly inside the queue document.

**Why a list?**
- Tokens are naturally ordered by priority and issue time. A list preserves this order.
- We frequently need to access tokens by index (e.g., to serve the next waiting token). List provides O(1) access by position.
- We also need to insert at the end (when a new token is added) – O(1) amortised.

**Why embedded?**
- Tokens are never queried without their parent queue. Embedding avoids a separate collection and reduces database round trips.
- Updates to tokens (e.g., changing status) are atomic with the queue document.

**Complexity considerations**
- Inserting a token is O(1) amortised (append to the end).
- Removing a token (when completed/cancelled) is O(n) if we remove by value, but we rarely remove by value; we update status instead. When we do remove (e.g., during reset), we clear the whole list, which is O(n).
- Accessing a specific token by `tokenId` is O(n) unless we index the list, but we rarely need to access by ID directly – we usually search within the list. In practice, queue size is limited (max capacity ~100), so linear search is acceptable.

---

### 21.2 Map for Notification Preferences (`Map<String, NotificationPreference>`)

**Where it’s used:**  
In `TokenNotificationScheduler`, we convert the list of preferences for a queue into a map keyed by `userId` for fast lookups.

**Why a map?**
- For each token, we need to check if the user has custom notification settings. A map provides O(1) average lookup time, compared to O(n) for a list.

**Implementation**
```java
Map<String, NotificationPreference> prefMap = preferenceService.getPreferencesForQueue(queue.getId())
        .stream()
        .collect(Collectors.toMap(NotificationPreference::getUserId, Function.identity()));
```

**Why not a database index?**  
MongoDB can index `queueId` to quickly fetch all preferences for a queue, but within the application we still need to map them to users. Using a map in memory avoids repeated linear scans.

---

### 21.3 Priority Queue for Emergency Tokens

**Where it’s used:**  
When serving the next token, we need to pick the waiting token with the highest priority. In `QueueService.serveNextToken()`, we sort the waiting tokens using a comparator:

```java
Optional<QueueToken> nextToken = queue.getTokens().stream()
        .filter(t -> TokenStatus.WAITING.toString().equals(t.getStatus()))
        .max((t1, t2) -> {
            if (!t1.getPriority().equals(t2.getPriority()))
                return t2.getPriority() - t1.getPriority();
            return t1.getIssuedAt().compareTo(t2.getIssuedAt());
        });
```

This is essentially a priority queue implemented as a max‑heap, but we use a stream with `max` which internally uses a linear scan (O(n)). Since the number of waiting tokens is limited by `maxCapacity` (usually ≤100), O(n) is acceptable.

If the queue grew very large, we could maintain a real priority queue in memory, but the overhead of synchronising with the database would be high.

**Why not a separate collection for emergency tokens?**  
Emergency tokens are still part of the main waiting list; they just have a higher `priority` value. This simplifies the data model.

---

### 21.4 Caching with Redis (Key‑Value Store)

**Where it’s used:**  
Spring Cache with Redis caches places, services, and queues.

**Why key‑value?**
- Simple lookup by ID: `places::<id>`.
- Fast, in‑memory access.
- Built‑in TTL for expiration.

**Why Redis over a local in‑memory cache like Caffeine?**
- Redis is distributed, so multiple backend instances share the same cache. This avoids stale data across instances.
- It also persists data (optional), though we don’t need persistence for cache.

---

### 21.5 Unique Constraints Using Set Semantics

**Where it’s used:**
- `users.email` – unique index.
- `tokens.tokenValue` – unique index.
- `notification_preferences (userId, queueId)` – unique compound index.

**Why?**
- Prevent duplicate entries.
- Enforce business rules (e.g., one token per user per queue).
- Leverage MongoDB’s unique indexes for automatic enforcement.

---

### 21.6 Geospatial Queries (2dsphere Index)

**Where it’s used:**
- Finding places near a given coordinate.

**How it works:**  
MongoDB uses a **2dsphere index** and a **geohash**‑based algorithm to quickly locate points within a given radius. The query is O(log n) for indexed searches, compared to O(n) for a full collection scan.

**Algorithm:**
- The `$near` operator returns results sorted by distance.
- Internally, it uses the index to find points in increasing distance order, stopping when the maximum distance is reached.

---

### 21.7 Sorting & Reordering

**Where it’s used:**
- Reordering tokens in a queue: the frontend sends a new order of tokens (list of `QueueToken` objects). The backend replaces the entire `tokens` list with the new order.

**Why not a linked list?**
- MongoDB documents are stored as BSON; reordering a list means replacing the whole array. This is acceptable because the number of tokens is small (max capacity).
- Using a linked list in the database would be complex and not atomic.

**Algorithm for reordering:**  
The frontend uses drag‑and‑drop, producing a new array. The backend validates that all tokens belong to the queue and that the order respects priority (though not enforced). The operation is O(n) for replacement.

---

### 21.8 Aggregation Pipeline (Search)

**Where it’s used:**
- Searching across services and places (e.g., `$lookup` to join services with places).

**Why aggregation?**
- It allows joining collections and transforming data in a single database query, reducing network round trips.

**Complexity:**
- `$lookup` is O(n + m) where n is the number of input documents and m is the size of the joined collection, assuming indexes on the foreign field. We have indexes on `services.placeId` and `places._id`.

---

### 21.9 Algorithm for Best Time to Join

**Where it’s used:**
- `/api/queues/{queueId}/best-time` returns the hours with the lowest average waiting count.

**Algorithm:**
1. Fetch hourly stats for the last 30 days.
2. Group by hour and calculate average waiting count.
3. Sort hours by average waiting count ascending.
4. Return the top 3 hours.

**Complexity:**
- O(k) where k is the number of hourly stats (max 30 days * 24 = 720 records). Sorting is O(m log m) with m ≤ 24 (hours). Very fast.

---

### 21.10 Token Prioritisation (Emergency Tokens)

**Algorithm:**
- When serving the next token, we select the waiting token with the highest `priority` value, and among those, the earliest `issuedAt`. This is a **stable sort** by priority then time.

**Why a simple comparator?**
- Because the number of waiting tokens is small, O(n) linear search is fine. If we had thousands of waiting tokens, we could maintain a priority queue in memory, but that would require synchronising with the database.

---

### 21.11 Complexity Summary

| Operation                           | Data Structure        | Complexity   | Notes                                      |
|-------------------------------------|-----------------------|--------------|--------------------------------------------|
| Add token                           | List (embedded)       | O(1) amortised| Append to list                              |
| Serve next token                    | List (linear scan)    | O(n)         | n ≤ max capacity (~100)                    |
| Lookup notification preference      | HashMap (in memory)   | O(1) average | Built from database list                   |
| Find place by ID                    | Redis cache           | O(1)         | After first fetch                          |
| Nearby search                       | 2dsphere index        | O(log n)     | Uses geospatial index                      |
| Aggregate hourly stats              | Collection scan       | O(k)         | k ≤ 720 (max 30 days * 24)                 |
| Reorder queue                       | List replacement      | O(n)         | n ≤ max capacity                           |

---

### 21.12 Interview Questions on Data Structures & Algorithms

**Q1: Why did you choose a list to store tokens instead of a separate collection?**
- Tokens are always accessed within the context of their queue. Embedding avoids a join and ensures atomic updates. The list order is preserved, which is crucial for the queue order.

**Q2: How do you ensure that emergency tokens are served first?**
- Each token has a `priority` field (0 for regular, higher for emergency). When serving the next token, we sort waiting tokens by priority descending, then by issue time ascending.

**Q3: What is the time complexity of serving the next token?**
- O(n) where n is the number of waiting tokens. Because we scan the list to find the highest priority token. Since queue capacity is limited, this is acceptable.

**Q4: How does the geospatial search work?**
- MongoDB uses a 2dsphere index and a geohash algorithm to quickly find points near a given coordinate. The query `$near` returns results sorted by distance.

**Q5: How would you improve the performance of serving tokens if a queue could have thousands of waiting tokens?**
- I would maintain a priority queue (e.g., using a Redis sorted set or a separate collection) where tokens are inserted with a score that combines priority and issue time. Then serving the next token would be O(log n). However, synchronising this with the queue document would add complexity.

**Q6: What data structure would you use to implement a custom ordering of waiting tokens (drag‑and‑drop)?**
- A list is sufficient because the frontend sends the new order as an array. We replace the entire list in the database. Since the list is small, this is fine.

**Q7: How do you handle the situation where a user has multiple FCM tokens (multiple devices)?**
- We store the tokens in a `List<String>` in the user document. When sending a push notification, we send a multicast to all tokens. This is efficient because FCM handles the fan‑out.

**Q8: How do you ensure that a user does not join two queues simultaneously?**
- The `User` entity has an `activeTokenId` field, which acts as a pointer to the user’s active token. We check this field before allowing a new token. This is essentially a set of active tokens per user (size 1). If we allowed multiple active tokens, we could use a `Set<String>`.

**Q9: Why do you use a map for notification preferences instead of querying the database for each token?**
- To avoid O(n) database queries for each token. We fetch all preferences for the queue once and build a map for O(1) lookup per token.

**Q10: How does MongoDB’s 2dsphere index work?**
- It uses a geohash‑based grid to index coordinates. Points are stored in a hierarchical grid, and queries use the grid to quickly find points within a bounding box, then refine by distance.

---

## 22. Data Structures Used in QueueLess

This section lists every data structure used in the codebase, explains where it appears, and justifies the choice. Understanding these choices demonstrates your ability to select appropriate structures for real‑world scenarios.

### 22.1 Backend – Java Collections

| Data Structure         | Where Used                                                                 | Why                                                                                                                             |
|------------------------|----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| **`List<QueueToken>`** | Embedded in `Queue` entity (tokens field).                                 | Tokens are ordered (by priority and issue time). A list preserves order and allows O(1) access by index for serving next token. The list size is bounded by queue capacity (≤100), so linear scans are acceptable. |
| **`List<String>`**     | `User.fcmTokens`, `User.managedPlaceIds`, `User.ownedPlaceIds`.            | Simple ordered collection of unique identifiers; we don’t need fast lookups by value, only iteration or removal by value (which is O(n) but small). |
| **`Map<String, NotificationPreference>`** | In `TokenNotificationScheduler`, built from list of preferences.         | Provides O(1) lookup of a user’s preference for a queue, avoiding a linear scan for each token.                                  |
| **`Map<String, Object>`** | In `AdminService.getDashboardStats` and other analytics methods.         | Flexible container for returning JSON responses with arbitrary key‑value pairs.                                                 |
| **`Set<String>`**      | Used in `AdminService.updateProvider` to validate place IDs.              | Fast O(1) membership test to ensure all new place IDs belong to the admin.                                                     |
| **`HashMap`**          | In `RateLimitConfig.cache` for storing rate limit buckets.                | ConcurrentHashMap (since it’s a Map) for thread‑safe bucket storage per key.                                                    |
| **`ArrayList`**        | Default collection used by Spring Data for many repository results.       | Simple, fast for iteration.                                                                                                     |
| **`Stack`** (indirect) | Not used directly; the call stack is managed by the JVM.                  |                                                                                                                                |

### 22.2 Backend – MongoDB Data Structures (Embedded & Indexed)

| Structure               | Where Used                                        | Why                                                                                                                           |
|-------------------------|---------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------|
| **Embedded Document**   | `Queue.tokens` (list of `QueueToken`)             | Tokens are always accessed with their queue; embedding avoids joins and ensures atomic updates.                               |
| **Embedded Document**   | `Queue.pendingEmergencyTokens` (list)             | Separate from main tokens for approval workflow; still part of the queue document.                                            |
| **Embedded Document**   | `User.preferences` (object)                       | Preferences are only relevant when the user is loaded; embedding simplifies queries.                                          |
| **References**          | `Service.placeId`, `Queue.placeId`, etc.          | Places, services, queues are independent entities; references keep them separate while linking.                               |
| **GeoJSON Point**       | `Place.location` (GeoJsonPoint)                   | Required for geospatial queries; MongoDB’s `$near` operator works with GeoJSON.                                               |
| **TTL Index**           | `otp.expiryTime`                                  | Automatically deletes expired OTP documents.                                                                                  |
| **Unique Index**        | `users.email`, `tokens.tokenValue`, `notification_preferences (userId, queueId)` | Enforce uniqueness at the database level; faster queries.                                                                   |
| **Compound Index**      | `queues` (`tokens.userId`)                        | Supports queries like “find all queues a user is in”.                                                                         |
| **2dsphere Index**      | `places.location`                                 | Enables efficient `$near` queries for nearby search.                                                                          |

### 22.3 Backend – Redis Data Structures

| Structure          | Where Used                                | Why                                                                                                      |
|--------------------|-------------------------------------------|----------------------------------------------------------------------------------------------------------|
| **Key‑Value (String)** | Spring Cache (`places`, `services`, etc.) | Simple cache by ID; Redis strings store JSON‑serialised objects.                                         |
| **Hash**           | Not used                                  | Could be used for storing user session data if needed.                                                   |
| **Sorted Set**     | Not used                                  | Could be used for priority queues (e.g., token ordering) but currently not needed.                      |
| **List**           | Not used                                  | Could be used for log queues.                                                                            |

### 22.4 Frontend – JavaScript Data Structures

| Structure               | Where Used                                                                                 | Why                                                                                                 |
|-------------------------|--------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------|
| **Array**               | Everywhere (state variables, API responses, Redux slices).                                 | Most common collection; easy to iterate, map, filter.                                               |
| **Object**              | State objects (e.g., `formData`), Redux slices.                                            | Key‑value store for structured data.                                                                |
| **Map / Set**           | Rarely used (e.g., could be used in `useInView` hook).                                     | Not heavily used; arrays suffice for small collections.                                             |
| **WeakMap**             | Not used.                                                                                  | Could be used for caching DOM elements but not needed.                                              |
| **Redux State (Object)**| Entire Redux store (normalised state).                                                     | Centralised state tree; Redux Toolkit uses Immer to treat state as immutable.                      |

### 22.5 Algorithms & Complexity

| Operation                     | Data Structure       | Time Complexity | Notes                                                                 |
|-------------------------------|----------------------|-----------------|-----------------------------------------------------------------------|
| Add token                     | `List` (append)      | O(1) amortised  | Insert at end of list.                                                |
| Serve next token              | `List` (linear scan) | O(n)            | n ≤ max capacity (usually ≤100).                                      |
| Lookup notification pref      | `Map` (hash)         | O(1)            | After building map once per queue.                                    |
| Find place by ID              | Redis (cache)        | O(1)            | After first fetch.                                                    |
| Nearby places                 | MongoDB 2dsphere     | O(log n)        | Uses geospatial index.                                                |
| Aggregate hourly stats        | Collection scan      | O(k)            | k ≤ 720 (30 days * 24).                                               |
| Reorder queue                 | `List` replace       | O(n)            | Replace entire list in database.                                      |
| Validate token during reg     | Unique index         | O(1)            | Database index lookup.                                                |

### 22.6 Why Not Other Data Structures?

| Alternative                 | Why Not Used                                                                                     |
|-----------------------------|--------------------------------------------------------------------------------------------------|
| **LinkedList** for tokens   | We need random access by position (to serve next). Also, LinkedList overhead is higher than ArrayList. |
| **PriorityQueue** in memory | Synchronising with database would be complex; queue size is small enough for linear scan.        |
| **TreeMap**                 | Not needed; simple HashMap suffices.                                                             |
| **Graph**                   | No graph‑like relationships; all relationships are tree‑like or references.                      |
| **Set** for user tokens     | Users have only one active token, so a single field (`activeTokenId`) is simpler.                |

---

### 22.7 Interview Questions on Data Structures

**Q1: Why did you use a `List` to store tokens instead of a `Queue` or `PriorityQueue`?**
- We need to maintain the order of tokens as they are added and also allow reordering (drag‑and‑drop). A `Queue` would not support reordering. A `PriorityQueue` would reorder automatically, but the provider may want a custom order (e.g., drag emergency token to the front). A `List` gives us full control.

**Q2: How do you ensure that the token order is preserved in the database?**
- MongoDB stores arrays in the order of insertion. When we reorder, we replace the entire array. The order is thus preserved exactly as provided.

**Q3: Why did you embed tokens instead of using a separate collection?**
- To avoid a join and ensure atomic updates. If we had a separate collection, adding a token would require two database operations (insert token, update queue). With embedding, it’s a single update.

**Q4: How would you handle a scenario where a queue could have thousands of waiting tokens?**
- I would reconsider embedding and move tokens to a separate collection with a `queueId` index. For serving next, I would query the waiting tokens sorted by priority and issue time, with a limit of 1. This would be O(log n) with proper indexes. However, our current limit of 100 makes embedding acceptable.

**Q5: Why do you use a `Map` for notification preferences?**
- To avoid O(n) lookups for each token. We fetch all preferences for a queue once and build a map keyed by userId. This turns a linear scan into a constant‑time lookup.

**Q6: What data structure would you use to implement a real‑time leaderboard for providers?**
- A Redis sorted set would be ideal: key = `provider_leaderboard`, member = provider ID, score = rating or tokens served. Updates would be O(log n), and fetching top N would be O(log n + N). This is not yet implemented.

**Q7: How does the 2dsphere index work under the hood?**
- It uses a geohash‑based grid that partitions the earth into a hierarchical grid of cells. Points are stored with their geohash prefixes, and the index can quickly find points within a bounding box. MongoDB then filters by distance.

**Q8: Why did you choose a `Set` for place ID validation?**
- To check membership efficiently. `Set.contains()` is O(1) average, which is better than iterating through a list.

**Q9: What is the time complexity of the “best time to join” algorithm?**
- O(k) to collect stats (k ≤ 720) + O(24 log 24) to sort hours. Overall negligible.

**Q10: How would you implement a cache for search results?**
- I would use a Redis hash or a simple key‑value store with a key built from the search query and pagination parameters. However, because search queries are many and unique, caching might not be efficient; we rely on database indexes instead.

---

## 23. How to Present QueueLess in an Interview

### 23.1 Structuring Your Introduction

Start with a **30‑second elevator pitch**:

> *“QueueLess is a multi‑tenant queue management system that allows users to join digital queues remotely, providers to manage queues in real time, and administrators to onboard places and services. It uses Spring Boot with MongoDB, React with Redux, and real‑time updates via WebSocket. I built it to solve the inefficiency of physical waiting lines.”*

Then expand with a **2‑minute overview** covering:

- **Problem**: Physical queues waste time; businesses lack digital tools.
- **Solution**: Digital queue with real‑time updates, notifications, and analytics.
- **Target audience**: Users (join queues), providers (manage queues), admins (manage places/services).
- **Key technologies**: Spring Boot, MongoDB, React, WebSocket, JWT, Razorpay, FCM.
- **My role**: Full‑stack developer – designed the database, implemented authentication, built the queue logic, integrated payments, set up real‑time updates, and wrote tests.

### 23.2 Highlighting Key Features & Decisions

When asked about specific features, structure your answer with:

- **What**: Feature name and purpose.
- **How**: Technical implementation.
- **Why**: Trade‑offs and alternatives considered.

**Example: Real‑time updates**
> *“We use WebSocket with STOMP to push queue changes immediately. When a token is added or served, the backend broadcasts the updated queue to all subscribers. This avoids polling, reduces server load, and gives users a live experience. We chose STOMP over raw WebSocket because Spring provides excellent support and we could use user‑specific destinations easily.”*

**Example: Group tokens**
> *“A group token allows multiple people to join together. The provider can set a flag on the queue to enable group tokens. When a user creates a group token, they provide a list of members. The token is stored as a single entry in the queue but has an embedded list of members. When served, all members are served together. We embedded the members inside the token because they are never accessed separately.”*

### 23.3 Explaining the Architecture

Use a **diagram** if you can (whiteboard or on screen). Describe:

- **Client layer**: React SPA, WebSocket client.
- **Backend**: Spring Boot, REST controllers, service layer, repositories.
- **Database**: MongoDB (collections, indexes), Redis (cache).
- **External integrations**: Razorpay, Firebase, SMTP.
- **Monitoring**: Prometheus, Grafana, Loki.

**Key points to mention**:
- Stateless backend (JWT) for horizontal scaling.
- Embedded tokens in queue documents for atomic updates.
- Geospatial indexes for nearby search.
- Scheduled jobs for notifications and analytics.
- Caching with Redis to reduce database load.

### 23.4 Demonstrating Ownership & Depth

**Know your code**: Be prepared to explain any class, method, or annotation. For example:

- *“In `QueueService.addNewToken`, we first check `user.getActiveTokenId()` to ensure the user doesn’t already have an active token. This prevents double joining.”*
- *“The `@PreAuthorize` annotations are enforced by Spring Security; we created custom annotations like `@AdminOnly` for readability.”*

**Talk about trade‑offs**:
- *“We chose MongoDB because tokens are naturally nested; but if we had complex joins, we might have chosen PostgreSQL.”*
- *“We used an embedded list for tokens because queue sizes are limited to 100. If queues could grow huge, we would move tokens to a separate collection.”*

**Mention improvements you’d make**:
- *“I’d add refresh tokens to improve security and user experience.”*
- *“I’d migrate to a message broker like RabbitMQ for WebSocket to support multiple backend instances.”*

### 23.5 Handling Tricky Questions

**Q: “What would you do differently if you started over?”**  
*“I’d use a shared message broker for WebSocket from the start to prepare for horizontal scaling. I’d also add integration tests for WebSocket endpoints. The database design would stay largely the same.”*

**Q: “How do you ensure data consistency when resetting a queue?”**  
*“We wrap the reset in a service method that clears tokens, resets counters, and clears user active tokens. Although not transactional, the operations are idempotent. If an error occurs, we log it and the user can retry.”*

**Q: “How do you test the payment flow?”**  
*“We mock Razorpay using `MockedConstruction` to simulate order creation and payment confirmation. In integration tests, we use test mode API keys and simulate the frontend callback.”*

**Q: “What if the system grows to millions of users?”**  
*“We would scale horizontally: multiple backend instances behind a load balancer, MongoDB replica set + sharding, Redis cluster, and a message broker for WebSocket. The stateless design (JWT) already supports this.”*

### 23.6 Showcasing Your Contributions

- **Quantify** where possible: *“I reduced database load by 40% by caching frequently accessed places.”* (if you have numbers)
- **Mention challenges overcome**: *“I solved a race condition where two users could join a queue at the same time by using MongoDB’s atomic updates.”*
- **Talk about collaboration**: If you worked in a team, explain your role. If solo, emphasise that you built it end‑to‑end.

### 23.7 Presenting on a Whiteboard / Screen

- **Draw a high‑level diagram** of the architecture.
- **Walk through a user flow** (e.g., “A user searches for a place, joins a queue, receives a notification, and is served.”).
- **Show code snippets** (if allowed) to explain a complex piece (e.g., the token serving algorithm).

### 23.8 Preparing for Different Interview Formats

- **Technical interview**: Expect deep dives into Spring, MongoDB, WebSocket. Use the documentation to rehearse.
- **System design interview**: Focus on scalability, database sharding, message brokers, caching.
- **Coding interview**: Might ask to implement a simplified queue or a scheduler. Practice writing small versions of key algorithms.

### 23.9 Final Tips

- **Be confident**: You built this project; you know it inside out.
- **Stay honest**: If you don’t remember a detail, say “I’d need to check the code, but I believe it works like this…”
- **Engage the interviewer**: Ask if they want more details on a particular area.
- **Show enthusiasm**: Talk about what excites you about the project (e.g., real‑time updates, solving a real problem).

