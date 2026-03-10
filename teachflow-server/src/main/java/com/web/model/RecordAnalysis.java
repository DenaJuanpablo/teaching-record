package com.web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "record_analyses")
public class RecordAnalysis {

    // 一条 record 对应一条 analysis，用 recordId 作为主键
    @Id
    @Column(name = "record_id")
    public Long recordId;

    @Lob
    @Column(name = "analysis_json", nullable = true, columnDefinition = "LONGTEXT")
    public String analysisJson;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public RecordAnalysis() {}

    public RecordAnalysis(Long recordId, String analysisJson, LocalDateTime updatedAt) {
        this.recordId = recordId;
        this.analysisJson = analysisJson;
        this.updatedAt = updatedAt;
    }
}