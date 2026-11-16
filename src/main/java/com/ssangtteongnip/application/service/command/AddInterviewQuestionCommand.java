package com.ssangtteongnip.application.service.command;

public record AddInterviewQuestionCommand(
        String question,
        String answer,
        String memo
) {}
