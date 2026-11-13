package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentVersionService;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import com.sni.bokaticowork.features.billing.service.support.BillingLifecycleSupport;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/** Lot B1 : le statut d'un document reste juste a l'envoi, a la validation et a la consommation d'un avoir. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BillingLifecycleB1Test {

    @Mock private BillingDocumentRepository documentRepository;
    @Mock private BillingDocumentVersionService versionService;
    @Mock private BillingEventWriter eventWriter;
    @Mock private BillingDocumentMapper mapper;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    private final BillingLifecycleSupport lifecycle = new BillingLifecycleSupport();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "lifecycleSupport", lifecycle);
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldKeepAPaidInvoicePaidWhenItIsSent() {
        BillingDocument invoice = exists(document("INV-1", BillingDocumentType.INVOICE, BillingDocumentStatus.PAID));

        service.send("INV-1");

        assertThat(invoice.getStatus()).isEqualTo(BillingDocumentStatus.PAID);
        assertThat(invoice.getSentAt()).isNotNull();
    }

    @Test
    void shouldRefuseToSendADraft() {
        BillingDocument draft = exists(document("INV-2", BillingDocumentType.INVOICE, BillingDocumentStatus.DRAFT));

        assertThatThrownBy(() -> service.send("INV-2")).isInstanceOf(BadRequestException.class).hasMessageContaining("émet");
        assertThat(draft.getSentAt()).isNull();
    }

    @Test
    void shouldMoveAnIssuedQuoteToSent() {
        BillingDocument quote = exists(document("QUO-1", BillingDocumentType.QUOTE, BillingDocumentStatus.ISSUED));

        service.send("QUO-1");

        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.SENT);
    }

    @Test
    void shouldRefuseToValidateADraftButAcceptAPaidInvoice() {
        assertThatThrownBy(() -> lifecycle.ensureCanValidate(document("INV-3", BillingDocumentType.INVOICE, BillingDocumentStatus.DRAFT)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("émet");
        assertThatCode(() -> lifecycle.ensureCanValidate(document("INV-4", BillingDocumentType.INVOICE, BillingDocumentStatus.PAID)))
                .doesNotThrowAnyException();
        assertThatCode(() -> lifecycle.ensureCanValidate(document("CRN-1", BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.ISSUED)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldKeepAnIssuedCreditNoteUsable() {
        // L'emission marquait l'avoir consomme : il n'etait plus imputable ni remboursable.
        assertThat(BillingLifecycleSupport.creditNoteConsumed(document("CRN-2", BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.ISSUED))).isFalse();
        assertThat(BillingLifecycleSupport.creditNoteConsumed(document("CRN-3", BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED))).isFalse();
        assertThat(BillingLifecycleSupport.creditNoteConsumed(document("CRN-4", BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.APPLIED))).isTrue();
        assertThat(BillingLifecycleSupport.creditNoteConsumed(document("CRN-5", BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.REFUNDED))).isTrue();
        assertThat(BillingReceivables.creditAvailable(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.APPLIED)).isFalse();
        assertThat(BillingReceivables.creditAvailable(BillingDocumentType.CREDIT_NOTE, BillingDocumentStatus.VALIDATED)).isTrue();
    }

    @Test
    void shouldCancelPendingAdvancesBeforeExpiringQuotes() {
        when(documentRepository.expireQuotes()).thenReturn(2);

        assertThat(service.expireQuotes()).isEqualTo(2);

        InOrder order = inOrder(documentRepository);
        order.verify(documentRepository).cancelPendingAdvancesOfExpiringQuotes();
        order.verify(documentRepository).expireQuotes();
    }

    private BillingDocument exists(BillingDocument document) {
        when(documentRepository.findByDocumentNumber(document.getDocumentNumber())).thenReturn(Optional.of(document));
        when(documentRepository.lockByDocumentNumber(document.getDocumentNumber())).thenReturn(Optional.of(document));
        return document;
    }

    private static BillingDocument document(String number, BillingDocumentType type, BillingDocumentStatus status) {
        return BillingDocument.builder().documentNumber(number).documentType(type).status(status).locked(false)
                .customerType("MEMBER").customerCode("MBR-1").currency("XAF").totalAmount(new BigDecimal("10000")).build();
    }
}
