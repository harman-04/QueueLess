
Project Overview & Architecture

Core Technologies & Why Chosen

Backend Deep Dive

3.1. Project Structure & Layers

3.2. Spring Boot Configuration

3.3. Security (JWT, role-based access, password encoding)

3.4. Authentication & Authorization (AuthController, AuthService, JwtTokenProvider)

3.5. Email & OTP Service

3.6. User Management (User entity, repository, service)

3.7. Place & Service Management

3.8. Queue Management (Queue entity, tokens, statuses, operations)

3.9. WebSocket & Real‑time Updates (STOMP, event handling)

3.10. Payments (Razorpay integration, token generation)

3.11. Feedback & Ratings

3.12. Search & Filtering (MongoDB geospatial, aggregation)

3.13. Analytics (charts, reports, export)

3.14. Notification Preferences & Push Notifications (FCM)

3.15. Admin & Provider Management

3.16. Audit Logging

3.17. Rate Limiting

3.18. Exception Handling & Validation

3.19. Caching with Redis

3.20. Testing Strategy

Frontend Deep Dive

4.1. Project Structure

4.2. State Management with Redux Toolkit

4.3. Authentication & Routing

4.4. Key Components (Navbar, PlaceList, Queue Management, etc.)

4.5. WebSocket Integration

4.6. Form Handling & Validation

4.7. UI/UX & Styling (Bootstrap, custom CSS, dark mode)

4.8. Notifications (toast, push)

4.9. Charting & Analytics

4.10. Testing

Deployment & DevOps

Docker, Docker Compose

SSL configuration

Monitoring (Prometheus, Grafana, Loki)

Interview Preparation

Common interview questions for each module

Design decisions & trade-offs

Potential bottlenecks & improvements

How to present the project

# QueueLess – Complete Project Documentation

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
%%{init: {'theme': 'base', 'themeVariables': { 'primaryColor': '#E1F5FE', 'edgeLabelBackground':'#f0f8ff', 'tertiaryColor': '#fff'}}}%%
graph TD
    %% Define Styles for the Main Container and Subgraphs
    classDef mainContainerStyle fill:#F5F9FC,stroke:#B0BEC5,stroke-width:2px,stroke-dasharray: 5 5;
    classDef hiddenSubgraph fill:none,stroke:none;

    %% Define Styles for Individual Nodes
    classDef browserNode fill:#ffffff,stroke:#0288D1,stroke-width:2px,rx:10,ry:10;
    classDef backendNode fill:#ffffff,stroke:#388E3C,stroke-width:2px,rx:10,ry:10;
    classDef dbNode fill:#ffffff,stroke:#E0E0E0,stroke-width:1px,rx:10,ry:10;
    classDef serviceNode fill:#ffffff,stroke:#E0E0E0,stroke-width:1px,rx:10,ry:10;

    %% Primary Container Subgraph
    subgraph MainContainer ["MODERN APPLICATION ARCHITECTURE DIAGRAM"]
        direction TD

        %% Core Nodes (Brower and Backend)
        UserBrowser[<br><b>USER BROWSER</b><br>]:::browserNode
        SpringBootBackend[<br><b>SPRING BOOT BACKEND</b><br>API & Business Logic<br>]:::backendNode

        %% Integration Services Subgraph Group
        subgraph IntegrationServices [Integration Services]
            direction LR
            FirebaseFCM[<br><b>FIREBASE FCM</b><br>]:::serviceNode
            SMTP[<br><b>SMTP (Email)</b><br>]:::serviceNode
        end

        %% Data and External API Subgraph Group
        subgraph DataPaymentsLayer [Data & Payments]
            direction LR
            Razorpay[<br><b>RAZORPAY</b><br>]:::serviceNode
            MongoDB[<br><b>MONGODB</b><br>NoSQL Database<br>]:::dbNode
            Redis[<br><b>REDIS</b><br>Caching & Messaging<br>]:::dbNode
        end
    end

    %% Apply the class to the primary container subgraph
    class MainContainer mainContainerStyle;
    %% Apply hidden style to the inner subgraph groups
    class IntegrationServices,DataPaymentsLayer hiddenSubgraph;

    %% Logical Connections (as shown in the provided diagram)
    UserBrowser -->|REST / WebSocket| SpringBootBackend
    UserBrowser --> Razorpay

    SpringBootBackend --> FirebaseFCM
    SpringBootBackend --> SMTP
    SpringBootBackend --> MongoDB
    SpringBootBackend --> Razorpay

    Razorpay --> SMTP
    MongoDB --> Redis
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

