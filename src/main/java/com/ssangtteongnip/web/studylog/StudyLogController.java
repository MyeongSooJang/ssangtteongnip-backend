package com.ssangtteongnip.web.studylog;

import com.ssangtteongnip.application.service.StudyLogService;
import com.ssangtteongnip.application.service.dto.HeatmapDayResponse;
import com.ssangtteongnip.application.service.dto.StudyLogResponse;
import com.ssangtteongnip.application.service.dto.StudyStatsResponse;
import com.ssangtteongnip.application.service.request.StudyLogCreateRequest;
import com.ssangtteongnip.application.service.request.StudyLogRequest;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/me/study-logs")
@RequiredArgsConstructor
public class StudyLogController {

    private final StudyLogService studyLogService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<StudyLogResponse> create(@AuthenticationPrincipal Long userId,
                                                @RequestBody @Valid StudyLogCreateRequest request) {
        return ApiResponse.ok(studyLogService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<StudyLogResponse> rewrite(@AuthenticationPrincipal Long userId,
                                                 @PathVariable Long id,
                                                 @RequestBody @Valid StudyLogRequest request) {
        return ApiResponse.ok(studyLogService.rewrite(userId, id, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<StudyLogResponse> find(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(studyLogService.find(userId, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        studyLogService.delete(userId, id);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<StudyLogResponse>> findRange(@AuthenticationPrincipal Long userId,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(studyLogService.findRange(userId, from, to));
    }

    @GetMapping("/heatmap")
    public ApiResponse<List<HeatmapDayResponse>> findHeatmap(@AuthenticationPrincipal Long userId,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(studyLogService.findHeatmap(userId, from, to));
    }

    @GetMapping("/stats")
    public ApiResponse<StudyStatsResponse> findStats(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(studyLogService.findStats(userId));
    }
}
