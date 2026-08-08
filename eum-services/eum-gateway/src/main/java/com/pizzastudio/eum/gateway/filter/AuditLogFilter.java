package com.pizzastudio.eum.gateway.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * 감사 로그.
 *
 * <p>누가 언제 무엇을 불렀는지 한곳에 남긴다. 공공에서는 빠질 수 없는 요건이고,
 * 업무 식별자가 필요하므로 인프라가 아니라 애플리케이션이 맡아야 한다(16.1·16.4).</p>
 *
 * <p><b>시큐리티 사슬보다 바깥에 둔다.</b> 안쪽에 두었더니 <b>거부당한 요청이 하나도
 * 남지 않았다.</b> 토큰 없이 부른 것도, 틀린 토큰으로 부른 것도 시큐리티가 401 로
 * 끊어 여기까지 오지 않았다. 세 번 불러 한 줄만 남는 것을 실제로 확인했다(16.4).
 * 공공 감사에서 기록되어야 하는 것은 성공한 요청보다 오히려 거부당한 시도다.</p>
 *
 * <p>사슬 바깥이면 {@code SecurityContextHolder} 가 이미 비어 있다. 그래서 주체는
 * {@link AuthenticationFilter#PRINCIPAL_ATTRIBUTE} 로 받는다.</p>
 *
 * <p><b>요청 식별자 필터보다는 안쪽이어야 한다.</b> 바깥에 두었더니 감사 줄에 식별자가
 * 안 찍혔다 — 그때는 MDC 가 아직 비어 있다. 본체·알림 로그에는 찍히는데 정작 게이트웨이
 * 감사만 빠져, 요청을 어디서부터 따라가야 할지 알 수 없었다(19.1).</p>
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class AuditLogFilter extends OncePerRequestFilter {

    /**
     * 프로브는 남기지 않는다.
     *
     * <p>준비 상태 10초·활성 상태 20초 주기로 들어온다(11.1). 이것까지 감사 형식으로
     * 쌓으면 사람이 볼 수 없는 로그가 된다.</p>
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {

        long started = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            Object principal = request.getAttribute(AuthenticationFilter.PRINCIPAL_ATTRIBUTE);
            Object reason = request.getAttribute(AuthenticationFilter.REJECT_REASON_ATTRIBUTE);
            // 요청 식별자는 로그 형식(logback-spring.xml)이 앞머리에 찍는다.
            // 감사 줄에도 그 값이 붙으므로 뒤쪽 서비스 로그와 이어진다(19.1).
            log.info("감사 user={} method={} path={} status={} 소요={}ms{}",
                principal == null ? "-" : principal,
                request.getMethod(), request.getRequestURI(),
                response.getStatus(), System.currentTimeMillis() - started,
                reason == null ? "" : " 거부사유=" + reason);
        }
    }
}
