package com.sni.bokaticowork.features.document.documentMaster.dto.response;

import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycRequirementStatus;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DocumentFolderResponse {
    private OwnerInfo owner;
    private List<SpaceFolder> spaces;
    private List<KycRequirementStatus> missingRequirements;
    private List<DocumentResponse> expiringSoon;

    @Data
    @Builder
    public static class OwnerInfo {
        private DocumentOwnerType type;
        private String code;
        private String name;
    }

    @Data
    @Builder
    public static class SpaceFolder {
        private DocumentSpace space;
        private long totalDocuments;
        private long approvedDocuments;
        private double completionRate;
        private List<DocumentResponse> documents;
    }
}
