package com.sni.bokaticowork.features.payment.compliance;

import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceCase;
import com.sni.bokaticowork.features.payment.compliance.model.ComplianceFreeze;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseNoteRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceCaseRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceFreezeRepository;
import com.sni.bokaticowork.features.payment.compliance.repository.ComplianceRuleRepository;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceAuditService;
import com.sni.bokaticowork.features.payment.compliance.service.ComplianceCaseService;
import com.sni.bokaticowork.features.payment.control.repository.WalletRiskFlagRepository;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Les vues de la conformite lisent le portefeuille du dossier et du gel apres la fin de la
 * transaction (open-in-view coupe). Sans chargement, la liste des dossiers, un dossier, ses
 * actions et la liste des gels tombaient en 500.
 */
class ComplianceLazyLoadingIT extends PostgresIntegrationTestBase {

    private static final ComplianceCase.Status OPEN = ComplianceCase.Status.OPEN;

    @Autowired private EntityManager entityManager;
    @Autowired private ComplianceCaseRepository cases;
    @Autowired private ComplianceFreezeRepository freezes;
    @Autowired private ComplianceCaseNoteRepository notes;
    @Autowired private ComplianceRuleRepository rules;
    @Autowired private WalletRiskFlagRepository flags;
    @Autowired private WalletAccountRepository wallets;

    @BeforeEach
    void seed() {
        entityManager.createNativeQuery("""
                INSERT INTO wallet_account (id, wallet_number, owner_type, owner_code, currency, status)
                VALUES (993000001, 'WAL-IT-LAZY', 'MEMBER', 'MBR-IT-LAZY', 'XAF', 'ACTIVE')
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO compliance_case (id, case_number, subject_type, subject_code, wallet_id, title, status,
                                             priority, assigned_to, due_at)
                VALUES (993000002, 'CCS-IT-LAZY', 'WALLET', 'WAL-IT-LAZY', 993000001, 'Fractionnement', 'OPEN',
                        'HIGH', 'analyste@test', now() + INTERVAL '3 days')
                """).executeUpdate();
        entityManager.createNativeQuery("""
                INSERT INTO compliance_freeze (id, freeze_number, wallet_id, authority, instruction_reference,
                                               rationale, frozen_by)
                VALUES (993000003, 'CFZ-IT-LAZY', 993000001, 'ANIF', 'REQ-1', 'Instruction de gel', 'it@test')
                """).executeUpdate();
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Les listes de dossiers se lisent avec le portefeuille deja charge")
    void caseListsLoadTheWallet() {
        assertLoaded(cases.findByStatusInOrderByPriorityDescDueAtAsc(List.of(OPEN), PageRequest.of(0, 20)).getContent());
        assertLoaded(cases.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)).getContent());
        assertLoaded(cases.findByAssignedToAndStatusInOrderByDueAtAsc("analyste@test", List.of(OPEN), PageRequest.of(0, 20)).getContent());
        assertLoaded(cases.findBySubjectTypeAndSubjectCodeOrderByCreatedAtDesc("WALLET", "WAL-IT-LAZY"));
    }

    @Test
    @DisplayName("Un dossier lu par le service garde son portefeuille une fois la session fermee")
    void caseReadByTheServiceKeepsItsWallet() {
        ComplianceCaseService service = new ComplianceCaseService(cases, notes, rules, flags, wallets,
                mock(ComplianceAuditService.class), mock(SequenceGeneratorFacade.class));

        ComplianceCase read = service.get("CCS-IT-LAZY");
        entityManager.clear();

        assertThat(Hibernate.isInitialized(read.getWallet())).isTrue();
        assertThat(read.getWallet().getWalletNumber()).isEqualTo("WAL-IT-LAZY");
    }

    @Test
    @DisplayName("Les listes de gels se lisent avec le portefeuille deja charge")
    void freezeListsLoadTheWallet() {
        List<ComplianceFreeze> active = freezes.findByLiftedAtIsNullOrderByFrozenAtDesc(PageRequest.of(0, 20)).getContent();
        entityManager.clear();
        assertThat(active).isNotEmpty();
        assertThat(active).allSatisfy(freeze -> assertThat(Hibernate.isInitialized(freeze.getWallet())).isTrue());

        List<ComplianceFreeze> history = freezes.findByWallet_IdOrderByFrozenAtDesc(993000001L);
        entityManager.clear();
        assertThat(history).singleElement()
                .satisfies(freeze -> assertThat(freeze.getWallet().getWalletNumber()).isEqualTo("WAL-IT-LAZY"));
    }

    private void assertLoaded(List<ComplianceCase> found) {
        entityManager.clear();
        assertThat(found).isNotEmpty();
        assertThat(found).allSatisfy(complianceCase -> {
            assertThat(Hibernate.isInitialized(complianceCase.getWallet())).isTrue();
            assertThat(complianceCase.getWallet().getWalletNumber()).isEqualTo("WAL-IT-LAZY");
        });
    }
}
