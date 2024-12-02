package com.theflexproject.thunder.model;

public class DownloadItem {
    private long downloadId;      // ID unik untuk unduhan dari DownloadManager
    private String title;         // Nama atau judul unduhan
    private String filename;         // Nama atau judul unduhan
    private int status;           // Status unduhan (Running, Paused, Completed, etc.)
    private int progress;         // Progres unduhan dalam persen

    // Constructor
    public DownloadItem(String filename, long downloadId, String title, int status, int progress) {
        this.filename = filename;
        this.downloadId = downloadId;
        this.title = title;
        this.status = status;
        this.progress = progress;
    }

    // Getter dan Setter
    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public String getTitle() {
        return title;
    }
    public String getFilename() {
        return filename;
    }

    public void setTitle(String title) {
        this.title = title;
    }
    public void setFilename(String filename) {
        this.filename = filename;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    // Opsional: Override toString untuk debug
    @Override
    public String toString() {
        return "DownloadItem{" +
                "downloadId=" + downloadId +
                ", title='" + title + '\'' +
                ", status=" + status +
                ", filename=" + filename +
                ", progress=" + progress +
                '}';
    }
}


