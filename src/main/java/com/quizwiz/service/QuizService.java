package com.quizwiz.service;

import com.quizwiz.dto.*;
import com.quizwiz.entity.Attempt;
import com.quizwiz.entity.Question;
import com.quizwiz.entity.Quiz;
import com.quizwiz.entity.Student;
import com.quizwiz.exception.DuplicateAttemptException;
import com.quizwiz.exception.QuizException;
import com.quizwiz.exception.ResourceNotFoundException;
import com.quizwiz.repository.AttemptRepository;
import com.quizwiz.repository.QuestionRepository;
import com.quizwiz.repository.QuizRepository;
import com.quizwiz.repository.StudentRepository;
import org.springframework.scheduling.annotation.Scheduled;
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
    // Business Rule Enforced: Prevent a student from attempting the same quiz twice (409 Conflict)
    public Attempt startAttempt(StartAttemptRequest request) {
        Quiz quiz = getQuizById(request.getQuizId());

        if (quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            throw new QuizException("Cannot attempt a quiz with no questions.");
        }

        // Find or create student with normalized roll number (remove spaces, hyphens, special chars)
        String roll = request.getRollNumber().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (roll.isBlank()) {
            throw new QuizException("Roll number cannot be empty or contain only symbols.");
        }

        Optional<Student> studentOpt = studentRepository.findByRollNumber(roll);
        Student student;
        if (studentOpt.isPresent()) {
            student = studentOpt.get();
            // Validate that the provided name matches the registered student record
            if (!student.getName().trim().equalsIgnoreCase(request.getName().trim())) {
                throw new QuizException("Roll number " + roll + " is already registered under the name: '" 
                        + student.getName() + "'. Please enter your exact registered name.");
            }
        } else {
            String fallbackEmail = roll.toLowerCase() + "@college.edu";
            String studentEmail = (request.getEmail() != null && !request.getEmail().isBlank()) ? request.getEmail().trim() : fallbackEmail;
            student = studentRepository.save(new Student(roll, request.getName().trim(), studentEmail));
        }

        // Enforce Rule: Prevent student from attempting the same quiz twice
        Optional<Attempt> existingAttempt = attemptRepository.findByStudentIdAndQuizId(student.getId(), quiz.getId());
        if (existingAttempt.isPresent()) {
            throw new DuplicateAttemptException("Student with Roll Number " + roll + " has already attempted this quiz! Duplicate attempts are forbidden.");
        }

        // Create new attempt
        Attempt attempt = new Attempt(student, quiz, quiz.getQuestions().size());
        return attemptRepository.save(attempt);
    }

    // 3. Student submits attempt & System auto-scores it
    // Business Rules Enforced:
    // - An attempt started must be auto-submitted when the time limit expires.
    // - Submissions past time limit are not scored (score = 0).
    // - Scores are computed only from questions actually answered.
    // - Invalid questions or options throw clear exception immediately.
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

        Map<Long, String> answers = request.getAnswers() != null ? request.getAnswers() : Collections.emptyMap();

        // Validate answer IDs and option values
        Set<Long> validIds = quiz.getQuestions().stream().map(Question::getId).collect(Collectors.toSet());
        for (Map.Entry<Long, String> e : answers.entrySet()) {
            if (!validIds.contains(e.getKey())) {
                throw new QuizException("Question " + e.getKey() + " does not belong to this quiz");
            }
            if (e.getValue() != null && !e.getValue().isBlank() && !e.getValue().trim().matches("(?i)[A-D]")) {
                throw new QuizException("Answer for question " + e.getKey() + " must be A, B, C or D");
            }
        }

        // Auto-scoring logic:
        // Rule: Submissions past deadline are marked TIME_EXPIRED with score 0.
        // Otherwise, scores are computed only from questions actually answered.
        int score = 0;
        if (!timeExpired) {
            for (Question q : quiz.getQuestions()) {
                String selected = answers.get(q.getId());
                // Only evaluate if question was actually answered
                if (selected != null && !selected.trim().isEmpty()) {
                    if (selected.trim().equalsIgnoreCase(q.getCorrectOption().trim())) {
                        score++;
                    }
                }
            }
        }

        attempt.setScore(score);
        attempt.setSubmissionTime(now);
        attempt.setStatus(timeExpired ? "TIME_EXPIRED" : "SUBMITTED");

        return attemptRepository.save(attempt);
    }

    // 4. Scheduled Background Job to auto-close abandoned expired attempts
    @Scheduled(fixedRate = 60000)
    public void closeExpiredAttempts() {
        LocalDateTime now = LocalDateTime.now();
        for (Attempt a : attemptRepository.findByStatus("IN_PROGRESS")) {
            if (a.getStartTime().plusMinutes(a.getQuiz().getTimeLimitMinutes()).isBefore(now)) {
                a.setStatus("TIME_EXPIRED");
                a.setSubmissionTime(now);
                attemptRepository.save(a);
            }
        }
    }

    // Get attempt by ID (enables attempt restoration upon browser refresh)
    public Attempt getAttemptById(Long id) {
        return attemptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attempt not found with id: " + id));
    }

    // 5. Faculty views class-wise score report for a quiz
    public List<ScoreReportDto> getScoreReportForQuiz(Long quizId) {
        // Ensure quiz exists
        getQuizById(quizId);

        List<Attempt> attempts = attemptRepository.findByQuizIdOrderByScoreDesc(quizId);

        return attempts.stream()
                .filter(a -> !"IN_PROGRESS".equals(a.getStatus()))
                .sorted(Comparator.comparingInt(Attempt::getScore).reversed()
                        .thenComparing(a -> a.getStudent().getRollNumber()))
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

    // 6. Get all registered students
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    // 7. Dashboard Statistics
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalQuizzes", quizRepository.count());
        stats.put("totalStudents", studentRepository.count());
        stats.put("totalAttempts", attemptRepository.count());
        return stats;
    }
}
