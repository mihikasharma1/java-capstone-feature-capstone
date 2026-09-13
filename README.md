# Library Management System — Capstone Submission

**Live URL:** http://library-management-api-env.eba-23bfdumz.us-east-1.elasticbeanstalk.com

**Swagger UI:** http://library-management-api-env.eba-23bfdumz.us-east-1.elasticbeanstalk.com/swagger-ui/index.html

**Health check:** http://library-management-api-env.eba-23bfdumz.us-east-1.elasticbeanstalk.com/actuator/health

---

## 1. Overview

A Spring Boot REST API for library book reservations, built across six milestones: data modeling, authentication, catalog service, reservation lifecycle, testing, and AWS deployment.

**Stack:** Java 21 (Corretto, production) / Java 25 (local dev) · Spring Boot 3.5.6 · Spring Data JPA / Hibernate · Spring Security + JWT (jjwt) · H2 (dev) / PostgreSQL on RDS (production) · JUnit 5 + Mockito + AssertJ · JaCoCo · springdoc-openapi (Swagger UI) · deployed via AWS Elastic Beanstalk (Corretto 21 on Amazon Linux 2023).

**Test coverage:** 91% overall instruction coverage (JaCoCo), 54 tests, 0 failures.

| Package | Coverage |
|---|---|
| `dto` | 100% |
| `entity` | 100% |
| `security` | 94% |
| `controllers` | 93% |
| `config` | 89% |
| `service` | 90% |
| `exception` | 82% |

---

## 2. Setup & run

**Local (dev, H2):**
```bash
./mvnw clean install
./mvnw spring-boot:run
```
Runs on `http://localhost:8080` with an in-memory H2 database, auto-seeded on every startup (see `DataSeeder`, `@Profile("dev")`) with 3 users and 7 books. Seeded accounts: `patron@example.com` / `librarian@example.com` / `patron2@example.com`, all password `TestPass123!`.

H2 console: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:librarydb`, user `sa`, no password).

**Tests + coverage:**
```bash
./mvnw clean test
open target/site/jacoco/index.html
```

**Production:** see live URL above. Deployed on AWS Elastic Beanstalk with a PostgreSQL RDS backend — see §6 for full infrastructure details and proof.

---

## 3. Architecture

Standard layered structure:

```
controller  → deserializes requests, calls one service method, returns a DTO
service     → business rules, orchestration, entity ↔ DTO mapping
repository  → Spring Data JPA interfaces; query logic only
entity      → JPA-mapped domain objects, mirror the DB schema
dto         → exact request/response wire shapes
security    → JWT issuance/validation, Spring Security wiring
config      → cross-cutting Spring configuration (security, auditing, OpenAPI)
```

<!-- Screenshot: Swagger UI landing page listing all endpoints -->
<img width="1890" height="987" alt="Screenshot 2026-09-13 161914" src="https://github.com/user-attachments/assets/9ed4272b-d47f-4c4c-95a8-5894eabb7efe" />


---

## 4. Deliberate deviations from the spec

**a) No mechanism to create a LIBRARIAN account.** Every user defaults to `PATRON` (US-001), and the 10-endpoint contract has no way to promote one — meaning, as written, the system could never contain a librarian. Added:
```
PATCH /api/users/{userId}/role   (LIBRARIAN-only)
```
The very first librarian is still bootstrapped outside the API (dev: `DataSeeder`; prod: a one-time manual DB update via a temporary, immediately-terminated EC2 instance). This is the resolution to the "who grants the first privilege" problem in any role-based system — an open self-promotion endpoint would be a privilege-escalation hole.

**b) No mechanism to add books to the catalog.** The contract only supports browsing an already-populated catalog (US-004–006). Added:
```
POST /api/catalog/books   (LIBRARIAN-only)
```
`availableCopies` always initializes equal to `totalCopies`. Both additions reuse the existing `hasRole("LIBRARIAN")` pattern and error-handling shape rather than introducing anything new.

---

## 5. Issues found in the starter project, and fixes applied

**Security**
- **The JWT signing secret was hardcoded in plaintext in `application-dev.properties` and committed/pushed to the repo.** Anyone with repo access could forge valid tokens. Fixed by moving the secret out of source control entirely: production reads `jwt.secret` from a `JWT_SECRET` environment variable (generated fresh via `openssl rand -base64 32`, never reused from the dev value). Going forward, no JWT secret exists in any tracked file.
- `application-prod.properties` had a plaintext password fallback (`${RDS_PASSWORD:password}`) — fixed to `${RDS_PASSWORD}` with no fallback, so a missing production credential fails loudly instead of silently trying a guessable default.


## 6. Proof it runs in production

**Green health check**, confirming both the app and its database connection are live:
<img width="1053" height="588" alt="image" src="https://github.com/user-attachments/assets/5cd8da94-1ca6-4392-881b-96bd7b22fee3" />
<img width="412" height="280" alt="image" src="https://github.com/user-attachments/assets/2bde8886-f4f8-453c-91b5-e86ea89bb97f" />


**Persistence across restarts:** restarted the Elastic Beanstalk environment mid-testing and confirmed a previously-registered user could still log in afterward — proving genuine PostgreSQL persistence, not H2-style wipe-on-restart behavior.

**Database is genuinely private, not publicly reachable:** attempted a direct connection from a local machine —
```bash
psql -h java-capstone.cuxwe6kqmml0.us-east-1.rds.amazonaws.com -U postgres -d librarydb
```
— which times out, confirming `Publicly accessible: No` is correctly enforced at the RDS level and the database is reachable only from within the VPC.
<img width="1506" height="112" alt="image" src="https://github.com/user-attachments/assets/44a0cfc8-4fe2-4468-8eb2-be5eb43c4bcc" />


---

## 7. Deployment infrastructure

- **Platform:** Corretto 21 on Amazon Linux 2023 (Elastic Beanstalk, single-instance)
- **Database:** PostgreSQL 15.x on RDS, `db.t4g.micro`, single-AZ, 20 GiB gp2, **not publicly accessible**
- **Required environment variables:** `SPRING_PROFILES_ACTIVE=prod`, `SERVER_PORT=5000`, `RDS_HOSTNAME`, `RDS_PORT`, `RDS_DB_NAME`, `RDS_USERNAME`, `RDS_PASSWORD`, `JWT_SECRET`
- **Networking:** RDS's security group allows inbound PostgreSQL/5432 specifically from the Elastic Beanstalk environment's security group (not the reverse — a mistake made and corrected during setup, see §6)
- **Secrets:** no credentials are committed to source control; all are environment-variable-driven in production and generated fresh for this deployment (JWT secret via `openssl rand -base64 32`, RDS master password set independently of any dev-profile value)

