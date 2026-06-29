package com.sni.bokaticowork.features.document.documentMaster.dto.request;

import lombok.Data;

@Data
public class UpdateFolderRequest {

    private String name;

    private String description;

    private String color;

    private String icon;

    private Integer sortOrder;
}
