package de.htwberlin.streakflow;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ExerciseController {

    @GetMapping("/exercises")
    public List<Exercise> getExercises() {
        return List.of(
                new Exercise(1L, "Joggen", "Cardio", 30),
                new Exercise(2L, "Krafttraining", "Strength", 45),
                new Exercise(3L, "Yoga", "Mobility", 20)
        );
    }

    @GetMapping("/executions")
    public List<ExerciseExecution> getExecutions() {
        return List.of(
                new ExerciseExecution(1L, "2026-04-20", 30, 1L),
                new ExerciseExecution(2L, "2026-04-19", 45, 2L),
                new ExerciseExecution(3L, "2026-04-18", 20, 3L)
        );
    }
}