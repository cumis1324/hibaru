package com.theflexproject.thunder.model;

public class MediaItem {
    private String fileName;
    private String filePath;

    public MediaItem(String fileName, String filePath) {
        this.fileName = fileName;
        this.filePath = filePath;
    }

    public String getFName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }
}
