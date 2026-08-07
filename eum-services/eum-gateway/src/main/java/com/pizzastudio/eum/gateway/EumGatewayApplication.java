package com.pizzastudio.eum.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 이음 게이트웨이.
 *
 * <p>모든 요청이 여기를 지난다. 토큰을 검증해 주체를 뒤로 넘기고, 누가 무엇을
 * 했는지 감사 로그로 남긴다. 그 둘만 한다.</p>
 */
@SpringBootApplication
public class EumGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(EumGatewayApplication.class, args);
    }
}
