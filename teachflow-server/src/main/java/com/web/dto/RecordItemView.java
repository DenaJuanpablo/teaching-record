package com.web.dto;

public class RecordItemView {
    public Long id;
    public String title;
    public Integer durationSeconds;
    public String status;
    public String createdAt; // yyyy-MM-dd HH:mm:ss
    public String sceneType;

    public RecordItemView() {}

    public RecordItemView(Long id, String title, Integer durationSeconds, String status, String createdAt, String sceneType) {
        this.id = id;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.createdAt = createdAt;
        this.sceneType = sceneType;
    }
}