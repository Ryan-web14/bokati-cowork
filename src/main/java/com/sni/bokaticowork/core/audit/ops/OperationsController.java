package com.sni.bokaticowork.core.audit.ops;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Evenements applicatifs (centre d'audit, lot A4) : sante, taches planifiees, erreurs serveur.
 * Droit {@code SYSTEM:AUDIT}.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/audit")
public class OperationsController {

    private final OperationsService operations;

    /** Sante : services (base, Redis, RabbitMQ...), boite de sortie, integrations, taches, erreurs. */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(operations.health());
    }

    /** Etat de chaque tache planifiee, les taches en echec d'abord. */
    @GetMapping("/jobs")
    public ResponseEntity<List<OperationsService.TaskStatus>> jobs() {
        return ResponseEntity.ok(operations.tasks());
    }

    /** Executions gardees une par une : echecs, executions lentes, executions des taches rares. */
    @GetMapping("/jobs/runs")
    public ResponseEntity<PaginatedResponse<OperationsService.TaskRun>> runs(
            @RequestParam(required = false) String task,
            @RequestParam(required = false) String outcome,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(operations.runs(task, outcome, from, to, page, size)));
    }

    /** Erreurs serveur (5xx), sans la trace. */
    @GetMapping("/errors")
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "AUDIT", action = "VIEW_ERRORS", ressource = "app_error")
    public ResponseEntity<PaginatedResponse<OperationsService.AppError>> errors(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String path,
            @RequestParam(required = false) String exception,
            @RequestParam(required = false) String requestId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(operations.errors(from, to, path, exception, requestId, page, size)));
    }

    /** Une erreur serveur, avec sa trace tronquee. */
    @GetMapping("/errors/{id}")
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "AUDIT", action = "VIEW_ERROR", ressource = "app_error")
    public ResponseEntity<OperationsService.AppError> error(@PathVariable long id) {
        return ResponseEntity.ok(operations.error(id));
    }
}
