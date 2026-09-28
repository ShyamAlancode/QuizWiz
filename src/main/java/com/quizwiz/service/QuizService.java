package com.quizwiz.service;

import com.quizwiz.dto.*;
import com.quizwiz.entity.Attempt;
import com.quizwiz.entity.Question;
import com.quizwiz.entity.Quiz;
import com.quizwiz.entity.Student;
import com.quizwiz.exception.QuizException;
import com.quizwiz.exception.ResourceNotFoundException;
import com.quizwiz.repository.AttemptRepository;
import com.quizwiz.repository.QuestionRepository;
import com.quizwiz.repository.QuizRepository;
import com.quizwiz.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final StudentRepository studentRepository;
    private final AttemptRepository attemptRepository;

    public QuizService(QuizRepository quizRepository,
                       QuestionRepository questionRepository,
                       StudentRepository studentRepository,
                       AttemptRepository attemptRepository) {
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.studentRepository = studentRepository;
        this.attemptRepository = attemptRepository;
    }

    // 1. Faculty creates a quiz with MCQ questions and correct options
    public Quiz createQuiz(CreateQuizRequest request) {
        Quiz quiz = new Quiz(request.getTitle(), request.getDescription(), request.getTimeLimitMinutes());

        if (request.getQuestions() != null) {
            for (QuestionDto qDto : request.getQuestions()) {
                Question question = new Question(
                        qDto.getQuestionText(),
                        qDto.getOptionA(),
                        qDto.getOptionB(),
                        qDto.getOptionC(),
                        qDto.getOptionD(),
                        qDto.getCorrectOption().toUpperCase()
                );
                quiz.addQuestion(question);
            }
        }

        return quizRepository.save(quiz);
    }

    // Get all quizzes
    public List<Quiz> getAllQuizzes() {
        return quizRepository.findAll();
    }

    // Get single quiz by ID
    public Quiz getQuizById(Long id) {
        return quizRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found with id: " + id));
    }

    // 2. Student starts a quiz attempt
    // Business Rule Enforced: Prevent a student from attempting the same quiz twice
    public Attempt startAttempt(StartAttemptRequest request) {
        Quiz quiz = getQuizById(request.getQuizId());

        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new QuizException("Cannot attempt a quiz with no questions.");
        }

        // Find or create student
        String roll = request.getRollNumber().trim().toUpperCase();
        Student student = studentRepository.findByRollNumber(roll)
                .orElseGet(() -> {
                    Student newStudent = new Student(roll, request.getName().trim(), 
                            (request.getEmail() != null && !request.getEmail().isBlank()) ? request.getEmail().trim() : roll.toLowerCase() + "@college.edu");
                    return studentRepository.save(newStudent);
                });

        // Enforce Rule: Prevent student from attempting the same quiz twice
        Optional<Attempt> existingAttempt = attemptRepository.findByStudentIdAndQuizId(student.getId(), quiz.getId());
        if (existingAttempt.isPresent()) {
            throw new QuizException("Student with Roll Number " + roll + " has already attempted this quiz! Duplicate attempts are forbidden.");
        }

        // Create new attempt
        Attempt attempt = new Attempt(student, quiz, quiz.getQuestions().size());
        return attemptRepository.save(attempt);
    }

    // 3. Student submits attempt & System auto-scores it
    // Business Rules Enforced:
    // - An attempt started must be auto-submitted when the time limit expires.
    // - Scores are computed only from questions actually answered.
    public Attempt submitAttempt(SubmitAttemptRequest request) {
        Attempt attempt = attemptRepository.findById(request.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + request.getAttemptId()));

        if (!"IN_PROGRESS".equals(attempt.getStatus())) {
            throw new QuizException("This attempt has already been submitted or finalized!");
        }

        Quiz quiz = attempt.getQuiz();
        LocalDateTime now = LocalDateTime.now();

        // Check time limit rule
        long secondsElapsed = Duration.between(attempt.getStartTime(), now).getSeconds();
        long allowedSeconds = (quiz.getTimeLimitMinutes() * 60L) + 15L; // 15 seconds grace period for network delays
        boolean timeExpired = secondsElapsed > allowedSeconds;

        // Auto-scoring logic
        // Rule: Scores are computed only from questions actually answered.
        int score = 0;
        Map<Long, String> answers = request.getAnswers() != null ? request.getAnswers() : Collections.emptyMap();

        for (Question q : quiz.getQuestions()) {
            String selected = answers.get(q.getId());
            // Only evaluate if question was actually answered
            if (selected != null && !selected.trim().isEmpty()) {
                if (selected.trim().equalsIgnoreCase(q.getCorrectOption().trim())) {
                    score++;
                }
            }
        }

        attempt.setScore(score);
        attempt.setSubmissionTime(now);
        attempt.setStatus(timeExpired ? "TIME_EXPIRED" : "SUBMITTED");

        return attemptRepository.save(attempt);
    }

    // 4. Faculty views class-wise score report for a quiz
    public List<ScoreReportDto> getScoreReportForQuiz(Long quizId) {
        // Ensure quiz exists
        getQuizById(quizId);

        List<Attempt> attempts = attemptRepository.findByQuizIdOrderByScoreDesc(quizId);

        return attempts.stream()
                .map(a -> new ScoreReportDto(
                        a.getStudent().getRollNumber(),
                        a.getStudent().getName(),
                        a.getStudent().getEmail(),
                        a.getScore(),
                        a.getTotalQuestions(),
                        a.getStatus(),
                        a.getStartTime(),
                        a.getSubmissionTime()
                ))
                .collect(Collectors.toList());
    }

    // 5. Get all registered students
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // Dashboard Statistics (Bonus feature)
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuizzes", quizRepository.count());
        stats.put("totalStudents", studentRepository.count());
        stats.put("totalAttempts", attemptRepository.count());
        return stats;
    }
}
