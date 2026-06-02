package de.htwberlin.streakflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
public class ExerciseController {

    private final ExerciseRepository exerciseRepository;

    public ExerciseController(ExerciseRepository exerciseRepository) {
        this.exerciseRepository = exerciseRepository;
    }

    @GetMapping("/exercises")
    public List<Exercise> getExercises() {
        return exerciseRepository.findAll();
    }

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public Exercise createExercise(@RequestBody Exercise exercise) {
        exercise.setId(null);
        return exerciseRepository.save(exercise);
    }

    @GetMapping("/executions")
    public List<ExerciseExecution> getExecutions() {
        return List.of(
                new ExerciseExecution(1L, "2026-04-20", 30, 1L),
                new ExerciseExecution(2L, "2026-04-19", 45, 2L),
                new ExerciseExecution(3L, "2026-04-18", 20, 3L)
        );
    }

    @GetMapping("/progress")
    public UserProgress getProgress() {
        return new UserProgress(
                7,
                14,
                860,
                4,
                120,
                2,
                2,
                3
        );
    }
}
