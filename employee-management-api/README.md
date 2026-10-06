# Employee Management REST API

A layered REST API for managing employees, built with **Java 17, Spring Boot 3, Spring Data JPA and PostgreSQL**.

## Features
- Full CRUD (Create, Read, Update, Delete) for employees, plus filtering by department
- Layered architecture: **Controller -> Service -> Repository**
- Input validation with Bean Validation (`@NotBlank`, `@Email`, `@Positive`, ...)
- Global exception handling with a consistent JSON error format
- Unit tests (JUnit 5 + Mockito) for the service layer
- API tests (REST Assured) checking status codes and response bodies
- Postman collection with automated test scripts

## Tech stack
Java 17 | Spring Boot 3.3 | Spring Data JPA (Hibernate) | PostgreSQL | JUnit 5 | Mockito | REST Assured | Postman | Maven | Git

## Project structure
```
src/main/java/com/example/employeemanagement
  controller/   REST endpoints (HTTP only)
  service/      business logic and rules
  repository/   Spring Data JPA interfaces
  model/        JPA entity
  dto/          request/response objects with validation
  exception/    custom exceptions + global handler
src/test/java/...
  service/      EmployeeServiceTest   (JUnit 5 + Mockito)
  controller/   EmployeeApiTest       (REST Assured)
postman/        Postman collection
```

## Running it

**Prerequisites:** JDK 17+, Maven 3.9+, and either Docker or a local PostgreSQL.

1. Start PostgreSQL (easiest with Docker):
   ```bash
   docker compose up -d
   ```
   This creates a database `employee_db` (user `postgres`, password `postgres`).
   Using your own PostgreSQL? Create the database `employee_db`, or set the
   `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` environment variables.

2. Start the app:
   ```bash
   mvn spring-boot:run
   ```
   The API is available at `http://localhost:8080/api/v1/employees`.
   The table is created automatically on first start.

## Running the tests
```bash
mvn test
```
Tests use an in-memory H2 database, so **no PostgreSQL is needed to run them**.

## Endpoints

| Method | URL | Description | Success |
|--------|-----|-------------|---------|
| POST | `/api/v1/employees` | Create an employee | 201 |
| GET | `/api/v1/employees` | List all (optional `?department=HR`) | 200 |
| GET | `/api/v1/employees/{id}` | Get one employee | 200 |
| PUT | `/api/v1/employees/{id}` | Update an employee | 200 |
| DELETE | `/api/v1/employees/{id}` | Delete an employee | 204 |

Error responses: `400` validation/malformed input, `404` not found, `409` duplicate email, `500` unexpected.

### Example
```bash
curl -X POST http://localhost:8080/api/v1/employees \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","email":"john.doe@example.com","department":"Engineering","salary":55000}'
```
Response (201):
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@example.com",
  "department": "Engineering",
  "salary": 55000
}
```
Validation error (400):
```json
{
  "timestamp": "2026-10-07T10:15:30Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/v1/employees",
  "validationErrors": { "email": "Email must be a valid email address" }
}
```

## Postman
Import `postman/Employee-Management-API.postman_collection.json` into Postman (app must be running on port 8080).
Run the whole collection with **Collection Runner**; each request has test scripts and they run in order.
