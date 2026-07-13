package cn.sdu.radar.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

public final class JwtUtils {
    private static final String TOKEN_PREFIX = "bearer ";
    private static final long EXPIRATION_MILLIS = 7L * 24 * 60 * 60 * 1000;
    private static final Key KEY = Keys.hmacShaKeyFor(
            "sdu-ai-job-radar-course-secret-2026".getBytes(StandardCharsets.UTF_8));

    private JwtUtils() {
    }

    public static String generateToken(Long userId, String username) {
        Date now = new Date();
        String token = Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + EXPIRATION_MILLIS))
                .signWith(KEY)
                .compact();
        return TOKEN_PREFIX + token;
    }

    public static Long parseUserId(String authorization) {
        if (authorization == null || !authorization.startsWith(TOKEN_PREFIX)) {
            throw new IllegalArgumentException("登录状态无效，请重新登录");
        }
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(KEY)
                    .build()
                    .parseClaimsJws(authorization.substring(TOKEN_PREFIX.length()))
                    .getBody();
            return claims.get("userId", Long.class);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new IllegalArgumentException("登录状态无效，请重新登录", exception);
        }
    }
}
