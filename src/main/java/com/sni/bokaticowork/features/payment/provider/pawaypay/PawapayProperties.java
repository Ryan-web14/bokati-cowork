package com.sni.bokaticowork.features.payment.provider.pawaypay;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "bokati.payment.pawaypay")
public class PawapayProperties {
    private boolean enabled = false;
    private String apiKey;
    private String baseUrl = "https://api.pawapay.io/";
    private String callbackBaseUrl = "http://localhost:8080";
    /** HMAC-SHA256 shared secret from PawaPay dashboard — leave empty to skip verification */
    private String callbackSecret;
    /** Interval in ms between polling runs for stuck PROCESSING transactions */
    private long pollingDelayMs = 300_000;
    /** Age threshold in minutes: only poll transactions older than this */
    private int pollingMaxAgeMinutes = 10;
}