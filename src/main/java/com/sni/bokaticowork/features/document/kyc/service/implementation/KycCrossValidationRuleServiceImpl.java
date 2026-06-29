package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCrossValidationRuleRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCrossValidationRuleResponse;
import com.sni.bokaticowork.features.document.kyc.model.KycCrossValidationRule;
import com.sni.bokaticowork.features.document.kyc.repository.KycCrossValidationRuleRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycCrossValidationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class KycCrossValidationRuleServiceImpl implements KycCrossValidationRuleService {

    private final KycCrossValidationRuleRepository repository;

    @Override
    public KycCrossValidationRuleResponse create(KycCrossValidationRuleRequest request) {
        KycCrossValidationRule rule = KycCrossValidationRule.builder()
                .documentTypeCode1(request.getDocumentTypeCode1().trim().toUpperCase())
                .documentTypeCode2(request.getDocumentTypeCode2().trim().toUpperCase())
                .fieldToCompare(request.getFieldToCompare().trim().toLowerCase())
                .blocking(request.getBlocking() == null ? Boolean.TRUE : request.getBlocking())
                .active(request.getActive() == null ? Boolean.TRUE : request.getActive())
                .build();
        return toResponse(repository.save(rule));
    }

    @Override
    public KycCrossValidationRuleResponse update(Long id, KycCrossValidationRuleRequest request) {
        KycCrossValidationRule rule = findOrThrow(id);
        rule.setDocumentTypeCode1(request.getDocumentTypeCode1().trim().toUpperCase());
        rule.setDocumentTypeCode2(request.getDocumentTypeCode2().trim().toUpperCase());
        rule.setFieldToCompare(request.getFieldToCompare().trim().toLowerCase());
        if (request.getBlocking() != null) {
            rule.setBlocking(request.getBlocking());
        }
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }
        return toResponse(repository.save(rule));
    }

    @Override
    @Transactional(readOnly = true)
    public KycCrossValidationRuleResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KycCrossValidationRuleResponse> list(Boolean activeOnly) {
        List<KycCrossValidationRule> rules = Boolean.TRUE.equals(activeOnly)
                ? repository.findAllByActiveTrueOrderByIdAsc()
                : repository.findAll();
        return rules.stream().map(this::toResponse).toList();
    }

    @Override
    public void delete(Long id) {
        KycCrossValidationRule rule = findOrThrow(id);
        repository.delete(rule);
    }

    private KycCrossValidationRule findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cross-validation rule not found"));
    }

    private KycCrossValidationRuleResponse toResponse(KycCrossValidationRule rule) {
        return KycCrossValidationRuleResponse.builder()
                .id(rule.getId())
                .documentTypeCode1(rule.getDocumentTypeCode1())
                .documentTypeCode2(rule.getDocumentTypeCode2())
                .fieldToCompare(rule.getFieldToCompare())
                .blocking(rule.getBlocking())
                .active(rule.getActive())
                .build();
    }
}
