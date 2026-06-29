package com.sni.bokaticowork.features.booking.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BookingCheckInProperties.class)
public class BookingCheckInConfig {
}
