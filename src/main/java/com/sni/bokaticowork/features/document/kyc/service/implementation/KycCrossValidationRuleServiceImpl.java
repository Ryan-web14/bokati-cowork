package com.sni.bokaticowork.features.document.kyc.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.document.kyc.dto.request.KycCrossValidationRuleRequest;
import com.sni.bokaticowork.features.document.kyc.dto.response.KycCrossValidationRuleResponse;
import com.sni.bokaticowork.features.document.kyc.enums.KycCrossRuleType;
import com.sni.bokaticowork.features.document.kyc.model.KycCrossValidationRule;
import com.sni.bokaticowork.features.document.kyc.repository.KycCrossValidationRuleRepository;
import com.sni.bokaticowork.features.document.kyc.service.interfaces.KycCrossValidationRuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class KycCrossValidationRuleServiceImpl implements KycCrossValidationRuleService {

    private static final Set<String> OCR_FIELDS = Set.of("first_name", "last_name", "date_of_birth", "expiry_date",
            "document_number", "nationality");
    private static final Set<String> PROFILE_FIELDS = Set.of("niu", "rccm", "last_name", "first_name");

    private final KycCrossValidationRuleRepository repository;

    @Override
    public KycCrossValidationRuleResponse create(KycCrossValidationRuleRequest request) {
        KycCrossValidationRule rule = KycCrossValidationRule.builder().systemRule(Boolean.FALSE).build();
        apply(rule, request);
        rule.setBlocking(request.getBlocking() == null ? Boolean.TRUE : request.getBlocking());
        rule.setActive(request.getActive() == null ? Boolean.TRUE : request.getActive());
        return toResponse(repository.save(rule));
    }

    @Override
    public KycCrossValidationRuleResponse update(Long id, KycCrossValidationRuleRequest request) {
        KycCrossValidationRule rule = findOrThrow(id);
        if (Boolean.TRUE.equals(rule.getSystemRule())) {
            // Regle preenregistree : seuls le libelle, le parametre, le caractere bloquant et l'activation changent.
            if (request.getName() != null) rule.setName(request.getName().trim());
            if (request.getDescription() != null) rule.setDescription(request.getDescription().trim());
            if (request.getParameter() != null) rule.setParameter(validParameter(rule.getRuleType(), request.getParameter()));
        } else {
            apply(rule, request);
        }
        if (request.getBlocking() != null) {
            rule.setBlocking(request.getBlocking());
        }
        if (request.getActive() != null) {
            rule.setActive(request.getActive());
        }
        return toResponse(repository.save(rule));
    }

    private void apply(KycCrossValidationRule rule, KycCrossValidationRuleRequest request) {
        KycCrossRuleType type = request.getRuleType() == null ? KycCrossRuleType.FIELD_MATCH : request.getRuleType();
        String field = StringUtils.hasText(request.getFieldToCompare()) ? request.getFieldToCompare().trim().toLowerCase(Locale.ROOT) : null;
        switch (type) {
            case FIELD_MATCH -> {
                if (!StringUtils.hasText(request.getDocumentTypeCode1()) || !StringUtils.hasText(request.getDocumentTypeCode2()) || field == null) {
                    throw new BadRequestException("Une règle FIELD_MATCH exige documentTypeCode1, documentTypeCode2 et fieldToCompare");
                }
                requireField(field, OCR_FIELDS);
            }
            case DECLARED_VS_OCR -> requireField(field, Set.of("document_number", "expiry_date"));
            case PROFILE_MATCH -> requireField(field, PROFILE_FIELDS);
            default -> { }
        }
        rule.setRuleType(type);
        rule.setCode(StringUtils.hasText(request.getCode()) ? request.getCode().trim().toUpperCase(Locale.ROOT) : rule.getCode());
        rule.setName(StringUtils.hasText(request.getName()) ? request.getName().trim() : rule.getName());
        rule.setDescription(request.getDescription());
        rule.setDocumentTypeCode1(codes(request.getDocumentTypeCode1()));
        rule.setDocumentTypeCode2(codes(request.getDocumentTypeCode2()));
        rule.setFieldToCompare(field);
        rule.setParameter(request.getParameter() == null ? null : validParameter(type, request.getParameter()));
    }

    private static void requireField(String field, Set<String> allowed) {
        if (field == null || !allowed.contains(field)) {
            throw new BadRequestException("Champ à comparer invalide : " + field + " (valeurs admises : " + String.join(", ", allowed) + ")");
        }
    }

    private static String validParameter(KycCrossRuleType type, String parameter) {
        String value = parameter.trim();
        if (type == KycCrossRuleType.MIN_VALIDITY) {
            try {
                if (Integer.parseInt(value) < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                throw new BadRequestException("Le paramètre d'une règle MIN_VALIDITY est un nombre de jours positif");
            }
        }
        return value.isEmpty() ? null : value;
    }

    private static String codes(String value) {
        return StringUtils.hasText(value) ? value.trim().toUpperCase(Locale.ROOT).replace(" ", "") : null;
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
        if (Boolean.TRUE.equals(rule.getSystemRule())) {
            throw new BadRequestException("Une règle préenregistrée ne se supprime pas : désactivez-la (active = false)");
        }
        repository.delete(rule);
    }

    private KycCrossValidationRule findOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Règle de contrôle croisé introuvable"));
    }

    private KycCrossValidationRuleResponse toResponse(KycCrossValidationRule rule) {
        return KycCrossValidationRuleResponse.builder()
                .id(rule.getId())
                .code(rule.getCode())
                .name(rule.getName())
                .description(rule.getDescription())
                .ruleType(rule.getRuleType())
                .parameter(rule.getParameter())
                .systemRule(rule.getSystemRule())
                .documentTypeCode1(rule.getDocumentTypeCode1())
                .documentTypeCode2(rule.getDocumentTypeCode2())
                .fieldToCompare(rule.getFieldToCompare())
                .blocking(rule.getBlocking())
                .active(rule.getActive())
                .build();
    }
}
