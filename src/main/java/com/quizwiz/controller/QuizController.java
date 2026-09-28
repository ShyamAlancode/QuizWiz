package com.quizwiz.controller;

import com.quizwiz.dto.*;
import com.quizwiz.entity.Attempt;
import com.quizwiz.entity.Quiz;
import com.quizwiz.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // Allows easy testing from frontend / Swagger / Postman
public class QuizController {

    private final QuizService quizService;

    public QuizController(QuizService quizService) {
        this.quizService = quizService;
    }

    // 1. Faculty creates a new quiz
    @PostMapping("/quizzes")
    public ResponseEntity<Quiz> createQuiz(@Valid @RequestBody CreateQuizRequest request) {
        Quiz createdQuiz = quizService.createQuiz(request);
        return new ResponseEntity<>(createdQuiz, HttpStatus.CREATED);
    }

    // 2. Get all available quizzes
    @GetMapping("/quizzes")
    public ResponseEntity<List<Quiz>> getAllQuizzes() {
        return ResponseEntity.ok(quizService.getAllQuizzes());
    }

    // 3. Get quiz details by ID
    @GetMapping("/quizzes/{id}")
    public ResponseEntity<Quiz> getQuizById(@PathVariable Long id) {
        return ResponseEntity.ok(quizService.getQuizById(id));
    }

    // 4. Student starts an attempt (Validates duplicate attempts)
    @PostMapping("/attempts/start")
    public ResponseEntity<Attempt> startAttempt(@Valid @RequestBody StartAttemptRequest request) {
        Attempt attempt = quizService.startAttempt(request);
        return new ResponseEntity<>(attempt, HttpStatus.CREATED);
    }

    // 5. Student submits answers (Auto-scores attempt & validates time limit)
    @PostMapping("/attempts/submit")
    public ResponseEntity<Attempt> submitAttempt(@Valid @RequestBody SubmitAttemptRequest request) {
        Attempt attempt = quizService.submitAttempt(request);
        return ResponseEntity.ok(attempt);
    }

    // 6. Faculty views class-wise score report for a quiz
    @GetMapping("/reports/quiz/{quizId}")
    public ResponseEntity<List<ScoreReportDto>> getScoreReport(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.getScoreReportForQuiz(quizId));
    }

    // 7. Dashboard overview stats
    @GetMapping("/dashboard/stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        return ResponseEntity.ok(quizService.getDashboardStats());
    }

    // 8. Get all registered students
    @GetMapping("/students")
    public ResponseEntity<List<com.quizwiz.entity.Student>> getAllStudents() {
        return ResponseEntity.ok(quizService.getAllStudents());
    }
}
