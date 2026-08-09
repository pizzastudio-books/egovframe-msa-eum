package com.pizzastudio.eum.core.outbox.service;

import org.springframework.stereotype.Component;

import com.pizzastudio.eum.core.outbox.domain.OutboxEventRepository;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 아직 못 보낸 이벤트 수를 지표로 내보낸다.
 *
 * <p><b>경보를 걸어 두었는데 지표가 없어 영원히 안 울리는 상태였다.</b> 규칙 파일에
 * {@code eum_outbox_pending} 을 적어 두고 그 이름의 지표를 아무도 안 만들었다.
 * 프로메테우스에서 그 이름을 물으면 결과가 0건으로 나온다 — 값이 0인 것이 아니라
 * <b>계열 자체가 없다.</b> 규칙은 조용히 {@code inactive} 로 남는다(19.2).</p>
 *
 * <p>게이지는 값을 저장하지 않는다. 프로메테우스가 긁을 때마다 아래 함수가 불린다.
 * 그래서 <b>세는 비용이 긁는 주기만큼 든다</b> — 15초에 한 번 {@code count} 질의가 나간다.
 * 표가 커지면 이 질의가 부담이 되므로, 그때는 값을 따로 들고 있다가 갱신하는 방식으로
 * 바꾼다.</p>
 */
@Component
public class OutboxMetrics {

    public OutboxMetrics(MeterRegistry registry, OutboxEventRepository repository) {
        Gauge.builder("eum.outbox.pending", repository,
                OutboxEventRepository::countByPublishedAtIsNull)
            .description("아직 브로커로 보내지 못한 이벤트 수")
            .register(registry);
    }
}
