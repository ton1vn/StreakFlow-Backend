package de.htwberlin.streakflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

public record PlayerWorkoutRequest(
        @Positive(message = "Duration must be positive")
        @Max(value = 240, message = "Duration must be at most 240 minutes")
        Integer duration
) {
}
