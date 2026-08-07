package com.pizzastudio.eum.gateway.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
 */
@Slf4j
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class AuditLogFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {

        long started = System.currentTimeMillis();
        try {
            chain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String user = authentication == null ? "-" : authentication.getName();
            log.info("감사 user={} method={} path={} status={} 소요={}ms",
                user, request.getMethod(), request.getRequestURI(),
                response.getStatus(), System.currentTimeMillis() - started);
        }
    }
}
