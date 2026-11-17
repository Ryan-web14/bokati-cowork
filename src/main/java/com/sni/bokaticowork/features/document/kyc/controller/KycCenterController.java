package com.sni.bokaticowork.features.document.kyc.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.kyc.service.support.KycCenterService;
import com.sni.bokaticowork.features.document.kyc.service.support.KycReasons;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Lot KG7 : pilotage du centre KYC. Lectures {@code KYC:READ}, ecritures {@code KYC:MANAGE}
 * (regle d'URL {@code /kyc}).
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(ApiPath.V1 + "/kyc")
public class KycCenterController {

    private final KycCenterService center;
    private final KycReasons reasons;

    /** Dossiers par statut, files par anciennete, delais, retards, charge par agent, dossiers bloques. */
    @GetMapping("/dashboard/center")
    public ResponseEntity<KycCenterService.CenterDashboard> dashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(center.dashboard(from, to));
    }

    /** Pieces qui expirent dans les {@code days} jours (90 par defaut), par semaine. */
    @GetMapping("/renewals")
    public ResponseEntity<List<KycCenterService.RenewalWeek>> renewals(@RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(center.renewals(days));
    }

    public record RemindRequest(Integer days, List<Long> kycDocumentIds) {
    }

    /** Relance groupee des titulaires ; une piece relancee il y a moins de 7 jours est sautee. */
    @PostMapping("/renewals/remind")
    @Audited(module = "KYC", action = "RENEWAL_REMIND", ressource = "kyc_document")
    public ResponseEntity<KycCenterService.RemindResult> remind(@RequestBody(required = false) RemindRequest request) {
        int days = request == null || request.days() == null ? 90 : request.days();
        return ResponseEntity.ok(center.remind(days, request == null ? null : request.kycDocumentIds()));
    }

    /** Meme piece (type et numero normalise) sur plusieurs titulaires. */
    @GetMapping("/duplicates")
    public ResponseEntity<List<KycCenterService.DuplicateGroup>> duplicates() {
        return ResponseEntity.ok(center.duplicates());
    }

    /** Motifs de refus et de correction ; {@code kind} = REJECTION ou CORRECTION pour filtrer. */
    @GetMapping("/reasons")
    public ResponseEntity<List<KycReasons.ReasonView>> reasons(@RequestParam(required = false) String kind,
                                                                @RequestParam(defaultValue = "true") boolean activeOnly) {
        return ResponseEntity.ok(reasons.list(kind, activeOnly));
    }

    @PostMapping("/reasons")
    @Audited(module = "KYC", action = "CREATE_REASON", ressource = "kyc_reason")
    public ResponseEntity<KycReasons.ReasonView> createReason(@RequestBody KycReasons.ReasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reasons.create(request));
    }

    @PutMapping("/reasons/{code}")
    @Audited(module = "KYC", action = "UPDATE_REASON", ressource = "kyc_reason")
    public ResponseEntity<KycReasons.ReasonView> updateReason(@PathVariable String code, @RequestBody KycReasons.ReasonRequest request) {
        return ResponseEntity.ok(reasons.update(code, request));
    }

    @DeleteMapping("/reasons/{code}")
    @Audited(module = "KYC", action = "DELETE_REASON", ressource = "kyc_reason")
    public ResponseEntity<Void> deleteReason(@PathVariable String code) {
        reasons.delete(code);
        return ResponseEntity.noContent().build();
    }
}
