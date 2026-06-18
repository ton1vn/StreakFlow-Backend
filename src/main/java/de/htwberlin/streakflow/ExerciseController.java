package de.htwberlin.streakflow;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@CrossOrigin(origins = "*")
@RestController
public class ExerciseController {

    private static final int XP_PER_MINUTE = 10;
    private static final int MINUTES_PER_COIN = 10;
    private static final int DEFAULT_DAILY_GOAL = 3;
    private static final String XP_BOOST_ID = "xp-boost";
    private static final String STREAK_FREEZE_ID = "streak-freeze";
    private static final int XP_BOOST_COST = 30;
    private static final int STREAK_FREEZE_COST = 50;
    private static final int BASE_STREAK_FREEZERS = 0;

    private final ExerciseRepository exerciseRepository;
    private final ExerciseExecutionRepository executionRepository;
    private final ShopPurchaseRepository shopPurchaseRepository;

    public ExerciseController(ExerciseRepository exerciseRepository, ExerciseExecutionRepository executionRepository, ShopPurchaseRepository shopPurchaseRepository) {
        this.exerciseRepository = exerciseRepository;
        this.executionRepository = executionRepository;
        this.shopPurchaseRepository = shopPurchaseRepository;
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

    @DeleteMapping("/exercises/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteExercise(@PathVariable Long id) {
        if (!exerciseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found");
        }

        executionRepository.deleteByExerciseId(id);
        exerciseRepository.deleteById(id);
    }

    @GetMapping("/executions")
    public List<ExerciseExecution> getExecutions() {
        return executionRepository.findAll();
    }

    @PostMapping("/executions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseExecution completeExercise(@RequestBody CompleteExerciseRequest request) {
        if (request.exerciseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise id is required");
        }

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

        ShopPurchase activeBoost = shopPurchaseRepository
                .findByItemId(XP_BOOST_ID)
                .stream()
                .filter(this::isActiveBoost)
                .findFirst()
                .orElse(null);
        int earnedXp = duration * XP_PER_MINUTE;
        if (activeBoost != null) {
            earnedXp *= 2;
        }
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

    @GetMapping("/shop/items")
    public List<ShopItem> getShopItems() {
        return List.of(
                new ShopItem(XP_BOOST_ID, "XP Boost", XP_BOOST_COST, "Verdoppelt 24 Stunden lang die XP aller bestätigten Übungen."),
                new ShopItem(STREAK_FREEZE_ID, "StreakFreeze", STREAK_FREEZE_COST, "Schützt deine Streak, wenn du einen Tag verpasst.")
        );
    }

    @GetMapping("/shop/purchases")
    public List<ShopPurchase> getShopPurchases() {
        return shopPurchaseRepository.findAll();
    }

    @PostMapping("/shop/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopPurchase buyShopItem(@RequestBody ShopPurchaseRequest request) {
        ShopItem item = getShopItems().stream()
                .filter(shopItem -> shopItem.id().equals(request.itemId()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop item not found"));

        if (availableCoins() < item.cost()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Not enough coins");
        }

        ShopPurchase purchase = new ShopPurchase(
                null,
                item.id(),
                item.name(),
                item.cost(),
                LocalDateTime.now(),
                null,
                null
        );

        return shopPurchaseRepository.save(purchase);
    }

    @PostMapping("/shop/purchases/{id}/use")
    public ShopPurchase useShopPurchase(@PathVariable Long id) {
        ShopPurchase purchase = shopPurchaseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Purchase not found"));

        if (!XP_BOOST_ID.equals(purchase.getItemId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only XP Boosts can be activated manually");
        }

        if (purchase.getUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "XP Boost was already activated");
        }

        LocalDateTime now = LocalDateTime.now();
        purchase.setUsedAt(now);
        purchase.setExpiresAt(now.plusHours(24));
        return shopPurchaseRepository.save(purchase);
    }

    @GetMapping("/progress")
    public UserProgress getProgress() {
        seedDefaultExercisesIfNeeded();

        List<ExerciseExecution> executions = executionRepository.findAll();
        LocalDate today = LocalDate.now();
        List<ExerciseExecution> todayExecutions = executions.stream()
                .filter(execution -> today.equals(execution.getDate()))
                .toList();

        int xp = executions.stream().mapToInt(ExerciseExecution::getEarnedXp).sum();
        int coins = availableCoins();
        int minutesToday = todayExecutions.stream().mapToInt(ExerciseExecution::getDuration).sum();
        int xpToday = todayExecutions.stream().mapToInt(ExerciseExecution::getEarnedXp).sum();
        int dailyGoal = Math.max(1, Math.min(DEFAULT_DAILY_GOAL, (int) exerciseRepository.count()));
        int currentStreak = calculateCurrentStreak(executions);
        int longestStreak = calculateLongestStreak(executions);
        int streakFreezers = BASE_STREAK_FREEZERS + availableStreakFreezerCount();
        int activeXpBoosts = activeXpBoostCount();
        int availableXpBoosts = availableXpBoostCount();
        String activeXpBoostExpiresAt = activeXpBoostExpiresAt();

        return new UserProgress(
                currentStreak,
                longestStreak,
                xp,
                xp / 1000 + 1,
                coins,
                streakFreezers,
                todayExecutions.size(),
                dailyGoal,
                minutesToday,
                xpToday,
                activeXpBoosts,
                availableXpBoosts,
                activeXpBoostExpiresAt
        );
    }

    private int availableCoins() {
        int earnedCoins = executionRepository.findAll().stream()
                .mapToInt(ExerciseExecution::getEarnedCoins)
                .sum();
        int spentCoins = shopPurchaseRepository.findAll().stream()
                .mapToInt(ShopPurchase::getCost)
                .sum();
        return earnedCoins - spentCoins;
    }

    private int activeXpBoostCount() {
        return (int) shopPurchaseRepository.findByItemId(XP_BOOST_ID)
                .stream()
                .filter(this::isActiveBoost)
                .count();
    }

    private int availableXpBoostCount() {
        return shopPurchaseRepository.findByItemIdAndUsedAtIsNull(XP_BOOST_ID).size();
    }

    private String activeXpBoostExpiresAt() {
        return shopPurchaseRepository.findByItemId(XP_BOOST_ID)
                .stream()
                .filter(this::isActiveBoost)
                .map(ShopPurchase::getExpiresAt)
                .sorted()
                .findFirst()
                .map(LocalDateTime::toString)
                .orElse(null);
    }

    private boolean isActiveBoost(ShopPurchase purchase) {
        return XP_BOOST_ID.equals(purchase.getItemId())
                && purchase.getUsedAt() != null
                && purchase.getExpiresAt() != null
                && purchase.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private int availableStreakFreezerCount() {
        return shopPurchaseRepository.findByItemIdAndUsedAtIsNull(STREAK_FREEZE_ID).size();
    }

    private int calculateCurrentStreak(List<ExerciseExecution> executions) {
        Set<LocalDate> completedDates = completedDates(executions);
        LocalDate today = LocalDate.now();
        LocalDate cursor = completedDates.contains(today) ? today : today.minusDays(1);
        int streak = 0;
        int availableFreezers = availableStreakFreezerCount();

        while (!cursor.isBefore(oldestCompletedDate(completedDates))) {
            if (completedDates.contains(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
                continue;
            }

            if (availableFreezers <= 0) {
                return 0;
            }

            consumeStreakFreeze();
            availableFreezers--;
            cursor = cursor.minusDays(1);
        }

        return streak;
    }

    private LocalDate oldestCompletedDate(Set<LocalDate> completedDates) {
        return completedDates.stream()
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());
    }

    private void consumeStreakFreeze() {
        ShopPurchase freeze = shopPurchaseRepository
                .findFirstByItemIdAndUsedAtIsNullOrderByPurchasedAtAsc(STREAK_FREEZE_ID)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "No streak freezer available"));
        freeze.setUsedAt(LocalDateTime.now());
        shopPurchaseRepository.save(freeze);
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

    public record ShopItem(String id, String name, int cost, String description) {
    }

    public record ShopPurchaseRequest(String itemId) {
    }
}
