package com.ssangtteongnip.application.service;

import com.ssangtteongnip.application.service.command.WriteReviewCommand;
import com.ssangtteongnip.domain.review.Review;
import com.ssangtteongnip.domain.review.ReviewRepository;
import com.ssangtteongnip.global.exception.BusinessException;
import com.ssangtteongnip.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public List<Review> findAll(Long userId) {
        return reviewRepository.findByApplicationUserId(userId);
    }

    @Transactional
    public Review write(Long userId, Long id, WriteReviewCommand command) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));
        review.verifyOwner(userId);
        review.compose(command.content(), command.rating(), command.difficulty(), command.atmosphere(),
                command.route(), command.questions());
        return review;
    }
}
