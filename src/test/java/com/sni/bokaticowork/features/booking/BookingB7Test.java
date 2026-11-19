package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.features.booking.dto.response.BookingRepairResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingChannel;
import com.sni.bokaticowork.features.booking.enums.BookingLineType;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.mapper.decorator.BookingMapperDecorator;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingLine;
import com.sni.bokaticowork.features.booking.repository.BookingLineRepository;
import com.sni.bokaticowork.features.booking.repository.BookingParticipantRepository;
import com.sni.bokaticowork.features.booking.service.implementation.BookingRepairServiceImpl;
import com.sni.bokaticowork.features.booking.service.implementation.BookingServiceImpl;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingHoldService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B7 : listes, recherche, reponse complete, tri, reparation. */
class BookingB7Test {

    @Test
    void theListSortIsHonouredAndAnUnknownFieldIsRefused() throws Exception {
        Class<?> type = Class.forName("com.sni.bokaticowork.features.booking.service.implementation.BookingListSort");
        Method of = type.getDeclaredMethod("of", Sort.class);
        of.setAccessible(true);
        Method key = type.getDeclaredMethod("key");
        Method direction = type.getDeclaredMethod("direction");
        key.setAccessible(true);
        direction.setAccessible(true);

        Object none = of.invoke(null, Sort.unsorted());
        assertThat(key.invoke(none)).isEqualTo("RELEVANCE");
        Object started = of.invoke(null, Sort.by(Sort.Direction.ASC, "startedAt"));
        assertThat(key.invoke(started)).isEqualTo("STARTED_AT");
        assertThat(direction.invoke(started)).isEqualTo("ASC");
        assertThat(key.invoke(of.invoke(null, Sort.by(Sort.Direction.DESC, "totalAmount")))).isEqualTo("TOTAL_AMOUNT");
        assertThatThrownBy(() -> of.invoke(null, Sort.by("password"))).hasCauseInstanceOf(BadRequestException.class)
                .cause().hasMessageContaining("Tri impossible sur « password »");
    }

    @Test
    @SuppressWarnings("unchecked")
    void theOwnerLookupAcceptsOnlyKnownTypes() throws Exception {
        Method types = Class.forName("com.sni.bokaticowork.features.booking.service.support.BookingOwnerLookup")
                .getDeclaredMethod("types", String.class);
        types.setAccessible(true);
        assertThat((Set<Object>) types.invoke(null, (Object) null)).containsExactly("MEMBER", "CUSTOMER", "BUSINESS_ENTITY");
        assertThat((Set<Object>) types.invoke(null, " member, business_entity ")).containsExactly("MEMBER", "BUSINESS_ENTITY");
        assertThatThrownBy(() -> types.invoke(null, "USER")).hasCauseInstanceOf(BadRequestException.class);
    }

    @Test
    void aReasonCodeIsCheckedAgainstTheListAndTheFreeTextStillWorks() throws Exception {
        BookingServiceImpl service = mock(BookingServiceImpl.class, CALLS_REAL_METHODS);
        ReferenceLists lists = mock(ReferenceLists.class);
        when(lists.describe(BookingServiceImpl.CANCEL_REASON_LIST, "client_request", "a rappelé"))
                .thenReturn("Demande du client : a rappelé");
        ReflectionTestUtils.setField(service, "referenceLists", lists);
        Method reasonOf = BookingServiceImpl.class.getDeclaredMethod("reasonOf", String.class, String.class, String.class, String.class, String.class);
        Method codeOf = BookingServiceImpl.class.getDeclaredMethod("reasonCodeOf", String.class);
        reasonOf.setAccessible(true);
        codeOf.setAccessible(true);

        assertThat(reasonOf.invoke(service, BookingServiceImpl.CANCEL_REASON_LIST, "client_request", "a rappelé", null, "Annulée"))
                .isEqualTo("Demande du client : a rappelé");
        assertThat(codeOf.invoke(service, "client_request")).isEqualTo("CLIENT_REQUEST");
        assertThat(reasonOf.invoke(service, BookingServiceImpl.CANCEL_REASON_LIST, null, null, " ancien texte ", "Annulée")).isEqualTo("ancien texte");
        assertThat(reasonOf.invoke(service, BookingServiceImpl.CANCEL_REASON_LIST, null, null, null, "Annulée")).isEqualTo("Annulée");
        assertThat(codeOf.invoke(service, (Object) null)).isNull();
    }

    @Test
    void aListRowCarriesTheFullBookingWithoutQrNorInvoiceAndTotalsTheExtras() {
        BookingLineRepository lines = mock(BookingLineRepository.class);
        BookingParticipantRepository participants = mock(BookingParticipantRepository.class);
        QrCodeRenderer qr = mock(QrCodeRenderer.class);
        BookingMapperDecorator mapper = mock(BookingMapperDecorator.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(mapper, "lineRepository", lines);
        ReflectionTestUtils.setField(mapper, "participantRepository", participants);
        ReflectionTestUtils.setField(mapper, "qrCodeRenderer", qr);

        Resource resource = Resource.builder().code("SAL-1").name("Salle").capacity(8)
                .resourceType(ResourceType.builder().code("RTY-00003").build()).build();
        Booking booking = Booking.builder().id(7L).bookingNumber("BKG-7").resource(resource).status(BookingStatus.CONFIRMED)
                .paymentMode(BookingPaymentMode.DIRECT).quantity(1).totalAmount(new BigDecimal("20000")).checkInToken("ABCD-EFGH")
                .createdBy("agent@exemple.cg").channel(BookingChannel.PHONE).cancellationReasonCode(null)
                .checkedInAt(Instant.parse("2026-11-19T08:00:00Z")).build();
        when(lines.findByBookingIdIn(List.of(7L))).thenReturn(List.of(
                line(booking, BookingLineType.RESOURCE, "20000"), line(booking, BookingLineType.SERVICE, "5000"),
                line(booking, BookingLineType.DEPOSIT, "10000"), line(booking, BookingLineType.DISCOUNT, "-2000")));
        when(participants.findByBookingIdIn(List.of(7L))).thenReturn(List.of());

        BookingResponse row = mapper.toListResponses(List.of(booking)).getFirst();

        assertThat(row.checkInQrValue()).isNull();
        assertThat(row.invoiceNumber()).isNull();
        assertThat(row.extrasAmount()).isEqualByComparingTo("15000");
        assertThat(row.grandTotal()).isEqualByComparingTo("35000");
        assertThat(row.createdBy()).isEqualTo("agent@exemple.cg");
        assertThat(row.channel()).isEqualTo(BookingChannel.PHONE);
        assertThat(row.checkedInAt()).isEqualTo(Instant.parse("2026-11-19T08:00:00Z"));
        assertThat(row.lines()).hasSize(4);
        verify(qr, never()).toDataUri(anyString());
        verify(lines, never()).findByBookingId(any());
    }

    @Test
    void theRepairReplaysTheAutomaticTasksAndCountsWhatItFixed() {
        BookingService bookings = mock(BookingService.class);
        BookingHoldService holds = mock(BookingHoldService.class);
        when(holds.expireDue(100)).thenReturn(2);
        when(bookings.expireUnpaid(100)).thenReturn(1);
        when(bookings.startCheckedInBookings(100)).thenReturn(0);
        when(bookings.overdueNoShowCandidates(100)).thenReturn(List.of("BKG-1", "BKG-2"));
        when(bookings.overdueCompletionCandidates(100)).thenReturn(List.of("BKG-3"));
        when(bookings.markOverdueNoShow("BKG-1")).thenReturn(true);
        when(bookings.markOverdueNoShow("BKG-2")).thenThrow(new IllegalStateException("verrou"));
        when(bookings.markOverdueCompleted("BKG-3")).thenReturn(true);

        BookingRepairResponse result = new BookingRepairServiceImpl(bookings, holds).repair(100);

        assertThat(result.inspected()).isEqualTo(6);
        assertThat(result.repaired()).isEqualTo(5);
        assertThat(result.failed()).isEqualTo(1);
    }

    private static BookingLine line(Booking booking, BookingLineType type, String amount) {
        return BookingLine.builder().booking(booking).lineType(type).amount(new BigDecimal(amount)).quantity(BigDecimal.ONE)
                .unitPrice(new BigDecimal(amount)).currency("XAF").build();
    }
}
