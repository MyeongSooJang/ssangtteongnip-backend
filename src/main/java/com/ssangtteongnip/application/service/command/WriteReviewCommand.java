package com.ssangtteongnip.application.service.command;

import com.ssangtteongnip.domain.review.ApplyRoute;
import com.ssangtteongnip.domain.review.InterviewAtmosphere;

import java.util.List;

public record WriteReviewCommand(
        String content,
        Integer rating,
        Integer difficulty,
        InterviewAtmosphere atmosphere,
        ApplyRoute route,
        List<String> questions
) {}
