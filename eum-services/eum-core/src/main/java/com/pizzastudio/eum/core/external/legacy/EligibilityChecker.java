package com.pizzastudio.eum.core.external.legacy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pizzastudio.eum.core.common.exception.BusinessMessageException;
import com.pizzastudio.eum.core.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 신청 자격 검증.
 *
 * <p>사업자 등록 상태와 국세 체납 여부를 상대 기관에서 읽어 판정한다.
 *
 * <p><b>어떻게 읽는지는 이 코드가 모른다.</b> {@link LegacyLookup} 구현이 둘이고
 * {@code eum.legacy.mode} 가 고른다 — 데이터베이스 직결이거나 연계 API 다. 13.1 에서
 * 앞의 것을 뒤의 것으로 바꾸는데, 이 파일은 그때 손대지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EligibilityChecker {

    // 어느 구현이 들어오는지 이 코드는 모른다. eum.legacy.mode 가 정한다(13.1).
    private final LegacyLookup legacyLookup;

    /** 이 금액 이상 체납이면 신청할 수 없다 */
    @Value("${eum.eligibility.arrears-limit:1000000}")
    private long arrearsLimit;

    /** 자격 검증을 켜고 끈다. 상대 기관 점검 시간에 내려 두기 위해 둔 값이다. */
    @Value("${eum.eligibility.enabled:true}")
    private boolean enabled;

    public void check(Member applicant) {
        if (!enabled) {
            log.warn("자격 검증을 건너뜁니다. 타 기관 연계가 내려간 상태입니다.");
            return;
        }
        if (applicant == null || applicant.getBusinessNo() == null) {
            throw new BusinessMessageException("사업자등록번호가 등록되어 있지 않습니다.");
        }

        BusinessInfo businessInfo = legacyLookup
            .findBusinessInfo(applicant.getBusinessNo())
            .orElseThrow(() -> new BusinessMessageException("사업자 등록 정보를 찾을 수 없습니다."));

        if (!businessInfo.isOperating()) {
            throw new BusinessMessageException("영업 중인 사업자만 신청할 수 있습니다.");
        }

        long arrears = legacyLookup.findArrearsAmount(applicant.getBusinessNo());
        if (arrears >= arrearsLimit) {
            throw new BusinessMessageException(
                "국세 체납액이 " + arrearsLimit + "원 이상이면 신청할 수 없습니다. (체납액 " + arrears + "원)");
        }
    }
}
