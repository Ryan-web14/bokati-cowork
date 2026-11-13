package com.sni.bokaticowork.core.communication.whatsapp.infobip;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Le client Infobip n'existe que si l'envoi WhatsApp est active. */
@Configuration
@EnableConfigurationProperties(InfobipWhatsAppProperties.class)
public class InfobipWhatsAppConfig {

    @Bean
    @ConditionalOnProperty(prefix = "bokati.whatsapp.infobip", name = "enabled", havingValue = "true")
    InfobipWhatsAppClient infobipWhatsAppClient(InfobipWhatsAppProperties properties) {
        return new InfobipWhatsAppClient(properties);
    }
}
