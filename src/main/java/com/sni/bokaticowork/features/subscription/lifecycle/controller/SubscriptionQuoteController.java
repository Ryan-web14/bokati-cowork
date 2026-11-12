package com.sni.bokaticowork.features.subscription.lifecycle.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.lifecycle.model.SubscriptionQuote;
import com.sni.bokaticowork.features.subscription.lifecycle.service.SubscriptionQuoteService;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Les devis · un abonnement promis, converti a l'acceptation. */
@RestController
@RequestMapping(ApiPath.V1 + "/subscriptions/quotes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN') or hasRole('STAFF')")
public class SubscriptionQuoteController {

    private final SubscriptionQuoteService quoteService;

    public record QuoteRequest(@NotNull SubscriberType subscriberType, @NotBlank String subscriberCode, @NotBlank String planCode,
                               String planVersionId, BillingCycle billingCycle, BigDecimal quotedPrice, BigDecimal setupFee,
                               Integer commitmentMonths, Integer trialDays, LocalDate startDate, LocalDate validUntil, String notes,
                               String proposalNumber) {
        SubscriptionQuoteService.Request toRequest() {
            return new SubscriptionQuoteService.Request(subscriberType, subscriberCode, planCode, planVersionId, billingCycle, quotedPrice,
                    setupFee, commitmentMonths, trialDays, startDate, validUntil, notes, proposalNumber);
        }
    }

    public record ReasonRequest(String reason) {
    }

    /** Facture ou paiement deja faits pour l'abonnement (facultatif). */
    public record AcceptRequest(String existingInvoiceNumber, String existingPaymentNumber) {
    }

    public record QuoteView(String quoteNumber, SubscriptionQuote.Status status, SubscriberType subscriberType, String subscriberCode,
                            String subscriberName, String subscriberEmail, String planCode, String planName, Integer planVersionNumber,
                            BillingCycle billingCycle, String currency, BigDecimal cataloguePrice, BigDecimal quotedPrice, boolean negotiated,
                            BigDecimal setupFee, Integer commitmentMonths, Integer trialDays, LocalDate startDate, LocalDate validUntil,
                            String notes, String preparedBy, Instant sentAt, Instant acceptedAt, String acceptedBy, Instant rejectedAt,
                            String rejectionReason, String convertedSubscriptionNumber, String derivationCode, String proposalNumber,
                            Instant createdAt, List<String> warnings) {
        static QuoteView of(SubscriptionQuote q) {
            return of(q, List.of());
        }

        static QuoteView of(SubscriptionQuote q, List<String> warnings) {
            return new QuoteView(q.getQuoteNumber(), q.getStatus(), q.getSubscriberType(), q.getSubscriberCode(), q.getSubscriberName(),
                    q.getSubscriberEmail(), q.getPlanVersion().getPlan().getCode(), q.getPlanVersion().getName(), q.getPlanVersion().getVersionNumber(),
                    q.getBillingCycle(), q.getCurrency(), q.getCataloguePrice(), q.getQuotedPrice(), q.negotiated(), q.getSetupFee(),
                    q.getCommitmentMonths(), q.getTrialDays(), q.getStartDate(), q.getValidUntil(), q.getNotes(), q.getPreparedBy(),
                    q.getSentAt(), q.getAcceptedAt(), q.getAcceptedBy(), q.getRejectedAt(), q.getRejectionReason(),
                    q.getConvertedSubscriptionNumber(), q.getDerivationCode(), q.getProposalNumber(), q.getCreatedAt(), warnings);
        }
    }

    @PostMapping
    public ResponseEntity<QuoteView> create(@Valid @RequestBody QuoteRequest request, Authentication authentication) {
        return ResponseEntity.ok(view(quoteService.create(request.toRequest(), actor(authentication), admin(authentication))));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<QuoteView>> list(@RequestParam(required = false) SubscriptionQuote.Status status,
                                                             @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(new PaginatedResponse<>(quoteService.list(status,
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize())).map(QuoteView::of)));
    }

    @GetMapping("/subscribers/{subscriberType}/{subscriberCode}")
    public ResponseEntity<List<QuoteView>> ofSubscriber(@PathVariable SubscriberType subscriberType, @PathVariable String subscriberCode) {
        return ResponseEntity.ok(quoteService.ofSubscriber(subscriberType, subscriberCode).stream().map(QuoteView::of).toList());
    }

    @GetMapping("/{quoteNumber}")
    public ResponseEntity<QuoteView> get(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(view(quoteService.get(quoteNumber)));
    }

    /** Ce que la conversion donnera · ecart au catalogue, visa a prevoir. */
    @GetMapping("/{quoteNumber}/outlook")
    public ResponseEntity<SubscriptionQuoteService.Outlook> outlook(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(quoteService.outlook(quoteService.get(quoteNumber)));
    }

    @PostMapping("/{quoteNumber}/send")
    public ResponseEntity<QuoteView> send(@PathVariable String quoteNumber) {
        return ResponseEntity.ok(QuoteView.of(quoteService.send(quoteNumber)));
    }

    @PostMapping("/{quoteNumber}/accept")
    public ResponseEntity<QuoteView> accept(@PathVariable String quoteNumber, @RequestBody(required = false) AcceptRequest request,
                                            Authentication authentication) {
        return ResponseEntity.ok(view(quoteService.accept(quoteNumber, actor(authentication), admin(authentication),
                request == null ? null : request.existingInvoiceNumber(), request == null ? null : request.existingPaymentNumber())));
    }

    @PostMapping("/{quoteNumber}/reject")
    public ResponseEntity<QuoteView> reject(@PathVariable String quoteNumber, @RequestBody(required = false) ReasonRequest request) {
        return ResponseEntity.ok(QuoteView.of(quoteService.reject(quoteNumber, request == null ? null : request.reason())));
    }

    private QuoteView view(SubscriptionQuote quote) {
        return QuoteView.of(quote, quoteService.warnings(quote));
    }

    /** Administrateur : peut lever le plancher et le rabais maximum d'un devis. */
    static boolean admin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream().map(a -> a.getAuthority())
                .anyMatch(a -> a.equals("ROLE_ADMIN") || a.equals("ROLE_SUPER_ADMIN") || a.equals("ADMIN") || a.equals("SUPER_ADMIN"));
    }

    private static String actor(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "STAFF" : authentication.getName();
    }
}
