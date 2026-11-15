package com.ssangtteongnip.web.jobapplication;

import com.ssangtteongnip.application.service.JobApplicationService;
import com.ssangtteongnip.application.service.dto.JobSiteStatisticsResponse;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
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
    public JobApplication register(@AuthenticationPrincipal Long userId,
                               @RequestBody @Valid JobApplicationRequest request) {
        return jobApplicationService.register(userId, request.toCommand());
    }

    @GetMapping
    public List<JobApplication> findAll(@AuthenticationPrincipal Long userId,
                                        @RequestParam(required = false) JobApplicationStatus status) {
        return status == null
                ? jobApplicationService.findAll(userId)
                : jobApplicationService.findByStatus(userId, status);
    }

    @GetMapping("/stats/job-site")
    public List<JobSiteStatisticsResponse> summarizeByJobSite(@AuthenticationPrincipal Long userId) {
        return jobApplicationService.summarizeByJobSite(userId);
    }

    @GetMapping("/{id}")
    public JobApplication findById(@AuthenticationPrincipal Long userId, @PathVariable Long id) {
        return jobApplicationService.findById(userId, id);
    }

    @PatchMapping("/{id}/job-site")
    public JobApplication changeJobSite(@AuthenticationPrincipal Long userId,
                                        @PathVariable Long id,
                                        @RequestBody @Valid JobApplicationJobSiteRequest request) {
        return jobApplicationService.changeJobSite(userId, id, request.jobSite());
    }

    @PatchMapping("/{id}/deadline")
    public JobApplication changeDeadline(@AuthenticationPrincipal Long userId,
                                         @PathVariable Long id,
                                         @RequestBody @Valid JobApplicationDeadlineRequest request) {
        return jobApplicationService.changeDeadline(userId, id, request.deadline());
    }

    @PatchMapping("/{id}/resume-view")
    public JobApplication changeResumeView(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long id,
                                           @RequestBody @Valid JobApplicationResumeViewRequest request) {
        return jobApplicationService.changeResumeView(userId, id, request.resumeView());
    }

    @PatchMapping("/{id}/status")
    public JobApplication decideResult(@AuthenticationPrincipal Long userId,
                                       @PathVariable Long id,
                                       @RequestBody @Valid JobApplicationStatusRequest request) {
        return jobApplicationService.decideResult(userId, id, request.status());
    }
}
