package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.features.payment.service.support.RefundSplit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot K1 de la revue comptable : regle de remboursement (A14) et migration. */
class K1CorrectionsTest {

    private static final List<RefundSplit.Part> ALLOCATIONS = List.of(new RefundSplit.Part("FAC-1", new BigDecimal("600")),
            new RefundSplit.Part("FAC-2", new BigDecimal("300")));

    @Test
    @DisplayName("A14 : le trop-percu est rembourse d'abord, aucune facture rouverte")
    void overpaymentFirst() {
        RefundSplit.Result result = RefundSplit.split(new BigDecimal("1000"), ALLOCATIONS, BigDecimal.ZERO, new BigDecimal("100"));
        assertThat(result.unallocated()).isEqualByComparingTo("100");
        assertThat(result.reopened()).isEmpty();
    }

    @Test
    @DisplayName("A14 : deux remboursements partiels ne rouvrent jamais deux fois la meme affectation")
    void successiveRefunds() {
        RefundSplit.Result first = RefundSplit.split(new BigDecimal("1000"), ALLOCATIONS, BigDecimal.ZERO, new BigDecimal("400"));
        assertThat(first.unallocated()).isEqualByComparingTo("100");
        assertThat(first.reopened()).containsExactly(new RefundSplit.Part("FAC-1", new BigDecimal("300")));

        RefundSplit.Result second = RefundSplit.split(new BigDecimal("1000"), ALLOCATIONS, new BigDecimal("400"), new BigDecimal("500"));
        assertThat(second.unallocated()).isEqualByComparingTo("0");
        assertThat(second.reopened()).extracting(RefundSplit.Part::documentNumber).containsExactly("FAC-1", "FAC-2");
        assertThat(second.reopened().get(0).amount()).isEqualByComparingTo("300");
        assertThat(second.reopened().get(1).amount()).isEqualByComparingTo("200");

        RefundSplit.Result last = RefundSplit.split(new BigDecimal("1000"), ALLOCATIONS, new BigDecimal("900"), new BigDecimal("100"));
        assertThat(last.reopened()).singleElement().satisfies(part -> {
            assertThat(part.documentNumber()).isEqualTo("FAC-2");
            assertThat(part.amount()).isEqualByComparingTo("100");
        });
    }

    @Test
    @DisplayName("A14 : transaction entierement affectee, le remboursement rouvre les factures dans l'ordre")
    void fullyAllocated() {
        RefundSplit.Result result = RefundSplit.split(new BigDecimal("900"), ALLOCATIONS, BigDecimal.ZERO, new BigDecimal("900"));
        assertThat(result.unallocated()).isEqualByComparingTo("0");
        assertThat(result.reopened()).extracting(RefundSplit.Part::amount).usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("600"), new BigDecimal("300"));
    }

    @Test
    @DisplayName("Migration K1 : identique en dev et en prod, sans tiret long, statut REVERSED et origine CORRECTION")
    void migrationTwins() throws Exception {
        String dev = Files.readString(Path.of("src/main/resources/db/migration/V401__comptabilite_k1_corriger_une_piece.sql"));
        String prod = Files.readString(Path.of("src/main/resources/db/migration-prod/V395__comptabilite_k1_corriger_une_piece.sql"));
        assertThat(prod).isEqualTo(dev);
        assertThat(dev).doesNotContain("—").doesNotContain("–");
        assertThat(dev).contains("'REVERSED'", "'CORRECTION'", "correction_of", "generated_lines_json", "ck_cash_movement_accounting_status");
    }
}
