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

    // 新增：视频/音频可访问地址（相对路径或签名URL）
    @Column(nullable = true)
    public String videoUrl;

    // 新增：时长（秒），可选，所以用 Integer（允许为 null）
    @Column(nullable = true)
    public Integer durationSeconds;

    // 场景元信息（JSON 字符串），用于存作业检查/答辩等具体信息
    @Lob
    @Column(nullable = true, columnDefinition = "LONGTEXT")
    public String sceneMetaJson;

    // 场景类型：用于按类型筛选（可为空，兼容存量数据；空按 GENERAL 处理）
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    public SceneType sceneType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public RecordStatus status;

    @Column(nullable = false)
    public LocalDateTime createdAt;

    // 新增：失败原因（可选）
    @Column(nullable = true, length = 1000)
    public String failedReason;

    // JPA 必须要有无参构造
    public Record() {}

    // 保留你现有构造（不破坏当前 create(title) 的代码）
    public Record(Long id, String title, RecordStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.createdAt = createdAt;
    }

    // 可选：新增一个全量构造，后面上传接口会更方便（不加也行）
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