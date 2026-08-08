package com.pizzastudio.eum.payment.config;

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
 * <p><b>세 번째 복사본이다.</b> 게이트웨이와 본체에 같은 코드가 있다. 서비스를 나누면
 * 이런 일이 생긴다 — 저장소가 갈라지면 공용 코드도 갈라진다. 언제 공용 모듈로 뽑을지는
 * 17.1 에서 다룬다.</p>
 *
 * <p>게이트웨이가 이미 검증했는데 왜 또 하는가. 클러스터 안에서는 게이트웨이를 지나지 않고
 * 이 서비스를 직접 부를 수 있기 때문이다(16.3). 게이트웨이만 믿는 구조는 그 길이 생기는
 * 순간 무너진다.</p>
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
