package com.sni.bokaticowork.core.audit.traffic;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

/** Trafic de l'API (centre d'audit, lot A3). Droit {@code SYSTEM:AUDIT}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/audit/traffic")
public class TrafficController {

    private final TrafficService traffic;

    /** Serie : requetes, erreurs 4xx et 5xx, duree moyenne, p50, p95 et maximale par periode. */
    @GetMapping
    public ResponseEntity<List<TrafficService.TrafficPoint>> series(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String granularity,
            @RequestParam(required = false) String route,
            @RequestParam(required = false) String method) {
        return ResponseEntity.ok(traffic.series(from, to, granularity, route, method));
    }

    /** Routes de la periode ; {@code sort} : requests (defaut), errors ou slow. */
    @GetMapping("/routes")
    public ResponseEntity<List<TrafficService.RouteTraffic>> routes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(traffic.routes(from, to, sort));
    }

    /** Requetes lentes (reason SLOW) ou en erreur serveur (reason ERROR), une par une. */
    @GetMapping("/requests")
    public ResponseEntity<PaginatedResponse<TrafficService.RequestEntry>> requests(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String reason,
            @RequestParam(required = false) String route,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) Integer minStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(
                traffic.requests(from, to, reason, route, user, requestId, minStatus, page, size)));
    }
}
