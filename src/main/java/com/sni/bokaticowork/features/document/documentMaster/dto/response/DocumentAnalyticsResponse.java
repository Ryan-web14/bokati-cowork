package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class DocumentAnalyticsResponse {

    private DocumentSpace space;
    private int periodMonths;

    // ── Current state ─────────────────────────────────────────────────────────
    private long totalDocuments;
    private long pendingReview;
    private long needsCorrection;
    private long expiredDocuments;
    private Map<String, Long> byStatus;
    private Map<String, Long> bySpace;
    private Map<String, Long> byCategory;

    // ── Quality metrics (last 30 d) ────────────────────────────────────────────
    private double correctionRate30d;
    private double rejectionRate30d;
    private double avgReviewTimeHours;

    // ── Time series ───────────────────────────────────────────────────────────
    private List<PeriodCount> uploadsByMonth;
    private List<PeriodCount> approvalsByMonth;
    private List<PeriodCount> rejectionsByMonth;
    private List<PeriodCount> correctionsByMonth;

    @Data
    @Builder
    public static class PeriodCount {
        private String period;
        private long count;
    }
}
