package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.sharing.dto.SharingDtos;
import com.sni.bokaticowork.features.company.sharing.model.EntitlementSharing;
import com.sni.bokaticowork.features.company.sharing.model.SeatAllowance;
import com.sni.bokaticowork.features.company.sharing.repository.CompanySeatRepository;
import com.sni.bokaticowork.features.company.sharing.repository.EntitlementSharingRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatAllowanceRepository;
import com.sni.bokaticowork.features.company.sharing.repository.SeatUsageRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatRole;
import com.sni.bokaticowork.features.subscription.seat.enums.SeatStatus;
import com.sni.bokaticowork.features.subscription.seat.model.SubscriptionSeat;
import com.sni.bokaticowork.features.subscription.seat.repository.SubscriptionSeatRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le partage d'un abonnement d'organisation · regles par droit, places des collaborateurs, et ce
 * que chacun a pris.
 *
 * <p>Les methodes recoivent l'organisation deja verifiee : par l'administration (code), ou par le
 * portail du gerant (lien OWNER actif). L'administration fixe le plafond de places vendu ; le
 * gerant regle la repartition, son nombre de places sous ce plafond, et attribue les places.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompanySharingService {

    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final CustomerMemberLinkRepository linkRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionSeatRepository legacySeatRepository;
    private final CompanySeatRepository seatRepository;
    private final EntitlementSharingRepository sharingRepository;
    private final SeatAllowanceRepository allowanceRepository;
    private final SeatUsageRepository usageRepository;
    private final EntitlementGrantRepository grantRepository;
    private final BookingRepository bookingRepository;
    private final SeatEntitlementGate gate;

    // ── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SharingDtos.SubscriptionSummary> subscriptions(Customer company) {
        return seatRepository.findOpenCompanySubscriptions(company.getCustomerId()).stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public SharingDtos.SharingView view(Customer company, String subscriptionNumber) {
        return view(company, subscription(company, subscriptionNumber));
    }

    /** Tous les abonnements ouverts de l'organisation · la consommation par collaborateur. */
    @Transactional(readOnly = true)
    public List<SharingDtos.SharingView> usage(Customer company) {
        return seatRepository.findOpenCompanySubscriptions(company.getCustomerId()).stream()
                .map(subscription -> view(company, subscription)).toList();
    }

    /** Les places du membre sur les abonnements de ses organisations · ce qu'il peut prendre. */
    @Transactional(readOnly = true)
    public List<SharingDtos.MySeat> mySeats(Member member) {
        List<SharingDtos.MySeat> seats = new ArrayList<>();
        java.util.Set<String> linked = new java.util.HashSet<>();
        linkRepository.findActiveCompanyCodes(member.getMemberId()).forEach(code -> linked.add(code.toUpperCase()));
        for (SubscriptionSeat seat : seatRepository.findActiveCompanySeatsOfMember(member.getMemberId())) {
            Subscription subscription = seat.getSubscription();
            if (!linked.contains(subscription.getSubscriberCode().toUpperCase()) || !subscription.getStatus().entitled()) {
                continue;
            }
            String companyName = customerRepository.findByCustomerId(subscription.getSubscriberCode())
                    .map(Customer::getCompanyName).orElse(subscription.getSubscriberCode());
            List<SharingDtos.MySeatEntitlement> entitlements = new ArrayList<>();
            for (Map.Entry<String, EntitlementDefinition> entry : definitions(subscription).entrySet()) {
                String code = entry.getKey();
                SeatEntitlementGate.Position position = gate.position(seat, code);
                SeatEntitlementGate.Granted granted = gate.granted(subscription, code);
                entitlements.add(new SharingDtos.MySeatEntitlement(code, entry.getValue().getName(),
                        entry.getValue().getUnit() == null ? null : entry.getValue().getUnit().name(), position.mode(),
                        position.quota(), position.quotaRemaining(), position.cap(), position.seatUsed(),
                        granted.unlimited() ? null : granted.remaining(), position.poolRemaining(), granted.unlimited()));
            }
            seats.add(new SharingDtos.MySeat(subscription.getSubscriberCode(), companyName, subscription.getSubscriptionNumber(),
                    seat.getShortId(), subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), entitlements));
        }
        return seats;
    }

    // ── Regles (administration et gerant) ────────────────────────────────────

    public SharingDtos.SharingView setRule(Customer company, String subscriptionNumber, String entitlementCode,
                                           SharingDtos.RuleRequest request, String actor) {
        Subscription subscription = subscription(company, subscriptionNumber);
        String code = entitlementCode(subscription, entitlementCode);
        EntitlementSharing rule = sharingRepository.findBySubscription_IdAndEntitlementCodeIgnoreCase(subscription.getId(), code)
                .orElseGet(() -> EntitlementSharing.builder().subscription(subscription).entitlementCode(code).build());
        rule.setMode(request.mode());
        rule.setDefaultQuota(request.mode() == EntitlementSharing.Mode.POOL ? null : request.defaultQuota());
        rule.setSeatCap(request.seatCap());
        rule.setUpdatedBy(actor);
        sharingRepository.save(rule);
        ensureQuotasFit(subscription, rule, code);
        return view(company, subscription);
    }

    /** L'administration · plafond vendu et nombre de places ensemble. */
    public SharingDtos.SharingView setMaxSeats(Customer company, String subscriptionNumber, Integer maxSeats) {
        Subscription subscription = seatRepository.lockSubscription(subscription(company, subscriptionNumber).getId()).orElseThrow();
        ensureSeatsFit(subscription, maxSeats);
        subscription.setSeatCeiling(maxSeats);
        subscription.setMaxSeats(maxSeats);
        subscriptionRepository.save(subscription);
        return view(company, subscription);
    }

    /** Le gerant · son nombre de places, sans depasser le plafond vendu. Vide = le plafond. */
    public SharingDtos.SharingView setMaxSeatsByOwner(Customer company, String subscriptionNumber, Integer maxSeats) {
        Subscription subscription = seatRepository.lockSubscription(subscription(company, subscriptionNumber).getId()).orElseThrow();
        Integer ceiling = subscription.getSeatCeiling();
        Integer wanted = maxSeats == null ? ceiling : maxSeats;
        if (ceiling != null && wanted > ceiling) {
            throw new BadRequestException("L'abonnement compte " + ceiling + " places au plus : pour en avoir davantage,"
                    + " contactez l'administration.");
        }
        ensureSeatsFit(subscription, wanted);
        subscription.setMaxSeats(wanted);
        subscriptionRepository.save(subscription);
        return view(company, subscription);
    }

    private void ensureSeatsFit(Subscription subscription, Integer maxSeats) {
        long active = seatRepository.countActive(subscription.getId());
        if (maxSeats != null && maxSeats < active) {
            throw new BadRequestException(active + " places sont occupées : libérez-en avant de descendre à " + maxSeats + ".");
        }
    }

    // ── Places (administration et gerant) ────────────────────────────────────

    public SharingDtos.SharingView assignSeat(Customer company, String subscriptionNumber,
                                              SharingDtos.AssignSeatRequest request, String actor) {
        Subscription subscription = seatRepository.lockSubscription(subscription(company, subscriptionNumber).getId()).orElseThrow();
        if (!subscription.getStatus().open()) {
            throw new BadRequestException("Cet abonnement est clos : on n'y attribue plus de place.");
        }
        Member member = memberRepository.findByMemberIdAndDeletedFalse(request.memberId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable : " + request.memberId()));
        if (linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE).isEmpty()) {
            throw new BadRequestException(member.getDisplayName() + " n'est pas rattaché à " + company.getCompanyName()
                    + " : rattachez-le avant de lui donner une place.");
        }
        if (legacySeatRepository.findActiveSeat(subscription.getId(), member.getMemberId()).isPresent()) {
            throw new BadRequestException(member.getDisplayName() + " a déjà une place sur cet abonnement.");
        }
        if (subscription.getMaxSeats() != null && seatRepository.countActive(subscription.getId()) >= subscription.getMaxSeats()) {
            throw new BadRequestException("Toutes les places de l'abonnement sont occupées (" + subscription.getMaxSeats() + ").");
        }
        Instant now = Instant.now();
        SubscriptionSeat seat = seatRepository.save(SubscriptionSeat.builder()
                .subscription(subscription)
                .member(member)
                .memberCode(member.getMemberId())
                .role(SeatRole.MEMBER)
                .status(SeatStatus.ACTIVE)
                .invitedAt(now)
                .activatedAt(now)
                .assignedBy(actor)
                .build());
        applyAllowances(subscription, seat, request.allowances());
        return view(company, subscription);
    }

    public SharingDtos.SharingView updateSeat(Customer company, String subscriptionNumber, Long seatId,
                                              SharingDtos.UpdateSeatRequest request) {
        Subscription subscription = seatRepository.lockSubscription(subscription(company, subscriptionNumber).getId()).orElseThrow();
        SubscriptionSeat seat = activeSeat(subscription, seatId);
        applyAllowances(subscription, seat, request.allowances());
        return view(company, subscription);
    }

    /**
     * Libere la place · ce qui reste de sa part est perdu, ce qu'elle n'a pas pris dans le pot y reste.
     * Les reservations a venir sur ce droit sont signalees, pas annulees.
     */
    public SharingDtos.ReleaseResult releaseSeat(Customer company, String subscriptionNumber, Long seatId, String reason) {
        Subscription subscription = subscription(company, subscriptionNumber);
        SubscriptionSeat seat = activeSeat(subscription, seatId);
        return release(seat, StringUtils.hasText(reason) ? reason.trim() : "Place libérée");
    }

    /** A la fin d'un rattachement · toutes les places du membre sur les abonnements de l'organisation. */
    public List<String> releaseSeatsOnLinkEnd(Customer company, Member member, String reason) {
        List<String> future = new ArrayList<>();
        for (SubscriptionSeat seat : seatRepository.findActiveSeatsOfMemberInCompany(member.getMemberId(), company.getCustomerId())) {
            future.addAll(release(seat, reason).futureBookings());
        }
        return future;
    }

    private SharingDtos.ReleaseResult release(SubscriptionSeat seat, String reason) {
        seat.setStatus(SeatStatus.REMOVED);
        seat.setRemovedAt(Instant.now());
        seat.setRemovedReason(reason);
        seatRepository.save(seat);
        List<String> future = bookingRepository.findFutureBookingNumbersOnSeats(List.of(seat.getId()), LocalDateTime.now());
        String warning = null;
        if (!future.isEmpty()) {
            warning = future.size() + " réservation(s) à venir prise(s) sur cette place : " + String.join(", ", future)
                    + ". Elles ne sont pas annulées ; à traiter par l'administration.";
            log.warn("Place {} liberee ({}) · reservations a venir a traiter : {}", seat.getShortId(), seat.getMemberCode(), future);
        }
        return new SharingDtos.ReleaseResult(seat.getShortId(), seat.getMemberCode(), future, warning);
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private void applyAllowances(Subscription subscription, SubscriptionSeat seat, List<SharingDtos.AllowanceRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }
        for (SharingDtos.AllowanceRequest request : requests) {
            String code = entitlementCode(subscription, request.entitlementCode());
            SeatAllowance allowance = allowanceRepository.findBySeat_IdAndEntitlementCodeIgnoreCase(seat.getId(), code)
                    .orElseGet(() -> SeatAllowance.builder().seat(seat).entitlementCode(code).build());
            allowance.setQuota(request.quota());
            allowance.setCap(request.cap());
            allowanceRepository.save(allowance);
            EntitlementSharing rule = sharingRepository
                    .findBySubscription_IdAndEntitlementCodeIgnoreCase(subscription.getId(), code).orElse(null);
            ensureQuotasFit(subscription, rule, code);
        }
    }

    /** Les parts nominatives ne distribuent pas plus que l'organisation n'a recu. */
    private void ensureQuotasFit(Subscription subscription, EntitlementSharing rule, String code) {
        if (rule == null || rule.getMode() == EntitlementSharing.Mode.POOL) {
            return;
        }
        SeatEntitlementGate.Granted granted = gate.granted(subscription, code);
        if (granted.unlimited()) {
            return;
        }
        BigDecimal distributed = gate.sumQuotas(subscription, rule, code);
        if (distributed.compareTo(granted.quantity()) > 0) {
            throw new BadRequestException("Les parts distribuées sur " + code + " (" + plain(distributed)
                    + ") dépassent ce que l'organisation a reçu pour la période (" + plain(granted.quantity()) + ").");
        }
    }

    private SharingDtos.SharingView view(Customer company, Subscription subscription) {
        LocalDate period = SeatEntitlementGate.periodStart(subscription);
        Map<String, EntitlementDefinition> definitions = definitions(subscription);
        List<SharingDtos.EntitlementView> entitlements = new ArrayList<>();
        for (Map.Entry<String, EntitlementDefinition> entry : definitions.entrySet()) {
            String code = entry.getKey();
            EntitlementSharing rule = sharingRepository.findBySubscription_IdAndEntitlementCodeIgnoreCase(subscription.getId(), code).orElse(null);
            EntitlementSharing.Mode mode = rule == null ? EntitlementSharing.Mode.POOL : rule.getMode();
            SeatEntitlementGate.Granted granted = gate.granted(subscription, code);
            BigDecimal distributed = mode == EntitlementSharing.Mode.POOL ? BigDecimal.ZERO : gate.sumQuotas(subscription, rule, code);
            BigDecimal poolSize = mode == EntitlementSharing.Mode.QUOTA_AND_POOL && !granted.unlimited()
                    ? granted.quantity().subtract(distributed).max(BigDecimal.ZERO) : null;
            BigDecimal poolUsed = mode == EntitlementSharing.Mode.QUOTA_AND_POOL
                    ? usageRepository.sumPool(subscription.getId(), code, period) : null;
            entitlements.add(new SharingDtos.EntitlementView(code, entry.getValue().getName(),
                    entry.getValue().getUnit() == null ? null : entry.getValue().getUnit().name(), mode,
                    rule == null ? null : rule.getDefaultQuota(), rule == null ? null : rule.getSeatCap(),
                    granted.unlimited(), granted.quantity(), granted.remaining(), distributed, poolSize, poolUsed));
        }
        List<SharingDtos.SeatView> seats = new ArrayList<>();
        for (SubscriptionSeat seat : seatRepository.findActiveBySubscription(subscription.getId())) {
            List<SharingDtos.SeatEntitlement> lines = new ArrayList<>();
            for (String code : definitions.keySet()) {
                SeatEntitlementGate.Position position = gate.position(subscription, seat, code, period);
                lines.add(new SharingDtos.SeatEntitlement(code, position.quota(), position.quotaUsed(),
                        position.quotaRemaining(), position.cap(), position.seatUsed()));
            }
            Member member = seat.getMember();
            seats.add(new SharingDtos.SeatView(seat.getShortId(), seat.getMemberCode(),
                    member == null ? seat.getMemberCode() : member.getDisplayName(), seat.getStatus().name(),
                    seat.getActivatedAt() != null ? seat.getActivatedAt() : seat.getCreatedAt(), seat.getAssignedBy(), lines));
        }
        return new SharingDtos.SharingView(company.getCustomerId(), summary(subscription), entitlements, seats);
    }

    /** Les droits de l'abonnement, par code · ceux qu'il a recus. */
    private Map<String, EntitlementDefinition> definitions(Subscription subscription) {
        Map<String, EntitlementDefinition> definitions = new LinkedHashMap<>();
        for (EntitlementGrant grant : grantRepository.findAllBySubscription(subscription.getId())) {
            EntitlementDefinition definition = grant.getEntitlementDefinition();
            if (definition != null && definition.getCode() != null) {
                definitions.putIfAbsent(definition.getCode(), definition);
            }
        }
        return definitions;
    }

    private String entitlementCode(Subscription subscription, String requested) {
        String wanted = requested == null ? "" : requested.trim();
        return definitions(subscription).keySet().stream().filter(code -> code.equalsIgnoreCase(wanted)).findFirst()
                .orElseThrow(() -> new BadRequestException("L'abonnement " + subscription.getSubscriptionNumber()
                        + " n'a pas de droit " + requested + "."));
    }

    private SharingDtos.SubscriptionSummary summary(Subscription subscription) {
        String planName = subscription.getPlanVersion() == null ? null
                : subscription.getPlanVersion().getPlan() != null ? subscription.getPlanVersion().getPlan().getName()
                : subscription.getPlanVersion().getName();
        return new SharingDtos.SubscriptionSummary(subscription.getSubscriptionNumber(), planName,
                subscription.getStatus() == null ? null : subscription.getStatus().name(),
                subscription.getCurrentPeriodStart(), subscription.getCurrentPeriodEnd(), subscription.getMaxSeats(),
                subscription.getSeatCeiling(), seatRepository.countActive(subscription.getId()));
    }

    /** L'abonnement, s'il est bien celui de l'organisation · 404 sinon. */
    private Subscription subscription(Customer company, String subscriptionNumber) {
        return subscriptionRepository.findBySubscriptionNumber(subscriptionNumber == null ? "" : subscriptionNumber.trim())
                .filter(s -> s.getSubscriberType() == SubscriberType.CUSTOMER
                        && company.getCustomerId().equalsIgnoreCase(s.getSubscriberCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Abonnement introuvable pour cette organisation : " + subscriptionNumber));
    }

    private SubscriptionSeat activeSeat(Subscription subscription, Long seatId) {
        return seatRepository.findByShortId(seatId)
                .filter(seat -> seat.getSubscription().getId().equals(subscription.getId()) && seat.getStatus() == SeatStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Place introuvable : " + seatId));
    }

    /** L'organisation par son code, pour l'administration. */
    @Transactional(readOnly = true)
    public Customer company(String customerId) {
        Customer company = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + customerId));
        if (company.getType() != CustomerType.COMPANY) {
            throw new BadRequestException("Le client " + customerId + " n'est pas une organisation.");
        }
        return company;
    }

    private static String plain(BigDecimal value) {
        return value == null ? "0" : value.stripTrailingZeros().toPlainString();
    }
}
