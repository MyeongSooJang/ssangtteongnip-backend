package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.interview.Interview;

import java.time.LocalDateTime;
import java.util.List;

// 지원 건 하위 경로(/applications/{id}/interviews)로 조회하므로 지원 건은 담지 않는다.
public record InterviewResponse(
        Long id,
        int round,
        LocalDateTime interviewDate,
        String location,
        String note,
        Integer score,
        List<InterviewQuestionResponse> questions,
        LocalDateTime createdAt) {

    public static InterviewResponse convert(Interview interview) {
        List<InterviewQuestionResponse> questions = interview.getQuestions().stream()
                .map(InterviewQuestionResponse::convert)
                .toList();
        return new InterviewResponse(
                interview.getId(),
                interview.getRound(),
                interview.getInterviewDate(),
                interview.getLocation(),
                interview.getNote(),
                interview.getScore(),
                questions,
                interview.getCreatedAt());
    }
}
