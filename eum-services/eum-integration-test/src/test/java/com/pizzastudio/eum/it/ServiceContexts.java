package com.pizzastudio.eum.it;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * 서비스 셋을 한 시험 JVM 에 각각 띄운다.
 *
 * <p>서비스마다 독립된 스프링 컨텍스트다. 컴포넌트 스캔 뿌리도, 데이터베이스도, 바인딩도
 * 겹치지 않는다. 배포했을 때와 같은 격리를 시험에서도 지키려는 것이다.</p>
 *
 * <p><b>설정을 여기에 적어 두는 이유가 있다.</b> 서비스마다 자기 {@code application.yml}
 * 을 클래스패스 뿌리에 두는데, 세 서비스를 한 JVM 에 올리면 그 셋이 같은 자리를 다툰다.
 * 먼저 잡히는 하나만 읽히고 나머지는 조용히 무시된다. 그래서 이 시험에서는 각 서비스가
 * 무엇을 듣고 무엇을 내보내는지 손으로 적어 넘긴다. 번거로운 대신 규격이 눈에 보인다.</p>
 *
 * <p>브로커는 띄우지 않는다. Spring Cloud Stream 의 시험 바인더가 큐를 대신한다. 대신
 * <b>서비스 사이를 자동으로 이어 주지는 않는다.</b> 한쪽이 내보낸 메시지를 시험이 직접
 * 꺼내 다른 쪽에 넣는다.</p>
 */
final class ServiceContexts {

    /** 본체 — 선정과 알림 요청을 내보내고, 지급 결과를 받는다 */
    static final String[] CORE = {
        "spring.datasource.url=jdbc:h2:mem:it-core;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.sql.init.schema-locations=classpath:db/eum-core-schema.sql",
        "spring.sql.init.data-locations=classpath:db/eum-core-data.sql",
        "spring.cloud.function.definition=paymentCompleted;paymentFailed",
        "spring.cloud.stream.bindings.application-approved-out-0.destination=application-approved",
        "spring.cloud.stream.bindings.notification-requested-out-0.destination=notification-requested",
        "spring.cloud.stream.bindings.paymentCompleted-in-0.destination=payment-completed",
        "spring.cloud.stream.bindings.paymentFailed-in-0.destination=payment-failed",
    };

    /** 지급 — 선정을 받아 지시를 만들고, 이체 결과를 내보낸다 */
    static final String[] PAYMENT = {
        "spring.datasource.url=jdbc:h2:mem:it-pay;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.sql.init.schema-locations=classpath:db/eum-payment-schema.sql",
        // 비우지 않으면 먼저 잡힌 본체 application.yml 의 값이 남아 본체 자료를 넣으려 든다
        "spring.sql.init.data-locations=",
        "spring.cloud.function.definition=applicationApproved",
        "spring.cloud.stream.bindings.applicationApproved-in-0.destination=application-approved",
        "spring.cloud.stream.bindings.payment-completed-out-0.destination=payment-completed",
        "spring.cloud.stream.bindings.payment-failed-out-0.destination=payment-failed",
    };

    /** 알림 — 알림 요청을 받는다 */
    static final String[] NOTIFICATION = {
        "spring.datasource.url=jdbc:h2:mem:it-noti;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false",
        "spring.sql.init.schema-locations=classpath:db/eum-notification-schema.sql",
        "spring.sql.init.data-locations=",
        "spring.cloud.function.definition=notificationRequested",
        "spring.cloud.stream.bindings.notificationRequested-in-0.destination=notification-requested",
    };

    private ServiceContexts() {
    }

    /**
     * 설정을 명령행 인자로 넘긴다.
     *
     * <p>{@code SpringApplicationBuilder.properties()} 로 넘기면 안 된다. 그쪽은 기본값
     * 자리라 우선순위가 가장 낮아서, 클래스패스에서 먼저 잡힌 {@code application.yml} 에
     * 그대로 덮인다. 지급 컨텍스트가 본체의 데이터베이스를 쓰는 일이 실제로 벌어졌다.
     * 명령행 인자는 파일 설정보다 세다.</p>
     */
    static ConfigurableApplicationContext boot(Class<?> applicationClass, String... properties) {
        String[] args = new String[properties.length];
        for (int i = 0; i < properties.length; i++) {
            args[i] = "--" + properties[i];
        }
        return new SpringApplicationBuilder(
            TestChannelBinderConfiguration.getCompleteConfiguration(applicationClass))
            .web(WebApplicationType.NONE)
            .profiles("it")
            .run(args);
    }

    /** 그 서비스가 내보낸 메시지를 꺼내는 창구 */
    static OutputDestination outbound(ConfigurableApplicationContext context) {
        return context.getBean(OutputDestination.class);
    }

    /** 그 서비스에 메시지를 넣는 창구 */
    static InputDestination inbound(ConfigurableApplicationContext context) {
        return context.getBean(InputDestination.class);
    }
}
