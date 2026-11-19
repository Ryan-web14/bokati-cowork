package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.core.exception.customs.ConflictException;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.model.BookingCompanyTerms;
import com.sni.bokaticowork.features.booking.model.BookingMemberBudget;
import com.sni.bokaticowork.features.booking.repository.BookingCompanyTermsRepository;
import com.sni.bokaticowork.features.booking.repository.BookingMemberBudgetRepository;
import com.sni.bokaticowork.features.booking.service.support.BookingCompanyAccounts;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Lot B9e : forfait d'heures, budgets et releve mensuel des entreprises. */
class BookingB9eTest {

    private final BookingCompanyTermsRepository terms = mock(BookingCompanyTermsRepository.class);
    private final BookingMemberBudgetRepository budgets = mock(BookingMemberBudgetRepository.class);
    private final JdbcTemplate jdbc = mock(JdbcTemplate.class);

    @SuppressWarnings("unchecked")
    private BookingCompanyAccounts accounts() {
        return new BookingCompanyAccounts(terms, budgets, jdbc, mock(ObjectProvider.class), mock(ObjectProvider.class));
    }

    private static Booking booking(String amount) {
        return Booking.builder().bookingNumber("BKG-1").billingCompanyCode("ENT-1").ownerType(SubscriberType.MEMBER).ownerCode("MBR-1")
                .startedAt(LocalDateTime.of(2026, 11, 20, 9, 0)).durationMinutes(240).totalAmount(new BigDecimal(amount)).currency("XAF").build();
    }

    @Test
    void theSharedHoursCoverPartOfTheBookingAndTheRestIsBilledProRata() {
        when(terms.findByBusinessCode("ENT-1")).thenReturn(Optional.of(BookingCompanyTerms.builder().businessCode("ENT-1").monthlyHours(10).build()));
        when(jdbc.queryForObject(contains("company_covered_minutes"), eq(Long.class), any(), any(), any())).thenReturn(540L);
        Booking booking = booking("40000");

        accounts().applyAtCreation(booking);

        assertThat(booking.getCompanyCoveredMinutes()).isEqualTo(60);
        assertThat(booking.getTotalAmount()).isEqualByComparingTo("30000");
        assertThat(booking.getMonthlyStatement()).isFalse();
    }

    @Test
    void theCompanyAndMemberBudgetsAreEnforced() {
        when(terms.findByBusinessCode("ENT-1")).thenReturn(Optional.of(BookingCompanyTerms.builder().businessCode("ENT-1")
                .monthlyBudget(new BigDecimal("100000")).monthlyStatement(true).build()));
        when(jdbc.queryForObject(contains("SUM(total_amount)"), eq(BigDecimal.class), any(), any(), any())).thenReturn(new BigDecimal("80000"));
        when(jdbc.queryForObject(contains("SUM(total_amount)"), eq(BigDecimal.class), any(), any(), any(), any())).thenReturn(new BigDecimal("40000"));

        assertThatThrownBy(() -> accounts().applyAtCreation(booking("30000"))).isInstanceOf(ConflictException.class)
                .hasMessageContaining("Budget mensuel de l'entreprise dépassé : 110000 pour 100000");

        Booking within = booking("15000");
        when(budgets.findByBusinessCodeAndMemberId("ENT-1", "MBR-1")).thenReturn(Optional.of(BookingMemberBudget.builder()
                .businessCode("ENT-1").memberId("MBR-1").monthlyBudget(new BigDecimal("50000")).build()));
        assertThatThrownBy(() -> accounts().applyAtCreation(within)).hasMessageContaining("Budget mensuel du collaborateur dépassé");

        when(budgets.findByBusinessCodeAndMemberId("ENT-1", "MBR-1")).thenReturn(Optional.empty());
        accounts().applyAtCreation(within);
        assertThat(within.getMonthlyStatement()).isTrue();
    }

    @Test
    void aBookingWithoutCompanyIsUntouched() {
        Booking own = booking("40000");
        own.setBillingCompanyCode(null);
        accounts().applyAtCreation(own);
        assertThat(own.getTotalAmount()).isEqualByComparingTo("40000");
        assertThat(own.getCompanyCoveredMinutes()).isNull();
    }
}
