package com.ssangtteongnip.application.service.command;

public record ConfirmEmailVerificationCommand(
        String email,
        String code
) {}
