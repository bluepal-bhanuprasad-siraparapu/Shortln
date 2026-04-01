# ⚙️ P7 Backend: RESTful API Documentation

The P7 backend is a high-performance, secure REST API built using the Spring Boot ecosystem. It handles link logic, analytics processing, user security, and automated background tasks.

---

## 📁 Source Code Organization

- **`com.urlshortener.controller`**: Handles HTTP requests and response mapping.
- **`com.urlshortener.service`**: Contains core business logic (Auth, Links, Analytics, Payments).
- **`com.urlshortener.entity`**: Defines database tables and relationships (JPA).
- **`com.urlshortener.repository`**: Data access layer interfaces (Spring Data JPA).
- **`com.urlshortener.dto`**: Data Transfer Objects for clean API communication.
- **`com.urlshortener.security`**: Security configuration, JWT providers, and filter chains.
- **`com.urlshortener.aspect`**: Aspect-Oriented Programming for automated audit logging.
- **`com.urlshortener.messaging`**: Kafka producers and consumers for system-wide alerts.

---

## 🔒 Security Architecture

### Authentication Flow
1. User provides credentials to `/api/auth/login`.
2. Server validates and returns a signed **JWT (JSON Web Token)**.
3. Client includes JWT in the `Authorization: Bearer <token>` header for future requests.

### Passwordless & Secure Recovery
- **Magic Links**: Integrated UUID-based tokens for one-click authentication from email.
- **OTP Recovery**: Secure, time-limited 6-digit codes for account restoration.

### Role-Based Access Control (RBAC)
- **`ROLE_USER`**: Access to personal links (up to 5 for FREE), analytics (PRO only), and subscription upgrading.
- **`ROLE_ADMIN`**: Access to platform-wide links, complete user profile management (activation, deactivation, deletion), and global data export.

---

## 🛠 Core Services Explained

### 1. Link Redirection & Caching
When a short link is accessed (e.g., `my-shortener.com/xyz`):
- The system first checks **Redis Cache** for the target URL.
- If not found, it queries **PostgreSQL**.
- It asynchronously records click data (IP, User-Agent) via the `AnalyticsService`.
- **Cache Invalidation**: On link deactivation, update, or deletion, associated Redis cache entries are aggressively cleared to instantly prevent redirected access.

### 2. Analytics Processing
- **GeoIP Resolution**: Converts IPv4/IPv6 addresses into City and Country using the MaxMind library.
- **User-Agent Parsing**: Extracts Browser Name, Version, and Device Type (Mobile/Tablet/Desktop).
- **Plan Enforcement**: Access to granular device and geo-location analytics is securely locked behind the PRO subscription tier.

### 3. Subscription Engine & User Management
- Tracks `subscriptionExpiry` for every user.
- Enforces link creation limitations (FREE users capped at 5 links).
- Every login triggers a check; if the current time > `subscriptionExpiry`, the user is downgraded from `PRO` to `FREE` automatically.
- Unified user management endpoints enabling admins to query active status, plan distributions, and manage lifecycles.

### 4. Automated Audit Logging (AOP)
The platform utilizes **Spring AOP (@Aspect)** to automatically intercept method calls in the service layer:
- **Success Tracking**: Logs method names, parameters, and categories for all successful operations.
- **Error Capture**: Records detailed exception messages and method arguments for failed backend attempts.
- **Data Persistence**: These events are stored in the `audit_logs` table and exposed via the Admin Dashboard.

### 5. Messaging & Notifications (Kafka)
- **Producer**: Sends messages when critical events occur (e.g., system alerts, link expiry).
- **Consumer**: Listens for these events and routes them to the `NotificationService` for database persistence and real-time frontend delivery.

---

## 📦 Deployment Guide

### JAR Packaging
```bash
./mvnw clean package -DskipTests
```
The executable JAR will be generated in the `target/` directory.

### Environment Overrides
You can override any `application.yml` property using environment variables:
- `SPRING_DATASOURCE_URL`
- `JWT_SECRET`
- `MAIL_USERNAME` / `MAIL_PASSWORD`

---

## 🧪 Testing
Run the test suite using:
```bash
./mvnw test
```
The suite includes unit tests for core services and integration tests for API endpoints.
