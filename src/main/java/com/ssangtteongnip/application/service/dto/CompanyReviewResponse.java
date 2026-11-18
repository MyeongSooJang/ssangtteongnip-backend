package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.jobapplication.JobApplicationStatus;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import com.ssangtteongnip.domain.review.Review;

import java.time.LocalDateTime;
import java.util.List;

// 다른 사용자에게 공개되는 후기. 작성자를 식별할 수 있는 정보(userId 등)는 담지 않는다.
public record CompanyReviewResponse(
        Long id,
        int rating,
        String content,
        String position,
        JobApplicationStatus stage,
        Integer difficulty,
        InterviewAtmosphere atmosphere,
        ApplyRoute route,
        List<String> questions,
        LocalDateTime createdAt) {

    public static CompanyReviewResponse convert(Review review) {
        JobApplication application = review.getApplication();
        return new CompanyReviewResponse(
                review.getId(),
                review.getRating(),
                review.getContent(),
                application.getPosition(),
                application.getStatus(),
                review.getDifficulty(),
                review.getAtmosphere(),
                review.getRoute(),
                List.copyOf(review.getQuestions()),
                review.getCreatedAt());
    }
}
