package de.htwberlin.streakflow.service;

import de.htwberlin.streakflow.dto.CompleteExerciseRequest;
import de.htwberlin.streakflow.dto.CreatePlayerRequest;
import de.htwberlin.streakflow.dto.PlayerWorkoutRequest;
import de.htwberlin.streakflow.dto.ShopItem;
import de.htwberlin.streakflow.dto.ShopPurchaseRequest;
import de.htwberlin.streakflow.model.Exercise;
import de.htwberlin.streakflow.model.ExerciseExecution;
import de.htwberlin.streakflow.model.Player;
import de.htwberlin.streakflow.model.ShopPurchase;
import de.htwberlin.streakflow.model.UserProgress;
import de.htwberlin.streakflow.repository.ExerciseExecutionRepository;
import de.htwberlin.streakflow.repository.ExerciseRepository;
import de.htwberlin.streakflow.repository.PlayerRepository;
import de.htwberlin.streakflow.repository.ShopPurchaseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class StreakFlowService {

    private static final int XP_PER_MINUTE = 10;
    private static final int MINUTES_PER_COIN = 10;
    private static final int DEFAULT_DAILY_GOAL = 3;
    private static final String XP_BOOST_ID = "xp-boost";
    private static final String STREAK_FREEZE_ID = "streak-freeze";
    private static final int XP_BOOST_COST = 30;
    private static final int STREAK_FREEZE_COST = 50;
    private static final int BASE_STREAK_FREEZERS = 0;
    private static final int MAX_PLAYERS = 4;
    private static final int MAX_DURATION_MINUTES = 240;

    private final ExerciseRepository exerciseRepository;
    private final ExerciseExecutionRepository executionRepository;
    private final ShopPurchaseRepository shopPurchaseRepository;
    private final PlayerRepository playerRepository;

    public StreakFlowService(ExerciseRepository exerciseRepository, ExerciseExecutionRepository executionRepository, ShopPurchaseRepository shopPurchaseRepository, PlayerRepository playerRepository) {
        this.exerciseRepository = exerciseRepository;
        this.executionRepository = executionRepository;
        this.shopPurchaseRepository = shopPurchaseRepository;
        this.playerRepository = playerRepository;
    }

    public List<Exercise> getExercises() {
        seedDefaultExercisesIfNeeded();
        return exerciseRepository.findAll();
    }

    public Exercise createExercise(Exercise exercise) {
        if (exercise == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise payload is required");
        }

        if (exercise.getName() == null || exercise.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name is required");
        }

        if (exercise.getCategory() == null || exercise.getCategory().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Category is required");
        }

        if (exercise.getTargetMinutesPerDay() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be positive");
        }

        if (exercise.getTargetMinutesPerDay() > MAX_DURATION_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be at most 240 minutes");
        }

        exercise.setName(exercise.getName().trim());
        exercise.setCategory(exercise.getCategory().trim());
        exercise.setId(null);
        return exerciseRepository.save(exercise);
    }

    @Transactional
    public void deleteExercise(Long id) {
        requirePositiveId(id, "Exercise id");

        if (!exerciseRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found");
        }

        executionRepository.deleteByExerciseId(id);
        exerciseRepository.deleteById(id);
    }

    public List<ExerciseExecution> getExecutions() {
        return executionRepository.findAll();
    }

    public ExerciseExecution completeExercise(CompleteExerciseRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Execution payload is required");
        }

        if (request.exerciseId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exercise id is required");
        }

        requirePositiveId(request.exerciseId(), "Exercise id");

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

        if (duration > MAX_DURATION_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be at most 240 minutes");
        }

        int earnedXp = duration * XP_PER_MINUTE;
        if (activeXpBoostCount() > 0) {
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

    public List<ShopItem> getShopItems() {
        return fallbackShopItems();
    }

    public List<ShopPurchase> getShopPurchases() {
        return shopPurchaseRepository.findAll();
    }

    public ShopPurchase buyShopItem(ShopPurchaseRequest request) {
        if (request == null || request.itemId() == null || request.itemId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Shop item id is required");
        }

        String requestedItemId = request.itemId().trim();
        ShopItem item = getShopItems().stream()
                .filter(shopItem -> shopItem.id().equals(requestedItemId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Shop item not found"));

        if (coinBalance() < item.cost()) {
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

    public ShopPurchase buyStreakFreezer() {
        return buyShopItem(new ShopPurchaseRequest(STREAK_FREEZE_ID));
    }

    public ShopPurchase useShopPurchase(Long id) {
        requirePositiveId(id, "Purchase id");

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

    public List<Exercise> fallbackExercises() {
        return List.of(
                new Exercise(1L, "Joggen", "Cardio", 30),
                new Exercise(2L, "Krafttraining", "Strength", 45),
                new Exercise(3L, "Yoga", "Mobility", 20)
        );
    }

    public List<ShopItem> fallbackShopItems() {
        return List.of(
                new ShopItem(XP_BOOST_ID, "XP Boost", XP_BOOST_COST, "Verdoppelt 24 Stunden lang die XP aller bestätigten Übungen."),
                new ShopItem(STREAK_FREEZE_ID, "StreakFreeze", STREAK_FREEZE_COST, "Schützt deine Streak, wenn du einen Tag verpasst.")
        );
    }

    public UserProgress fallbackProgress() {
        return new UserProgress(0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, null);
    }

    public List<Player> getPlayers() {
        return playerRepository.findAll();
    }

    public Player createPlayer(CreatePlayerRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player payload is required");
        }

        if (request.name() == null || request.name().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Player name is required");
        }

        if (playerRepository.count() >= MAX_PLAYERS) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A team can have at most four players");
        }

        return playerRepository.save(new Player(null, request.name().trim(), 0, 0, 0));
    }

    public Player completePlayerWorkout(Long id, PlayerWorkoutRequest request) {
        requirePositiveId(id, "Player id");

        Player player = playerRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));
        int duration = request == null || request.duration() == null ? 30 : request.duration();
        if (duration < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be positive");
        }

        if (duration > MAX_DURATION_MINUTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duration must be at most 240 minutes");
        }

        player.addWorkout(duration, duration * XP_PER_MINUTE, Math.max(1, duration / MINUTES_PER_COIN));
        return playerRepository.save(player);
    }

    public void deletePlayer(Long id) {
        requirePositiveId(id, "Player id");

        if (!playerRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        playerRepository.deleteById(id);
    }

    private int availableCoins() {
        return coinBalance();
    }

    private int coinBalance() {
        List<CoinLedgerEntry> entries = new ArrayList<>();
        executionRepository.findAll().forEach(execution ->
                entries.add(new CoinLedgerEntry(
                        execution.getDate().atStartOfDay(),
                        execution.getId() == null ? 0 : execution.getId(),
                        execution.getEarnedCoins()
                ))
        );
        shopPurchaseRepository.findAll().forEach(purchase ->
                entries.add(new CoinLedgerEntry(
                        purchase.getPurchasedAt() == null ? LocalDateTime.MIN : purchase.getPurchasedAt(),
                        purchase.getId() == null ? 0 : purchase.getId(),
                        -purchase.getCost()
                ))
        );

        entries.sort(Comparator
                .comparing(CoinLedgerEntry::dateTime)
                .thenComparing(CoinLedgerEntry::id));

        int balance = 0;
        for (CoinLedgerEntry entry : entries) {
            if (entry.amount() >= 0) {
                balance += entry.amount();
                continue;
            }

            int cost = Math.abs(entry.amount());
            if (balance >= cost) {
                balance -= cost;
            }
        }

        return balance;
    }

    private record CoinLedgerEntry(LocalDateTime dateTime, Long id, int amount) {
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
                return streak;
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

    private void requirePositiveId(Long id, String label) {
        if (id == null || id < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " must be positive");
        }
    }

    private void seedDefaultExercisesIfNeeded() {
        if (exerciseRepository.count() > 0) {
            return;
        }

        exerciseRepository.saveAll(fallbackExercises().stream()
                .map(exercise -> new Exercise(null, exercise.getName(), exercise.getCategory(), exercise.getTargetMinutesPerDay()))
                .toList());
    }
}
