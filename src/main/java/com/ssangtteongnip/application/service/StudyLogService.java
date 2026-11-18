package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.HeatmapDayResponse;
import com.ssangtteongnip.application.service.dto.StudyLogResponse;
import com.ssangtteongnip.application.service.dto.StudyStatsResponse;
import com.ssangtteongnip.application.service.request.StudyLogCreateRequest;
import com.ssangtteongnip.application.service.request.StudyLogRequest;
import com.ssangtteongnip.domain.studylog.StudyLog;
import com.ssangtteongnip.domain.studylog.StudyLogRepository;
import com.ssangtteongnip.domain.studylog.StudyLogs;
import com.ssangtteongnip.domain.studylog.StudyPeriod;
import com.ssangtteongnip.domain.studylog.StudyStreak;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StudyLogService {

    private static final int MAX_HEATMAP_DAYS = 400;
    private static final int MAX_LIST_DAYS = 62;

    private final StudyLogRepository studyLogRepository;
    private final StudyImageService studyImageService;
    private final Clock clock;

    @Transactional
    public StudyLogResponse create(Long userId, StudyLogCreateRequest request) {
        StudyLog created = StudyLog.create(userId, request.studyDate(), request.title(), request.content(),
                request.studyMinutes(), findToday());
        return StudyLogResponse.convert(studyLogRepository.save(created));
    }

    @Transactional
    public StudyLogResponse rewrite(Long userId, Long id, StudyLogRequest request) {
        StudyLog studyLog = findOrThrow(userId, id);
        Set<UUID> removedImages = studyLog.findRemovedImageIds(request.content());
        studyLog.write(request.title(), request.content(), request.studyMinutes());
        studyImageService.deleteUnused(userId, removedImages);
        return StudyLogResponse.convert(studyLog);
    }

    public StudyLogResponse find(Long userId, Long id) {
        return StudyLogResponse.convert(findOrThrow(userId, id));
    }

    public List<StudyLogResponse> findRange(Long userId, LocalDate from, LocalDate to) {
        StudyPeriod period = StudyPeriod.createWithin(from, to, MAX_LIST_DAYS);
        return studyLogRepository
                .findInPeriod(userId, period.from(), period.to())
                .stream().map(StudyLogResponse::convert).toList();
    }

    @Transactional
    public void delete(Long userId, Long id) {
        StudyLog studyLog = findOrThrow(userId, id);
        Set<UUID> images = studyLog.findImageIds();
        studyLogRepository.delete(studyLog);
        studyImageService.deleteUnused(userId, images);
    }

    public List<HeatmapDayResponse> findHeatmap(Long userId, LocalDate from, LocalDate to) {
        StudyPeriod period = StudyPeriod.createWithin(from, to, MAX_HEATMAP_DAYS);
        StudyLogs logs = new StudyLogs(studyLogRepository
                .findInPeriod(userId, period.from(), period.to()));
        return logs.summarizeDaily().stream().map(HeatmapDayResponse::convert).toList();
    }

    public StudyStatsResponse findStats(Long userId) {
        LocalDate today = findToday();
        YearMonth month = YearMonth.from(today);

        StudyStreak streak = new StudyStreak(studyLogRepository.findStudyDatesByUserId(userId));
        StudyLogs todayLogs = new StudyLogs(studyLogRepository
                .findInPeriod(userId, today, today));
        long monthWrittenDays = studyLogRepository
                .countStudyDatesBetween(userId, month.atDay(1), month.atEndOfMonth());

        return new StudyStatsResponse(streak.calculateCurrent(today), streak.calculateLongest(),
                (int) monthWrittenDays, todayLogs.calculateMinutesOn(today));
    }

    private StudyLog findOrThrow(Long userId, Long id) {
        return studyLogRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.STUDY_LOG_NOT_FOUND));
    }

    private LocalDate findToday() {
        return LocalDate.now(clock);
    }
}
