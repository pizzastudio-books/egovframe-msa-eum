package com.pizzastudio.eum.core.external.legacy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import com.pizzastudio.eum.core.common.exception.BusinessMessageException;
import com.sun.net.httpserver.HttpServer;

/**
 * 연계 API 로 바꾼 뒤의 조회를 확인한다.
 *
 * <p>상대 기관을 흉내 내는 서버를 시험 안에서 띄운다. {@code local/mock-legacy} 와 같은
 * 규격이다. 실습에서는 그쪽을 클러스터에 올려 쓰고, 여기서는 시험이 직접 띄운다.</p>
 *
 * <p><b>느려질 때와 죽었을 때를 함께 본다.</b> 잘 될 때만 확인하면 상대가 답하지 않을 때
 * 무슨 일이 벌어지는지 모른 채로 넘어간다. 실제 사업에서 문제가 되는 쪽은 늘 이쪽이다.</p>
 */
@DisplayName("타 기관 연계 API 조회")
class LegacyApiClientTest {

    private static HttpServer server;
    private static int port;

    /** 시험이 서버 행동을 바꾼다 */
    private static final AtomicLong delayMillis = new AtomicLong(0);
    private static final AtomicInteger forcedStatus = new AtomicInteger(0);

    private LegacyApiClient client;

    @BeforeAll
    static void startMockServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();

        Map<String, String> business = new HashMap<>();
        business.put("123-45-67890",
            "{\"businessNo\":\"123-45-67890\",\"businessName\":\"가나다상회\","
                + "\"openDate\":\"2019-03-02\",\"statusCode\":\"01\"}");
        business.put("345-67-89012",
            "{\"businessNo\":\"345-67-89012\",\"businessName\":\"사아자상사\","
                + "\"openDate\":\"2015-11-30\",\"statusCode\":\"03\"}");

        Map<String, String> arrears = new HashMap<>();
        arrears.put("123-45-67890", "{\"businessNo\":\"123-45-67890\",\"arrearsAmount\":0}");
        arrears.put("234-56-78901", "{\"businessNo\":\"234-56-78901\",\"arrearsAmount\":2500000}");

        server.createContext("/api/legacy/business/", exchange -> {
            String key = exchange.getRequestURI().getPath().substring("/api/legacy/business/".length());
            respond(exchange, business.get(key));
        });
        server.createContext("/api/legacy/arrears/", exchange -> {
            String key = exchange.getRequestURI().getPath().substring("/api/legacy/arrears/".length());
            respond(exchange, arrears.get(key));
        });

        // 스레드 풀을 준다. 기본값은 한 스레드라, 느리게 만든 시험의 요청이 서버를
        // 붙잡아 뒤따르는 시험까지 시간 초과로 실패한다. 처음에 그렇게 겪었다.
        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(4));
        server.start();
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, String body)
        throws IOException {
        try {
            long delay = delayMillis.get();
            if (delay > 0) {
                Thread.sleep(delay);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        int status = forcedStatus.get();
        if (status != 0) {
            byte[] payload = "{\"message\":\"타 기관 시스템 오류\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(status, payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(payload);
            }
            return;
        }

        byte[] payload = (body == null ? "{\"message\":\"없습니다\"}" : body)
            .getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(body == null ? 404 : 200, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    @AfterAll
    static void stopMockServer() {
        server.stop(0);
    }

    @BeforeEach
    void reset() {
        delayMillis.set(0);
        forcedStatus.set(0);
        client = new LegacyApiClient(RestClient.builder(),
            "http://localhost:" + port, 500, 800);
    }

    @Test
    @DisplayName("영업 중인 사업자를 읽어 온다")
    void readsBusinessInfo() {
        assertThat(client.findBusinessInfo("123-45-67890"))
            .get()
            .satisfies(info -> {
                assertThat(info.getBusinessName()).isEqualTo("가나다상회");
                assertThat(info.isOperating()).isTrue();
            });
    }

    @Test
    @DisplayName("폐업한 사업자는 영업 중이 아니다")
    void closedBusinessIsNotOperating() {
        assertThat(client.findBusinessInfo("345-67-89012"))
            .get()
            .satisfies(info -> assertThat(info.isOperating()).isFalse());
    }

    @Test
    @DisplayName("없는 사업자는 오류가 아니라 빈 결과다")
    void unknownBusinessIsEmpty() {
        assertThat(client.findBusinessInfo("999-99-99999")).isEmpty();
    }

    @Test
    @DisplayName("체납액을 읽어 온다")
    void readsArrears() {
        assertThat(client.findArrearsAmount("234-56-78901")).isEqualTo(2_500_000L);
    }

    @Test
    @DisplayName("체납 이력이 없으면 0 원이다")
    void noArrearsHistoryMeansZero() {
        assertThat(client.findArrearsAmount("123-45-67890")).isZero();
    }

    @Test
    @DisplayName("상대가 느리면 기다리지 않고 업무 오류로 끊는다")
    void slowPartnerIsCutOff() {
        delayMillis.set(3_000);   // 읽기 제한은 800ms 다

        long started = System.currentTimeMillis();
        assertThatThrownBy(() -> client.findBusinessInfo("123-45-67890"))
            .isInstanceOf(BusinessMessageException.class)
            .hasMessageContaining("잠시 뒤 다시");

        assertThat(System.currentTimeMillis() - started)
            .as("제한 시간 안에 끊어야 한다. 끊지 않으면 상대가 느린 만큼 이음의 "
                + "요청 스레드가 붙잡히고, 파드를 늘려도 소용없다")
            .isLessThan(2_500);
    }

    @Test
    @DisplayName("상대가 5xx 로 답하면 빈 결과가 아니라 실패로 다룬다")
    void serverErrorIsNotEmptyResult() {
        forcedStatus.set(500);

        assertThatThrownBy(() -> client.findBusinessInfo("123-45-67890"))
            .as("빈 결과로 삼키면 '사업자 등록 정보를 찾을 수 없습니다' 가 되어, "
                + "상대 시스템 장애가 신청자 잘못처럼 보인다")
            .isInstanceOf(RuntimeException.class);
    }
}
