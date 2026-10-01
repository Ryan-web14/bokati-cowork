package com.sni.bokaticowork.features.billing.dunning.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.dunning.model.DunningNotice;
import com.sni.bokaticowork.features.billing.dunning.model.DunningPolicy;
import com.sni.bokaticowork.features.billing.dunning.model.DunningStep;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningNoticeRepository;
import com.sni.bokaticowork.features.billing.dunning.repository.DunningPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/** Les politiques de relance · lues, ecrites, avec leurs paliers. */
@Service
@RequiredArgsConstructor
public class DunningPolicyService {

    private final DunningPolicyRepository policyRepository;
    private final DunningNoticeRepository noticeRepository;

    public record StepSpec(Integer daysAfterDue, DunningStep.Action action, DunningStep.Channel channel, String subjectTemplate, String messageTemplate) {
    }

    public record PolicySpec(String policyCode, String name, DunningPolicy.Segment segment, DunningPolicy.Tone tone, Boolean active, List<StepSpec> steps) {
    }

    @Transactional(readOnly = true)
    public List<DunningPolicy> list() {
        return policyRepository.findAllByOrderBySegmentAsc();
    }

    @Transactional(readOnly = true)
    public DunningPolicy get(String policyCode) {
        return policyRepository.findByPolicyCode(policyCode)
                .orElseThrow(() -> new ResourceNotFoundException("Politique de relance introuvable"));
    }

    /**
     * Cree ou remplace · les paliers sont remplaces en bloc, pas fusionnes. Une politique active
     * par segment : activer celle-ci desactive l'autre.
     */
    @Transactional
    public DunningPolicy save(PolicySpec spec) {
        if (!StringUtils.hasText(spec.policyCode()) || !StringUtils.hasText(spec.name())) {
            throw new BadRequestException("Code et nom sont requis");
        }
        if (spec.steps() == null || spec.steps().isEmpty()) {
            throw new BadRequestException("Une politique de relance a au moins un palier");
        }
        DunningPolicy policy = policyRepository.findByPolicyCode(spec.policyCode().trim())
                .orElseGet(() -> DunningPolicy.builder().policyCode(spec.policyCode().trim()).build());
        policy.setName(spec.name().trim());
        policy.setSegment(spec.segment() == null ? DunningPolicy.Segment.DEFAULT : spec.segment());
        policy.setTone(spec.tone() == null ? DunningPolicy.Tone.STANDARD : spec.tone());
        boolean active = spec.active() == null || spec.active();
        if (active) {
            policyRepository.findFirstBySegmentAndActiveTrue(policy.getSegment())
                    .filter(other -> !other.getPolicyCode().equals(policy.getPolicyCode()))
                    .ifPresent(other -> {
                        other.setActive(false);
                        policyRepository.save(other);
                    });
        }
        policy.setActive(active);

        policy.getSteps().clear();
        int order = 1;
        int previousDay = -1;
        for (StepSpec step : spec.steps()) {
            if (step.daysAfterDue() == null || step.daysAfterDue() < 0 || step.action() == null) {
                throw new BadRequestException("Chaque palier a un nombre de jours et une action");
            }
            if (step.daysAfterDue() < previousDay) {
                throw new ConflictException("dunning", "les paliers se donnent dans l'ordre des jours");
            }
            previousDay = step.daysAfterDue();
            policy.getSteps().add(DunningStep.builder().policy(policy).stepOrder(order++).daysAfterDue(step.daysAfterDue())
                    .action(step.action()).channel(step.channel() == null ? DunningStep.Channel.EMAIL : step.channel())
                    .subjectTemplate(trim(step.subjectTemplate())).messageTemplate(trim(step.messageTemplate())).build());
        }
        return policyRepository.save(policy);
    }

    @Transactional
    public DunningPolicy deactivate(String policyCode) {
        DunningPolicy policy = get(policyCode);
        policy.setActive(false);
        return policyRepository.save(policy);
    }

    @Transactional(readOnly = true)
    public List<DunningNotice> noticesOf(String documentNumber) {
        return noticeRepository.findByDocumentNumberOrderByExecutedAtDesc(documentNumber);
    }

    @Transactional(readOnly = true)
    public List<DunningNotice> noticesOfCustomer(String customerType, String customerCode) {
        return noticeRepository.findByCustomerTypeAndCustomerCodeOrderByExecutedAtDesc(customerType, customerCode);
    }

    @Transactional(readOnly = true)
    public List<DunningNotice> recent() {
        return noticeRepository.findTop200ByOrderByExecutedAtDesc();
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
