package com.web.dto;

public class ProcessView {
    public Long id;
    public String status;

    public ProcessView() {}

    public ProcessView(Long id, String status) {
        this.id = id;
        this.status = status;
    }
}