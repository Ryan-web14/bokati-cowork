package com.sni.bokaticowork.security.ratelimit;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le filtre bout a bout · il refuse, il le dit, et il dit quand reessayer.
 */
class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        ObjectProvider<StringRedisTemplate> noRedis = mock(ObjectProvider.class);
        when(noRedis.getIfAvailable()).thenReturn(null);

        RateLimitRules rules = new RateLimitRules();
        Map.of("loginMax", 2, "otpMax", 3, "callbackMax", 120, "checkinMax", 120, "publicWriteMax", 10,
                        "verifyMax", 60, "uploadMax", 5, "adminMax", 600, "defaultMax", 300)
                .forEach((field, value) -> ReflectionTestUtils.setField(rules, field, value));
        Map.of("loginWindow", 60L, "otpWindow", 600L, "callbackWindow", 60L, "checkinWindow", 60L, "publicWriteWindow", 600L,
                        "verifyWindow", 600L, "uploadWindow", 600L, "adminWindow", 60L, "defaultWindow", 60L)
                .forEach((field, value) -> ReflectionTestUtils.setField(rules, field, value));

        ClientIpResolver resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxyCount", 1);

        filter = new RateLimitingFilter(rules, new RateLimitStore(noRedis), resolver);
        ReflectionTestUtils.setField(filter, "enabled", true);
    }

    private MockHttpServletRequest login(String forwarded) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", ApiPath.V1 + "/auth/login");
        request.setRemoteAddr("10.0.0.1");
        request.addHeader("X-Forwarded-For", forwarded);
        return request;
    }

    /**
     * Une requete neuve a chaque appel · {@code OncePerRequestFilter} marque celle qu'il a deja
     * vue et ne la filtre pas deux fois.
     */
    private MockHttpServletRequest upload() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/verify/doc/42/compare");
        request.setRemoteAddr("203.0.113.9");
        return request;
    }

    @Test
    @DisplayName("Au-dela du plafond, la requete est refusee avec un Retry-After")
    void refusesBeyondTheCeilingAndSaysWhenToRetry() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        for (int i = 0; i < 2; i++) {
            filter.doFilter(login("203.0.113.9"), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse refused = new MockHttpServletResponse();
        filter.doFilter(login("203.0.113.9"), refused, chain);

        verify(chain, times(2)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(refused.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(refused.getHeader("Retry-After")).isEqualTo("60");
        assertThat(refused.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    @DisplayName("Un prefixe X-Forwarded-For different ne rouvre pas le plafond")
    void forgedPrefixDoesNotResetTheCounter() throws Exception {
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(login("a, 203.0.113.9"), new MockHttpServletResponse(), chain);
        filter.doFilter(login("b, 203.0.113.9"), new MockHttpServletResponse(), chain);
        MockHttpServletResponse refused = new MockHttpServletResponse();
        filter.doFilter(login("c, 203.0.113.9"), refused, chain);

        assertThat(refused.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
    }

    @Test
    @DisplayName("Une route hors API est comptee aussi · elle ne l'etait pas du tout")
    void nonApiRoutesAreCountedToo() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        ReflectionTestUtils.setField(filter, "enabled", true);

        for (int i = 0; i < 5; i++) {
            filter.doFilter(upload(), new MockHttpServletResponse(), chain);
        }
        MockHttpServletResponse refused = new MockHttpServletResponse();
        filter.doFilter(upload(), refused, chain);

        assertThat(refused.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(refused.getHeader("Retry-After")).isEqualTo("600");
    }

    @Test
    @DisplayName("Plafond desactive · le filtre ne compte rien et laisse passer")
    void disabledFilterPassesEverythingThrough() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        ReflectionTestUtils.setField(filter, "enabled", false);

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            filter.doFilter(login("203.0.113.9"), response, chain);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }

        verify(chain, times(20)).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("La requete refusee ne traverse pas la chaine")
    void refusedRequestNeverReachesTheApplication() throws Exception {
        FilterChain chain = mock(FilterChain.class);
        ReflectionTestUtils.setField(filter, "enabled", true);

        filter.doFilter(login("203.0.113.9"), new MockHttpServletResponse(), chain);
        filter.doFilter(login("203.0.113.9"), new MockHttpServletResponse(), chain);
        FilterChain afterCeiling = mock(FilterChain.class);
        filter.doFilter(login("203.0.113.9"), new MockHttpServletResponse(), afterCeiling);

        verify(afterCeiling, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
