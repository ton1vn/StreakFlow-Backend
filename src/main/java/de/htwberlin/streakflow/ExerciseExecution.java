package de.htwberlin.streakflow;

public class ExerciseExecution {

    private Long id;
    private String date;
    private int duration;
    private Long exerciseId;

    public ExerciseExecution(Long id, String date, int duration, Long exerciseId) {
        this.id = id;
        this.date = date;
        this.duration = duration;
        this.exerciseId = exerciseId;
    }

    public Long getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public int getDuration() {
        return duration;
    }

    public Long getExerciseId() {
        return exerciseId;
    }
}