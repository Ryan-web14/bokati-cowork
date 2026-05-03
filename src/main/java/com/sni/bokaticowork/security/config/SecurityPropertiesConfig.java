package com.sni.bokaticowork.security.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TokenHashProperties.class)
public class SecurityPropertiesConfig {
}