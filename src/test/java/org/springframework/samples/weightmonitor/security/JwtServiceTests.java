package org.springframework.samples.weightmonitor.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import io.jsonwebtoken.JwtException;

class JwtServiceTests {

    // Base64 of "test-secret-key-for-unit-tests-ok!" (>=32 bytes for HS256)
    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LWZvci11bml0LXRlc3RzLW9rIQ==";

    private JwtService service;

    private UserDetails user;

    @BeforeEach
    void setUp() {
        service = new JwtService();
        ReflectionTestUtils.setField(service, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(service, "expiration", 86400000L); // 24h

        user = User.withUsername("doc001")
                .password("irrelevant")
                .roles("PROVIDER")
                .build();
    }

    // ── generateToken ─────────────────────────────────────────────────

    @Test
    void generateToken_returnsNonNullToken() {
        String token = service.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void generateToken_producesThreeParts() {
        // JWT format: header.payload.signature
        String token = service.generateToken(user);
        assertThat(token.split("\\.")).hasSize(3);
    }

    // ── extractUsername ───────────────────────────────────────────────

    @Test
    void extractUsername_returnsCorrectUsername() {
        String token = service.generateToken(user);
        assertThat(service.extractUsername(token)).isEqualTo("doc001");
    }

    @Test
    void extractUsername_invalidToken_throwsJwtException() {
        assertThatThrownBy(() -> service.extractUsername("not.a.token"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void extractUsername_tamperedToken_throwsJwtException() {
        String token = service.generateToken(user);
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThatThrownBy(() -> service.extractUsername(tampered))
                .isInstanceOf(JwtException.class);
    }

    // ── isTokenValid ──────────────────────────────────────────────────

    @Test
    void isTokenValid_validTokenAndMatchingUser_returnsTrue() {
        String token = service.generateToken(user);
        assertThat(service.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_tokenForDifferentUser_returnsFalse() {
        String token = service.generateToken(user);

        UserDetails otherUser = User.withUsername("doc999")
                .password("irrelevant")
                .roles("PROVIDER")
                .build();

        assertThat(service.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        JwtService shortLivedService = new JwtService();
        ReflectionTestUtils.setField(shortLivedService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(shortLivedService, "expiration", -1000L); // already expired

        String expiredToken = shortLivedService.generateToken(user);

        assertThat(service.isTokenValid(expiredToken, user)).isFalse();
    }

    // ── round trip ────────────────────────────────────────────────────

    @Test
    void generateThenExtract_roundTrip() {
        String token = service.generateToken(user);
        String extracted = service.extractUsername(token);
        assertThat(service.isTokenValid(token, user)).isTrue();
        assertThat(extracted).isEqualTo(user.getUsername());
    }
}