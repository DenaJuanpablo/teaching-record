package com.web.dto;

public class DeleteRecordView {
    public Long id;
    public Boolean deleted;

    public DeleteRecordView() {}

    public DeleteRecordView(Long id, Boolean deleted) {
        this.id = id;
        this.deleted = deleted;
    }
}