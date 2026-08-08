package com.pizzastudio.eum.gateway.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.pizzastudio.eum.gateway.config.JwtVerifier;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 토큰을 검증하고 주체를 세운다.
 *
 * <p>뒤에 있는 서비스는 헤더로 받은 주체를 믿는다. 그래서 클러스터 밖에서 이 헤더가
 * 들어오지 못하게 막아야 한다. 그 판단을 9.3 에서 다룬다.</p>
 */
@Component
@RequiredArgsConstructor
public class AuthenticationFilter extends OncePerRequestFilter {

    /** 뒤로 넘기는 주체 헤더 */
    public static final String USER_HEADER = "X-Eum-User";
    public static final String ROLES_HEADER = "X-Eum-Roles";

    /**
     * 감사 로그가 읽는 주체.
     *
     * <p>감사 필터는 시큐리티 사슬 <b>바깥</b>에 있다(16.4). 거부당한 요청도 남기려면
     * 그래야 하는데, 사슬을 벗어나면 {@code SecurityContextHolder} 는 이미 비어 있다.
     * 그래서 요청 속성에 따로 심어 둔다.</p>
     */
    public static final String PRINCIPAL_ATTRIBUTE = "eum.principal";

    /** 토큰을 거부한 사유. 만료인지 서명 불일치인지 남긴다. */
    public static final String REJECT_REASON_ATTRIBUTE = "eum.rejectReason";

    private final JwtVerifier jwtVerifier;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {

        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (ObjectUtils.isEmpty(token) || "undefined".equals(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtVerifier.parse(token);
            String authorities = claims.get(JwtVerifier.AUTHORITIES_CLAIM, String.class);
            List<SimpleGrantedAuthority> roles = authorities == null ? List.of()
                : Arrays.stream(authorities.split(","))
                    .map(String::trim).filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new).toList();

            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(claims.getSubject(), null, roles));
            request.setAttribute(PRINCIPAL_ATTRIBUTE, claims.getSubject());

            chain.doFilter(new PrincipalForwardingRequest(request, claims.getSubject(), authorities),
                response);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            // 사유를 남긴다. 만료인지 서명 불일치인지 모르면 장애 대응이 안 된다.
            request.setAttribute(PRINCIPAL_ATTRIBUTE, "-");
            request.setAttribute(REJECT_REASON_ATTRIBUTE, e.getClass().getSimpleName());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
        }
    }

    /** 주체를 헤더로 실어 뒤로 넘긴다 */
    static class PrincipalForwardingRequest extends jakarta.servlet.http.HttpServletRequestWrapper {

        private final String user;
        private final String roles;

        PrincipalForwardingRequest(HttpServletRequest request, String user, String roles) {
            super(request);
            this.user = user;
            this.roles = roles == null ? "" : roles;
        }

        @Override
        public String getHeader(String name) {
            if (USER_HEADER.equalsIgnoreCase(name)) {
                return user;
            }
            if (ROLES_HEADER.equalsIgnoreCase(name)) {
                return roles;
            }
            return super.getHeader(name);
        }

        /**
         * {@code getHeader} 만 재정의하면 뒤로 안 넘어간다.
         *
         * <p>프록시는 넘길 헤더를 모을 때 이름 목록부터 훑는다. 목록에 없는 이름은
         * 값을 물어보지도 않는다. 실제로 겪었다 — 헤더를 심는 코드가 있는데 받는 쪽에는
         * 오지 않았다(16.3).</p>
         */
        @Override
        public java.util.Enumeration<String> getHeaderNames() {
            java.util.List<String> names = new java.util.ArrayList<>();
            java.util.Enumeration<String> original = super.getHeaderNames();
            while (original.hasMoreElements()) {
                String name = original.nextElement();
                if (!USER_HEADER.equalsIgnoreCase(name) && !ROLES_HEADER.equalsIgnoreCase(name)) {
                    names.add(name);
                }
            }
            names.add(USER_HEADER);
            names.add(ROLES_HEADER);
            return java.util.Collections.enumeration(names);
        }

        @Override
        public java.util.Enumeration<String> getHeaders(String name) {
            if (USER_HEADER.equalsIgnoreCase(name)) {
                return java.util.Collections.enumeration(java.util.List.of(user));
            }
            if (ROLES_HEADER.equalsIgnoreCase(name)) {
                return java.util.Collections.enumeration(java.util.List.of(roles));
            }
            return super.getHeaders(name);
        }
    }
}
