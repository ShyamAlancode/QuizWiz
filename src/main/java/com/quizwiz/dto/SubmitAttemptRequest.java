package com.quizwiz.dto;

import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class SubmitAttemptRequest {

    @NotNull(message = "Attempt ID is required")
    private Long attemptId;

    // Maps questionId -> student's selected option ("A", "B", "C", "D")
    private Map<Long, String> answers = new HashMap<>();

    public Long getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(Long attemptId) {
        this.attemptId = attemptId;
    }

    public Map<Long, String> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<Long, String> answers) {
        this.answers = answers;
    }
}
