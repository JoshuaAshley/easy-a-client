package com.example.proactive_opsc7311_poe.models;

public class Workout {

    private String workoutID;
    private String name;
    private String description;
    private int progress;
    private int totalExercises;

    private Double totalLoggedTime;

    public Workout(String workoutID, String name, String description, int progress, int totalExercises) {
        this.workoutID = workoutID;
        this.name = name;
        this.description = description;
        this.progress = progress;
        this.totalExercises = totalExercises;
    }

    public String getWorkoutID() {
        return workoutID;
    }

    public void setWorkoutID(String workoutID) {
        this.workoutID = workoutID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getTotalExercises() { return totalExercises; }

    public void setTotalExercises(int totalExercises) { this.totalExercises = totalExercises; }

    public Double getTotalLoggedTime() {
        return totalLoggedTime;
    }

    public void setTotalLoggedTime(Double totalLoggedTime) {
        this.totalLoggedTime = totalLoggedTime;
    }
}
