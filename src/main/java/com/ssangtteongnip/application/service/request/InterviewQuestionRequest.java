package com.ssangtteongnip.application.service.request;

import jakarta.validation.constraints.NotBlank;

public record InterviewQuestionRequest(
        @NotBlank String question,
        String answer,
        String memo
) {}
