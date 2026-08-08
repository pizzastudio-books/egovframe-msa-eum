package com.pizzastudio.eum.payment.config;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

/**
 * 지급 서비스의 인증.
 *
 * <p>지급은 돈을 다루므로 조회도 담당자만 할 수 있다. 권한 판정은 이 서비스가 한다 —
 * 게이트웨이는 "누구인가"까지만 정한다(16.3).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtVerifier jwtVerifier;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .logout(logout -> logout.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(ex -> ex
                // 인증이 안 된 것(401)과 권한이 없는 것(403)을 가른다. 둘 다 "못 본다"지만
                // 대응이 다르다 — 앞은 로그인 문제, 뒤는 권한 신청 문제다.
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                .accessDeniedHandler((request, response, e) ->
                    response.setStatus(HttpStatus.FORBIDDEN.value())))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                // 권한을 여기서 건다. 메서드에 @PreAuthorize 로 걸었더니 **거부 응답이
                // 401 로 나왔다** — 권한 판정이 시큐리티 사슬 밖(컨트롤러 호출 시점)에서
                // 일어나 403 을 내는 처리기까지 닿지 않았다. 실제로 겪었다(17.2).
                // 경로로 걸면 사슬 안의 인가 필터가 판정해 401 과 403 이 제대로 갈린다.
                .requestMatchers("/api/v1/payments/**", "/api/v1/payments").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(new TokenFilter(jwtVerifier),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /** 게이트웨이가 이미 검증했더라도 다시 확인한다(16.3). */
    @RequiredArgsConstructor
    static class TokenFilter extends OncePerRequestFilter {

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
                chain.doFilter(request, response);
            } catch (JwtException e) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
            }
        }
    }
}
