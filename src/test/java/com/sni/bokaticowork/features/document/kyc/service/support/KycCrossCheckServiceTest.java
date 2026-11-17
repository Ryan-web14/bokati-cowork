package com.sni.bokaticowork.features.document.kyc.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.document.kyc.KycDocumentVerificationStatus;
import com.sni.bokaticowork.features.document.kyc.enums.KycCrossRuleType;
import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycCrossValidationRule;
import com.sni.bokaticowork.features.document.kyc.model.KycDocument;
import com.sni.bokaticowork.features.document.kyc.repository.KycCrossValidationRuleRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentOcrResultRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Regles de controle croise sans OCR : dates saisies, unicite du numero, NIU de la fiche ; OCR absent = ignore. */
class KycCrossCheckServiceTest {

    private final KycCrossValidationRuleRepository rules = mock(KycCrossValidationRuleRepository.class);
    private final KycDocumentRepository pieces = mock(KycDocumentRepository.class);
    private final KycDocumentOcrResultRepository ocr = mock(KycDocumentOcrResultRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final KycCrossCheckService service = new KycCrossCheckService(rules, pieces, ocr, mock(MemberRepository.class),
            customers, mock(BusinessRepository.class));

    private final LocalDate today = LocalDate.of(2026, 11, 17);
    private final KycCase kycCase = KycCase.builder().id(1L).ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L).build();

    private static KycCrossValidationRule rule(String code, KycCrossRuleType type, String types, String field, String parameter, boolean blocking) {
        return KycCrossValidationRule.builder().code(code).name(code).ruleType(type).documentTypeCode1(types)
                .fieldToCompare(field).parameter(parameter).blocking(blocking).active(true).build();
    }

    private KycDocument piece(long id, String type, String number, LocalDate issue, LocalDate expiry) {
        return KycDocument.builder().id(id).kycCase(kycCase).ownerType(DocumentOwnerType.CUSTOMER).ownerId(9L)
                .documentType(type).documentNumber(number).issueDate(issue).expiryDate(expiry)
                .status(KycDocumentVerificationStatus.PENDING).build();
    }

    @BeforeEach
    void setUp() {
        when(ocr.findByKycDocument(any())).thenReturn(Optional.empty());
        when(pieces.findAllByDocumentNumberIgnoreCase(anyString())).thenReturn(List.of());
    }

    @Test
    void declaredDatesAreCheckedWithoutOcr() {
        when(rules.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of(
                rule("PIECE_NON_ECHUE", KycCrossRuleType.DOCUMENT_NOT_EXPIRED, null, null, null, true),
                rule("DATES_COHERENTES", KycCrossRuleType.ISSUE_BEFORE_EXPIRY, null, null, null, true),
                rule("VALIDITE_RESTANTE", KycCrossRuleType.MIN_VALIDITY, null, null, "30", false)));
        when(pieces.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of(
                piece(1, "CUSTOMER_PERSON_ID_CARD", "A1", today.minusYears(2), today.minusDays(1)),
                piece(2, "CUSTOMER_PERSON_PASSPORT", "P1", today.plusDays(3), today.plusDays(10))));

        List<KycCrossCheckService.Result> results = service.evaluate(kycCase, today);

        assertThat(results).filteredOn(r -> r.ruleCode().equals("PIECE_NON_ECHUE") && r.documentType().equals("CUSTOMER_PERSON_ID_CARD"))
                .singleElement().extracting(KycCrossCheckService.Result::outcome).isEqualTo(KycCrossCheckService.Outcome.FAILED);
        assertThat(results).filteredOn(r -> r.ruleCode().equals("DATES_COHERENTES") && r.documentType().equals("CUSTOMER_PERSON_PASSPORT"))
                .singleElement().extracting(KycCrossCheckService.Result::outcome).isEqualTo(KycCrossCheckService.Outcome.FAILED);
        assertThat(results).filteredOn(r -> r.ruleCode().equals("VALIDITE_RESTANTE"))
                .singleElement().extracting(KycCrossCheckService.Result::outcome).isEqualTo(KycCrossCheckService.Outcome.FAILED);
        assertThat(KycCrossCheckService.blockingFailures(results)).hasSize(2);
    }

    @Test
    void theNiuMustMatchTheCompanyProfileIgnoringSeparators() {
        BusinessEntity business = new BusinessEntity();
        business.setNiuNumber("M 0123 4567 8");
        Customer customer = new Customer();
        customer.setBusinessEntity(business);
        when(customers.findById(9L)).thenReturn(Optional.of(customer));
        when(rules.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of(
                rule("NIU_PROFIL", KycCrossRuleType.PROFILE_MATCH, "CUSTOMER_COMPANY_NIU", "niu", null, false)));

        when(pieces.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of(piece(1, "CUSTOMER_COMPANY_NIU", "m0123-45678", null, null)));
        assertThat(service.evaluate(kycCase, today)).singleElement().extracting(KycCrossCheckService.Result::outcome)
                .isEqualTo(KycCrossCheckService.Outcome.PASSED);

        when(pieces.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of(piece(1, "CUSTOMER_COMPANY_NIU", "M999", null, null)));
        assertThat(service.evaluate(kycCase, today)).singleElement().extracting(KycCrossCheckService.Result::outcome)
                .isEqualTo(KycCrossCheckService.Outcome.FAILED);
    }

    @Test
    void aNumberUsedByAnotherHolderIsFlagged() {
        when(rules.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of(
                rule("NUMERO_UNIQUE", KycCrossRuleType.UNIQUE_DOCUMENT_NUMBER, null, null, null, false)));
        KycDocument mine = piece(1, "CUSTOMER_PERSON_ID_CARD", "A1", null, null);
        KycDocument other = KycDocument.builder().id(2L).ownerType(DocumentOwnerType.MEMBER).ownerId(3L)
                .documentType("CUSTOMER_PERSON_ID_CARD").documentNumber("a1").build();
        when(pieces.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of(mine));
        when(pieces.findAllByDocumentNumberIgnoreCase("A1")).thenReturn(List.of(mine, other));

        assertThat(service.evaluate(kycCase, today)).singleElement().satisfies(r -> {
            assertThat(r.outcome()).isEqualTo(KycCrossCheckService.Outcome.FAILED);
            assertThat(r.detail()).contains("1 autre(s) titulaire(s)");
        });
    }

    @Test
    void ocrRulesAreSkippedWhenNothingWasRead() {
        when(rules.findAllByActiveTrueOrderByIdAsc()).thenReturn(List.of(
                rule("NUMERO_SAISI_OCR", KycCrossRuleType.DECLARED_VS_OCR, null, "document_number", null, true)));
        when(pieces.findAllByKycCaseOrderByIdAsc(kycCase)).thenReturn(List.of(piece(1, "CUSTOMER_PERSON_ID_CARD", "A1", null, null)));

        List<KycCrossCheckService.Result> results = service.evaluate(kycCase, today);

        assertThat(results).singleElement().extracting(KycCrossCheckService.Result::outcome).isEqualTo(KycCrossCheckService.Outcome.SKIPPED);
        assertThat(KycCrossCheckService.blockingFailures(results)).isEmpty();
    }
}
