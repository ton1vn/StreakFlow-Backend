package de.htwberlin.streakflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDateTime;

@Entity
public class ShopPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String itemId;
    private String itemName;
    private int cost;
    private LocalDateTime purchasedAt;
    private LocalDateTime usedAt;
    private LocalDateTime expiresAt;

    public ShopPurchase() {
    }

    public ShopPurchase(Long id, String itemId, String itemName, int cost, LocalDateTime purchasedAt, LocalDateTime usedAt, LocalDateTime expiresAt) {
        this.id = id;
        this.itemId = itemId;
        this.itemName = itemName;
        this.cost = cost;
        this.purchasedAt = purchasedAt;
        this.usedAt = usedAt;
        this.expiresAt = expiresAt;
    }

    public Long getId() {
        return id;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public int getCost() {
        return cost;
    }

    public LocalDateTime getPurchasedAt() {
        return purchasedAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
