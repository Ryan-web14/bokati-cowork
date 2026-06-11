package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class DocumentTagResponse {
    private Long id;
    private String code;
    private String label;
    private String color;
    private DocumentSpace space;
    private Long createdBy;
    private Instant createdAt;
}
