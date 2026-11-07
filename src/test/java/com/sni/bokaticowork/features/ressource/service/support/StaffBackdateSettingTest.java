package com.sni.bokaticowork.features.ressource.service.support;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * La retroaction de l'equipe suit les parametres, sans redemarrer.
 */
class StaffBackdateSettingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 20, 10, 0);

    @Test
    @DisplayName("Sans parametre, la configuration · modifie, la nouvelle limite aussitot")
    void followsTheSetting() {
        StaffBookingOverride override = new StaffBookingOverride(31);
        assertThat(override.earliestStart(NOW).toLocalDate()).isEqualTo(LocalDate.of(2026, 10, 20));

        PlatformSettings settings = mock(PlatformSettings.class);
        when(settings.number(eq("bokati.booking.staff.max-backdate-days"), anyLong())).thenReturn(7L);
        override.setSettings(settings);

        assertThat(override.maxBackdateDays()).isEqualTo(7);
        assertThat(override.earliestStart(NOW).toLocalDate()).isEqualTo(LocalDate.of(2026, 11, 13));
    }
}
