package com.sni.bokaticowork.features.document.kyc.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class KycDashboardResponse {
    private Instant generatedAt;
    private Map<String, Long> summary;
    private SlaMetrics sla;
    private ExpiringSoonMetrics expiringSoon;
    private List<OwnerTypeMetrics> byOwnerType;
    private List<ActivityMetrics> recentActivity;

    @Data
    @Builder
    public static class SlaMetrics {
        private Long casesReviewedWithin24h;
        private Long casesExceeding48h;
        private BigDecimal avgReviewTimeHours;
    }

    @Data
    @Builder
    public static class ExpiringSoonMetrics {
        private Long within7Days;
        private Long within30Days;
        private Long within60Days;
    }

    @Data
    @Builder
    public static class OwnerTypeMetrics {
        private DocumentOwnerType ownerType;
        private Long count;
        private Long pendingReview;
    }

    @Data
    @Builder
    public static class ActivityMetrics {
        private String action;
        private Long count;
        private String period;
    }
}
