package com.theflexproject.thunder.model;

public class TvCredit {
    private int id;
    private String name;
    private String job;
    private String posterPath;

    public TvCredit(int id, String name, String job, String posterPath) {
        this.id = id;
        this.name = name;
        this.job = job;
        this.posterPath = posterPath;
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

    public String getPosterPath() {
        return posterPath;
    }
}
