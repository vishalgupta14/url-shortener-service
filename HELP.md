# URL Shortener Service

A scalable, reactive URL shortening service built with Spring Boot WebFlux, MongoDB, Redis, and Docker Compose.

---

## 🚀 Features

- Shorten URLs with optional custom aliases and expiry
- Redirect short links to original URLs
- Click analytics and history (daily clicks)
- Bulk URL shortening via JSON or CSV upload
- Admin APIs to list, delete, and manage links
- Caching with Redis for fast redirects
- Auto-expiry support using Mongo TTL
- Fully containerized with Docker Compose

---

## 🧱 Tech Stack

- **Spring Boot WebFlux**
- **MongoDB** (TTL + Document Store)
- **Redis** (Reactive Caching)
- **Lombok**, **Reactor**, **Commons CSV**
- **Docker** + **Docker Compose**

---

## 🔧 Build & Run Locally

### Prerequisites:
- Java 17+
- Maven 3.6+
- Docker & Docker Compose

### Build Project
```bash
./mvnw clean package -DskipTests
```

### Run with Docker Compose
```bash
docker-compose up --build
```

Spring Boot App → [http://localhost:8111](http://localhost:8111)  
MongoDB → `localhost:27017`  
Redis → `localhost:6379`

---

## 🔗 API Highlights

### ✂️ Shorten URL
```http
POST /shorten
Content-Type: application/json
{
  "longUrl": "https://example.com",
  "customAlias": "myalias"
}
```

### 🔁 Redirect
```http
GET /{shortKey}
```

### 📦 Bulk Shorten (JSON)
```http
POST /bulk-shorten
Content-Type: application/json
[
  { "longUrl": "https://a.com" },
  { "longUrl": "https://b.com", "customAlias": "b123" }
]
```

### 📄 Bulk Shorten (CSV)
```bash
curl -F "file=@urls.csv" http://localhost:8111/bulk-shorten/csv
```

### 📈 Analytics
```http
GET /admin/analytics/top-clicked?page=0&size=10
GET /admin/analytics/summary
GET /admin/analytics/{shortKey}/clicks-by-day
```

### 🧹 Admin Cleanup
```http
DELETE /admin/urls/{shortKey}
DELETE /admin/expired
```

---

## 🛠️ Configuration

Example `application-prod.properties`:
```properties
spring.data.mongodb.uri=${SPRING_DATA_MONGODB_URI}
spring.redis.host=${SPRING_REDIS_HOST}
server.port=${SERVER_PORT:8111}
```

These values are injected via Docker Compose.

---

## 📁 Folder Structure

```
src/main/java/com/example/urlshortener
├── controller         # REST Controllers
├── service            # Business logic
├── repository         # Mongo repositories
├── model              # MongoDB entities
├── dto                # Request/response payloads
├── validate           # Input validation utils
├── exception          # Custom exceptions
└── config             # Redis/Mongo configs
```

---

## ✍️ Author

Vishal Gupta  
Built as a modular, production-ready clone of services like Bitly.


