package com.theflexproject.thunder.model;

public class MovieCredit {
    private int id;
    private String title;
    private String character;
    private String posterPath;

    public MovieCredit(int id, String title, String character, String posterPath) {
        this.id = id;
        this.title = title;
        this.character = character;
        this.posterPath = posterPath;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCharacter() {
        return character;
    }

    public String getPosterPath() {
        return posterPath;
    }
}
