package com.sni.bokaticowork.features.crm.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.enums.LeadInterest;
import com.sni.bokaticowork.features.crm.enums.LeadSource;
import com.sni.bokaticowork.features.crm.enums.LeadStage;
import com.sni.bokaticowork.features.crm.enums.OpportunityStage;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface CrmService {
    // Lead
    LeadResponse create(CreateLeadRequest request);
    LeadResponse update(Long id, UpdateLeadRequest request);
    LeadResponse get(Long id);
    LeadResponse getByNumber(String leadNumber);
    PaginatedResponse<LeadResponse> search(LeadStage stage, Long assignedTo,
                                           LeadSource source, String searchText,
                                           Pageable pageable);
    LeadResponse qualify(Long id);
    LeadResponse updateStage(Long id, UpdateLeadStageRequest request);
    LeadResponse convert(Long id, ConvertLeadRequest request);
    LeadResponse addActivity(Long id, AddLeadActivityRequest request);
    KanbanBoardResponse getBoard(Long assignedTo, LeadSource source, LeadInterest interest, String searchText);
    PipelineResponse pipeline();
    CrmMetricsResponse metrics();
    CrmAnalyticsResponse analytics(Instant from, Instant to);
    String generateQuote(Long id, GenerateQuoteRequest request);

    // Opportunity
    OpportunityResponse createOpportunity(Long leadId, CreateOpportunityRequest request);
    PaginatedResponse<OpportunityResponse> listOpportunities(OpportunityStage stage, Pageable pageable);
    OpportunityResponse updateOpportunityStage(Long opportunityId, UpdateOpportunityStageRequest request);
}
