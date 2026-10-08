package vn.ticketscenter.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.*;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import vn.ticketscenter.controller.common.ReturnToValidator;
import vn.ticketscenter.exception.BusinessException;
import vn.ticketscenter.filter.CsrfFilter;
import vn.ticketscenter.filter.RequestValidationFilter;
import vn.ticketscenter.service.identity.*;
import vn.ticketscenter.support.MutableClock;

class RequestSecurityTest {
    private HttpSession session() {
        var session = mock(HttpSession.class);
        var values = new HashMap<String, Object>();
        when(session.getAttribute(anyString())).thenAnswer(call -> values.get(call.getArgument(0)));
        doAnswer(
                        call -> {
                            values.put(call.getArgument(0), call.getArgument(1));
                            return null;
                        })
                .when(session)
                .setAttribute(anyString(), any());
        return session;
    }

    @Test
    void tokenIsSessionBoundAndRotates() {
        var first = session();
        var second = session();
        String token = CsrfTokens.getOrCreate(first);
        assertTrue(CsrfTokens.matches(first, token));
        assertFalse(CsrfTokens.matches(second, token));
        assertNotEquals(token, CsrfTokens.rotate(first));
        assertFalse(CsrfTokens.matches(first, token));
    }

    @Test
    void postWithoutCsrfNeverReachesController() throws Exception {
        var request = mock(HttpServletRequest.class);
        var chain = mock(FilterChain.class);
        when(request.getMethod()).thenReturn("POST");
        assertEquals(
                403,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        new CsrfFilter()
                                                .doFilter(
                                                        request,
                                                        mock(HttpServletResponse.class),
                                                        chain))
                        .getHttpStatus());
        verifyNoInteractions(chain);
    }

    @Test
    void limiterExpiresAtExactBoundaryAndBoundsMemory() {
        var clock = new MutableClock();
        var limiter = new AuthRateLimiter(clock, 1);
        assertTrue(limiter.allow("login:buyer_a", 1, Duration.ofMinutes(15)));
        assertFalse(limiter.allow("login:buyer_a", 1, Duration.ofMinutes(15)));
        assertFalse(limiter.allow("other", 1, Duration.ofMinutes(15)));
        clock.advance(Duration.ofMinutes(15));
        assertTrue(limiter.allow("other", 1, Duration.ofMinutes(15)));
    }

    @Test
    void returnToCannotEscapeApplication() {
        for (String value :
                List.of(
                        "//evil.test",
                        "https://evil.test",
                        "/%2f%2fevil.test",
                        "/me\\evil",
                        "/me\r\nX:x")) assertEquals("/events", ReturnToValidator.validate(value));
        assertEquals("/me/orders", ReturnToValidator.validate("/me/orders"));
    }

    private HttpServletRequest jsonRequest(byte[] body) throws Exception {
        var request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/ticketscenter/auth/login");
        when(request.getContextPath()).thenReturn("/ticketscenter");
        when(request.getContentType()).thenReturn("application/json");
        var input = new ByteArrayInputStream(body);
        when(request.getInputStream())
                .thenReturn(
                        new ServletInputStream() {
                            public int read() {
                                return input.read();
                            }

                            public boolean isFinished() {
                                return input.available() == 0;
                            }

                            public boolean isReady() {
                                return true;
                            }

                            public void setReadListener(ReadListener listener) {}
                        });
        return request;
    }

    @Test
    void rejectsOversizedChunkedBodyMalformedAndForgedActor() throws Exception {
        var filter = new RequestValidationFilter();
        var response = mock(HttpServletResponse.class);
        assertEquals(
                413,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        filter.doFilter(
                                                jsonRequest(new byte[65537]),
                                                response,
                                                mock(FilterChain.class)))
                        .getHttpStatus());
        for (String value : List.of("{", "{}{}", "{\"role\":\"ADMIN\"}", "[]")) {
            var chain = mock(FilterChain.class);
            assertEquals(
                    400,
                    assertThrows(
                                    BusinessException.class,
                                    () ->
                                            filter.doFilter(
                                                    jsonRequest(value.getBytes()), response, chain))
                            .getHttpStatus());
            verifyNoInteractions(chain);
        }
    }
}
