package com.theflexproject.thunder.model;

public class PersonDetails {
    private String name;
    private String biography;
    private String profilePath;
    private String birthday; // Menambahkan atribut birthday

    public PersonDetails(String name, String biography, String profilePath, String birthday) {
        this.name = name;
        this.biography = biography;
        this.profilePath = profilePath;
        this.birthday = birthday; // Menyimpan nilai birthday
    }

    // Getters
    public String getName() {
        return name;
    }

    public String getBiography() {
        return biography;
    }

    public String getProfilePath() {
        return profilePath;
    }

    public String getBirthday() {
        return birthday; // Getter untuk birthday
    }
}