package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.communication.mailService.baseService.DefaultEmailSender;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.reference.model.ReferenceValue;
import com.sni.bokaticowork.core.reference.service.ReferenceLists;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.enums.BookingParticipantStatus;
import com.sni.bokaticowork.features.booking.enums.BookingStatus;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingParticipant;
import com.sni.bokaticowork.features.booking.repository.BookingParticipantRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingGuests;
import com.sni.bokaticowork.features.ressource.model.Resource;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B9d : invites (invitation, reponse, pointage) et details de la reservation. */
class BookingB9dTest {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingParticipantRepository participants = mock(BookingParticipantRepository.class);
    private final BookingEventWriter events = mock(BookingEventWriter.class);
    private final DefaultEmailSender email = mock(DefaultEmailSender.class);
    private final ReferenceLists lists = mock(ReferenceLists.class);

    private BookingGuests guests() {
        BookingGuests guests = new BookingGuests(bookings, participants, events);
        ReflectionTestUtils.setField(guests, "email", email);
        ReflectionTestUtils.setField(guests, "lists", lists);
        when(email.sendEmail(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(true));
        when(bookings.save(any())).thenAnswer(call -> call.getArgument(0));
        return guests;
    }

    private static Booking booking(BookingStatus status) {
        return Booking.builder().id(4L).bookingNumber("BKG-4").status(status).contactName("Awa")
                .resource(Resource.builder().code("SAL-1").name("Salle Kouilou").build())
                .startedAt(LocalDateTime.now().plusDays(2)).endedAt(LocalDateTime.now().plusDays(2).plusHours(2)).build();
    }

    @Test
    void anInvitedGuestReceivesPersonalLinksAndAnswers() {
        BookingGuests guests = guests();
        Booking booking = booking(BookingStatus.CONFIRMED);
        BookingParticipant guest = BookingParticipant.builder().id(9L).booking(booking).name("Paul").email("paul@exemple.cg")
                .status(BookingParticipantStatus.INVITED).build();
        when(bookings.findByBookingNumber("BKG-4")).thenReturn(Optional.of(booking));
        when(participants.findByIdAndBookingId(9L, 4L)).thenReturn(Optional.of(guest));

        assertThat(guests.invite("BKG-4", 9L)).isEqualTo(1);
        assertThat(guest.getInviteToken()).isNotBlank();
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(email).sendEmail(eq("paul@exemple.cg"), anyString(), body.capture());
        assertThat(body.getValue()).contains("/public/bookings/invitations/" + guest.getInviteToken() + "?answer=ACCEPT", "answer=DECLINE");

        when(participants.findByInviteToken(guest.getInviteToken())).thenReturn(Optional.of(guest));
        guests.respond(guest.getInviteToken(), false);
        assertThat(guest.getStatus()).isEqualTo(BookingParticipantStatus.DECLINED);
        assertThat(guest.getRespondedAt()).isNotNull();
        verify(events).write(eq(booking), eq(BookingEventType.BOOKING_PARTICIPANT_ANSWERED), anyString(), anyString(), any());
    }

    @Test
    void aGuestIsCheckedInOnceAndOnlyOnALiveBooking() {
        BookingGuests guests = guests();
        Booking booking = booking(BookingStatus.IN_PROGRESS);
        BookingParticipant guest = BookingParticipant.builder().id(9L).booking(booking).name("Paul").status(BookingParticipantStatus.CONFIRMED).build();
        when(bookings.findByBookingNumber("BKG-4")).thenReturn(Optional.of(booking));
        when(participants.findByIdAndBookingId(9L, 4L)).thenReturn(Optional.of(guest));

        assertThat(guests.checkIn("BKG-4", 9L).getCheckedInAt()).isNotNull();

        booking.setStatus(BookingStatus.CANCELLED);
        assertThatThrownBy(() -> guests.checkIn("BKG-4", 9L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void detailsUseTheSoftListsAndKeepUntouchedFields() {
        BookingGuests guests = guests();
        Booking booking = booking(BookingStatus.CONFIRMED);
        booking.setSpecialRequests("Vidéoprojecteur");
        when(bookings.findByBookingNumber("BKG-4")).thenReturn(Optional.of(booking));
        when(lists.require(BookingGuests.ROOM_LAYOUT_LIST, "u_shape")).thenReturn(ReferenceValue.builder().code("U_SHAPE").build());
        when(lists.require(BookingGuests.TAG_LIST, "vip")).thenReturn(ReferenceValue.builder().code("VIP").build());

        Booking updated = guests.updateDetails("BKG-4", new BookingGuests.Details("u_shape", null, "Client exigeant", List.of("vip", " ")));

        assertThat(updated.getRoomLayout()).isEqualTo("U_SHAPE");
        assertThat(updated.getSpecialRequests()).isEqualTo("Vidéoprojecteur");
        assertThat(updated.getInternalNotes()).isEqualTo("Client exigeant");
        assertThat(BookingGuests.tags(updated)).containsExactly("VIP");
    }
}
