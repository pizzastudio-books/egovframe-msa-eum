package com.pizzastudio.eum.notification.common;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청 식별자를 로그에 심는다(19.1).
 *
 * <p>서비스가 넷이면 한 요청이 로그 네 곳에 흩어진다. 이 값이 없으면 이을 수 없다.
 * 실제로 접수 한 건을 따라가 봤더니 게이트웨이·본체 로그에서는 신청번호조차 안 나왔다.</p>
 *
 * <p>값을 새로 만들지 않고 <b>인그레스 컨트롤러가 붙인 {@code X-Request-ID} 를 그대로
 * 쓴다.</b> 새로 만들면 인그레스 로그와 이어지지 않는다. 그 헤더가 없을 때만 만든다 —
 * 클러스터 안에서 서비스끼리 직접 부르는 경우다.</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-ID";
    public static final String MDC_KEY = "reqId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {

        String requestId = request.getHeader(HEADER);
        if (ObjectUtils.isEmpty(requestId)) {
            requestId = UUID.randomUUID().toString().substring(0, 8);
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            // 스레드는 재사용된다. 지우지 않으면 다음 요청 로그에 남의 식별자가 찍힌다.
            MDC.remove(MDC_KEY);
        }
    }
}
