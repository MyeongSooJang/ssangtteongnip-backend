package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.ScheduleResponse;
import com.ssangtteongnip.application.service.request.ScheduleRequest;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.domain.schedule.Schedule;
import com.ssangtteongnip.domain.schedule.ScheduleRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final JobApplicationService jobApplicationService;
    private final ReviewRepository reviewRepository;
    private final Clock clock;

    @Transactional
    public ScheduleResponse register(Long userId, ScheduleRequest request) {
        JobApplication application = jobApplicationService.findById(userId, request.applicationId());
        Schedule schedule = scheduleRepository.save(
                Schedule.create(application, request.title(), request.scheduledAt(),
                        request.type(), request.address(), request.note())
        );
        if (schedule.isInterview()) {
            application.startInterview(LocalDateTime.now(clock));
            createReviewIfAbsent(application);
        }
        return ScheduleResponse.convert(schedule);
    }

    private void createReviewIfAbsent(JobApplication application) {
        if (!reviewRepository.existsByApplicationId(application.getId())) {
            reviewRepository.save(Review.create(application));
        }
    }

    @Transactional
    public ScheduleResponse updateAddress(Long userId, Long id, String address) {
        Schedule schedule = scheduleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.SCHEDULE_NOT_FOUND));
        schedule.validateOwner(userId);
        schedule.changeAddress(address);
        return ScheduleResponse.convert(schedule);
    }

    // from이나 to가 null이면 그쪽 끝은 제한하지 않는다.
    public List<ScheduleResponse> findAll(Long userId, LocalDateTime from, LocalDateTime to) {
        return scheduleRepository.findInPeriod(userId, from, to).stream()
                .map(ScheduleResponse::convert)
                .toList();
    }
}
