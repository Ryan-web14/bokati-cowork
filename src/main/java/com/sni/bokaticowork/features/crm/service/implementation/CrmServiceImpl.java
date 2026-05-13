package com.sni.bokaticowork.features.crm.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.richtext.RichTextSupport;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentLineRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.enums.BillingLineType;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.crm.dto.CrmDtos.*;
import com.sni.bokaticowork.features.crm.enums.*;
import com.sni.bokaticowork.features.crm.mapper.CrmMapper;
import com.sni.bokaticowork.features.crm.model.Lead;
import com.sni.bokaticowork.features.crm.model.LeadActivity;
import com.sni.bokaticowork.features.crm.model.Opportunity;
import com.sni.bokaticowork.features.crm.repository.LeadActivityRepository;
import com.sni.bokaticowork.features.crm.repository.LeadRepository;
import com.sni.bokaticowork.features.crm.repository.OpportunityRepository;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmEmailService;
import com.sni.bokaticowork.features.crm.service.interfaces.CrmService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class CrmServiceImpl implements CrmService {

    private final LeadRepository leadRepository;
    private final LeadActivityRepository activityRepository;
    private final OpportunityRepository opportunityRepository;
    private final CrmMapper mapper;
    private final RichTextSupport richTextSupport;
    @Lazy private final CrmEmailService emailService;
    @Lazy private final BillingDocumentService billingDocumentService;

    // ── Lead CRUD ─────────────────────────────────────────────────

    @Override
    public LeadResponse create(CreateLeadRequest request) {
        if (request == null || !StringUtils.hasText(request.fullName())) {
            throw new BadRequestException("Lead name is required");
        }
        Lead lead = leadRepository.save(Lead.builder()
                .fullName(request.fullName().trim())
                .email(trim(request.email()))
                .phone(trim(request.phone()))
                .company(trim(request.company()))
                .note(trim(request.note()))
                .source(request.source())
                .interest(request.interest())
                .stage(LeadStage.NEW)
                .estimatedAmount(request.estimatedAmount())
                .probability(request.probability())
                .expectedCloseDate(request.expectedCloseDate())
                .assignedTo(request.assignedTo())
                .build());
        if (lead.getAssignedTo() != null) {
            emailService.sendLeadAssigned(lead);
        }
        return response(lead);
    }

    @Override
    public LeadResponse update(Long id, UpdateLeadRequest request) {
        Lead lead = getLead(id);
        Long previousAgent = lead.getAssignedTo();
        if (StringUtils.hasText(request.fullName())) lead.setFullName(request.fullName().trim());
        if (request.email() != null)          lead.setEmail(trim(request.email()));
        if (request.phone() != null)          lead.setPhone(trim(request.phone()));
        if (request.company() != null)        lead.setCompany(trim(request.company()));
        if (request.note() != null)           lead.setNote(trim(request.note()));
        if (request.source() != null)         lead.setSource(request.source());
        if (request.interest() != null)       lead.setInterest(request.interest());
        if (request.estimatedAmount() != null) lead.setEstimatedAmount(request.estimatedAmount());
        if (request.probability() != null)    lead.setProbability(request.probability());
        if (request.expectedCloseDate() != null) lead.setExpectedCloseDate(request.expectedCloseDate());
        if (request.assignedTo() != null)     lead.setAssignedTo(request.assignedTo());

        Lead saved = leadRepository.save(lead);
        if (saved.getAssignedTo() != null && !saved.getAssignedTo().equals(previousAgent)) {
            emailService.sendLeadAssigned(saved);
        }
        return response(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse get(Long id) {
        return response(getLead(id));
    }

    @Override
    @Transactional(readOnly = true)
    public LeadResponse getByNumber(String leadNumber) {
        Lead lead = leadRepository.findByLeadNumber(leadNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + leadNumber));
        return response(lead);
    }

    // ── Recherche ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<LeadResponse> search(LeadStage stage, Long assignedTo,
                                                  LeadSource source, String searchText,
                                                  Pageable pageable) {
        String stageStr  = stage  != null ? stage.name()  : null;
        String sourceStr = source != null ? source.name() : null;
        String text      = StringUtils.hasText(searchText) ? searchText.trim() : null;
        return new PaginatedResponse<>(
                leadRepository.search(stageStr, assignedTo, sourceStr, text, pageable)
                        .map(this::response));
    }

    // ── Pipeline ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PipelineResponse pipeline() {
        Map<LeadStage, List<LeadResponse>> stages = Arrays.stream(LeadStage.values())
                .collect(Collectors.toMap(
                        s -> s,
                        s -> leadRepository.findAllByStage(s).stream().map(this::response).toList(),
                        (a, b) -> a,
                        () -> new java.util.LinkedHashMap<>()));
        return new PipelineResponse(stages);
    }

    // ── Stage management ─────────────────────────────────────────

    @Override
    public LeadResponse qualify(Long id) {
        return updateStage(id, new UpdateLeadStageRequest(LeadStage.QUALIFIED, null));
    }

    @Override
    public LeadResponse updateStage(Long id, UpdateLeadStageRequest request) {
        if (request == null || request.stage() == null) {
            throw new BadRequestException("Lead stage is required");
        }
        Lead lead = getLead(id);
        String previousStage = lead.getStage().name();
        lead.setStage(request.stage());
        lead.setLostReason(request.stage() == LeadStage.LOST ? request.lostReason() : null);
        Lead saved = leadRepository.save(lead);
        if (!previousStage.equals(saved.getStage().name())) {
            emailService.sendStageChanged(saved, previousStage);
        }
        return response(saved);
    }

    @Override
    public LeadResponse convert(Long id, ConvertLeadRequest request) {
        if (request == null || !StringUtils.hasText(request.ownerType())
                || !StringUtils.hasText(request.ownerCode())) {
            throw new BadRequestException("Converted owner type and code are required");
        }
        Lead lead = getLead(id);
        String previousStage = lead.getStage().name();
        lead.setStage(LeadStage.WON);
        lead.setConvertedOwnerType(request.ownerType().trim());
        lead.setConvertedOwnerCode(request.ownerCode().trim());
        Lead saved = leadRepository.save(lead);
        emailService.sendStageChanged(saved, previousStage);
        return response(saved);
    }

    // ── Activités ─────────────────────────────────────────────────

    @Override
    public LeadResponse addActivity(Long id, AddLeadActivityRequest request) {
        if (request == null || request.activityType() == null) {
            throw new BadRequestException("Activity type is required");
        }
        Lead lead = getLead(id);
        activityRepository.save(LeadActivity.builder()
                .lead(lead)
                .activityType(request.activityType())
                .subject(trim(request.subject()))
                .notes(richTextSupport.normalize(request.notes()))
                .performedBy(trim(request.performedBy()))
                .performedAt(request.performedAt())
                .build());
        lead.setLastActivityAt(Instant.now());
        lead.setDormantAlertSentAt(null);
        leadRepository.save(lead);
        return response(lead);
    }

    // ── Génération de devis ───────────────────────────────────────

    @Override
    public String generateQuote(Long id, GenerateQuoteRequest request) {
        if (request == null || !StringUtils.hasText(request.currency())) {
            throw new BadRequestException("Currency is required to generate a quote");
        }
        Lead lead = getLead(id);
        if (lead.getStage() == LeadStage.LOST) {
            throw new BadRequestException("Cannot generate a quote for a lost lead");
        }
        BigDecimal amount = lead.getEstimatedAmount() != null
                ? lead.getEstimatedAmount() : BigDecimal.ZERO;

        String interestLabel = lead.getInterest() != null
                ? lead.getInterest().name().replace('_', ' ') : "Prestation Bokati Cowork";
        CreateBillingDocumentLineRequest line = new CreateBillingDocumentLineRequest(
                1,
                BillingLineType.SERVICE,
                null,
                interestLabel,
                "Devis généré depuis le lead " + lead.getLeadNumber(),
                BigDecimal.ONE,
                amount,
                null,
                null,
                null,
                null,
                null,
                "CRM_LEAD",
                lead.getLeadNumber()
        );

        BillingDocumentResponse quote = billingDocumentService.createManualQuote(
                new CreateManualBillingDocumentRequest(
                        null, null,
                        lead.getFullName(),
                        lead.getEmail(),
                        lead.getPhone(),
                        null,
                        "CRM_LEAD", lead.getLeadNumber(),
                        "Devis — " + lead.getFullName(),
                        lead.getNote(),
                        null,
                        request.currency().trim().toUpperCase(),
                        LocalDate.now(),
                        LocalDate.now().plusDays(30),
                        null,
                        List.of(line),
                        null, null
                ));

        activityRepository.save(LeadActivity.builder()
                .lead(lead)
                .activityType(LeadActivityType.NOTE)
                .subject("Devis généré")
                .notes(richTextSupport.normalize("Devis " + quote.documentNumber() + " généré automatiquement"))
                .performedBy(trim(request.performedBy()))
                .build());
        lead.setLastActivityAt(Instant.now());
        leadRepository.save(lead);
        return quote.documentNumber();
    }

    // ── Métriques ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CrmMetricsResponse metrics() {
        long total  = leadRepository.count();
        long newL   = leadRepository.countByStage(LeadStage.NEW);
        long won    = leadRepository.countByStage(LeadStage.WON);
        long lost   = leadRepository.countByStage(LeadStage.LOST);
        long active = total - won - lost;
        BigDecimal value = leadRepository.sumActivePipelineValue();
        if (value == null) value = BigDecimal.ZERO;
        double convRate = total > 0 ? Math.round((double) won / total * 1000.0) / 10.0 : 0.0;
        return new CrmMetricsResponse(total, newL, won, lost, active, value, convRate);
    }

    // ── Analytics ─────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public CrmAnalyticsResponse analytics(Instant from, Instant to) {
        if (from == null) from = Instant.now().minus(30, ChronoUnit.DAYS);
        if (to == null)   to   = Instant.now();

        long created = leadRepository.countCreatedBetween(from, to);
        long won     = leadRepository.countWonBetween(from, to);
        long lost    = leadRepository.countLostBetween(from, to);
        double convRate = created > 0 ? Math.round((double) won / created * 1000.0) / 10.0 : 0.0;

        Double avgDays = leadRepository.avgDaysToWinBetween(from, to);
        BigDecimal pipeline = leadRepository.sumActivePipelineValue();

        Map<String, Long> bySource = toMap(leadRepository.countBySourceBetween(from, to));
        Map<String, Long> byStage  = toMap(leadRepository.countByStageBetween(from, to));

        return new CrmAnalyticsResponse(
                from, to, created, won, lost, convRate,
                avgDays != null ? Math.round(avgDays * 10.0) / 10.0 : 0.0,
                bySource, byStage, pipeline
        );
    }

    // ── Opportunity ───────────────────────────────────────────────

    @Override
    public OpportunityResponse createOpportunity(Long leadId, CreateOpportunityRequest request) {
        if (request == null || !StringUtils.hasText(request.title())) {
            throw new BadRequestException("Opportunity title is required");
        }
        Lead lead = getLead(leadId);
        Opportunity opp = opportunityRepository.save(Opportunity.builder()
                .lead(lead)
                .title(request.title().trim())
                .estimatedAmount(request.estimatedAmount())
                .probability(request.probability())
                .expectedCloseDate(request.expectedCloseDate())
                .assignedTo(request.assignedTo())
                .notes(trim(request.notes()))
                .build());
        return mapper.toOpportunityResponse(opp);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<OpportunityResponse> listOpportunities(OpportunityStage stage, Pageable pageable) {
        return new PaginatedResponse<>(
                (stage == null ? opportunityRepository.findAll(pageable)
                               : opportunityRepository.findAllByStage(stage, pageable))
                        .map(mapper::toOpportunityResponse));
    }

    @Override
    public OpportunityResponse updateOpportunityStage(Long opportunityId, UpdateOpportunityStageRequest request) {
        if (request == null || request.stage() == null) {
            throw new BadRequestException("Opportunity stage is required");
        }
        Opportunity opp = opportunityRepository.findById(opportunityId)
                .orElseThrow(() -> new ResourceNotFoundException("Opportunity not found: " + opportunityId));
        opp.setStage(request.stage());
        opp.setLostReason(request.stage() == OpportunityStage.LOST ? request.lostReason() : null);
        if (request.stage() == OpportunityStage.WON)  opp.setWonAt(Instant.now());
        if (request.stage() == OpportunityStage.LOST) opp.setLostAt(Instant.now());
        return mapper.toOpportunityResponse(opportunityRepository.save(opp));
    }

    // ── Helpers ───────────────────────────────────────────────────

    private Lead getLead(Long id) {
        if (id == null) throw new BadRequestException("Lead id is required");
        return leadRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lead not found: " + id));
    }

    private LeadResponse response(Lead lead) {
        List<LeadActivity> activities = activityRepository.findAllByLeadOrderByPerformedAtDesc(lead);
        return mapper.toResponse(lead, activities, computeScore(lead, activities.size()));
    }

    private int computeScore(Lead lead, int activityCount) {
        if (lead.getStage() == LeadStage.WON)  return 100;
        if (lead.getStage() == LeadStage.LOST) return 0;
        int base = switch (lead.getStage()) {
            case NEW           -> 5;
            case CONTACTED     -> 20;
            case QUALIFIED     -> 40;
            case PROPOSAL_SENT -> 65;
            default            -> 5;
        };
        int activityBonus = Math.min(activityCount * 5, 20);
        int recencyBonus = 0;
        if (lead.getLastActivityAt() != null) {
            long daysSince = ChronoUnit.DAYS.between(lead.getLastActivityAt(), Instant.now());
            recencyBonus = daysSince <= 7 ? 5 : -5;
        }
        return Math.min(95, Math.max(1, base + activityBonus + recencyBonus));
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            if (row[0] != null) result.put(row[0].toString(), ((Number) row[1]).longValue());
        }
        return result;
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
