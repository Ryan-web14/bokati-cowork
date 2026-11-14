package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Messages WhatsApp automatiques : rien sans configuration ni numero, envoi apres la validation de
 * la transaction, un echec ne remonte jamais.
 */
class WhatsAppNotifierTest {

    private final WhatsAppMessageSender sender = mock(WhatsAppMessageSender.class);
    private final InfobipWhatsAppProperties properties = new InfobipWhatsAppProperties();
    private final PlatformSettings settings = mock(PlatformSettings.class);
    private final WhatsAppNotifier notifier;

    @SuppressWarnings("unchecked")
    WhatsAppNotifierTest() {
        ObjectProvider<WhatsAppMessageSender> senderProvider = mock(ObjectProvider.class);
        when(senderProvider.getIfAvailable()).thenReturn(sender);
        ObjectProvider<PlatformSettings> settingsProvider = mock(ObjectProvider.class);
        when(settingsProvider.getIfAvailable()).thenReturn(settings);
        when(settings.bool(eq(WhatsAppNotifier.SETTING), anyBoolean())).thenReturn(true);
        notifier = new WhatsAppNotifier(senderProvider, properties, settingsProvider, Runnable::run);
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void enable() {
        properties.setEnabled(true);
        properties.setSender("242061135836");
    }

    @Test
    void nothingLeavesWithoutConfigurationSwitchOrPhone() {
        notifier.notify("242055645569", "BOOKING_CONFIRMED", List.of("Awa"), "BKG-1", null);
        enable();
        notifier.notify(" ", "BOOKING_CONFIRMED", List.of("Awa"), "BKG-1", null);
        when(settings.bool(eq(WhatsAppNotifier.SETTING), anyBoolean())).thenReturn(false);
        notifier.notify("242055645569", "BOOKING_CONFIRMED", List.of("Awa"), "BKG-1", null);
        assertThat(notifier.active()).isFalse();
        verify(sender, never()).send(anyString(), anyString(), anyList(), any(), anyString(), any());
    }

    @Test
    void sendsAfterCommitWithCleanedValues() {
        enable();
        TransactionSynchronizationManager.initSynchronization();
        notifier.notify("242055645569", "BOOKING_CONFIRMED", List.of("Awa", "Salle\nKouilou", ""), "BKG-1", null);
        verify(sender, never()).send(anyString(), anyString(), anyList(), any(), anyString(), any());

        List<TransactionSynchronization> pending = TransactionSynchronizationManager.getSynchronizations();
        assertThat(pending).hasSize(1);
        pending.forEach(TransactionSynchronization::afterCommit);
        verify(sender).send("242055645569", "BOOKING_CONFIRMED", List.of("Awa", "Salle Kouilou", "-"), null, "BKG-1", null);
    }

    @Test
    void aFailureIsOnlyLogged() {
        enable();
        when(sender.send(anyString(), anyString(), anyList(), any(), anyString(), any()))
                .thenThrow(new BadRequestException("Aucun modele WhatsApp approuve pour INVOICE_PAID"));
        notifier.notify("242055645569", "INVOICE_PAID", List.of("Awa", "150 000", "FAC-1"), "FAC-1", "factures");
        verify(sender).send("242055645569", "INVOICE_PAID", List.of("Awa", "150 000", "FAC-1"), null, "FAC-1", "factures");
    }

    /** Chaque objet envoye automatiquement a son modele, avec le nombre de variables que le code fournit. */
    @Test
    void everyAutomaticPurposeHasItsTemplate() {
        Map<String, Integer> sent = Map.ofEntries(
                Map.entry("BOOKING_CONFIRMED", 5), Map.entry("BOOKING_UPDATED", 5), Map.entry("SUBSCRIPTION_ACTIVATED", 4),
                Map.entry("INVOICE_PAID", 3), Map.entry("WALLET_TOPUP_COMPLETED", 4), Map.entry("WALLET_TRANSFER_SENT", 5),
                Map.entry("WALLET_TRANSFER_RECEIVED", 5), Map.entry("WALLET_PAYMENT_REQUEST_RECEIVED", 4),
                Map.entry("WALLET_LOW_BALANCE", 3), Map.entry("WALLET_SECURITY_ALERT", 2));
        Map<String, WhatsAppService.DefaultTemplate> defaults = WhatsAppService.DEFAULTS.stream()
                .collect(Collectors.toMap(WhatsAppService.DefaultTemplate::purpose, template -> template));
        sent.forEach((purpose, count) -> {
            assertThat(defaults).as(purpose).containsKey(purpose);
            assertThat(WhatsAppService.variables(defaults.get(purpose).text())).as(purpose).isEqualTo(count);
        });
        Set<String> names = WhatsAppService.DEFAULTS.stream().map(WhatsAppService.DefaultTemplate::name).collect(Collectors.toSet());
        assertThat(names).hasSize(WhatsAppService.DEFAULTS.size());
        WhatsAppService.DEFAULTS.forEach(template -> assertThat(template.name()).matches("[a-z0-9_]{1,512}"));
    }
}
