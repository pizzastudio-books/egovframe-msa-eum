package com.pizzastudio.eum.gateway.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 토큰 검증.
 *
 * <p>회원 서비스를 부르지 않고 서명만 확인한다. 요청마다 회원 서비스를 동기 호출하면
 * 그 서비스가 느려질 때 전부 느려진다. 표준프레임워크 템플릿이 그렇게 되어 있고,
 * 이 책은 그 방식을 따르지 않는다(16.3).</p>
 */
@Component
public class JwtVerifier {

    public static final String AUTHORITIES_CLAIM = "authorities";

    private final String tokenSecret;

    public JwtVerifier(@Value("${token.secret}") String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    private SecretKey key() {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(tokenSecret.getBytes(StandardCharsets.UTF_8));
            return Keys.hmacShaKeyFor(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Claims parse(String token) {
        String value = token == null ? "" : token.trim();
        if (value.startsWith("Bearer ")) {
            value = value.substring(7).trim();
        }
        return Jwts.parser().verifyWith(key()).build().parseSignedClaims(value).getPayload();
    }
}
