package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.domain.review.Review;

import java.time.LocalDateTime;
import java.util.List;

// 작성자 본인에게 내려가는 후기. 다른 사용자에게 공개되는 후기는 CompanyReviewResponse다.
public record ReviewResponse(
        Long id,
        JobApplicationResponse application,
        String content,
        Integer rating,
        Integer difficulty,
        InterviewAtmosphere atmosphere,
        ApplyRoute route,
        List<String> questions,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static ReviewResponse convert(Review review) {
        return new ReviewResponse(
                review.getId(),
                JobApplicationResponse.convert(review.getApplication()),
                review.getContent(),
                review.getRating(),
                review.getDifficulty(),
                review.getAtmosphere(),
                review.getRoute(),
                List.copyOf(review.getQuestions()),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }
}
