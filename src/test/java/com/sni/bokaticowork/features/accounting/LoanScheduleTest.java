package com.sni.bokaticowork.features.accounting;

import com.sni.bokaticowork.features.accounting.service.support.LoanSchedule;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Tableau d'amortissement (lot F17). */
class LoanScheduleTest {

    private static List<LoanSchedule.Line> schedule(String method, String deferral, int deferralCount) {
        return LoanSchedule.compute(new LoanSchedule.Terms(new BigDecimal("1000000"), new BigDecimal("12"), "30_360", 1, 12, deferral, deferralCount,
                method, new BigDecimal("1000"), LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15), 1));
    }

    @Test
    @DisplayName("Echeances constantes : 1 000 000 a 12 % sur 12 mois, echeance de 88 849 hors assurance, capital solde")
    void annuity() {
        List<LoanSchedule.Line> lines = schedule("ANNUITY", "NONE", 0);
        assertThat(lines).hasSize(12);
        assertThat(lines.get(0).interest()).isEqualByComparingTo("10000");
        assertThat(lines.get(0).payment()).isEqualByComparingTo("89849");
        assertThat(lines.get(0).principal()).isEqualByComparingTo("78849");
        assertThat(lines.get(11).closingBalance()).isEqualByComparingTo("0");
        assertThat(lines.stream().map(LoanSchedule.Line::principal).reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1000000");
        assertThat(lines.get(11).dueDate()).isEqualTo(LocalDate.of(2027, 1, 15));
    }

    @Test
    @DisplayName("Amortissement constant du capital et in fine")
    void constantCapitalAndBullet() {
        List<LoanSchedule.Line> constant = schedule("CONSTANT_CAPITAL", "NONE", 0);
        assertThat(constant.get(0).principal()).isEqualByComparingTo("83333");
        assertThat(constant.get(11).closingBalance()).isEqualByComparingTo("0");
        List<LoanSchedule.Line> bullet = schedule("BULLET", "NONE", 0);
        assertThat(bullet.get(0).principal()).isEqualByComparingTo("0");
        assertThat(bullet.get(0).interest()).isEqualByComparingTo("10000");
        assertThat(bullet.get(11).principal()).isEqualByComparingTo("1000000");
    }

    @Test
    @DisplayName("Differe : en capital, les interets sont payes ; total, ils s'ajoutent au capital")
    void deferrals() {
        List<LoanSchedule.Line> capital = schedule("ANNUITY", "CAPITAL", 2);
        assertThat(capital.get(0).principal()).isEqualByComparingTo("0");
        assertThat(capital.get(1).closingBalance()).isEqualByComparingTo("1000000");
        assertThat(capital.get(11).closingBalance()).isEqualByComparingTo("0");
        List<LoanSchedule.Line> total = schedule("ANNUITY", "TOTAL", 2);
        assertThat(total.get(0).payment()).isEqualByComparingTo("1000");
        assertThat(total.get(1).closingBalance()).isEqualByComparingTo("1020100");
        assertThat(total.get(11).closingBalance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Duree restante pour une echeance donnee (remboursement anticipe a duree reduite)")
    void termsFor() {
        assertThat(LoanSchedule.termsFor(new BigDecimal("1000000"), new BigDecimal("12"), 1, new BigDecimal("88849"))).isEqualTo(12);
        assertThat(LoanSchedule.termsFor(new BigDecimal("500000"), new BigDecimal("12"), 1, new BigDecimal("88849"))).isEqualTo(6);
    }
}
