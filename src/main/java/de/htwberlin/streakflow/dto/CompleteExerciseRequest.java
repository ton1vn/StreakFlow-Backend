package de.htwberlin.streakflow.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CompleteExerciseRequest(
        @NotNull(message = "Exercise id is required")
        @Positive(message = "Exercise id must be positive")
        Long exerciseId,

        @Positive(message = "Duration must be positive")
        @Max(value = 240, message = "Duration must be at most 240 minutes")
        Integer duration
) {
}
