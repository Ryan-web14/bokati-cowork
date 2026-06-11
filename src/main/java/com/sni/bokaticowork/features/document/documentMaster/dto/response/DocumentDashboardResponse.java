package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DocumentDashboardResponse {
    private DocumentSpace space;
    private long total;
    private Map<DocumentStatus, Long> byStatus;
    private Map<DocumentCategory, Long> byCategory;
    private long pendingReviewOlderThan48h;
    private long needsCorrectionCount;
    private long expiringIn30Days;
    private BigDecimal rejectionRate30d;
    private double avgReviewTimeHours;
    private List<TagCount> topTags;

    @Data
    @Builder
    public static class TagCount {
        private String tagCode;
        private String tagLabel;
        private long count;
    }
}
