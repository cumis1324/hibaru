package com.theflexproject.thunder.model;

// Model untuk Crew
public class Crew implements MyMedia {
    private int id;
    private String name;
    private String job;
    private String profilePath;
    private String department;

    public Crew(int id, String name, String job, String department, String profilePath) {
        this.id = id;
        this.name = name;
        this.job = job;
        this.department = department;
        this.profilePath = profilePath;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJob() {
        return job;
    }
    public String getDepartment() {
        return department;
    }

    public String getProfilePath() {
        return profilePath;
    }
}
