package com.theflexproject.thunder.model;

// Model untuk Cast
public class Cast implements MyMedia {
    private int id;
    private String name;
    private String character;
    private String profilePath;

    public Cast(int id, String name, String character, String profilePath) {
        this.id = id;
        this.name = name;
        this.character = character;
        this.profilePath = profilePath;
    }

    // Getters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCharacter() {
        return character;
    }

    public String getProfilePath() {
        return profilePath;
    }
}
