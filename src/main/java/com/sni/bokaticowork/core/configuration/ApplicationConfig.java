package com.sni.bokaticowork.core.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class ApplicationConfig {

    private final Environment environment;

    //TODO: define this value
    @Value("${app.error.verbose:false}")
    private boolean verboseErrors;

    public ApplicationConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public boolean isDevEnvironment() {
        for (String profile : environment.getActiveProfiles()) {
            if (profile.equals("dev") || profile.equals("test")) {
                return true;
            }
        }
        return verboseErrors;
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return objectMapper;
    }

}
