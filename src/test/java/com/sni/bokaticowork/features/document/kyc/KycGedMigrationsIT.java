package com.sni.bokaticowork.features.document.kyc;

import com.sni.bokaticowork.features.document.kyc.model.KycCase;
import com.sni.bokaticowork.features.document.kyc.model.KycReason;
import com.sni.bokaticowork.features.document.kyc.repository.KycCaseRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycCrossValidationRuleRepository;
import com.sni.bokaticowork.features.document.kyc.repository.KycReasonRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot KG8 : les migrations du centre KYC et GED (production V361 a V364) passent sur une base
 * vierge, leurs donnees preenregistrees sont la, et les entites lisent les nouvelles colonnes.
 */
class KycGedMigrationsIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private KycReasonRepository reasonRepository;
    @Autowired private KycCrossValidationRuleRepository ruleRepository;
    @Autowired private KycCaseRepository caseRepository;

    private List<String> columns(JdbcTemplate jdbc, String table) {
        return jdbc.queryForList("SELECT column_name FROM information_schema.columns WHERE table_name = ?", String.class, table);
    }

    @Test
    @DisplayName("Motifs et regles preenregistres, colonnes des lots KG1, KG7 et KG9 presentes")
    void seededDataAndColumns() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);

        List<KycReason> reasons = reasonRepository.findAllByOrderBySortOrderAscLabelAsc();
        assertThat(reasons).hasSize(11).allMatch(r -> Boolean.TRUE.equals(r.getSystemReason()));
        assertThat(reasonRepository.findByCode("AUTRE")).get().extracting(KycReason::getRequiresText).isEqualTo(true);
        assertThat(reasonRepository.findByCode("DOCUMENT_FALSIFIE")).get().extracting(KycReason::getKind).isEqualTo("REJECTION");

        assertThat(ruleRepository.findAll()).filteredOn(r -> Boolean.TRUE.equals(r.getSystemRule())).hasSize(10);

        assertThat(columns(jdbc, "kyc_case")).contains("decision_reason_code", "requested_kyc_level");
        assertThat(columns(jdbc, "kyc_document")).contains("renewal_reminded_at", "quality_status", "back_quality_blur_score");
    }

    @Test
    @DisplayName("Un dossier refuse relit son motif normalise")
    void aCaseReadsItsDecisionReason() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.update("""
                INSERT INTO kyc_case (id, code, owner_type, owner_id, status, started_at, risk_level, kyc_level, requested_kyc_level,
                                      reminder_count, decision_reason_code, decision_comment)
                VALUES (987654321, 'KYC-IT-KG8', 'MEMBER', 1, 'REJECTED', now(), 'LOW', 1, 1, 0, 'DOCUMENT_FALSIFIE',
                        'Document falsifié ou altéré : essai')
                """);

        KycCase loaded = caseRepository.findByCode("KYC-IT-KG8").orElseThrow();

        assertThat(loaded.getDecisionReasonCode()).isEqualTo("DOCUMENT_FALSIFIE");
        assertThat(loaded.getStatus().name()).isEqualTo("REJECTED");
    }
}
