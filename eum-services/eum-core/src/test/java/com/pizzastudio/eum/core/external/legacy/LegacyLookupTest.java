package com.pizzastudio.eum.core.external.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.pizzastudio.eum.core.common.exception.BusinessMessageException;
import com.pizzastudio.eum.core.member.domain.Member;
import com.pizzastudio.eum.core.member.service.MemberService;

/**
 * 타 기관 데이터베이스 직결 조회.
 *
 * <p>이음이 상대 기관의 테이블을 직접 읽는다는 사실을 시험으로 못 박아 둔다.
 * 13.1에서 이 직결을 연계 API 로 바꾸면 이 시험도 함께 바뀐다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class LegacyLookupTest {

    @Autowired
    private LegacyLookupRepository legacyLookupRepository;

    @Autowired
    private EligibilityChecker eligibilityChecker;

    @Autowired
    private MemberService memberService;

    @Test
    @DisplayName("가상 행정정보에서 사업자 등록 정보를 직접 읽는다")
    void 사업자_정보_조회() {
        BusinessInfo info = legacyLookupRepository.findBusinessInfo("123-45-67890").orElseThrow();

        assertThat(info.getBusinessName()).isEqualTo("가나다상회");
        assertThat(info.isOperating()).isTrue();
    }

    @Test
    @DisplayName("등록되지 않은 사업자는 빈 값이 온다")
    void 없는_사업자() {
        assertThat(legacyLookupRepository.findBusinessInfo("999-99-99999")).isEmpty();
    }

    @Test
    @DisplayName("가상 국세 정보에서 체납액을 직접 읽는다")
    void 체납액_조회() {
        assertThat(legacyLookupRepository.findArrearsAmount("234-56-78901")).isEqualTo(1_500_000L);
        assertThat(legacyLookupRepository.findArrearsAmount("123-45-67890")).isZero();
    }

    @Test
    @DisplayName("영업 중이고 체납이 없으면 자격이 있다")
    void 자격_있음() {
        Member member = memberService.findEntity("user1");

        eligibilityChecker.check(member);
    }

    @Test
    @DisplayName("체납액이 한도 이상이면 자격이 없다")
    void 체납으로_자격_없음() {
        Member member = memberService.findEntity("user2");

        assertThatThrownBy(() -> eligibilityChecker.check(member))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("국세 체납액");
    }

    @Test
    @DisplayName("사업자등록번호가 없으면 자격을 볼 수 없다")
    void 사업자번호_없음() {
        Member admin = memberService.findEntity("admin");

        assertThatThrownBy(() -> eligibilityChecker.check(admin))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("사업자등록번호");
    }
}
