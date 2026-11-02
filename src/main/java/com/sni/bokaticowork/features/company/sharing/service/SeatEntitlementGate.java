package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing;
import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing.Mode;
import com.sni.bokaticowork.features.company.sharing.model.SeatAllowance;
import com.sni.bokaticowork.features.company.sharing.model.SeatUsage;
import com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository;
import com.sni.bokaticowork.features.company.sharing.repository.EntitlementSharingRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatAllowanceRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.subscription.enums.EntitlementGrantStatus;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * La part, le plafond et le pot d'une place · ce que le solde de l'organisation ne sait pas dire.
 *
 * <p>Le solde du droit reste celui de l'organisation, decompte comme avant. Ce composant ajoute
 * la regle de repartition : il refuse ce qui depasserait la part nominative, le plafond de la place
 * ou le pot commun, et garde pour chaque reservation ce qui a ete pris sur la part et sur le pot.</p>
 *
 * <p>Les places d'un meme abonnement puisent dans le meme pot · l'abonnement est verrouille le
 * temps du calcul, comme le solde l'est par ses propres verrous.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeatEntitlementGate {

    private final CompanySeatRepository seatRepository;
    private final EntitlementSharingRepository sharingRepository;
    private final SeatAllowanceRepository allowanceRepository;
    private final SeatUsageRepository usageRepository;
    private final EntitlementGrantRepository grantRepository;

    /** Ce que la place a et a pris sur un droit, pour la periode en cours. */
    public record Position(Mode mode,
                           /** Part nominative · null hors QUOTA et QUOTA_AND_POOL. */
                           BigDecimal quota,
                           BigDecimal quotaUsed,
                           /** Plafond de la place · null = sans plafond. */
                           BigDecimal cap,
                           BigDecimal seatUsed,
                           /** Taille du pot commun · null = sans limite propre (le solde de l'organisation decide). */
                           BigDecimal poolSize,
                           BigDecimal poolUsed) {

        public BigDecimal quotaRemaining() {
            return quota == null ? null : quota.subtract(quotaUsed).max(BigDecimal.ZERO);
        }

        public BigDecimal poolRemaining() {
            return poolSize == null ? null : poolSize.subtract(poolUsed).max(BigDecimal.ZERO);
        }
    }

    public record Allocation(BigDecimal fromQuota, BigDecimal fromPool) {
    }

    /**
     * Ce qu'une demande prend sur la part et sur le pot · refus avec la raison sinon.
     */
    public static Allocation allocate(Position p, BigDecimal requested) {
        BigDecimal q = requested == null ? BigDecimal.ZERO : requested;
        if (p.cap() != null && p.seatUsed().add(q).compareTo(p.cap()) > 0) {
            throw new BadRequestException("Plafond de votre place atteint sur ce droit : "
                    + plain(p.seatUsed()) + " pris sur " + plain(p.cap()) + " pour la période.");
        }
        return switch (p.mode()) {
            case POOL -> new Allocation(BigDecimal.ZERO, q);
            case QUOTA -> {
                BigDecimal remaining = p.quotaRemaining() == null ? BigDecimal.ZERO : p.quotaRemaining();
                if (q.compareTo(remaining) > 0) {
                    throw new BadRequestException("Votre part sur ce droit est épuisée : il reste "
                            + plain(remaining) + " pour " + plain(q) + " demandés.");
                }
                yield new Allocation(q, BigDecimal.ZERO);
            }
            case QUOTA_AND_POOL -> {
                BigDecimal remaining = p.quotaRemaining() == null ? BigDecimal.ZERO : p.quotaRemaining();
                BigDecimal fromQuota = q.min(remaining);
                BigDecimal rest = q.subtract(fromQuota);
                if (rest.signum() > 0 && p.poolSize() != null && rest.compareTo(p.poolRemaining()) > 0) {
                    throw new BadRequestException("Votre part est épuisée et le pot commun de l'organisation ne suffit pas : il reste "
                            + plain(p.poolRemaining()) + " pour " + plain(rest) + " demandés.");
                }
                yield new Allocation(fromQuota, rest);
            }
        };
    }

    @Transactional(readOnly = true)
    public Position position(SubscriptionSeat seat, String entitlementCode) {
        Subscription subscription = seat.getSubscription();
        return position(subscription, seat, entitlementCode, periodStart(subscription));
    }

    /** Retient la part de la place pour une reservation · refuse si la regle ne le permet pas. */
    @Transactional
    public void reserve(Long seatId, String entitlementCode, String referenceType, String referenceId, BigDecimal quantity) {
        Optional<SeatUsage> existing = existing(seatId, entitlementCode, referenceType, referenceId);
        if (existing.isPresent() && existing.get().getStatus() != SeatUsage.Status.RELEASED) {
            return;
        }
        SubscriptionSeat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new BadRequestException("Place introuvable pour cette réservation."));
        Subscription subscription = seatRepository.lockSubscription(seat.getSubscription().getId()).orElseThrow();
        LocalDate period = periodStart(subscription);
        Allocation allocation = allocate(position(subscription, seat, entitlementCode, period), quantity);
        save(existing.orElse(null), seat, subscription, entitlementCode, period, referenceType, referenceId, quantity,
                allocation, SeatUsage.Status.RESERVED);
    }

    /**
     * Passe la retenue en consommation · ou l'enregistre si rien n'avait ete retenu.
     *
     * <p>A la cloture, la reservation a eu lieu : on ne refuse plus rien. Sans retenue, la part est
     * prise si elle le permet, sinon tout va au pot.</p>
     */
    @Transactional
    public void consume(Long seatId, String entitlementCode, String referenceType, String referenceId, BigDecimal quantity) {
        Optional<SeatUsage> existing = existing(seatId, entitlementCode, referenceType, referenceId);
        if (existing.isPresent() && existing.get().getStatus() == SeatUsage.Status.RESERVED) {
            existing.get().setStatus(SeatUsage.Status.CONSUMED);
            usageRepository.save(existing.get());
            return;
        }
        if (existing.isPresent() && existing.get().getStatus() == SeatUsage.Status.CONSUMED) {
            return;
        }
        SubscriptionSeat seat = seatRepository.findById(seatId).orElse(null);
        if (seat == null) {
            log.warn("Place {} introuvable a la cloture de {} {} · consommation non repartie", seatId, referenceType, referenceId);
            return;
        }
        Subscription subscription = seatRepository.lockSubscription(seat.getSubscription().getId()).orElseThrow();
        LocalDate period = periodStart(subscription);
        Allocation allocation;
        try {
            allocation = allocate(position(subscription, seat, entitlementCode, period), quantity);
        } catch (BadRequestException ex) {
            allocation = new Allocation(BigDecimal.ZERO, quantity);
        }
        save(existing.orElse(null), seat, subscription, entitlementCode, period, referenceType, referenceId, quantity,
                allocation, SeatUsage.Status.CONSUMED);
    }

    /** Rend a la place et au pot ce que la reservation retenait. */
    @Transactional
    public void release(Long seatId, String entitlementCode, String referenceType, String referenceId) {
        existing(seatId, entitlementCode, referenceType, referenceId)
                .filter(u -> u.getStatus() == SeatUsage.Status.RESERVED)
                .ifPresent(u -> {
                    u.setStatus(SeatUsage.Status.RELEASED);
                    usageRepository.save(u);
                });
    }

    // ── Calcul ───────────────────────────────────────────────────────────────

    Position position(Subscription subscription, SubscriptionSeat seat, String entitlementCode, LocalDate period) {
        EntitlementSharing rule = sharingRepository
                .findBySubscription_IdAndEntitlementCodeIgnoreCase(subscription.getId(), entitlementCode).orElse(null);
        Mode mode = rule == null ? Mode.POOL : rule.getMode();
        SeatAllowance allowance = allowanceRepository
                .findBySeat_IdAndEntitlementCodeIgnoreCase(seat.getId(), entitlementCode).orElse(null);
        BigDecimal quota = mode == Mode.POOL ? null : effectiveQuota(rule, allowance);
        BigDecimal cap = allowance != null && allowance.getCap() != null ? allowance.getCap()
                : rule == null ? null : rule.getSeatCap();

        BigDecimal poolSize = null;
        BigDecimal poolUsed = BigDecimal.ZERO;
        if (mode == Mode.QUOTA_AND_POOL) {
            Granted granted = granted(subscription, entitlementCode);
            if (!granted.unlimited()) {
                poolSize = granted.quantity().subtract(sumQuotas(subscription, rule, entitlementCode)).max(BigDecimal.ZERO);
            }
            poolUsed = usageRepository.sumPool(subscription.getId(), entitlementCode, period);
        }
        return new Position(mode, quota,
                usageRepository.sumSeatQuota(seat.getId(), entitlementCode, period), cap,
                usageRepository.sumSeatTotal(seat.getId(), entitlementCode, period), poolSize, poolUsed);
    }

    /** Ce que l'organisation a recu sur ce droit pour l'abonnement · grants en cours. */
    public Granted granted(Subscription subscription, String entitlementCode) {
        Instant now = Instant.now();
        BigDecimal quantity = BigDecimal.ZERO, remaining = BigDecimal.ZERO;
        boolean unlimited = false;
        for (EntitlementGrant grant : grantRepository.findAllBySubscription(subscription.getId())) {
            if (grant.getEntitlementDefinition() == null
                    || !entitlementCode.equalsIgnoreCase(grant.getEntitlementDefinition().getCode())
                    || grant.getStatus() == EntitlementGrantStatus.CANCELLED
                    || grant.getStatus() == EntitlementGrantStatus.EXPIRED
                    || (grant.getValidFrom() != null && grant.getValidFrom().isAfter(now))
                    || (grant.getValidUntil() != null && !grant.getValidUntil().isAfter(now))) {
                continue;
            }
            unlimited |= Boolean.TRUE.equals(grant.getUnlimited());
            quantity = quantity.add(zero(grant.getQuantityGranted()));
            remaining = remaining.add(zero(grant.getQuantityRemaining()));
        }
        return new Granted(quantity, remaining, unlimited);
    }

    public record Granted(BigDecimal quantity, BigDecimal remaining, boolean unlimited) {
    }

    /** La somme des parts nominatives des places actives · ce qui n'est pas dans le pot. */
    public BigDecimal sumQuotas(Subscription subscription, EntitlementSharing rule, String entitlementCode) {
        List<SubscriptionSeat> seats = seatRepository.findActiveBySubscription(subscription.getId());
        if (seats.isEmpty()) {
            return BigDecimal.ZERO;
        }
        Map<Long, SeatAllowance> allowances = allowanceRepository
                .findAllBySeat_IdIn(seats.stream().map(SubscriptionSeat::getId).toList()).stream()
                .filter(a -> entitlementCode.equalsIgnoreCase(a.getEntitlementCode()))
                .collect(Collectors.toMap(a -> a.getSeat().getId(), a -> a, (a, b) -> a));
        BigDecimal total = BigDecimal.ZERO;
        for (SubscriptionSeat seat : seats) {
            BigDecimal quota = effectiveQuota(rule, allowances.get(seat.getId()));
            total = total.add(zero(quota));
        }
        return total;
    }

    static BigDecimal effectiveQuota(EntitlementSharing rule, SeatAllowance allowance) {
        if (allowance != null && allowance.getQuota() != null) {
            return allowance.getQuota();
        }
        return rule == null ? null : rule.getDefaultQuota();
    }

    /** Le debut de la periode de l'abonnement · les parts et le pot repartent a chaque periode. */
    public static LocalDate periodStart(Subscription subscription) {
        if (subscription.getCurrentPeriodStart() != null) {
            return subscription.getCurrentPeriodStart();
        }
        return LocalDate.now().withDayOfMonth(1);
    }

    private Optional<SeatUsage> existing(Long seatId, String code, String referenceType, String referenceId) {
        return usageRepository.findBySeatIdAndEntitlementCodeAndReferenceTypeAndReferenceId(seatId, code, referenceType, referenceId);
    }

    private void save(SeatUsage usage, SubscriptionSeat seat, Subscription subscription, String code, LocalDate period,
                      String referenceType, String referenceId, BigDecimal quantity, Allocation allocation,
                      SeatUsage.Status status) {
        SeatUsage target = usage != null ? usage : SeatUsage.builder()
                .seatId(seat.getId())
                .subscriptionId(subscription.getId())
                .memberCode(seat.getMemberCode())
                .entitlementCode(code)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build();
        target.setPeriodStart(period);
        target.setQuantity(quantity);
        target.setFromQuota(allocation.fromQuota());
        target.setFromPool(allocation.fromPool());
        target.setStatus(status);
        usageRepository.save(target);
    }

    private static BigDecimal zero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
