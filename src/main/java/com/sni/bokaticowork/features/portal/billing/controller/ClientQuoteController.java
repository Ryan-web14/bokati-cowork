package com.sni.bokaticowork.features.portal.billing.controller;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.ClaimDtos;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.payment.dto.response.MobileMoneyDepositResponse;
import com.sni.bokaticowork.features.portal.billing.dto.request.ClientInitiateMobileMoneyPaymentRequest;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceResponse;
import com.sni.bokaticowork.features.portal.billing.dto.response.ClientInvoiceSummaryResponse;
import com.sni.bokaticowork.features.portal.billing.service.ClientQuoteService;
import com.sni.bokaticowork.features.portal.context.ClientContextService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lot C16 : devis, proformas et reclamations dans l'espace client. Le membre connecte ne voit que
 * ses pieces ; les factures, leur paiement et les releves restent sur les routes existantes.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/client/billing")
@RequiredArgsConstructor
public class ClientQuoteController {

    private final ClientContextService clientContextService;
    private final ClientQuoteService quotes;

    /** Mes devis ({@code type=QUOTE}, par defaut) ou mes proformas ({@code type=PROFORMA_INVOICE}). */
    @GetMapping("/quotes")
    public ResponseEntity<PaginatedResponse<ClientInvoiceSummaryResponse>> list(@RequestParam(defaultValue = "QUOTE") BillingDocumentType type,
                                                                                @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(quotes.list(member(), type, pageable));
    }

    @GetMapping("/quotes/{documentNumber}")
    public ResponseEntity<ClientInvoiceResponse> get(@PathVariable String documentNumber) {
        return ResponseEntity.ok(quotes.get(member(), documentNumber));
    }

    @GetMapping("/quotes/{documentNumber}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable String documentNumber) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + documentNumber + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(quotes.pdf(member(), documentNumber));
    }

    @PostMapping("/quotes/{documentNumber}/accept")
    public ResponseEntity<ClientInvoiceResponse> accept(@PathVariable String documentNumber) {
        return ResponseEntity.ok(quotes.accept(member(), documentNumber));
    }

    @PostMapping("/quotes/{documentNumber}/reject")
    public ResponseEntity<ClientInvoiceResponse> reject(@PathVariable String documentNumber) {
        return ResponseEntity.ok(quotes.reject(member(), documentNumber));
    }

    /** Paiement d'une proforma par mobile money : il devient un acompte du devis. */
    @PostMapping("/proformas/{documentNumber}/pay/mobile-money")
    public ResponseEntity<MobileMoneyDepositResponse> payProforma(@PathVariable String documentNumber,
                                                                  @Valid @RequestBody ClientInitiateMobileMoneyPaymentRequest request) {
        return ResponseEntity.ok(quotes.payProforma(member(), documentNumber, request));
    }

    // ------------------------------------------------------------------ reclamations

    @GetMapping("/claim-reasons")
    public ResponseEntity<List<ClaimDtos.ReasonView>> claimReasons() {
        return ResponseEntity.ok(quotes.claimReasons());
    }

    @PostMapping("/invoices/{documentNumber}/claims")
    public ResponseEntity<ClaimDtos.ClaimView> openClaim(@PathVariable String documentNumber, @Valid @RequestBody ClaimDtos.OpenRequest request) {
        return ResponseEntity.ok(quotes.openClaim(member(), documentNumber, request));
    }

    @GetMapping("/claims")
    public ResponseEntity<List<ClaimDtos.ClaimView>> claims() {
        return ResponseEntity.ok(quotes.claims(member()));
    }

    @PostMapping("/claims/{claimNumber}/withdraw")
    public ResponseEntity<ClaimDtos.ClaimView> withdraw(@PathVariable String claimNumber) {
        return ResponseEntity.ok(quotes.withdrawClaim(member(), claimNumber));
    }

    private Member member() {
        return clientContextService.getAuthenticatedMember();
    }
}
