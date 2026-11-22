package com.ssangtteongnip.web.jobapplication;

import com.ssangtteongnip.application.service.JobApplicationService;
import com.ssangtteongnip.application.service.dto.JobApplicationResponse;
import com.ssangtteongnip.application.service.dto.JobSiteStatisticsResponse;
import com.ssangtteongnip.application.service.request.JobApplicationRequest;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class JobApplicationController {

    private final JobApplicationService jobApplicationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<JobApplicationResponse> register(@AuthenticationPrincipal Long userId,
                                                @RequestBody @Valid JobApplicationRequest request) {
        return ApiResponse.ok(jobApplicationService.register(userId, request));
    }

    @GetMapping
    public ApiResponse<List<JobApplicationResponse>> findAll(@AuthenticationPrincipal Long userId,
                                                     @RequestParam(required = false) JobApplicationStatus status) {
        List<JobApplicationResponse> applications = status == null
                ? jobApplicationService.findAll(userId)
                : jobApplicationService.findAll(userId, status);
        return ApiResponse.ok(applications);
    }

    @GetMapping("/stats/job-site")
    public ApiResponse<List<JobSiteStatisticsResponse>> summarizeByJobSite(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(jobApplicationService.summarizeByJobSite(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<JobApplicationResponse> findById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return ApiResponse.ok(jobApplicationService.findDetail(userId, id));
    }

    @PatchMapping("/{id}/job-site")
    public ApiResponse<JobApplicationResponse> changeJobSite(@AuthenticationPrincipal Long userId,
                                                     @PathVariable Long id,
                                                     @RequestBody @Valid JobApplicationJobSiteRequest request) {
        return ApiResponse.ok(jobApplicationService.changeJobSite(userId, id, request.jobSite()));
    }

    @PatchMapping("/{id}/deadline")
    public ApiResponse<JobApplicationResponse> changeDeadline(@AuthenticationPrincipal Long userId,
                                                      @PathVariable Long id,
                                                      @RequestBody @Valid JobApplicationDeadlineRequest request) {
        return ApiResponse.ok(jobApplicationService.changeDeadline(userId, id, request.deadline()));
    }

    @PatchMapping("/{id}/resume-view")
    public ApiResponse<JobApplicationResponse> changeResumeView(@AuthenticationPrincipal Long userId,
                                                        @PathVariable Long id,
                                                        @RequestBody @Valid JobApplicationResumeViewRequest request) {
        return ApiResponse.ok(jobApplicationService.changeResumeView(userId, id, request.resumeView()));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<JobApplicationResponse> decideResult(@AuthenticationPrincipal Long userId,
                                                    @PathVariable Long id,
                                                    @RequestBody @Valid JobApplicationStatusRequest request) {
        return ApiResponse.ok(jobApplicationService.decideResult(userId, id, request.status()));
    }
}
