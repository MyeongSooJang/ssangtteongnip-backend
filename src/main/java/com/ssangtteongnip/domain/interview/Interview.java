package com.ssangtteongnip.domain.interview;

import com.ssangtteongnip.domain.jobapplication.JobApplication;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(name = "interviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Interview {

    private static final int MIN_ROUND = 1;
    private static final int MIN_SCORE = 0;
    private static final int MAX_SCORE = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "application_id")
    private JobApplication application;

    private int round;

    private LocalDateTime interviewDate;

    private String location;

    @Column(columnDefinition = "TEXT")
    private String note;

    private Integer score;

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    @Column(updatable = false)
    private LocalDateTime createdAt;

    public static Interview create(JobApplication application, int round,
                                   LocalDateTime interviewDate, String location, String note) {
        validateRound(round);
        Interview interview = new Interview();
        interview.application = application;
        interview.round = round;
        interview.interviewDate = interviewDate;
        interview.location = location;
        interview.note = note;
        return interview;
    }

    // 점수는 선택값이라 null이면 평가 전으로 둔다.
    public void evaluate(Integer score) {
        validateScore(score);
        this.score = score;
    }

    public void addQuestion(String question, String answer, String memo) {
        questions.add(InterviewQuestion.create(this, question, answer, memo));
    }

    public List<InterviewQuestion> getQuestions() {
        return Collections.unmodifiableList(questions);
    }

    private static void validateRound(int round) {
        if (round < MIN_ROUND) {
            throw new BusinessException(ErrorCode.INVALID_INTERVIEW_ROUND);
        }
    }

    private static void validateScore(Integer score) {
        if (score != null && (score < MIN_SCORE || score > MAX_SCORE)) {
            throw new BusinessException(ErrorCode.INVALID_INTERVIEW_SCORE);
        }
    }

    @PrePersist
    private void recordCreation() {
        this.createdAt = LocalDateTime.now();
    }
}
