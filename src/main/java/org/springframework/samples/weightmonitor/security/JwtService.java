package org.springframework.samples.weightmonitor.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtService {

    @Value("${jwt.secret}") // application.properties 里配置
    private String secretKey;

    @Value("${jwt.expiration:86400000}") // 默认 24 小时
    private long expiration;

    // 生成 token
    public String generateToken(UserDetails user) {
        return Jwts.builder()
                .subject(user.getUsername())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignKey())
                .compact();
    }

    // 验证 token
    public boolean isTokenValid(String token, UserDetails user) {
        try {
            String username = extractUsername(token);
            return username.equals(user.getUsername()) && !isTokenExpired(token);
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false; // expired token is simply invalid
        }
    }

    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    private boolean isTokenExpired(String token) {
        return Jwts.parser().verifyWith(getSignKey()).build()
                .parseSignedClaims(token).getPayload()
                .getExpiration().before(new Date());
    }

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
    }
}