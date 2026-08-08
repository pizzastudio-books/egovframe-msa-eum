package com.pizzastudio.eum.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;

/**
 * 라우트 차례를 못 박는다.
 *
 * <p>게이트웨이는 위에서부터 맞는 것을 찾고 <b>먼저 맞은 것으로 끝낸다.</b> 그래서
 * {@code /api/v1/**} 를 위에 두면 그 아래의 {@code /api/v1/payments/**} 는 영영 걸리지
 * 않는다. 지급 요청이 본체로 가고, 본체에는 그 경로가 없으니 404 가 난다.</p>
 *
 * <p>이 결함은 설정이 문법상 멀쩡하고 게이트웨이도 정상으로 뜨기 때문에 눈으로는
 * 잡히지 않는다. 실제로 이 저장소에서 그렇게 적혀 있었다.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("게이트웨이 라우트 차례")
class RouteOrderTest {

    @Autowired
    private GatewayMvcProperties properties;

    @Test
    @DisplayName("좁은 경로가 넓은 경로보다 앞에 있다")
    void narrowerPathComesFirst() {
        List<RouteProperties> routes = properties.getRoutes();

        int payment = indexOf(routes, "eum-payment");
        int core = indexOf(routes, "eum-core");

        assertThat(payment)
            .as("지급 라우트가 설정에 있어야 한다")
            .isNotNegative();
        assertThat(core)
            .as("본체 라우트가 설정에 있어야 한다")
            .isNotNegative();
        assertThat(payment)
            .as("/api/v1/payments/** 가 /api/v1/** 보다 앞에 있어야 한다. "
                + "뒤에 있으면 지급 요청이 전부 본체로 간다")
            .isLessThan(core);
    }

    /**
     * 이 시험은 한동안 아무것도 못 잡았다.
     *
     * <p>이름은 "주소가 환경변수로 들어온다"인데 단언은 {@code isNotNull()} 하나뿐이라,
     * 주소를 통째로 박아 두어도 통과했다. <b>이름이 지키는 것과 단언이 지키는 것이
     * 달랐다.</b> 시험이 있다는 사실만으로 안심하면 안 되는 이유다(16.2).</p>
     *
     * <p>{@code getUri()} 는 자리표가 이미 치환된 값을 돌려주므로 "${" 를 찾는 식으로는
     * 고칠 수 없다. 그래서 실려 나가는 설정 파일의 글자를 직접 본다.</p>
     */
    @Test
    @DisplayName("뒤쪽 서비스 주소가 설정에 박혀 있지 않다")
    void backendUriComesFromEnvironment() throws Exception {
        String shipped = new String(new ClassPathResource("application.yml")
            .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        for (String line : shipped.split("\n")) {
            String trimmed = line.trim();
            if (!trimmed.startsWith("uri:")) {
                continue;
            }
            assertThat(trimmed)
                .as("주소가 박혀 있으면 환경마다 이미지를 새로 만들어야 한다 — %s", trimmed)
                .contains("${");
        }
    }

    @Test
    @DisplayName("라우트가 셋이고 좁은 것부터 적혀 있다")
    void routeCountAndOrder() {
        List<String> ids = properties.getRoutes().stream().map(RouteProperties::getId).toList();

        assertThat(ids)
            .as("아직 옮기지 않은 것을 옛 곳으로 보내는 라우트가 있어야 한다(16.4)")
            .containsExactly("legacy", "eum-payment", "eum-core");
    }

    private int indexOf(List<RouteProperties> routes, String id) {
        for (int i = 0; i < routes.size(); i++) {
            if (id.equals(routes.get(i).getId())) {
                return i;
            }
        }
        return -1;
    }
}
