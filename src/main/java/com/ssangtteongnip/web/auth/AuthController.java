package com.ssangtteongnip.web.auth;

import com.ssangtteongnip.application.service.AuthService;
import com.ssangtteongnip.application.service.EmailVerificationService;
import com.ssangtteongnip.application.service.dto.TokenResponse;
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
    public void sendVerificationCode(@RequestBody @Valid EmailVerificationRequest request) {
        emailVerificationService.send(request.email());
    }

    @PostMapping("/email-verifications/confirm")
    public void confirmVerificationCode(@RequestBody @Valid EmailVerificationConfirmRequest request) {
        emailVerificationService.confirm(request.toCommand());
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse signup(@RequestBody @Valid SignupRequest request) {
        return authService.signup(request.toCommand());
    }

    @PostMapping("/login")
    public TokenResponse login(@RequestBody @Valid LoginRequest request) {
        return authService.login(request.toCommand());
    }
}
