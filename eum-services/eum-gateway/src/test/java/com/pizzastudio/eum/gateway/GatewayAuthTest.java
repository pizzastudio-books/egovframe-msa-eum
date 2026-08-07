package com.pizzastudio.eum.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.pizzastudio.eum.gateway.config.JwtVerifier;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 게이트웨이의 토큰 검증.
 *
 * <p>회원 서비스를 부르지 않고 서명만으로 판단한다는 사실을 못 박아 둔다(16.3).</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewayAuthTest {

    @Autowired
    private JwtVerifier jwtVerifier;

    @Value("${token.secret}")
    private String tokenSecret;

    private SecretKey key(String secret) throws Exception {
        byte[] digest = java.security.MessageDigest.getInstance("SHA-256")
            .digest(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return Keys.hmacShaKeyFor(digest);
    }

    private String 토큰(String secret, String user, String roles) throws Exception {
        Date now = new Date();
        return Jwts.builder()
            .subject(user)
            .claim(JwtVerifier.AUTHORITIES_CLAIM, roles)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + 60_000))
            .signWith(key(secret))
            .compact();
    }

    @Test
    @DisplayName("바른 서명이면 주체와 권한을 꺼낸다")
    void 토큰_검증_성공() throws Exception {
        Claims claims = jwtVerifier.parse(토큰(tokenSecret, "user1", "ROLE_USER"));

        assertThat(claims.getSubject()).isEqualTo("user1");
        assertThat(claims.get(JwtVerifier.AUTHORITIES_CLAIM, String.class)).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("Bearer 접두사가 있어도 읽는다")
    void 접두사_처리() throws Exception {
        Claims claims = jwtVerifier.parse("Bearer " + 토큰(tokenSecret, "admin", "ROLE_ADMIN"));

        assertThat(claims.getSubject()).isEqualTo("admin");
    }

    @Test
    @DisplayName("다른 열쇠로 서명한 토큰은 거부한다")
    void 서명_불일치() throws Exception {
        String forged = 토큰("다른_비밀", "attacker", "ROLE_ADMIN");

        assertThatThrownBy(() -> jwtVerifier.parse(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void 만료_토큰() throws Exception {
        Date past = new Date(System.currentTimeMillis() - 120_000);
        String expired = Jwts.builder()
            .subject("user1")
            .claim(JwtVerifier.AUTHORITIES_CLAIM, "ROLE_USER")
            .issuedAt(past)
            .expiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(key(tokenSecret))
            .compact();

        assertThatThrownBy(() -> jwtVerifier.parse(expired)).isInstanceOf(JwtException.class);
    }
}
