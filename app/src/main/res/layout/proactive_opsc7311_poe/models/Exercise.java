package com.example.proactive_opsc7311_poe.models;

import com.google.firebase.Timestamp;
import com.google.type.Date;

public class Exercise {

    private String exerciseID;
    private String name;
    private String description;
    private String image;
    private Date date;
    private Timestamp startTime;
    private Timestamp endTime;
    private String category;
    private Double min;
    private Double max;
    private Double loggedTime;
    private boolean goalsMet;

    public Exercise(String exerciseID, String name, String description, String image, Date date, Timestamp startTime, Timestamp endTime, String category, Double min, Double max) {
        this.exerciseID = exerciseID;
        this.name = name;
        this.description = description;
        this.image = image;
        this.date = date;
        this.startTime = startTime;
        this.endTime = endTime;
        this.category = category;
        this.min = min;
        this.max = max;
    }

    public String getExerciseID() {
        return exerciseID;
    }

    public void setExerciseID(String exerciseID) {
        this.exerciseID = exerciseID;
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

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(Timestamp startTime) {
        this.startTime = startTime;
    }

    public Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public Double getLoggedTime() {
        return loggedTime;
    }

    public void setLoggedTime(Double loggedTime) {
        this.loggedTime = loggedTime;
    }

    public boolean isGoalsMet() {
        return goalsMet;
    }

    public void setGoalsMet(boolean goalsMet) {
        this.goalsMet = goalsMet;
    }

    // Method to convert java.util.Date to com.google.type.Date
    public static Date convertToGoogleDate(java.util.Date utilDate) {
        if (utilDate == null)
            return null;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(utilDate);
        return Date.newBuilder()
                .setYear(cal.get(java.util.Calendar.YEAR))
                .setMonth(cal.get(java.util.Calendar.MONTH) + 1)
                .setDay(cal.get(java.util.Calendar.DAY_OF_MONTH))
                .build();
    }

    // Method to convert com.google.type.Date to java.util.Date
    public static java.util.Date convertToUtilDate(Date googleDate) {
        if (googleDate == null)
            return null;

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(googleDate.getYear(), googleDate.getMonth() - 1, googleDate.getDay());
        return cal.getTime();
    }
}
