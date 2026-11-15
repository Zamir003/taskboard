# TaskBoard

A REST API for managing personal tasks and labels. Users register, sign in with a JWT, and can only see
and change **their own** tasks and labels. They can create tasks, attach labels, set deadlines and
reminders, move tasks through a status flow, search them with filters, sorting and pagination, and
export completed tasks as CSV.

## Features

- **Task CRUD**: create, read, update (full replacement) and delete your own tasks
- **Labels**: per-user labels, many labels per task (many-to-many)
- **Status flow**: `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`, with enforced transition rules
- **Search**: filter by status, label, deadline range and keyword, with sorting and pagination
- **Ownership protection**: every query is scoped to the authenticated user
- **Reminders**: a scheduled job turns due reminders into in-app notifications
- **CSV export**: download all completed (`DONE`) tasks
- **Soft delete**: deleted tasks and labels are kept in the database but hidden everywhere

## Tech stack

| Area           | Technology                                                  |
|----------------|-------------------------------------------------------------|
| Language       | Java 17                                                     |
| Framework      | Spring Boot 4.1 (Web MVC, Data JPA, Security, Validation)   |
| Persistence    | PostgreSQL, Hibernate 7                                     |
| Authentication | JWT (jjwt 0.12), BCrypt password hashing                    |
| Mapping        | MapStruct 1.6                                               |
| CSV            | Apache Commons CSV                                          |
| API docs       | springdoc-openapi (Swagger UI)                              |
| Build          | Maven (wrapper included)                                    |

## Getting started

### Prerequisites

- JDK 17+
- PostgreSQL with an empty database named `taskboard` (or another name, set via `DB_URL`)

### Configuration

All settings come from environment variables:

| Variable                 | Required | Default                                       | Description                          |
|--------------------------|----------|-----------------------------------------------|--------------------------------------|
| `DB_PASSWORD`            | yes      | none                                          | PostgreSQL password                  |
| `JWT_SECRET`             | yes      | none                                          | Base64-encoded key, at least 32 bytes |
| `DB_URL`                 | no       | `jdbc:postgresql://localhost:5432/taskboard`  | JDBC URL                             |
| `DB_USERNAME`            | no       | `postgres`                                    | PostgreSQL user                      |
| `JWT_EXPIRATION_SECONDS` | no       | `1800`                                        | Access token lifetime                |

Generate a JWT secret:

```bash
openssl rand -base64 32
```

The application fails at startup if `JWT_SECRET` is missing, is not valid Base64, or decodes to fewer than 32 bytes.

The schema is created and updated automatically (`spring.jpa.hibernate.ddl-auto=update`).

### Run

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

The API starts on `http://localhost:8080`.

Swagger UI: `http://localhost:8080/swagger-ui.html`. Register, log in, then click **Authorize** and paste
the `jwtToken` from the login response.

### Build

```bash
./mvnw clean package
```

## API overview

All endpoints except register and login require an `Authorization: Bearer <token>` header.

### Authentication

| Method | Path                 | Description                                   |
|--------|----------------------|-----------------------------------------------|
| POST   | `/api/auth/register` | Create an account → `201`                     |
| POST   | `/api/auth/login`    | Get an access token (`jwtToken`, `expiresInSeconds`) |

```json
POST /api/auth/register
{ "name": "Jane Doe", "email": "jane@example.com", "password": "secret-password" }
```

Passwords must be 8–72 characters (and at most 72 bytes in UTF-8, a BCrypt limit). Emails are
case-insensitive.

### Tasks

| Method | Path                                | Description                                    |
|--------|-------------------------------------|------------------------------------------------|
| GET    | `/api/tasks`                        | Search with filters, sorting and pagination    |
| GET    | `/api/tasks/{id}`                   | Get one task                                   |
| POST   | `/api/tasks`                        | Create a task (status starts as `TODO`) → `201` |
| PUT    | `/api/tasks/{id}`                   | Replace a task (fields sent as `null` are cleared) |
| DELETE | `/api/tasks/{id}`                   | Soft delete → `204`                            |
| PATCH  | `/api/tasks/{id}/status`            | Change status                                  |
| POST   | `/api/tasks/{id}/labels/{labelId}`  | Attach a label (idempotent)                    |
| DELETE | `/api/tasks/{id}/labels/{labelId}`  | Detach a label (idempotent)                    |
| GET    | `/api/tasks/export`                 | Download `DONE` tasks as CSV                   |

```json
POST /api/tasks
{
  "title": "Write report",
  "description": "Quarterly summary",
  "deadline": "2026-12-01T17:00:00Z",
  "reminderAt": "2026-12-01T09:00:00Z",
  "labelIds": [1, 2]
}
```

- `title` is required (max 200 characters), `description` max 2000.
- `reminderAt` must be in the future and must not be after `deadline`.
- `labelIds` must be your own labels (max 50 per request). `PUT` replaces the whole label set.

### Status transitions

| From          | Allowed targets                |
|---------------|--------------------------------|
| `TODO`        | `IN_PROGRESS`, `BLOCKED`, `DONE` |
| `IN_PROGRESS` | `TODO`, `BLOCKED`, `DONE`      |
| `BLOCKED`     | `TODO`, `IN_PROGRESS`          |
| `DONE`        | `TODO`                         |

Any other transition, including a transition to the same status, returns `409` with the allowed targets
in the message. `completedAt` is set when a task becomes `DONE` and cleared when it leaves `DONE`.

### Search, sorting and pagination

```
GET /api/tasks?status=TODO&status=BLOCKED&labelId=3
              &deadlineFrom=2026-11-01T00:00:00Z&deadlineTo=2026-11-30T23:59:59Z
              &keyword=report&sort=deadline,asc&page=0&size=20
```

| Parameter                     | Description                                                        |
|-------------------------------|--------------------------------------------------------------------|
| `status`                      | One or more statuses                                               |
| `labelId`                     | Tasks that have this label                                         |
| `deadlineFrom`, `deadlineTo`  | Inclusive deadline range (tasks without a deadline are excluded)   |
| `keyword`                     | Case-insensitive search in title and description (max 100 chars)   |
| `sort`                        | `title`, `status`, `deadline`, `createdAt`, `updatedAt`, `completedAt` |
| `page`, `size`                | Page number (from 0) and page size (default 20, max 100)           |

Default sort is `createdAt,desc`. Sorting by any other field returns `400`.

### Labels

| Method | Path               | Description                          |
|--------|--------------------|--------------------------------------|
| GET    | `/api/labels`      | List your labels (sorted by name)    |
| GET    | `/api/labels/{id}` | Get one label                        |
| POST   | `/api/labels`      | Create a label → `201`               |
| PUT    | `/api/labels/{id}` | Rename a label                       |
| DELETE | `/api/labels/{id}` | Soft delete → `204`                  |

Label names are unique per user, case-insensitively (max 50 characters). A duplicate returns `409`.

### Notifications

| Method | Path                               | Description                       |
|--------|------------------------------------|-----------------------------------|
| GET    | `/api/notifications`               | List notifications, newest first (paginated) |
| GET    | `/api/notifications/unread-count`  | Number of unread notifications    |
| PATCH  | `/api/notifications/{id}/read`     | Mark as read (idempotent)         |

## Reminders

A scheduled job runs every minute. For each task whose `reminderAt` has passed, which is not `DONE` and
not yet reminded, it creates a notification such as:

```
Reminder: "Write report" is due at 2026-12-01T17:00:00Z
```

Changing `reminderAt` re-arms the reminder. When a task is deleted, its notifications are deleted too.

| Property                   | Default | Description                          |
|----------------------------|---------|--------------------------------------|
| `app.reminder.fixed-delay` | `PT1M`  | Delay between runs                   |
| `app.reminder.batch-size`  | `100`   | Reminders processed per transaction  |

The job assumes a single application instance.

## CSV export

`GET /api/tasks/export` returns `tasks-done.csv` as a download (`text/csv; charset=UTF-8`, with a UTF-8 BOM
so Excel shows non-ASCII characters correctly).

Columns: `id, title, description, labels, deadline, completedAt, createdAt`. Labels are joined with `; `,
and dates are ISO-8601 in UTC. Text cells that start with `=`, `+`, `-`, `@`, tab or carriage return are
prefixed with `'` to prevent spreadsheet formula injection.

## Error responses

Every error uses the same JSON shape:

```json
{
  "timestamp": "2026-11-15T10:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/tasks",
  "validationErrors": { "title": "Title is required" }
}
```

| Status | When                                                                   |
|--------|------------------------------------------------------------------------|
| 400    | Validation error, malformed body, invalid parameter or sort field      |
| 401    | Missing or invalid token, wrong email or password                      |
| 404    | Resource does not exist, is deleted, or belongs to another user        |
| 406    | The `Accept` header asks for a format the endpoint cannot return       |
| 409    | Duplicate email or label name, invalid status transition, concurrent update |
| 415    | Request body is not JSON                                               |

Resources owned by other users return `404`, not `403`, so their existence is not revealed.

## Project structure

```
src/main/java/com/devlab/taskboard/
  config/          Security, OpenAPI, scheduling, clock
  controller/      REST endpoints
  dto/             Request and response records
  entity/          JPA entities (User, Task, Label, Notification)
  enums/           Role, UserStatus, TaskStatus (with transition rules)
  exception/       API exceptions and global exception handler
  mapper/          MapStruct mappers
  repository/      Spring Data repositories
  scheduler/       Reminder job
  security/        JWT filter and service, user principal, 401/403 handlers
  service/         Business logic and transactions
  specification/   JPA Specifications for task search
  util/            Shared helpers
```

## Notes

This project is configured for local development: Swagger UI is public, SQL logging is on, and the schema
is managed by `ddl-auto=update`. There are no automated tests.
