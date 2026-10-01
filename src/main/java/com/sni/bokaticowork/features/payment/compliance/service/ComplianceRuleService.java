package com.sni.bokaticowork.features.payment.compliance.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.payment.compliance.dto.ComplianceDtos.RuleUpdateRequest;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceRule;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Les regles, en tant que donnees.
 *
 * <p>Modifier un seuil est une decision de conformite comme une autre : elle se motive, et elle
 * laisse une trace avec l'etat d'avant et l'etat d'apres. Une regle dont on ne sait plus pourquoi
 * le seuil a change est une regle dont on ne sait plus ce qu'elle mesure.</p>
 */
@Service
@RequiredArgsConstructor
public class ComplianceRuleService {

    private final ComplianceRuleRepository ruleRepository;
    private final ComplianceAuditService auditService;

    @Transactional(readOnly = true)
    public List<ComplianceRule> all() {
        return ruleRepository.findAllByOrderByRuleCodeAsc();
    }

    @Transactional(readOnly = true)
    public ComplianceRule get(String ruleCode) {
        return ruleRepository.findByRuleCode(ruleCode)
                .orElseThrow(() -> new ResourceNotFoundException("Règle introuvable"));
    }

    @Transactional
    public ComplianceRule update(String ruleCode, RuleUpdateRequest request, String actor) {
        if (!StringUtils.hasText(request.rationale())) {
            throw new BadRequestException("Un changement de règle se motive");
        }
        ComplianceRule rule = get(ruleCode);
        String before = describe(rule);

        if (request.name() != null) rule.setName(request.name().trim());
        if (request.description() != null) rule.setDescription(request.description().trim());
        if (request.severity() != null) rule.setSeverity(request.severity());
        if (request.action() != null) rule.setAction(request.action());
        if (request.amountThreshold() != null) rule.setAmountThreshold(request.amountThreshold());
        if (request.countThreshold() != null) rule.setCountThreshold(request.countThreshold());
        if (request.ratioThreshold() != null) rule.setRatioThreshold(request.ratioThreshold());
        if (request.windowMinutes() != null) rule.setWindowMinutes(request.windowMinutes());
        if (request.active() != null) rule.setActive(request.active());
        if (request.effectiveFrom() != null) rule.setEffectiveFrom(request.effectiveFrom());
        if (request.effectiveTo() != null) rule.setEffectiveTo(request.effectiveTo());
        rule.setApprovedBy(actor);
        ComplianceRule saved = ruleRepository.save(rule);

        auditService.record(actor, null, "RULE_UPDATED", "COMPLIANCE_RULE", saved.getRuleCode(),
                before, describe(saved), request.rationale().trim());
        return saved;
    }

    private String describe(ComplianceRule r) {
        return "severity=" + r.getSeverity() + " action=" + r.getAction()
                + " amount=" + r.getAmountThreshold() + " count=" + r.getCountThreshold()
                + " ratio=" + r.getRatioThreshold() + " window=" + r.getWindowMinutes()
                + " active=" + r.getActive() + " from=" + r.getEffectiveFrom() + " to=" + r.getEffectiveTo();
    }
}
