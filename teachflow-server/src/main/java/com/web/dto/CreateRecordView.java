package com.web.dto;

public class CreateRecordView {
    public Long id;
    public String status;
    public String createdAt; // yyyy-MM-dd HH:mm:ss
    public String sceneType;

    public CreateRecordView() {}

    public CreateRecordView(Long id, String status, String createdAt, String sceneType) {
        this.id = id;
        this.status = status;
        this.createdAt = createdAt;
        this.sceneType = sceneType;
    }
}