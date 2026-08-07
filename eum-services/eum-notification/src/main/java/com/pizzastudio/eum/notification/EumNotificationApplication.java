package com.pizzastudio.eum.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 알림 서비스.
 *
 * <p>본체에서 가장 먼저 떼어낸 업무다. 본체는 아웃박스에 요청을 적기만 하고,
 * 이 서비스가 큐에서 꺼내 보낸다. 이 서비스가 죽어 있으면 메시지가 큐에 쌓일 뿐
 * 접수는 그대로 돌아간다.</p>
 */
@SpringBootApplication
public class EumNotificationApplication {

    public static void main(String[] args) {
        SpringApplication.run(EumNotificationApplication.class, args);
    }
}
