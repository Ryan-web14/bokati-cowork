package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingEventWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillingDocumentArchiveWorkflowTest {

    @Mock
    private BillingDocumentRepository documentRepository;

    @Mock
    private BillingDocumentMapper mapper;

    @Mock
    private BillingEventWriter eventWriter;

    @InjectMocks
    private BillingDocumentServiceImpl service;

    @Test
    void shouldCancelAndArchiveInvoice() {
        BillingDocument invoice = BillingDocument.builder()
                .documentNumber("INV-ARCH-001")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .customerType("MEMBER")
                .customerCode("MBR-001")
                .customerName("Jean")
                .currency("XAF")
                .totalAmount(new BigDecimal("100.0000"))
                .paidAmount(BigDecimal.ZERO)
                .balanceDue(new BigDecimal("100.0000"))
                .issueDate(LocalDate.of(2026, 4, 27))
                .build();

        when(documentRepository.findByDocumentNumber("INV-ARCH-001")).thenReturn(Optional.of(invoice));
        when(documentRepository.save(any(BillingDocument.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BillingDocument archived = service.cancelAndArchive("INV-ARCH-001", "Annulation automatique");

        assertEquals(BillingDocumentStatus.CANCELLED, archived.getStatus());
        assertEquals(BigDecimal.ZERO, archived.getBalanceDue());
        assertNotNull(archived.getCancelledAt());
        assertNotNull(archived.getArchivedAt());
        assertNull(archived.getPaidAt());
        verify(documentRepository, times(2)).save(any(BillingDocument.class));
        verify(eventWriter).write(eq(archived), eq("BILLING_DOCUMENT_CANCELLED"), any());
        verify(eventWriter).write(eq(archived), eq("BILLING_DOCUMENT_ARCHIVED"), any());
    }
}
