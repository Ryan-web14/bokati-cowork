package com.sni.bokaticowork.security.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.token-hash")
public record TokenHashProperties(

        @Value("${app.security.token-hash.secret}")
        String secret
) {
}