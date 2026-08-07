package com.pizzastudio.eum.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 지급 서비스.
 *
 * <p>선정 이벤트를 받아 지급 지시를 만들고, 야간에 금융망으로 이체를 요청한 뒤
 * 결과를 다시 이벤트로 돌려준다. 계좌 원본은 여기에만 있다.</p>
 */
@EnableScheduling
@SpringBootApplication
public class EumPaymentApplication {

    public static void main(String[] args) {
        SpringApplication.run(EumPaymentApplication.class, args);
    }
}
