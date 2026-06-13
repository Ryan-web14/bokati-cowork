package com.sni.bokaticowork.core.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.databind.type.LogicalType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Configuration
public class ApplicationConfig {

    private final Environment environment;

    //TODO: define this value
    @Value("${app.error.verbose:false}")
    private boolean verboseErrors;

    @Value("${app.time-zone:Africa/Lagos}")
    private String appTimeZone;

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

    @Primary
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.coercionConfigFor(LogicalType.Enum)
                .setCoercion(CoercionInputShape.EmptyString, CoercionAction.AsNull);
        SimpleModule longAsStringModule = new SimpleModule();
        longAsStringModule.addSerializer(Long.class, ToStringSerializer.instance);
        longAsStringModule.addSerializer(long.class, ToStringSerializer.instance);
        objectMapper.registerModule(longAsStringModule);
        SimpleModule frontendDateModule = new SimpleModule();
        frontendDateModule.addSerializer(Instant.class, new JsonSerializer<>() {
            @Override
            public void serialize(Instant value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                if (value == null) {
                    gen.writeNull();
                    return;
                }
                gen.writeString(value.truncatedTo(ChronoUnit.MILLIS).toString());
            }
        });
        frontendDateModule.addDeserializer(Instant.class, new FrontendInstantDeserializer(appTimeZone));
        objectMapper.registerModule(frontendDateModule);
        return objectMapper;
    }

    @Bean
    public WebMvcConfigurer instantQueryParamConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addFormatters(FormatterRegistry registry) {
                registry.addConverter(new FrontendInstantConverter(appTimeZone));
            }
        };
    }

}
