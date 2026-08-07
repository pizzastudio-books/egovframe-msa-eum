package com.pizzastudio.eum.common.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 토큰 발급과 검증.
 */
@Component
public class JwtTokenProvider {

    public static final String AUTHORITIES_CLAIM = "authorities";

    private final String tokenSecret;
    private final long expirationMillis;

    public JwtTokenProvider(@Value("${token.secret}") String tokenSecret,
        @Value("${token.expiration-millis:3600000}") long expirationMillis) {
        this.tokenSecret = tokenSecret;
        this.expirationMillis = expirationMillis;
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

    public String issue(String userId, String authorities) {
        Date now = new Date();
        return Jwts.builder()
            .subject(userId)
            .claim(AUTHORITIES_CLAIM, authorities)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMillis))
            .signWith(key())
            .compact();
    }

    public Claims parse(String token) {
        String value = token == null ? "" : token.trim();
        if (value.startsWith("Bearer ")) {
            value = value.substring(7).trim();
        }
        return Jwts.parser()
            .verifyWith(key())
            .build()
            .parseSignedClaims(value)
            .getPayload();
    }
}
