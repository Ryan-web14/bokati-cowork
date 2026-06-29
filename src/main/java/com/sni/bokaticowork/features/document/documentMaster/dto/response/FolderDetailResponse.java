package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class FolderDetailResponse {
    private String code;
    private String name;
    private String description;
    private String parentCode;
    private DocumentSpace space;
    private DocumentOwnerType ownerType;
    private Long ownerId;
    private String ownerName;
    private String path;
    private Integer depth;
    private Integer sortOrder;
    private String color;
    private String icon;
    private long childrenCount;
    private long documentsCount;
    private Long createdBy;
    private String createdByEmail;
    private Instant createdAt;
    private Instant updatedAt;
}
