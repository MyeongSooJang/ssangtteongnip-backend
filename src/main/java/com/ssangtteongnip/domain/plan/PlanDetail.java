package com.ssangtteongnip.domain.plan;

import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 계획을 만들 때와 고칠 때 함께 바뀌는 내용. 만들 때와 고칠 때 같은 규칙을 지키도록 검증을 한곳에 둔다.
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlanDetail {

    static final int MAX_CONTENT_LENGTH = 50;
    static final int MAX_DONE_CRITERIA_LENGTH = 200;
    static final int MAX_OPTION_LENGTH = 100;
    private static final int MIN_ESTIMATED_MINUTES = 1;

    @Column(nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlanCategory category;

    @Column(nullable = false, length = MAX_DONE_CRITERIA_LENGTH)
    private String doneCriteria;

    @Column(nullable = false)
    private int estimatedMinutes;

    @Column(length = MAX_OPTION_LENGTH)
    private String timeSlot;

    @Column(length = MAX_OPTION_LENGTH)
    private String place;

    @Column(length = MAX_OPTION_LENGTH)
    private String firstAction;

    public PlanDetail(String content, PlanCategory category, String doneCriteria, int estimatedMinutes,
                      String timeSlot, String place, String firstAction) {
        if (content == null || content.isBlank() || content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException(ErrorCode.PLAN_CONTENT_REQUIRED);
        }
        if (category == null) {
            throw new BusinessException(ErrorCode.PLAN_CATEGORY_REQUIRED);
        }
        if (doneCriteria == null || doneCriteria.isBlank() || doneCriteria.length() > MAX_DONE_CRITERIA_LENGTH) {
            throw new BusinessException(ErrorCode.PLAN_DONE_CRITERIA_REQUIRED);
        }
        if (estimatedMinutes < MIN_ESTIMATED_MINUTES) {
            throw new BusinessException(ErrorCode.INVALID_ESTIMATED_MINUTES);
        }
        this.content = content;
        this.category = category;
        this.doneCriteria = doneCriteria;
        this.estimatedMinutes = estimatedMinutes;
        this.timeSlot = normalizeOption(timeSlot);
        this.place = normalizeOption(place);
        this.firstAction = normalizeOption(firstAction);
    }

    // 선택 항목은 비어 있으면 null로 저장한다.
    private static String normalizeOption(String option) {
        if (option == null || option.isBlank()) {
            return null;
        }
        if (option.length() > MAX_OPTION_LENGTH) {
            throw new BusinessException(ErrorCode.PLAN_TEXT_TOO_LONG);
        }
        return option;
    }
}
