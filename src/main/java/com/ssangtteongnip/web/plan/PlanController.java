package com.ssangtteongnip.web.plan;

import com.ssangtteongnip.application.service.PlanService;
import com.ssangtteongnip.application.service.dto.FileDownload;
import com.ssangtteongnip.application.service.dto.PlanEvidenceResponse;
import com.ssangtteongnip.application.service.dto.PlanResponse;
import com.ssangtteongnip.application.service.request.PlanCompleteRequest;
import com.ssangtteongnip.application.service.request.PlanCreateRequest;
import com.ssangtteongnip.application.service.request.PlanDetailRequest;
import com.ssangtteongnip.application.service.request.PlanReflectionRequest;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/me/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanResponse> create(@AuthenticationPrincipal Long userId,
                                            @RequestBody @Valid PlanCreateRequest request) {
        return ApiResponse.ok(planService.create(userId, request));
    }

    @PatchMapping("/{id}")
    public ApiResponse<PlanResponse> revise(@AuthenticationPrincipal Long userId,
                                            @PathVariable Long id,
                                            @RequestBody @Valid PlanDetailRequest request) {
        return ApiResponse.ok(planService.revise(userId, id, request));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<PlanResponse> complete(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long id,
                                              @RequestBody @Valid PlanCompleteRequest request) {
        return ApiResponse.ok(planService.complete(userId, id, request));
    }

    @PatchMapping("/{id}/reflection")
    public ApiResponse<PlanResponse> reflect(@AuthenticationPrincipal Long userId,
                                             @PathVariable Long id,
                                             @RequestBody @Valid PlanReflectionRequest request) {
        return ApiResponse.ok(planService.reflect(userId, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        planService.delete(userId, id);
        return ApiResponse.ok();
    }

    @GetMapping
    public ApiResponse<List<PlanResponse>> findRange(@AuthenticationPrincipal Long userId,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.ok(planService.findRange(userId, from, to));
    }

    @PostMapping(value = "/{id}/evidences", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanEvidenceResponse> addLink(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long id,
                                                     @RequestBody @Valid PlanLinkRequest request) {
        return ApiResponse.ok(planService.addLink(userId, id, request.url()));
    }

    @PostMapping(value = "/{id}/evidences", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlanEvidenceResponse> addFile(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long id,
                                                     @RequestPart("file") MultipartFile file) throws IOException {
        return ApiResponse.ok(planService.addFile(userId, id, file.getOriginalFilename(), file.getBytes()));
    }

    @DeleteMapping("/{id}/evidences/{evidenceId}")
    public ApiResponse<Void> removeEvidence(@AuthenticationPrincipal Long userId,
                                            @PathVariable Long id,
                                            @PathVariable Long evidenceId) {
        planService.removeEvidence(userId, id, evidenceId);
        return ApiResponse.ok();
    }

    @GetMapping("/{id}/evidences/{evidenceId}/download")
    public ApiResponse<FileDownload> findEvidenceDownload(@AuthenticationPrincipal Long userId,
                                                          @PathVariable Long id,
                                                          @PathVariable Long evidenceId) {
        return ApiResponse.ok(planService.findEvidenceDownload(userId, id, evidenceId));
    }
}
