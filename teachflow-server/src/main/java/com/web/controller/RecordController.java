package com.web.controller;

import com.web.common.ApiResponse;
import com.web.dto.*;
import com.web.model.Record;
import com.web.model.RecordStatus;
import com.web.model.SceneType;
import com.web.service.RecordService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    private static final long MAX_UPLOAD_BYTES = 500L * 1024 * 1024;

    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }


    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreateRecordView> create(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) Integer durationSeconds,
            @RequestParam(required = false) String sceneType,
            @RequestParam(required = false) String sceneMeta
    ) {
        if (file == null || file.isEmpty()) {
            return ApiResponse.fail(1001, "file is required");
        }


        String originalName = file.getOriginalFilename();
        String lowerName = originalName == null ? "" : originalName.toLowerCase();
        if (!(lowerName.endsWith(".mp4")
                || lowerName.endsWith(".mov")
                || lowerName.endsWith(".wav")
                || lowerName.endsWith(".mp3"))) {
            return ApiResponse.fail(1002, "file type not supported");
        }

        if (durationSeconds != null && durationSeconds < 0) {
            return ApiResponse.fail(1001, "durationSeconds must be >= 0");
        }

        SceneType sceneTypeEnum = SceneType.GENERAL;
        if (sceneType != null && !sceneType.trim().isEmpty()) {
            try {
                sceneTypeEnum = SceneType.valueOf(sceneType.trim());
            } catch (Exception e) {
                return ApiResponse.fail(1001, "invalid sceneType");
            }
        }

        if (file.getSize() > MAX_UPLOAD_BYTES) {
            return ApiResponse.fail(1003, "file too large");
        }
        try {
            Record r = recordService.create(file, title, durationSeconds, sceneTypeEnum, sceneMeta);
            return ApiResponse.ok(new CreateRecordView(
                    r.id,
                    r.status == null ? null : r.status.name(),
                    r.createdAt == null ? null : r.createdAt.format(DT_FMT),
                    (r.sceneType == null ? SceneType.GENERAL : r.sceneType).name()
            ));
        } catch (IllegalArgumentException e) {

            return ApiResponse.fail(1001, e.getMessage());
        } catch (Exception e) {
            return ApiResponse.fail(5000, "upload failed: " + e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<ListView<RecordItemView>> list(
            @RequestParam int page,
            @RequestParam int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) String sceneType
    ) {
        if (page < 1 || size < 1) {
            return ApiResponse.fail(1001, "invalid page/size");
        }


        if (status != null && !status.trim().isEmpty()) {
            try {
                RecordStatus.valueOf(status.trim());
            } catch (Exception e) {
                return ApiResponse.fail(1001, "invalid status");
            }
        }


        LocalDateTime from = null;
        LocalDateTime to = null;
        try {
            if (dateFrom != null && !dateFrom.trim().isEmpty()) {
                from = LocalDateTime.parse(dateFrom.trim(), DT_FMT);
            }
            if (dateTo != null && !dateTo.trim().isEmpty()) {
                to = LocalDateTime.parse(dateTo.trim(), DT_FMT);
            }
        } catch (Exception e) {
            return ApiResponse.fail(1001, "invalid dateFrom/dateTo format");
        }

        SceneType sceneTypeEnum = null;
        if (sceneType != null && !sceneType.trim().isEmpty()) {
            try {
                sceneTypeEnum = SceneType.valueOf(sceneType.trim());
            } catch (Exception e) {
                return ApiResponse.fail(1001, "invalid sceneType");
            }
        }

        return ApiResponse.ok(recordService.list(page, size, keyword, status, from, to, sceneTypeEnum));
    }

    @GetMapping("/{id}")
    public ApiResponse<RecordDetailView> get(@PathVariable Long id) {
        Record r = recordService.get(id);
        if (r == null) {
            return ApiResponse.fail(2001, "record not found");
        }

        return ApiResponse.ok(new RecordDetailView(
                r.id,
                r.title,
                r.videoUrl,
                r.durationSeconds,
                r.status == null ? null : r.status.name(),
                r.createdAt == null ? null : r.createdAt.format(DT_FMT),
                r.failedReason,
                (r.sceneType == null ? SceneType.GENERAL : r.sceneType).name(),
                r.sceneMetaJson
        ));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<DeleteRecordView> delete(@PathVariable Long id) {
        Record r = recordService.get(id);
        if (r == null) {
            return ApiResponse.fail(2001, "record not found");
        }




        boolean deleted = recordService.delete(id);
        if (!deleted) {
            return ApiResponse.fail(2001, "record not found");
        }
        return ApiResponse.ok(new DeleteRecordView(id, true));
    }

    @PostMapping("/{id}/process")
    public ApiResponse<ProcessView> process(@PathVariable Long id) {
        try {
            ProcessView pv = recordService.process(id);
            if (pv == null) {
                return ApiResponse.fail(2001, "record not found");
            }
            return ApiResponse.ok(pv);
        } catch (IllegalStateException e) {

            return ApiResponse.fail(2002, "record not operable");
        } catch (Exception e) {
            return ApiResponse.fail(5000, "process failed: " + e.getMessage());
        }
    }


    @GetMapping("/{id}/transcript")
    public ApiResponse<TranscriptView> transcript(@PathVariable Long id) {
        try {
            Record r = recordService.get(id);
            if (r == null) {
                return ApiResponse.fail(2001, "record not found");
            }


            if (r.status == RecordStatus.FAILED) {
                String msg = (r.failedReason == null || r.failedReason.isBlank())
                        ? "transcript failed"
                        : r.failedReason;
                return ApiResponse.fail(3003, msg);
            }


            TranscriptView tv = recordService.getTranscript(id);
            if (tv == null) {
                return ApiResponse.fail(2001, "record not found");
            }


            if (tv.segments == null) {
                return ApiResponse.fail(3001, "transcript not ready");
            }

            return ApiResponse.ok(tv);
        } catch (Exception e) {
            return ApiResponse.fail(5000, "get transcript failed: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/analysis")
    public ApiResponse<AnalysisView> analysis(@PathVariable Long id) {
        try {
            Record r = recordService.get(id);
            if (r == null) {
                return ApiResponse.fail(2001, "record not found");
            }


            if (r.status == RecordStatus.FAILED) {
                String msg = (r.failedReason == null || r.failedReason.isBlank())
                        ? "analysis failed"
                        : r.failedReason;
                return ApiResponse.fail(3004, msg);
            }

            AnalysisView av = recordService.getAnalysis(id);
            if (av == null) {
                return ApiResponse.fail(2001, "record not found");
            }


            if (av.summary == null && av.keywords == null && av.outline == null) {
                return ApiResponse.fail(3002, "analysis not ready");
            }

            return ApiResponse.ok(av);
        } catch (Exception e) {
            return ApiResponse.fail(5000, "get analysis failed: " + e.getMessage());
        }
    }

}