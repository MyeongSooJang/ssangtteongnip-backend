package com.ssangtteongnip.web.interview;

import com.ssangtteongnip.application.service.command.AddInterviewQuestionCommand;
import jakarta.validation.constraints.NotBlank;

public record InterviewQuestionRequest(
        @NotBlank String question,
        String answer,
        String memo
) {

    public AddInterviewQuestionCommand toCommand() {
        return new AddInterviewQuestionCommand(question, answer, memo);
    }
}
