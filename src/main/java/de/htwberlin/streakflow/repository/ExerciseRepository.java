package de.htwberlin.streakflow.repository;

import de.htwberlin.streakflow.model.Exercise;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExerciseRepository extends JpaRepository<Exercise, Long> {
}
