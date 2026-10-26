package com.siva.assistant.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = "test-secret-test-secret-test-secret-123456";

    private final JwtService jwtService =
            new JwtService(new JwtProperties(SECRET, Duration.ofMinutes(15), Duration.ofDays(7)));

    @Test
    void accessTokenRoundTrips() {
        String token = jwtService.generateAccessToken(42L);

        assertThat(jwtService.parse(token, JwtService.ACCESS)).contains(42L);
    }

    @Test
    void refreshTokenIsRejectedAsAccessToken() {
        String token = jwtService.generateRefreshToken(42L);

        assertThat(jwtService.parse(token, JwtService.ACCESS)).isEmpty();
        assertThat(jwtService.parse(token, JwtService.REFRESH)).contains(42L);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expiring = new JwtService(new JwtProperties(SECRET, Duration.ofSeconds(-1), Duration.ofDays(7)));

        assertThat(expiring.parse(expiring.generateAccessToken(1L), JwtService.ACCESS)).isEmpty();
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwtService.generateAccessToken(42L);
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThat(jwtService.parse(tampered, JwtService.ACCESS)).isEqualTo(Optional.empty());
    }
}
