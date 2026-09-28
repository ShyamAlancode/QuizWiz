// QuizWiz Frontend Logic
const API_BASE = '/api';

// Current State
let currentRole = 'student';
let activeAttempt = null;
let activeQuiz = null;
let timerInterval = null;
let questionCount = 0;

// Initialize on load
document.addEventListener('DOMContentLoaded', () => {
    loadQuizzes();
    loadDashboardStats();
    // Pre-populate one question in faculty form
    addQuestionItem();
    // Check for in-progress attempt to recover session on page refresh
    checkForActiveAttempt();
});

// Role Switcher (Student / Faculty)
function switchRole(role) {
    currentRole = role;
    const studentBtn = document.getElementById('tab-student-btn');
    const facultyBtn = document.getElementById('tab-faculty-btn');
    const studentSec = document.getElementById('student-section');
    const facultySec = document.getElementById('faculty-section');

    hideAlert();

    if (role === 'student') {
        studentBtn.classList.add('active');
        facultyBtn.classList.remove('active');
        studentSec.classList.remove('d-none');
        facultySec.classList.add('d-none');
    } else {
        facultyBtn.classList.add('active');
        studentBtn.classList.remove('active');
        facultySec.classList.remove('d-none');
        studentSec.classList.add('d-none');
        loadDashboardStats();
    }
}

// Show & Hide Alert
function showAlert(message, type = 'danger') {
    const alertBox = document.getElementById('alert-box');
    alertBox.className = `alert alert-${type}`;
    alertBox.textContent = message;
    alertBox.classList.remove('d-none');
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function hideAlert() {
    const alertBox = document.getElementById('alert-box');
    alertBox.classList.add('d-none');
}

// Check for active attempt from sessionStorage (recovers session on page refresh)
async function checkForActiveAttempt() {
    const savedAttemptId = sessionStorage.getItem('quizwiz_active_attempt_id');
    if (!savedAttemptId) return;

    try {
        const res = await fetch(`${API_BASE}/attempts/${savedAttemptId}`);
        if (!res.ok) {
            sessionStorage.removeItem('quizwiz_active_attempt_id');
            return;
        }

        const attempt = await res.json();
        if (attempt.status === 'IN_PROGRESS' && attempt.remainingSeconds != null && attempt.remainingSeconds > 0) {
            activeAttempt = attempt;
            activeQuiz = attempt.quiz;
            renderQuizScreen(activeQuiz, attempt.remainingSeconds);
            showAlert('Resumed active quiz attempt. Your timer has been synchronized with the server.', 'warning');
        } else {
            sessionStorage.removeItem('quizwiz_active_attempt_id');
        }
    } catch (err) {
        console.error('Failed to resume attempt:', err);
        sessionStorage.removeItem('quizwiz_active_attempt_id');
    }
}

// Load Quizzes from API
async function loadQuizzes() {
    try {
        const res = await fetch(`${API_BASE}/quizzes`);
        if (!res.ok) throw new Error('Failed to fetch quizzes');
        const quizzes = await res.json();

        // 1. Populate Student Dropdown
        const select = document.getElementById('student-quiz-select');
        select.innerHTML = '<option value="">-- Choose an Available Quiz --</option>';
        quizzes.forEach(q => {
            const opt = document.createElement('option');
            opt.value = q.id;
            opt.textContent = `${q.title} (${q.timeLimitMinutes} mins, ${q.questions ? q.questions.length : 0} questions)`;
            select.appendChild(opt);
        });

        // 2. Populate Faculty Table
        const tbody = document.getElementById('faculty-quizzes-table-body');
        tbody.innerHTML = '';
        if (quizzes.length === 0) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">No quizzes created yet.</td></tr>';
            return;
        }

        quizzes.forEach(q => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>#${q.id}</td>
                <td><strong>${escapeHtml(q.title)}</strong><br><small class="subtitle">${escapeHtml(q.description || '')}</small></td>
                <td>${q.timeLimitMinutes} mins</td>
                <td>${q.questions ? q.questions.length : 0}</td>
                <td class="action-cell"></td>
            `;
            const btn = document.createElement('button');
            btn.className = 'btn btn-sm btn-secondary';
            btn.textContent = 'View Report';
            btn.addEventListener('click', () => viewScoreReport(q.id, q.title));
            tr.querySelector('.action-cell').appendChild(btn);
            tbody.appendChild(tr);
        });

    } catch (err) {
        console.error('Error loading quizzes:', err);
    }
}

// Load Dashboard Stats
async function loadDashboardStats() {
    try {
        const res = await fetch(`${API_BASE}/dashboard/stats`);
        if (res.ok) {
            const stats = await res.json();
            document.getElementById('stat-quizzes').textContent = stats.totalQuizzes || 0;
            document.getElementById('stat-students').textContent = stats.totalStudents || 0;
            document.getElementById('stat-attempts').textContent = stats.totalAttempts || 0;
        }
    } catch (err) {
        console.error('Failed to load stats:', err);
    }
}

// =================== STUDENT WORKFLOW ===================

// Step 1: Start Quiz
async function handleStartQuiz(e) {
    e.preventDefault();
    hideAlert();

    const quizId = document.getElementById('student-quiz-select').value;
    const rawRoll = document.getElementById('student-roll').value;
    const rollNumber = rawRoll.replace(/[^A-Za-z0-9]/g, '').toUpperCase();
    const name = document.getElementById('student-name').value.trim();
    const email = document.getElementById('student-email').value.trim();

    if (!quizId) {
        showAlert('Please select a quiz to start.');
        return;
    }

    if (!rollNumber) {
        showAlert('Please enter a valid alphanumeric roll number.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/attempts/start`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quizId: Number(quizId), rollNumber, name, email })
        });

        const data = await res.json();

        if (!res.ok) {
            // Handles duplicate attempt business rule or validation errors
            showAlert(data.message || 'Error starting quiz.', 'danger');
            return;
        }

        activeAttempt = data;
        activeQuiz = data.quiz;
        sessionStorage.setItem('quizwiz_active_attempt_id', data.id);

        const initialSeconds = data.remainingSeconds != null ? data.remainingSeconds : (activeQuiz.timeLimitMinutes * 60);
        renderQuizScreen(activeQuiz, initialSeconds);

    } catch (err) {
        showAlert('Could not start quiz. Server error: ' + err.message);
    }
}

// Step 2: Render Active Quiz
function renderQuizScreen(quiz, secondsLeft) {
    document.getElementById('student-start-card').classList.add('d-none');
    document.getElementById('quiz-taking-card').classList.remove('d-none');

    document.getElementById('active-quiz-title').textContent = quiz.title;
    document.getElementById('active-quiz-desc').textContent = quiz.description || 'Answer all questions before time expires.';

    const container = document.getElementById('questions-list');
    container.innerHTML = '';

    quiz.questions.forEach((q, idx) => {
        const qCard = document.createElement('div');
        qCard.className = 'question-card';
        qCard.innerHTML = `
            <div class="question-title">Q${idx + 1}. ${escapeHtml(q.questionText)}</div>
            <div class="options-group">
                <label class="option-label">
                    <input type="radio" name="question_${q.id}" value="A">
                    <span><strong>A.</strong> ${escapeHtml(q.optionA)}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="question_${q.id}" value="B">
                    <span><strong>B.</strong> ${escapeHtml(q.optionB)}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="question_${q.id}" value="C">
                    <span><strong>C.</strong> ${escapeHtml(q.optionC)}</span>
                </label>
                <label class="option-label">
                    <input type="radio" name="question_${q.id}" value="D">
                    <span><strong>D.</strong> ${escapeHtml(q.optionD)}</span>
                </label>
            </div>
        `;
        container.appendChild(qCard);
    });

    const seconds = (secondsLeft != null && secondsLeft > 0) ? secondsLeft : (quiz.timeLimitMinutes * 60);
    startTimer(seconds);
}

// Timer Logic with Auto-submit (strictly synchronized with server duration)
function startTimer(secondsLeft) {
    const timerElem = document.getElementById('time-left');

    clearInterval(timerInterval);

    const updateTimerDisplay = () => {
        if (secondsLeft <= 0) {
            clearInterval(timerInterval);
            timerElem.textContent = '00:00';
            showAlert('Time has expired! Your quiz is being auto-submitted...', 'warning');
            submitQuizAnswers(true);
            return;
        }

        const mins = Math.floor(secondsLeft / 60);
        const secs = secondsLeft % 60;
        timerElem.textContent = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;

        if (secondsLeft <= 30) {
            document.getElementById('timer-display').style.backgroundColor = '#fee2e2';
            document.getElementById('timer-display').style.color = '#991b1b';
        } else {
            document.getElementById('timer-display').style.backgroundColor = '';
            document.getElementById('timer-display').style.color = '';
        }

        secondsLeft--;
    };

    updateTimerDisplay();
    timerInterval = setInterval(updateTimerDisplay, 1000);
}

// Step 3: Handle Quiz Submission
async function handleSubmitQuiz(e) {
    if (e) e.preventDefault();
    clearInterval(timerInterval);
    await submitQuizAnswers(false);
}

async function submitQuizAnswers(isAutoSubmit = false) {
    if (!activeAttempt || !activeQuiz) return;

    // Collect answered questions
    const answers = {};
    activeQuiz.questions.forEach(q => {
        const selected = document.querySelector(`input[name="question_${q.id}"]:checked`);
        if (selected) {
            answers[q.id] = selected.value;
        }
    });

    try {
        const res = await fetch(`${API_BASE}/attempts/submit`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                attemptId: activeAttempt.id,
                answers: answers
            })
        });

        const result = await res.json();
        if (!res.ok) {
            showAlert(result.message || 'Error submitting quiz.');
            return;
        }

        sessionStorage.removeItem('quizwiz_active_attempt_id');
        renderResultScreen(result, isAutoSubmit);

    } catch (err) {
        showAlert('Failed to submit quiz: ' + err.message);
    }
}

// Render Instant Score Result
function renderResultScreen(attempt, isAutoSubmit) {
    document.getElementById('quiz-taking-card').classList.add('d-none');
    document.getElementById('quiz-result-card').classList.remove('d-none');

    document.getElementById('result-score').textContent = attempt.score;
    document.getElementById('result-total').textContent = attempt.totalQuestions;

    const percent = attempt.totalQuestions > 0 ? ((attempt.score / attempt.totalQuestions) * 100).toFixed(1) : 0;
    document.getElementById('result-percent').textContent = `${percent}% Marks`;

    document.getElementById('result-student-name').textContent = attempt.student.name;
    document.getElementById('result-student-roll').textContent = attempt.student.rollNumber;

    const statusBadge = document.getElementById('result-status');
    statusBadge.textContent = attempt.status;
    statusBadge.className = attempt.status === 'SUBMITTED' ? 'badge badge-success' : 'badge badge-warning';

    const statusMsg = document.getElementById('result-status-msg');
    if (isAutoSubmit || attempt.status === 'TIME_EXPIRED') {
        statusMsg.textContent = 'Submitted automatically because the time limit expired.';
    } else {
        statusMsg.textContent = 'Submitted successfully and scored immediately!';
    }

    loadDashboardStats();
}

function resetStudentPortal() {
    sessionStorage.removeItem('quizwiz_active_attempt_id');
    document.getElementById('quiz-result-card').classList.add('d-none');
    document.getElementById('quiz-taking-card').classList.add('d-none');
    document.getElementById('student-start-card').classList.remove('d-none');
    document.getElementById('start-quiz-form').reset();
    activeAttempt = null;
    activeQuiz = null;
    hideAlert();
    loadQuizzes();
}

// =================== FACULTY WORKFLOW ===================

// Dynamic Question Builder Item
function addQuestionItem() {
    questionCount++;
    const container = document.getElementById('questions-builder-container');
    const item = document.createElement('div');
    item.className = 'builder-item';
    item.id = `q-builder-${questionCount}`;

    item.innerHTML = `
        <div class="builder-item-header">
            <span>Question #${questionCount}</span>
            ${questionCount > 1 ? `<button type="button" class="btn btn-sm btn-outline" onclick="removeQuestionItem(${questionCount})">Remove</button>` : ''}
        </div>
        <div class="form-group">
            <label>Question Text *</label>
            <input type="text" class="q-text" placeholder="Enter question..." required>
        </div>
        <div class="form-row">
            <div class="form-group flex-1">
                <label>Option A *</label>
                <input type="text" class="q-optA" placeholder="Option A" required>
            </div>
            <div class="form-group flex-1">
                <label>Option B *</label>
                <input type="text" class="q-optB" placeholder="Option B" required>
            </div>
        </div>
        <div class="form-row">
            <div class="form-group flex-1">
                <label>Option C *</label>
                <input type="text" class="q-optC" placeholder="Option C" required>
            </div>
            <div class="form-group flex-1">
                <label>Option D *</label>
                <input type="text" class="q-optD" placeholder="Option D" required>
            </div>
        </div>
        <div class="form-group">
            <label>Correct Option *</label>
            <select class="q-correct" required>
                <option value="A">Option A</option>
                <option value="B">Option B</option>
                <option value="C">Option C</option>
                <option value="D">Option D</option>
            </select>
        </div>
    `;
    container.appendChild(item);
}

function removeQuestionItem(id) {
    const item = document.getElementById(`q-builder-${id}`);
    if (item) item.remove();
}

// Handle Faculty Create Quiz
async function handleCreateQuiz(e) {
    e.preventDefault();
    hideAlert();

    const title = document.getElementById('quiz-title').value.trim();
    const timeLimitMinutes = Number(document.getElementById('quiz-time').value);
    const description = document.getElementById('quiz-desc').value.trim();

    // Gather questions
    const items = document.querySelectorAll('.builder-item');
    if (items.length === 0) {
        showAlert('Please add at least one question.');
        return;
    }

    const questions = [];
    items.forEach(item => {
        questions.push({
            questionText: item.querySelector('.q-text').value.trim(),
            optionA: item.querySelector('.q-optA').value.trim(),
            optionB: item.querySelector('.q-optB').value.trim(),
            optionC: item.querySelector('.q-optC').value.trim(),
            optionD: item.querySelector('.q-optD').value.trim(),
            correctOption: item.querySelector('.q-correct').value
        });
    });

    try {
        const res = await fetch(`${API_BASE}/quizzes`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                title,
                description,
                timeLimitMinutes,
                questions
            })
        });

        const data = await res.json();
        if (!res.ok) {
            showAlert(data.message || 'Error creating quiz.');
            return;
        }

        showAlert(`Quiz "${data.title}" created successfully!`, 'success');
        document.getElementById('create-quiz-form').reset();
        document.getElementById('questions-builder-container').innerHTML = '';
        questionCount = 0;
        addQuestionItem();

        loadQuizzes();
        loadDashboardStats();

    } catch (err) {
        showAlert('Failed to create quiz: ' + err.message);
    }
}

// View Score Report
async function viewScoreReport(quizId, quizTitle) {
    hideAlert();
    try {
        const res = await fetch(`${API_BASE}/reports/quiz/${quizId}`);
        if (!res.ok) throw new Error('Failed to load score report');
        const report = await res.json();

        const modal = document.getElementById('score-report-modal');
        document.getElementById('report-quiz-title').textContent = `Class Score Report: ${quizTitle}`;

        const tbody = document.getElementById('score-report-table-body');
        tbody.innerHTML = '';

        if (report.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center">No students have attempted this quiz yet.</td></tr>';
        } else {
            report.forEach(r => {
                const tr = document.createElement('tr');
                const badgeClass = r.status === 'SUBMITTED' ? 'badge-success' : 'badge-warning';
                const subDate = r.submissionTime ? new Date(r.submissionTime).toLocaleString() : 'N/A';

                tr.innerHTML = `
                    <td><strong>${escapeHtml(r.rollNumber)}</strong></td>
                    <td>${escapeHtml(r.studentName)}</td>
                    <td>${escapeHtml(r.studentEmail || '-')}</td>
                    <td><strong>${r.score}</strong> / ${r.totalQuestions}</td>
                    <td>${r.percentage}%</td>
                    <td><span class="badge ${badgeClass}">${r.status}</span></td>
                    <td><small>${subDate}</small></td>
                `;
                tbody.appendChild(tr);
            });
        }

        modal.classList.remove('d-none');
        modal.scrollIntoView({ behavior: 'smooth' });

    } catch (err) {
        showAlert('Could not load report: ' + err.message);
    }
}

function closeScoreReport() {
    document.getElementById('score-report-modal').classList.add('d-none');
}

// View All Registered Students
async function viewAllStudents() {
    hideAlert();
    try {
        const res = await fetch(`${API_BASE}/students`);
        if (!res.ok) throw new Error('Failed to load students');
        const students = await res.json();

        const modal = document.getElementById('students-modal');
        const tbody = document.getElementById('students-table-body');
        tbody.innerHTML = '';

        if (students.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center">No students registered yet.</td></tr>';
        } else {
            students.forEach(s => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>#${s.id}</td>
                    <td><strong>${escapeHtml(s.rollNumber)}</strong></td>
                    <td>${escapeHtml(s.name)}</td>
                    <td>${escapeHtml(s.email || '-')}</td>
                `;
                tbody.appendChild(tr);
            });
        }

        modal.classList.remove('d-none');
        modal.scrollIntoView({ behavior: 'smooth' });

    } catch (err) {
        showAlert('Could not load students: ' + err.message);
    }
}

function closeStudentsModal() {
    document.getElementById('students-modal').classList.add('d-none');
}

// Helper: escape HTML
function escapeHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
