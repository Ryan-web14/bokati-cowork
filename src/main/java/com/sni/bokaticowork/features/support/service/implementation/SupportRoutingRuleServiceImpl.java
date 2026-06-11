package com.sni.bokaticowork.features.support.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.support.dto.SupportDtos.RoutingRuleRequest;
import com.sni.bokaticowork.features.support.dto.SupportDtos.RoutingRuleResponse;
import com.sni.bokaticowork.features.support.mapper.SupportTicketMapper;
import com.sni.bokaticowork.features.support.model.SupportRoutingRule;
import com.sni.bokaticowork.features.support.model.SupportTicket;
import com.sni.bokaticowork.features.support.repository.SupportRoutingRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class SupportRoutingRuleServiceImpl {

    private final SupportRoutingRuleRepository repository;
    private final SupportTicketMapper mapper;

    public RoutingRuleResponse create(RoutingRuleRequest request) {
        validate(request);
        SupportRoutingRule rule = SupportRoutingRule.builder()
                .name(request.name().trim())
                .active(request.active() == null ? Boolean.TRUE : request.active())
                .category(request.category())
                .priority(request.priority())
                .ownerType(normalize(request.ownerType()))
                .relatedType(normalize(request.relatedType()))
                .assignedTo(request.assignedTo())
                .teamCode(normalize(request.teamCode()))
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();
        return mapper.toRoutingRuleResponse(repository.save(rule));
    }

    public RoutingRuleResponse update(Long id, RoutingRuleRequest request) {
        validate(request);
        SupportRoutingRule rule = getRule(id);
        rule.setName(request.name().trim());
        if (request.active() != null) rule.setActive(request.active());
        rule.setCategory(request.category());
        rule.setPriority(request.priority());
        rule.setOwnerType(normalize(request.ownerType()));
        rule.setRelatedType(normalize(request.relatedType()));
        rule.setAssignedTo(request.assignedTo());
        rule.setTeamCode(normalize(request.teamCode()));
        if (request.sortOrder() != null) rule.setSortOrder(request.sortOrder());
        return mapper.toRoutingRuleResponse(repository.save(rule));
    }

    public void delete(Long id) {
        SupportRoutingRule rule = getRule(id);
        repository.delete(rule);
    }

    @Transactional(readOnly = true)
    public List<RoutingRuleResponse> list() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream()
                .map(mapper::toRoutingRuleResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<SupportRoutingRule> matchRule(SupportTicket ticket) {
        return repository.findAllByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .filter(rule -> matches(rule, ticket))
                .findFirst();
    }

    private boolean matches(SupportRoutingRule rule, SupportTicket ticket) {
        if (rule.getCategory() != null && rule.getCategory() != ticket.getCategory()) return false;
        if (rule.getPriority() != null && rule.getPriority() != ticket.getPriority()) return false;
        if (StringUtils.hasText(rule.getOwnerType())
                && !rule.getOwnerType().equalsIgnoreCase(ticket.getOwnerType())) return false;
        if (StringUtils.hasText(rule.getRelatedType())
                && !rule.getRelatedType().equalsIgnoreCase(ticket.getRelatedType())) return false;
        return true;
    }

    private SupportRoutingRule getRule(Long id) {
        if (id == null) throw new BadRequestException("L'identifiant de la règle est requis");
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Routing rule not found: " + id));
    }

    private void validate(RoutingRuleRequest request) {
        if (request == null || !StringUtils.hasText(request.name())) {
            throw new BadRequestException("Le nom de la règle est requis");
        }
        if (request.assignedTo() == null && !StringUtils.hasText(request.teamCode())) {
            throw new BadRequestException("La règle doit définir un agent ou une équipe");
        }
    }

    private String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
