package de.htwberlin.streakflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private int minutes;
    private int xp;
    private int coins;

    public Player() {
    }

    public Player(Long id, String name, int minutes, int xp, int coins) {
        this.id = id;
        this.name = name;
        this.minutes = minutes;
        this.xp = xp;
        this.coins = coins;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getMinutes() {
        return minutes;
    }

    public int getXp() {
        return xp;
    }

    public int getCoins() {
        return coins;
    }

    public void addWorkout(int duration, int earnedXp, int earnedCoins) {
        this.minutes += duration;
        this.xp += earnedXp;
        this.coins += earnedCoins;
    }
}
