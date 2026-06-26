package de.htwberlin.streakflow.repository;

import de.htwberlin.streakflow.model.ExerciseExecution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ExerciseExecutionRepository extends JpaRepository<ExerciseExecution, Long> {
    boolean existsByExerciseIdAndDate(Long exerciseId, LocalDate date);

    void deleteByExerciseId(Long exerciseId);
}
