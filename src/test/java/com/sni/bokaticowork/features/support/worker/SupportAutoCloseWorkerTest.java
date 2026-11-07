package com.sni.bokaticowork.features.support.worker;

import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import com.sni.bokaticowork.features.support.repository.SupportTicketRepository;
import com.sni.bokaticowork.features.support.service.implementation.SupportTicketEventWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le delai de fermeture automatique suit les parametres, sans redemarrer.
 */
class SupportAutoCloseWorkerTest {

    private final SupportTicketRepository tickets = mock(SupportTicketRepository.class);
    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final SupportAutoCloseWorker worker = new SupportAutoCloseWorker(tickets, mock(SupportTicketEventWriter.class), settings);

    @Test
    @DisplayName("Le passage lit le delai en vigueur · sans valeur modifiee, celui de la configuration")
    void readsTheCurrentDelay() {
        ReflectionTestUtils.setField(worker, "autoCloseDays", 5);
        when(settings.number(anyString(), anyLong())).thenAnswer(call -> call.getArgument(1));
        when(settings.number(eq("bokati.support.auto-close-delay-days"), anyLong())).thenReturn(2L);
        when(tickets.findAutoCloseCandidates(any())).thenReturn(List.of());

        worker.closeStaleResolvedTickets();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(tickets).findAutoCloseCandidates(cutoff.capture());
        assertThat(Duration.between(cutoff.getValue(), Instant.now()).toDays()).isEqualTo(2L);
        assertThat(cutoff.getValue()).isCloseTo(Instant.now().minus(Duration.ofDays(2)), within(Duration.ofSeconds(5)));
    }
}
