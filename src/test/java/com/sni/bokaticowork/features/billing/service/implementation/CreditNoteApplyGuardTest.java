package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Un avoir ne s'impute sur sa facture qu'une fois valide, et une seule fois.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CreditNoteApplyGuardTest {

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    @org.junit.jupiter.api.BeforeEach
    void injectEntityManager() {
        // Champ @PersistenceContext · l'injection par constructeur de Mockito ne le renseigne pas.
        org.springframework.test.util.ReflectionTestUtils.setField(service, "entityManager", entityManager);
    }

    @Test
    void shouldRefuseToApplyAnUnsealedCreditNote() {
        creditNoteExists(creditNote("CRN-010", false, BillingDocumentStatus.DRAFT));

        assertThatThrownBy(() -> service.applyCreditNote("CRN-010"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("doit etre valide");
        verify(documentRepository, never()).lockByDocumentNumber("INV-010");
        verify(documentRepository, never()).save(any(BillingDocument.class));
    }

    @Test
    void shouldRefuseToApplyACreditNoteTwice() {
        creditNoteExists(creditNote("CRN-011", true, BillingDocumentStatus.APPLIED));

        assertThatThrownBy(() -> service.applyCreditNote("CRN-011"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("déjà été consommé");
        verify(documentRepository, never()).lockByDocumentNumber("INV-010");
        verify(documentRepository, never()).save(any(BillingDocument.class));
    }

    @Test
    void shouldReadTheCreditNoteUnderLock() {
        creditNoteExists(creditNote("CRN-012", true, BillingDocumentStatus.APPLIED));

        assertThatThrownBy(() -> service.applyCreditNote("CRN-012")).isInstanceOf(ConflictException.class);
        verify(documentRepository).lockByDocumentNumber("CRN-012");
    }

    private void creditNoteExists(BillingDocument document) {
        when(documentRepository.findByDocumentNumber(document.getDocumentNumber())).thenReturn(Optional.of(document));
        when(documentRepository.lockByDocumentNumber(document.getDocumentNumber())).thenReturn(Optional.of(document));
    }

    private BillingDocument creditNote(String number, boolean locked, BillingDocumentStatus status) {
        return BillingDocument.builder()
                .documentNumber(number)
                .documentType(BillingDocumentType.CREDIT_NOTE)
                .sourceType("INVOICE")
                .sourceCode("INV-010")
                .status(status)
                .locked(locked)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .currency("XAF")
                .totalAmount(new BigDecimal("50000"))
                .build();
    }
}
