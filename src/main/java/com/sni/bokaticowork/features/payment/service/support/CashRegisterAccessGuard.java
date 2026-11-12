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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Qui peut ouvrir une session sur quelle caisse, depuis quel poste et a quelle heure (lot C1).
 *
 * <ul>
 *   <li>La caisse doit etre active ; un coffre n'a pas de session.</li>
 *   <li>La personne doit avoir une affectation en vigueur (titulaire, remplacant ou interimaire),
 *       avec un role autre que lecture.</li>
 *   <li>Des qu'un poste est autorise sur la caisse, seuls les postes autorises ouvrent une
 *       session ; tant qu'aucun ne l'est, un poste inconnu est simplement propose.</li>
 *   <li>Hors des horaires d'ouverture, un motif choisi dans la liste est exige.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class CashRegisterAccessGuard {

    public static final ZoneId ZONE = ZoneId.of("Africa/Brazzaville");

    private final CashRegisterAssignmentRepository assignmentRepository;
    private final CashRegisterDeviceRepository deviceRepository;
    private final CashRegisterScheduleRepository scheduleRepository;
    private final CashReferenceLists referenceLists;
    private final com.sni.bokaticowork.features.payment.repository.CashStaffAbsenceRepository absenceRepository;

    /** Desactivable le temps de completer les affectations reprises ; actif par defaut. */
    @Value("${bokati.cash.assignment.enforced:true}")
    private boolean assignmentEnforced = true;

    private Clock clock = Clock.systemUTC();

    void useClock(Clock clock) {
        this.clock = clock;
    }

    void enforceAssignments(boolean enforced) {
        this.assignmentEnforced = enforced;
    }

    /** Resultat du controle : ce qu'il faut inscrire sur la session. */
    public record OpeningClearance(String deviceCode, String outOfHoursReason, CashRegisterAssignment assignment) {
    }

    public OpeningClearance assertCanOpen(CashRegister register, String actor, String deviceCode,
                                          String outOfHoursReasonCode, String outOfHoursReasonText) {
        if (register.getStatus() == CashRegisterStatus.SUSPENDED) {
            throw new BadRequestException("La caisse " + register.getRegisterCode() + " est suspendue"
                    + (StringUtils.hasText(register.getStatusReason()) ? " (" + register.getStatusReason() + ")" : "")
                    + " : aucune session ne peut y etre ouverte");
        }
        if (register.getStatus() == CashRegisterStatus.CLOSED || !Boolean.TRUE.equals(register.getActive())) {
            throw new BadRequestException("La caisse " + register.getRegisterCode() + " n'est pas active");
        }
        if (register.getRegisterType() == CashRegisterType.SAFE) {
            throw new BadRequestException("Un coffre n'a pas de session : ses mouvements sont des transferts internes");
        }
        Instant now = clock.instant();
        currentAbsence(actor, now).ifPresent(absence -> {
            throw new ForbiddenException("Vous etes declare absent (" + absence.getAbsenceNumber()
                    + ") : vous ne pouvez pas ouvrir de session pendant votre absence");
        });
        CashRegisterAssignment assignment = effectiveAssignment(register.getRegisterCode(), actor, now).orElse(null);
        if (assignmentEnforced && (assignment == null || assignment.getRole() == CashAssignmentRole.VIEWER)) {
            throw new ForbiddenException("Vous n'etes pas affecte a la caisse " + register.getRegisterCode()
                    + " : demandez une affectation a son responsable, ou une affectation d'urgence a un superviseur");
        }
        String device = checkDevice(register, actor, deviceCode);
        String outOfHours = checkSchedule(register, now, outOfHoursReasonCode, outOfHoursReasonText);
        return new OpeningClearance(device, outOfHours, assignment);
    }

    /**
     * L'affectation en vigueur de la personne sur la caisse ; la plus forte si elle en a
     * plusieurs (titulaire, puis interimaire, puis remplacant).
     */
    public Optional<CashRegisterAssignment> effectiveAssignment(String registerCode, String actor, Instant at) {
        if (!StringUtils.hasText(actor)) {
            return Optional.empty();
        }
        return assignmentRepository.findLiveByUser(actor).stream()
                .filter(assignment -> registerCode.equals(assignment.getRegisterCode()))
                .filter(assignment -> assignment.isEffectiveAt(at))
                .min(Comparator.comparingInt(CashRegisterAccessGuard::strength));
    }

    /** Absence validee ou en cours de la personne a cet instant. */
    public Optional<com.sni.bokaticowork.features.payment.model.CashStaffAbsence> currentAbsence(String actor, Instant at) {
        if (!StringUtils.hasText(actor)) {
            return Optional.empty();
        }
        java.time.LocalDate today = at.atZone(ZONE).toLocalDate();
        return absenceRepository.findByUser(actor).stream()
                .filter(absence -> absence.getStatus() == com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus.APPROVED
                        || absence.getStatus() == com.sni.bokaticowork.features.payment.enums.CashAbsenceStatus.ACTIVE)
                .filter(absence -> !absence.getStartsOn().isAfter(today)
                        && (absence.getEndsOn() == null || !absence.getEndsOn().isBefore(today)))
                .findFirst();
    }

    /** Les affectations en vigueur de la personne, toutes caisses confondues. */
    public List<CashRegisterAssignment> effectiveAssignments(String actor, Instant at) {
        if (!StringUtils.hasText(actor)) {
            return List.of();
        }
        return assignmentRepository.findLiveByUser(actor).stream()
                .filter(assignment -> assignment.isEffectiveAt(at))
                .sorted(Comparator.comparingInt(CashRegisterAccessGuard::strength))
                .toList();
    }

    static int strength(CashRegisterAssignment assignment) {
        int quality = switch (assignment.getQuality() == null ? CashAssignmentQuality.BACKUP : assignment.getQuality()) {
            case HOLDER -> 0;
            case INTERIM -> 1;
            case BACKUP -> 2;
        };
        int rank = assignment.getBackupRank() == null ? 99 : Math.min(assignment.getBackupRank(), 99);
        return quality * 100 + rank;
    }

    /** La caisse est-elle dans ses horaires a cet instant ? Vrai si elle n'en a pas. */
    public boolean withinSchedule(String registerCode, Instant at) {
        List<CashRegisterSchedule> schedules = scheduleRepository.findByRegisterCodeOrderByDayOfWeekAscOpensAtAsc(registerCode);
        if (schedules.isEmpty()) {
            return true;
        }
        ZonedDateTime local = at.atZone(ZONE);
        int day = local.getDayOfWeek().getValue();
        return schedules.stream()
                .filter(schedule -> schedule.getDayOfWeek() != null && schedule.getDayOfWeek() == day)
                .anyMatch(schedule -> !local.toLocalTime().isBefore(schedule.getOpensAt())
                        && local.toLocalTime().isBefore(schedule.getClosesAt()));
    }

    private String checkDevice(CashRegister register, String actor, String deviceCode) {
        String device = StringUtils.hasText(deviceCode) ? deviceCode.trim() : null;
        List<CashRegisterDevice> devices = deviceRepository.findByRegisterCodeOrderByFirstSeenAtAsc(register.getRegisterCode());
        boolean restricted = devices.stream().anyMatch(known -> known.getStatus() == CashDeviceStatus.ALLOWED);
        if (restricted) {
            if (device == null) {
                throw new ForbiddenException("La caisse " + register.getRegisterCode()
                        + " ne s'ouvre que depuis un poste autorise : ce poste n'est pas identifie");
            }
            boolean allowed = devices.stream().anyMatch(known -> known.getStatus() == CashDeviceStatus.ALLOWED
                    && known.getDeviceCode().equalsIgnoreCase(device));
            if (!allowed) {
                throw new ForbiddenException("Le poste " + device + " n'est pas autorise sur la caisse "
                        + register.getRegisterCode());
            }
            return device;
        }
        if (device != null && devices.stream().noneMatch(known -> known.getDeviceCode().equalsIgnoreCase(device))) {
            // Premier passage de ce poste : il est propose au responsable, sans rien bloquer.
            deviceRepository.save(CashRegisterDevice.builder()
                    .registerCode(register.getRegisterCode())
                    .deviceCode(device)
                    .status(CashDeviceStatus.PROPOSED)
                    .firstSeenBy(actor)
                    .build());
        }
        return device;
    }

    private String checkSchedule(CashRegister register, Instant now, String reasonCode, String reasonText) {
        if (withinSchedule(register.getRegisterCode(), now)) {
            return null;
        }
        if (!StringUtils.hasText(reasonCode)) {
            throw new BadRequestException("La caisse " + register.getRegisterCode()
                    + " est hors de ses horaires d'ouverture : indiquez un motif (evenement, inventaire, rattrapage...)");
        }
        return referenceLists.describe(CashReferenceLists.OUT_OF_HOURS_REASON, reasonCode, reasonText);
    }
}
