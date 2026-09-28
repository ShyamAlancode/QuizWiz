package com.quizwiz.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class StartAttemptRequest {

    @NotNull(message = "Quiz ID is required")
    private Long quizId;

    @NotBlank(message = "Roll number is required")
    @Pattern(regexp = "^[A-Za-z0-9 -]+$", message = "Roll number can only contain alphanumeric characters, spaces, or hyphens")
    @Size(max = 50, message = "Roll number cannot exceed 50 characters")
    private String rollNumber;

    @NotBlank(message = "Student name is required")
    @Size(max = 100, message = "Student name cannot exceed 100 characters")
    private String name;

    @Email(message = "Invalid email format")
    @Size(max = 100, message = "Email cannot exceed 100 characters")
    private String email;

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
