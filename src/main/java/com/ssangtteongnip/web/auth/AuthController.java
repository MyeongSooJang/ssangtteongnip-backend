package com.ssangtteongnip.web.auth;

import com.ssangtteongnip.application.service.AuthService;
import com.ssangtteongnip.application.service.EmailVerificationService;
import com.ssangtteongnip.application.service.dto.TokenResponse;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    @PostMapping("/email-verifications")
    public ApiResponse<Void> sendVerificationCode(@RequestBody @Valid EmailVerificationRequest request) {
        emailVerificationService.send(request.email());
        return ApiResponse.ok();
    }

    @PostMapping("/email-verifications/confirm")
    public ApiResponse<Void> confirmVerificationCode(@RequestBody @Valid EmailVerificationConfirmRequest request) {
        emailVerificationService.confirm(request.toCommand());
        return ApiResponse.ok();
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TokenResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ApiResponse.ok(authService.signup(request.toCommand()));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@RequestBody @Valid LoginRequest request) {
        return ApiResponse.ok(authService.login(request.toCommand()));
    }
}
