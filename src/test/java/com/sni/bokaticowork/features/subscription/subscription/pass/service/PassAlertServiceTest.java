package com.sni.bokaticowork.features.subscription.subscription.pass.service;

import com.sni.bokaticowork.features.subscription.notification.dto.CreateSubscriptionNotificationRequest;
import com.sni.bokaticowork.features.subscription.notification.enums.SubscriptionNotificationType;
import com.sni.bokaticowork.features.subscription.notification.service.SubscriptionNotificationService;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassType;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassAlert;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassAlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Les alertes de pass.
 *
 * <p>Trois situations qu'un titulaire découvrait au mauvais moment. Les deux premières lui coûtent ;
 * la troisième coûte à la relation, et c'est celle que personne ne voyait — un pass jamais utilisé
 * est un client qui ne reviendra pas, et on l'apprend quand il est parti.</p>
 *
 * <p>Ce qui est vérifié ici est surtout la mémoire des annonces. Sans elle, un traitement quotidien
 * répéterait la même alerte chaque jour jusqu'à l'expiration du pass, ce qui est la façon la plus
 * sûre de faire ignorer toutes les suivantes.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PassAlertServiceTest {

    @Mock private PassRepository passRepository;
    @Mock private PassAlertRepository alertRepository;
    @Mock private SubscriptionNotificationService notificationService;

    @InjectMocks
    private PassAlertService service;

    @Captor
    private ArgumentCaptor<CreateSubscriptionNotificationRequest> notificationCaptor;
    @Captor
    private ArgumentCaptor<PassAlert> alertCaptor;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "expiringDays", 7);
        ReflectionTestUtils.setField(service, "lowBalanceRatio", "0.2");
        ReflectionTestUtils.setField(service, "unusedDays", 14);

        when(passRepository.findExpiringSoon(any(), any(), anyInt())).thenReturn(List.of());
        when(passRepository.findLowBalance(any(BigDecimal.class), any(), anyInt())).thenReturn(List.of());
        when(passRepository.findUnused(any(), any(), anyInt())).thenReturn(List.of());
        when(alertRepository.alreadyAnnounced(anyLong(), anyString(), anyString())).thenReturn(false);
        when(alertRepository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    // -------------------------------------------------------------------------------------
    // Expiration
    // -------------------------------------------------------------------------------------

    @Test
    void announcesAPassAboutToExpire() {
        Pass pass = pass(10, 2);
        pass.setValidUntil(Instant.now().plus(Duration.ofDays(3)));
        when(passRepository.findExpiringSoon(any(), any(), anyInt())).thenReturn(List.of(pass));

        assertEquals(1, service.announceExpiring());

        verify(notificationService).queue(notificationCaptor.capture());
        assertEquals(SubscriptionNotificationType.PASS_EXPIRING, notificationCaptor.getValue().notificationType());
        assertTrue(notificationCaptor.getValue().subject().contains("expire dans 3 jours"));
        assertTrue(notificationCaptor.getValue().payloadJson().contains("PASS-0001"));
    }

    @Test
    void saysTomorrowRatherThanZeroDays() {
        Pass pass = pass(10, 2);
        pass.setValidUntil(Instant.now().plus(Duration.ofHours(20)));
        when(passRepository.findExpiringSoon(any(), any(), anyInt())).thenReturn(List.of(pass));

        service.announceExpiring();

        verify(notificationService).queue(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().subject().contains("demain"));
    }

    /** Sans cette mémoire, le titulaire recevrait le même message chaque matin pendant une semaine. */
    @Test
    void announcesAnExpiryOnlyOnce() {
        Pass pass = pass(10, 2);
        pass.setValidUntil(Instant.now().plus(Duration.ofDays(3)));
        when(passRepository.findExpiringSoon(any(), any(), anyInt())).thenReturn(List.of(pass));
        when(alertRepository.alreadyAnnounced(anyLong(), anyString(), anyString())).thenReturn(true);

        assertEquals(0, service.announceExpiring());
        verify(notificationService, never()).queue(any());
    }

    /** L'échéance fait la clef : un pass renouvelé porte une nouvelle date, et se réannonce. */
    @Test
    void keysTheExpiryAlertOnTheDueDateSoARenewedPassAlertsAgain() {
        Pass pass = pass(10, 2);
        pass.setValidUntil(Instant.now().plus(Duration.ofDays(3)));
        when(passRepository.findExpiringSoon(any(), any(), anyInt())).thenReturn(List.of(pass));

        service.announceExpiring();

        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(pass.getValidUntil().atZone(java.time.ZoneId.of("Africa/Lagos")).toLocalDate().toString(),
                alertCaptor.getValue().getThresholdKey());
    }

    // -------------------------------------------------------------------------------------
    // Solde bas
    // -------------------------------------------------------------------------------------

    @Test
    void announcesALowBalanceWithTheRemainingCount() {
        when(passRepository.findLowBalance(any(BigDecimal.class), any(), anyInt()))
                .thenReturn(List.of(pass(10, 8)));

        assertEquals(1, service.announceLowBalance());

        verify(notificationService).queue(notificationCaptor.capture());
        assertEquals(SubscriptionNotificationType.PASS_LOW_BALANCE, notificationCaptor.getValue().notificationType());
        assertTrue(notificationCaptor.getValue().subject().contains("2 utilisations"));
    }

    @Test
    void saysOneUseRatherThanOneUses() {
        when(passRepository.findLowBalance(any(BigDecimal.class), any(), anyInt()))
                .thenReturn(List.of(pass(10, 9)));

        service.announceLowBalance();

        verify(notificationService).queue(notificationCaptor.capture());
        assertTrue(notificationCaptor.getValue().subject().contains("une utilisation"));
    }

    /**
     * Le solde restant fait la clef : chaque palier franchi mérite son propre message. Une seule
     * alerte pour toute la descente laisserait le titulaire sans rappel jusqu'à l'épuisement.
     */
    @Test
    void keysTheBalanceAlertOnTheRemainingCount() {
        when(passRepository.findLowBalance(any(BigDecimal.class), any(), anyInt()))
                .thenReturn(List.of(pass(10, 8)));

        service.announceLowBalance();

        verify(alertRepository).save(alertCaptor.capture());
        assertEquals("2", alertCaptor.getValue().getThresholdKey());
    }

    // -------------------------------------------------------------------------------------
    // Pass inutilisé
    // -------------------------------------------------------------------------------------

    @Test
    void announcesAPassNeverUsed() {
        when(passRepository.findUnused(any(), any(), anyInt())).thenReturn(List.of(pass(10, 0)));

        assertEquals(1, service.announceUnused());

        verify(notificationService).queue(notificationCaptor.capture());
        assertEquals(SubscriptionNotificationType.PASS_UNUSED, notificationCaptor.getValue().notificationType());
    }

    /**
     * Une seule fois par pass. Relancer chaque semaine quelqu'un qui n'est pas venu ne le fait pas
     * venir, cela le fait se désabonner.
     */
    @Test
    void neverNagsTheSameUnusedPassTwice() {
        when(passRepository.findUnused(any(), any(), anyInt())).thenReturn(List.of(pass(10, 0)));

        service.announceUnused();

        verify(alertRepository).save(alertCaptor.capture());
        assertEquals("once", alertCaptor.getValue().getThresholdKey());
    }

    // -------------------------------------------------------------------------------------

    /** Un échec de notification n'empêche pas les pass suivants d'être annoncés. */
    @Test
    void keepsGoingWhenOnePassFailsToQueue() {
        Pass first = pass(10, 0);
        Pass second = pass(10, 0);
        second.setPassNumber("PASS-0002");
        second.setId(2L);
        when(passRepository.findUnused(any(), any(), anyInt())).thenReturn(List.of(first, second));
        when(notificationService.queue(any()))
                .thenThrow(new IllegalStateException("canal indisponible"))
                .thenReturn(null);

        assertEquals(1, service.announceUnused());
    }

    private Pass pass(int maxUses, int usedCount) {
        Pass pass = Pass.builder()
                .passNumber("PASS-0001")
                .name("Pass journée")
                .passType(PassType.DAY_PASS)
                .ownerType(SubscriberType.MEMBER)
                .ownerCode("MEM-1")
                .status(PassStatus.ACTIVE)
                .maxUses(maxUses)
                .usedCount(usedCount)
                .validFrom(Instant.now().minus(Duration.ofDays(30)))
                .build();
        pass.setId(1L);
        return pass;
    }
}
