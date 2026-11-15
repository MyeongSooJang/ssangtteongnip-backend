package com.ssangtteongnip.web.auth;

import com.ssangtteongnip.application.service.command.LoginCommand;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password) {

    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
