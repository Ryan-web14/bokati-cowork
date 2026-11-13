package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.dto.request.ConfirmSignatureRequest;
import com.sni.bokaticowork.features.billing.dto.response.PublicSignatureConfirmation;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.enums.BillingSignatureStatus;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentSignature;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentSignatureRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingDocumentVersionService;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.RecordComponent;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Lot B0 : la signature publique d'un devis ne renvoie que l'essentiel et refuse un devis sorti du circuit ou revise. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteSignatureConfirmTest {

    @Mock private BillingDocumentSignatureRepository signatureRepository;
    @Mock private BillingEventWriter eventWriter;
    @Mock private BillingDocumentVersionService versionService;

    @InjectMocks
    private QuoteSignatureServiceImpl service;

    private BillingDocument quote;
    private BillingDocumentSignature signature;
    private final ConfirmSignatureRequest request = new ConfirmSignatureRequest("Awa Mbemba", "awa@example.com", "data:image/png;base64,AAA", "1.2.3.4", "test");

    @BeforeEach
    void setUp() {
        quote = BillingDocument.builder().documentNumber("QUO-1").documentType(BillingDocumentType.QUOTE).status(BillingDocumentStatus.SENT)
                .issueDate(LocalDate.of(2026, 11, 13)).totalAmount(new BigDecimal("590000")).currency("XAF")
                .internalNotes("Marge serree, ne pas descendre").build();
        signature = BillingDocumentSignature.builder().document(quote).signatureToken("t").status(BillingSignatureStatus.PENDING)
                .expiresAt(Instant.now().plusSeconds(3600)).documentRevision(2).build();
        when(signatureRepository.findBySignatureTokenWithDocument(anyString())).thenReturn(Optional.of(signature));
        when(signatureRepository.save(any(BillingDocumentSignature.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(versionService.currentRevision(quote)).thenReturn(2);
    }

    @Test
    void shouldReturnOnlyWhatTheSignerSaw() {
        PublicSignatureConfirmation confirmation = service.confirmSignature("t", request);

        assertThat(confirmation.quoteNumber()).isEqualTo("QUO-1");
        assertThat(confirmation.signatureStatus()).isEqualTo("SIGNED");
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.ACCEPTED);
        assertThat(Arrays.stream(PublicSignatureConfirmation.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("internalNotes", "recoverables", "lines");
    }

    @Test
    void shouldRefuseACancelledOrConvertedQuote() {
        for (BillingDocumentStatus status : new BillingDocumentStatus[]{BillingDocumentStatus.CANCELLED, BillingDocumentStatus.CONVERTED,
                BillingDocumentStatus.REJECTED}) {
            quote.setStatus(status);
            signature.setStatus(BillingSignatureStatus.PENDING);
            assertThatThrownBy(() -> service.confirmSignature("t", request)).isInstanceOf(ConflictException.class);
            assertThat(quote.getStatus()).isEqualTo(status);
            assertThat(signature.getStatus()).isEqualTo(BillingSignatureStatus.CANCELLED);
        }
    }

    @Test
    void shouldRefuseAQuoteRevisedSinceTheRequest() {
        when(versionService.currentRevision(quote)).thenReturn(3);

        assertThatThrownBy(() -> service.confirmSignature("t", request)).isInstanceOf(ConflictException.class)
                .hasMessageContaining("nouvelle demande");
        assertThat(quote.getStatus()).isEqualTo(BillingDocumentStatus.SENT);
    }
}
