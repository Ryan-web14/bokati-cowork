package com.sni.bokaticowork.features.crm.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.enums.*;
import com.sni.bokaticowork.features.crm.mapper.CrmMapper;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.LeadActivity;
import com.sni.bokaticowork.features.crm.repository.LeadActivityRepository;
import com.sni.bokaticowork.features.crm.repository.LeadRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CrmServiceImpl implements CrmService {
    private final LeadRepository leadRepository;
    private final LeadActivityRepository activityRepository;
    private final CrmMapper mapper;

    @Override
    public LeadResponse create(CreateLeadRequest request) {
        if (request == null || !StringUtils.hasText(request.fullName())) {
            throw new BadRequestException("Lead name is required");
        }
        Lead lead = leadRepository.save(Lead.builder()
                .fullName(request.fullName())
                .email(request.email())
                .phone(request.phone())
                .company(request.company())
                .source(request.source())
                .interest(request.interest())
                .stage(LeadStage.NEW)
                .estimatedAmount(request.estimatedAmount())
                .probability(request.probability())
                .expectedCloseDate(request.expectedCloseDate())
                .assignedTo(request.assignedTo())
                .build());
        return response(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<LeadResponse> list(LeadStage stage, Pageable pageable) {
        return new PaginatedResponse<>((stage == null ? leadRepository.findAll(pageable) : leadRepository.findAllByStage(stage, pageable))
                .map(this::response));
    }

    @Override
    public LeadResponse qualify(Long id) {
        Lead lead = getLead(id);
        lead.setStage(LeadStage.QUALIFIED);
        return response(leadRepository.save(lead));
    }

    @Override
    public LeadResponse updateStage(Long id, UpdateLeadStageRequest request) {
        if (request == null || request.stage() == null) {
            throw new BadRequestException("Lead stage is required");
        }
        Lead lead = getLead(id);
        lead.setStage(request.stage());
        lead.setLostReason(request.stage() == LeadStage.LOST ? request.lostReason() : null);
        return response(leadRepository.save(lead));
    }

    @Override
    public LeadResponse convert(Long id, ConvertLeadRequest request) {
        if (request == null || !StringUtils.hasText(request.ownerType()) || !StringUtils.hasText(request.ownerCode())) {
            throw new BadRequestException("Converted owner type and code are required");
        }
        Lead lead = getLead(id);
        lead.setStage(LeadStage.WON);
        lead.setConvertedOwnerType(request.ownerType());
        lead.setConvertedOwnerCode(request.ownerCode());
        return response(leadRepository.save(lead));
    }

    @Override
    public LeadResponse addActivity(Long id, AddLeadActivityRequest request) {
        if (request == null || request.activityType() == null) {
            throw new BadRequestException("Activity type is required");
        }
        Lead lead = getLead(id);
        activityRepository.save(LeadActivity.builder()
                .lead(lead)
                .activityType(request.activityType())
                .subject(request.subject())
                .notes(request.notes())
                .performedBy(request.performedBy())
                .performedAt(request.performedAt())
                .build());
        return response(lead);
    }

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse pipeline() {
        Map<LeadStage, java.util.List<LeadResponse>> stages = Arrays.stream(LeadStage.values())
                .collect(Collectors.toMap(stage -> stage, stage -> leadRepository.findAllByStage(stage).stream().map(this::response).toList()));
        return new PipelineResponse(stages);
    }

    @Override
    @Transactional(readOnly = true)
    public CrmMetricsResponse metrics() {
        var leads = leadRepository.findAll();
        BigDecimal value = leads.stream()
                .filter(lead -> lead.getStage() != LeadStage.WON && lead.getStage() != LeadStage.LOST)
                .map(Lead::getEstimatedAmount)
                .filter(v -> v != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CrmMetricsResponse(
                leads.size(),
                leads.stream().filter(lead -> lead.getStage() == LeadStage.WON).count(),
                leads.stream().filter(lead -> lead.getStage() == LeadStage.LOST).count(),
                value
        );
    }

    private Lead getLead(Long id) {
        if (id == null) {
            throw new BadRequestException("Lead id is required");
        }
        return leadRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    private LeadResponse response(Lead lead) {
        return mapper.toResponse(lead, activityRepository.findAllByLeadOrderByPerformedAtDesc(lead));
    }
}
