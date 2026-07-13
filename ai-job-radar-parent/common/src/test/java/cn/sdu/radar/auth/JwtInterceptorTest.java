package cn.sdu.radar.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtInterceptorTest {

    private final JwtInterceptor interceptor = new JwtInterceptor();

    @AfterEach
    void cleanUp() {
        UserContext.remove();
    }

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(
                new MockHttpServletRequest(), response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
    }

    @Test
    void acceptsValidTokenAndClearsContextAfterRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", JwtUtils.generateToken(9L, "student"));

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(9L, UserContext.getUserId());

        interceptor.afterCompletion(request, new MockHttpServletResponse(), new Object(), null);
        assertNull(UserContext.getUserId());
    }
}
