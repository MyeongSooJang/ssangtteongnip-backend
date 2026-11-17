package com.ssangtteongnip.web.profile;

import com.ssangtteongnip.application.service.ProfileService;
import com.ssangtteongnip.application.service.dto.ProfileResponse;
import com.ssangtteongnip.application.service.request.ProfileRequest;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ApiResponse<ProfileResponse> getMyProfile(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(profileService.getMyProfile(userId));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> reviseMyProfile(@AuthenticationPrincipal Long userId,
                                                        @RequestBody @Valid ProfileRequest request) {
        return ApiResponse.ok(profileService.reviseMyProfile(userId, request));
    }
}
