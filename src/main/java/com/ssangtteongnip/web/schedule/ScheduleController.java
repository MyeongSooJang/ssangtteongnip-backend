package com.ssangtteongnip.web.schedule;

import com.ssangtteongnip.application.service.ScheduleService;
import com.ssangtteongnip.domain.schedule.Schedule;
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
    public ApiResponse<Schedule> register(@AuthenticationPrincipal Long userId,
                                          @RequestBody @Valid ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.register(userId, request.toCommand()));
    }

    @PatchMapping("/{id}/address")
    public ApiResponse<Schedule> updateAddress(@AuthenticationPrincipal Long userId,
                                               @PathVariable Long id,
                                               @RequestBody @Valid ScheduleAddressRequest request) {
        return ApiResponse.ok(scheduleService.updateAddress(userId, id, request.address()));
    }

    @GetMapping
    public ApiResponse<List<Schedule>> findAll(
            @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        if (from != null && to != null) {
            return ApiResponse.ok(scheduleService.findByPeriod(userId, from, to));
        }
        return ApiResponse.ok(scheduleService.findAll(userId));
    }
}
