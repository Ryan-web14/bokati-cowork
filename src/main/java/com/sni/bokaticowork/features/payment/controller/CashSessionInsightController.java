package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.service.interfaces.CashRegisterService;
import com.sni.bokaticowork.features.payment.service.support.CashClosingReportMailer;
import com.sni.bokaticowork.features.payment.service.support.CashSessionInsightService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

/**
 * Ce qu'une session de caisse donne a voir et quelques actions de supervision (17/11/2026) :
 * encaissements detailles, chronologie, indicateurs en direct, reaffectation d'un paiement saisi
 * dans la mauvaise session, envoi du rapport a la demande.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/cash-registers/sessions/{sessionNumber}")
@RequiredArgsConstructor
public class CashSessionInsightController {

    private final CashSessionInsightService insights;
    private final CashRegisterService cashRegisterService;
    private final CashClosingReportMailer reportMailer;

    /** Encaissements et remboursements de la session, avec totaux par mode et par heure. CASH:READ. */
    @GetMapping("/payments")
    public ResponseEntity<CashSessionInsightService.SessionPayments> payments(@PathVariable String sessionNumber,
                                                                             @RequestParam(required = false) String paymentMethod,
                                                                             @RequestParam(required = false) String receivedBy) {
        return ResponseEntity.ok(insights.payments(sessionNumber, paymentMethod, receivedBy));
    }

    /** Tout ce qui s'est passe dans la session, dans l'ordre. CASH:READ. */
    @GetMapping("/timeline")
    public ResponseEntity<List<CashSessionInsightService.TimelineEvent>> timeline(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(insights.timeline(sessionNumber));
    }

    /** Indicateurs en direct. CASH:READ. */
    @GetMapping("/indicators")
    public ResponseEntity<CashSessionInsightService.Indicators> indicators(@PathVariable String sessionNumber) {
        return ResponseEntity.ok(insights.indicators(sessionNumber));
    }

    public record ReassignRequest(@NotBlank String targetSessionNumber, @NotBlank String reason) {
    }

    /** Rattache un paiement en especes a la bonne session. CASH_VARIANCE:DECIDE (encadrement). */
    @com.sni.bokaticowork.core.audit.aop.Audited(module = "CASH", action = "CASH_PAYMENT_REASSIGN", ressource = "cash_session")
    @PatchMapping("/payments/{transactionNumber}/reassign")
    public ResponseEntity<CashRegisterService.ReassignedPayment> reassign(@PathVariable String sessionNumber,
                                                                         @PathVariable String transactionNumber,
                                                                         @Valid @RequestBody ReassignRequest request,
                                                                         Authentication authentication) {
        return ResponseEntity.ok(cashRegisterService.reassignPayment(sessionNumber, transactionNumber,
                request.targetSessionNumber(), request.reason(), authentication == null ? null : authentication.getName()));
    }

    public record SendReportRequest(String extraRecipients) {
    }

    public record SentReport(String sessionNumber, Set<String> recipients) {
    }

    /** Envoie le rapport de session (PDF) au responsable de la caisse, sinon au superviseur, et aux adresses indiquees. CASH:READ. */
    @PostMapping("/report/send")
    public ResponseEntity<SentReport> sendReport(@PathVariable String sessionNumber,
                                                 @RequestBody(required = false) SendReportRequest request,
                                                 Authentication authentication) {
        Set<String> recipients = reportMailer.sendOnDemand(sessionNumber, request == null ? null : request.extraRecipients(),
                authentication == null ? "ADMIN" : authentication.getName());
        return ResponseEntity.ok(new SentReport(sessionNumber, recipients));
    }
}
