package com.ssangtteongnip.web.auth;

import com.ssangtteongnip.application.service.command.ConfirmEmailVerificationCommand;
import jakarta.validation.constraints.NotBlank;

public record EmailVerificationConfirmRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "인증 코드를 입력해주세요.") String code) {

    public ConfirmEmailVerificationCommand toCommand() {
        return new ConfirmEmailVerificationCommand(email, code);
    }
}
