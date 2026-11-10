package com.sni.bokaticowork.features.booking.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Pendant un lot, l'element facturable d'une reservation est mis de cote au lieu d'etre facture seul. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingBillableBridgeDeferredTest {

    @Mock private BillableItemRepository billableItemRepository;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private BookingLineRepository lineRepository;
    @Mock private BillableItemInvoiceSupport billableItemInvoiceSupport;
    @Mock private BillingEmailService billingEmailService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private BookingBillableBridge bridge;

    @AfterEach
    void cleanUp() {
        bridge.endDeferredInvoicing();
    }

    private Booking booking() {
        Resource resource = new Resource();
        resource.setName("Salle A");
        return Booking.builder().id(1L).bookingNumber("BKG-1").resource(resource)
                .ownerType(SubscriberType.MEMBER).ownerCode("MBR-1").paymentMode(BookingPaymentMode.DIRECT)
                .totalAmount(new BigDecimal("15000")).currency("XAF")
                .startedAt(LocalDateTime.of(2026, 11, 20, 9, 0)).endedAt(LocalDateTime.of(2026, 11, 20, 11, 0))
                .build();
    }

    @Test
    @DisplayName("En lot : l'element est cree et mis de cote, aucune facture seule")
    void deferredItemIsNotInvoiced() {
        when(sequenceGenerator.next("billable_item")).thenReturn("BI-1");
        when(billableItemRepository.save(any(BillableItem.class))).thenAnswer(call -> call.getArgument(0));

        bridge.beginDeferredInvoicing();
        String number = bridge.ensureBillableItem(booking());
        List<BillableItem> items = bridge.endDeferredInvoicing();

        assertThat(number).isEqualTo("BI-1");
        assertThat(items).extracting(BillableItem::getBillableNumber).containsExactly("BI-1");
        verify(billableItemInvoiceSupport, never()).ensureInvoiced(any(BillableItem.class), anyString(), anyString());
    }

    @Test
    @DisplayName("Hors lot : la facture de la reservation est emise comme avant")
    void outsideABatchTheBookingIsInvoiced() {
        when(sequenceGenerator.next("billable_item")).thenReturn("BI-2");
        when(billableItemRepository.save(any(BillableItem.class))).thenAnswer(call -> call.getArgument(0));

        bridge.ensureBillableItem(booking());

        verify(billableItemInvoiceSupport).ensureInvoiced(any(BillableItem.class), anyString(), anyString());
        assertThat(bridge.endDeferredInvoicing()).isEmpty();
    }
}
