package com.quizwiz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizwizApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuizwizApplication.class, args);
        System.out.println("=================================================");
        System.out.println(" QuizWiz System is running on http://localhost:8080 ");
        System.out.println("=================================================");
    }
}
