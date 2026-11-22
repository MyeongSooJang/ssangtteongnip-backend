package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.dto.InterviewResponse;
import com.ssangtteongnip.application.service.request.InterviewQuestionRequest;
import com.ssangtteongnip.application.service.request.InterviewRequest;
import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.domain.interview.InterviewRepository;
import com.ssangtteongnip.domain.jobapplication.JobApplication;
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
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final JobApplicationService jobApplicationService;
    private final Clock clock;

    @Transactional
    public InterviewResponse register(Long userId, Long applicationId, InterviewRequest request) {
        JobApplication application = jobApplicationService.findById(userId, applicationId);
        Interview interview = Interview.create(application, request.round(), request.interviewDate(),
                request.location(), request.note());
        interview.evaluate(request.score());
        application.startInterview(LocalDateTime.now(clock));
        return InterviewResponse.convert(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse evaluate(Long userId, Long applicationId, Long interviewId, Integer score) {
        Interview interview = findInterview(userId, applicationId, interviewId);
        interview.evaluate(score);
        return InterviewResponse.convert(interview);
    }

    public List<InterviewResponse> findInterviews(Long userId, Long applicationId) {
        jobApplicationService.findById(userId, applicationId);
        return interviewRepository.findByApplicationId(applicationId).stream()
                .map(InterviewResponse::convert)
                .toList();
    }

    @Transactional
    public InterviewResponse addQuestion(Long userId, Long applicationId, Long interviewId,
                                         InterviewQuestionRequest request) {
        Interview interview = findInterview(userId, applicationId, interviewId);
        interview.addQuestion(request.question(), request.answer(), request.memo());
        return InterviewResponse.convert(interview);
    }

    private Interview findInterview(Long userId, Long applicationId, Long interviewId) {
        jobApplicationService.findById(userId, applicationId);
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND));
        if (!interview.getApplication().getId().equals(applicationId)) {
            throw new BusinessException(ErrorCode.INTERVIEW_NOT_FOUND);
        }
        return interview;
    }
}
