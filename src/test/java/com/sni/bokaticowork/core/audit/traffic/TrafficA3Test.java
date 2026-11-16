package com.sni.bokaticowork.core.audit.traffic;

import com.sni.bokaticowork.core.observability.RequestContext;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot A3 : mesure par route (gabarit), erreurs, histogramme, requetes lentes ou en erreur gardees. */
class TrafficA3Test {

    private final TrafficRecorder recorder = new TrafficRecorder();
    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final TrafficFilter filter = new TrafficFilter(recorder, settings);

    @Test
    void requestsAreCountedPerRouteTemplateAndSlowOnesKept() throws Exception {
        when(settings.number(eq(TrafficFilter.SLOW_MS_KEY), anyLong())).thenReturn(0L);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/sni/api/v1/billing/documents/FAC-1");
        request.setAttribute(RequestContext.ATTRIBUTE_USER, "awa@example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/sni/api/v1/billing/documents/{documentNumber}");
            ((MockHttpServletResponse) res).setStatus(503);
        });

        List<TrafficRecorder.MinuteRow> rows = recorder.drainMinutes(null);
        assertThat(rows).singleElement().satisfies(row -> {
            assertThat(row.route()).isEqualTo("/sni/api/v1/billing/documents/{documentNumber}");
            assertThat(row.count()).isEqualTo(1);
            assertThat(row.serverErrors()).isEqualTo(1);
        });
        List<TrafficRecorder.SlowRequest> kept = recorder.drainRequests();
        assertThat(kept).singleElement().satisfies(entry -> {
            assertThat(entry.reason()).isEqualTo("ERROR");
            assertThat(entry.path()).isEqualTo("/sni/api/v1/billing/documents/FAC-1");
            assertThat(entry.userEmail()).isEqualTo("awa@example.com");
        });
    }

    @Test
    void anUnmatchedRequestKeepsNoRawAddressAndWebSocketsAreSkipped() throws Exception {
        when(settings.number(eq(TrafficFilter.SLOW_MS_KEY), anyLong())).thenReturn(60_000L);
        filter.doFilter(new MockHttpServletRequest("GET", "/n-importe-quoi/123"), new MockHttpServletResponse(),
                (req, res) -> ((MockHttpServletResponse) res).setStatus(404));
        filter.doFilter(new MockHttpServletRequest("GET", "/ws/555/abc/websocket"), new MockHttpServletResponse(), (req, res) -> { });
        filter.doFilter(new MockHttpServletRequest("GET", "/sni/api/v1/admin/x"), new MockHttpServletResponse(),
                (req, res) -> ((MockHttpServletResponse) res).setStatus(401));

        assertThat(recorder.drainMinutes(null)).extracting(TrafficRecorder.MinuteRow::route)
                .containsExactlyInAnyOrder(TrafficFilter.UNMATCHED, TrafficFilter.REFUSED);
        assertThat(recorder.drainRequests()).isEmpty();
    }

    @Test
    void onlyFinishedMinutesAreDrained() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MINUTES);
        recorder.record(now.minusSeconds(90), "GET", "/a", 200, 12);
        recorder.record(now.plusSeconds(1), "GET", "/a", 200, 12);
        assertThat(recorder.drainMinutes(now)).hasSize(1);
        assertThat(recorder.drainMinutes(null)).hasSize(1);
    }

    @Test
    void percentilesComeFromTheHistogram() {
        long[] histogram = new long[TrafficRecorder.BOUNDS.length + 1];
        histogram[TrafficRecorder.slot(8)] = 90;
        histogram[TrafficRecorder.slot(450)] = 9;
        histogram[TrafficRecorder.slot(45_000)] = 1;
        assertThat(TrafficRecorder.percentile(histogram, 0.5, 45_000)).isEqualTo(10);
        assertThat(TrafficRecorder.percentile(histogram, 0.95, 45_000)).isEqualTo(500);
        assertThat(TrafficRecorder.percentile(histogram, 1.0, 45_000)).isEqualTo(45_000);
        assertThat(TrafficRecorder.percentile(new long[TrafficRecorder.BOUNDS.length + 1], 0.95, 0)).isZero();
    }
}
