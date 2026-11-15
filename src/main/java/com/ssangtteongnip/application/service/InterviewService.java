package com.ssangtteongnip.application.service;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.domain.interview.Interview;
import com.ssangtteongnip.domain.interview.InterviewRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import com.ssangtteongnip.application.service.command.AddInterviewQuestionCommand;
import com.ssangtteongnip.application.service.command.RegisterInterviewCommand;
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
    public Interview register(Long userId, Long applicationId, RegisterInterviewCommand command) {
        JobApplication application = jobApplicationService.findById(userId, applicationId);
        Interview interview = Interview.create(application, command.round(), command.interviewDate(),
                command.location(), command.note());
        interview.evaluate(command.score());
        application.startInterview(LocalDateTime.now(clock));
        return interviewRepository.save(interview);
    }

    @Transactional
    public Interview evaluate(Long userId, Long applicationId, Long interviewId, Integer score) {
        Interview interview = findInterview(userId, applicationId, interviewId);
        interview.evaluate(score);
        return interview;
    }

    public List<Interview> findByApplicationId(Long userId, Long applicationId) {
        jobApplicationService.findById(userId, applicationId);
        return interviewRepository.findByApplicationId(applicationId);
    }

    @Transactional
    public Interview addQuestion(Long userId, Long applicationId, Long interviewId, AddInterviewQuestionCommand command) {
        Interview interview = findInterview(userId, applicationId, interviewId);
        interview.addQuestion(command.question(), command.answer(), command.memo());
        return interview;
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
