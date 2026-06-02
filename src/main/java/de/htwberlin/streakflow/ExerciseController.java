package de.htwberlin.streakflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
public class ExerciseController {

    private static final int XP_PER_MINUTE = 10;
    private static final int MINUTES_PER_COIN = 10;
    private static final int DEFAULT_DAILY_GOAL = 3;

    private final ExerciseRepository exerciseRepository;
    private final ExerciseExecutionRepository executionRepository;

    public ExerciseController(ExerciseRepository exerciseRepository, ExerciseExecutionRepository executionRepository) {
        this.exerciseRepository = exerciseRepository;
        this.executionRepository = executionRepository;
    }

    @GetMapping("/exercises")
    public List<Exercise> getExercises() {
        seedDefaultExercisesIfNeeded();
        return exerciseRepository.findAll();
    }

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public Exercise createExercise(@RequestBody Exercise exercise) {
        if (exercise.getName() == null || exercise.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        if (exercise.getCategory() == null || exercise.getCategory().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }

        if (exercise.getTargetMinutesPerDay() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be positive");
        }

        exercise.setId(null);
        return exerciseRepository.save(exercise);
    }

    @GetMapping("/executions")
    public List<ExerciseExecution> getExecutions() {
        return executionRepository.findAll();
    }

    @PostMapping("/executions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseExecution completeExercise(@RequestBody CompleteExerciseRequest request) {
        Exercise exercise = exerciseRepository.findById(request.exerciseId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));

        LocalDate today = LocalDate.now();
        if (executionRepository.existsByExerciseIdAndDate(exercise.getId(), today)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Exercise already completed today");
        }

        int duration = request.duration() == null ? exercise.getTargetMinutesPerDay() : request.duration();
        if (duration < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be positive");
        }

        int earnedXp = duration * XP_PER_MINUTE;
        int earnedCoins = Math.max(1, duration / MINUTES_PER_COIN);

        ExerciseExecution execution = new ExerciseExecution(
                null,
                today,
                duration,
                exercise.getId(),
                exercise.getName(),
                earnedXp,
                earnedCoins
        );

        return executionRepository.save(execution);
    }

    @GetMapping("/progress")
    public UserProgress getProgress() {
        List<ExerciseExecution> executions = executionRepository.findAll();
        LocalDate today = LocalDate.now();
        List<ExerciseExecution> todayExecutions = executions.stream()
                .filter(execution -> today.equals(execution.getDate()))
                .toList();

        int xp = executions.stream().mapToInt(ExerciseExecution::getEarnedXp).sum();
        int coins = executions.stream().mapToInt(ExerciseExecution::getEarnedCoins).sum();
        int minutesToday = todayExecutions.stream().mapToInt(ExerciseExecution::getDuration).sum();
        int xpToday = todayExecutions.stream().mapToInt(ExerciseExecution::getEarnedXp).sum();
        int dailyGoal = Math.max(DEFAULT_DAILY_GOAL, Math.min(DEFAULT_DAILY_GOAL, (int) exerciseRepository.count()));
        int currentStreak = calculateCurrentStreak(executions);
        int longestStreak = calculateLongestStreak(executions);

        return new UserProgress(
                currentStreak,
                longestStreak,
                xp,
                xp / 1000 + 1,
                coins,
                2,
                todayExecutions.size(),
                dailyGoal,
                minutesToday,
                xpToday
        );
    }

    private int calculateCurrentStreak(List<ExerciseExecution> executions) {
        Set<LocalDate> completedDates = completedDates(executions);
        LocalDate cursor = LocalDate.now();
        int streak = 0;

        while (completedDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    private int calculateLongestStreak(List<ExerciseExecution> executions) {
        Set<LocalDate> completedDates = completedDates(executions);
        int longest = 0;
        int current = 0;
        LocalDate previousDate = null;

        for (LocalDate date : completedDates.stream().sorted().toList()) {
            if (previousDate != null && date.equals(previousDate.plusDays(1))) {
                current++;
            } else {
                current = 1;
            }

            longest = Math.max(longest, current);
            previousDate = date;
        }

        return longest;
    }

    private Set<LocalDate> completedDates(List<ExerciseExecution> executions) {
        Set<LocalDate> dates = new HashSet<>();
        for (ExerciseExecution execution : executions) {
            dates.add(execution.getDate());
        }
        return dates;
    }

    private void seedDefaultExercisesIfNeeded() {
        if (exerciseRepository.count() > 0) {
            return;
        }

        exerciseRepository.saveAll(List.of(
                new Exercise(null, "Joggen", "Cardio", 30),
                new Exercise(null, "Krafttraining", "Strength", 45),
                new Exercise(null, "Yoga", "Mobility", 20)
        ));
    }

    public record CompleteExerciseRequest(Long exerciseId, Integer duration) {
    }
}
