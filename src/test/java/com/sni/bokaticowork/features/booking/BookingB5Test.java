package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.features.booking.dto.request.BookingCallerRequest;
import com.sni.bokaticowork.features.booking.enums.BookingEventType;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingEventWriter;
import com.sni.bokaticowork.features.booking.service.support.BookingPhoneIntake;
import com.sni.bokaticowork.features.crm.dto.CrmDtos;
import com.sni.bokaticowork.features.crm.enums.LeadActivityType;
import com.sni.bokaticowork.features.crm.enums.LeadSource;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.repository.LeadRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import com.sni.bokaticowork.features.ressource.model.Resource;
import com.sni.bokaticowork.features.ressource.model.ResourceType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot B5 : rendez-vous pris au telephone, prospect du CRM relie puis converti. */
class BookingB5Test {

    private final BookingRepository bookings = mock(BookingRepository.class);
    private final BookingEventWriter events = mock(BookingEventWriter.class);
    private final CrmService crm = mock(CrmService.class);
    private final LeadRepository leads = mock(LeadRepository.class);

    @SuppressWarnings("unchecked")
    private BookingPhoneIntake intake() {
        ObjectProvider<CrmService> crmProvider = mock(ObjectProvider.class);
        when(crmProvider.getIfAvailable()).thenReturn(crm);
        ObjectProvider<LeadRepository> leadProvider = mock(ObjectProvider.class);
        when(leadProvider.getIfAvailable()).thenReturn(leads);
        return new BookingPhoneIntake(bookings, events, crmProvider, leadProvider);
    }

    private static Booking booking() {
        Resource resource = Resource.builder().code("SAL-1").name("Salle de réunion")
                .resourceType(ResourceType.builder().code("RTY-00003").name("Salle de réunion").build()).build();
        return Booking.builder().bookingNumber("BKG-5").resource(resource).ownerType(SubscriberType.CUSTOMER).ownerCode("GUEST-1")
                .contactName("Paul Okemba").contactPhone("+242060000000").startedAt(LocalDateTime.of(2026, 11, 20, 9, 0))
                .totalAmount(new BigDecimal("30000")).build();
    }

    @Test
    void anUnknownCallerBecomesAPhoneLeadWithTheCallNote() {
        Booking booking = booking();
        CrmDtos.LeadResponse created = mock(CrmDtos.LeadResponse.class);
        when(created.leadNumber()).thenReturn("LEAD-1");
        when(crm.create(any())).thenReturn(created);
        Lead lead = Lead.builder().id(11L).leadNumber("LEAD-1").stage(LeadStage.NEW).build();
        when(leads.findByLeadNumber("LEAD-1")).thenReturn(Optional.of(lead));

        intake().onBooked(booking, new BookingCallerRequest("Paul Okemba", null, null, "Okemba SARL", "Veut un vidéoprojecteur", null, null), false);

        ArgumentCaptor<CrmDtos.CreateLeadRequest> request = ArgumentCaptor.forClass(CrmDtos.CreateLeadRequest.class);
        verify(crm).create(request.capture());
        assertThat(request.getValue().source()).isEqualTo(LeadSource.PHONE);
        assertThat(request.getValue().phone()).isEqualTo("+242060000000");
        ArgumentCaptor<CrmDtos.AddLeadActivityRequest> activity = ArgumentCaptor.forClass(CrmDtos.AddLeadActivityRequest.class);
        verify(crm).addActivity(eq(11L), activity.capture());
        assertThat(activity.getValue().activityType()).isEqualTo(LeadActivityType.CALL);
        assertThat(activity.getValue().notes()).isEqualTo("Veut un vidéoprojecteur");
        assertThat(booking.getLeadNumber()).isEqualTo("LEAD-1");
        assertThat(booking.getCallNote()).isEqualTo("Veut un vidéoprojecteur");
        verify(events).write(eq(booking), eq(BookingEventType.BOOKING_PHONE_CALL), anyString(), anyString(), any());
    }

    @Test
    void aKnownClientKeepsTheCallNoteWithoutANewLead() {
        Booking booking = booking();
        intake().onBooked(booking, new BookingCallerRequest(null, null, null, null, "Rappeler la veille", null, null), true);
        verify(crm, never()).create(any());
        assertThat(booking.getCallNote()).isEqualTo("Rappeler la veille");
        assertThat(booking.getLeadNumber()).isNull();
    }

    @Test
    void theLeadIsConvertedAtTheVisitOnlyOnce() {
        Booking booking = booking();
        booking.setLeadNumber("LEAD-1");
        Lead lead = Lead.builder().id(11L).leadNumber("LEAD-1").stage(LeadStage.NEW).build();
        when(leads.findByLeadNumber("LEAD-1")).thenReturn(Optional.of(lead));

        intake().convert(booking, "Venue");

        ArgumentCaptor<CrmDtos.ConvertLeadRequest> convert = ArgumentCaptor.forClass(CrmDtos.ConvertLeadRequest.class);
        verify(crm).convert(eq(11L), convert.capture());
        assertThat(convert.getValue().ownerType()).isEqualTo("CUSTOMER");
        assertThat(convert.getValue().ownerCode()).isEqualTo("GUEST-1");

        lead.setStage(LeadStage.WON);
        intake().convert(booking, "Paiement");
        verify(crm).convert(eq(11L), any());
    }
}
