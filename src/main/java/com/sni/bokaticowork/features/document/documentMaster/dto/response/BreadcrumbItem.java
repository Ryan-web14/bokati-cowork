package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BreadcrumbItem {
    private String code;
    private String name;
    private Integer depth;
}
