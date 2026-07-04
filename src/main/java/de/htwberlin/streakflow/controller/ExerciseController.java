package de.htwberlin.streakflow.controller;

import de.htwberlin.streakflow.dto.CompleteExerciseRequest;
import de.htwberlin.streakflow.dto.ShopItem;
import de.htwberlin.streakflow.dto.ShopPurchaseRequest;
import de.htwberlin.streakflow.model.Exercise;
import de.htwberlin.streakflow.model.ExerciseExecution;
import de.htwberlin.streakflow.model.ShopPurchase;
import de.htwberlin.streakflow.model.UserProgress;
import de.htwberlin.streakflow.service.StreakFlowService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@Validated
public class ExerciseController {

    private final StreakFlowService streakFlowService;

    public ExerciseController(StreakFlowService streakFlowService) {
        this.streakFlowService = streakFlowService;
    }

    @GetMapping("/exercises")
    public List<Exercise> getExercises() {
        try {
            return streakFlowService.getExercises();
        } catch (RuntimeException exception) {
            return streakFlowService.fallbackExercises();
        }
    }

    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public Exercise createExercise(@Valid @RequestBody Exercise exercise) {
        return streakFlowService.createExercise(exercise);
    }

    @DeleteMapping("/exercises/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExercise(@Positive @PathVariable Long id) {
        streakFlowService.deleteExercise(id);
    }

    @GetMapping("/executions")
    public List<ExerciseExecution> getExecutions() {
        try {
            return streakFlowService.getExecutions();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @PostMapping("/executions")
    @ResponseStatus(HttpStatus.CREATED)
    public ExerciseExecution completeExercise(@Valid @RequestBody CompleteExerciseRequest request) {
        return streakFlowService.completeExercise(request);
    }

    @GetMapping("/shop/items")
    public List<ShopItem> getShopItems() {
        try {
            return streakFlowService.getShopItems();
        } catch (RuntimeException exception) {
            return streakFlowService.fallbackShopItems();
        }
    }

    @GetMapping("/shop/purchases")
    public List<ShopPurchase> getShopPurchases() {
        try {
            return streakFlowService.getShopPurchases();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @PostMapping("/shop/purchases")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopPurchase buyShopItem(@Valid @RequestBody ShopPurchaseRequest request) {
        return streakFlowService.buyShopItem(request);
    }

    @PostMapping("/shop/freezer")
    @ResponseStatus(HttpStatus.CREATED)
    public ShopPurchase buyStreakFreezer() {
        return streakFlowService.buyStreakFreezer();
    }

    @PostMapping("/shop/purchases/{id}/use")
    public ShopPurchase useShopPurchase(@Positive @PathVariable Long id) {
        return streakFlowService.useShopPurchase(id);
    }

    @GetMapping("/progress")
    public UserProgress getProgress() {
        try {
            return streakFlowService.getProgress();
        } catch (RuntimeException exception) {
            return streakFlowService.fallbackProgress();
        }
    }
}
