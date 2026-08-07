package com.pizzastudio.eum.review.domain;

import java.time.LocalDateTime;

import com.pizzastudio.eum.common.domain.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 심사 이력. 신청 한 건에 여러 번 남을 수 있다.
 */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "review")
public class Review extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    @Column(name = "application_id", length = 40, nullable = false)
    private String applicationId;

    @Column(name = "reviewer_id", length = 50, nullable = false)
    private String reviewerId;

    /** approve 또는 reject */
    @Column(name = "result_id", length = 30, nullable = false)
    private String resultId;

    @Column(name = "opinion", length = 4000)
    private String opinion;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Builder
    public Review(Long reviewId, String applicationId, String reviewerId, String resultId,
        String opinion, LocalDateTime reviewedAt) {
        this.reviewId = reviewId;
        this.applicationId = applicationId;
        this.reviewerId = reviewerId;
        this.resultId = resultId;
        this.opinion = opinion;
        this.reviewedAt = reviewedAt == null ? LocalDateTime.now() : reviewedAt;
    }
}
