package com.sni.bokaticowork.core.audit.alert;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Tableau de bord et alertes du centre d'audit (lot A6). Droit {@code SYSTEM:AUDIT}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/audit")
public class AlertController {

    private final AlertService alerts;

    /** Indicateurs courants (avec leur seuil), trafic de la derniere heure, comptes verrouilles, alertes en cours. */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard() {
        return ResponseEntity.ok(alerts.dashboard());
    }

    /** Regles : seuil en vigueur (parametre), active ou non, valeur mesuree maintenant. */
    @GetMapping("/alerts/rules")
    public ResponseEntity<List<AlertService.RuleView>> rules() {
        return ResponseEntity.ok(alerts.rules());
    }

    @GetMapping("/alerts")
    @Audited(module = "AUDIT", action = "VIEW_ALERTS", ressource = "audit_alert")
    public ResponseEntity<PaginatedResponse<AlertService.Alert>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String rule,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(new PaginatedResponse<>(alerts.list(status, rule, from, to, page, size)));
    }

    @GetMapping("/alerts/{id}")
    public ResponseEntity<AlertService.Alert> get(@PathVariable long id) {
        return ResponseEntity.ok(alerts.get(id));
    }

    /** Accuse de lecture ; l'alerte reste en cours jusqu'a ce que la mesure redescende. */
    @PostMapping("/alerts/{id}/acknowledge")
    @Audited(module = "AUDIT", action = "ACKNOWLEDGE_ALERT", ressource = "audit_alert")
    public ResponseEntity<AlertService.Alert> acknowledge(@PathVariable long id,
                                                          @RequestBody(required = false) Map<String, String> body,
                                                          Authentication authentication) {
        String note = body == null ? null : body.get("note");
        return ResponseEntity.ok(alerts.acknowledge(id, authentication == null ? null : authentication.getName(), note));
    }

    /** Evaluation immediate des regles (sinon chaque minute). */
    @PostMapping("/alerts/evaluate")
    @Audited(module = "AUDIT", action = "EVALUATE_ALERTS", ressource = "audit_alert")
    public ResponseEntity<List<AlertService.RuleView>> evaluate() {
        alerts.evaluate();
        return ResponseEntity.ok(alerts.rules());
    }
}
