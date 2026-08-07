package com.pizzastudio.eum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 이음 — 소상공인 지원금 신청·심사 시스템.
 *
 * <p>한 덩어리로 배포된다. 화면·API·배치·연계가 모두 이 프로세스 안에 있다.</p>
 */
@EnableScheduling
@SpringBootApplication
public class EumApplication {

    public static void main(String[] args) {
        SpringApplication.run(EumApplication.class, args);
    }
}
