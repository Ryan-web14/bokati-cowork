package com.sni.bokaticowork.core.audit.retention;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Integrite et conservation des journaux du centre d'audit (lot A7). Droit {@code SYSTEM:AUDIT}. */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/admin/audit")
public class RetentionController {

    private final AuditIntegrity integrity;
    private final AuditRetention retention;

    /** Verification du chainage du journal des actions et des evenements de securite. */
    @GetMapping("/integrity")
    @Audited(module = "AUDIT", action = "VERIFY_INTEGRITY", ressource = "audit_log")
    public ResponseEntity<List<AuditIntegrity.Report>> integrity() {
        return ResponseEntity.ok(integrity.verify());
    }

    /** Duree de conservation en vigueur, nombre de lignes et plus ancienne ligne de chaque journal. */
    @GetMapping("/retention")
    public ResponseEntity<List<AuditRetention.TargetView>> retention() {
        return ResponseEntity.ok(retention.view());
    }

    /** Application immediate des durees (sinon chaque nuit a 3 h 30). */
    @PostMapping("/retention/run")
    @Audited(module = "AUDIT", action = "RUN_RETENTION", ressource = "audit_log")
    public ResponseEntity<Map<String, Long>> run() {
        return ResponseEntity.ok(retention.purge());
    }
}
