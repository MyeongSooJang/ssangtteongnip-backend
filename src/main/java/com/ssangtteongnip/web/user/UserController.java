package com.ssangtteongnip.web.user;

import com.ssangtteongnip.application.service.UserService;
import com.ssangtteongnip.application.service.dto.NicknameResponse;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/nickname")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ApiResponse<NicknameResponse> findNickname(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(userService.findNickname(userId));
    }

    @PatchMapping
    public ApiResponse<Void> changeNickname(@AuthenticationPrincipal Long userId,
                                            @RequestBody @Valid NicknameRequest request) {
        userService.changeNickname(userId, request.nickname());
        return ApiResponse.ok();
    }
}
