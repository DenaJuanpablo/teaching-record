package com.web.repository;

import com.web.model.Record;
import com.web.model.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {

    // 1. 统计总时长：给看板用，但查的是 records 表的 duration_seconds 字段
    @Query("SELECT SUM(r.durationSeconds) FROM Record r")
    Long sumDurationSeconds();

    // 2. 按状态统计数量：给看板算成功率用，查的是 records 表的 status 字段
    long countByStatus(RecordStatus status);

    // 3. 查最近几天的记录：给看板画折线图用，查的是 records 表的 created_at 字段
    List<Record> findByCreatedAtAfter(LocalDateTime date);
}