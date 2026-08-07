package com.pizzastudio.eum.core.external.legacy;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.pizzastudio.eum.core.common.exception.BusinessMessageException;

import lombok.extern.slf4j.Slf4j;

/**
 * 타 기관 연계 API 를 부른다.
 *
 * <p>{@link LegacyLookupRepository} 를 대신한다. 상대 기관 데이터베이스에 직접 붙지 않고
 * 상대가 열어 준 API 를 부른다. 13.1 에서 이 전환을 다룬다.</p>
 *
 * <p><b>무엇이 나아지는가.</b> 상대 기관이 표를 바꿔도 이음이 깨지지 않는다. 접속 계정을
 * 받아 둘 필요도 없다. 상대 시스템이 내려가면 여전히 접수가 막히지만, 그때 무슨 일이
 * 벌어지는지는 여기서 눈에 보인다 — 데이터베이스 직결에서는 커넥션 풀이 마르는 형태로
 * 엉뚱한 곳에서 터졌다.</p>
 *
 * <p><b>타임아웃을 반드시 건다.</b> 걸지 않으면 상대가 느려질 때 이음의 요청 스레드가
 * 그만큼 붙잡힌다. 파드를 늘려도 소용없다 — 늘어난 파드도 똑같이 붙잡힌다. 상대가 느릴 때
 * 이음을 지키는 이야기는 13.3 이다.</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "eum.legacy.mode", havingValue = "api")
public class LegacyApiClient implements LegacyLookup {

    private final RestClient restClient;

    public LegacyApiClient(
        RestClient.Builder builder,
        @Value("${eum.legacy.api.base-uri:http://mock-legacy}") String baseUri,
        @Value("${eum.legacy.api.connect-timeout-millis:1000}") long connectTimeout,
        @Value("${eum.legacy.api.read-timeout-millis:2000}") long readTimeout) {

        // SimpleClientHttpRequestFactory 를 쓰지 않는다.
        //
        // 그쪽은 4xx·5xx 응답의 본문을 읽다가 IOException 을 내고, 스프링은 그것을
        // 연결 실패로 감싼다. 그러면 "없는 사업자"(404)가 "상대 시스템이 죽었다"로
        // 둔갑한다. 신청자에게는 자기 사업자번호가 틀렸다는 말 대신 잠시 뒤 다시
        // 시도하라는 말이 나가고, 담당자는 멀쩡한 상대 기관을 의심하게 된다.
        var httpClient = java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .build();
        var factory = new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        this.restClient = builder.clone()
            .baseUrl(baseUri)
            .requestFactory(factory)
            .build();

        log.info("타 기관 연계를 API 로 부릅니다. base={} 연결 {}ms 읽기 {}ms",
            baseUri, connectTimeout, readTimeout);
    }

    @Override
    public Optional<BusinessInfo> findBusinessInfo(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            return Optional.empty();
        }
        try {
            BusinessInfoResponse body = restClient.get()
                .uri("/api/legacy/business/{businessNo}", businessNo)
                .retrieve()
                // 없는 사업자는 오류가 아니다. 빈 결과로 돌려준다.
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> { })
                .body(BusinessInfoResponse.class);

            return body == null || body.businessNo() == null
                ? Optional.empty()
                : Optional.of(body.toDomain());

        } catch (ResourceAccessException e) {
            // 시간 초과와 연결 실패. 상대 시스템이 느리거나 내려간 상태다.
            throw unavailable("사업자 등록 정보", e);
        }
    }

    @Override
    public long findArrearsAmount(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            return 0L;
        }
        try {
            ArrearsResponse body = restClient.get()
                .uri("/api/legacy/arrears/{businessNo}", businessNo)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> { })
                .body(ArrearsResponse.class);

            // 체납 이력이 없는 것과 0 원은 다르지 않다.
            return body == null || body.arrearsAmount() == null ? 0L : body.arrearsAmount();

        } catch (ResourceAccessException e) {
            throw unavailable("국세 체납 정보", e);
        }
    }

    /**
     * 상대가 답하지 않을 때.
     *
     * <p>업무 오류로 바꿔 되돌린다. 스택 추적을 사용자에게 보이지 않으면서, 담당자가
     * 로그에서 원인을 찾을 수 있게 이유는 남긴다.</p>
     */
    private BusinessMessageException unavailable(String what, Exception cause) {
        log.warn("타 기관 연계 실패 — {} : {}", what, cause.getMessage());
        return new BusinessMessageException(
            what + "를 조회하지 못했습니다. 잠시 뒤 다시 시도해 주십시오.");
    }

    /** 연계 규격. 상대가 정한 이름을 그대로 받는다 */
    record BusinessInfoResponse(String businessNo, String businessName,
                                String openDate, String statusCode) {

        BusinessInfo toDomain() {
            return new BusinessInfo(businessNo, businessName,
                openDate == null || openDate.isBlank() ? null : LocalDate.parse(openDate),
                statusCode);
        }
    }

    record ArrearsResponse(String businessNo, Long arrearsAmount) {
    }
}
