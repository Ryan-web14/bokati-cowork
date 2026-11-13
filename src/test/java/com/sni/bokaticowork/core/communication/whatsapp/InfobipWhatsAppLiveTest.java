package com.sni.bokaticowork.core.communication.whatsapp;

import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppClient;
import com.sni.bokaticowork.core.communication.whatsapp.infobip.InfobipWhatsAppProperties;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Essai d'envoi reel par Infobip, ignore sans cle. Les valeurs viennent de l'environnement, ou du
 * fichier {@code .env} designe par {@code -Dinfobip.env=chemin} : INFOBIP_BASE_URL, INFOBIP_API_KEY,
 * INFOBIP_WHATSAPP_SENDER, INFOBIP_TEST_TO.
 *
 * <p>Texte libre : Meta ne le delivre que dans les 24 heures qui suivent un message du destinataire
 * vers l'expediteur. Ecrire d'abord au numero expediteur depuis le telephone qui recoit l'essai.</p>
 */
class InfobipWhatsAppLiveTest {

    @Test
    void sendsATestMessage() throws IOException {
        Assumptions.assumeTrue(System.getProperty("infobip.create") == null && System.getProperty("infobip.template") == null, "Autre essai demande");
        Map<String, String> values = values();
        String key = values.get("INFOBIP_API_KEY");
        String to = values.get("INFOBIP_TEST_TO");
        Assumptions.assumeTrue(key != null && !key.isBlank() && to != null && !to.isBlank()
                && values.get("INFOBIP_BASE_URL") != null && !values.get("INFOBIP_BASE_URL").isBlank(), "Cle, adresse ou destinataire Infobip absents : essai ignore");
        InfobipWhatsAppProperties properties = new InfobipWhatsAppProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(values.get("INFOBIP_BASE_URL"));
        properties.setApiKey(key);
        properties.setSender(values.get("INFOBIP_WHATSAPP_SENDER"));
        InfobipWhatsAppClient client = new InfobipWhatsAppClient(properties);

        List<InfobipWhatsAppClient.RemoteTemplate> templates = client.templates(properties.getSender());
        System.out.println("Modeles de l'expediteur : " + templates);

        InfobipWhatsAppClient.Sent sent = client.sendText(properties.getSender(), to.replaceAll("[^0-9]", ""),
                "Essai d'envoi WhatsApp depuis Bokati Cowork (ELLE A OSE) par Infobip.", UUID.randomUUID().toString());
        System.out.println("Envoi : " + sent);
        assertThat(sent.statusGroup()).isNotBlank();
    }

    /**
     * Cree chez Infobip des modeles fournis, par le service de l'application
     * ({@code -Dinfobip.create=nom1,nom2} ou {@code all}), puis relit leurs statuts.
     */
    @Test
    void createsDefaultTemplates() throws IOException {
        Map<String, String> values = values();
        String names = System.getProperty("infobip.create");
        Assumptions.assumeTrue(names != null && values.get("INFOBIP_API_KEY") != null && !values.get("INFOBIP_API_KEY").isBlank(), "Aucun modele a creer");
        InfobipWhatsAppProperties properties = properties(values);
        InfobipWhatsAppClient client = new InfobipWhatsAppClient(properties);
        java.util.Set<String> existing = new java.util.HashSet<>();
        client.templates(properties.getSender()).forEach(item -> existing.add(item.name()));
        for (WhatsAppService.DefaultTemplate template : WhatsAppService.DEFAULTS) {
            if ((!"all".equals(names) && !List.of(names.split(",")).contains(template.name())) || existing.contains(template.name())) {
                continue;
            }
            WhatsAppService service = service(properties, client);
            try {
                WhatsAppTemplate created = service.createTemplate(new WhatsAppService.TemplateRequest(template.purpose(), template.name(), "fr",
                        template.category(), template.text(), template.examples(), template.footer(), template.button(), template.codeExpirationMinutes()));
                System.out.println("Modele cree : " + created.getName() + " " + created.getStatus());
            } catch (RuntimeException exception) {
                System.out.println("Modele refuse : " + template.name() + " : " + exception.getMessage());
            }
        }
        client.templates(properties.getSender()).forEach(item -> System.out.println("Modele : " + item));
    }

    /** Envoi par modele approuve ({@code -Dinfobip.template=nom}, variables {@code -Dinfobip.values=a|b}, suffixe {@code -Dinfobip.button=}). */
    @Test
    void sendsATemplateMessage() throws IOException {
        Map<String, String> values = values();
        String name = System.getProperty("infobip.template");
        Assumptions.assumeTrue(name != null && values.get("INFOBIP_API_KEY") != null && !values.get("INFOBIP_API_KEY").isBlank(), "Aucun modele a envoyer");
        InfobipWhatsAppProperties properties = properties(values);
        InfobipWhatsAppClient client = new InfobipWhatsAppClient(properties);
        List<String> placeholders = List.of(System.getProperty("infobip.values", "").split("\\|"));
        InfobipWhatsAppClient.Sent sent = client.sendTemplate(properties.getSender(), values.get("INFOBIP_TEST_TO").replaceAll("[^0-9]", ""), name, "fr",
                placeholders, System.getProperty("infobip.button"), UUID.randomUUID().toString());
        System.out.println("Envoi par modele : " + sent);
        assertThat(sent.statusGroup()).isNotBlank();
    }

    private static InfobipWhatsAppProperties properties(Map<String, String> values) {
        InfobipWhatsAppProperties properties = new InfobipWhatsAppProperties();
        properties.setEnabled(true);
        properties.setBaseUrl(values.get("INFOBIP_BASE_URL"));
        properties.setApiKey(values.get("INFOBIP_API_KEY"));
        properties.setSender(values.get("INFOBIP_WHATSAPP_SENDER"));
        return properties;
    }

    /** Service de l'application, avec un depot en memoire (aucune base pour l'essai). */
    @SuppressWarnings("unchecked")
    private static WhatsAppService service(InfobipWhatsAppProperties properties, InfobipWhatsAppClient client) {
        org.springframework.beans.factory.ObjectProvider<InfobipWhatsAppClient> provider = org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class);
        org.mockito.Mockito.when(provider.getIfAvailable()).thenReturn(client);
        WhatsAppTemplateRepository repository = org.mockito.Mockito.mock(WhatsAppTemplateRepository.class);
        org.mockito.Mockito.when(repository.findByNameAndLanguage(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(java.util.Optional.empty());
        org.mockito.Mockito.when(repository.save(org.mockito.ArgumentMatchers.any(WhatsAppTemplate.class))).thenAnswer(invocation -> invocation.getArgument(0));
        return new WhatsAppService(properties, provider, repository, org.mockito.Mockito.mock(WhatsAppMessageRepository.class),
                org.mockito.Mockito.mock(com.sni.bokaticowork.core.utils.phone.PhoneNumberService.class),
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    private static Map<String, String> values() throws IOException {
        Map<String, String> values = new HashMap<>();
        String file = System.getProperty("infobip.env");
        if (file != null && Files.exists(Path.of(file))) {
            for (String line : Files.readAllLines(Path.of(file), StandardCharsets.UTF_8)) {
                int equals = line.indexOf('=');
                if (!line.startsWith("#") && equals > 0) {
                    values.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
                }
            }
        }
        for (String name : List.of("INFOBIP_BASE_URL", "INFOBIP_API_KEY", "INFOBIP_WHATSAPP_SENDER", "INFOBIP_TEST_TO")) {
            String value = System.getenv(name);
            if (value != null && !value.isBlank()) {
                values.put(name, value);
            }
        }
        return values;
    }
}
