package de.htwberlin.streakflow.controller;

import de.htwberlin.streakflow.dto.CreatePlayerRequest;
import de.htwberlin.streakflow.dto.PlayerWorkoutRequest;
import de.htwberlin.streakflow.model.Player;
import de.htwberlin.streakflow.service.StreakFlowService;
import org.springframework.http.HttpStatus;
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
public class TeamController {

    private final StreakFlowService streakFlowService;

    public TeamController(StreakFlowService streakFlowService) {
        this.streakFlowService = streakFlowService;
    }

    @GetMapping("/players")
    public List<Player> getPlayers() {
        try {
            return streakFlowService.getPlayers();
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    @PostMapping("/players")
    @ResponseStatus(HttpStatus.CREATED)
    public Player createPlayer(@RequestBody CreatePlayerRequest request) {
        return streakFlowService.createPlayer(request);
    }

    @PostMapping("/players/{id}/workouts")
    public Player completePlayerWorkout(@PathVariable Long id, @RequestBody PlayerWorkoutRequest request) {
        return streakFlowService.completePlayerWorkout(id, request);
    }

    @DeleteMapping("/players/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePlayer(@PathVariable Long id) {
        streakFlowService.deletePlayer(id);
    }
}
