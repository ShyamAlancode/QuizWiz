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
4. **Duplicate Attempt Prevention**:
   - Enforces the business rule: **Prevent a student from attempting the same quiz twice**. Rejects duplicate attempts with an HTTP 400 Bad Request and a clear error message.
5. **Class-Wise Score Report**:
   - Faculty can view a complete breakdown of all student attempts for any quiz, ranked by score (Roll Number, Name, Email, Score, Percentage, Status, and Submission Time).
6. **Dashboard Overview**:
   - Shows live counts of Total Quizzes, Registered Students, and Total Attempts.

---

## Tech Stack and Database

- **Backend**: Spring Boot 3.4.3, Java 21, Spring Data JPA, Hibernate Validator
- **Database**: MySQL Server (`quizwiz` database)
- **Frontend**: Clean Vanilla HTML5, CSS3, JavaScript (served from `src/main/resources/static`)
- **Port**: `8080` (accessible at `http://localhost:8080`)

---

## Database Design (Entities)

The system uses 4 core JPA entities:

1. **`Quiz`** ([Quiz.java](file:///e:/1-PROJECT/src/main/java/com/quizwiz/entity/Quiz.java))
   - `id` (Primary Key)
   - `title`, `description`, `timeLimitMinutes`
   - `questions` (One-to-Many relationship with `Question`)

2. **`Question`** ([Question.java](file:///e:/1-PROJECT/src/main/java/com/quizwiz/entity/Question.java))
   - `id` (Primary Key)
   - `questionText`
   - `optionA`, `optionB`, `optionC`, `optionD`
   - `correctOption` ("A", "B", "C", or "D")
   - `quiz` (Many-to-One relationship with `Quiz`)

3. **`Student`** ([Student.java](file:///e:/1-PROJECT/src/main/java/com/quizwiz/entity/Student.java))
   - `id` (Primary Key)
   - `rollNumber` (Unique constraint)
   - `name`, `email`

4. **`Attempt`** ([Attempt.java](file:///e:/1-PROJECT/src/main/java/com/quizwiz/entity/Attempt.java))
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
- Password: *(empty by default, modify in `application.properties` if needed)*

### 2. Start Spring Boot
In the terminal, run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
.\mvnw.cmd spring-boot:run
```
Or execute the packaged JAR:
```powershell
java -jar target/quizwiz-0.0.1-SNAPSHOT.jar
```

### 3. Open in Browser
Open your browser and navigate to:
```
http://localhost:8080
```

---

## REST API Endpoints (For Postman / Swagger)

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/quizzes` | Fetch all available quizzes |
| `GET` | `/api/quizzes/{id}` | Fetch a single quiz by ID |
| `POST` | `/api/quizzes` | Faculty creates a new quiz with questions |
| `POST` | `/api/attempts/start` | Student starts a quiz attempt (validates duplicates) |
| `POST` | `/api/attempts/submit` | Student submits answers (auto-scores & validates time) |
| `GET` | `/api/reports/quiz/{quizId}` | Class-wise score report for a specific quiz |
| `GET` | `/api/dashboard/stats` | Aggregate dashboard statistics |

### Example Payloads:

#### 1. Faculty Creates Quiz (`POST /api/quizzes`)
```json
{
  "title": "Computer Networks Basics",
  "description": "OSI Model and TCP/IP",
  "timeLimitMinutes": 5,
  "questions": [
    {
      "questionText": "Which OSI layer is responsible for routing packets?",
      "optionA": "Data Link Layer",
      "optionB": "Network Layer",
      "optionC": "Transport Layer",
      "optionD": "Session Layer",
      "correctOption": "B"
    }
  ]
}
```

#### 2. Student Starts Attempt (`POST /api/attempts/start`)
```json
{
  "quizId": 1,
  "rollNumber": "CS2026-10",
  "name": "Aarav Sharma",
  "email": "aarav@college.edu"
}
```

#### 3. Student Submits Attempt (`POST /api/attempts/submit`)
```json
{
  "attemptId": 1,
  "answers": {
    "1": "B"
  }
}
```
