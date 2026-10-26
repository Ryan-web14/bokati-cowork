package com.sni.bokaticowork.features.payment.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService;
import com.sni.bokaticowork.features.payment.service.pawaypay.MobileMoneyOperationsService.Action;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

/**
 * L'outil admin des paiements mobile money · liste, synthese, fiche comparee a PawaPay,
 * resolution d'un litige, rapprochement d'une periode.
 *
 * <p>Consulter et relire : ADMIN, SUPER_ADMIN, MANAGER. Forcer l'issue d'un paiement : ADMIN et
 * SUPER_ADMIN seulement, motif obligatoire, decision tracee et transmise aux super-administrateurs.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/payments/mobile-money/operations")
@RequiredArgsConstructor
public class MobileMoneyOperationsController {

    private static final String STAFF = "hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('MANAGER')";
    private static final String DECIDERS = "hasRole('ADMIN') or hasRole('SUPER_ADMIN')";

    private final MobileMoneyOperationsService service;

    @Value("${app.time-zone:Africa/Lagos}")
    private String timeZone;

    /** Corps d'une resolution · {@code providerTransactionId} exige pour declarer un paiement reussi. */
    public record ResolveRequest(Action action, String reason, String providerTransactionId) {
    }

    @GetMapping
    @PreAuthorize(STAFF)
    public ResponseEntity<PaginatedResponse<MobileMoneyDepositResponse>> search(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) String customerCode,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String reference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        MobileMoneyOperationsService.Filter filter = new MobileMoneyOperationsService.Filter(
                start(from), end(to), status, provider, customerCode, phone, reference);
        return ResponseEntity.ok(service.search(filter,
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), MobileMoneyOperationsService.defaultSort())));
    }

    /** Nombre et montant par statut sur la periode · 30 derniers jours par defaut. */
    @GetMapping("/summary")
    @PreAuthorize(STAFF)
    public ResponseEntity<List<MobileMoneyOperationsService.StatusTotal>> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(service.summary(startOrDefault(from), endOrToday(to)));
    }

    /** Les paiements de la periode qui divergent de PawaPay · 7 derniers jours par defaut. */
    @GetMapping("/reconciliation")
    @PreAuthorize(STAFF)
    public ResponseEntity<MobileMoneyOperationsService.Reconciliation> reconciliation(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate first = from == null ? LocalDate.now(zone()).minusDays(7) : from;
        return ResponseEntity.ok(service.reconcile(start(first), endOrToday(to)));
    }

    /** La fiche d'un paiement · notre base, PawaPay relu en direct, les rappels, les ecarts. */
    @GetMapping("/{depositId}")
    @PreAuthorize(STAFF)
    public ResponseEntity<MobileMoneyOperationsService.Detail> detail(@PathVariable String depositId) {
        return ResponseEntity.ok(service.detail(depositId));
    }

    @PostMapping("/{depositId}/resolve")
    @Audited(module = "MOBILE_MONEY", action = "RESOLVE", ressource = "pawapay_deposit")
    @PreAuthorize(STAFF)
    public ResponseEntity<MobileMoneyOperationsService.Detail> resolve(@PathVariable String depositId,
                                                                     @RequestBody ResolveRequest request,
                                                                     Authentication authentication) {
        Action action = request == null ? null : request.action();
        // Relire PawaPay est sans risque · forcer une issue engage de l'argent, reserve aux administrateurs.
        if (action != null && action != Action.SYNC && !isDecider(authentication)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Seuls un administrateur ou un super-administrateur peuvent forcer l'issue d'un paiement.");
        }
        return ResponseEntity.ok(service.resolve(depositId, action,
                request == null ? null : request.reason(),
                request == null ? null : request.providerTransactionId(),
                authentication == null ? "SYSTEM" : authentication.getName()));
    }

    private static boolean isDecider(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()) || "ROLE_SUPER_ADMIN".equals(a.getAuthority()));
    }

    private ZoneId zone() {
        return ZoneId.of(timeZone);
    }

    private Instant start(LocalDate day) {
        return day == null ? null : day.atStartOfDay(zone()).toInstant();
    }

    private Instant end(LocalDate day) {
        return day == null ? null : day.plusDays(1).atStartOfDay(zone()).toInstant();
    }

    private Instant startOrDefault(LocalDate day) {
        return start(day == null ? LocalDate.now(zone()).minusDays(30) : day);
    }

    private Instant endOrToday(LocalDate day) {
        return end(day == null ? LocalDate.now(zone()) : day);
    }
}
