package com.theflexproject.thunder.model;

public class DownloadItem {
    private String fileName;
    private long downloadId;
    private int progress;

    public DownloadItem(String fileName, long downloadId) {
        this.fileName = fileName;
        this.downloadId = downloadId;
        this.progress = 0;
    }

    public String getFileName() {
        return fileName;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}

