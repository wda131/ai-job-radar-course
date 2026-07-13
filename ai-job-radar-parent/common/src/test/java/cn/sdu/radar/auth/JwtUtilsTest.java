package cn.sdu.radar.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilsTest {

    @Test
    void generatesAndParsesBearerToken() {
        String token = JwtUtils.generateToken(1L, "student");

        assertTrue(token.startsWith("bearer "));
        assertEquals(1L, JwtUtils.parseUserId(token));
    }

    @Test
    void rejectsInvalidToken() {
        assertThrows(IllegalArgumentException.class,
                () -> JwtUtils.parseUserId("bearer invalid-token"));
    }
}
