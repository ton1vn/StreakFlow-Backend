package de.htwberlin.streakflow.dto;

import jakarta.validation.constraints.NotBlank;

public record ShopPurchaseRequest(
        @NotBlank(message = "Shop item id is required")
        String itemId
) {
}
