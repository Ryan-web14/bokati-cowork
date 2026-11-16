package com.sni.bokaticowork.core.audit.traffic;

import com.sni.bokaticowork.core.audit.ops.OperationsWriterAccess;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot A3 sur une vraie base : minutes ecrites et additionnees, cumul horaire sans doublon, series,
 * routes et requetes gardees relues. La migration passe la validation du schema.
 */
@Import(TrafficService.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrafficA3IT extends PostgresIntegrationTestBase {

    private static final String ROUTE = "/sni/api/v1/a3-test/{code}";

    @Autowired private JdbcTemplate jdbc;
    @Autowired private TrafficService traffic;

    @AfterEach
    void clean() {
        jdbc.update("DELETE FROM traffic_minute WHERE route = ?", ROUTE);
        jdbc.update("DELETE FROM traffic_hour WHERE route = ?", ROUTE);
        jdbc.update("DELETE FROM request_log WHERE route = ?", ROUTE);
    }

    @Test
    void minutesHoursSeriesRoutesAndRequests() {
        Instant hour = Instant.now().truncatedTo(ChronoUnit.HOURS).minus(2, ChronoUnit.HOURS);
        TrafficRecorder recorder = new TrafficRecorder();
        recorder.record(hour.plusSeconds(60), "GET", ROUTE, 200, 8);
        recorder.record(hour.plusSeconds(60), "GET", ROUTE, 500, 400);
        recorder.record(hour.plusSeconds(600), "GET", ROUTE, 404, 20);
        recorder.keep(new TrafficRecorder.SlowRequest(hour.plusSeconds(61), "req-a3", "GET", "/sni/api/v1/a3-test/X", ROUTE,
                500, 400, "awa@example.com", "1.2.3.4", "ERROR"));
        OperationsWriterAccess.flush(jdbc, recorder);
        // Une seconde ecriture de la meme minute s'additionne (deux dynos, ou arret).
        recorder.record(hour.plusSeconds(60), "GET", ROUTE, 200, 6);
        OperationsWriterAccess.flush(jdbc, recorder);

        List<TrafficService.TrafficPoint> minutes = traffic.series(hour, hour.plus(1, ChronoUnit.HOURS), "MINUTE", ROUTE, null);
        assertThat(minutes).hasSize(2);
        assertThat(minutes.getFirst().requests()).isEqualTo(3);
        assertThat(minutes.getFirst().serverErrors()).isEqualTo(1);
        assertThat(minutes.getFirst().maxMs()).isEqualTo(400);

        assertThat(traffic.rollup(hour, hour.plus(1, ChronoUnit.HOURS))).isEqualTo(1);
        assertThat(traffic.rollup(hour, hour.plus(1, ChronoUnit.HOURS))).isEqualTo(1);
        Long hourly = jdbc.queryForObject("SELECT request_count FROM traffic_hour WHERE route = ?", Long.class, ROUTE);
        assertThat(hourly).isEqualTo(4);

        TrafficService.RouteTraffic route = traffic.routes(hour, hour.plus(1, ChronoUnit.HOURS), "errors").stream()
                .filter(r -> r.route().equals(ROUTE)).findFirst().orElseThrow();
        assertThat(route.requests()).isEqualTo(4);
        assertThat(route.clientErrors()).isEqualTo(1);
        assertThat(route.p95Ms()).isEqualTo(400);

        assertThat(traffic.requests(hour, null, "error", null, "awa", null, 500, 0, 10).getContent())
                .singleElement().extracting(TrafficService.RequestEntry::requestId).isEqualTo("req-a3");
    }
}
