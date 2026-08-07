package com.pizzastudio.eum.payment.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.application.api.dto.ApplicationResponseDto;
import com.pizzastudio.eum.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.application.domain.ApplicationRepository;
import com.pizzastudio.eum.application.domain.ApplicationStatus;
import com.pizzastudio.eum.application.service.ApplicationService;
import com.pizzastudio.eum.payment.domain.PaymentStatus;
import com.pizzastudio.eum.review.api.dto.ReviewRequestDto;
import com.pizzastudio.eum.review.service.ReviewService;

/**
 * 야간 배치가 하는 일 — 선정 건을 모아 지급 지시를 만들고 이체한다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentServiceTest {

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ApplicationRepository applicationRepository;

    private ApplicationResponseDto 접수하고_선정한다(String accountNo) {
        ApplicationResponseDto saved = applicationService.apply(ApplicationSaveRequestDto.builder()
            .programId(1L)
            .amount(1_000_000L)
            .purposeContent("운영자금")
            .accountNo(accountNo)
            .applicantId("user1")
            .build());
        reviewService.review(saved.getApplicationId(),
            ReviewRequestDto.builder().resultId(ReviewService.RESULT_APPROVE).opinion("적격").build());
        return saved;
    }

    @Test
    @DisplayName("선정된 신청으로 지급 지시를 만들고 이체하면 지급완료가 된다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 지급_완료() {
        ApplicationResponseDto saved = 접수하고_선정한다("110-000-000000");

        assertThat(paymentService.createInstructions(100)).isEqualTo(1);
        assertThat(paymentService.executeReady()).isEqualTo(1);

        assertThat(paymentService.findByApplication(saved.getApplicationId()).getStatusId())
            .isEqualTo(PaymentStatus.DONE.getKey());
        assertThat(applicationRepository.findById(saved.getApplicationId()).orElseThrow().getStatusId())
            .isEqualTo(ApplicationStatus.PAID.getKey());
    }

    @Test
    @DisplayName("계좌가 비어 있으면 지급에 실패하고 신청 상태는 그대로다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 지급_실패() {
        ApplicationResponseDto saved = 접수하고_선정한다("");

        paymentService.createInstructions(100);
        assertThat(paymentService.executeReady()).isZero();

        assertThat(paymentService.findByApplication(saved.getApplicationId()).getStatusId())
            .isEqualTo(PaymentStatus.FAILED.getKey());
        assertThat(applicationRepository.findById(saved.getApplicationId()).orElseThrow().getStatusId())
            .isEqualTo(ApplicationStatus.APPROVE.getKey());
    }

    @Test
    @DisplayName("같은 신청으로 지급 지시를 두 번 만들지 않는다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 지급_지시_중복_방지() {
        접수하고_선정한다("110-000-000000");

        assertThat(paymentService.createInstructions(100)).isEqualTo(1);
        assertThat(paymentService.createInstructions(100)).isZero();
    }
}
