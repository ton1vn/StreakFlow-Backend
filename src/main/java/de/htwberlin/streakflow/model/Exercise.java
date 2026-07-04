package de.htwberlin.streakflow.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Entity
public class Exercise {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @NotBlank(message = "Name is required")
    @Size(max = 80, message = "Name must be at most 80 characters")
    private String name;
    @NotBlank(message = "Category is required")
    @Size(max = 50, message = "Category must be at most 50 characters")
    private String category;
    @Positive(message = "Duration must be positive")
    @Max(value = 240, message = "Duration must be at most 240 minutes")
    private int targetMinutesPerDay;

    public Exercise() {
    }

    public Exercise(Long id, String name, String category, int targetMinutesPerDay) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.targetMinutesPerDay = targetMinutesPerDay;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getTargetMinutesPerDay() {
        return targetMinutesPerDay;
    }

    public void setTargetMinutesPerDay(int targetMinutesPerDay) {
        this.targetMinutesPerDay = targetMinutesPerDay;
    }
}
