package de.htwberlin.streakflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseExecutionRepository extends JpaRepository<ExerciseExecution, Long> {
    List<ExerciseExecution> findByDate(LocalDate date);

    boolean existsByExerciseIdAndDate(Long exerciseId, LocalDate date);
}
