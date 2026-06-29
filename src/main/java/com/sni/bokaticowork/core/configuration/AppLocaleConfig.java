package com.sni.bokaticowork.core.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;

@Configuration
public class AppLocaleConfig {

    @Bean
    public Locale appLocale(@Value("${app.locale:fr_FR}") String localeTag) {
        return Locale.forLanguageTag(localeTag.replace('_', '-'));
    }
}
