package com.sni.bokaticowork.features.document.kyc.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.kyc")
public class KycAutomationProperties {
    private Expiry expiry = new Expiry();
    private Reminders reminders = new Reminders();
    private Ocr ocr = new Ocr();

    @Data
    public static class Expiry {
        private List<Integer> reminderDays = List.of(60, 30, 7);
        private Integer gracePeriodDays = 15;
    }

    @Data
    public static class Reminders {
        private Boolean enabled = Boolean.TRUE;
        private List<Integer> reminderAfterDays = List.of(3, 7, 14);
        private Integer maxReminders = 3;
    }

    @Data
    public static class Ocr {
        private Boolean enabled = Boolean.TRUE;
        private String provider = "TESSERACT";
        private String language = "fra+eng";
        private String dataPath;
        private Integer minConfidencePercent = 60;
    }
}
