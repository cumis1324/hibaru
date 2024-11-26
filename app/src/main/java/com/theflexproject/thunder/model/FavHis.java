package com.theflexproject.thunder.model;

import java.util.ArrayList;
import java.util.List;

public class FavHis {
    private List<String> favorit;
    private List<String> history;

    // Konstruktor tanpa parameter
    public FavHis() {
        this.favorit = new ArrayList<>();
        this.history = new ArrayList<>();
    }

    // Konstruktor dengan parameter
    public FavHis(List<String> favorit, List<String> history) {
        this.favorit = favorit != null ? favorit : new ArrayList<>();
        this.history = history != null ? history : new ArrayList<>();
    }

    // Getter dan Setter
    public List<String> getFavorit() {
        return favorit;
    }

    public void setFavorit(List<String> favorit) {
        this.favorit = favorit != null ? favorit : new ArrayList<>();
    }

    public List<String> getHistory() {
        return history;
    }

    public void setHistory(List<String> history) {
        this.history = history != null ? history : new ArrayList<>();
    }

    // Override toString
    @Override
    public String toString() {
        return "FavHis{" +
                "favorit=" + favorit +
                ", history=" + history +
                '}';
    }
}
