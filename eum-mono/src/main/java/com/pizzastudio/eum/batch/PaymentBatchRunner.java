package com.pizzastudio.eum.batch;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.pizzastudio.eum.payment.service.PaymentService;

/**
 * 배치를 한 번만 돌리고 끝내는 실행기.
 *
 * <p>쿠버네티스 CronJob 이 이 방식으로 부른다(11.5). 웹 서버를 띄우지 않고
 * 배치만 돌린 뒤 프로세스가 끝난다.</p>
 *
 * <pre>
 * java -jar app.jar --spring.main.web-application-type=none --eum.batch.payment.run-once=true
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "eum.batch.payment.run-once", havingValue = "true")
public class PaymentBatchRunner {

    @Value("${eum.batch.payment.limit:500}")
    private int limit;

    @Bean
    ApplicationRunner runPaymentBatchOnce(PaymentService paymentService) {
        return args -> PaymentBatch.execute(paymentService, limit);
    }
}
