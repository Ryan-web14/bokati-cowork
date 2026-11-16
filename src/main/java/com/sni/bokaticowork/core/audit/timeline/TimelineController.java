package com.sni.bokaticowork.core.audit.timeline;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;

/**
 * Centre unifie (lot A5) : chronologie commune de tous les journaux. Droit {@code SYSTEM:AUDIT}.
 * Vues : {@code user} (ce qu'a fait un utilisateur), {@code entityCode} et {@code entityType} (ce qui
 * est arrive a un element), {@code requestId} (ce qui s'est passe dans une requete).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/audit/timeline")
public class TimelineController {

    static final String EXPORT_MAX_ROWS_KEY = "bokati.audit.export.max-rows";

    private final TimelineService timeline;
    private final PlatformSettings settings;

    @GetMapping
    public ResponseEntity<TimelineService.Page> read(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String sources,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer limit) {
        TimelineService.Filter filter = new TimelineService.Filter(from, to, user, entityType, entityCode, requestId,
                TimelineService.parseSources(sources));
        return ResponseEntity.ok(timeline.read(filter, cursor, limit));
    }

    /** Sources lisibles dans la chronologie. */
    @GetMapping("/sources")
    public ResponseEntity<Set<String>> sources() {
        return ResponseEntity.ok(TimelineService.sourceNames());
    }

    /** Export CSV de la chronologie, memes filtres ; l'export est lui-meme journalise. */
    @GetMapping("/export")
    @Audited(module = "AUDIT", action = "EXPORT_TIMELINE", ressource = "timeline")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String user,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String entityCode,
            @RequestParam(required = false) String requestId,
            @RequestParam(required = false) String sources) {
        TimelineService.Filter filter = new TimelineService.Filter(from, to, user, entityType, entityCode, requestId,
                TimelineService.parseSources(sources));
        TimelineService.Export export = timeline.export(filter, (int) settings.number(EXPORT_MAX_ROWS_KEY, 10_000));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename("chronologie-" + LocalDate.now() + ".csv").build());
        headers.add("X-Export-Rows", String.valueOf(export.rows()));
        headers.add("X-Export-Truncated", String.valueOf(export.truncated()));
        headers.add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "X-Export-Rows, X-Export-Truncated, Content-Disposition");
        return ResponseEntity.ok().headers(headers).body(export.csv().getBytes(StandardCharsets.UTF_8));
    }
}
