package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentFolderTreeNode {
    private String code;
    private String name;
    private String icon;
    private String color;
    private Integer depth;
    private long documentsCount;
    private List<DocumentFolderTreeNode> children;
}
