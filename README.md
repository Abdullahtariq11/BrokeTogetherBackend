# 🏠 BrokeTogether Backend

A robust Spring Boot REST API for the BrokeTogether expense-splitting application. Handle shared expenses, track balances, and settle up with roommates seamlessly.

> 🏆 Built for **RevenueCat Shipaton 2026** — Next Gen Award entry. Pairs with the [BrokeTogether mobile app](https://github.com/Abdullahtariq11/BrokeTogetherClient).

## 🎬 Demo Video

▶️ [Watch the demo](https://www.youtube.com/) *(link goes live before the submission deadline)*

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Railway](https://img.shields.io/badge/Railway-0B0D0E?style=for-the-badge&logo=railway&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-Auth-000000?style=for-the-badge&logo=json-web-tokens&logoColor=white)

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Getting Started](#-getting-started)
- [Docker Setup](#-docker-setup)
- [API Endpoints](#-api-endpoints)
- [Database Schema](#-database-schema)
- [Development Practices](#-development-practices)
- [Deployment](#-deployment)
- [Contributing](#-contributing)

## ✨ Features

- **🔐 JWT Authentication** - Secure, stateless authentication with Google & Apple mobile sign-in
- **🔑 Password Recovery** - Token-based forgot/reset password flow
- **🏡 Household Management** - Create homes, invite members via unique codes
- **💰 Expense Tracking** - Log expenses with equal, selective, or custom splits
- **🔁 Recurring Expenses** - Schedule repeating household bills
- **🛒 Shopping List** - Shared list with price tracking, convertible to expenses (Pro)
- **📊 Balance Calculation** - Real-time net balance computation
- **💡 Settlement Suggestions** - Smart who-pays-whom recommendations
- **📈 Analytics & Export** - Insights dashboard plus PDF/CSV export
- **💳 Billing** - Stripe checkout/portal plus RevenueCat webhooks for mobile entitlements
- **👥 Member Management** - Admin controls for home creators
- **🐳 Dockerized** - Containerized for consistent deployments
- **☁️ Cloud Ready** - Deployed on Railway

## 🛠️ Tech Stack

| Category | Technology |
|----------|------------|
| Language | Java 17 |
| Framework | Spring Boot 3.x |
| Security | Spring Security + JWT |
| Database | PostgreSQL |
| ORM | Spring Data JPA / Hibernate |
| Build Tool | Maven |
| Containerization | Docker |
| Deployment | Railway |
| Documentation | Swagger / OpenAPI |

## 🏗️ Architecture

```
src/main/java/com/broketogether/api/
├── config/
│   ├── SecurityConfig.java      # JWT filter chain configuration
│   └── JwtService.java          # Token generation & validation
├── controller/
│   ├── AuthController.java      # Login, registration, social sign-in, password reset
│   ├── BillingController.java   # Stripe + RevenueCat billing & webhooks
│   ├── ExpenseController.java   # Expense, balance, analytics & export operations
│   ├── HomeController.java      # Household management
│   ├── RecurringExpenseController.java  # Recurring expense schedules
│   ├── ShoppingItemController.java      # Shared shopping list
│   └── UserController.java      # User profile endpoints
├── dto/
│   ├── request/                 # Incoming request DTOs
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── HomeRequest.java
│   │   ├── ExpenseRequest.java
│   │   └── SettlementRequest.java
│   └── response/                # Outgoing response DTOs
│       ├── HomeResponse.java
│       ├── ExpenseResponse.java
│       └── MemberResponse.java
├── model/
│   ├── User.java                # User entity
│   ├── Home.java                # Household entity
│   ├── Expense.java             # Expense entity
│   ├── ExpenseSplit.java        # Split tracking entity
│   ├── RecurringExpense.java    # Recurring expense schedule
│   ├── ShoppingItem.java        # Shopping list item
│   ├── PasswordResetToken.java  # Password reset tokens
│   └── SubscriptionStatus.java  # Billing subscription state
├── repository/
│   ├── UserRepository.java
│   ├── HomeRepository.java
│   ├── ExpenseRepository.java
│   └── ExpenseSplitRepository.java
├── service/
│   ├── AuthService.java         # Authentication logic
│   ├── HomeService.java         # Home business logic
│   ├── ExpenseService.java      # Expense & balance logic
│   ├── RecurringExpenseService.java  # Recurring expense logic
│   ├── ShoppingItemService.java # Shopping list logic
│   ├── AnalyticsService.java    # Insights & analytics
│   ├── ExportService.java       # PDF/CSV export
│   ├── BillingService.java      # Stripe + RevenueCat billing
│   ├── PasswordResetService.java# Password recovery flow
│   └── UserService.java         # User profile logic
└── exception/
    └── GlobalExceptionHandler.java
```

## 🚀 Getting Started

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL 14+
- Docker & Docker Compose (recommended)

### Local Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/Abdullahtariq11/BrokeTogetherBackend.git
   cd BrokeTogetherBackend
   ```

2. **Configure the database**
   
   Create a PostgreSQL database:
   ```sql
   CREATE DATABASE broketogether;
   ```

3. **Set up environment variables**
   
   Create `application.properties` or set environment variables:
   ```properties
   # Database
   spring.datasource.url=jdbc:postgresql://localhost:5432/broketogether
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   
   # JPA
   spring.jpa.hibernate.ddl-auto=update
   spring.jpa.show-sql=true
   
   # JWT
   jwt.secret=your-256-bit-secret-key-here
   jwt.expiration=86400000
   ```

4. **Build and run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

5. **Access the API**
   ```
   http://localhost:8080/api/v1
   ```

---

## 🐳 Docker Setup

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Docker Compose

```yaml
version: '3.8'

services:
  api:
    build: .
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/broketogether
      - SPRING_DATASOURCE_USERNAME=postgres
      - SPRING_DATASOURCE_PASSWORD=postgres
      - JWT_SECRET=your-secret-key
    depends_on:
      - db

  db:
    image: postgres:14-alpine
    environment:
      - POSTGRES_DB=broketogether
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

volumes:
  postgres_data:
```

### Running with Docker

```bash
# Build and run all services
docker-compose up -d

# View logs
docker-compose logs -f api

# Stop all services
docker-compose down

# Rebuild after changes
docker-compose up -d --build
```

### Useful Docker Commands

```bash
# Build image only
docker build -t broketogether-api .

# Run container
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/broketogether \
  -e SPRING_DATASOURCE_USERNAME= \
  -e SPRING_DATASOURCE_PASSWORD= \
  -e JWT_SECRET= \
  broketogether-api

# Check running containers
docker ps

# Enter container shell
docker exec -it <container_id> /bin/sh
```

---

## 🔌 API Endpoints

The API follows RESTful conventions and uses JWT for authorization. All protected endpoints require the `Authorization: Bearer <token>` header.

### 🔐 Authentication — `/api/v1/auth`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/register` | Create a new user account | ❌ |
| `POST` | `/login` | Login and receive JWT token | ❌ |
| `POST` | `/forgot-password` | Send a password-reset email (always 200 to prevent email enumeration) | ❌ |
| `POST` | `/reset-password` | Reset password with a valid reset token | ❌ |
| `POST` | `/google/mobile` | Mobile Google sign-in — verifies the Google access token, returns a BrokeTogether JWT (30-day) | ❌ |
| `POST` | `/apple/mobile` | Mobile Sign in with Apple — verifies the Apple identity token, returns a BrokeTogether JWT | ❌ |

**Register Request:**
```json
{
  "name": "John Doe",
  "username": "john@example.com",
  "password": "securepassword"
}
```

**Login Request:**
```json
{
  "username": "john@example.com",
  "password": "securepassword"
}
```

**Login Response:**
```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "name": "John Doe",
  "username": "john@example.com",
  "type": "Bearer"
}
```

---

### 👤 User — `/api/v1/users`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `GET` | `/me` | Get current user profile | ✅ |
| `PUT` | `/edit` | Update display name | ✅ |
| `POST` | `/password-reset` | Change password (current + new password) | ✅ |
| `DELETE` | `/me` | Permanently delete account, homes owned, and associated expenses | ✅ |

---

### 🏠 Home Management — `/api/v1/homes`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/` | Create a new home | ✅ |
| `GET` | `/my-homes` | List user's homes | ✅ |
| `GET` | `/{homeId}` | Get home by ID | ✅ |
| `PUT` | `/{homeId}` | Rename home | ✅ |
| `POST` | `/join` | Join home via invite code | ✅ |
| `GET` | `/{homeId}/members` | Get all members | ✅ |
| `GET` | `/{homeId}/invite-code` | Get invite code | ✅ |
| `POST` | `/inviteCode/{homeId}` | Regenerate invite code | ✅ |
| `DELETE` | `/{homeId}/members/{userId}` | Remove member (creator only) | ✅ |
| `DELETE` | `/{homeId}/leave` | Leave home | ✅ |

**Create Home Request:**
```json
{
  "name": "Apartment 4B"
}
```

**Home Response:**
```json
{
  "id": 1,
  "name": "Apartment 4B",
  "inviteCode": "ABC12345"
}
```

**Join Home Request:**
```json
{
  "inviteCode": "ABC12345"
}
```

---

### 💸 Expenses — `/api/v1/expenses`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/` | Create expense (equal split) | ✅ |
| `POST` | `/selective` | Create expense (selective split) | ✅ |
| `POST` | `/split` | Create expense with explicit split type | ✅ |
| `POST` | `/settle` | Record a settlement payment | ✅ |
| `GET` | `/home/{homeId}/history` | Get expense history (paginated) | ✅ |
| `GET` | `/home/{homeId}/balances` | Get member balances | ✅ |
| `GET` | `/home/{homeId}/settlements` | Settlement suggestions (who pays whom) | ✅ |
| `GET` | `/home/{homeId}/analytics` | Expense analytics & insights | ✅ |
| `GET` | `/home/{homeId}/export?format=pdf\|csv` | Export expense history as PDF or CSV | ✅ |
| `GET` | `/expense/{expenseId}` | Get expense by ID | ✅ |
| `DELETE` | `/{expenseId}` | Delete expense (payer only) | ✅ |

**Create Expense (Equal Split):**
```json
{
  "amount": 100.00,
  "description": "Weekly groceries",
  "category": "Groceries",
  "homeId": 1
}
```

**Create Expense (Selective Split):**
```json
{
  "amount": 60.00,
  "description": "Dinner",
  "category": "Food",
  "homeId": 1,
  "userId": [2, 3]
}
```

**Settlement Request:**
```json
{
  "homeId": 1,
  "payeeId": 2,
  "amount": 50.00
}
```

**Expense Response:**
```json
{
  "id": 1,
  "amount": 100.00,
  "description": "Weekly groceries",
  "category": "Groceries",
  "splits": {
    "1": { "id": 1, "amount": 50.00 },
    "2": { "id": 2, "amount": 50.00 }
  }
}
```

**Balances Response:**
```json
{
  "1": 50.00,
  "2": -50.00
}
```

> **Balance Interpretation:**
> - **Positive** → User is owed money
> - **Negative** → User owes money
> - **Zero** → All settled

---

### 🔁 Recurring Expenses — `/api/v1/expenses/recurring`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/` | Create a recurring expense | ✅ |
| `GET` | `/home/{homeId}` | List recurring expenses for a home | ✅ |
| `DELETE` | `/{id}` | Deactivate a recurring expense | ✅ |

---

### 🛒 Shopping Items — `/api/v1/shopping-items` (Broketogether Pro)

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/` | Add a shopping item | ✅ |
| `GET` | `/item/{itemId}` | Get item by ID | ✅ |
| `GET` | `/home/{homeId}/items` | List all shopping items for a home | ✅ |
| `PUT` | `/item/{itemId}` | Edit a shopping item | ✅ |
| `PATCH` | `/item/mark/{itemId}` | Mark item purchased / unpurchased | ✅ |
| `DELETE` | `/item/{itemId}` | Delete a shopping item | ✅ |
| `POST` | `/item/{itemId}/convert` | Convert a shopping item into an expense | ✅ |

---

### 💳 Billing — `/api/v1/billing`

| Method | Endpoint | Description | Auth |
|--------|----------|-------------|------|
| `POST` | `/checkout` | Create a Stripe checkout session | ✅ |
| `GET` | `/status` | Get subscription / billing status | ✅ |
| `POST` | `/portal` | Create a Stripe customer portal session | ✅ |
| `POST` | `/webhook` | Stripe webhook receiver (signature-verified) | ❌ |
| `POST` | `/revenuecat/webhook` | RevenueCat webhook receiver — syncs mobile entitlements | ❌ |

---

## 📊 Database Schema

The architecture focuses on data normalization to ensure every cent is accounted for. The `ExpenseSplit` table is the source of truth for all debt calculations.

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    User     │       │    Home     │       │   Expense   │
├─────────────┤       ├─────────────┤       ├─────────────┤
│ id          │──┐    │ id          │──┐    │ id          │
│ name        │  │    │ name        │  │    │ amount      │
│ email       │  │    │ invite_code │  │    │ description │
│ password    │  │    │ creator_id  │──┘    │ category    │
│ created_at  │  │    └─────────────┘       │ home_id     │──┐
└─────────────┘  │           │              │ payer_id    │──┘
                 │           │              └─────────────┘
                 │    ┌──────┴──────┐              │
                 │    │ home_members│              │
                 │    │ (Join Table)│              │
                 │    ├─────────────┤              │
                 └────│ user_id     │              │
                      │ home_id     │              │
                      └─────────────┘              │
                                                   │
                      ┌─────────────┐              │
                      │ExpenseSplit │◄─────────────┘
                      ├─────────────┤
                      │ id          │
                      │ expense_id  │
                      │ user_id     │
                      │ amount      │
                      └─────────────┘
```

### Entity Relationships

| Relationship | Description |
|--------------|-------------|
| User ↔ Home | Many-to-Many (via home_members) |
| Home → User | One-to-One (creator) |
| Expense → Home | Many-to-One |
| Expense → User | Many-to-One (payer) |
| ExpenseSplit → Expense | Many-to-One |
| ExpenseSplit → User | Many-to-One |
| RecurringExpense → Home | Many-to-One |
| ShoppingItem → Home | Many-to-One |
| PasswordResetToken → User | Many-to-One |

---

## 🧪 Development Practices

### Test-First Mindset

Every business logic path is covered with unit tests:

```java
@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @Test
    void shouldCreateExpenseWithEqualSplits() { ... }

    @Test
    void shouldThrowExceptionWhenUserNotMember() { ... }

    @Test
    void shouldCalculateBalancesCorrectly() { ... }
}
```

Run tests:
```bash
mvn test
```

### Security Practices

- **Stateless JWT** - No server-side session storage
- **Password Hashing** - BCrypt encryption
- **Authorization Checks** - Service-level validation

### Code Quality

- **DTO Pattern** - Decouples entities from API responses
- **Service Layer** - Business logic separated from controllers
- **Repository Pattern** - Clean data access abstraction
- **Global Exception Handler** - Consistent error responses

---

## ☁️ Deployment

### Railway Deployment

The application is deployed on [Railway](https://railway.app) with Docker.

**Live API:** `https://broketogether-backend-production.up.railway.app`

#### Setup Steps

1. **Create a Railway account** at [railway.app](https://railway.app)

2. **Create a new project** and connect your GitHub repository

3. **Add PostgreSQL database**
   - Click "New" → "Database" → "PostgreSQL"
   - Railway auto-provisions the database

4. **Configure environment variables**
   
   In your Railway service settings, add:
   ```
   SPRING_DATASOURCE_URL=${{Postgres.DATABASE_URL}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
   JWT_SECRET=your-super-secret-jwt-key-256-bits
   JWT_EXPIRATION=86400000
   ```

5. **Deploy**
   - Railway automatically detects the Dockerfile
   - Builds and deploys on every push to main branch

#### Railway Configuration

Create `railway.json` in your project root:
```json
{
  "$schema": "https://railway.app/railway.schema.json",
  "build": {
    "builder": "DOCKERFILE",
    "dockerfilePath": "Dockerfile"
  },
  "deploy": {
    "startCommand": "java -jar app.jar",
    "healthcheckPath": "/api/v1/health",
    "restartPolicyType": "ON_FAILURE"
  }
}
```

#### Monitoring

- View logs: Railway Dashboard → Service → Logs
- Check metrics: Railway Dashboard → Service → Metrics
- Database access: Railway Dashboard → PostgreSQL → Connect

### Manual Deployment

```bash
# Build JAR
mvn clean package -DskipTests

# Run JAR
java -jar target/broketogether-api-0.0.1-SNAPSHOT.jar
```

---

## 🔧 Environment Variables

| Variable | Description | Required |
|----------|-------------|----------|
| `SPRING_DATASOURCE_URL` | PostgreSQL connection URL | ✅ |
| `SPRING_DATASOURCE_USERNAME` | Database username | ✅ |
| `SPRING_DATASOURCE_PASSWORD` | Database password | ✅ |
| `JWT_SECRET` | Secret key for JWT signing (256-bit) | ✅ |
| `JWT_EXPIRATION` | Token expiration in ms (default: 86400000) | ❌ |
| `SERVER_PORT` | Server port (default: 8080) | ❌ |

---

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

---

## 🔗 Related

- [BrokeTogether Client](https://github.com/Abdullahtariq11/BrokeTogetherClient) - React Native Mobile App

## 👤 Author

**Abdullah Tariq**
- GitHub: [@Abdullah Tariq](https://github.com/Abdullahtariq11)
- LinkedIn: [Abdullah Tariq](https://www.linkedin.com/in/abdullah-tariq-499629171/)

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ☕ and Spring Boot | Deployed on Railway 🚂
</p>
