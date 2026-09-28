# QuizWiz - Simple Online Quiz Conducting System

A lightweight, beginner-friendly online quiz application built using **Spring Boot**, **Spring Data JPA**, **MySQL**, and a clean **HTML/CSS/JavaScript frontend** served directly by Spring Boot on `http://localhost:8080`.

---

## Key Features

1. **Faculty Quiz Creation**:
   - Faculty can create quizzes with titles, descriptions, time limits (in minutes), and multiple-choice questions (MCQs) with options A, B, C, D and the correct option.
2. **Student Quiz Attempt with Live Timer**:
   - Students enter their Roll Number and select an available quiz.
   - A real-time countdown timer automatically ticks down and auto-submits when time expires.
3. **Instant Auto-Scoring**:
   - Calculates the score immediately upon submission.
   - Enforces the rule: **Scores are computed only from questions actually answered**.
   - Submissions past the time limit are marked as `TIME_EXPIRED` with score 0.
4. **Duplicate Attempt Prevention**:
   - Enforces the business rule: **Prevent a student from attempting the same quiz twice**. Rejects duplicate attempts with an HTTP 409 Conflict and a clear error message.
5. **Class-Wise Score Report**:
   - Faculty can view a complete breakdown of all student attempts for any quiz, ranked by score (Roll Number, Name, Email, Score, Percentage, Status, and Submission Time).
6. **Background Auto-Close Scheduler**:
   - A scheduled task running every 60 seconds scans for abandoned `IN_PROGRESS` attempts past their quiz duration and automatically finalizes them as `TIME_EXPIRED`.
7. **Secure Answers**:
   - Correct options are marked `WRITE_ONLY`, meaning students cannot inspect network payloads or JSON responses to cheat.

---

## Tech Stack and Database

- **Backend**: Spring Boot 3.4.3, Java 21, Spring Data JPA, Hibernate Validator
- **Database**: MySQL Server (`quizwiz` database)
- **Frontend**: Clean Vanilla HTML5, CSS3, JavaScript (served from `src/main/resources/static`)
- **Port**: `8080` (accessible at `http://localhost:8080`)

---

## Database Design (Entities)

The system uses 4 core JPA entities:

1. **`Quiz`** (`src/main/java/com/quizwiz/entity/Quiz.java`)
   - `id` (Primary Key)
   - `title`, `description`, `timeLimitMinutes`
   - `questions` (One-to-Many relationship with `Question`)

2. **`Question`** (`src/main/java/com/quizwiz/entity/Question.java`)
   - `id` (Primary Key)
   - `questionText`
   - `optionA`, `optionB`, `optionC`, `optionD`
   - `correctOption` ("A", "B", "C", or "D", marked `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`)
   - `quiz` (Many-to-One relationship with `Quiz`)

3. **`Student`** (`src/main/java/com/quizwiz/entity/Student.java`)
   - `id` (Primary Key)
   - `rollNumber` (Unique constraint)
   - `name`, `email`

4. **`Attempt`** (`src/main/java/com/quizwiz/entity/Attempt.java`)
   - `id` (Primary Key)
   - `student` (Many-to-One with `Student`)
   - `quiz` (Many-to-One with `Quiz`)
   - `score`, `totalQuestions`, `status` ("IN_PROGRESS", "SUBMITTED", "TIME_EXPIRED")
   - `startTime`, `submissionTime`
   - **Unique Constraint**: `(student_id, quiz_id)` ensures a student cannot have multiple attempts for the same quiz.

---

## How to Run the Application

### 1. Ensure MySQL is running
The application connects to MySQL with:
- URL: `jdbc:mysql://localhost:3306/quizwiz`
- Username: `root`
- Password: *(leave blank for default XAMPP setup, or update `application.properties` with your password)*

### 2. Start Spring Boot
In Command Prompt:
```cmd
cd /d E:\1-PROJECT
run.bat
```
*Note: `run.bat` defaults to `C:\Program Files\Java\jdk-21`. If your JDK is installed in a different folder, update the `JAVA_HOME` line in `run.bat`.*

Or run with Maven Wrapper:
```cmd
mvnw.cmd spring-boot:run
```

### 3. Open in Browser
Open your browser and navigate to:
```
http://localhost:8080
```

---

## REST API Endpoints (For Postman / Swagger)

| Method | Endpoint | Description | Status Code |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/quizzes` | Fetch all available quizzes (answers hidden) | 200 OK |
| `GET` | `/api/quizzes/{id}` | Fetch a single quiz by ID | 200 OK |
| `POST` | `/api/quizzes` | Faculty creates a new quiz with questions | 201 Created |
| `POST` | `/api/attempts/start` | Student starts attempt (normalizes roll, validates name, rejects duplicates) | 201 Created / 409 Conflict |
| `POST` | `/api/attempts/submit` | Student submits answers (auto-scores & validates time) | 200 OK |
| `GET` | `/api/attempts/{id}` | Get attempt details with server-computed remaining seconds (session recovery) | 200 OK |
| `GET` | `/api/reports/quiz/{quizId}` | Class-wise score report (completed attempts, score desc, roll asc) | 200 OK |
| `GET` | `/api/students` | Get all registered students | 200 OK |
| `GET` | `/api/dashboard/stats` | Aggregate dashboard statistics | 200 OK |

---

## Viva & Project Defense Prep

When presenting this project for an evaluation:
1. **Authentication Scope**:
   - The roll number acts as the student's unique identifier for quick classroom quizzes. In production, OAuth2 or Spring Security JWT can be layered on top.
   - To prevent impersonation, if an existing roll number is used with a different name, the system rejects it with an HTTP 400 error.
2. **Timer & Auto-Close Logic**:
   - The frontend timer is synchronized with the server's start time and duration via `remainingSeconds`.
   - If a student refreshes their browser during an active attempt, `sessionStorage` and `GET /api/attempts/{id}` seamlessly restore their attempt with the correct remaining time instead of restarting or locking them out.
   - A Spring `@Scheduled` background worker independently runs every 60 seconds to close abandoned `IN_PROGRESS` attempts whose elapsed minutes exceed the quiz time limit.
   - If an attempt is closed by the scheduler due to abandonment or submitted past the deadline, it receives a score of 0.
3. **Class Grouping & Score Reports**:
   - "Class-wise" reporting is organized per quiz. Adding a `section` or `batch` column to `Student` is a straightforward extension for multi-section departments.
   - Score reports only display completed attempts (excluding premature in-progress 0s) and use student roll number as a secondary tie-breaker.
4. **Input Integrity & Error Handling**:
   - Roll numbers are normalized by stripping whitespace and non-alphanumeric characters (e.g. `CS 101` and `CS-101` map to `CS101`).
   - Submissions validate that all question IDs belong strictly to the quiz being attempted and that choices conform to A, B, C, or D.
   - All standard error cases return structured JSON error payloads with correct HTTP status codes (400 for validation or type mismatch, 404 for unknown endpoints, 405 for unsupported HTTP methods, 409 for duplicate attempts, and sanitized 500 without internal SQL leakage).
