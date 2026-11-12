package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentQuality;
import com.sni.bokaticowork.features.payment.enums.CashAssignmentRole;
import com.sni.bokaticowork.features.payment.enums.CashDeviceStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterStatus;
import com.sni.bokaticowork.features.payment.enums.CashRegisterType;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashRegisterAssignment;
import com.sni.bokaticowork.features.payment.model.CashRegisterDevice;
import com.sni.bokaticowork.features.payment.model.CashRegisterSchedule;
import com.sni.bokaticowork.features.payment.repository.CashRegisterAssignmentRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterDeviceRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Lot C1 : qui ouvre une session, sur quelle caisse, depuis quel poste, a quelle heure. */
@ExtendWith(MockitoExtension.class)
class CashRegisterAccessGuardTest {

    /** Mercredi 11 novembre 2026, 10 h a Brazzaville (9 h UTC). */
    private static final Instant WEDNESDAY_10H = Instant.parse("2026-11-11T09:00:00Z");
    private static final String CASHIER = "caissier@elleaose.com";

    @Mock private CashRegisterAssignmentRepository assignmentRepository;
    @Mock private CashRegisterDeviceRepository deviceRepository;
    @Mock private CashRegisterScheduleRepository scheduleRepository;
    @Mock private CashReferenceLists referenceLists;
    @Mock private com.sni.bokaticowork.features.payment.repository.CashStaffAbsenceRepository absenceRepository;

    @InjectMocks private CashRegisterAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard.useClock(Clock.fixed(WEDNESDAY_10H, ZoneOffset.UTC));
        guard.enforceAssignments(true);
        lenient().when(deviceRepository.findByRegisterCodeOrderByFirstSeenAtAsc("CSR-1")).thenReturn(List.of());
        lenient().when(scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc("CSR-1")).thenReturn(List.of());
    }

    @Test
    @DisplayName("Une personne non affectee ne peut pas ouvrir de session")
    void unassignedPersonIsRefused() {
        when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of());

        assertThatThrownBy(() -> guard.assertCanOpen(register(), CASHIER, null, null, null))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("pas affecte");
    }

    @Test
    @DisplayName("Un role de lecture ne suffit pas, une affectation echue non plus")
    void viewerOrEndedAssignmentIsRefused() {
        when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of(
                assignment(CashAssignmentRole.VIEWER, CashAssignmentQuality.BACKUP, null, null),
                assignment(CashAssignmentRole.CASHIER, CashAssignmentQuality.INTERIM,
                        Instant.parse("2026-10-01T00:00:00Z"), Instant.parse("2026-10-31T00:00:00Z"))));

        assertThatThrownBy(() -> guard.assertCanOpen(register(), CASHIER, null, null, null))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Le titulaire ouvre ; son affectation est retenue")
    void holderMayOpen() {
        CashRegisterAssignment holder = assignment(CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null);
        when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of(
                assignment(CashAssignmentRole.CASHIER, CashAssignmentQuality.BACKUP, null, null), holder));

        CashRegisterAccessGuard.OpeningClearance clearance = guard.assertCanOpen(register(), CASHIER, null, null, null);

        assertThat(clearance.assignment()).isSameAs(holder);
        assertThat(clearance.outOfHoursReason()).isNull();
    }

    @Test
    @DisplayName("Une caisse suspendue ou un coffre refusent toute session")
    void suspendedRegisterAndSafeAreRefused() {
        CashRegister suspended = register();
        suspended.setStatus(CashRegisterStatus.SUSPENDED);
        suspended.setActive(false);
        assertThatThrownBy(() -> guard.assertCanOpen(suspended, CASHIER, null, null, null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("suspendue");

        CashRegister safe = register();
        safe.setRegisterType(CashRegisterType.SAFE);
        assertThatThrownBy(() -> guard.assertCanOpen(safe, CASHIER, null, null, null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("coffre");
    }

    @Test
    @DisplayName("Des qu'un poste est autorise, un autre poste est refuse")
    void onlyAllowedDevicesOnceOneIsAllowed() {
        when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of(
                assignment(CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null)));
        when(deviceRepository.findByRegisterCodeOrderByFirstSeenAtAsc("CSR-1")).thenReturn(List.of(
                CashRegisterDevice.builder().registerCode("CSR-1").deviceCode("POS-ACCUEIL").status(CashDeviceStatus.ALLOWED).build()));

        assertThatThrownBy(() -> guard.assertCanOpen(register(), CASHIER, "PC-PERSO", null, null))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("PC-PERSO");
        assertThat(guard.assertCanOpen(register(), CASHIER, "pos-accueil", null, null).deviceCode()).isEqualTo("pos-accueil");
    }

    @Test
    @DisplayName("Sans poste autorise, un poste inconnu est propose sans rien bloquer")
    void unknownDeviceIsProposed() {
        when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of(
                assignment(CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null)));

        guard.assertCanOpen(register(), CASHIER, "POS-NOUVEAU", null, null);

        ArgumentCaptor<CashRegisterDevice> saved = ArgumentCaptor.forClass(CashRegisterDevice.class);
        verify(deviceRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(CashDeviceStatus.PROPOSED);
        assertThat(saved.getValue().getFirstSeenBy()).isEqualTo(CASHIER);
    }

    @Test
    @DisplayName("Hors horaires, un motif de la liste est exige")
    void outOfHoursNeedsAReason() {
        when(assignmentRepository.findLiveByUser(CASHIER)).thenReturn(List.of(
                assignment(CashAssignmentRole.CASHIER, CashAssignmentQuality.HOLDER, null, null)));
        // Mercredi (3) de 14 h a 18 h seulement : 10 h est hors horaires.
        when(scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc("CSR-1")).thenReturn(List.of(
                CashRegisterSchedule.builder().registerCode("CSR-1").dayOfWeek(3)
                        .opensAt(LocalTime.of(14, 0)).closesAt(LocalTime.of(18, 0)).build()));
        when(referenceLists.describe(CashReferenceLists.OUT_OF_HOURS_REASON, "EVENT", null)).thenReturn("Evenement");

        assertThatThrownBy(() -> guard.assertCanOpen(register(), CASHIER, null, null, null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("horaires");
        assertThat(guard.assertCanOpen(register(), CASHIER, null, "EVENT", null).outOfHoursReason()).isEqualTo("Evenement");
    }

    @Test
    @DisplayName("Dans les horaires, aucun motif n'est demande")
    void withinScheduleNeedsNothing() {
        when(scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc("CSR-1")).thenReturn(List.of(
                CashRegisterSchedule.builder().registerCode("CSR-1").dayOfWeek(3)
                        .opensAt(LocalTime.of(8, 0)).closesAt(LocalTime.of(18, 0)).build()));

        assertThat(guard.withinSchedule("CSR-1", WEDNESDAY_10H)).isTrue();
        assertThat(guard.withinSchedule("CSR-1", Instant.parse("2026-11-12T09:00:00Z"))).as("jeudi").isFalse();
    }

    @Test
    @DisplayName("Une personne absente ne peut pas ouvrir de session, meme affectee")
    void absentPersonIsRefused() {
        when(absenceRepository.findByUser(CASHIER)).thenReturn(List.of(
                com.sni.bokaticowork.features.payment.model.CashStaffAbsence.builder().absenceNumber("ABS-1").userEmail(CASHIER)
                        .kindCode("ANNUAL_LEAVE").status(com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus.ACTIVE)
                        .startsOn(java.time.LocalDate.of(2026, 11, 9)).endsOn(java.time.LocalDate.of(2026, 11, 20)).build()));

        assertThatThrownBy(() -> guard.assertCanOpen(register(), CASHIER, null, null, null))
                .isInstanceOf(ForbiddenException.class).hasMessageContaining("absent");
    }

    private CashRegister register() {
        return CashRegister.builder()
                .registerCode("CSR-1")
                .name("Accueil")
                .registerType(CashRegisterType.COUNTER)
                .status(CashRegisterStatus.ACTIVE)
                .active(true)
                .build();
    }

    private CashRegisterAssignment assignment(CashAssignmentRole role, CashAssignmentQuality quality, Instant from, Instant to) {
        return CashRegisterAssignment.builder()
                .registerCode("CSR-1")
                .userEmail(CASHIER)
                .role(role)
                .quality(quality)
                .validFrom(from == null ? Instant.parse("2026-01-01T00:00:00Z") : from)
                .validTo(to)
                .build();
    }
}
