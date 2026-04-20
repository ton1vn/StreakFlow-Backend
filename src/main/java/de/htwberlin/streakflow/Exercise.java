package de.htwberlin.streakflow;

public class Exercise {

    private Long id;
    private String name;
    private String category;
    private int targetMinutesPerDay;

    public Exercise(Long id, String name, String category, int targetMinutesPerDay) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.targetMinutesPerDay = targetMinutesPerDay;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public int getTargetMinutesPerDay() {
        return targetMinutesPerDay;
    }
}