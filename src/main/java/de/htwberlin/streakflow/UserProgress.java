package de.htwberlin.streakflow;

public class UserProgress {

    private int currentStreak;
    private int longestStreak;
    private int xp;
    private int level;
    private int coins;
    private int streakFreezers;
    private int completedToday;
    private int dailyGoal;

    public UserProgress(int currentStreak, int longestStreak, int xp, int level, int coins, int streakFreezers, int completedToday, int dailyGoal) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.xp = xp;
        this.level = level;
        this.coins = coins;
        this.streakFreezers = streakFreezers;
        this.completedToday = completedToday;
        this.dailyGoal = dailyGoal;
    }

    public int getCurrentStreak() {
        return currentStreak;
    }

    public int getLongestStreak() {
        return longestStreak;
    }

    public int getXp() {
        return xp;
    }

    public int getLevel() {
        return level;
    }

    public int getCoins() {
        return coins;
    }

    public int getStreakFreezers() {
        return streakFreezers;
    }

    public int getCompletedToday() {
        return completedToday;
    }

    public int getDailyGoal() {
        return dailyGoal;
    }
}