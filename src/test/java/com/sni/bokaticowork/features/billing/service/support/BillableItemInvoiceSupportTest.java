package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BillableItemInvoiceSupportTest {

    @Mock
    private BillingDocumentRepository billingDocumentRepository;

    @Mock
    private BillingDocumentService billingDocumentService;

    @InjectMocks
    private BillableItemInvoiceSupport support;

    @Test
    void shouldCreateAndIssueInvoiceForNewBillableItem() {
        BillableItem item = billableItem("BIL-0001", null);
        BillingDocumentResponse createdDraft = response("INV-0001", BillingDocumentStatus.DRAFT);
        BillingDocumentResponse issued = response("INV-0001", BillingDocumentStatus.ISSUED);

        when(billingDocumentRepository.findFirstBySourceAndType("BILLABLE_ITEM", "BIL-0001", BillingDocumentType.INVOICE.name()))
                .thenReturn(Optional.empty());
        when(billingDocumentService.createInvoiceFromBillableItems(org.mockito.ArgumentMatchers.any(CreateInvoiceFromBillableItemsRequest.class)))
                .thenReturn(createdDraft);
        when(billingDocumentService.issue("INV-0001")).thenReturn(issued);

        BillingDocumentResponse response = support.ensureInvoiced(item, "Facture reservation", "Reservation directe");

        ArgumentCaptor<CreateInvoiceFromBillableItemsRequest> captor = ArgumentCaptor.forClass(CreateInvoiceFromBillableItemsRequest.class);
        verify(billingDocumentService).createInvoiceFromBillableItems(captor.capture());
        assertEquals(List.of("BIL-0001"), captor.getValue().billableNumbers());
        assertEquals("Facture reservation", captor.getValue().title());
        assertEquals(BillingDocumentStatus.ISSUED, response.status());
    }

    @Test
    void shouldReuseExistingInvoiceWhenBillableItemAlreadyLinked() {
        BillableItem item = billableItem("BIL-0002", 55L);
        BillingDocument existing = BillingDocument.builder()
                .documentNumber("INV-EXISTING")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .customerName("MBR-0001")
                .currency("XAF")
                .issueDate(LocalDate.now())
                .totalAmount(BigDecimal.TEN)
                .balanceDue(BigDecimal.TEN)
                .build();
        BillingDocumentResponse existingResponse = response("INV-EXISTING", BillingDocumentStatus.ISSUED);

        when(billingDocumentRepository.findFirstBySourceAndType("BILLABLE_ITEM", "BIL-0002", BillingDocumentType.INVOICE.name()))
                .thenReturn(Optional.of(existing));
        when(billingDocumentService.get("INV-EXISTING")).thenReturn(existingResponse);

        BillingDocumentResponse response = support.ensureInvoiced(item);

        verify(billingDocumentService, never()).createInvoiceFromBillableItems(org.mockito.ArgumentMatchers.any(CreateInvoiceFromBillableItemsRequest.class));
        assertEquals("INV-EXISTING", response.documentNumber());
        assertEquals(BillingDocumentStatus.ISSUED, response.status());
    }

    @Test
    void shouldIssueDraftInvoiceWhenBillableItemAlreadyLinkedByInvoiceId() {
        BillableItem item = billableItem("BIL-0003", 77L);
        BillingDocument existing = BillingDocument.builder()
                .id(77L)
                .documentNumber("INV-DRAFT")
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.DRAFT)
                .customerType("MEMBER")
                .customerCode("MBR-0001")
                .customerName("MBR-0001")
                .currency("XAF")
                .issueDate(LocalDate.now())
                .totalAmount(BigDecimal.TEN)
                .balanceDue(BigDecimal.TEN)
                .build();
        BillingDocumentResponse draftResponse = response("INV-DRAFT", BillingDocumentStatus.DRAFT);
        BillingDocumentResponse issuedResponse = response("INV-DRAFT", BillingDocumentStatus.ISSUED);

        when(billingDocumentRepository.findById(77L)).thenReturn(Optional.of(existing));
        when(billingDocumentService.get("INV-DRAFT")).thenReturn(draftResponse);
        when(billingDocumentService.issue("INV-DRAFT")).thenReturn(issuedResponse);

        BillingDocumentResponse response = support.ensureInvoiced(item);

        verify(billingDocumentService, never()).createInvoiceFromBillableItems(org.mockito.ArgumentMatchers.any(CreateInvoiceFromBillableItemsRequest.class));
        assertEquals("INV-DRAFT", response.documentNumber());
        assertEquals(BillingDocumentStatus.ISSUED, response.status());
    }

    @Test
    void shouldUseNeutralDefaultDescriptionWhenBillableItemHasNoDescription() {
        BillableItem item = billableItem("BIL-0004", null);
        item.setDescription(null);
        BillingDocumentResponse createdDraft = response("INV-0004", BillingDocumentStatus.DRAFT);
        BillingDocumentResponse issued = response("INV-0004", BillingDocumentStatus.ISSUED);

        when(billingDocumentRepository.findFirstBySourceAndType("BILLABLE_ITEM", "BIL-0004", BillingDocumentType.INVOICE.name()))
                .thenReturn(Optional.empty());
        when(billingDocumentService.createInvoiceFromBillableItems(org.mockito.ArgumentMatchers.any(CreateInvoiceFromBillableItemsRequest.class)))
                .thenReturn(createdDraft);
        when(billingDocumentService.issue("INV-0004")).thenReturn(issued);

        support.ensureInvoiced(item);

        ArgumentCaptor<CreateInvoiceFromBillableItemsRequest> captor = ArgumentCaptor.forClass(CreateInvoiceFromBillableItemsRequest.class);
        verify(billingDocumentService).createInvoiceFromBillableItems(captor.capture());
        assertEquals("Facture booking", captor.getValue().title());
        assertEquals("Prestation relative a booking", captor.getValue().description());
    }

    private BillableItem billableItem(String billableNumber, Long invoiceId) {
        return BillableItem.builder()
                .billableNumber(billableNumber)
                .sourceType("BOOKING")
                .sourceId("BKG-001")
                .subscriberType(SubscriberType.MEMBER)
                .subscriberCode("MBR-0001")
                .description("Booking test")
                .amount(BigDecimal.TEN)
                .currency("XAF")
                .invoiceId(invoiceId)
                .build();
    }

    private BillingDocumentResponse response(String documentNumber, BillingDocumentStatus status) {
        return new BillingDocumentResponse(
                documentNumber,
                BillingDocumentType.INVOICE,
                status,
                "MEMBER",
                "MBR-0001",
                "MBR-0001",
                null,
                null,
                null,
                "BILLABLE_ITEM",
                "BIL-0001",
                "Facture",
                null,
                null,
                "XAF",
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.TEN,
                BigDecimal.ZERO,
                BigDecimal.TEN,
                LocalDate.now(),
                LocalDate.now(),
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
