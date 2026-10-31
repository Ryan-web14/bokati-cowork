package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing.Mode;
import com.sni.bokaticowork.features.company.sharing.model.SeatUsage;
import com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository;
import com.sni.bokaticowork.features.company.sharing.repository.EntitlementSharingRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatAllowanceRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La part, le plafond et le pot d'une place.
 */
class SeatEntitlementGateTest {

    private static BigDecimal n(String v) {
        return new BigDecimal(v);
    }

    private static SeatEntitlementGate.Position position(Mode mode, String quota, String quotaUsed, String cap, String seatUsed,
                                                         String poolSize, String poolUsed) {
        return new SeatEntitlementGate.Position(mode, quota == null ? null : n(quota), n(quotaUsed),
                cap == null ? null : n(cap), n(seatUsed), poolSize == null ? null : n(poolSize), n(poolUsed));
    }

    @Test
    @DisplayName("POOL : tout va au pot, sous le plafond de la place")
    void poolUnderCap() {
        var allocation = SeatEntitlementGate.allocate(position(Mode.POOL, null, "0", "10", "6", null, "0"), n("4"));
        assertThat(allocation.fromPool()).isEqualByComparingTo("4");
        assertThatThrownBy(() -> SeatEntitlementGate.allocate(position(Mode.POOL, null, "0", "10", "7", null, "0"), n("4")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("Plafond");
    }

    @Test
    @DisplayName("QUOTA : la part seulement, rien au-dela")
    void quotaOnly() {
        assertThat(SeatEntitlementGate.allocate(position(Mode.QUOTA, "5", "3", null, "3", null, "0"), n("2")).fromQuota())
                .isEqualByComparingTo("2");
        assertThatThrownBy(() -> SeatEntitlementGate.allocate(position(Mode.QUOTA, "5", "4", null, "4", null, "0"), n("2")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("part");
    }

    @Test
    @DisplayName("QUOTA_AND_POOL : la part d'abord, le reste au pot")
    void quotaThenPool() {
        var allocation = SeatEntitlementGate.allocate(position(Mode.QUOTA_AND_POOL, "5", "4", null, "4", "20", "15"), n("3"));
        assertThat(allocation.fromQuota()).isEqualByComparingTo("1");
        assertThat(allocation.fromPool()).isEqualByComparingTo("2");
    }

    @Test
    @DisplayName("QUOTA_AND_POOL : pot insuffisant, refus")
    void poolExhausted() {
        assertThatThrownBy(() -> SeatEntitlementGate.allocate(
                position(Mode.QUOTA_AND_POOL, "5", "5", null, "5", "20", "19"), n("2")))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("pot commun");
    }

    @Test
    @DisplayName("Une retenue deja posee n'est pas reposee · rien n'est recalcule")
    void reserveIsIdempotent() {
        SeatUsageRepository usages = mock(SeatUsageRepository.class);
        CompanySeatRepository seats = mock(CompanySeatRepository.class);
        SeatEntitlementGate gate = new SeatEntitlementGate(seats, mock(EntitlementSharingRepository.class),
                mock(SeatAllowanceRepository.class), usages, mock(EntitlementGrantRepository.class));
        SeatUsage reserved = SeatUsage.builder().status(SeatUsage.Status.RESERVED).build();
        when(usages.findBySeatIdAndEntitlementCodeAndReferenceTypeAndReferenceId(1L, "SALLE_H", "BOOKING", "BKG-1"))
                .thenReturn(Optional.of(reserved));

        gate.reserve(1L, "SALLE_H", "BOOKING", "BKG-1", n("2"));
        gate.consume(1L, "SALLE_H", "BOOKING", "BKG-1", n("2"));

        assertThat(reserved.getStatus()).isEqualTo(SeatUsage.Status.CONSUMED);
        verify(seats, never()).lockSubscription(any());
    }

    @Test
    @DisplayName("Liberer rend la retenue · une consommation ne se libere pas")
    void releaseOnlyReserved() {
        SeatUsageRepository usages = mock(SeatUsageRepository.class);
        SeatEntitlementGate gate = new SeatEntitlementGate(mock(CompanySeatRepository.class), mock(EntitlementSharingRepository.class),
                mock(SeatAllowanceRepository.class), usages, mock(EntitlementGrantRepository.class));
        SeatUsage consumed = SeatUsage.builder().status(SeatUsage.Status.CONSUMED).build();
        when(usages.findBySeatIdAndEntitlementCodeAndReferenceTypeAndReferenceId(1L, "SALLE_H", "BOOKING", "BKG-1"))
                .thenReturn(Optional.of(consumed));

        gate.release(1L, "SALLE_H", "BOOKING", "BKG-1");

        assertThat(consumed.getStatus()).isEqualTo(SeatUsage.Status.CONSUMED);
    }
}
