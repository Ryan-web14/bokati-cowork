package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.company.service.interfaces.BusinessService;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.documentMaster.model.DocumentRequirement;
import com.sni.bokaticowork.features.document.documentMaster.repository.DocumentRequirementRepository;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCaseRequirement;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRequirementRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lot KG1 : les exigences d'un dossier sont les exigences globales (filtrees par type de client et
 * niveau demande) plus celles du dossier ; une exigence ajoutee s'ajoute (elle ne remplace plus tout),
 * une exigence du dossier desactivee dispense de la piece.
 */
class KycRequirementResolverKg1Test {

    private final DocumentRequirementRepository requirements = mock(DocumentRequirementRepository.class);
    private final KycCaseRequirementRepository caseRequirements = mock(KycCaseRequirementRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final KycRequirementResolver resolver = new KycRequirementResolver(requirements, caseRequirements, customers, mock(BusinessService.class));

    private static DocumentRequirement global(String type, boolean required, Integer level, String customerType) {
        DocumentRequirement requirement = new DocumentRequirement();
        requirement.setOwnerType(DocumentOwnerType.CUSTOMER);
        requirement.setDocumentTypeCode(type);
        requirement.setDocumentTypeName(type);
        requirement.setRequired(required);
        requirement.setKycLevel(level);
        requirement.setCustomerType(customerType);
        return requirement;
    }

    private static KycCaseRequirement own(String type, boolean required, boolean active) {
        return KycCaseRequirement.builder().documentTypeCode(type).documentTypeName(type).required(required).active(active).build();
    }

    private KycCase customerCase(int level) {
        Customer customer = new Customer();
        customer.setType(CustomerType.values()[0]);
        when(customers.findById(9L)).thenReturn(Optional.of(customer));
        return KycCase.builder().id(1L).ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L).requestedKycLevel(level).build();
    }

    private List<String> types(List<DocumentRequirement> resolved) {
        return resolved.stream().map(DocumentRequirement::getDocumentTypeCode).toList();
    }

    @Test
    void anAddedRequirementIsAddedToTheGlobalOnes() {
        KycCase kycCase = customerCase(1);
        when(requirements.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.CUSTOMER))
                .thenReturn(List.of(global("CNI", true, 1, null), global("JUSTIF_DOMICILE", true, 1, null)));
        when(caseRequirements.findAllByKycCaseOrderByDocumentTypeNameAsc(any())).thenReturn(List.of(own("ATTESTATION_FISCALE", true, true)));

        assertThat(types(resolver.resolve(kycCase))).containsExactlyInAnyOrder("CNI", "JUSTIF_DOMICILE", "ATTESTATION_FISCALE");
        assertThat(resolver.accepts(kycCase, "attestation_fiscale")).isTrue();
        assertThat(resolver.accepts(kycCase, "PASSEPORT")).isFalse();
    }

    @Test
    void aCaseRowOverridesOrWaivesAGlobalRequirement() {
        KycCase kycCase = customerCase(1);
        when(requirements.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.CUSTOMER))
                .thenReturn(List.of(global("CNI", true, 1, null), global("JUSTIF_DOMICILE", true, 1, null)));
        when(caseRequirements.findAllByKycCaseOrderByDocumentTypeNameAsc(any()))
                .thenReturn(List.of(own("CNI", false, true), own("JUSTIF_DOMICILE", true, false)));

        List<DocumentRequirement> resolved = resolver.resolve(kycCase);
        assertThat(types(resolved)).containsExactly("CNI");
        assertThat(resolved.getFirst().getRequired()).isFalse();
    }

    @Test
    void higherLevelPiecesAreRequestedOnlyFromThatLevel() {
        when(requirements.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.CUSTOMER))
                .thenReturn(List.of(global("CNI", true, 1, null), global("RIB", true, 2, null), global("ORIGINE_FONDS", true, 3, null)));
        when(caseRequirements.findAllByKycCaseOrderByDocumentTypeNameAsc(any())).thenReturn(List.of());

        assertThat(types(resolver.resolve(customerCase(1)))).containsExactly("CNI");
        assertThat(types(resolver.resolve(customerCase(2)))).containsExactlyInAnyOrder("CNI", "RIB");
        assertThat(types(resolver.resolve(customerCase(3)))).containsExactlyInAnyOrder("CNI", "RIB", "ORIGINE_FONDS");
    }

    @Test
    void theCustomerTypeFiltersGlobalRequirements() {
        KycCase kycCase = customerCase(1);
        String own = CustomerType.values()[0].name();
        String other = CustomerType.values().length > 1 ? CustomerType.values()[1].name() : "AUTRE";
        when(requirements.findAllByOwnerTypeAndActiveTrueOrderByDocumentTypeNameAsc(DocumentOwnerType.CUSTOMER))
                .thenReturn(List.of(global("CNI", true, 1, own), global("STATUTS", true, 1, other)));
        when(caseRequirements.findAllByKycCaseOrderByDocumentTypeNameAsc(any())).thenReturn(List.of());

        assertThat(types(resolver.resolve(kycCase))).containsExactly("CNI");
    }
}
