# StayHyderabad Backend (Spring Boot 3 + Java 21 LTS + PostgreSQL)

Production-ready REST API for **StayHyderabad**, featuring JWT authentication, Flyway migrations, pessimistic write locking for vacancy management, Cloudinary integration, and OpenAPI 3.0 documentation.

---

## 🚀 Live Hosting on Render / Docker
This repository includes a multi-stage `Dockerfile` ready for deployment on [Render](https://render.com), Railway, Fly.io, or any Docker host.

### Environment Variables
Configure these in your Render or hosting dashboard:

| Variable | Description | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Active Spring profile | `prod` |
| `DB_URL` | Neon/Supabase PostgreSQL JDBC URL with SSL | `jdbc:postgresql://ep-xyz.neon.tech/neondb?sslmode=require` |
| `DB_USERNAME` | Database username | `<db_user>` |
| `DB_PASSWORD` | Database password | `<db_password>` |
| `JWT_SECRET` | 256-bit signing key for HMAC-SHA256 | `yourSecretSigningKeyMustBeAtLeast256BitsLong1234567890` |
| `CORS_ALLOWED_ORIGINS` | Deployed frontend Vercel domain | `https://your-frontend.vercel.app,http://localhost:5173` |
| `CLOUDINARY_CLOUD_NAME` | Cloudinary Cloud Name (optional in dev) | `<cloud_name>` |
| `CLOUDINARY_API_KEY` | Cloudinary API Key | `<api_key>` |
| `CLOUDINARY_API_SECRET` | Cloudinary API Secret | `<api_secret>` |

Health Check URL: `/actuator/health`

---

## 🛠️ Local Development

### 1. Run Tests
```bash
mvn clean test
```

### 2. Start Local Server
```bash
mvn spring-boot:run
```
Starts on `http://localhost:8080`.
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- Health Check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
