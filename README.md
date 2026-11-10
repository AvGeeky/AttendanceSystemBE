# Attendez Backend

Attendez is a Spring Boot backend for managing academic attendance, timetables, classes, faculty, students, and administrative data. It supports multiple attendance-capture methods, including QR codes, passcodes, manual entry, and live attendance views.

The backend is currently under active development and is deployed for the SSN College environment at [attendez.ssn.net](https://attendez.ssn.net/). APIs and database models may change as the application evolves.

> **Repository status:** This repository is intended for internal development at present. Do not commit credentials, private keys, Firebase service-account data, certificates, or production connection strings.

## Features

- Student authentication through:
  - Email and one-time password (OTP)
  - Google ID tokens
  - Firebase ID tokens
- Faculty login, password recovery, JWT refresh, and encrypted identity cookies
- Separate JWT utilities and expiration settings for students, faculty, and super administrators
- Class creation, updates, transfers, and logical groupings
- Timetable refresh and timetable parsing
- Attendance capture using:
  - Time-sensitive QR codes
  - Faculty-generated passcodes
  - Manual attendance entry
  - Substitution codes
- Live attendance views and lecture-level attendance management
- Faculty mentor and class-advisor attendance views
- Super-admin management of students, faculty, and logical groupings
- MongoDB persistence
- Redis-backed OTP, attendance, and short-lived code state
- Prometheus metrics through Spring Boot Actuator
- OpenAPI/Swagger support during development
- Docker-based deployment with optional Nginx TLS termination and MongoDB backups

## Technology stack

| Area | Technology |
| --- | --- |
| Runtime | Java 21 |
| Framework | Spring Boot 3.4.5 |
| Build | Maven Wrapper |
| API | Spring Web MVC |
| Persistence | Spring Data MongoDB |
| Cache and ephemeral state | Spring Data Redis with Jedis/Lettuce configuration |
| Authentication | JWT, Google OAuth ID-token verification, Firebase Admin SDK |
| Email | JavaMail and Gmail API integrations |
| API documentation | SpringDoc OpenAPI |
| Monitoring | Spring Boot Actuator, Micrometer, Prometheus |
| Deployment | Docker Compose, Nginx |

## Architecture

The application follows a controller, function/service, and database-access organization:

```text
HTTP request
    |
    v
Controllers/
    |-- Students
    |-- Faculty
    |-- Attendance
    `-- SuperAdmin
    |
    v
functions/
    |-- Students
    |-- Faculty
    |-- Attendance
    |-- Class
    |-- LogicalGrouping
    |-- SuperAdmin
    `-- TimetableParser
    |
    +--> database/MongoDB
    +--> database/redis
    +--> JWT, Firebase, email, and encryption utilities
```

The main application class is `com.appbuildersinc.attendance.SmartAttendanceProjectApplication`. It enables asynchronous execution with Spring's `@EnableAsync`.

### Response conventions

Most application responses use a `status` field:

- `status: "S"` indicates success.
- `status: "E"` indicates an application error.

Common HTTP statuses include:

- `200 OK` for successful requests
- `400 Bad Request` for missing or malformed input
- `401 Unauthorized` for missing, invalid, or expired authentication
- `403 Forbidden` when the authenticated user lacks permission
- `404 Not Found` when a requested resource does not exist
- `503 Service Unavailable` when a dependent service or operation cannot be completed

The exact response body depends on the endpoint. Use the controller implementation or generated OpenAPI documentation as the source of truth for request and response fields.

## Requirements

For local development, install:

- JDK 21
- Git
- Docker and Docker Compose, if running MongoDB and Redis through containers
- A MongoDB instance
- A Redis instance

The Maven Wrapper is included, so Maven does not need to be installed separately.

## Configuration

The application reads configuration from `src/main/resources/application.properties` and environment variables. Create a local `.env` file for development, or provide the variables through your shell, IDE, container runtime, or deployment platform.

### Required and commonly used variables

| Variable | Purpose |
| --- | --- |
| `API_KEY` | MongoDB connection URI used by the database classes and Spring Data MongoDB |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port |
| `REDIS_PASSWORD` | Redis password when Redis authentication is enabled |
| `JWT_HMAC_SECRET` | Secret used to sign and validate application JWTs |
| `JWT_EXPIRATION_MINUTES_STUDENT` | Student JWT lifetime in minutes |
| `JWT_EXPIRATION_MINUTES_FACULTY` | Faculty JWT lifetime in minutes |
| `JWT_EXPIRATION_MINUTES_SUPERADMIN` | Super-admin JWT lifetime in minutes |
| `KEYPAIR_PASSPHRASE` | Passphrase used for application key-pair operations |
| `DEFAULT_TTL_CODE_SECONDS_SERVER` | Server-side TTL for short-lived attendance codes |
| `DEFAULT_TTL_CODE_SECONDS_FRONTEND` | Frontend-facing TTL for short-lived attendance codes |
| `MAIL_ID` | Email account used for OTP and notification delivery |
| `MAIL_PASSKEY` | Mail account password or app password |
| `GOOGLE_CLIENT_ID` | Google OAuth client ID used to verify student ID tokens |
| `FIREBASE_PROJECT_ID` | Firebase project identifier |
| `FIREBASE_PRIVATE_KEY` | Firebase service-account private key |
| `FIREBASE_PRIVATE_KEY_ID` | Firebase service-account private-key ID |
| `FIREBASE_CLIENT_EMAIL` | Firebase service-account client email |
| `FIREBASE_CLIENT_ID` | Firebase service-account client ID |
| `FIREBASE_CLIENT_X509_CERT_URL` | Firebase service-account certificate URL |

Some variables are only needed for the corresponding feature. For example, Firebase variables are required for `/student/firebaseAuth`, while mail variables are required for OTP delivery.

### Example local environment

Use placeholders locally and replace them with real values through a secret manager or an untracked `.env` file:

```dotenv
API_KEY=mongodb://localhost:27017/AttendEz
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=

JWT_HMAC_SECRET=replace-with-a-long-random-secret
JWT_EXPIRATION_MINUTES_STUDENT=60
JWT_EXPIRATION_MINUTES_FACULTY=60
JWT_EXPIRATION_MINUTES_SUPERADMIN=60
KEYPAIR_PASSPHRASE=replace-with-a-strong-passphrase

DEFAULT_TTL_CODE_SECONDS_SERVER=60
DEFAULT_TTL_CODE_SECONDS_FRONTEND=60

MAIL_ID=development@example.com
MAIL_PASSKEY=replace-with-an-app-password
GOOGLE_CLIENT_ID=replace-with-google-client-id
```

Do not use production secrets in a local checkout. The Firebase private key must be supplied in the format expected by `FirebaseConfig`; never commit it to source control.

## Running locally

### 1. Start dependencies

Start MongoDB and Redis using local installations or the development Compose definition:

```bash
docker compose -f redis-setup_dev/docker-compose.yml up -d
```

If MongoDB is not already available locally, start a MongoDB container separately or use the MongoDB service from `attendance-final_docker/docker-compose.yml`.

### 2. Configure the environment

Export the variables in the configuration section, or load an untracked `.env` file in the environment used to launch the application.

### 3. Start the application

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

The application listens on `0.0.0.0:8080` by default.

### 4. Build and run the packaged application

```bash
./mvnw clean package
java -jar target/attendance-1.5.jar
```

The exact artifact name is controlled by Maven and may change with the project version. To list generated artifacts:

```bash
ls target/*.jar
```

## Docker deployment

The repository contains more than one Compose definition for different deployment stages.

### Full deployment

`attendance-final_docker/docker-compose.yml` defines the production-style stack:

- `redis`
- `mongodb`
- `app`
- `nginx`
- `mongo-backup`

The application image is built from `attendance-final_docker/springBoot/Dockerfile` and expects a compiled JAR at `target/attendance.jar` in that Docker build context. The Nginx service terminates HTTPS, proxies `/api/` requests to the Spring Boot application, and mounts TLS certificates from the Compose host.

Before starting this stack:

1. Provide an `.env` file beside the Compose file.
2. Provide the certificate files expected by the Nginx volume mounts.
3. Ensure MongoDB and Redis credentials are not committed.
4. Build the application artifact and place or copy it where the Dockerfile expects it.

Start the stack with:

```bash
cd attendance-final_docker
docker compose up -d --build
```

Inspect service status and logs with:

```bash
docker compose ps
docker compose logs -f app
```

The backup service performs an immediate compressed MongoDB dump and schedules daily backups, retaining backups newer than three days. Verify backup files and retention behavior in the deployment environment before relying on them for disaster recovery.

### Monitoring-oriented deployment

`attendance-proj-d_m_nginx/docker-compose.yml` is an alternate deployment definition containing Redis, the application, Nginx, Cloudflare Tunnel, Prometheus, Grafana, and a Redis exporter. It references an older application image and additional services, so treat it as environment-specific rather than as the canonical local-development stack.

## API overview

Unless an external reverse proxy is configured, endpoints are served directly from port `8080`. In the production Nginx configuration, backend requests are exposed under `/api/` and forwarded to the application.

### Student endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/student/setEmail` | Start student email OTP flow |
| `POST` | `/student/verifyOTPAndLogin` | Verify OTP and issue a student JWT |
| `POST` | `/student/firebaseAuth` | Verify a Firebase ID token and issue a student JWT |
| `POST` | `/student/googleAuth` | Verify a Google ID token and issue a student JWT |
| `GET` | `/student/getDetails` | Return authenticated student details |
| `GET` | `/student/refreshTimetable` | Refresh the authenticated student's timetable |
| `POST` | `/student/getAttendance` | Return attendance for requested class codes |
| `POST` | `/student/qr/sendCode` | Submit a QR attendance code and digest |
| `POST` | `/student/passcode/sendCode` | Submit a passcode and digest |
| `GET` | `/student/getStudentHmacPasscode` | Retrieve the student's attendance HMAC passcode |

### Faculty and class endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/faculty/login` | Authenticate a faculty member |
| `POST` | `/faculty/setEmail` | Start faculty email/OTP flow |
| `POST` | `/faculty/verifyOtp` | Verify a faculty OTP |
| `POST` | `/faculty/updatePassword` | Update a faculty password |
| `POST` | `/faculty/returnHTTPRefreshCookie` | Set an HTTP-only refresh identity cookie |
| `POST` | `/faculty/refreshJWTWithCookie` | Refresh an expired faculty JWT using the cookie |
| `POST` | `/faculty/refreshJWTWithEncEmail` | Refresh an expired faculty JWT using encrypted email data |
| `GET` | `/faculty/getDetails` | Return authenticated faculty details |
| `POST` | `/faculty/setDetails` | Update faculty profile details |
| `GET` | `/faculty/refreshTimetable` | Refresh the faculty timetable |
| `POST` | `/faculty/createOrUpdateClass` | Create or update a class |
| `DELETE` | `/faculty/dropClass` | Drop a faculty class assignment |
| `POST` | `/faculty/transferClass` | Transfer a class to another faculty member |
| `GET` | `/faculty/getClassDetails` | Return class details |
| `GET` | `/faculty/getAllLogicalGroupings` | Return department logical groupings |
| `POST` | `/faculty/updateMenteeListAndReturnDetails` | Update and retrieve a mentee list |
| `GET` | `/faculty/getMentorListAttendance` | Return attendance for mentees |
| `GET` | `/faculty/getAdvisorListAttendance` | Return attendance for advisees |
| `POST` | `/faculty/getStudentAttendanceByClassCode` | Return student attendance for a class |
| `POST` | `/faculty/getLectureAttendanceByClassCode` | Return lecture attendance for a class |
| `POST` | `/faculty/flipAttendance` | Toggle an individual attendance record |
| `POST` | `/faculty/deleteLecture` | Delete a lecture attendance record |

Faculty JWT claims distinguish standard faculty from additional roles such as mentor, class advisor, or both. Authorization checks must remain consistent with these role values.

### Attendance-session endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/faculty/createSubstitutionCode` | Create a dated substitution code |
| `GET` | `/faculty/fetchClassCodeFromSubstitutionCode` | Resolve a substitution code to a class |
| `POST` | `/faculty/qr/generateQRCode` | Generate QR attendance codes |
| `POST` | `/faculty/passcode/generateCode` | Generate a passcode attendance session |
| `GET` | `/faculty/liveAttendanceViewWithVersion` | Read live attendance and its version |
| `POST` | `/faculty/qrpasscode/confirmAttendanceClose` | Close and persist an attendance session |
| `POST` | `/faculty/getAllStudentDetails` | Get students for a class |
| `POST` | `/faculty/saveManualAttendance` | Save manually entered attendance |
| `GET` | `/generateDigest` | Generate a digest for testing or verification |

QR and passcode flows use short-lived values and digest/HMAC checks. Clients should not cache or reuse attendance codes beyond the TTL configured for the deployment.

### Super-admin endpoints

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/SuperAdmin/login` | Authenticate a super administrator |
| `POST` | `/SuperAdmin/addStudents` | Add student records |
| `POST` | `/SuperAdmin/viewAllStudents` | List students |
| `DELETE` | `/SuperAdmin/deleteStudents` | Delete student records |
| `POST` | `/SuperAdmin/addOrUpdateTeacher` | Add or update a faculty record |
| `GET` | `/SuperAdmin/viewAllTeachers` | List faculty records |
| `DELETE` | `/SuperAdmin/deleteTeacher` | Delete a faculty record |
| `POST` | `/SuperAdmin/createOrEditLogicalGrouping` | Create or edit a logical grouping |
| `GET` | `/SuperAdmin/viewAllGroupings` | List logical groupings |
| `DELETE` | `/SuperAdmin/deleteGrouping` | Delete a logical grouping |
| `POST` | `/SuperAdmin/TimeTableParser` | Upload or process timetable data |
| `GET` | `/SuperAdmin/health` | Super-admin health check |
| `GET` | `/ping` | Basic availability check |

The `/test/genHash` endpoint is a development/testing utility and should not be exposed in an internet-facing production deployment.

### Authentication example

Authenticated endpoints generally expect the JWT in the `Authorization` header:

```http
Authorization: Bearer your-jwt-token
```

Example request:

```bash
curl -H "Authorization: Bearer $STUDENT_TOKEN" \
  http://localhost:8080/student/getDetails
```

Consult each controller for required query parameters, request bodies, and role restrictions. Several endpoints use `GET` requests with request bodies or query parameters, so clients should follow the implemented controller contract rather than assuming conventional REST semantics.

## OpenAPI and health endpoints

When running locally, SpringDoc normally exposes:

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

The production Nginx configuration explicitly blocks `/swagger-ui` and `/v3/api-docs`.

Actuator exposure is limited by `application.properties` to:

- `/actuator/health`
- `/actuator/info`
- `/actuator/prometheus`

Health details are disabled by default. Prometheus scraping is enabled and HTTP request histograms are configured for latency dashboards.

## Project layout

```text
.
├── pom.xml
├── mvnw, mvnw.cmd
├── src
│   ├── main/java/com/appbuildersinc/attendance
│   │   ├── source/Controllers       # HTTP API controllers
│   │   ├── source/functions         # Application/business logic
│   │   ├── source/database          # MongoDB and Redis access
│   │   └── source/Utilities          # JWT, email, Firebase, errors, CORS, crypto
│   ├── main/resources/application.properties
│   └── test                         # Spring context tests
├── attendance-final_docker          # Full MongoDB/Redis/app/Nginx deployment
├── attendance-proj-d_m_nginx        # Alternate monitoring/tunnel deployment
└── redis-setup_dev                  # Redis development Compose setup
```

## Testing

Run the test suite with:

```bash
./mvnw test
```

The current test suite includes a Spring application-context test. Integration tests that require MongoDB, Redis, email, Firebase, or Google credentials should provide isolated test configuration and test-only credentials rather than using production services.

Before opening a change, also build the application:

```bash
./mvnw clean verify
```

## Security and operations

- Store secrets in a secret manager or deployment environment, not in Git.
- Use a long, random `JWT_HMAC_SECRET`; changing it invalidates existing JWTs.
- Use separate secrets and credentials for development, staging, and production.
- Restrict MongoDB and Redis network access to the application network.
- Keep TLS private keys readable only by the deployment process.
- Do not expose Swagger, OpenAPI, password-hashing utilities, or administrative endpoints publicly without an appropriate access boundary.
- Confirm that Nginx CORS configuration matches the actual frontend origin before deployment.
- Monitor `/actuator/prometheus` through an internal monitoring network rather than exposing it directly to the public internet.
- Test MongoDB restore procedures, not only backup creation.
- Review TTL values for OTPs, QR codes, passcodes, and Redis keys when changing attendance behavior.

## Development workflow

1. Create a feature branch.
2. Configure local MongoDB, Redis, and required environment variables.
3. Run `./mvnw test`.
4. Implement the smallest cohesive change.
5. Run `./mvnw clean verify`.
6. Review endpoint authorization and sensitive-data handling.
7. Keep deployment files and application configuration synchronized when changing ports, service names, or environment variables.

## License and contribution status

This project is currently maintained as an internal, closed-development codebase. No open-source license or public contribution process is provided at this time. Treat all source code and operational configuration as private unless the project owners explicitly authorize reuse or contribution.
