package com.ssangtteongnip.web.review;

import com.ssangtteongnip.application.service.ReviewService;
import com.ssangtteongnip.application.service.request.ReviewRequest;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping
    public ApiResponse<List<Review>> findAll(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(reviewService.findAll(userId));
    }

    @PatchMapping("/{id}")
    public ApiResponse<Review> write(@AuthenticationPrincipal Long userId,
                                     @PathVariable Long id,
                                     @RequestBody @Valid ReviewRequest request) {
        return ApiResponse.ok(reviewService.write(userId, id, request));
    }
}
