package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.dto.request.CreateBookingRequest;
import com.sni.bokaticowork.features.booking.dto.response.BookingAvailabilityResponse;
import com.sni.bokaticowork.features.booking.dto.response.BookingResponse;
import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.enums.BookingWaitlistStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingNotificationOutbox;
import com.sni.bokaticowork.features.booking.model.BookingWaitlistEntry;
import com.sni.bokaticowork.features.booking.repository.BookingNotificationOutboxRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.repository.BookingWaitlistEntryRepository;
import com.sni.bokaticowork.features.booking.service.implementation.BookingNotificationServiceImpl;
import com.sni.bokaticowork.features.booking.service.implementation.BookingWaitlistServiceImpl;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingAvailabilityService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingService;
import com.sni.bokaticowork.features.booking.service.interfaces.BookingSuggestionService;
import com.sni.bokaticowork.features.booking.service.support.BookingIdentityResolver;
import com.sni.bokaticowork.features.booking.service.support.BookingMemberPreferences;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.service.interfaces.ResourceService;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B6 : liste d'attente refaite et rappels automatiques. */
class BookingB6Test {

    private final BookingWaitlistEntryRepository waitlist = mock(BookingWaitlistEntryRepository.class);
    private final BookingAvailabilityService availability = mock(BookingAvailabilityService.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final DefaultEmailSender email = mock(DefaultEmailSender.class);

    @SuppressWarnings("unchecked")
    private BookingWaitlistServiceImpl service() {
        ObjectProvider<BookingService> provider = mock(ObjectProvider.class);
        when(provider.getObject()).thenReturn(bookingService);
        BookingWaitlistServiceImpl service = new BookingWaitlistServiceImpl(waitlist, mock(ResourceService.class), mock(BookingIdentityResolver.class),
                mock(BookingSuggestionService.class), availability, provider);
        ReflectionTestUtils.setField(service, "offerTtlMinutes", 30);
        ReflectionTestUtils.setField(service, "emailSender", email);
        when(email.sendEmail(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(true));
        when(waitlist.save(any())).thenAnswer(call -> call.getArgument(0));
        when(waitlist.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        return service;
    }

    private static BookingWaitlistEntry entry(long id, BookingWaitlistStatus status) {
        Resource resource = Resource.builder().id(3L).code("SAL-1").name("Salle").build();
        return BookingWaitlistEntry.builder().id(id).resource(resource).ownerType(SubscriberType.MEMBER).ownerCode("MBR-" + id)
                .contactEmail("m" + id + "@exemple.cg").contactName("Membre " + id).startedAt(LocalDateTime.now().plusDays(1))
                .endedAt(LocalDateTime.now().plusDays(1).plusHours(2)).quantity(1).paymentMode(BookingPaymentMode.DIRECT).status(status).build();
    }

    private static BookingAvailabilityResponse availability(boolean available) {
        BookingAvailabilityResponse response = mock(BookingAvailabilityResponse.class);
        when(response.available()).thenReturn(available);
        return response;
    }

    @Test
    void aFreedSlotIsOfferedToTheOldestEntryOnlyAndTheContactIsWarned() {
        BookingWaitlistServiceImpl service = service();
        BookingWaitlistEntry first = entry(1, BookingWaitlistStatus.WAITING);
        BookingWaitlistEntry second = entry(2, BookingWaitlistStatus.WAITING);
        when(waitlist.findPastWaiting(any(), anyInt())).thenReturn(List.of());
        when(waitlist.findWaitingCandidates(any(), anyInt())).thenReturn(List.of(first, second));
        when(waitlist.existsOpenOffer(eq(3L), any(), any())).thenReturn(false, true);
        BookingAvailabilityResponse free = availability(true);
        when(availability.check(any())).thenReturn(free);

        assertThat(service.promoteAvailable(10)).isEqualTo(1);

        assertThat(first.getStatus()).isEqualTo(BookingWaitlistStatus.OFFERED);
        assertThat(first.getExpiresAt()).isAfter(Instant.now().plus(29, ChronoUnit.MINUTES));
        assertThat(first.getNotifiedAt()).isNotNull();
        assertThat(second.getStatus()).isEqualTo(BookingWaitlistStatus.WAITING);
        verify(email).sendEmail(eq("m1@exemple.cg"), anyString(), anyString());
    }

    @Test
    void aFullSlotIsNotOfferedAndAPastEntryExpires() {
        BookingWaitlistServiceImpl service = service();
        BookingWaitlistEntry past = entry(4, BookingWaitlistStatus.WAITING);
        BookingWaitlistEntry waiting = entry(5, BookingWaitlistStatus.WAITING);
        when(waitlist.findPastWaiting(any(), anyInt())).thenReturn(List.of(past));
        when(waitlist.findWaitingCandidates(any(), anyInt())).thenReturn(List.of(waiting));
        BookingAvailabilityResponse full = availability(false);
        when(availability.check(any())).thenReturn(full);

        assertThat(service.promoteAvailable(10)).isZero();
        assertThat(past.getStatus()).isEqualTo(BookingWaitlistStatus.EXPIRED);
        assertThat(waiting.getStatus()).isEqualTo(BookingWaitlistStatus.WAITING);
        verify(email, never()).sendEmail(anyString(), anyString(), anyString());
    }

    @Test
    void acceptingTheOfferCreatesTheBookingOnce() {
        BookingWaitlistServiceImpl service = service();
        BookingWaitlistEntry offered = entry(6, BookingWaitlistStatus.OFFERED);
        offered.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        when(waitlist.findById(6L)).thenReturn(Optional.of(offered));
        BookingResponse created = mock(BookingResponse.class);
        when(created.bookingNumber()).thenReturn("BKG-77");
        when(bookingService.create(any())).thenReturn(created);

        assertThat(service.accept(6L).bookingNumber()).isEqualTo("BKG-77");
        ArgumentCaptor<CreateBookingRequest> request = ArgumentCaptor.forClass(CreateBookingRequest.class);
        verify(bookingService).create(request.capture());
        assertThat(request.getValue().memberId()).isEqualTo("MBR-6");
        assertThat(request.getValue().idempotencyKey()).isEqualTo("WAITLIST-6");
        assertThat(offered.getStatus()).isEqualTo(BookingWaitlistStatus.BOOKED);

        assertThatThrownBy(() -> service.accept(6L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void remindersAreQueuedTheDayBeforeAndTwoHoursBeforeAndSkippedForCancelledBookings() {
        BookingRepository bookings = mock(BookingRepository.class);
        BookingNotificationOutboxRepository outbox = mock(BookingNotificationOutboxRepository.class);
        SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
        when(sequences.next(anyString())).thenReturn("BKN-1");
        BookingMemberPreferences preferences = mock(BookingMemberPreferences.class);
        BookingNotificationServiceImpl service = new BookingNotificationServiceImpl(bookings, outbox, sequences, email, preferences);
        Resource resource = Resource.builder().code("SAL-1").name("Salle").build();
        Booking booking = Booking.builder().id(9L).bookingNumber("BKG-9").resource(resource).status(BookingStatus.CONFIRMED)
                .contactEmail("c@exemple.cg").contactName("Client").startedAt(LocalDateTime.now().plusHours(30)).build();
        when(outbox.findBookingsToRemind(any(), any(), anyInt())).thenReturn(List.of("BKG-9"));
        when(bookings.findByBookingNumber("BKG-9")).thenReturn(Optional.of(booking));

        assertThat(service.scheduleUpcoming(50)).isEqualTo(2);
        ArgumentCaptor<BookingNotificationOutbox> saved = ArgumentCaptor.forClass(BookingNotificationOutbox.class);
        verify(outbox, org.mockito.Mockito.times(2)).save(saved.capture());
        assertThat(saved.getAllValues().getFirst().getBody()).contains("Rappel", "BKG-9").doesNotContain(" at ");

        booking.setStatus(BookingStatus.CANCELLED);
        BookingNotificationOutbox due = BookingNotificationOutbox.builder().booking(booking).recipient("c@exemple.cg").subject("s").body("b")
                .status(com.sni.bokaticowork.features.booking.enums.BookingNotificationStatus.PENDING).attemptCount(0).build();
        when(outbox.findDue(any(), anyInt())).thenReturn(List.of(due));
        service.dispatchDue(10);
        assertThat(due.getStatus()).isEqualTo(com.sni.bokaticowork.features.booking.enums.BookingNotificationStatus.CANCELLED);
        verify(email, never()).sendEmail(eq("c@exemple.cg"), anyString(), anyString());
    }
}
