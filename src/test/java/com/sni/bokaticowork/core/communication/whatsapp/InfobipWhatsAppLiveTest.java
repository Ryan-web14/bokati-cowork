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
