package com.sni.bokaticowork.features.crm.mapper;

import com.sni.bokaticowork.features.crm.dto.CrmDtos.LeadActivityResponse;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.LeadResponse;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.LeadActivity;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class CrmMapper {
    public LeadResponse toResponse(Lead lead, List<LeadActivity> activities) {
        return new LeadResponse(
                lead.getId(),
                lead.getFullName(),
                lead.getEmail(),
                lead.getPhone(),
                lead.getCompany(),
                lead.getSource(),
                lead.getInterest(),
                lead.getStage(),
                lead.getEstimatedAmount(),
                lead.getProbability(),
                lead.getExpectedCloseDate(),
                lead.getAssignedTo(),
                lead.getConvertedOwnerType(),
                lead.getConvertedOwnerCode(),
                lead.getLostReason(),
                lead.getCreatedAt(),
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
}
