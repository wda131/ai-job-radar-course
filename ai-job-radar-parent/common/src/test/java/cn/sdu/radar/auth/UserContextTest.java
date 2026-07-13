package cn.sdu.radar.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserContextTest {

    @AfterEach
    void cleanUp() {
        UserContext.remove();
    }

    @Test
    void storesAndRemovesCurrentUser() {
        UserContext.setUserId(7L);

        assertEquals(7L, UserContext.getUserId());
        UserContext.remove();
        assertNull(UserContext.getUserId());
    }
}
