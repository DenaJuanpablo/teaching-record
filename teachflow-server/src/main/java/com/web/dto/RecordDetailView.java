package com.web.dto;

public class RecordDetailView {
    public Long id;
    public String title;
    public String videoUrl;
    public Integer durationSeconds;
    public String status;
    public String createdAt;   // yyyy-MM-dd HH:mm:ss
    public String failedReason;
    public String sceneType;
    public String sceneMeta;

    public RecordDetailView() {}

    public RecordDetailView(Long id, String title, String videoUrl, Integer durationSeconds,
                            String status, String createdAt, String failedReason,
                            String sceneType, String sceneMeta) {
        this.id = id;
        this.title = title;
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.createdAt = createdAt;
        this.failedReason = failedReason;
        this.sceneType = sceneType;
        this.sceneMeta = sceneMeta;
    }
}