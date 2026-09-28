package com.quizwiz.repository;

import com.quizwiz.entity.Attempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AttemptRepository extends JpaRepository<Attempt, Long> {
    Optional<Attempt> findByStudentIdAndQuizId(Long studentId, Long quizId);
    List<Attempt> findByQuizIdOrderByScoreDesc(Long quizId);
    long count();
}
