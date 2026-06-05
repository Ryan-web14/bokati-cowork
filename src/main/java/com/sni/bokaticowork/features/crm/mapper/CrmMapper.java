package com.sni.bokaticowork.features.crm.mapper;

import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.LeadActivity;
import com.sni.bokaticowork.features.crm.model.Opportunity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CrmMapper {

    public LeadResponse toResponse(Lead lead, List<LeadActivity> activities, int scoredProbability) {
        return new LeadResponse(
                lead.getId(),
                lead.getLeadNumber(),
                lead.getFullName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getCompany(),
                lead.getNote(),
                lead.getSource(),
                lead.getInterest(),
                lead.getStage(),
                lead.getEstimatedAmount(),
                lead.getProbability(),
                scoredProbability,
                lead.getExpectedCloseDate(),
                lead.getAssignedTo(),
                lead.getConvertedOwnerType(),
                lead.getConvertedOwnerCode(),
                lead.getLostReason(),
                lead.getLastActivityAt(),
                lead.getCreatedAt(),
                lead.getUpdatedAt(),
                activities == null ? List.of() : activities.stream().map(this::toActivityResponse).toList()
        );
    }

    public LeadActivityResponse toActivityResponse(LeadActivity activity) {
        return new LeadActivityResponse(
                activity.getId(),
                activity.getActivityType(),
                activity.getSubject(),
                activity.getNotes(),
                activity.getPerformedBy(),
                activity.getPerformedAt()
        );
    }

    public KanbanCardResponse toKanbanCard(Lead lead, int activityCount, int score) {
        return new KanbanCardResponse(
                lead.getId(),
                lead.getLeadNumber(),
                lead.getFullName(),
                lead.getCompany(),
                lead.getEmail(),
                lead.getStage(),
                lead.getSource(),
                lead.getInterest(),
                lead.getEstimatedAmount(),
                lead.getProbability(),
                score,
                lead.getAssignedTo(),
                lead.getLastActivityAt(),
                activityCount,
                lead.getCreatedAt()
        );
    }

    public OpportunityResponse toOpportunityResponse(Opportunity opp) {
        return new OpportunityResponse(
                opp.getId(),
                opp.getOpportunityNumber(),
                opp.getLead().getId(),
                opp.getLead().getLeadNumber(),
                opp.getLead().getFullName(),
                opp.getTitle(),
                opp.getEstimatedAmount(),
                opp.getProbability(),
                opp.getStage(),
                opp.getExpectedCloseDate(),
                opp.getAssignedTo(),
                opp.getNotes(),
                opp.getWonAt(),
                opp.getLostAt(),
                opp.getLostReason(),
                opp.getCreatedAt(),
                opp.getUpdatedAt()
        );
    }
}
