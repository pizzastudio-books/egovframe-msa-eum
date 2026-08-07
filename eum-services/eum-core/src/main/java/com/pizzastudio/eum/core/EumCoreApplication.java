package com.pizzastudio.eum.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 이음 본체 — 회원·공통·지원사업·신청·심사.
 *
 * <p>알림과 지급은 각자의 서비스로 떨어져 나갔다. 이 프로세스가 그들에게 보내는 것은
 * 아웃박스를 거친 이벤트뿐이다.</p>
 */
@EnableScheduling
@SpringBootApplication
public class EumCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(EumCoreApplication.class, args);
    }
}
