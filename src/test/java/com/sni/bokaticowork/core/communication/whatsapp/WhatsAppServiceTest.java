package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppClient;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.phone.PhoneNumberService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Envoi WhatsApp par Infobip : modele approuve, texte libre, configuration absente, validation des modeles. */
class WhatsAppServiceTest {

    private final InfobipWhatsAppProperties properties = new InfobipWhatsAppProperties();
    private final InfobipWhatsAppClient client = mock(InfobipWhatsAppClient.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<InfobipWhatsAppClient> provider = mock(ObjectProvider.class);
    private final WhatsAppTemplateRepository templates = mock(WhatsAppTemplateRepository.class);
    private final WhatsAppMessageRepository messages = mock(WhatsAppMessageRepository.class);
    private final PhoneNumberService phones = mock(PhoneNumberService.class);
    private final WhatsAppService service = new WhatsAppService(properties, provider, templates, messages, phones,
            mock(org.springframework.context.ApplicationEventPublisher.class));

    @BeforeEach
    void setUp() {
        properties.setEnabled(true);
        properties.setSender("242050000000");
        when(provider.getIfAvailable()).thenReturn(client);
        when(phones.normalize("06 123 45 67", "CG")).thenReturn("+242061234567");
        when(templates.findFirstByPurposeAndLanguageAndStatusOrderByCreatedAtDesc(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
    }

    @Test
    void sendsThroughTheApprovedTemplate() {
        when(templates.findFirstByPurposeAndLanguageAndStatusOrderByCreatedAtDesc("BALANCE_CONFIRMATION", "fr", "APPROVED"))
                .thenReturn(Optional.of(WhatsAppTemplate.builder().name("confirmation_solde").language("fr").placeholders(2).status("APPROVED").build()));
        when(client.sendTemplate(eq("242050000000"), eq("242061234567"), eq("confirmation_solde"), eq("fr"), eq(List.of("A", "B")), eq(null), anyString()))
                .thenReturn(new InfobipWhatsAppClient.Sent("m1", "PENDING", "PENDING_ENROUTE", null));
        WhatsAppMessageSender.Result result = service.send("06 123 45 67", "BALANCE_CONFIRMATION", List.of("A", "B"), "texte", "CONF-1");
        assertThat(result.kind()).isEqualTo("TEMPLATE");
        assertThat(result.statusGroup()).isEqualTo("PENDING");
        verify(messages).save(any(WhatsAppMessage.class));
        assertThatThrownBy(() -> service.send("06 123 45 67", "BALANCE_CONFIRMATION", List.of("A"), "texte", "CONF-1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("attend 2");
    }

    @Test
    void freeTextOnlyWhenAllowed() {
        assertThatThrownBy(() -> service.send("06 123 45 67", "BALANCE_CONFIRMATION", List.of(), "texte", "CONF-1"))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Aucun modele");
        verify(client, never()).sendText(anyString(), anyString(), anyString(), anyString());
        properties.setFreeTextAllowed(true);
        when(client.sendText(eq("242050000000"), eq("242061234567"), eq("texte"), anyString()))
                .thenReturn(new InfobipWhatsAppClient.Sent("m2", "PENDING", "PENDING_ENROUTE", null));
        assertThat(service.send("06 123 45 67", "BALANCE_CONFIRMATION", List.of(), "texte", "CONF-1").kind()).isEqualTo("TEXT");
    }

    @Test
    void refusedWhenNotConfigured() {
        properties.setEnabled(false);
        assertThatThrownBy(() -> service.send("06 123 45 67", "X", List.of(), "texte", null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("pas configure");
    }

    @Test
    void templateNeedsOneExamplePerVariable() {
        assertThat(WhatsAppService.variables("Solde {{1}} au {{2}}, rappel {{1}}")).isEqualTo(2);
        assertThatThrownBy(() -> service.createTemplate(new WhatsAppService.TemplateRequest("X", "modele_x", "fr", "UTILITY", "Solde {{1}}", List.of())))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("exemples");
        assertThatThrownBy(() -> service.createTemplate(new WhatsAppService.TemplateRequest("X", "Modele X", "fr", "UTILITY", "Solde", List.of())))
                .isInstanceOf(BadRequestException.class);
        verify(client, never()).createTemplate(anyString(), anyString(), anyString(), anyString(), anyMap());
        for (WhatsAppService.DefaultTemplate item : WhatsAppService.DEFAULTS) {
            if (item.text() != null) {
                assertThat(item.examples()).as(item.name()).hasSize(WhatsAppService.variables(item.text()));
                assertThat(item.text().trim()).as(item.name()).doesNotStartWith("{{").doesNotEndWith("}}");
            }
        }
        // Bouton de lien : suffixe exige ; code d'authentification : le code sert au bouton.
        WhatsAppTemplate invoice = WhatsAppTemplate.builder().name("facture_payee").buttonType("URL").buttonUrl("https://espace.elleaose.com/{{1}}").build();
        assertThat(WhatsAppService.buttonParameter(invoice, List.of("A"), "/factures/F1")).isEqualTo("factures/F1");
        assertThatThrownBy(() -> WhatsAppService.buttonParameter(invoice, List.of("A"), null)).isInstanceOf(BadRequestException.class);
        WhatsAppTemplate otp = WhatsAppTemplate.builder().name("code_verification").buttonType("COPY_CODE").build();
        assertThat(WhatsAppService.buttonParameter(otp, List.of("482913"), null)).isEqualTo("482913");
    }
}
