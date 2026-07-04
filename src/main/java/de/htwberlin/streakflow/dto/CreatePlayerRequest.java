package de.htwberlin.streakflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePlayerRequest(
        @NotBlank(message = "Player name is required")
        @Size(max = 60, message = "Player name must be at most 60 characters")
        String name
) {
}
