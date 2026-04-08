package com.web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "record_transcripts")
public class RecordTranscript {


    @Id
    @Column(name = "record_id")
    public Long recordId;

    @Lob
    @Column(name = "transcript_json", nullable = true, columnDefinition = "LONGTEXT")
    public String transcriptJson;

    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    public RecordTranscript() {}

    public RecordTranscript(Long recordId, String transcriptJson, LocalDateTime updatedAt) {
        this.recordId = recordId;
        this.transcriptJson = transcriptJson;
        this.updatedAt = updatedAt;
    }
}