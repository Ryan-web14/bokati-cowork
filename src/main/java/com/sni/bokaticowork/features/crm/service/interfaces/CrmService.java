package com.sni.bokaticowork.features.crm.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import org.springframework.data.domain.Pageable;

public interface CrmService {
    LeadResponse create(CreateLeadRequest request);
    PaginatedResponse<LeadResponse> list(LeadStage stage, Pageable pageable);
    LeadResponse qualify(Long id);
    LeadResponse updateStage(Long id, UpdateLeadStageRequest request);
    LeadResponse convert(Long id, ConvertLeadRequest request);
    LeadResponse addActivity(Long id, AddLeadActivityRequest request);
    PipelineResponse pipeline();
    CrmMetricsResponse metrics();
}
