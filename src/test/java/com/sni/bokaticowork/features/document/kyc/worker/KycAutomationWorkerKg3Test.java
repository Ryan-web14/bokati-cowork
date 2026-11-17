package com.sni.bokaticowork.features.document.kyc.worker;

import com.sni.bokaticowork.core.outbox.service.interfaces.OutboxService;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import com.sni.bokaticowork.features.document.documentMaster.model.Document;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentType;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRepository;
import com.sni.bokaticowork.features.document.kyc.KycCaseStatus;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.config.KycAutomationProperties;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot KG3 : rappels d'echeance par seuil franchi et memorise, expiration unique apres grace. */
class KycAutomationWorkerKg3Test {

    private final KycDocumentRepository pieces = mock(KycDocumentRepository.class);
    private final KycCaseRepository cases = mock(KycCaseRepository.class);
    private final DocumentRepository documents = mock(DocumentRepository.class);
    private final OutboxService outbox = mock(OutboxService.class);
    private final KycAutomationProperties properties = new KycAutomationProperties();
    private KycAutomationWorker worker;

    private final LocalDate today = LocalDate.of(2026, 11, 17);
    private KycCase kycCase;
    private Document file;
    private KycDocument piece;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager tx = mock(PlatformTransactionManager.class);
        when(tx.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        worker = new KycAutomationWorker(pieces, cases, documents, mock(MemberRepository.class),
                mock(CustomerRepository.class), outbox, properties, tx);
        kycCase = KycCase.builder().id(1L).code("KYC-1").status(KycCaseStatus.APPROVED)
                .ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L).build();
        file = Document.builder().id(5L).code("DOC-5").status(DocumentStatus.APPROVED)
                .documentType(DocumentType.builder().code("CNI").name("Carte d'identité").build()).build();
        piece = KycDocument.builder().id(10L).kycCase(kycCase).document(file).documentType("CNI")
                .status(KycDocumentVerificationStatus.VERIFIED).ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L).build();
        when(pieces.findAllByStatusIn(any())).thenReturn(List.of(piece));
        when(pieces.findById(10L)).thenReturn(Optional.of(piece));
    }

    @Test
    void aCrossedThresholdIsSentOnceEvenIfTheExactDayWasMissed() {
        piece.setExpiryDate(today.plusDays(25)); // seuil 30 franchi il y a 5 jours

        assertThat(worker.runExpiry(today).reminders()).isEqualTo(1);
        assertThat(file.getExpiryReminderSentDays()).isEqualTo(30);
        verify(outbox).publish(eq("KYC_DOCUMENT_EXPIRY_REMINDER"), eq("KYC_DOCUMENT"), anyString(), any());

        assertThat(worker.runExpiry(today.plusDays(1)).reminders()).isZero();
        assertThat(worker.runExpiry(today.plusDays(19)).reminders()).isEqualTo(1); // 6 jours : seuil 7
        assertThat(file.getExpiryReminderSentDays()).isEqualTo(7);
    }

    @Test
    void replacedOrRejectedPiecesGetNoReminder() {
        piece.setExpiryDate(today.plusDays(5));
        file.setStatus(DocumentStatus.SUPERSEDED);
        assertThat(worker.runExpiry(today).reminders()).isZero();

        file.setStatus(DocumentStatus.APPROVED);
        kycCase.setStatus(KycCaseStatus.REJECTED);
        assertThat(worker.runExpiry(today).reminders()).isZero();
        verify(outbox, never()).publish(anyString(), anyString(), anyString(), any());
    }

    @Test
    void anApprovedCaseGoesToRenewalOnlyAfterTheGracePeriod() {
        properties.getExpiry().setGracePeriodDays(15);
        piece.setExpiryDate(today.minusDays(10));
        assertThat(worker.runExpiry(today).expired()).isZero();
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.APPROVED);

        piece.setExpiryDate(today.minusDays(16));
        assertThat(worker.runExpiry(today).expired()).isEqualTo(1);
        assertThat(piece.getStatus()).isEqualTo(KycDocumentVerificationStatus.EXPIRED);
        assertThat(file.getStatus()).isEqualTo(DocumentStatus.EXPIRED);
        assertThat(kycCase.getStatus()).isEqualTo(KycCaseStatus.RENEWAL_REQUIRED);
        verify(outbox).publish(eq("KYC_RENEWAL_REQUIRED"), eq("KYC_CASE"), eq("KYC-1"), any());
    }

    @Test
    void oneFailingPieceDoesNotStopTheRun() {
        KycDocument other = KycDocument.builder().id(11L).build();
        when(pieces.findAllByStatusIn(any())).thenReturn(List.of(other, piece));
        when(pieces.findById(11L)).thenThrow(new IllegalStateException("panne"));
        piece.setExpiryDate(today.plusDays(3));

        KycAutomationWorker.ExpiryRun run = worker.runExpiry(today);

        assertThat(run.failures()).isEqualTo(1);
        assertThat(run.reminders()).isEqualTo(1);
    }
}
