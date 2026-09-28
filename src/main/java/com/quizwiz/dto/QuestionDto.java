package com.quizwiz.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class QuestionDto {

    private Long id;

    @NotBlank(message = "Question text cannot be blank")
    @Size(max = 1000, message = "Question text cannot exceed 1000 characters")
    private String questionText;

    @NotBlank(message = "Option A cannot be blank")
    @Size(max = 255, message = "Option A cannot exceed 255 characters")
    private String optionA;

    @NotBlank(message = "Option B cannot be blank")
    @Size(max = 255, message = "Option B cannot exceed 255 characters")
    private String optionB;

    @NotBlank(message = "Option C cannot be blank")
    @Size(max = 255, message = "Option C cannot exceed 255 characters")
    private String optionC;

    @NotBlank(message = "Option D cannot be blank")
    @Size(max = 255, message = "Option D cannot exceed 255 characters")
    private String optionD;

    @NotBlank(message = "Correct option must be A, B, C, or D")
    @Pattern(regexp = "^[A-Da-d]$", message = "Correct option must be A, B, C, or D")
    private String correctOption;

    public QuestionDto() {
    }

    public QuestionDto(String questionText, String optionA, String optionB, String optionC, String optionD, String correctOption) {
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctOption = correctOption;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public String getCorrectOption() {
        return correctOption;
    }

    public void setCorrectOption(String correctOption) {
        this.correctOption = correctOption != null ? correctOption.toUpperCase() : null;
    }
}
