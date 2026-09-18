package com.sni.bokaticowork.features.subscription.lifecycle.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionExitItem;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionTermination;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionTerminationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Les resiliations · un preavis, des frais chiffres avant d'etre acceptes, une liste de sortie.
 *
 * <p>Toujours prevoir avant de demander : la prevision dit la date d'effet la plus proche, les frais
 * de rupture et ce que la liste de sortie contiendra.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions/terminations")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class SubscriptionTerminationController {

    private final SubscriptionTerminationService terminationService;

    public record TerminationRequest(@NotNull SubscriptionTermination.ReasonCategory reasonCategory, String reason,
                                     SubscriptionTermination.Channel channel, LocalDate requestedEffectiveDate, Integer noticePeriodDays) {
        SubscriptionTerminationService.Request toRequest() {
            return new SubscriptionTerminationService.Request(reasonCategory, reason, channel, requestedEffectiveDate, noticePeriodDays);
        }
    }

    public record ReasonRequest(@NotBlank String reason) {
    }

    public record ItemRequest(String detail) {
    }

    public record ExitItemView(String itemCode, String label, boolean mandatory, SubscriptionExitItem.Status status, String detail,
                               String doneBy, Instant doneAt) {
        static ExitItemView of(SubscriptionExitItem i) {
            return new ExitItemView(i.getItemCode(), i.getLabel(), Boolean.TRUE.equals(i.getMandatory()), i.getStatus(), i.getDetail(),
                    i.getDoneBy(), i.getDoneAt());
        }
    }

    public record TerminationView(String terminationCode, String subscriptionNumber, SubscriptionTermination.Status status,
                                  Instant requestedAt, String requestedBy, SubscriptionTermination.Channel channel, Integer noticePeriodDays,
                                  LocalDate effectiveDate, SubscriptionTermination.ReasonCategory reasonCategory, String reason,
                                  boolean earlyTermination, Integer remainingCommitmentMonths, BigDecimal feeAmount, BigDecimal feeDue,
                                  boolean feeWaived, String feeWaivedBy, String feeWaivedReason, String feeBillableNumber,
                                  String bridgingBillableNumber, String acceptedBy, Instant acceptedAt, String retractedBy, Instant retractedAt,
                                  Instant completedAt, boolean exitChecklistClear, List<ExitItemView> exitItems, String notes) {
        static TerminationView of(SubscriptionTermination t) {
            return new TerminationView(t.getTerminationCode(), t.getSubscription().getSubscriptionNumber(), t.getStatus(), t.getRequestedAt(),
                    t.getRequestedBy(), t.getChannel(), t.getNoticePeriodDays(), t.getEffectiveDate(), t.getReasonCategory(), t.getReason(),
                    Boolean.TRUE.equals(t.getEarlyTermination()), t.getRemainingCommitmentMonths(), t.getFeeAmount(), t.feeDue(),
                    Boolean.TRUE.equals(t.getFeeWaived()), t.getFeeWaivedBy(), t.getFeeWaivedReason(), t.getFeeBillableNumber(),
                    t.getBridgingBillableNumber(), t.getAcceptedBy(), t.getAcceptedAt(), t.getRetractedBy(), t.getRetractedAt(),
                    t.getCompletedAt(), t.exitChecklistClear(), t.getExitItems().stream().map(ExitItemView::of).toList(), t.getNotes());
        }
    }

    @PostMapping("/subscriptions/{subscriptionNumber}/preview")
    public ResponseEntity<SubscriptionTerminationService.Preview> preview(@PathVariable String subscriptionNumber,
                                                                          @Valid @RequestBody TerminationRequest request) {
        return ResponseEntity.ok(terminationService.preview(subscriptionNumber, request.toRequest()));
    }

    @PostMapping("/subscriptions/{subscriptionNumber}")
    public ResponseEntity<TerminationView> request(@PathVariable String subscriptionNumber, @Valid @RequestBody TerminationRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.request(subscriptionNumber, request.toRequest(), actor(authentication))));
    }

    @GetMapping("/subscriptions/{subscriptionNumber}")
    public ResponseEntity<List<TerminationView>> ofSubscription(@PathVariable String subscriptionNumber) {
        return ResponseEntity.ok(terminationService.ofSubscription(subscriptionNumber).stream().map(TerminationView::of).toList());
    }

    @GetMapping
    public ResponseEntity<List<TerminationView>> byStatus(@RequestParam(defaultValue = "REQUESTED") SubscriptionTermination.Status status) {
        return ResponseEntity.ok(terminationService.byStatus(status).stream().map(TerminationView::of).toList());
    }

    /** Date d'effet passee, liste de sortie encore ouverte · le tableau des sorties a debloquer. */
    @GetMapping("/blocked")
    public ResponseEntity<List<TerminationView>> blocked() {
        return ResponseEntity.ok(terminationService.blocked().stream().map(TerminationView::of).toList());
    }

    @GetMapping("/{terminationCode}")
    public ResponseEntity<TerminationView> get(@PathVariable String terminationCode) {
        return ResponseEntity.ok(TerminationView.of(terminationService.get(terminationCode)));
    }

    @PostMapping("/{terminationCode}/accept")
    public ResponseEntity<TerminationView> accept(@PathVariable String terminationCode, Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.accept(terminationCode, actor(authentication))));
    }

    @PostMapping("/{terminationCode}/retract")
    public ResponseEntity<TerminationView> retract(@PathVariable String terminationCode, @RequestBody(required = false) ReasonRequest request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.retract(terminationCode,
                request == null ? null : request.reason(), actor(authentication))));
    }

    @PostMapping("/{terminationCode}/waive-fee")
    @PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
    public ResponseEntity<TerminationView> waiveFee(@PathVariable String terminationCode, @Valid @RequestBody ReasonRequest request,
                                                    Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.waiveFee(terminationCode, request.reason(), actor(authentication))));
    }

    @PostMapping("/{terminationCode}/exit-items/{itemCode}/done")
    public ResponseEntity<TerminationView> tick(@PathVariable String terminationCode, @PathVariable String itemCode,
                                                @RequestBody(required = false) ItemRequest request, Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.tick(terminationCode, itemCode,
                request == null ? null : request.detail(), actor(authentication))));
    }

    @PostMapping("/{terminationCode}/exit-items/{itemCode}/waive")
    public ResponseEntity<TerminationView> waiveItem(@PathVariable String terminationCode, @PathVariable String itemCode,
                                                     @Valid @RequestBody ReasonRequest request, Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.waiveItem(terminationCode, itemCode, request.reason(), actor(authentication))));
    }

    /** Acheve avant la date d'effet · le client est parti, la liste est vide. */
    @PostMapping("/{terminationCode}/complete")
    public ResponseEntity<TerminationView> complete(@PathVariable String terminationCode, Authentication authentication) {
        return ResponseEntity.ok(TerminationView.of(terminationService.completeNow(terminationCode, actor(authentication))));
    }

    private static String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
    }
}
