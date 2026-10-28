# Lab Resource Troubleshooting System

## 1. Project Overview & Specification

The **Lab Resource Troubleshooting System** is a Spring Boot & PostgreSQL web application for managing hardware, software, and connectivity complaints in college computer laboratories. Students submit complaints regarding specific equipment (e.g. PC-12) in a selected lab. Complaints are automatically routed to the technician assigned to that lab. Technicians update complaint progress, triggering automated email notifications to the reporting student. Administrators manage lab configurations, technician assignments, user accounts, and view analytical dashboards detailing lab status and issue trends.

### Core Objectives
- Modular, beginner-friendly Spring Boot architecture following standard MVC patterns.
- Clear separation of concerns between Controllers, Services, Repositories, and Entities.
- Strict role-based access control for Students, Technicians, and Admins.
- Automated technician assignment at complaint creation time.
- Robust status lifecycle tracking and email notifications.

---

## 2. Roles and Authorization Rules

The system recognizes three roles with strict boundaries:

| Action / Resource | Student | Technician | Admin |
|---|---|---|---|
| User Auth (Login/Logout/Me) | Yes | Yes | Yes |
| Submit Complaint | Yes | No | No |
| View Own Complaints | Yes | No | No |
| View Assigned Complaints | No | Yes | No |
| Update Complaint Status (`IN_PROGRESS`, `COMPLETED`) & Notes | No | Yes (Assigned complaints only) | No |
| View All Complaints | No | No | Yes |
| Manage Labs (Create/Edit/Deactivate) | No | No | Yes |
| Manage Technician Accounts (Create/Edit/Deactivate) | No | No | Yes |
| Assign Technician to Lab | No | No | Yes |
| View Dashboard Analytics & Summary Cards | No | No | Yes |

### Detailed Authorization Policies
- **Student Policy**: Can only submit complaints and view complaints where `student_id` matches the authenticated student's ID.
- **Technician Policy**: Can only view and update complaints where `technician_id` matches the authenticated technician's ID. A technician cannot alter complaints assigned to other technicians.
- **Admin Policy**: Full administrative control over lab entities, user management, and aggregate reporting metrics.

---

## 3. Assignment Rules & Complaint Workflows

### 3.1 Automatic Assignment Rule
1. When a student submits a complaint, the system looks up the target `Lab`.
2. The system checks the `assigned_technician_id` attached to the `Lab`.
3. If an active technician is assigned to the lab:
   - The technician's ID is copied directly to the `complaint.technician_id` field.
   - Initial complaint status is set to `PENDING`.
4. If no active technician is currently assigned to the selected lab:
   - Complaint submission is rejected with a clear business error ("No active technician assigned to this lab. Please contact the administrator.").
   - Unassigned complaints are prevented at entry.
5. **Snapshot Assignment**: The `technician_id` recorded on the complaint is fixed at creation time. If an Admin subsequently changes the lab's assigned technician, historical complaints remain assigned to their original technician.

### 3.2 Complaint Status Lifecycle
```
[PENDING] ---> [IN_PROGRESS] ---> [COMPLETED]
```
- **PENDING**: Default status assigned on submission.
- **IN_PROGRESS**: Set by the assigned technician when work commences.
- **COMPLETED**: Set by the assigned technician when the issue is resolved.

### 3.3 Email Notification Rules
- Whenever a complaint's status changes (e.g. `PENDING` -> `IN_PROGRESS` or `IN_PROGRESS` -> `COMPLETED`), an email is dispatched to the reporting student's registered email address.
- Email contents: Complaint ID, Lab Name, Resource Number, Complaint Type, New Status, and Timestamp.
- **Error Handling**: Mail delivery failures are logged gracefully without rolling back DB status changes or throwing unhandled HTTP 500 errors to the technician.

---

## 4. Database Schema Design (PostgreSQL)

The system utilizes three core relational tables:

### 4.1 Table: `users`
Stores user credentials and roles.

```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL CHECK (role IN ('STUDENT', 'TECHNICIAN', 'ADMIN')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
```

### 4.2 Table: `labs`
Stores lab details and single technician assignment.

```sql
CREATE TABLE labs (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(150),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_technician_id BIGINT REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX idx_labs_technician ON labs(assigned_technician_id);
```

### 4.3 Table: `complaints`
Stores student complaints and tracking history.

```sql
CREATE TABLE complaints (
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES users(id),
    lab_id BIGINT NOT NULL REFERENCES labs(id),
    technician_id BIGINT REFERENCES users(id),
    resource_number VARCHAR(50) NOT NULL,
    complaint_type VARCHAR(30) NOT NULL,
    description TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED')),
    technician_note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_complaints_student ON complaints(student_id);
CREATE INDEX idx_complaints_lab ON complaints(lab_id);
CREATE INDEX idx_complaints_technician ON complaints(technician_id);
CREATE INDEX idx_complaints_status ON complaints(status);
```

---

## 5. Planned API Endpoints

### 5.1 Authentication API (`/api/auth`)
- `POST /api/auth/register` - Student registration.
- `POST /api/auth/login` - User login & session/JWT issue.
- `GET /api/auth/me` - Fetch current user profile.

### 5.2 Admin API (`/api/admin`)
- `GET /api/admin/labs` - List all labs.
- `POST /api/admin/labs` - Create lab.
- `PUT /api/admin/labs/{id}` - Update lab details & assigned technician.
- `PATCH /api/admin/labs/{id}/deactivate` - Deactivate lab.
- `GET /api/admin/technicians` - List all technician accounts.
- `POST /api/admin/technicians` - Create technician account.
- `PATCH /api/admin/technicians/{id}/deactivate` - Deactivate technician account.
- `GET /api/admin/complaints` - View all complaints with filtering.
- `GET /api/admin/dashboard` - Get analytics data (summary counts & chart aggregations).

### 5.3 Student API (`/api/student`)
- `GET /api/student/labs` - List active labs for dropdown selection.
- `POST /api/student/complaints` - Submit new complaint.
- `GET /api/student/complaints` - View student's submitted complaints.

### 5.4 Technician API (`/api/technician`)
- `GET /api/technician/complaints` - List complaints assigned to authenticated technician.
- `GET /api/technician/complaints/{id}` - View details of an assigned complaint.
- `PATCH /api/technician/complaints/{id}/status` - Update status and add technician note.

---

## 6. Project Architecture & Stack

- **Language**: Java 17
- **Framework**: Spring Boot 4.1.1 (Spring 7.0.9)
- **Build System**: Gradle 9.7.1
- **Database**: PostgreSQL
- **Security**: Spring Security & Password Hashing (BCrypt)
- **Email**: Spring Boot Starter Mail (`JavaMailSender`)
- **Utilities**: Lombok, Jakarta Validation

---

## 7. Implementation Stages & Progress Checklist

### Progress Checklist

- [x] **Stage 0: Project Inspection & Master Plan Setup**
  - [x] Inspect existing initializr structure, dependencies, and configuration.
  - [x] Document project specification, database design, authorization rules, and assignment logic.
  - [x] Establish master plan and build verification strategy in `project.md`.

- [x] **Stage 1: PostgreSQL Configuration & Application Foundation**
  - [x] Create database `lab_resource_db` in PostgreSQL.
  - [x] Configure environment variables and placeholders in `application.properties`.
  - [x] Setup JPA Hibernate `ddl-auto` for local development.
  - [x] Verify HikariCP connection during `./gradlew build`.


- [x] **Stage 2: Core Domain Entities, Enums & Repositories**
  - [x] Implement `Role`, `ComplaintStatus`, and `ComplaintType` enums.
  - [x] Implement `User`, `Lab`, and `Complaint` JPA entities with relationships & constraints.
  - [x] Implement `UserRepository`, `LabRepository`, and `ComplaintRepository`.
  - [x] Verify database table creation in PostgreSQL (`users`, `labs`, `complaints`).


- [x] **Stage 3: Authentication & Role-Based Security**
  - [x] Configure Spring Security filter chain, `BCryptPasswordEncoder`, and session management.
  - [x] Implement `CustomUserDetailsService` and `CustomUserDetails`.
  - [x] Implement Auth API (`/api/auth/register`, `/api/auth/login`, `/api/auth/me`, `/api/auth/logout`).
  - [x] Enforce role protection (`/api/admin/**`, `/api/technician/**`, `/api/student/**`).
  - [x] Implement safe `DataInitializer` for initial admin account setup.
  - [x] Write `AuthIntegrationTest` verifying login, registration, password hashing, and role security.


- [x] **Stage 4: Admin Lab & Technician Management**
  - [x] Implement DTOs: `LabRequestDto`, `LabResponseDto`, `CreateTechnicianRequestDto`, `UpdateTechnicianRequestDto`, `AssignTechnicianRequestDto`.
  - [x] Implement `LabService` with duplicate validation, technician assignment constraints, and status updates.
  - [x] Implement `TechnicianService` for technician provisioning and account management.
  - [x] Implement `AdminLabController` and `AdminTechnicianController` under `/api/admin/**`.
  - [x] Implement `GlobalExceptionHandler` for consistent REST error responses (400, 404, etc.).
  - [x] Enforce validation preventing assignment of non-technicians or inactive technicians.
  - [x] Enforce isolation ensuring changing lab technician does not alter existing complaints.
  - [x] Write `AdminManagementIntegrationTest` verifying permissions, assignment validation, and lab management.

- [x] **Stage 5: Complete Complaint Workflow**
  - [x] Implement DTOs: `CreateComplaintRequestDto`, `ComplaintResponseDto`, `UpdateComplaintStatusRequestDto`, `PageResponseDto`.
  - [x] Implement `ComplaintService` with student principal binding, snapshot technician capture, ownership isolation, and status transition rules.
  - [x] Implement `StudentComplaintController` (`/api/student/complaints` and `/api/student/labs`).
  - [x] Implement `TechnicianComplaintController` (`/api/technician/complaints`).
  - [x] Implement `AdminComplaintController` (`/api/admin/complaints`) with filtering and pagination.
  - [x] Enforce backend security checks preventing cross-user complaint access and ID guessing.
  - [x] Enforce status transition restrictions (`PENDING -> IN_PROGRESS / COMPLETED`, reject repeating status, reject modifying completed).
  - [x] Write `ComplaintWorkflowIntegrationTest` covering assignment capture, ownership, status validation, and pagination.

- [x] **Stage 6: Status-Change Email Notifications**
  - [x] Configure mail properties and environment variable placeholders in `application.properties`.
  - [x] Implement `EmailService` using `JavaMailSender` with safe exception handling.
  - [x] Integrate email notification into `ComplaintService.updateComplaintStatus`.
  - [x] Guarantee status changes are preserved even when mail delivery fails.
  - [x] Write `StatusChangeEmailIntegrationTest` verifying email triggers, zero emails on unchanged status, and mail failure resilience.


- [x] **Stage 7: Admin Dashboard & Analytics**
  - [x] Implement DTOs: `DashboardSummaryDto`, `ChartDataDto`, `LabSummaryDto`.
  - [x] Implement database aggregate queries in `LabRepository` and `ComplaintRepository` without loading full dataset into memory.
  - [x] Implement `DashboardService` aggregating total, pending, in-progress, completed counts, unassigned labs, and grouped charts.
  - [x] Ensure labs with zero complaints are included in per-lab aggregation via SQL LEFT JOIN.
  - [x] Implement `AdminDashboardController` (`/api/admin/dashboard`) with strict admin authorization.
  - [x] Write `AdminDashboardIntegrationTest` verifying empty datasets, mixed statuses, grouping accuracy, zero-complaint labs, and role permissions.


- [x] **Stage 8: Integration Testing, Security Review & Cleanup**
  - [x] Implement comprehensive `FullWorkflowEndToEndIntegrationTest` verifying student submission, snapshot capture, reassignment isolation, technician transitions, email delivery, and admin dashboard reporting.
  - [x] Conduct security review: zero password leaks, BCrypt hashing verified, no hardcoded production secrets, strict role-based access rules.
  - [x] Ensure transaction isolation and rollback for automated tests.
  - [x] Run full automated test suite: 35 passing tests across 7 test suites with 0 failures.
  - [x] Build passing clean (`.\gradlew.bat build` -> `BUILD SUCCESSFUL`).


---

## 8. Verification, Security Audit & Test Summary

### 8.1 Exact Commands to Build and Run
- **Compile Java**:
  ```powershell
  .\gradlew.bat compileJava
  ```
- **Execute Full Test Suite**:
  ```powershell
  .\gradlew.bat test
  ```
- **Build Bootable JAR**:
  ```powershell
  .\gradlew.bat build
  ```
- **Run Spring Boot Application**:
  ```powershell
  .\gradlew.bat bootRun
  ```

### 8.2 Automated Test Execution Results
All 35 tests across 7 test suites pass with 0 failures and 0 errors:

| Test Class | Tests | Status | Execution Scope |
|---|---|---|---|
| `AuthIntegrationTest` | 7 | Passed | Password hashing, student registration role forcing, login success/failure, role restrictions |
| `AdminManagementIntegrationTest` | 11 | Passed | Admin permissions, lab CRUD, duplicate checks, technician provisioning, assignment constraints, isolation |
| `ComplaintWorkflowIntegrationTest` | 7 | Passed | Student submission, snapshot capture, unassigned lab handling, student privacy, technician isolation, status transitions |
| `StatusChangeEmailIntegrationTest` | 3 | Passed | Single email per status change, zero emails on unchanged status, resilience to SMTP failures |
| `AdminDashboardIntegrationTest` | 5 | Passed | Dashboard security, empty datasets, mixed statuses, zero-complaint labs representation, unassigned lab warnings |
| `FullWorkflowEndToEndIntegrationTest` | 1 | Passed | End-to-end multi-user journey from admin setup to student submission, technician resolution, and dashboard reflection |
| `LabResourseTroubleshootingApplicationTests` | 1 | Passed | Spring application context loading and datasource initialization |
| **Total** | **35** | **100% Passed** | **0 Failures, 0 Errors** |

### 8.3 Security Audit Findings
1. **Password Protection**:
   - Passwords hashed using `BCryptPasswordEncoder`.
   - Raw passwords and password hashes are never exposed in any DTO response.
2. **Authorization Enforcement**:
   - Public registration strictly forces `STUDENT` role; privilege escalation to `ADMIN` or `TECHNICIAN` is impossible.
   - Ordinary students and technicians cannot access `/api/admin/**`.
   - Students cannot view other students' complaints (`403 Forbidden`).
   - Technicians cannot view or update complaints assigned to other technicians (`403 Forbidden`).
3. **Data Integrity & Immutability**:
   - Reassigning a lab to a new technician preserves the historical technician snapshot on all pre-existing complaints.
   - Repeating current status or moving status back to `PENDING` is rejected (`400 Bad Request`).
   - Completed complaints cannot be modified.
4. **Resilience**:
   - Mail server delivery failures are safely trapped and logged; database status updates remain committed.

### 8.4 Code Simplification & Human-Centric Style
The codebase was refactored to look natural, simple, and maintainabl):e (avoiding overly complex/AI-heavy annotations
- **Authentication**: Kept very simple and intuitive in `AuthService` — direct user lookup by email, standard `passwordEncoder.matches(...)` check, and simple session context binding. Removed complex `AuthenticationManager`, `DaoAuthenticationProvider`, and custom redirect handler beans.
- **Controllers & Services**: Eliminated Lombok's `@RequiredArgsConstructor` and `@Slf4j` across all controllers, services, and configs in favor of standard Java constructors with constructor injection.
- **Entities & DTOs**: Removed unnecessary table index clutter, complex builder chains, and stream operations, using standard Java loops, constructors, and clear getter/setter patterns.
- **Test Compatibility**: All 35 tests across 7 test suites continue to pass 100% without issues.

