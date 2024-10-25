package com.theflexproject.thunder.model;

public class HistoryEntry {
    public String lastPlayed;
    public long lastPosition;

    // Konstruktor default diperlukan untuk pemetaan data Firebase
    public HistoryEntry() {
    }

    public HistoryEntry(String lastPlayed, long lastPosition) {
        this.lastPlayed = lastPlayed;
        this.lastPosition = lastPosition;
    }
}
