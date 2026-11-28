package com.ssangtteongnip.web.schedule;

import com.ssangtteongnip.application.service.ScheduleService;
import com.ssangtteongnip.application.service.dto.ScheduleResponse;
import com.ssangtteongnip.application.service.request.ScheduleRequest;
import com.ssangtteongnip.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> register(@AuthenticationPrincipal Long userId,
                                          @RequestBody @Valid ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.register(userId, request));
    }

    @PatchMapping("/{id}/address")
    public ApiResponse<ScheduleResponse> updateAddress(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long id,
                                               @RequestBody @Valid ScheduleAddressRequest request) {
        return ApiResponse.ok(scheduleService.updateAddress(userId, id, request.address()));
    }

    @GetMapping
    public ApiResponse<List<ScheduleResponse>> findAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ApiResponse.ok(scheduleService.findAll(userId, from, to));
    }
}
