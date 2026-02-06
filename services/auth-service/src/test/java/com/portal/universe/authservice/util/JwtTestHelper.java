package com.portal.universe.authservice.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class JwtTestHelper {

    public static final String SECRET_KEY = "test-secret-key-must-be-at-least-256-bits-long!!";
    public static final String USER_UUID = "550e8400-e29b-41d4-a716-446655440000";
    public static final String USER_EMAIL = "test@example.com";

    private JwtTestHelper() {}

    public static String createValidToken(String secretKey, String userId, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 900_000); // 15 min
        return Jwts.builder()
                .header().add("kid", "test-key").and()
                .subject(userId)
                .claim("roles", roles)
                .claim("email", USER_EMAIL)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    public static String createExpiredToken(String secretKey, String userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() - 1000); // already expired
        return Jwts.builder()
                .header().add("kid", "test-key").and()
                .subject(userId)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(new Date(now.getTime() - 900_000))
                .expiration(expiration)
                .signWith(getSigningKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    public static String createTokenWithKid(String secretKey, String kid, String userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 900_000);
        return Jwts.builder()
                .header().add("kid", kid).and()
                .subject(userId)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    public static String createTokenWithMemberships(String secretKey, String userId,
                                                     Map<String, String> memberships) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 900_000);
        return Jwts.builder()
                .header().add("kid", "test-key").and()
                .subject(userId)
                .claim("roles", List.of("ROLE_USER"))
                .claim("memberships", memberships)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    public static String createTokenWithClaims(String secretKey, String userId,
                                                Map<String, Object> claims) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 900_000);
        return Jwts.builder()
                .header().add("kid", "test-key").and()
                .subject(userId)
                .claims(claims)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    public static String createTokenWithoutKid(String secretKey, String userId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + 900_000);
        return Jwts.builder()
                .subject(userId)
                .claim("roles", List.of("ROLE_USER"))
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey(secretKey), Jwts.SIG.HS256)
                .compact();
    }

    private static SecretKey getSigningKey(String secretKey) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
