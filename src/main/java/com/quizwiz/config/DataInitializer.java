package com.quizwiz.config;

import com.quizwiz.entity.Question;
import com.quizwiz.entity.Quiz;
import com.quizwiz.repository.QuizRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

        private final QuizRepository quizRepository;

        public DataInitializer(QuizRepository quizRepository) {
                this.quizRepository = quizRepository;
        }

        @Override
        public void run(String... args) {
                if (quizRepository.count() == 0) {
                        // Sample Quiz 1: Java Basics
                        Quiz javaQuiz = new Quiz("Java Basics Quick Quiz", "Core Java Concepts, OOP, and Keywords", 1);

                        Question q1 = new Question(
                                        "Which keyword is used to inherit a class in Java?",
                                        "implements",
                                        "extends",
                                        "inherits",
                                        "super",
                                        "B");
                        javaQuiz.addQuestion(q1);

                        Question q2 = new Question(
                                        "Which data type is used to create an object that can store 64-bit integer values?",
                                        "int",
                                        "float",
                                        "long",
                                        "double",
                                        "C");
                        javaQuiz.addQuestion(q2);

                        Question q3 = new Question(
                                        "What is the default value of a boolean variable in Java?",
                                        "true",
                                        "false",
                                        "null",
                                        "0",
                                        "B");
                        javaQuiz.addQuestion(q3);

                        quizRepository.save(javaQuiz);

                        // Sample Quiz 2: DBMS Fundamentals
                        Quiz dbQuiz = new Quiz("DBMS Fundamentals Quiz", "Relational Database Concepts and SQL", 10);

                        Question dbq1 = new Question(
                                        "Which SQL command is used to fetch data from a database table?",
                                        "GET",
                                        "FETCH",
                                        "SELECT",
                                        "EXTRACT",
                                        "C");
                        dbQuiz.addQuestion(dbq1);

                        Question dbq2 = new Question(
                                        "Which key uniquely identifies each record in a relational database table?",
                                        "Foreign Key",
                                        "Primary Key",
                                        "Unique Index",
                                        "Candidate Key",
                                        "B");
                        dbQuiz.addQuestion(dbq2);

                        quizRepository.save(dbQuiz);

                        System.out.println(">> Initialized Sample Quizzes for QuizWiz!");
                }
        }
}
