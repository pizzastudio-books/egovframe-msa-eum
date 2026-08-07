package com.pizzastudio.eum.payment.service;

import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 한 번만 돌고 끝나는 배치.
 *
 * <p>크론잡은 <b>끝나는 프로세스</b>를 기대한다. 그런데 같은 이미지를 그대로 띄우면
 * 스프링 부트가 웹 서버를 열고 계속 살아 있다. 잡은 영영 Running 이고, 다음 회차는
 * {@code concurrencyPolicy: Forbid} 때문에 건너뛴다. 그러면 지급이 하루씩 밀리는데
 * 아무도 실패 알림을 받지 못한다 — 실패한 적이 없기 때문이다.</p>
 *
 * <p>실제로 겪었다. 크론잡 매니페스트에 {@code --eum.batch.payment.run-once=true} 를
 * 넘겼는데 앱이 그 값을 몰랐고, 잡 파드는 2분이 지나도 그냥 서버로 떠 있었다.</p>
 *
 * <p>그래서 이 값을 받으면 아웃박스 발송까지 마치고 프로세스를 끝낸다. 배포 워크로드와
 * 배치 워크로드를 이미지 하나로 쓰되, 인자로 갈라 쓰는 방식이다(11.5).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "eum.batch.payment.run-once", havingValue = "true")
public class PaymentBatchRunner implements ApplicationRunner {

    private final PaymentService paymentService;
    private final com.pizzastudio.eum.payment.outbox.PaymentOutbox outbox;
    private final ApplicationContext context;

    @Override
    public void run(ApplicationArguments args) {
        log.info("야간 지급 배치 시작 — 한 번만 돌고 끝냅니다");
        int done = paymentService.executeReady();

        // 결과 이벤트를 여기서 마저 내보낸다. 발송기는 상주 파드에도 있지만, 이 프로세스가
        // 적어 놓고 그냥 끝나면 다음 발송까지 본체가 결과를 모른다.
        int published = outbox.publishPending();

        log.info("야간 지급 배치 종료 — {}건 지급, 결과 이벤트 {}건 발행", done, published);

        // 정상 종료 코드로 끝낸다. 그래야 잡이 Completed 가 된다.
        System.exit(SpringApplication.exit(context, (ExitCodeGenerator) () -> 0));
    }
}
