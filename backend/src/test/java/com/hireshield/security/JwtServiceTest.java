package com.hireshield.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.hireshield.entity.Role;
import com.hireshield.entity.User;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-abcdef";

    @Test
    void createdTokenIsValid() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.createToken(user());
        assertThat(jwt.validateAndGetSubject(token)).isEqualTo("asha@example.com");
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, 60);
        String token = jwt.createToken(user());
        String tampered = token.substring(0, token.length() - 3) + "abc";
        assertThat(jwt.validateAndGetSubject(tampered)).isNull();
    }

    @Test
    void tokenFromOtherSecretIsRejected() {
        String token = new JwtService(SECRET, 60).createToken(user());
        JwtService other = new JwtService("another-secret-key-0123456789-abcdefgh", 60);
        assertThat(other.validateAndGetSubject(token)).isNull();
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService jwt = new JwtService(SECRET, -1);
        assertThat(jwt.validateAndGetSubject(jwt.createToken(user()))).isNull();
    }

    @Test
    void shortSecretFailsFast() {
        assertThatThrownBy(() -> new JwtService("too-short", 60)).isInstanceOf(IllegalStateException.class);
    }

    private static AuthUser user() {
        User user = new User("Asha", "asha@example.com", "hash", Role.USER);
        user.setId(7L);
        return new AuthUser(user);
    }
}
