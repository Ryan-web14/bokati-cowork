package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.communication.whatsapp.WhatsAppService;
import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingCatalog;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingsAdminService;
import com.sni.bokaticowork.features.booking.model.BookingPayOnSiteGrant;
import com.sni.bokaticowork.features.booking.repository.BookingPayOnSiteGrantRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingPayOnSite;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Reglages decides le 19/11/2026 : valeurs par defaut, reglement sur place pour tous, modeles WhatsApp, accord explicite. */
class BookingSettingsDecisionsTest {

    @Test
    void theDecidedDefaultsAreInTheCatalog() {
        assertThat(PlatformSettingCatalog.find("bookings.early-release.minutes")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("30");
        assertThat(PlatformSettingCatalog.find("bookings.last-minute.discount-rate")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("10");
        assertThat(PlatformSettingCatalog.find("bookings.calendar-feed.enabled")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("true");
        assertThat(PlatformSettingCatalog.find("bookings.deposit.rate")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("50");
        assertThat(PlatformSettingCatalog.find("bookings.reliability.threshold")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("60");
        assertThat(PlatformSettingCatalog.find("bookings.reliability.mode")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("DEPOSIT");
        assertThat(PlatformSettingCatalog.find("bookings.pay-on-site.portal-enabled")).get().extracting(PlatformSettingCatalog.Definition::defaultValue).isEqualTo("false");
    }

    @Test
    void openingToAllCanClearTheManualChoicesButKeepsTheAutomaticWithdrawals() {
        BookingPayOnSiteGrantRepository grants = mock(BookingPayOnSiteGrantRepository.class);
        PlatformSettingsAdminService admin = mock(PlatformSettingsAdminService.class);
        BookingPayOnSite payOnSite = new BookingPayOnSite(grants, mock(BookingRepository.class));
        ReflectionTestUtils.setField(payOnSite, "settingsAdmin", admin);
        BookingPayOnSiteGrant manual = BookingPayOnSiteGrant.builder().ownerType(SubscriberType.MEMBER).ownerCode("MBR-1").enabled(false).build();
        BookingPayOnSiteGrant automatic = BookingPayOnSiteGrant.builder().ownerType(SubscriberType.MEMBER).ownerCode("MBR-2").enabled(false)
                .revokedAutomatically(true).build();
        when(grants.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(manual, automatic));

        assertThat(payOnSite.setAll(true, true)).isEqualTo(1);

        verify(admin).update(eq("bookings.pay-on-site.portal-enabled"), eq("true"), anyString(), any());
        verify(grants).delete(manual);
        verify(grants, never()).delete(automatic);
    }

    @Test
    void aSelectionSetsSeveralClientsAtOnce() {
        BookingPayOnSiteGrantRepository grants = mock(BookingPayOnSiteGrantRepository.class);
        when(grants.findByOwnerTypeAndOwnerCode(any(), anyString())).thenReturn(Optional.empty());
        when(grants.save(any())).thenAnswer(call -> call.getArgument(0));
        BookingPayOnSite payOnSite = new BookingPayOnSite(grants, mock(BookingRepository.class));

        List<BookingPayOnSite.Grant> done = payOnSite.setMany(List.of(
                new BookingPayOnSite.ClientChoice(SubscriberType.MEMBER, "MBR-1", true, null),
                new BookingPayOnSite.ClientChoice(SubscriberType.BUSINESS_ENTITY, "ENT-1", false, "Impayés"),
                new BookingPayOnSite.ClientChoice(SubscriberType.MEMBER, " ", true, null)));

        assertThat(done).extracting(BookingPayOnSite.Grant::ownerCode, BookingPayOnSite.Grant::enabled)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("MBR-1", true), org.assertj.core.groups.Tuple.tuple("ENT-1", false));
    }

    @Test
    void lastMinuteOffersNeedAnExplicitYesAndBothTemplatesExist() {
        assertThat(EventType.LAST_MINUTE_OFFERS.defaultEnabled()).isFalse();
        assertThat(EventType.BOOKING_REMINDER.defaultEnabled()).isTrue();
        assertThat(WhatsAppService.DEFAULTS).extracting(WhatsAppService.DefaultTemplate::name)
                .contains("rappel_reservation", "offre_derniere_minute");
        assertThat(WhatsAppService.DEFAULTS).filteredOn(template -> template.name().equals("offre_derniere_minute"))
                .singleElement().extracting(WhatsAppService.DefaultTemplate::category).isEqualTo("MARKETING");
    }
}
