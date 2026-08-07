package com.pizzastudio.eum.core.external.legacy;

import java.util.Optional;

/**
 * 타 기관 정보 조회.
 *
 * <p>구현이 둘이다. 하나는 상대 기관 데이터베이스에 직접 붙고({@link LegacyLookupRepository}),
 * 하나는 연계 API 를 부른다({@link LegacyApiClient}). 13.1 에서 앞의 것을 뒤의 것으로
 * 바꾼다.</p>
 *
 * <p><b>인터페이스를 먼저 둔 이유가 있다.</b> 자격 검증 코드가 어느 쪽인지 모르게 해야
 * 갈아 끼울 때 업무 코드를 건드리지 않는다. 실제 사업에서는 두 방식이 한동안 함께
 * 돌아간다 — 기관마다 연계 API 가 열리는 시점이 다르기 때문이다.</p>
 */
public interface LegacyLookup {

    /** 사업자 등록 정보 */
    Optional<BusinessInfo> findBusinessInfo(String businessNo);

    /** 국세 체납액. 이력이 없으면 0 원이다 */
    long findArrearsAmount(String businessNo);
}
