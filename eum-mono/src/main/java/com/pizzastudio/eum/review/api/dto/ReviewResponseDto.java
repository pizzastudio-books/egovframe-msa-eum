package com.pizzastudio.eum.review.api.dto;

import java.time.LocalDateTime;

import com.pizzastudio.eum.review.domain.Review;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReviewResponseDto {

    private Long reviewId;
    private String applicationId;
    private String reviewerId;
    private String resultId;
    private String opinion;
    private LocalDateTime reviewedAt;

    @Builder
    public ReviewResponseDto(Review entity) {
        this.reviewId = entity.getReviewId();
        this.applicationId = entity.getApplicationId();
        this.reviewerId = entity.getReviewerId();
        this.resultId = entity.getResultId();
        this.opinion = entity.getOpinion();
        this.reviewedAt = entity.getReviewedAt();
    }
}
