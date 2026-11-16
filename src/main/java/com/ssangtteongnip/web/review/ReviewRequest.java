package com.ssangtteongnip.web.review;

import com.ssangtteongnip.application.service.command.WriteReviewCommand;
import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

// difficulty, atmosphere, route, questions는 선택값이다.
public record ReviewRequest(
        @NotBlank String content,
        @NotNull @Min(1) @Max(5) Integer rating,
        @Min(1) @Max(5) Integer difficulty,
        InterviewAtmosphere atmosphere,
        ApplyRoute route,
        List<String> questions) {

    public WriteReviewCommand toCommand() {
        return new WriteReviewCommand(content, rating, difficulty, atmosphere, route, questions);
    }
}
