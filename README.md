# TaskFlow

![Java](https://img.shields.io/badge/Java-25-orange?style=flat-square&logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen?style=flat-square&logo=springboot)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-18-blue?style=flat-square&logo=postgresql)
![JWT](https://img.shields.io/badge/Auth-JWT-black?style=flat-square&logo=jsonwebtokens)
![Build](https://img.shields.io/badge/Build-Gradle-02303A?style=flat-square&logo=gradle)
![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)

> A secure, multi-user project and task management REST API built with Spring Boot.

---

## Table of Contents

1. [Abstract](#1-abstract)
2. [Tech Stack](#2-tech-stack)
3. [Requirements](#3-requirements)
4. [Project Flow](#4-project-flow)
5. [Database Design](#5-database-design)
6. [Class Diagram](#6-class-diagram)
7. [API Design](#7-api-design)
8. [Getting Started](#8-getting-started)
9. [Project Structure](#9-project-structure)

---

## 1. Abstract

TaskFlow is a RESTful backend application developed using Spring Boot, designed to support
collaborative project and task management across multiple users. The system provides JWT-based
authentication and stateless session management, enabling secure access control where project
ownership and membership roles determine what each user can see and do.

Users can create projects, invite members, and manage tasks with full lifecycle support —
including priority levels, status transitions, start and due dates, assignee tracking,
subtasks, and comments. All entities support soft deletion, preserving data integrity while
allowing recovery. Every state change is captured in a structured audit log, providing full
traceability of who changed what and when.

The application uses PostgreSQL as its primary database, with Spring Data JPA handling all
persistence and relationship management. A global exception handling layer ensures consistent
and meaningful error responses across all endpoints. The architecture is intentionally layered
and modular, designed to accommodate future extensions such as Redis caching, Kafka-based
event streaming, and an organizational hierarchy layer without structural changes to the
existing codebase.

---

## 2. Tech Stack

| Technology | Version | Purpose |
|---|---|---|
| Java | 25 | Core programming language |
| Spring Boot | 4.0.6 | Application framework and auto-configuration |
| Spring Web | 4.0.6 | REST API layer, controllers, request/response handling |
| Spring Security | 4.0.6 | JWT authentication, role-based access control |
| Spring Data JPA | 4.0.6 | ORM, repositories, entity relationships |
| PostgreSQL | 16 | Primary relational database |
| MapStruct | 1.5.5 | DTO mapping between entity and response layers |
| Lombok | Latest | Boilerplate reduction |
| jjwt | 0.12.6 | JWT token generation and validation |
| Jackson | 2.18.2 | JSON serialization and deserialization |
| Gradle | 8.x | Build tool and dependency management |

---

## 3. Requirements

### Authentication & User Management
- A user can register with their name, email, and password.
- A user can log in and receive a JWT access token.
- A user can refresh their expired access token.
- A user can view their own profile.
- A user can update their own profile.

### Projects
- A user can create a project and automatically becomes its Owner.
- An Owner can update or soft delete their project.
- An Owner can invite another user to their project as a Member.
- An Owner can remove a Member from their project.
- An Owner can promote a Member to Owner.
- A Member can view projects they belong to.
- A Member cannot delete or update a project.

### Tasks
- An Owner or Member can create a task inside a project they belong to.
- A task must have a title, priority, status, start date, and due date.
- A task can be assigned to any Member of the same project.
- An Owner or Member can update a task.
- An Owner can delete a task (soft delete).
- A task status can only transition: `TODO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`.
- A user can filter tasks by status, priority, and assignee.
- A user can sort tasks by created date or updated date.

### Subtasks
- An Owner or Member can create a subtask under an existing task.
- A subtask follows the same fields as a task.
- A subtask cannot have its own subtask (one level deep maximum).
- An Owner can delete a subtask (soft delete).
- Deleting a task automatically soft deletes all its subtasks.

### Comments
- An Owner or Member can add a comment on a task or subtask.
- A user can edit their own comment.
- A user can delete their own comment (soft delete).

### Audit Log
- Every create, update, and delete action on any entity is recorded.
- The audit log captures entity type, entity ID, action, changed fields, user, and timestamp.
- The audit log is read-only — no user can modify or delete entries.

---

## 4. Project Flow

### 4.1 Authentication Flow

```mermaid
sequenceDiagram
    actor User
    participant API
    participant DB

    User->>API: POST /auth/register (name, email, password)
    API->>DB: Save user with BCrypt hashed password
    DB-->>API: User saved
    API-->>User: accessToken + refreshToken

    User->>API: POST /auth/login (email, password)
    API->>DB: Find user by email
    DB-->>API: User found
    API->>API: Validate BCrypt password
    API-->>User: accessToken + refreshToken

    User->>API: POST /auth/refresh (refreshToken)
    API->>API: Validate refresh token
    API-->>User: New accessToken + refreshToken
```

### 4.2 JWT Token Lifecycle

```mermaid
sequenceDiagram
    actor User
    participant API
    participant JwtFilter
    participant Controller

    User->>API: Request with Authorization: Bearer <token>
    API->>JwtFilter: Intercept request
    JwtFilter->>JwtFilter: Validate token signature
    JwtFilter->>JwtFilter: Extract userId from token
    JwtFilter->>Controller: Pass authenticated request
    Controller-->>User: Response

    Note over JwtFilter: If token invalid or expired
    JwtFilter-->>User: 401 Unauthorized
```

### 4.3 Project & Member Flow

```mermaid
sequenceDiagram
    actor Owner
    actor Member
    participant API
    participant DB

    Owner->>API: POST /projects (name, description)
    API->>DB: Save project + add Owner to ProjectMember
    DB-->>API: Project saved
    API-->>Owner: ProjectResponseDto

    Owner->>API: POST /projects/{id}/members (userId, role)
    API->>DB: Validate owner role
    API->>DB: Save new ProjectMember
    API-->>Owner: 201 Created

    Member->>API: GET /projects
    API->>DB: Find all memberships for user
    DB-->>API: Project list
    API-->>Member: List of projects
```

### 4.4 Task Lifecycle

```mermaid
stateDiagram-v2
    [*] --> TODO: Task Created
    TODO --> IN_PROGRESS: Start Task
    IN_PROGRESS --> IN_REVIEW: Submit for Review
    IN_REVIEW --> DONE: Approve
    DONE --> [*]: Task Completed

    note right of TODO: Default status on creation
    note right of DONE: Cannot transition further
```

### 4.5 Task & Subtask Flow

```mermaid
sequenceDiagram
    actor User
    participant API
    participant DB

    User->>API: POST /projects/{projectId}/tasks
    API->>DB: Validate user is project member
    API->>DB: Save task with project, creator, assignee
    DB-->>API: Task saved
    API-->>User: TaskResponseDto

    User->>API: POST /projects/{projectId}/tasks/{taskId}/subtasks
    API->>DB: Validate parent task exists
    API->>DB: Check parent is not already a subtask
    API->>DB: Save subtask with parentId set
    DB-->>API: Subtask saved
    API-->>User: TaskResponseDto

    User->>API: PATCH /projects/{projectId}/tasks/{taskId}/status
    API->>API: Validate status transition
    API->>DB: Update task status
    DB-->>API: Updated
    API-->>User: TaskResponseDto

    User->>API: DELETE /projects/{projectId}/tasks/{taskId}
    API->>DB: Validate user is project owner
    API->>DB: Soft delete all subtasks
    API->>DB: Soft delete task
    DB-->>API: Deleted
    API-->>User: 204 No Content
```

### 4.6 Comment Flow

```mermaid
sequenceDiagram
    actor User
    participant API
    participant DB

    User->>API: POST /projects/{projectId}/tasks/{taskId}/comments
    API->>DB: Validate user is project member
    API->>DB: Validate task exists in project
    API->>DB: Save comment with task and author
    DB-->>API: Comment saved
    API-->>User: CommentResponseDto

    User->>API: PATCH /projects/{projectId}/tasks/{taskId}/comments/{commentId}
    API->>DB: Validate user is project member
    API->>DB: Validate comment author is logged in user
    API->>DB: Update comment content
    DB-->>API: Updated
    API-->>User: CommentResponseDto

    User->>API: DELETE /projects/{projectId}/tasks/{taskId}/comments/{commentId}
    API->>DB: Validate comment author is logged in user
    API->>DB: Soft delete comment
    DB-->>API: Deleted
    API-->>User: 204 No Content
```

### 4.7 Audit Log Flow

```mermaid
sequenceDiagram
    actor User
    participant Service
    participant AuditLogService
    participant DB

    User->>Service: Perform action (create/update/delete)
    Service->>DB: Save entity change
    Service->>AuditLogService: log(entityType, entityId, action, changedFields, userId)
    AuditLogService->>DB: Save AuditLog record
    DB-->>AuditLogService: Saved
    AuditLogService-->>Service: Done
    Service-->>User: Response
```

---

## 5. Database Design

### 5.1 Entity Design

#### BaseEntity *(inherited by all entities except AuditLog)*

| Field | Type | Notes |
|---|---|---|
| `createdAt` | LocalDateTime | Auto-set on creation |
| `updatedAt` | LocalDateTime | Auto-updated on every save |
| `isDeleted` | Boolean | Default false |
| `deletedAt` | LocalDateTime | Null until soft deleted |

#### User

| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `name` | String | Full name |
| `email` | String | Unique, used for login |
| `password` | String | BCrypt hashed |

#### Project

| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `name` | String | Project name |
| `description` | String | Optional |
| `owner` | User (FK) | User who created the project |

#### ProjectMember

| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `project` | Project (FK) | Reference to project |
| `user` | User (FK) | Reference to user |
| `role` | Enum | OWNER, MEMBER — default MEMBER |

#### Task

| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `title` | String | Task title |
| `description` | String | Optional |
| `priority` | Enum | LOW, MEDIUM, HIGH, CRITICAL |
| `status` | Enum | TODO, IN_PROGRESS, IN_REVIEW, DONE |
| `startDate` | LocalDate | Planned start date |
| `dueDate` | LocalDate | Deadline |
| `assignee` | User (FK) | Nullable — user responsible |
| `project` | Project (FK) | Project this task belongs to |
| `parent` | Task (FK) | Null for root tasks, set for subtasks |
| `createdBy` | User (FK) | User who created the task |

#### Comment

| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `content` | String (TEXT) | Comment body |
| `task` | Task (FK) | Works for both tasks and subtasks |
| `author` | User (FK) | User who wrote the comment |

#### AuditLog *(does not extend BaseEntity)*

| Field | Type | Notes |
|---|---|---|
| `id` | Long (PK) | Auto-generated |
| `entityType` | Enum | TASK, PROJECT, COMMENT, USER, PROJECT_MEMBER |
| `entityId` | Long | ID of affected entity |
| `action` | Enum | CREATE, UPDATE, DELETE |
| `changedFields` | String (JSON) | Snapshot of changed fields |
| `performedBy` | User (FK) | User who triggered the action |
| `performedAt` | LocalDateTime | Exact timestamp |

### 5.2 ERD Diagram

```mermaid
erDiagram
    USER {
        bigint id PK
        varchar name
        varchar email
        varchar password
        timestamp created_at
        timestamp updated_at
        boolean is_deleted
        timestamp deleted_at
    }

    PROJECT {
        bigint id PK
        varchar name
        varchar description
        bigint owner_id FK
        timestamp created_at
        timestamp updated_at
        boolean is_deleted
        timestamp deleted_at
    }

    PROJECT_MEMBER {
        bigint id PK
        bigint project_id FK
        bigint user_id FK
        varchar role
        timestamp created_at
        timestamp updated_at
        boolean is_deleted
        timestamp deleted_at
    }

    TASK {
        bigint id PK
        varchar title
        text description
        varchar priority
        varchar status
        date start_date
        date due_date
        bigint assignee_id FK
        bigint project_id FK
        bigint parent_id FK
        bigint created_by FK
        timestamp created_at
        timestamp updated_at
        boolean is_deleted
        timestamp deleted_at
    }

    COMMENT {
        bigint id PK
        text content
        bigint task_id FK
        bigint author_id FK
        timestamp created_at
        timestamp updated_at
        boolean is_deleted
        timestamp deleted_at
    }

    AUDIT_LOG {
        bigint id PK
        varchar entity_type
        bigint entity_id
        varchar action
        text changed_fields
        bigint performed_by FK
        timestamp performed_at
    }

    USER ||--o{ PROJECT : "owns"
    USER ||--o{ PROJECT_MEMBER : "joins as"
    PROJECT ||--o{ PROJECT_MEMBER : "has"
    PROJECT ||--o{ TASK : "contains"
    USER ||--o{ TASK : "assignee"
    TASK ||--o{ TASK : "parent of"
    TASK ||--o{ COMMENT : "has"
    USER ||--o{ COMMENT : "written by"
    USER ||--o{ AUDIT_LOG : "triggers"
```

---

## 6. Class Diagram

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        # createdAt : LocalDateTime
        # updatedAt : LocalDateTime
        # isDeleted : Boolean
        # deletedAt : LocalDateTime
        + softDelete() void
    }

    class User {
        - id : Long
        - name : String
        - email : String
        - password : String
    }

    class Project {
        - id : Long
        - name : String
        - description : String
        - owner : User
    }

    class ProjectMember {
        - id : Long
        - project : Project
        - user : User
        - role : ProjectRole
    }

    class Task {
        - id : Long
        - title : String
        - description : String
        - priority : TaskPriority
        - status : TaskStatus
        - startDate : LocalDate
        - dueDate : LocalDate
        - assignee : User
        - project : Project
        - parent : Task
        - createdBy : User
        - subTasks : List~Task~
    }

    class Comment {
        - id : Long
        - content : String
        - task : Task
        - author : User
    }

    class AuditLog {
        - id : Long
        - entityType : EntityType
        - entityId : Long
        - action : AuditAction
        - changedFields : String
        - performedBy : User
        - performedAt : LocalDateTime
    }

    class TaskPriority {
        <<enumeration>>
        LOW
        MEDIUM
        HIGH
        CRITICAL
    }

    class TaskStatus {
        <<enumeration>>
        TODO
        IN_PROGRESS
        IN_REVIEW
        DONE
    }

    class ProjectRole {
        <<enumeration>>
        OWNER
        MEMBER
    }

    class AuditAction {
        <<enumeration>>
        CREATE
        UPDATE
        DELETE
    }

    class EntityType {
        <<enumeration>>
        USER
        PROJECT
        PROJECT_MEMBER
        TASK
        COMMENT
    }

    BaseEntity <|-- User
    BaseEntity <|-- Project
    BaseEntity <|-- ProjectMember
    BaseEntity <|-- Task
    BaseEntity <|-- Comment

    Project "1" --> "1" User : owned by
    ProjectMember "many" --> "1" Project : belongs to
    ProjectMember "many" --> "1" User : member
    Task "many" --> "1" Project : belongs to
    Task "many" --> "0..1" User : assignee
    Task "many" --> "1" User : created by
    Task "many" --> "0..1" Task : parent
    Comment "many" --> "1" Task : on
    Comment "many" --> "1" User : written by
    AuditLog "many" --> "1" User : performed by

    Task --> TaskPriority : uses
    Task --> TaskStatus : uses
    ProjectMember --> ProjectRole : uses
    AuditLog --> AuditAction : uses
    AuditLog --> EntityType : uses
```

---

## 7. API Design

> **Base URL:** `/api/v1`
> **Auth:** All protected routes require `Authorization: Bearer <accessToken>`

---

### 7.1 Authentication

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/auth/register` | None | Register new user |
| `POST` | `/auth/login` | None | Login and get tokens |
| `POST` | `/auth/refresh` | None | Refresh access token |

#### POST `/auth/register`
```json
{
  "name": "string",
  "email": "string",
  "password": "string"
}
```
**Response `201`**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer"
}
```

#### POST `/auth/login`
```json
{
  "email": "string",
  "password": "string"
}
```
**Response `200`**
```json
{
  "accessToken": "string",
  "refreshToken": "string",
  "tokenType": "Bearer"
}
```

#### POST `/auth/refresh`
```json
"your_refresh_token"
```
**Response `200`**
```json
{
  "accessToken": "string",
  "refreshToken": "string"
}
```

---

### 7.2 User

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/users/profile` | Bearer Token | Get own profile |
| `PATCH` | `/users/profile` | Bearer Token | Update own profile |

#### GET `/users/profile`
**Response `200`**
```json
{
  "id": 1,
  "name": "string",
  "email": "string",
  "createdAt": "datetime"
}
```

#### PATCH `/users/profile`
```json
{
  "name": "string",
  "password": "string"
}
```
**Response `200`**
```json
{
  "id": 1,
  "name": "string",
  "email": "string",
  "updatedAt": "datetime"
}
```

---

### 7.3 Project

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/projects` | Bearer Token | Create project |
| `GET` | `/projects` | Bearer Token | Get all projects |
| `GET` | `/projects/{projectId}` | Bearer Token | Get project by ID |
| `PATCH` | `/projects/{projectId}` | Bearer Token (OWNER) | Update project |
| `DELETE` | `/projects/{projectId}` | Bearer Token (OWNER) | Delete project |
| `POST` | `/projects/{projectId}/members` | Bearer Token (OWNER) | Add member |
| `PATCH` | `/projects/{projectId}/members/{memberId}` | Bearer Token (OWNER) | Update member role |
| `DELETE` | `/projects/{projectId}/members/{memberId}` | Bearer Token (OWNER) | Remove member |

#### POST `/projects`
```json
{
  "name": "string",
  "description": "string"
}
```
**Response `201`**
```json
{
  "id": 1,
  "name": "string",
  "description": "string",
  "owner": {
    "id": 1,
    "name": "string",
    "email": "string"
  },
  "createdAt": "datetime"
}
```

#### POST `/projects/{projectId}/members`
```json
{
  "userId": 2,
  "role": "MEMBER"
}
```
**Response `201`**

#### PATCH `/projects/{projectId}/members/{memberId}`
```json
{
  "role": "OWNER"
}
```
**Response `200`**

---

### 7.4 Task

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/projects/{projectId}/tasks` | Bearer Token | Create task |
| `GET` | `/projects/{projectId}/tasks` | Bearer Token | Get all tasks |
| `GET` | `/projects/{projectId}/tasks/{taskId}` | Bearer Token | Get task by ID |
| `PATCH` | `/projects/{projectId}/tasks/{taskId}` | Bearer Token | Update task |
| `PATCH` | `/projects/{projectId}/tasks/{taskId}/status` | Bearer Token | Update task status |
| `DELETE` | `/projects/{projectId}/tasks/{taskId}` | Bearer Token (OWNER) | Delete task |

#### POST `/projects/{projectId}/tasks`
```json
{
  "title": "string",
  "description": "string",
  "priority": "HIGH",
  "status": "TODO",
  "startDate": "2026-06-01",
  "dueDate": "2026-06-15",
  "assigneeId": 1
}
```
**Response `201`**
```json
{
  "id": 1,
  "title": "string",
  "description": "string",
  "priority": "HIGH",
  "status": "TODO",
  "startDate": "2026-06-01",
  "dueDate": "2026-06-15",
  "assigneeId": 1,
  "assigneeName": "string",
  "projectId": 1,
  "createdBy": 1,
  "subTasks": [],
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

#### GET `/projects/{projectId}/tasks`
```
Query params:
?page=0&size=10
?sortBy=createdAt&order=desc
?status=TODO
?priority=HIGH
?assigneeId=1
```

#### PATCH `/projects/{projectId}/tasks/{taskId}/status`
```json
{
  "status": "IN_PROGRESS"
}
```
> Status transitions: `TODO` → `IN_PROGRESS` → `IN_REVIEW` → `DONE`

---

### 7.5 Subtask

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/projects/{projectId}/tasks/{taskId}/subtasks` | Bearer Token | Create subtask |
| `GET` | `/projects/{projectId}/tasks/{taskId}/subtasks` | Bearer Token | Get all subtasks |
| `PATCH` | `/projects/{projectId}/tasks/{taskId}/subtasks/{subtaskId}` | Bearer Token | Update subtask |
| `DELETE` | `/projects/{projectId}/tasks/{taskId}/subtasks/{subtaskId}` | Bearer Token (OWNER) | Delete subtask |

#### POST `/projects/{projectId}/tasks/{taskId}/subtasks`
```json
{
  "title": "string",
  "description": "string",
  "priority": "MEDIUM",
  "status": "TODO",
  "startDate": "2026-06-01",
  "dueDate": "2026-06-10",
  "assigneeId": 1
}
```
**Response `201`**

---

### 7.6 Comments

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/projects/{projectId}/tasks/{taskId}/comments` | Bearer Token | Add comment |
| `GET` | `/projects/{projectId}/tasks/{taskId}/comments` | Bearer Token | Get all comments |
| `PATCH` | `/projects/{projectId}/tasks/{taskId}/comments/{commentId}` | Bearer Token (Author) | Update comment |
| `DELETE` | `/projects/{projectId}/tasks/{taskId}/comments/{commentId}` | Bearer Token (Author) | Delete comment |

#### POST `/projects/{projectId}/tasks/{taskId}/comments`
```json
{
  "content": "string"
}
```
**Response `201`**
```json
{
  "id": 1,
  "content": "string",
  "authorId": 1,
  "authorName": "string",
  "taskId": 1,
  "createdAt": "datetime",
  "updatedAt": "datetime"
}
```

---

### 7.7 Audit Log

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `GET` | `/audit-logs` | Bearer Token | Get all audit logs |
| `GET` | `/audit-logs/{entityType}/{entityId}` | Bearer Token | Get logs by entity |

#### GET `/audit-logs`
```
Query params:
?entityType=TASK
?action=UPDATE
?performedBy=1
?page=0&size=10
```
**Response `200`**
```json
{
  "content": [
    {
      "id": 1,
      "entityType": "TASK",
      "entityId": 1,
      "action": "UPDATE",
      "changedFields": "{\"title\":{\"old\":\"old title\",\"new\":\"new title\"}}",
      "performedBy": 1,
      "performedAt": "datetime"
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "page": 0
}
```

#### GET `/audit-logs/{entityType}/{entityId}`
```
Example: /api/v1/audit-logs/TASK/1
         /api/v1/audit-logs/PROJECT/1
         /api/v1/audit-logs/COMMENT/1
```

---

## 8. Getting Started

### 8.1 Prerequisites

Make sure you have the following installed:

- [Java 25](https://openjdk.org/)
- [PostgreSQL 16](https://www.postgresql.org/)
- [Gradle 8.x](https://gradle.org/)
- [Git](https://git-scm.com/)

---

### 8.2 Installation

**1. Clone the repository**
```bash
git clone https://github.com/yourusername/taskflow.git
cd taskflow
```

**2. Create PostgreSQL database**
```sql
CREATE DATABASE taskflow;
```

**3. Configure environment**

Create `application-local.properties` in `src/main/resources/`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/taskflow
spring.datasource.username=your_db_username
spring.datasource.password=your_db_password

jwt.secret=your_jwt_secret_minimum_32_characters
jwt.expiration.access=900000
jwt.expiration.refresh=604800000
```

---

### 8.3 Running the Application

**Using Gradle:**
```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

**Using IntelliJ:**
- Open Run/Debug Configurations
- Set Active Profiles to `local`
- Click Run

**Application runs on:**
```
http://localhost:8080/api/v1
```

---

## 9. Project Structure

```
src/main/java/com/taskflow/
├── audit/
│   ├── AuditLog.java
│   ├── AuditLogRepository.java
│   ├── AuditLogService.java
│   ├── AuditLogController.java
│   └── dto/
│       └── AuditLogResponseDto.java
├── comment/
│   ├── Comment.java
│   ├── CommentRepository.java
│   ├── CommentMapper.java
│   ├── CommentService.java
│   ├── CommentController.java
│   └── dto/
│       ├── CommentRequestDto.java
│       └── CommentResponseDto.java
├── common/
│   ├── BaseEntity.java
│   ├── JsonUtil.java
│   ├── enums/
│   │   ├── AuditAction.java
│   │   ├── EntityType.java
│   │   ├── ProjectRole.java
│   │   ├── TaskPriority.java
│   │   └── TaskStatus.java
│   └── exception/
│       ├── AppException.java
│       └── GlobalExceptionHandler.java
├── project/
│   ├── Project.java
│   ├── ProjectMember.java
│   ├── ProjectRepository.java
│   ├── ProjectMemberRepository.java
│   ├── ProjectMapper.java
│   ├── ProjectService.java
│   ├── ProjectController.java
│   └── dto/
│       ├── MemberRequestDto.java
│       ├── MemberUpdateDto.java
│       ├── OwnerDto.java
│       ├── ProjectRequestDto.java
│       ├── ProjectResponseDto.java
│       └── ProjectUpdateDto.java
├── security/
│   ├── JwtFilter.java
│   ├── JwtUtil.java
│   ├── SecurityConfig.java
│   └── UserDetailsServiceImpl.java
├── task/
│   ├── Task.java
│   ├── TaskRepository.java
│   ├── TaskMapper.java
│   ├── TaskService.java
│   ├── TaskController.java
│   └── dto/
│       ├── TaskRequestDto.java
│       ├── TaskResponseDto.java
│       ├── TaskStatusUpdateDto.java
│       └── TaskUpdateDto.java
├── user/
│   ├── User.java
│   ├── UserRepository.java
│   ├── UserMapper.java
│   ├── UserService.java
│   ├── UserController.java
│   ├── AuthService.java
│   ├── AuthController.java
│   └── dto/
│       ├── AuthResponseDto.java
│       ├── LoginRequestDto.java
│       ├── RegisterRequestDto.java
│       ├── UserResponseDto.java
│       └── UserUpdateDto.java
└── TaskflowApplication.java

src/main/resources/
├── application.properties
└── application-local.properties  ← not committed to Git
```

---

## License

This project is licensed under the MIT License.