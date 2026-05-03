package com.sni.bokaticowork.core.communication.mailService.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "microsoft.graph")
public class MicrosoftGraphMailProperties {

    @Value("${spring.mail.microsoft.graph.client-id:}")
    private String clientId;

    @Value("${spring.mail.microsoft.graph.client-secret:}")
    private String clientSecret;

    @Value("${spring.mail.microsoft.graph.tenant-id:}")
    private String tenantId;

    @Value("${spring.mail.microsoft.graph.sender-email:}")
    private String senderEmail;
}
