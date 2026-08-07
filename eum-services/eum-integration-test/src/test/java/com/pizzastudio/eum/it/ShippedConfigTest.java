package com.pizzastudio.eum.it;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

/**
 * 서비스가 실제로 담아 배포하는 설정 파일을 읽어 본다.
 *
 * <p>다른 시험들은 설정을 시험 쪽에서 넣어 준다. 그래야 세 서비스를 한 JVM 에 올릴 수
 * 있기 때문인데, <b>그 편의가 배포되는 설정의 결함을 가린다.</b> 실제로 겪었다 —
 * {@code cloud:} 블록이 {@code spring:} 이 아니라 {@code token:} 아래로 한 칸 들어가
 * 있었다. YAML 로는 멀쩡하고 앱도 정상으로 뜨는데, 브로커 바인딩이 통째로 무시됐다.
 * 본체는 {@code application-approved-out-0} 이라는 이름 그대로를 목적지로 삼아 보냈고,
 * 지급은 {@code application-approved} 를 듣고 있었다. 아무도 못 받는다.</p>
 *
 * <p>시험 63 건이 전부 통과하는 상태였다. 클러스터에 올려서야 드러났다.</p>
 */
@DisplayName("배포되는 설정 파일")
class ShippedConfigTest {

    @Test
    @DisplayName("본체가 보내는 목적지와 지급·알림이 듣는 목적지가 같다")
    void producerAndConsumerAgreeOnDestinations() {
        Map<String, Object> core = load("eum-core");
        Map<String, Object> payment = load("eum-payment");
        Map<String, Object> notification = load("eum-notification");

        assertThat(destinationOf(core, "application-approved-out-0"))
            .as("본체가 선정 이벤트를 보내는 곳")
            .isEqualTo(destinationOf(payment, "applicationApproved-in-0"));

        assertThat(destinationOf(core, "notification-requested-out-0"))
            .as("본체가 알림 요청을 보내는 곳")
            .isEqualTo(destinationOf(notification, "notificationRequested-in-0"));

        assertThat(destinationOf(payment, "payment-completed-out-0"))
            .as("지급이 완료를 알리는 곳")
            .isEqualTo(destinationOf(core, "paymentCompleted-in-0"));

        assertThat(destinationOf(payment, "payment-failed-out-0"))
            .as("지급이 실패를 알리는 곳")
            .isEqualTo(destinationOf(core, "paymentFailed-in-0"));
    }

    @Test
    @DisplayName("바인딩 설정이 spring 아래에 있다")
    void bindingsLiveUnderSpring() {
        for (String service : List.of("eum-core", "eum-payment", "eum-notification")) {
            Map<String, Object> doc = load(service);

            assertThat(at(doc, "spring", "cloud", "stream", "bindings"))
                .as("%s 의 바인딩 설정이 spring 아래에 있어야 한다. 한 칸만 어긋나도 "
                    + "YAML 은 멀쩡하고 앱도 뜨지만 설정이 통째로 무시된다", service)
                .isNotNull();

            assertThat(at(doc, "spring", "rabbitmq", "host"))
                .as("%s 의 브로커 주소가 spring 아래에 있어야 한다", service)
                .isNotNull();
        }
    }

    @Test
    @DisplayName("듣는 쪽 함수 이름이 바인딩 이름과 맞는다")
    void functionDefinitionMatchesBindings() {
        for (String service : List.of("eum-core", "eum-payment", "eum-notification")) {
            Map<String, Object> doc = load(service);
            Object definition = at(doc, "spring", "cloud", "function", "definition");

            assertThat(definition)
                .as("%s 에 소비 함수 이름이 있어야 한다", service)
                .isNotNull();

            for (String name : definition.toString().split(";")) {
                assertThat(destinationOf(doc, name.trim() + "-in-0"))
                    .as("%s 의 %s 함수에 대응하는 바인딩이 있어야 한다. 이름이 어긋나면 "
                        + "소비자가 엉뚱한 목적지를 듣는다", service, name.trim())
                    .isNotNull();
            }
        }
    }

    @Test
    @DisplayName("브로커 계정과 데이터베이스 계정을 환경변수로 받는다")
    void credentialsComeFromEnvironment() {
        for (String service : List.of("eum-core", "eum-payment", "eum-notification")) {
            Map<String, Object> doc = load(service);

            assertThat(at(doc, "spring", "datasource", "url").toString())
                .as("%s 의 기본 설정은 실습용 H2 여야 한다", service)
                .startsWith("jdbc:h2:mem:");

            assertThat(at(doc, "spring", "rabbitmq", "username").toString())
                .as("%s 의 브로커 계정은 환경변수로 들어와야 한다. 값을 박아 두면 "
                    + "환경마다 이미지를 새로 만들어야 한다", service)
                .contains("${");
        }
    }

    // ----- 거들기 -----

    /**
     * 그 서비스가 이미지에 담아 배포하는 application.yml 을 읽는다.
     *
     * <p>세 서비스가 모두 클래스패스 뿌리에 {@code application.yml} 을 둔다. 이름만으로는
     * 가릴 수 없어서, 자원이 들어 있는 자리(그 서비스의 jar 나 build 폴더) 이름으로
     * 가린다. 이 다툼 자체가 한 JVM 에 여러 서비스를 올릴 때 겪는 일이다.</p>
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> load(String service) {
        try {
            List<java.net.URL> found = java.util.Collections.list(
                getClass().getClassLoader().getResources("application.yml"));
            for (java.net.URL url : found) {
                if (url.toString().contains(service)) {
                    try (InputStream in = url.openStream()) {
                        return new Yaml().loadAs(in, Map.class);
                    }
                }
            }
            throw new IllegalStateException(
                service + " 의 application.yml 을 찾지 못했습니다. 찾은 것: " + found);
        } catch (java.io.IOException e) {
            throw new IllegalStateException(service + " 의 application.yml 을 읽지 못했습니다.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object at(Map<String, Object> doc, String... path) {
        Object node = doc;
        for (String key : path) {
            if (!(node instanceof Map)) {
                return null;
            }
            node = ((Map<String, Object>) node).get(key);
        }
        return node;
    }

    @SuppressWarnings("unchecked")
    private Object destinationOf(Map<String, Object> doc, String binding) {
        Object bindings = at(doc, "spring", "cloud", "stream", "bindings");
        if (!(bindings instanceof Map)) {
            return null;
        }
        Object one = ((Map<String, Object>) bindings).get(binding);
        if (!(one instanceof Map)) {
            return null;
        }
        return ((Map<String, Object>) one).get("destination");
    }
}
