package com.quizwiz.dto;

import java.time.LocalDateTime;

public class ScoreReportDto {

    private String rollNumber;
    private String studentName;
    private String studentEmail;
    private Integer score;
    private Integer totalQuestions;
    private Double percentage;
    private String status;
    private LocalDateTime startTime;
    private LocalDateTime submissionTime;

    public ScoreReportDto() {
    }

    public ScoreReportDto(String rollNumber, String studentName, String studentEmail, Integer score, Integer totalQuestions, String status, LocalDateTime startTime, LocalDateTime submissionTime) {
        this.rollNumber = rollNumber;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.score = score;
        this.totalQuestions = totalQuestions;
        this.percentage = (totalQuestions != null && totalQuestions > 0 && score != null) 
                ? Math.round(((double) score / totalQuestions * 100.0) * 10.0) / 10.0 
                : 0.0;
        this.status = status;
        this.startTime = startTime;
        this.submissionTime = submissionTime;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getTotalQuestions() {
        return totalQuestions;
    }

    public void setTotalQuestions(Integer totalQuestions) {
        this.totalQuestions = totalQuestions;
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getSubmissionTime() {
        return submissionTime;
    }

    public void setSubmissionTime(LocalDateTime submissionTime) {
        this.submissionTime = submissionTime;
    }
}
