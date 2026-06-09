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
    private int minutesToday;
    private int xpToday;
    private int activeXpBoosts;

    public UserProgress(int currentStreak, int longestStreak, int xp, int level, int coins, int streakFreezers, int completedToday, int dailyGoal, int minutesToday, int xpToday, int activeXpBoosts) {
        this.currentStreak = currentStreak;
        this.longestStreak = longestStreak;
        this.xp = xp;
        this.level = level;
        this.coins = coins;
        this.streakFreezers = streakFreezers;
        this.completedToday = completedToday;
        this.dailyGoal = dailyGoal;
        this.minutesToday = minutesToday;
        this.xpToday = xpToday;
        this.activeXpBoosts = activeXpBoosts;
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

    public int getMinutesToday() {
        return minutesToday;
    }

    public int getXpToday() {
        return xpToday;
    }

    public int getActiveXpBoosts() {
        return activeXpBoosts;
    }
}
