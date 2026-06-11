package com.sni.bokaticowork.features.document.documentMaster.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentAnalyticsResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentDashboardResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentFolderResponse;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentCategory;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentSpace;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface DocumentSearchService {

    PaginatedResponse<DocumentResponse> search(
            String q,
            DocumentSpace space,
            DocumentCategory category,
            List<String> tags,
            DocumentOwnerType ownerType,
            String ownerCode,
            List<DocumentStatus> statuses,
            Integer expiresInDays,
            Boolean isExpired,
            LocalDate uploadedAfter,
            LocalDate uploadedBefore,
            String metaKey,
            String metaValue,
            Pageable pageable
    );

    DocumentDashboardResponse dashboard(DocumentSpace space);

    DocumentFolderResponse folder(DocumentOwnerType ownerType, String ownerCode);

    DocumentAnalyticsResponse analytics(DocumentSpace space, int months);

    byte[] exportCsv(DocumentSpace space, DocumentCategory category,
                     List<String> tags, DocumentOwnerType ownerType, String ownerCode,
                     List<DocumentStatus> statuses, LocalDate uploadedAfter, LocalDate uploadedBefore);
}
