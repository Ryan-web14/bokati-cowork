package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseRequirement;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRequirementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Exigences d'un dossier KYC (centre KYC et GED, lot KG1), un seul calcul pour la fiche, le
 * recalcul automatique, l'approbation, l'espace client et le controle des depots.
 *
 * <p>Exigences globales du type de titulaire, filtrees par type de client et forme juridique, puis
 * les exigences propres au dossier par-dessus : une exigence ajoutee au dossier s'y ajoute (ou change
 * le caractere obligatoire d'une exigence globale du meme type) ; une exigence du dossier desactivee
 * dispense ce dossier de la piece. Avant, la moindre exigence ajoutee remplacait toutes les exigences
 * globales, et le recalcul ignorait les exigences du dossier et le type de client.</p>
 */
@Component
@RequiredArgsConstructor
public class KycRequirementResolver {

    private final DocumentRequirementRepository requirementRepository;
    private final KycCaseRequirementRepository caseRequirementRepository;
    private final CustomerRepository customerRepository;
    private final BusinessService businessService;

    public List<DocumentRequirement> resolve(KycCase kycCase) {
        Map<String, DocumentRequirement> byType = new LinkedHashMap<>();
        int level = kycCase.getRequestedKycLevel() == null ? 1 : kycCase.getRequestedKycLevel();
        for (DocumentRequirement requirement : global(kycCase.getOwnerType(), kycCase.getOwnerId())) {
            // Pieces du niveau demande et des niveaux inferieurs.
            int needed = requirement.getKycLevel() == null ? 1 : requirement.getKycLevel();
            if (needed <= level) {
                byType.putIfAbsent(code(requirement.getDocumentTypeCode()), requirement);
            }
        }
        if (kycCase.getId() != null) {
            for (KycCaseRequirement own : caseRequirementRepository.findAllByKycCaseOrderByDocumentTypeNameAsc(kycCase)) {
                String type = code(own.getDocumentTypeCode());
                if (!Boolean.TRUE.equals(own.getActive())) {
                    byType.remove(type);
                    continue;
                }
                DocumentRequirement merged = new DocumentRequirement();
                merged.setOwnerType(kycCase.getOwnerType());
                merged.setDocumentTypeCode(type);
                merged.setDocumentTypeName(StringUtils.hasText(own.getDocumentTypeName()) ? own.getDocumentTypeName()
                        : Optional.ofNullable(byType.get(type)).map(DocumentRequirement::getDocumentTypeName).orElse(type));
                merged.setRequired(own.getRequired() == null || own.getRequired());
                merged.setActive(true);
                byType.put(type, merged);
            }
        }
        List<DocumentRequirement> result = new ArrayList<>(byType.values());
        result.sort(Comparator.comparing(r -> String.valueOf(r.getDocumentTypeName()).toLowerCase(Locale.ROOT)));
        return result;
    }

    /** Une piece de ce type est-elle demandee (obligatoire ou facultative) pour ce dossier ? */
    public boolean accepts(KycCase kycCase, String documentTypeCode) {
        String type = code(documentTypeCode);
        return resolve(kycCase).stream().anyMatch(requirement -> code(requirement.getDocumentTypeCode()).equals(type));
    }

    private List<DocumentRequirement> global(DocumentOwnerType ownerType, Long ownerId) {
        List<DocumentRequirement> all = requirementRepository.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(ownerType);
        String customerType = ownerType == DocumentOwnerType.CUSTOMER && ownerId != null
                ? customerRepository.findById(ownerId).map(c -> c.getType() == null ? null : c.getType().name()).orElse(null) : null;
        String legalForm = ownerType == DocumentOwnerType.BUSINESS && ownerId != null ? legalForm(ownerId) : null;
        return all.stream()
                .filter(r -> customerType == null || !StringUtils.hasText(r.getCustomerType()) || customerType.equalsIgnoreCase(r.getCustomerType()))
                .filter(r -> legalForm == null || !StringUtils.hasText(r.getBusinessLegalForm()) || legalForm.equalsIgnoreCase(r.getBusinessLegalForm()))
                .toList();
    }

    private String legalForm(Long ownerId) {
        try {
            return businessService.serviceBusinessById(ownerId).getLegalForm();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String code(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
