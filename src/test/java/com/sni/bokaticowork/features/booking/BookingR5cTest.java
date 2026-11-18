package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.model.ServiceCatalogItem;
import com.sni.bokaticowork.features.billing.repository.ServiceCatalogItemRepository;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingLine;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingExtras;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenities;
import com.sni.bokaticowork.features.ressource.model.ResourceAmenityLink;
import com.sni.bokaticowork.features.ressource.model.ResourcePolicy;
import com.sni.bokaticowork.features.ressource.repository.repo.ResourceAmenityLinkRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot R5c : supplements d'une reservation. */
class BookingR5cTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingLineRepository lines = mock(BookingLineRepository.class);
    private final BillableItemRepository items = mock(BillableItemRepository.class);
    private final SequenceGeneratorFacade sequence = mock(SequenceGeneratorFacade.class);
    private final BillableItemInvoiceSupport invoices = mock(BillableItemInvoiceSupport.class);
    private final List<BookingLine> saved = new ArrayList<>();
    private BookingExtras extras;
    private Booking booking;
    private Resource resource;

    @BeforeEach
    void setUp() {
        extras = new BookingExtras(bookings, lines, items, sequence, invoices, mock(BookingEventWriter.class));
        resource = Resource.builder().id(1L).code("SAL-1").name("Baobab").capacity(10).slotDurationMinutes(30).build();
        LocalDateTime start = LocalDateTime.of(2026, 12, 2, 10, 0);
        booking = Booking.builder().id(5L).bookingNumber("BKG-1").resource(resource).status(BookingStatus.CONFIRMED)
                .startedAt(start).endedAt(start.plusHours(2)).durationMinutes(120).totalAmount(BigDecimal.valueOf(20000))
                .currency("XAF").quantity(1).build();
        when(bookings.findByBookingNumber("BKG-1")).thenReturn(Optional.of(booking));
        when(lines.save(any())).thenAnswer(invocation -> {
            BookingLine line = invocation.getArgument(0);
            if (!saved.contains(line)) saved.add(line);
            return line;
        });
        when(lines.findByBookingId(5L)).thenAnswer(invocation -> List.copyOf(saved));
        when(items.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sequence.next(anyString())).thenReturn("BIL-1", "BIL-2", "BIL-3");
    }

    @Test
    void aServiceWaitsForTheBookingInvoiceThenGoesOnIt() {
        ServiceCatalogItemRepository catalog = mock(ServiceCatalogItemRepository.class);
        when(catalog.findByItemCode("CAFE")).thenReturn(Optional.of(ServiceCatalogItem.builder().itemCode("CAFE").name("Café")
                .unitPrice(BigDecimal.valueOf(500)).active(true).build()));
        ReflectionTestUtils.setField(extras, "catalog", catalog);

        BookingExtras.LineView line = extras.addService("BKG-1", "CAFE", 6);

        assertThat(line.amount()).isEqualByComparingTo("3000");
        assertThat(line.billableNumber()).isNull();
        verify(invoices, never()).ensureInvoiced(any(), anyString(), anyString());

        List<BillableItem> pending = extras.pendingItems(booking);
        assertThat(pending).hasSize(1);
        assertThat(pending.getFirst().getAmount()).isEqualByComparingTo("3000");
        assertThat(pending.getFirst().getSourceType()).isEqualTo(BookingExtras.ADDON_SOURCE);
        assertThat(saved.getFirst().getBillableNumber()).isEqualTo("BIL-1");
    }

    @Test
    void afterTheInvoiceAServiceIsInvoicedOnItsOwn() {
        booking.setBillableNumber("BIL-0");
        ServiceCatalogItemRepository catalog = mock(ServiceCatalogItemRepository.class);
        when(catalog.findByItemCode("IMP")).thenReturn(Optional.of(ServiceCatalogItem.builder().itemCode("IMP").name("Impression")
                .unitPrice(BigDecimal.valueOf(100)).active(true).build()));
        ReflectionTestUtils.setField(extras, "catalog", catalog);

        extras.addService("BKG-1", "IMP", 10);

        verify(invoices).ensureInvoiced(any(), anyString(), anyString());
    }

    @Test
    void anIncludedAmenityIsNotSoldAsAnOption() {
        ResourceAmenityLinkRepository links = mock(ResourceAmenityLinkRepository.class);
        ResourceAmenities screen = ResourceAmenities.builder().code("ECRAN").name("Écran").build();
        when(links.findAllByResource(resource)).thenReturn(List.of(ResourceAmenityLink.builder().amenity(screen).optional(false).build()));
        ReflectionTestUtils.setField(extras, "amenityLinks", links);

        assertThatThrownBy(() -> extras.addAmenity("BKG-1", "ECRAN", 1)).isInstanceOf(ConflictException.class);
    }

    @Test
    void theDepositIsAddedOnceAndBilledAsADeposit() {
        resource.setDepositAmount(50000);

        extras.addDepositIfAny(booking);
        extras.addDepositIfAny(booking);
        List<BillableItem> pending = extras.pendingItems(booking);

        assertThat(saved).filteredOn(line -> line.getLineType() == BookingLineType.DEPOSIT).hasSize(1);
        assertThat(pending).singleElement().satisfies(item -> {
            assertThat(item.getSourceType()).isEqualTo("DEPOSIT");
            assertThat(item.getAmount()).isEqualByComparingTo("50000");
        });
    }

    @Test
    void anOverrunBeyondTheGraceIsBilledBySlots() {
        resource.setResourcePolicy(ResourcePolicy.builder().active(true).overrunGraceMinutes(10).build());
        booking.setCheckedOutAt(booking.getEndedAt().plusMinutes(40).atZone(ZoneId.of("Africa/Brazzaville")).toInstant());

        extras.onCheckOut(booking);

        // 40 min de depassement, factures 60 min au prorata de 20 000 pour 120 min.
        ArgumentCaptor<BookingLine> line = ArgumentCaptor.forClass(BookingLine.class);
        verify(lines).save(line.capture());
        assertThat(line.getValue().getLineType()).isEqualTo(BookingLineType.OVERRUN);
        assertThat(line.getValue().getAmount()).isEqualByComparingTo("10000");
    }

    @Test
    void anOverrunWithinTheGraceIsFree() {
        resource.setResourcePolicy(ResourcePolicy.builder().active(true).overrunGraceMinutes(15).build());
        booking.setCheckedOutAt(booking.getEndedAt().plusMinutes(10).atZone(ZoneId.of("Africa/Brazzaville")).toInstant());

        extras.onCheckOut(booking);

        verify(lines, never()).save(any());
    }

    @Test
    void aNoShowFeeFollowsThePolicy() {
        resource.setResourcePolicy(ResourcePolicy.builder().active(true).noShowFee(5000).build());

        extras.onNoShow(booking);

        assertThat(saved).singleElement().satisfies(line -> {
            assertThat(line.getLineType()).isEqualTo(BookingLineType.NO_SHOW);
            assertThat(line.getAmount()).isEqualByComparingTo("5000");
        });
    }
}
