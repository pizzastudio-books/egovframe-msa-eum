package com.pizzastudio.eum.external.legacy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.pizzastudio.eum.common.exception.BusinessMessageException;
import com.pizzastudio.eum.member.domain.Member;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 신청 자격 검증.
 *
 * <p>사업자 등록 상태와 국세 체납 여부를 <b>타 기관 데이터베이스에서 직접 읽어</b> 판정한다.
 * 상대 기관 DB 가 내려가면 이음의 접수도 함께 멈춘다. 그 사실을 1.2에서 문제로 제기하고
 * 13.1에서 연계 API 로 바꾼다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EligibilityChecker {

    private final LegacyLookupRepository legacyLookupRepository;

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

        BusinessInfo businessInfo = legacyLookupRepository
            .findBusinessInfo(applicant.getBusinessNo())
            .orElseThrow(() -> new BusinessMessageException("사업자 등록 정보를 찾을 수 없습니다."));

        if (!businessInfo.isOperating()) {
            throw new BusinessMessageException("영업 중인 사업자만 신청할 수 있습니다.");
        }

        long arrears = legacyLookupRepository.findArrearsAmount(applicant.getBusinessNo());
        if (arrears >= arrearsLimit) {
            throw new BusinessMessageException(
                "국세 체납액이 " + arrearsLimit + "원 이상이면 신청할 수 없습니다. (체납액 " + arrears + "원)");
        }
    }
}
