package com.web.repository;

import com.web.model.Record;
import com.web.model.RecordStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface RecordRepository extends JpaRepository<Record, Long>, JpaSpecificationExecutor<Record> {


    @Query("SELECT SUM(r.durationSeconds) FROM Record r")
    Long sumDurationSeconds();


    long countByStatus(RecordStatus status);


    List<Record> findByCreatedAtAfter(LocalDateTime date);
}