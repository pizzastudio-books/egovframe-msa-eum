package com.pizzastudio.eum.review.service;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.application.domain.Application;
import com.pizzastudio.eum.application.service.ApplicationService;
import com.pizzastudio.eum.common.exception.BusinessMessageException;
import com.pizzastudio.eum.notification.service.NotificationService;
import com.pizzastudio.eum.program.domain.Program;
import com.pizzastudio.eum.program.service.ProgramService;
import com.pizzastudio.eum.review.api.dto.ReviewRequestDto;
import com.pizzastudio.eum.review.api.dto.ReviewResponseDto;
import com.pizzastudio.eum.review.domain.Review;
import com.pizzastudio.eum.review.domain.ReviewRepository;

import lombok.RequiredArgsConstructor;

/**
 * 심사.
 *
 * <p>선정하면 신청 상태만 바꾸고, 반려하면 차감했던 예산을 되돌린다. 지급은 심사와 같은
 * 트랜잭션에서 일어나지 않고 야간 배치가 따로 처리한다.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    public static final String RESULT_APPROVE = "approve";
    public static final String RESULT_REJECT = "reject";

    private final ReviewRepository reviewRepository;
    private final ApplicationService applicationService;
    private final ProgramService programService;
    private final NotificationService notificationService;

    public ReviewResponseDto review(String applicationId, ReviewRequestDto requestDto) {
        Application application = applicationService.findEntity(applicationId);

        if (!application.isRequest()) {
            throw new BusinessMessageException("접수 상태인 신청만 심사할 수 있습니다.");
        }

        String result = requestDto.getResultId();
        if (RESULT_APPROVE.equals(result)) {
            application.approve();
            notificationService.notify(application.getApplicantId(), "sms",
                "지원금 신청이 선정되었습니다",
                "신청번호 " + applicationId + " 이 선정되었습니다.");
        } else if (RESULT_REJECT.equals(result)) {
            application.reject(requestDto.getOpinion());
            Program program = programService.findEntity(application.getProgramId());
            program.decreaseBudget(-application.getAmount());
            notificationService.notify(application.getApplicantId(), "sms",
                "지원금 신청이 반려되었습니다",
                "신청번호 " + applicationId + " 이 반려되었습니다. 사유: " + requestDto.getOpinion());
        } else {
            throw new BusinessMessageException("심사 결과는 approve 또는 reject 여야 합니다.");
        }

        Review saved = reviewRepository.save(Review.builder()
            .applicationId(applicationId)
            .reviewerId(currentMemberId())
            .resultId(result)
            .opinion(requestDto.getOpinion())
            .build());

        return ReviewResponseDto.builder().entity(saved).build();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponseDto> findByApplication(String applicationId) {
        return reviewRepository.findByApplicationIdOrderByReviewIdDesc(applicationId).stream()
            .map(review -> ReviewResponseDto.builder().entity(review).build())
            .toList();
    }

    private String currentMemberId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        return authentication.getName();
    }
}
