package com.web.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "records")
public class Record {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false)
    public String title;


    @Column(nullable = true)
    public String videoUrl;


    @Column(nullable = true)
    public Integer durationSeconds;


    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    public String sceneMetaJson;


    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    public SceneType sceneType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RecordStatus status;

    @Column(nullable = false)
    public LocalDateTime createdAt;


    @Column(nullable = true, length = 1000)
    public String failedReason;


    public Record() {}


    public Record(Long id, String title, RecordStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.createdAt = createdAt;
    }


    public Record(Long id, String title, String videoUrl, Integer durationSeconds,
                  RecordStatus status, LocalDateTime createdAt, String failedReason) {
        this.id = id;
        this.title = title;
        this.videoUrl = videoUrl;
        this.durationSeconds = durationSeconds;
        this.status = status;
        this.createdAt = createdAt;
        this.failedReason = failedReason;
    }
}