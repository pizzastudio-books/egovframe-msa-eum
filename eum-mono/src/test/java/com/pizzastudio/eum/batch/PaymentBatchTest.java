package com.pizzastudio.eum.batch;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.pizzastudio.eum.application.api.dto.ApplicationResponseDto;
import com.pizzastudio.eum.application.api.dto.ApplicationSaveRequestDto;
import com.pizzastudio.eum.application.domain.ApplicationRepository;
import com.pizzastudio.eum.application.domain.ApplicationStatus;
import com.pizzastudio.eum.application.service.ApplicationService;
import com.pizzastudio.eum.payment.service.PaymentService;
import com.pizzastudio.eum.review.api.dto.ReviewRequestDto;
import com.pizzastudio.eum.review.service.ReviewService;

/**
 * 야간 배치.
 *
 * <p>지금은 앱 안에서 {@code @Scheduled} 로 돈다. 11.5 에서 CronJob 으로 떼어낼 때
 * 같은 코드가 그대로 쓰이는지 확인한다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentBatchTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ApplicationRepository applicationRepository;

    @Test
    @DisplayName("시험에서는 스케줄이 등록돼 있고, run-once 실행기는 꺼져 있다")
    void 배치_빈_구성() {
        assertThat(applicationContext.getBeanNamesForType(PaymentBatch.class)).hasSize(1);
        assertThat(applicationContext.containsBean("runPaymentBatchOnce")).isFalse();
    }

    @Test
    @DisplayName("배치를 한 번 돌리면 선정 건이 지급완료가 된다")
    @WithMockUser(username = "admin", roles = "ADMIN")
    void 배치_실행() {
        ApplicationResponseDto saved = applicationService.apply(ApplicationSaveRequestDto.builder()
            .programId(1L)
            .amount(1_000_000L)
            .purposeContent("운영자금")
            .accountNo("110-000-000000")
            .applicantId("user1")
            .build());
        reviewService.review(saved.getApplicationId(),
            ReviewRequestDto.builder().resultId(ReviewService.RESULT_APPROVE).opinion("적격").build());

        PaymentBatch.execute(paymentService, 500);

        assertThat(applicationRepository.findById(saved.getApplicationId()).orElseThrow().getStatusId())
            .isEqualTo(ApplicationStatus.PAID.getKey());
    }
}
