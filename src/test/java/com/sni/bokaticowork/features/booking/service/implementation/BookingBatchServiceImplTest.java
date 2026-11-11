package com.sni.bokaticowork.features.booking.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingBatchRequest;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.mapper.interfaces.BookingMapper;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingBatch;
import com.sni.bokaticowork.features.booking.repository.BookingBatchRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.support.BookingBillableBridge;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Reservation multi-ressources : chaque couple ligne et membre devient une reservation, le lot
 * passe en entier ou pas du tout, et chaque membre recoit une seule facture.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingBatchServiceImplTest {

    private static final LocalDateTime START = LocalDateTime.of(2026, 11, 20, 9, 0);

    @Mock private BookingBatchRepository batchRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private BookingService bookingService;
    @Mock private BookingBillableBridge billableBridge;
    @Mock private BookingMapper bookingMapper;
    @Mock private SequenceGeneratorFacade sequenceGenerator;
    @Mock private BillableItemRepository billableItemRepository;
    @Mock private BillingDocumentRepository billingDocumentRepository;
    @Mock private BillingDocumentService billingDocumentService;

    @InjectMocks private BookingBatchServiceImpl service;

    /** Les reservations « creees », par numero, et les elements facturables qu'elles auraient produits. */
    private final Map<String, Booking> bookings = new HashMap<>();
    private final List<BillableItem> deferred = new ArrayList<>();
    private final AtomicInteger seq = new AtomicInteger();

    @BeforeEach
    void setUp() {
        when(sequenceGenerator.next("booking_batch")).thenReturn("BKL-202611-000001");
        when(batchRepository.save(any(BookingBatch.class))).thenAnswer(call -> call.getArgument(0));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(call -> call.getArgument(0));
        when(bookingRepository.findAllByBatchNumber(anyString())).thenReturn(List.of());
        when(billableBridge.endDeferredInvoicing()).thenAnswer(call -> List.copyOf(deferred));
        when(bookingRepository.findByBookingNumber(anyString())).thenAnswer(call -> Optional.ofNullable(bookings.get(call.<String>getArgument(0))));
        when(bookingService.create(any(CreateBookingRequest.class))).thenAnswer(call -> {
            CreateBookingRequest request = call.getArgument(0);
            String number = "BKG-" + seq.incrementAndGet();
            Resource resource = new Resource();
            resource.setName("Res " + request.resourceCode());
            Booking booking = Booking.builder().bookingNumber(number).ownerType(SubscriberType.MEMBER)
                    .ownerCode(request.memberId()).resource(resource).build();
            bookings.put(number, booking);
            deferred.add(BillableItem.builder().billableNumber("BI-" + number).sourceId(number)
                    .subscriberType(SubscriberType.MEMBER).subscriberCode(request.memberId())
                    .subscriberName("Membre " + request.memberId()).currency("XAF").amount(new BigDecimal("10000")).build());
            BookingResponse response = mock(BookingResponse.class);
            when(response.bookingNumber()).thenReturn(number);
            return response;
        });
    }

    private static CreateBookingBatchRequest.Line line(String resource, List<String> members) {
        return new CreateBookingBatchRequest.Line(resource, START, START.plusHours(2), 1, null, members, null, null);
    }

    private static CreateBookingBatchRequest request(List<String> members, BookingPaymentMode mode, CreateBookingBatchRequest.Line... lines) {
        return new CreateBookingBatchRequest(members, List.of(lines), mode, null, null, null, null, null);
    }

    @Test
    @DisplayName("Deux membres sur deux ressources : quatre reservations, une facture par membre")
    void oneInvoicePerMember() {
        service.create(request(List.of("MBR-1", "MBR-2"), BookingPaymentMode.DIRECT,
                line("SALLE-A", null), line("BUREAU-B", null)));

        verify(bookingService, times(4)).create(any(CreateBookingRequest.class));
        verify(billableBridge).beginDeferredInvoicing();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BillableItem>> groups = ArgumentCaptor.forClass(List.class);
        verify(billableBridge, times(2)).invoiceGroup(groups.capture(), anyString(), anyString());
        assertThat(groups.getAllValues()).allSatisfy(group -> {
            assertThat(group).hasSize(2);
            assertThat(group).extracting(BillableItem::getSubscriberCode).containsOnly(group.get(0).getSubscriberCode());
        });
        assertThat(bookings.values()).allSatisfy(booking -> assertThat(booking.getBatchNumber()).isEqualTo("BKL-202611-000001"));

        ArgumentCaptor<BookingBatch> batch = ArgumentCaptor.forClass(BookingBatch.class);
        verify(batchRepository).save(batch.capture());
        assertThat(batch.getValue().getBookingCount()).isEqualTo(4);
        assertThat(batch.getValue().getMemberCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Les membres d'une ligne remplacent ceux du lot pour cette ligne")
    void lineMembersOverrideDefaults() {
        List<BookingBatchServiceImpl.Planned> planned = service.plan(request(List.of("MBR-1", "MBR-2"), BookingPaymentMode.DIRECT,
                line("SALLE-A", null), line("BUREAU-B", List.of("MBR-3"))), "BKL-1");

        assertThat(planned).extracting(p -> p.request().resourceCode() + "/" + p.request().memberId())
                .containsExactly("SALLE-A/MBR-1", "SALLE-A/MBR-2", "BUREAU-B/MBR-3");
    }

    @Test
    @DisplayName("Une ligne sans membre refuse le lot avant toute creation")
    void lineWithoutMemberIsRefused() {
        assertThatThrownBy(() -> service.create(request(null, BookingPaymentMode.DIRECT, line("SALLE-A", null))))
                .isInstanceOf(ValidationException.class);
        verify(bookingService, never()).create(any());
    }

    @Test
    @DisplayName("Une reservation impossible refuse tout le lot, la liste des erreurs la nomme, rien n'est facture")
    void allOrNothing() {
        when(bookingService.create(org.mockito.ArgumentMatchers.argThat(r -> r != null && "MBR-2".equals(r.memberId()))))
                .thenThrow(new ConflictException("booking", "creneau deja pris"));

        assertThatThrownBy(() -> service.create(request(List.of("MBR-1", "MBR-2"), BookingPaymentMode.DIRECT, line("SALLE-A", null))))
                .isInstanceOfSatisfying(ValidationException.class, ex -> assertThat(ex.getErrors())
                        .singleElement().asString().contains("Ligne 1").contains("SALLE-A").contains("MBR-2"));

        verify(billableBridge).endDeferredInvoicing();
        verify(billableBridge, never()).invoiceGroup(any(), anyString(), anyString());
        verify(batchRepository, never()).save(any());
    }

    @Test
    @DisplayName("Le portefeuille est accepte : chaque reservation bloque sa part, une facture pour le membre")
    void walletIsAccepted() {
        service.create(request(List.of("MBR-1"), BookingPaymentMode.WALLET, line("SALLE-A", null), line("BUREAU-B", null)));

        verify(bookingService, times(2)).create(org.mockito.ArgumentMatchers.argThat(r -> r != null && r.paymentMode() == BookingPaymentMode.WALLET));
        verify(billableBridge).invoiceGroup(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("Un lot trop grand est refuse avant toute creation")
    void tooLargeIsRefused() {
        List<String> members = java.util.stream.IntStream.rangeClosed(1, 101).mapToObj(i -> "MBR-" + i).toList();
        assertThatThrownBy(() -> service.create(request(members, BookingPaymentMode.DIRECT, line("SALLE-A", null), line("BUREAU-B", null))))
                .isInstanceOf(BadRequestException.class);
        verify(bookingService, never()).create(any());
    }

    @Test
    @DisplayName("Rejouer la cle d'idempotence rend le lot existant sans rien recreer")
    void replayReturnsTheExistingBatch() {
        BookingBatch existing = BookingBatch.builder().batchNumber("BKL-OLD").paymentMode(BookingPaymentMode.DIRECT)
                .bookingCount(1).memberCount(1).build();
        when(batchRepository.findByIdempotencyKey("cle-1")).thenReturn(Optional.of(existing));

        var response = service.create(new CreateBookingBatchRequest(List.of("MBR-1"), List.of(line("SALLE-A", null)),
                BookingPaymentMode.DIRECT, null, null, null, null, "cle-1"));

        assertThat(response.batchNumber()).isEqualTo("BKL-OLD");
        verify(bookingService, never()).create(any());
    }

    @Test
    @DisplayName("Une reservation soumise a approbation entre quand meme dans la facture commune")
    void pendingApprovalBookingIsInvoicedWithTheBatch() {
        when(billableBridge.ensureBillableItem(any(Booking.class))).thenReturn("BI-APPROVAL");
        when(bookingService.create(org.mockito.ArgumentMatchers.argThat(r -> r != null && "SALLE-VIP".equals(r.resourceCode()))))
                .thenAnswer(call -> {
                    Booking booking = Booking.builder().bookingNumber("BKG-VIP").ownerType(SubscriberType.MEMBER).ownerCode("MBR-1")
                            .status(com.sni.bokaticowork.features.booking.enums.BookingStatus.PENDING_APPROVAL)
                            .resource(new Resource()).build();
                    bookings.put("BKG-VIP", booking);
                    BookingResponse response = mock(BookingResponse.class);
                    when(response.bookingNumber()).thenReturn("BKG-VIP");
                    return response;
                });

        service.create(request(List.of("MBR-1"), BookingPaymentMode.DIRECT, line("SALLE-VIP", null), line("BUREAU-B", null)));

        verify(billableBridge).ensureBillableItem(bookings.get("BKG-VIP"));
        assertThat(bookings.get("BKG-VIP").getBillableNumber()).isEqualTo("BI-APPROVAL");
    }
}
