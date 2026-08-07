package com.pizzastudio.eum.core.application.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.pizzastudio.eum.core.common.exception.BusinessMessageException;

/**
 * 신청 도메인 규칙.
 */
class ApplicationTest {

    private Application application(String statusId) {
        return Application.builder()
            .applicationId("app-1")
            .programId(1L)
            .applicantId("user1")
            .amount(1000L)
            .statusId(statusId)
            .accountNo("110-000-000000")
            .build();
    }

    @Test
    @DisplayName("신청자 본인만 자기 신청으로 인정된다")
    void 신청자_판정() {
        Application application = application(ApplicationStatus.REQUEST.getKey());

        assertThat(application.isApplicant("user1")).isTrue();
        assertThat(application.isApplicant("user2")).isFalse();
    }

    @Test
    @DisplayName("선정하면 반려 사유가 지워진다")
    void 선정_처리() {
        Application application = application(ApplicationStatus.REQUEST.getKey());
        application.reject("서류 미비");

        application.approve();

        assertThat(application.getStatusId()).isEqualTo(ApplicationStatus.APPROVE.getKey());
        assertThat(application.getRejectReason()).isNull();
    }

    @Test
    @DisplayName("반려하면 사유가 남는다")
    void 반려_처리() {
        Application application = application(ApplicationStatus.REQUEST.getKey());

        application.reject("서류 미비");

        assertThat(application.getStatusId()).isEqualTo(ApplicationStatus.REJECT.getKey());
        assertThat(application.getRejectReason()).isEqualTo("서류 미비");
    }

    @Test
    @DisplayName("이미 지급된 신청은 취소할 수 없다")
    void 지급된_신청은_취소_불가() {
        Application application = application(ApplicationStatus.PAID.getKey());

        assertThatThrownBy(() -> application.cancel("변심", "이미 지급된 신청은 취소할 수 없습니다."))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessage("이미 지급된 신청은 취소할 수 없습니다.");
    }

    @Test
    @DisplayName("접수 상태는 취소된다")
    void 접수_상태_취소() {
        Application application = application(ApplicationStatus.REQUEST.getKey());

        application.cancel("변심", "이미 지급된 신청은 취소할 수 없습니다.");

        assertThat(application.getStatusId()).isEqualTo(ApplicationStatus.CANCEL.getKey());
    }

    @Test
    @DisplayName("금액 부호를 뒤집어 예산을 되돌린다")
    void 금액_부호_반전() {
        Application application = application(ApplicationStatus.REQUEST.getKey());

        assertThat(application.conversionAmount().getAmount()).isEqualTo(-1000L);
        assertThat(application.conversionAmount().getAmount()).isEqualTo(1000L);
    }

    @Test
    @DisplayName("알 수 없는 상태 코드는 거부한다")
    void 상태_코드_변환() {
        assertThat(ApplicationStatus.of("approve")).isEqualTo(ApplicationStatus.APPROVE);
        assertThatThrownBy(() -> ApplicationStatus.of("unknown"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
