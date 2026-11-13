package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.dto.request.RevalidateQuoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.ReviseQuoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.QuoteNegotiationResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.enums.BillingSignatureStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentAdvanceRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentClauseRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentEarlyPaymentDiscountRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSignatureRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentTaxRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingCalculationService;
import com.sni.bokaticowork.features.billing.service.support.BillingCustomerSnapshotResolver;
import com.sni.bokaticowork.features.billing.service.support.BillingDiscountGuard;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentVersionService;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B2 : un devis se negocie par versions, avec motif, controle des prix et validite prolongee. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteRevisionB2Test {

    @Mock private BillingDocumentRepository documentRepository;
    @Mock private BillingDocumentLineRepository lineRepository;
    @Mock private BillingDocumentDiscountRepository discountRepository;
    @Mock private BillingDocumentTaxRepository taxRepository;
    @Mock private BillingDocumentClauseRepository clauseRepository;
    @Mock private BillingCalculationService calculationService;
    @Mock private com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport lifecycleSupport;
    @Mock private BillingDocumentVersionService versionService;
    @Mock private com.sni.bokaticowork.features.billing.service.support.BillingEventWriter eventWriter;
    @Mock private com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper mapper;
    @Mock private BillingCustomerSnapshotResolver customerSnapshotResolver;
    @Mock private BillingDiscountGuard discountGuard;
    @Mock private BillingDocumentSignatureRepository signatureRepository;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    private BillingDocument quote;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "documentWriter", new BillingDocumentWriter(documentRepository, lineRepository, discountRepository,
                taxRepository, clauseRepository, mock(BillingDocumentAdvanceRepository.class), mock(BillingDocumentEarlyPaymentDiscountRepository.class)));
        quote = BillingDocument.builder().documentNumber("QUO-100").documentType(BillingDocumentType.QUOTE).status(BillingDocumentStatus.NEGOTIATION)
                .locked(false).paidAmount(BigDecimal.ZERO).currency("XAF").dueDate(LocalDate.now().plusDays(2)).build();
        BillingDocumentLine line = BillingDocumentLine.builder().document(quote).lineOrder(1).lineType(BillingLineType.SERVICE)
                .description("Bureau").quantity(BigDecimal.ONE).unitPrice(new BigDecimal("100000")).subtotalAmount(new BigDecimal("100000"))
                .discountRate(BigDecimal.ZERO).discountAmount(BigDecimal.ZERO).taxable(true).build();
        when(documentRepository.findByDocumentNumber("QUO-100")).thenReturn(Optional.of(quote));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(quote)).thenReturn(List.of(line));
        when(customerSnapshotResolver.organizationOf(any(), any())).thenReturn(Optional.empty());
        when(calculationService.calculate(anyList(), anyList())).thenReturn(new BillingCalculationService.CalculatedDocument(
                List.of(BillingDocumentLine.builder().lineOrder(1).build()), new BigDecimal("90000"), BigDecimal.ZERO, List.of(),
                new BigDecimal("90000"), new BigDecimal("16200"), new BigDecimal("810"), new BigDecimal("17010"), new BigDecimal("107010")));
        when(discountGuard.evaluate(anyList(), any(), any())).thenReturn(List.of());
        when(versionService.currentRevision(quote)).thenReturn(2);
    }

    @Test
    void shouldReviseANegotiatedQuoteInANewVersionAndKeepItInNegotiation() {
        service.reviseQuote("QUO-100", new ReviseQuoteRequest("Le client demande 10 % de moins", "CLIENT_PROPOSAL", null, changes()));

        verify(versionService).archive(eq(quote), anyList(), eq("QUOTE_REVISION"));
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.NEGOTIATION);
        assertThat(quote.getVersionReason()).isEqualTo("Le client demande 10 % de moins");
        assertThat(quote.getVersionOrigin()).isEqualTo("CLIENT_PROPOSAL");
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("107010");
        // La validite se prolonge a la revision (30 jours par defaut).
        assertThat(quote.getDueDate()).isAfterOrEqualTo(LocalDate.now().plusDays(30));
    }

    @Test
    void shouldSendAnAcceptedQuoteBackToNegotiationAndCancelItsSignature() {
        quote.setStatus(BillingDocumentStatus.ACCEPTED);
        BillingDocumentSignature signed = BillingDocumentSignature.builder().document(quote).status(BillingSignatureStatus.SIGNED).build();
        when(signatureRepository.findAllByDocumentOrderByCreatedAtDesc(quote)).thenReturn(List.of(signed));

        service.reviseQuote("QUO-100", new ReviseQuoteRequest("Contre-proposition", "COUNTER_PROPOSAL", null, changes()));

        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.NEGOTIATION);
        assertThat(signed.getStatus()).isEqualTo(BillingSignatureStatus.CANCELLED);
    }

    @Test
    void shouldRefuseAPriceUnderTheFloorWithoutDerogation() {
        when(discountGuard.enforce(anyList(), isNull())).thenThrow(new BadRequestException("Remise hors limites : prix sous le plancher"));

        assertThatThrownBy(() -> service.reviseQuote("QUO-100", new ReviseQuoteRequest("Baisse", "INTERNAL", null, changes())))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("plancher");
    }

    @Test
    void shouldRefuseAConvertedQuoteAndAskToRevalidateAnExpiredOne() {
        quote.setStatus(BillingDocumentStatus.CONVERTED);
        assertThatThrownBy(() -> service.reviseQuote("QUO-100", new ReviseQuoteRequest("x", null, null, changes())))
                .isInstanceOf(BadRequestException.class);
        quote.setStatus(BillingDocumentStatus.EXPIRED);
        assertThatThrownBy(() -> service.reviseQuote("QUO-100", new ReviseQuoteRequest("x", null, null, changes())))
                .hasMessageContaining("réactivez");
    }

    @Test
    void shouldRouteTheUpdateOfASentQuoteToTheRevision() {
        // Ecart E9 : un devis envoye, vu ou en negociation n'etait pas modifiable.
        quote.setStatus(BillingDocumentStatus.VIEWED);

        service.update("QUO-100", changes());

        verify(versionService).archive(eq(quote), anyList(), eq("QUOTE_REVISION"));
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.NEGOTIATION);
    }

    @Test
    void shouldRevalidateAnExpiredQuote() {
        quote.setStatus(BillingDocumentStatus.EXPIRED);
        LocalDate until = LocalDate.now().plusDays(15);

        service.revalidateQuote("QUO-100", new RevalidateQuoteRequest(until, "Le client confirme son intérêt"));

        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.SENT);
        assertThat(quote.getDueDate()).isEqualTo(until);
    }

    @Test
    void shouldShowTheNegotiationTimelineWithPriceGaps() {
        when(versionService.rounds(eq(quote), anyList())).thenReturn(List.of(
                snapshot(1, false, "120000", line("Bureau", "1", "120000")),
                snapshot(2, true, "137010", line("Bureau", "1", "100000"), line("Salle", "1", "30000"))));

        QuoteNegotiationResponse negotiation = service.negotiation("QUO-100");

        assertThat(negotiation.rounds()).hasSize(2);
        QuoteNegotiationResponse.Round current = negotiation.rounds().get(1);
        assertThat(current.current()).isTrue();
        assertThat(current.totalDelta()).isEqualByComparingTo("17010");
        assertThat(current.lineChanges()).extracting(QuoteNegotiationResponse.LineChange::change).containsExactly("CHANGED", "ADDED");
        assertThat(current.lineChanges().get(0).unitPriceBefore()).isEqualTo("120000");
        assertThat(current.lineChanges().get(0).unitPriceAfter()).isEqualTo("100000");
    }

    private static Map<String, Object> snapshot(int version, boolean current, String total, Map<String, Object>... lines) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("_version", version);
        snapshot.put("_current", current);
        snapshot.put("status", "NEGOTIATION");
        snapshot.put("totalAmount", total);
        snapshot.put("versionAuthorName", "Awa Mbemba");
        snapshot.put("versionReason", current ? "Baisse consentie" : null);
        snapshot.put("lines", List.of(lines));
        return snapshot;
    }

    private static Map<String, Object> line(String description, String quantity, String unitPrice) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("description", description);
        line.put("quantity", quantity);
        line.put("unitPrice", unitPrice);
        line.put("discountAmount", "0");
        line.put("totalAmount", unitPrice);
        return line;
    }

    private static UpdateBillingDocumentRequest changes() {
        return new UpdateBillingDocumentRequest(null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null);
    }
}
