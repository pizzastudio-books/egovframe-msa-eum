package com.pizzastudio.eum.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.pizzastudio.eum.gateway.common.RequestIdFilter;

import jakarta.servlet.ServletException;

/**
 * 요청 식별자(19.1).
 *
 * <p>서비스가 넷이면 한 요청이 로그 네 곳에 흩어진다. 이 값이 없으면 이을 수 없다.</p>
 */
@DisplayName("요청 식별자")
class RequestIdTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Test
    @DisplayName("인그레스가 붙인 값을 그대로 쓴다 — 새로 만들면 인그레스 로그와 안 이어진다")
    void reusesIngressHeader() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc123");
        MockHttpServletResponse response = new MockHttpServletResponse();

        String[] seen = new String[1];
        filter.doFilter(request, response, (req, res) -> seen[0] = MDC.get(RequestIdFilter.MDC_KEY));

        assertThat(seen[0]).isEqualTo("abc123");
        assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc123");
    }

    @Test
    @DisplayName("헤더가 없으면 만든다 — 클러스터 안에서 직접 부르는 경우다")
    void createsWhenMissing() throws ServletException, IOException {
        String[] seen = new String[1];
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
            (req, res) -> seen[0] = MDC.get(RequestIdFilter.MDC_KEY));

        assertThat(seen[0]).isNotBlank();
    }

    @Test
    @DisplayName("요청이 끝나면 지운다 — 스레드는 재사용된다")
    void clearsAfterRequest() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(RequestIdFilter.HEADER, "abc123");

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(MDC.get(RequestIdFilter.MDC_KEY))
            .as("지우지 않으면 다음 요청 로그에 남의 식별자가 찍힌다")
            .isNull();
    }
}
