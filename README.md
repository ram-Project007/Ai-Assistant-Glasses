# AI Assistive Glasses — Backend (Phase 1)

Java 21 + Spring Boot 3.3 + MySQL 8 backend providing auth, user profile, and
device management. No AI/CV/OCR/speech/real-time glasses logic is implemented
yet — that's Phase 2, deliberately out of scope here.

---

## 1. Project structure

```
ai-assistive-glasses-backend/
├── pom.xml
├── README.md
├── .gitignore
└── src
    ├── main
    │   ├── java/com/aiassistiveglasses/
    │   │   ├── AiAssistiveGlassesApplication.java
    │   │   ├── config/
    │   │   │   ├── SecurityConfig.java        (SecurityFilterChain, CORS, BCrypt, DaoAuthenticationProvider)
    │   │   │   ├── JwtConfig.java              (binds jwt.* properties)
    │   │   │   └── OpenApiConfig.java          (Swagger bearer-auth scheme)
    │   │   ├── controller/
    │   │   │   ├── AuthController.java
    │   │   │   ├── UserController.java
    │   │   │   └── DeviceController.java
    │   │   ├── dto/
    │   │   │   ├── request/  (RegisterRequest, LoginRequest, DeviceRequest,
    │   │   │   │              UpdateProfileRequest, ChangePasswordRequest)
    │   │   │   └── response/ (ApiResponse<T>, AuthResponse, UserResponse, DeviceResponse)
    │   │   ├── entity/
    │   │   │   ├── User.java        (implements UserDetails directly)
    │   │   │   ├── Device.java
    │   │   │   ├── Role.java        (USER, ADMIN)
    │   │   │   └── DeviceStatus.java (ACTIVE, INACTIVE, CONNECTED, DISCONNECTED)
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   └── DeviceRepository.java
    │   │   ├── service/
    │   │   │   ├── AuthService.java
    │   │   │   ├── UserService.java
    │   │   │   └── DeviceService.java
    │   │   ├── security/
    │   │   │   ├── JwtService.java
    │   │   │   ├── JwtAuthenticationFilter.java
    │   │   │   └── CustomUserDetailsService.java
    │   │   └── exception/
    │   │       ├── GlobalExceptionHandler.java
    │   │       ├── ResourceNotFoundException.java
    │   │       ├── DuplicateResourceException.java
    │   │       └── UnauthorizedAccessException.java
    │   └── resources/application.yml
    └── test
        ├── java/com/aiassistiveglasses/controller/
        │   ├── AuthControllerTest.java
        │   ├── UserControllerTest.java
        │   └── DeviceControllerTest.java
        └── resources/application.yml   (H2 in-memory, overrides main config)
```

This matches the package layout you specified, with two small additions:
`OpenApiConfig` (needed to register the JWT bearer scheme in Swagger) and
`UnauthorizedAccessException` (kept available for explicit 403 cases,
though device ownership checks currently return 404 — see note in §4).

---

## 2. Dependencies added (`pom.xml`)

Parent: `spring-boot-starter-parent:3.3.4` (Java 21)

| Dependency | Purpose |
|---|---|
| spring-boot-starter-web | REST controllers |
| spring-boot-starter-data-jpa | Hibernate/JPA repositories |
| spring-boot-starter-security | Auth, filter chain |
| spring-boot-starter-validation | Jakarta Bean Validation |
| com.mysql:mysql-connector-j | MySQL JDBC driver |
| com.h2database:h2 (test scope) | In-memory DB for tests |
| io.jsonwebtoken:jjwt-api/impl/jackson 0.12.6 | JWT issue/verify |
| org.projectlombok:lombok | Boilerplate reduction |
| org.springdoc:springdoc-openapi-starter-webmvc-ui 2.6.0 | Swagger UI / OpenAPI 3 |
| spring-boot-starter-test, spring-security-test (test scope) | MockMvc + security test support |

---

## 3. Database schema

Database: `ai_assistive_glasses` (create manually first — see §6).

**`users`**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK, auto-increment | |
| full_name | VARCHAR, NOT NULL | |
| email | VARCHAR, NOT NULL, UNIQUE | |
| phone_number | VARCHAR | |
| password | VARCHAR, NOT NULL | BCrypt hash only, never plain text |
| role | VARCHAR (enum: USER, ADMIN), NOT NULL | |
| created_at | DATETIME, NOT NULL | set on insert |
| updated_at | DATETIME, NOT NULL | set on insert/update |
| enabled | BOOLEAN, NOT NULL | |

**`devices`**

| Column | Type | Notes |
|---|---|---|
| id | BIGINT, PK, auto-increment | |
| device_name | VARCHAR, NOT NULL | |
| device_identifier | VARCHAR, NOT NULL, UNIQUE | |
| device_type | VARCHAR, NOT NULL | |
| status | VARCHAR (enum: ACTIVE, INACTIVE, CONNECTED, DISCONNECTED), NOT NULL | |
| created_at | DATETIME, NOT NULL | |
| last_connected_at | DATETIME, nullable | set when status → CONNECTED |
| user_id | BIGINT, FK → users.id, NOT NULL | |

Relationship: `User 1 — N Device` (unidirectional `Device.user`, lazy-loaded,
`@JsonIgnore`d to prevent recursive/leaky serialization). Controllers never
return entities directly — only `UserResponse` / `DeviceResponse` DTOs, so
the password field and the back-reference never reach an API response.

`spring.jpa.hibernate.ddl-auto=update` will create/update these tables
automatically on startup — no manual DDL needed beyond creating the database
itself.

---

## 4. API endpoint list

| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Create account |
| POST | `/api/auth/login` | Public | Get JWT |
| GET | `/api/users/me` | JWT | Current user's profile |
| PUT | `/api/users/me` | JWT | Update fullName/phoneNumber only |
| PUT | `/api/users/me/password` | JWT | Change password (added beyond spec, per §5's "if needed") |
| POST | `/api/devices` | JWT | Register a device |
| GET | `/api/devices` | JWT | List my devices |
| GET | `/api/devices/{id}` | JWT | Get one of my devices |
| PUT | `/api/devices/{id}` | JWT | Update one of my devices |
| DELETE | `/api/devices/{id}` | JWT | Unregister one of my devices |

**Design note on cross-user device access:** `DeviceRepository.findByIdAndUser`
scopes every lookup to the authenticated user in one query, so a device that
belongs to someone else and a device that doesn't exist both come back as a
plain **404 Not Found**, not 403. This is the standard secure pattern
(it avoids confirming to an attacker that a given device ID exists at all).
If you'd rather distinguish "exists but not yours" with a 403, swap the
`ResourceNotFoundException` in `DeviceService.findOwnedDeviceOrThrow` for
`UnauthorizedAccessException` (already wired into `GlobalExceptionHandler`) —
that requires an extra existence check first.

**Response envelope:** every endpoint returns
`{ "success": bool, "message": string, "data": ... }` per your §12 spec.
Login therefore returns `{ success, message, data: { token, user } }` rather
than the flatter `{ success, message, token, user }` shown in your §4 example
— I standardized on the one consistent envelope from §12 rather than a
special case for login. Say the word if you want login flattened instead.

---

## 5. Important configuration (`application.yml`)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_assistive_glasses?createDatabaseIfNotExist=false&useSSL=false&serverTimezone=UTC
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
  jpa:
    hibernate:
      ddl-auto: update

jwt:
  secret: ${JWT_SECRET:this-is-a-local-development-only-jwt-secret-change-it-now}
  expiration: ${JWT_EXPIRATION:86400000}
```

- All secrets are overridable via environment variables; nothing production-
  sensitive is hard-coded, and the default is clearly marked dev-only.
- The default secret is intentionally long enough to satisfy HS256's
  256-bit minimum key length — a short literal like `change-this-secret`
  would actually throw a `WeakKeyException` at startup, so I lengthened it.
  **Set a real `JWT_SECRET` env var before deploying anywhere real.**
- CORS is wide open (`*`) for local Expo/React Native development
  (`SecurityConfig.corsConfigurationSource`) — tighten `allowedOriginPatterns`
  before production.

---

## 6. How to run

**Prerequisites:** JDK 21, Maven 3.9+, a running MySQL 8+ instance.

```bash
# 1. Create the database (one time)
mysql -u root -p -e "CREATE DATABASE ai_assistive_glasses;"

# 2. (optional) export overrides instead of using defaults
export DB_USERNAME=root
export DB_PASSWORD=yourpassword
export JWT_SECRET=$(openssl rand -base64 48)

# 3. Run
cd ai-assistive-glasses-backend
mvn spring-boot:run
```

The API comes up on `http://localhost:8080`. Swagger UI:
`http://localhost:8080/swagger-ui.html`.

To run the test suite (uses an in-memory H2 DB, no MySQL needed):

```bash
mvn test
```

> **Note on this environment:** I built and reviewed every file here, but I
> don't have Maven or internet access in this sandbox, so I could not
> actually run `mvn compile` / `mvn test` myself. I've checked the code
> carefully by hand (JJWT 0.12.x fluent API, Lombok annotations, Spring
> Security 6 filter-chain style, DTO field ordering against `@AllArgsConstructor`,
> etc.), but please run `mvn clean test` yourself as the real verification
> step — and send me any compiler errors if they come up; I'll fix them
> immediately.

---

## 7. Testing APIs with Postman

1. **Register**
   `POST http://localhost:8080/api/auth/register`
   Body (JSON):
   ```json
   { "fullName": "Test User", "email": "test@gmail.com", "phoneNumber": "9876543210", "password": "Test@123" }
   ```

2. **Login**
   `POST http://localhost:8080/api/auth/login`
   Body:
   ```json
   { "email": "test@gmail.com", "password": "Test@123" }
   ```
   Copy `data.token` from the response.

3. **Authenticated requests** — in Postman, set
   `Authorization` header to `Bearer <token>` (or use the "Bearer Token" auth
   tab and paste just the token), then call:
   - `GET /api/users/me`
   - `PUT /api/users/me` with `{ "fullName": "...", "phoneNumber": "..." }`
   - `PUT /api/users/me/password` with `{ "currentPassword": "...", "newPassword": "..." }`
   - `POST /api/devices` with `{ "deviceName": "My Glasses", "deviceIdentifier": "GLS-001", "deviceType": "GLASSES_V1" }`
   - `GET /api/devices`, `GET /api/devices/{id}`, `PUT /api/devices/{id}`, `DELETE /api/devices/{id}`

   You can also just open `http://localhost:8080/swagger-ui.html`, click
   "Authorize", paste the token, and try every endpoint from there without
   Postman at all.

---

## 8. Remaining items / TODOs for you

- **Compilation/tests not run here** — no Maven/network in this sandbox (see §6 note). Please run `mvn clean test` locally as the first step.
- **Login response shape** flattened vs. nested under `data` — see the note in §4; let me know if you want it changed.
- **403 vs 404 for cross-user device access** — currently 404 by design (see §4); flip to `UnauthorizedAccessException` if you'd rather have 403.
- Phase 1 deliberately has **no** rate limiting, refresh tokens, email verification, or account lockout — worth considering before this goes anywhere near production, even though they weren't in scope here.
- `ddl-auto: update` is convenient for development but isn't a real migration strategy — consider Flyway/Liquibase before Phase 2 adds more tables.
