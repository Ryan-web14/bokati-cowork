package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ZipImportResponse {
    private int totalFiles;
    private int succeeded;
    private int failed;
    private int foldersCreated;
    private List<ZipImportItem> items;

    @Data
    @Builder
    public static class ZipImportItem {
        private String originalPath;
        private String documentCode;
        private String folderCode;
        private boolean success;
        private String error;
    }
}
