## 🔌 API Endpoints

The API follows RESTful conventions and uses JWT for authorization. All protected endpoints require the `Authorization: Bearer <token>` header.

### 🔐 Authentication

| Method | Endpoint | Description | Auth Required |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Create a new user account | No |
| `POST` | `/api/v1/auth/login` | Login and receive a JWT token | No |

### 🏠 Home Management

| Method | Endpoint | Description | Auth Required |
| --- | --- | --- | --- |
| `POST` | `/api/v1/homes` | Create a new home (becomes Creator) | Yes |
| `GET` | `/api/v1/homes` | List all homes user belongs to | Yes |
| `POST` | `/api/v1/homes/join` | Join a home via Invite Code | Yes |
| `GET` | `/api/v1/homes/{id}/members` | View all members in a specific home | Yes |
| `DELETE` | `/api/v1/homes/{id}/members/{uId}` | Remove a member (Creator only) | Yes |

### 💸 Expense & Balances

| Method | Endpoint | Description | Auth Required |
| --- | --- | --- | --- |
| `POST` | `/api/v1/expenses` | Log an expense & auto-split among members | Yes |
| `GET` | `/api/v1/expenses/home/{id}` | View expense history for a specific home | Yes |
| `GET` | `/api/v1/balances/{homeId}` | **The Core:** Get net balances for all members | Yes |

---

## 📊 Database Schema

Our architecture focuses on data normalization to ensure that every cent is accounted for. The `ExpenseSplit` table is the source of truth for all debt calculations.

---

## 🛠️ Development Practices

### **The "Test-First" Mindset**

* **Service Layer Testing:** Every business logic path (Success, Unauthorized, Not Found) is covered.
* **Stateless Security:** JWT ensures that the backend remains scalable and session-free.
* **DTO Pattern:** Decoupling Database Entities from API Responses to prevent data leakage and recursion.
