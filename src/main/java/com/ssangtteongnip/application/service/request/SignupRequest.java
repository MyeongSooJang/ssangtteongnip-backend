package com.ssangtteongnip.application.service.request;

import jakarta.validation.constraints.NotBlank;

public record SignupRequest(
        @NotBlank(message = "이메일을 입력해주세요.") String email,
        @NotBlank(message = "이름을 입력해주세요.") String name,
        @NotBlank(message = "닉네임을 입력해주세요.") String nickname,
        @NotBlank(message = "비밀번호를 입력해주세요.") String password) {}
