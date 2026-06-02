package de.htwberlin.streakflow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.time.LocalDate;

@Entity
public class ExerciseExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDate date;
    private int duration;
    private Long exerciseId;
    private String exerciseName;
    private int earnedXp;
    private int earnedCoins;

    public ExerciseExecution() {
    }

    public ExerciseExecution(Long id, LocalDate date, int duration, Long exerciseId, String exerciseName, int earnedXp, int earnedCoins) {
        this.id = id;
        this.date = date;
        this.duration = duration;
        this.exerciseId = exerciseId;
        this.exerciseName = exerciseName;
        this.earnedXp = earnedXp;
        this.earnedCoins = earnedCoins;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(Long exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public int getEarnedXp() {
        return earnedXp;
    }

    public void setEarnedXp(int earnedXp) {
        this.earnedXp = earnedXp;
    }

    public int getEarnedCoins() {
        return earnedCoins;
    }

    public void setEarnedCoins(int earnedCoins) {
        this.earnedCoins = earnedCoins;
    }
}
