package com.ssangtteongnip.web.auth;

import com.ssangtteongnip.application.service.command.SignupCommand;
import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "이름을 입력해주세요.") String name,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password) {

    public SignupCommand toCommand() {
        return new SignupCommand(email, name, password);
    }
}
