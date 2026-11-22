package com.ssangtteongnip.application.service.dto;

import com.ssangtteongnip.domain.interview.InterviewQuestion;

public record InterviewQuestionResponse(Long id, String question, String answer, String memo) {

    public static InterviewQuestionResponse convert(InterviewQuestion question) {
        return new InterviewQuestionResponse(question.getId(), question.getQuestion(), question.getAnswer(),
                question.getMemo());
    }
}
