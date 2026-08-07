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

            chain.doFilter(new PrincipalForwardingRequest(request, claims.getSubject(), authorities),
                response);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
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
    }
}
