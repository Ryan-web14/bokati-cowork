package com.sni.bokaticowork.core.observability;

import com.sni.bokaticowork.security.ratelimit.ClientIpResolver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot A0 : chaque requete a un identifiant (recu s'il est sain, cree sinon), renvoye dans la reponse
 * et porte dans le MDC pendant la requete seulement ; l'adresse du client est celle de droite de la
 * chaine X-Forwarded-For (le proxy Heroku), pas celle que l'appelant ecrit ; une tache asynchrone
 * garde le contexte de son appelant.
 */
class RequestContextA0Test {

    private final ClientIpResolver resolver = new ClientIpResolver();
    private final RequestContextFilter filter = new RequestContextFilter(resolver);

    RequestContextA0Test() {
        ReflectionTestUtils.setField(resolver, "trustedProxyCount", 1);
    }

    @AfterEach
    void clear() {
        MDC.clear();
    }

    @Test
    void requestIdIsKeptWhenSafeAndCreatedOtherwise() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sni/api/v1/billing/documents");
        request.addHeader(RequestContext.HEADER, "front-12345678");
        request.setRemoteAddr("10.1.2.3");
        request.addHeader("X-Forwarded-For", "6.6.6.6, 41.202.207.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenId = new AtomicReference<>();
        AtomicReference<String> seenIp = new AtomicReference<>();
        filter.doFilter(request, response, (req, res) -> {
            seenId.set(RequestContext.currentRequestId());
            seenIp.set(RequestContext.clientIp((jakarta.servlet.http.HttpServletRequest) req));
        });
        assertThat(seenId.get()).isEqualTo("front-12345678");
        assertThat(response.getHeader(RequestContext.HEADER)).isEqualTo("front-12345678");
        // La valeur ecrite par l'appelant (6.6.6.6) est ignoree ; seule celle du proxy compte.
        assertThat(seenIp.get()).isEqualTo("41.202.207.5");
        assertThat(MDC.get(RequestContext.MDC_REQUEST_ID)).isNull();

        MockHttpServletRequest forged = new MockHttpServletRequest("GET", "/x");
        forged.addHeader(RequestContext.HEADER, "bad id with spaces\nand newline");
        MockHttpServletResponse second = new MockHttpServletResponse();
        filter.doFilter(forged, second, (req, res) -> { });
        assertThat(second.getHeader(RequestContext.HEADER)).matches("[0-9a-f-]{36}");
    }

    @Test
    void anAsyncTaskKeepsTheContextOfItsCaller() {
        MDC.put(RequestContext.MDC_REQUEST_ID, "req-abcdefgh");
        Runnable decorated = new MdcTaskDecorator().decorate(() -> assertThat(RequestContext.currentRequestId()).isEqualTo("req-abcdefgh"));
        MDC.clear();
        decorated.run();
        assertThat(RequestContext.currentRequestId()).isNull();
    }
}
