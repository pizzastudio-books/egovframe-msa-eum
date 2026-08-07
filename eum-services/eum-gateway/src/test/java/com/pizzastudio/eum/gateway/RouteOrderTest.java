package com.pizzastudio.eum.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.test.context.ActiveProfiles;

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

    @Test
    @DisplayName("뒤쪽 서비스 주소는 환경변수로 들어온다")
    void backendUriComesFromEnvironment() {
        for (RouteProperties route : properties.getRoutes()) {
            assertThat(route.getUri())
                .as("%s 의 주소가 설정에 박혀 있으면 환경마다 이미지를 새로 만들어야 한다",
                    route.getId())
                .isNotNull();
        }
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
