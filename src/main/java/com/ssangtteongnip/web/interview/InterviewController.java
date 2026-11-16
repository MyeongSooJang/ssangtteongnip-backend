package com.ssangtteongnip.web.interview;

import com.ssangtteongnip.application.service.InterviewService;
import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications/{applicationId}/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Interview> register(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long applicationId,
                                           @RequestBody @Valid InterviewRequest request) {
        return ApiResponse.ok(interviewService.register(userId, applicationId, request.toCommand()));
    }

    @GetMapping
    public ApiResponse<List<Interview>> findAll(@AuthenticationPrincipal Long userId, @PathVariable Long applicationId) {
        return ApiResponse.ok(interviewService.findByApplicationId(userId, applicationId));
    }

    @PatchMapping("/{interviewId}/score")
    public ApiResponse<Interview> evaluate(@AuthenticationPrincipal Long userId,
                                           @PathVariable Long applicationId,
                                           @PathVariable Long interviewId,
                                           @RequestBody @Valid InterviewScoreRequest request) {
        return ApiResponse.ok(interviewService.evaluate(userId, applicationId, interviewId, request.score()));
    }

    @PostMapping("/{interviewId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Interview> addQuestion(@AuthenticationPrincipal Long userId,
                                              @PathVariable Long applicationId,
                                              @PathVariable Long interviewId,
                                              @RequestBody @Valid InterviewQuestionRequest request) {
        return ApiResponse.ok(interviewService.addQuestion(userId, applicationId, interviewId, request.toCommand()));
    }
}
