
# QueueLess – Smart Queue Management System

QueueLess is a comprehensive, multi‑tenant queue management platform designed for businesses of all types – hospitals, banks, shops, restaurants, and more. It allows administrators to create and manage multiple locations (places), providers to handle queues in real time, and users to join queues, track their position, receive notifications, and provide feedback. The system features live updates via WebSocket, email/push notifications, Razorpay payment integration, role‑based access control, and detailed analytics.

---

## Table of Contents

1. [Features](#features)
2. [Tech Stack](#tech-stack)
3. [Architecture Overview](#architecture-overview)
4. [Screenshots & Demo](#screenshots--demo)
5. [Getting Started](#getting-started)
   - [Prerequisites](#prerequisites)
   - [Installation](#installation)
   - [Environment Variables](#environment-variables)
   - [Running Locally](#running-locally)
   - [Docker Setup](#docker-setup)
6. [API Documentation](#api-documentation)
7. [WebSocket Real‑Time Updates](#websocket-realtime-updates)
8. [Testing](#testing)
9. [Deployment](#deployment)
10. [Troubleshooting](#troubleshooting)
11. [Future Roadmap](#future-roadmap)
12. [Contributing](#contributing)
13. [License](#license)
14. [Code of Conduct](#code-of-conduct)

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
- **Per‑Queue Notification Preferences**: Customise when and how you are notified for each queue.

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
- **Geographic Heat Map**: Visualise queue load across all places.

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

## Screenshots & Demo

*(All screenshots are from the running application. Click on each collapsible section to view.)*

### 🏠 Public Pages

<details>
<summary>Home Page (Click to expand)</summary>

**Hero Section**  
![home-page-1.png](screenshots/home-page-1.png)  
*Landing page with call‑to‑action, statistics, and live queue status.*

**Features & Testimonials**  
![home-page-2.png](screenshots/home-page-2.png)  
![home-page-3.png](screenshots/home-page-3.png)  
![home-page-4.png](screenshots/home-page-4.png)  
![home-page-5.png](screenshots/home-page-5.png)  
*Overview of key features and user testimonials.*

</details>

<details>
<summary>Places List & Place Details</summary>

**Places List**  
![place-list-2.png](screenshots/place-list-2.png)  
*All active places displayed with images, ratings, and addresses.*

**Places List (Dark Mode)**  
![place-list-1.png](screenshots/place-list-1.png)

**Place Detail**  
![place-detail-1.png](screenshots/place-detail-1.png)  
*Main view with services, business hours, and location map.*

**Place Detail – Feedback & Best Time**  
![place-detail-2.png](screenshots/place-detail-2.png)  
*User reviews, average ratings, and best‑time recommendations.*

</details>

<details>
<summary>Advanced Search</summary>

**Search Filters**  
![advanced-search-1.png](screenshots/advanced-search-1.png)  
*Filter by location, rating, wait time, and features.*

**Search Results – Places**  
![advanced-search-2.png](screenshots/advanced-search-2.png)

**Search Results – Services**  
![advanced-search-3.png](screenshots/advanced-search-3.png)

**Search Results – Queues**  
![advanced-search-4.png](screenshots/advanced-search-4.png)  
*Paginated results with place details and queue status.*

</details>

<details>
<summary>Authentication & Registration</summary>

**Login Page**  
![login-1.png](screenshots/login-1.png)  
*All roles login with email and password.*

**Register Page – User**  
![register-1.png](screenshots/register-1.png)  
![reegister-2.png](screenshots/reegister-2.png)  
*User registration – no payment required.*

**Register – Mobile View**  
![register-3.png](screenshots/register-3.png)

**Email Verification**  
*All roles receive a verification email after registration.*  
*Example email (responsive):*  
![verify-otp-2.png](screenshots/verify-otp-2.png)  
*Verification page:*  
![verify-otp-1.png](screenshots/verify-otp-1.png)

**Forgot Password**  
![forgot-password.png](screenshots/forgot-password.png)  
*Enter email, receive OTP, then reset password.*

</details>

<details>
<summary>Admin Token Purchase & Registration</summary>

**Pricing Page (Admin)**  
![admin-payement-1.png](screenshots/admin-payement-1.png)  
![admin-payement-2.png](screenshots/admin-payement-2.png)  
![admin-payement-3.png](screenshots/admin-payement-3.png)  
*Razorpay checkout for purchasing admin tokens.*

**Admin Token Generated**  
![admin-payement-4.png](screenshots/admin-payement-4.png)  
*Copy the token and use it during registration.*

**Admin Registration**  
![register-admin-1.png](screenshots/register-admin-1.png)  
![register-admin-2.png](screenshots/register-admin-2.png)  
*Choose “ADMIN” role and paste the purchased token. Token works once and expires.*

</details>

---

### 👤 User Dashboard (Logged‑in)

<details>
<summary>Dashboard Overview</summary>

**My Queues**  
![user-dashboard-1.png](screenshots/user-dashboard-1.png)

**Favourite Places**  
![user-dashboard-2.png](screenshots/user-dashboard-2.png)

**Token Usage Chart**  
![user-dashboard-3.png](screenshots/user-dashboard-3.png)

**Places Section**  
![user-dashboard-4.png](screenshots/user-dashboard-4.png)

**Dark Mode**  
![user-dashboard-5.png](screenshots/user-dashboard-5.png)

**Mobile View**  
![user-dashboard-6.png](screenshots/user-dashboard-6.png)

*Personal dashboard showing active queues, favourite places, and token history.*

</details>

<details>
<summary>Joining a Queue & Token Flow</summary>

**Regular Token – Join Form**  
![join-queue-1.png](screenshots/join-queue-1.png)

**Get Token with Details**  
![join-queue-2.png](screenshots/join-queue-2.png)

**Group Token – Form**  
![join-queue-3.png](screenshots/join-queue-3.png)

**Emergency Token – Form**  
![join-queue-4.png](screenshots/join-queue-4.png)

**Queue Information**  
![join-queue-5.png](screenshots/join-queue-5.png)

**Mobile View**  
![join-queue-6.png](screenshots/join-queue-6.png)

**Live Position**  
![join-queue-7.png](screenshots/join-queue-7.png)

**Feedback Prompt** (after token completion)  
![join-queue-8.png](screenshots/join-queue-8.png)

**Feedback Form**  
![join-queue-9.png](screenshots/join-queue-9.png)

**Filled Form**  
![join-queue-10.png](screenshots/join-queue-10.png)

**Completion Message**  
![join-queue-11.png](screenshots/join-queue-11.png)

*Users can choose regular, group, or emergency tokens, provide details, and give feedback after completion.*

</details>

<details>
<summary>Queue Position & Notifications</summary>

**Queue Position**  
![queue-position-1.png](screenshots/queue-position-1.png)  
![queue-position-2.png](screenshots/queue-position-2.png)

**Push Notifications**  
*Your turn is coming up:*  
![notification-preferences-4.png](screenshots/notification-preferences-4.png)

*Queue is short:*  
![notification-preferences-7.png](screenshots/notification-preferences-7.png)

**Email Notification**  
*Reminder when your turn is approaching:*  
![notification-preferences-8.png](screenshots/notification-preferences-8.png)

</details>

<details>
<summary>Favourite Places</summary>

**Favourites Page**  
![favourite-place-1.png](screenshots/favourite-place-1.png)

**Dark Mode**  
![favourite-place-2.png](screenshots/favourite-place-2.png)

**Mobile View**  
![favourite-place-3.png](screenshots/favourite-place-3.png)

</details>

<details>
<summary>Notification Preferences</summary>

**Empty State**  
![notification-preferences-1.png](screenshots/notification-preferences-1.png)

**Add Preference – Select Queue**  
![notification-preferences-2.png](screenshots/notification-preferences-2.png)

**Add Preference – Set Options**  
![notification-preferences-3.png](screenshots/notification-preferences-3.png)

**Preferences List**  
![notification-preferences-5.png](screenshots/notification-preferences-5.png)

**Mobile View**  
![notification-preferences-6.png](screenshots/notification-preferences-6.png)

*Per‑queue settings for reminder time, status changes, and best‑time alerts.*

</details>

<details>
<summary>User Profile</summary>

**Profile Page**  
![user-profile-1.png](screenshots/user-profile-1.png)

**Avatar Selection**  
![user-profile-2.png](screenshots/user-profile-2.png)

**Personal Information**  
![user-profile-3.png](screenshots/user-profile-3.png)

**Change Password**  
![user-profile-4.png](screenshots/user-profile-4.png)

**Preferences & Account**  
![user-profile-5.png](screenshots/user-profile-5.png)

**Dark Mode**  
![user-profile-6.png](screenshots/user-profile-6.png)

**Mobile View – Light**  
![user-profile-7.png](screenshots/user-profile-7.png)

**Mobile View – Dark**  
![user-profile-8.png](screenshots/user-profile-8.png)  
![user-profile-9.png](screenshots/user-profile-9.png)

*Update name, phone, password, upload profile image, and manage preferences.*

</details>

<details>
<summary>Token History</summary>

![Token History](screenshots/token-history.png)  
*Past tokens with ratings and service details.*

</details>

---

### 🛠️ Provider Dashboard

<details>
<summary>Provider Queue List</summary>

**Queues List**  
![provider-queues-1.png](screenshots/provider-queues-1.png)  
![provider-queues-2.png](screenshots/provider-queues-2.png)

**Dark Mode**  
![provider-queues-3.png](screenshots/provider-queues-3.png)

**Mobile View**  
![provider-queues-4.png](screenshots/provider-queues-4.png)  
![provider-queues-5.png](screenshots/provider-queues-5.png)

*List of all queues with status and quick actions.*

</details>

<details>
<summary>Queue Management</summary>

**Dashboard**  
![queue-management-1.png](screenshots/queue-management-1.png)

**Complete Token**  
![queue-management-2.png](screenshots/queue-management-2.png)

**Completed Tokens List**  
![queue-management-3.png](screenshots/queue-management-3.png)

**QR Code**  
![queue-management-4.png](screenshots/queue-management-4.png)

**Reset Queue with Export**  
![queue-management-6.png](screenshots/queue-management-6.png)

**Export History**  
![queue-management-7.png](screenshots/queue-management-7.png)

**Dark Mode**  
![queue-management-8.png](screenshots/queue-management-8.png)

**Mobile View**  
![queue-management-9.png](screenshots/queue-management-9.png)

*Drag‑and‑drop reorder, serve next, complete/cancel tokens, view user details, and reset.*

</details>

<details>
<summary>Emergency Approval</summary>

![Emergency Approval](screenshots/emergency-approval.png)  
*Approve or reject emergency token requests with optional reason.*

</details>

<details>
<summary>Analytics & Exports</summary>

**Token Volume (30 days)**  
![provider-analytics-1.png](screenshots/provider-analytics-1.png)

**Busiest Hours**  
![provider-analytics-2.png](screenshots/provider-analytics-2.png)

**Average Wait Time Trend**  
![provider-analytics-3.png](screenshots/provider-analytics-3.png)

**Export Reports**  
![queue-management-5.png](screenshots/queue-management-5.png)

**Dark Mode Charts**  
![provider-analytics-4.png](screenshots/provider-analytics-4.png)

**Mobile View**  
![provider-analytics-5.png](screenshots/provider-analytics-5.png)

*Charts for token volume, busiest hours, average wait time, and PDF/Excel exports.*

</details>

---

### 👑 Admin Dashboard

<details>
<summary>Admin Overview</summary>

**Dashboard**  
![admin-dashboard-1.png](screenshots/admin-dashboard-1.png)

**Dark Mode**  
![admin-dashboard-2.png](screenshots/admin-dashboard-2.png)

**Mobile View**  
![admin-dashboard-3.png](screenshots/admin-dashboard-3.png)

*Statistics cards, quick actions, and tabs for places, queues, payments, providers, analytics.*

</details>

<details>
<summary>Place Management</summary>

**Places List**  
![admin-places-1.png](screenshots/admin-places-1.png)  
![admin-places-2.png](screenshots/admin-places-2.png)  
![admin-places-3.png](screenshots/admin-places-3.png)  
![admin-places-4.png](screenshots/admin-places-4.png)

**Create/Edit Place (Dark Mode)**  
![admin-places-5.png](screenshots/admin-places-5.png)

**Mobile View**  
![admin-places-6.png](screenshots/admin-places-6.png)

*Create and edit places with images, location, business hours, and contact info.*

</details>

<details>
<summary>Service Management</summary>

**Services List**  
![admin-services-1.png](screenshots/admin-services-1.png)

**Create Service**  
![admin-services-2.png](screenshots/admin-services-2.png)

**Services Table**  
![admin-services-3.png](screenshots/admin-services-3.png)

**Dark Mode – Create Service**  
![admin-services-4.png](screenshots/admin-services-4.png)

**Mobile View**  
![admin-services-5.png](screenshots/admin-services-5.png)

*Add services under a place, set average service time, and enable group/emergency support.*

</details>

<details>
<summary>Provider Management</summary>

**Providers Table**  
![admin-providers-1.png](screenshots/admin-providers-1.png)

**Dark Mode**  
![admin-providers-2.png](screenshots/admin-providers-2.png)

**Provider Details**  
![admin-providers-3.png](screenshots/admin-providers-3.png)

**Edit Provider**  
![admin-providers-4.png](screenshots/admin-providers-4.png)

**Dark Mode – Details**  
![admin-providers-5.png](screenshots/admin-providers-5.png)

**Mobile View**  
![admin-providers-6.png](screenshots/admin-providers-6.png)

**Password Reset Email**  
![admin-providers-7.png](screenshots/admin-providers-7.png)

*List all providers, view performance, edit details, assign places, and reset passwords.*

</details>

<details>
<summary>Payment History</summary>

**Payment History**  
![payment-history-1.png](screenshots/payment-history-1.png)

**Dark Mode**  
![payment-history-2.png](screenshots/payment-history-2.png)

**Mobile View**  
![payment-history-3.png](screenshots/payment-history-3.png)

*Record of all token purchases (admin and provider) with transaction details.*

</details>

<details>
<summary>Admin Queues</summary>

**Queues Table**  
![admin-queues-1.png](screenshots/admin-queues-1.png)

**Dark Mode**  
![admin-queues-2.png](screenshots/admin-queues-2.png)

**Mobile View**  
![admin-queues-3.png](screenshots/admin-queues-3.png)

*Admins can see all queues managed by their providers and toggle active status.*

</details>

<details>
<summary>Analytics Charts</summary>

**Token Volume**  
![admin-analytics-1.png](screenshots/admin-analytics-1.png)

**Busiest Hours**  
![admin-analytics-2.png](screenshots/admin-analytics-2.png)

**Dark Mode**  
![admin-analytics-3.png](screenshots/admin-analytics-3.png)

**Mobile View**  
![admin-analytics-4.png](screenshots/admin-analytics-4.png)

*Token volume over time and busiest hours across all places.*

</details>

<details>
<summary>Heat Map (Queue Load)</summary>

**Heat Map**  
![heat-map-1.png](screenshots/heat-map-1.png)

**Dark Mode**  
![heat-map-2.png](screenshots/heat-map-2.png)

**Mobile View**  
![heat-map-3.png](screenshots/heat-map-3.png)

*Geographic visualisation of queue load per place.*

</details>

<details>
<summary>Alert Configuration</summary>

**Alert Config Form**  
![admin-alert-1.png](screenshots/admin-alert-1.png)

**Dark Mode**  
![admin-alert-2.png](screenshots/admin-alert-2.png)

**Mobile View**  
![admin-alert-3.png](screenshots/admin-alert-3.png)

**Alert Email**  
![admin-alert-4.png](screenshots/admin-alert-4.png)  
![admin-alert-5.png](screenshots/admin-alert-5.png)

*Set wait time thresholds; receive email alerts when exceeded.*

</details>

---

### 📄 Additional Public Pages

<details>
<summary>About Us</summary>

![about-page-1.png](screenshots/about-page-1.png)  
![about-page-2.png](screenshots/about-page-2.png)  
![about-page-3.png](screenshots/about-page-3.png)  
![about-page-4.png](screenshots/about-page-4.png)

**Mobile View**  
![about-page-5.png](screenshots/about-page-5.png)

*Our mission, problem statement, solution, differentiators, and technologies used.*

</details>

<details>
<summary>How to Use</summary>

**User Guide**  
![how-to-use-page-1.png](screenshots/how-to-use-page-1.png)

**Provider Guide**  
![how-to-use-page-2.png](screenshots/how-to-use-page-2.png)

**Admin Guide**  
![how-to-use-page-3.png](screenshots/how-to-use-page-3.png)

**Pro Tips**  
![how-to-use-page-4.png](screenshots/how-to-use-page-4.png)

**Mobile View**  
![how-to-use-page-5.png](screenshots/how-to-use-page-5.png)

*Step‑by‑step instructions for each role, plus tips for best experience.*

</details>

<details>
<summary>Documentation</summary>

![documentation-page-1.png](screenshots/documentation-page-1.png)

**Mobile View**  
![documentation-page-2.png](screenshots/documentation-page-2.png)

*Major API documentation – User Friendly Ui for apis and getting started.*

</details>

<details>
<summary>Privacy Policy & Terms of Service</summary>

**Privacy Policy**  
![privacy-policy-page-1.png](screenshots/privacy-policy-page-1.png)

**Mobile View**  
![privicy-policy-page-2.png](screenshots/privicy-policy-page-2.png)

*Legal information page containing both privacy policy and terms of service.*

</details>

---

### 📚 Swagger – API Documentation

![swagger-api-1.png](screenshots/swagger-api-1.png)

**Schemas**  
![swagger-api-2.png](screenshots/swagger-api-2.png)

*Interactive OpenAPI documentation for all controllers and models.*

---

### 📊 Grafana – Monitoring Dashboards

**Queue Metrics**  
![grafana-1.png](screenshots/grafana-1.png)

**Spring Boot Metrics**  
![grafana-2.png](screenshots/grafana-2.png)

**Cache & Performance**  
![grafana-3.png](screenshots/grafana-3.png)

**Logs Drilldown**  
![grafana-4.png](screenshots/grafana-4.png)

**All Metrics**  
![grafana-5.png](screenshots/grafana-5.png)

*Visualise metrics and logs from Prometheus and Loki.*

---

### 🎬 Real‑time Updates (Demo)

**Live Queue Updates**  

![Live Demo](screenshots/queue-updates.gif) 
 
*Watch as tokens are added, served, and completed – the queue updates instantly on all connected clients.*
---

*All screenshots are from the running application.*

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
   git clone https://github.com/harman-04/QueueLess.git
   cd QueueLess
   ```

2. **Backend setup**

   ```bash
   cd backend/backend
   ./mvnw clean install
   ```

3. **Frontend setup**

   ```bash
   cd ..
   cd ../frontend/queue-less-frontend
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

#### Docker Compose Root `.env`

When running the full stack with Docker Compose, place a **third `.env` file** in the **project root** (the same directory as `docker-compose.yml`). This file supplies environment variables to the containers. It typically contains a superset of the variables needed by both backend and frontend, plus any additional configuration for the containers themselves.

Example `docker-compose.env` (or simply `.env` at the root):

```env
# Backend
MONGODB_URI=mongodb://mongodb:27017/queueless
JWT_SECRET=your_jwt_secret_key_here
JWT_EXPIRATION=86400000
RAZORPAY_KEY=rzp_test_xxxxxxxxxx
RAZORPAY_SECRET=your_razorpay_secret
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password
SSL_KEY_STORE_PASSWORD=changeit
APP_FRONTEND_URL=https://localhost:5173

# Frontend
VITE_API_BASE_URL=https://backend:8443/api
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
cd backend/backend
./mvnw spring-boot:run

The backend will start on `https://localhost:8443` (SSL enabled). You can access the API at `https://localhost:8443/api`.

#### Frontend

```bash
cd frontend/queue-less-frontend
npm run dev
```

The frontend will be available at `https://localhost:5173`.

### SSL Certificate (Development)

The application uses HTTPS (port 8443 for backend, 443 for frontend in Docker) with self‑signed certificates. To avoid browser warnings, you can generate trusted certificates using **mkcert**.

#### Install mkcert

- **macOS**: `brew install mkcert`
- **Windows**: `choco install mkcert` or download from [github.com/FiloSottile/mkcert](https://github.com/FiloSottile/mkcert)
- **Linux**: `sudo apt install libnss3-tools` then `curl -JLO "https://dl.filippo.io/mkcert/latest?for=linux/amd64" && chmod +x mkcert-v*-linux-amd64 && sudo mv mkcert-v*-linux-amd64 /usr/local/bin/mkcert`

#### Generate and Install Certificates

1. **Create a local CA** (one‑time):

   ```bash
   mkcert -install
   ```



2. **Generate certificates for localhost**:

   ```bash
   cd backend/src/main/resources
   mkcert localhost 127.0.0.1 ::1
   ```

   This creates `localhost+2.pem` and `localhost+2-key.pem`. Rename them:

   ```bash
   mv localhost+2.pem localhost.pem
   mv localhost+2-key.pem localhost-key.pem
   ```

3. **Convert to PKCS12 format** (required by Spring Boot):

   ```bash
   openssl pkcs12 -export -in localhost.pem -inkey localhost-key.pem -out localhost.p12 -name localhost -password pass:changeit
   ```

   The password `changeit` matches the default in `application.properties`.

4. **Place the `.p12` file** inside `src/main/resources/`.

#### Backend Configuration

The `application.properties` already contains:

```properties
server.ssl.key-store=classpath:localhost.p12
server.ssl.key-store-password=${SSL_KEY_STORE_PASSWORD:changeit}
server.ssl.key-store-type=PKCS12
```

Make sure the password matches the one used when generating the `.p12` file.

#### Frontend Configuration (Vite)

For the Vite dev server to trust the certificate, add this to `vite.config.js`:

```js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    https: {
      key: 'path/to/localhost-key.pem',
      cert: 'path/to/localhost.pem',
    },
    port: 5173,
    proxy: {
      '/api': {
        target: 'https://localhost:8443',
        changeOrigin: true,
        secure: false,
      },
    },
  },
});
```

Replace the paths with the actual location of your `.pem` files. The proxy ensures API requests are forwarded to the backend without CORS issues.

#### Docker

In the provided `docker-compose.yml`, the backend uses the `localhost.p12` file mounted from the host. Ensure you have generated the `.p12` file and placed it in the expected location (`backend/src/main/resources/localhost.p12`).

> **Note**: For production, use certificates from a trusted CA like Let’s Encrypt.

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

## WebSocket Real‑Time Updates

QueueLess uses WebSocket (STOMP over SockJS) to provide live queue updates. The WebSocket endpoint is `/ws`.

### Connecting

```javascript
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('https://localhost:8443/ws'),
  connectHeaders: { Authorization: `Bearer ${token}` },
  onConnect: () => console.log('Connected'),
});

client.activate();
```
### Subscribing to a Queue

Subscribe to `/topic/queues/{queueId}` to receive updates whenever the queue changes (token added, served, completed, cancelled).

```javascript
client.subscribe('/topic/queues/' + queueId, (message) => {
  const queue = JSON.parse(message.body);
  // update UI
});
```

### User‑Specific Notifications

- `/user/queue/emergency-approved` – emergency token approval/rejection.
- `/user/queue/token-cancelled` – token cancellation notifications.
- `/user/queue/provider-updates` – (for providers) updates when they serve a token.

### Sending Commands

- `/app/queue/serve-next` – serve next token (provider only).
- `/app/queue/add-token` – add a regular token (user).
- `/app/queue/status` – toggle queue active status (provider).

All commands require authentication and appropriate role.


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

Frontend tests are not yet implemented, but the architecture is ready for them using Jest and React Testing Library.

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
## Troubleshooting

### Backend won't start – MongoDB connection refused
- Ensure MongoDB is running locally or the `MONGODB_URI` is correct.
- If using embedded MongoDB for tests, it starts automatically.

### Redis connection errors
- Verify Redis is installed and running (`redis-server`).
- If Redis is not needed, you can disable caching by setting `spring.cache.type=none` in `application.properties`.

### Emails not sending
- For Gmail, enable 2FA and generate an [App Password](https://support.google.com/accounts/answer/185833).
- Check that `MAIL_USERNAME` and `MAIL_PASSWORD` are correct.
- If using a different SMTP provider, adjust `spring.mail.host` and port.

### Push notifications not working
- Ensure Firebase project is configured correctly and `VAPID_KEY` is set.
- In the browser, check that notifications are allowed for the site.
- Verify that FCM tokens are being stored in the database (user's `fcmTokens` array).

### File uploads fail with 403
- Make sure the `uploads/` directory exists and is writable.
- The backend serves static files under `/uploads/**`. If you get 404, check that `WebConfig` is properly configured.

### WebSocket connection fails
- Check that the backend is running and the WebSocket endpoint `/ws` is accessible.
- Ensure the JWT token is valid and passed in the `Authorization` header.
- If using self‑signed certificates, the browser may block the connection. In development, you can temporarily disable SSL verification or use a valid certificate.

### Rate limiting too strict
- Adjust `rate.limit.*` properties in `application.properties`. For example, increase `capacity` and `refill` for development.

### Common error: "Token expired" after login
- The JWT expiration is set to 24 hours by default. If your system clock is skewed, tokens may appear expired. Synchronize time via NTP.

---
## Future Roadmap

While QueueLess already provides a robust set of features, the following enhancements are planned for upcoming releases:

- **Multiple Active Tokens per User**: Allow users to join queues in different places simultaneously.
- **Per‑Service Multiple Queues**: Let users choose from multiple providers offering the same service (e.g., different doctors at the same hospital).
- **Emergency Toggle for Providers**: Enable/disable emergency support for an existing queue.
- **Export Cleanup**: Automatic removal of old export files from the cache.
- **Admin Audit Log Viewer**: Interface for administrators to inspect audit logs.
- **Internationalization (i18n)**: Support for multiple languages.
- **Mobile App**: React Native or Flutter version for native push notifications and offline support.
- **Advanced Analytics**: Predictive wait time models using historical data.

---

## Contributing

We welcome contributions! To ensure a smooth process, please follow these guidelines:

1. **Fork the repository** and create your branch from `main`.
2. **Write tests** for any new functionality. We aim for high test coverage.
3. **Ensure all tests pass** by running `mvn test` in the backend and (if applicable) `npm test` in the frontend.
4. **Follow coding conventions**:
    - Backend: Use standard Java naming conventions, include Javadoc for public methods, and format code with your IDE's default settings.
    - Frontend: Use ESLint and Prettier (configuration included).
5. **Commit messages** should be clear and follow [Conventional Commits](https://www.conventionalcommits.org/) (e.g., `feat: add user profile image upload`).
6. **Open a pull request** against the `main` branch. Describe your changes in detail and link any related issues.

### Development Workflow

- Backend: Run `./mvnw spring-boot:run` for hot reload.
- Frontend: Run `npm run dev` for Vite dev server with HMR.

### Reporting Issues

Use the GitHub issue tracker to report bugs or suggest features. Include as much detail as possible: steps to reproduce, expected behavior, screenshots, and environment details.

---

## License

This project is licensed under the **Apache License, Version 2.0**.  
You may obtain a copy of the License at [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
---

## Code of Conduct

We as members, contributors, and leaders pledge to make participation in our community a harassment‑free experience for everyone, regardless of age, body size, visible or invisible disability, ethnicity, sex characteristics, gender identity and expression, level of experience, education, socio‑economic status, nationality, personal appearance, race, caste, color, religion, or sexual identity and orientation.

We pledge to act and interact in ways that contribute to an open, welcoming, diverse, inclusive, and healthy community.

Our full Code of Conduct is available in the [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) file. By participating, you are expected to uphold this code.

---
## Acknowledgments

- Thanks to all contributors and open‑source libraries that made this project possible.
- Special thanks to the Spring Boot and React communities for excellent documentation and tools.
