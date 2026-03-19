
# QueueLess – Smart Queue Management System

QueueLess is a comprehensive, multi‑tenant queue management platform designed for businesses of all types – hospitals, banks, shops, restaurants, and more. It allows administrators to create and manage multiple locations (places), providers to handle queues in real time, and users to join queues, track their position, receive notifications, and provide feedback. The system features live updates via WebSocket, email/push notifications, Razorpay payment integration, role‑based access control, and detailed analytics.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture Overview](#architecture-overview)
4. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Installation](#installation)
   - [Environment Variables](#environment-variables)
   - [Running Locally](#running-locally)
   - [Docker Setup](#docker-setup)
5. [API Documentation](#api-documentation)
6. [Testing](#testing)
7. [Deployment](#deployment)
8. [Contributing](#contributing)
9. [License](#license)

---

## Features

### For End Users
- **Search & Discover**: Find places by name, type, location, or rating.
- **Join Queues**: Choose regular, group, or emergency tokens (with provider approval).
- **Real‑time Updates**: Live position, wait time, and status changes via WebSocket.
- **Notifications**: Email and push notifications before your turn, and when a queue becomes short.
- **Feedback**: Rate your experience with detailed dimensions (staff, service, wait time).
- **Favorites**: Save favourite places for quick access.
- **Token History**: View past tokens and ratings.

### For Providers
- **Queue Management**: Create, pause, resume, and reset queues.
- **Token Handling**: Serve next, complete, cancel tokens; view user details for each token.
- **Emergency Approvals**: Approve or reject emergency tokens with optional reason.
- **Dashboard**: Real‑time statistics, drag‑and‑drop reorder, export reports (PDF/Excel).
- **Analytics**: Token volume over time, busiest hours, average wait time trends.

### For Administrators
- **Place Management**: Create and update places with images, location, contact info, business hours.
- **Service Management**: Define services under each place (e.g., cardiology, haircut) with average service time.
- **Provider Management**: Add providers (via token purchase), update their details, assign places, enable/disable accounts, reset passwords.
- **Dashboard**: Overview of all places, queues, providers, payment history, and analytics charts.
- **Export**: Generate admin reports in PDF/Excel.
- **Alert Configuration**: Set wait time thresholds to receive email alerts.

### General
- **Authentication & Authorization**: JWT‑based with roles: `USER`, `PROVIDER`, `ADMIN`. Email verification via OTP.
- **Payments**: Razorpay integration for purchasing admin/provider tokens.
- **Audit Logging**: All critical actions are logged for compliance and debugging.
- **Rate Limiting**: Per‑user and IP‑based limits to prevent abuse.
- **Caching**: Redis cache for places, services, queues.
- **Monitoring**: Prometheus metrics, Grafana dashboards, Loki logs.

---

## Tech Stack

| Layer          | Technology                                                                                                                                                                                                                                                                                                                                                                                      |
|----------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Backend**    | Java 25, Spring Boot 3.5, Spring Security, Spring Data MongoDB, Spring WebSocket, JWT, Lombok                                                                                                                                                                                                                                   |
| **Frontend**   | React 18, Vite, Redux Toolkit, React Bootstrap, React Router, Formik & Yup, React Toastify, Recharts, STOMP.js, SockJS, Firebase Cloud Messaging                                                                                                                                                                                |
| **Database**   | MongoDB (primary), Redis (caching, OTP store)                                                                                                                                                                                                                                                                                   |
| **Messaging**  | WebSocket (STOMP)                                                                                                                                                                                                                                                                                                                |
| **Payments**   | Razorpay                                                                                                                                                                                                                                                                                                                         |
| **Notifications** | Firebase Cloud Messaging (push), JavaMail (SMTP)                                                                                                                                                                                                                                                                                |
| **Monitoring** | Prometheus, Grafana, Loki, Promtail                                                                                                                                                                                                                                                                                             |
| **DevOps**     | Docker, Docker Compose, Maven                                                                                                                                                                                                                                                                                                   |

---

## Architecture Overview

QueueLess follows a **client‑server architecture** with a Spring Boot backend and a React frontend.

- **Backend**: Exposes RESTful API and STOMP WebSocket endpoints. Business logic is encapsulated in services. Data persistence uses MongoDB (with geospatial queries). Redis is used for caching and temporary OTP storage.
- **Frontend**: Single‑page application built with React and Redux for state management. WebSocket client maintains live connections for queue updates.
- **Real‑time Updates**: When a queue changes (token added, served, completed), the backend broadcasts the updated queue via WebSocket to all subscribed clients.
- **Security**: All endpoints except public ones require a JWT token. Role‑based access is enforced with method‑level annotations (`@AdminOnly`, `@ProviderOnly`, etc.).
- **Payments**: Admin/Provider tokens are purchased via Razorpay; the backend generates and validates the tokens.
- **Push Notifications**: Firebase Cloud Messaging is used for sending notifications when a user’s turn is approaching or when a queue becomes short.

### Data Model Highlights
- **User** – can be USER, PROVIDER, or ADMIN.
- **Place** – represents a physical location (hospital, shop) with geo‑coordinates.
- **Service** – a specific service offered at a place (e.g., “Cardiology Consultation”).
- **Queue** – linked to a service and provider, contains a list of tokens.
- **Token** – represents a user’s spot in a queue; can be regular, group, or emergency.
- **Feedback** – submitted for completed tokens, includes multi‑dimensional ratings.
- **AuditLog** – records important actions for traceability.
- **NotificationPreference** – per‑queue notification settings for users.

---

## Getting Started

### Prerequisites

- **Java 25** (or newer)
- **Node.js 20+** and npm
- **MongoDB** (local or Atlas)
- **Redis** (local or cloud)
- **Razorpay account** (for payments)
- **Firebase project** (for push notifications)
- **SMTP server** (e.g., Gmail) for emails

### Installation

1. **Clone the repository**

   ```bash
   git clone https://github.com/yourusername/queueless.git
   cd queueless
   ```

2. **Backend setup**

   ```bash
   cd backend
   ./mvnw clean install
   ```

3. **Frontend setup**

   ```bash
   cd ../frontend
   npm install
   ```

### Environment Variables

Create a `.env` file in both `backend` and `frontend` directories.

#### Backend `.env` (inside `backend/`)

```properties
MONGODB_URI=mongodb://localhost:27017/queueless
REDIS_HOST=localhost
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION=86400000
RAZORPAY_KEY=rzp_test_xxxxxxxxxx
RAZORPAY_SECRET=your_razorpay_secret
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
SSL_KEY_STORE_PASSWORD=changeit
APP_FRONTEND_URL=https://localhost:5173
```

> **Note**: For Gmail, use an [App Password](https://support.google.com/accounts/answer/185833) if 2FA is enabled.

#### Frontend `.env` (inside `frontend/`)

```properties
VITE_API_BASE_URL=https://localhost:8443/api
VITE_FIREBASE_API_KEY=your_firebase_api_key
VITE_FIREBASE_AUTH_DOMAIN=your_firebase_auth_domain
VITE_FIREBASE_PROJECT_ID=your_firebase_project_id
VITE_FIREBASE_STORAGE_BUCKET=your_firebase_storage_bucket
VITE_FIREBASE_MESSAGING_SENDER_ID=your_firebase_messaging_sender_id
VITE_FIREBASE_APP_ID=your_firebase_app_id
VITE_FIREBASE_VAPID_KEY=your_firebase_vapid_key
VITE_RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxx
```

### Running Locally

#### Backend

```bash
cd backend
./mvnw spring-boot:run
```

The backend will start on `https://localhost:8443` (SSL enabled). You can access the API at `https://localhost:8443/api`.

#### Frontend

```bash
cd frontend
npm run dev
```

The frontend will be available at `https://localhost:5173`.

### Docker Setup

The project includes a `docker-compose.yml` that runs the entire stack (backend, frontend, MongoDB, Redis, Prometheus, Grafana, Loki, Promtail). Make sure you have Docker and Docker Compose installed.

1. **Build and start all services**

   ```bash
   docker-compose up --build
   ```

2. **Access the application**
   - Frontend: `https://localhost:5173`
   - Backend API: `https://localhost:8443`
   - Grafana: `http://localhost:3000` (admin/admin)
   - Prometheus: `http://localhost:9090`
   - Loki: `http://localhost:3100`

> **Note**: SSL certificates are self‑signed for development; browsers will show a warning – proceed anyway.

---

## API Documentation

Interactive API documentation is available via Swagger UI when the backend is running:

```
https://localhost:8443/swagger-ui.html
```

### Main API Endpoints

| Category         | Endpoint                     | Description                                  | Roles          |
|------------------|------------------------------|----------------------------------------------|----------------|
| **Auth**         | `/api/auth/register`         | Register a new user                          | Public         |
|                  | `/api/auth/login`            | Login, receive JWT                           | Public         |
|                  | `/api/auth/verify-email`     | Verify email with OTP                        | Public         |
| **Password**     | `/api/password/forgot`       | Request password reset OTP                    | Public         |
|                  | `/api/password/verify-otp`   | Verify OTP                                    | Public         |
|                  | `/api/password/reset`        | Reset password (with OTP)                     | Public         |
| **Payments**     | `/api/payment/create-order`  | Create Razorpay order                         | Public         |
|                  | `/api/payment/confirm`       | Confirm payment (admin token)                  | Public         |
|                  | `/api/payment/confirm-provider` | Confirm provider token                        | Public         |
| **Places**       | `/api/places`                | CRUD operations for places                     | Admin / Public |
| **Services**     | `/api/services`              | CRUD for services                              | Admin / Public |
| **Queues**       | `/api/queues`                | Manage queues and tokens                        | Mixed          |
| **Feedback**     | `/api/feedback`              | Submit and retrieve feedback                    | User / Public  |
| **Search**       | `/api/search`                | Comprehensive search with filters               | Public         |
| **Admin**        | `/api/admin`                 | Admin dashboard, provider management, reports   | Admin          |
| **Provider**     | `/api/providers`             | Provider‑specific data and analytics             | Provider       |
| **User**         | `/api/user`                  | Profile, favorites, token history                | User           |
| **Export**       | `/api/export`                | Export queue reports (PDF/Excel)                 | Admin/Provider |
| **Notifications**| `/api/notifications/preferences` | Manage per‑queue notification settings        | User           |
| **PasswordResetToken** | `/api/password-reset-token` | Admin‑initiated password reset (token)           | Admin          |

For a complete list, refer to the Swagger UI.

---

## Testing

### Backend Tests

Run unit and integration tests with Maven:

```bash
cd backend
./mvnw test
```

The tests cover services, controllers, and some integration flows. A local MongoDB instance is automatically started/stopped via embedded MongoDB for tests.

### Frontend Tests

Frontend tests are not yet implemented but can be added using Jest and React Testing Library.

---

## Deployment

### Backend

1. Build an executable JAR:

   ```bash
   cd backend
   ./mvnw clean package -DskipTests
   ```

2. Copy the JAR (`target/backend-0.0.1-SNAPSHOT.jar`) to your server.

3. Run with Java:

   ```bash
   java -jar backend-0.0.1-SNAPSHOT.jar
   ```

   Use a process manager like `systemd` or `supervisor` to keep it running.

### Frontend

1. Build the production bundle:

   ```bash
   cd frontend
   npm run build
   ```

2. Serve the `dist` folder with a web server (e.g., Nginx).

### Docker / Docker Compose

For a full stack deployment, use the provided `docker-compose.yml` on a server with Docker installed. Adjust environment variables in the compose file or in a `.env` file.

---

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'Add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

Make sure to write/update tests for any new functionality and ensure all tests pass.

---

## License

This project is licensed under the **Apache License, Version 2.0**.  
You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
---

## Acknowledgments

- Thanks to all contributors and open‑source libraries that made this project possible.
- Special thanks to the Spring Boot and React communities for excellent documentation and tools.

```
