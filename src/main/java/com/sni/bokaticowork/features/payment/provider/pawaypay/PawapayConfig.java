package com.sni.bokaticowork.features.payment.provider.pawaypay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PawapayProperties.class)
public class PawapayConfig {

    @Bean
    @ConditionalOnProperty(name = "bokati.payment.pawaypay.enabled", havingValue = "true")
    public PawapayClient pawapayClient(PawapayProperties properties, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new PawapayClient(properties, objectMapper);
    }
}