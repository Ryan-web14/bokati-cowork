package com.sni.bokaticowork.features.company.sharing.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.sharing.dto.PassSharingDtos;
import com.sni.bokaticowork.features.subscription.repository.EntitlementGrantRepository;
import com.sni.bokaticowork.features.subscription.repository.EntitlementLedgerRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.PassStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementDefinition;
import com.sni.bokaticowork.features.subscription.subscription.model.EntitlementGrant;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassBeneficiaryRole;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassBeneficiary;
import com.sni.bokaticowork.features.subscription.subscription.pass.repository.PassBeneficiaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Les passes d'une organisation partages avec ses collaborateurs.
 *
 * <p>L'organisation detient le pass et ses droits. L'administration ou le gerant ouvre l'acces a un
 * collaborateur rattache ; celui-ci reserve alors sur le pass, le droit est decompte chez
 * l'organisation et il reste note comme beneficiaire. La fin du rattachement revoque ses acces.</p>
 *
 * <p>Les methodes recoivent l'organisation deja verifiee, comme {@link CompanySharingService}.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyPassSharingService {

    static final String MEMBER = "MEMBER";

    /** Un pass clos ne recoit plus de beneficiaire. */
    private static final Set<PassStatus> CLOSED = EnumSet.of(PassStatus.CONSUMED, PassStatus.EXPIRED, PassStatus.CANCELLED);

    private final PassRepository passRepository;
    private final PassBeneficiaryRepository beneficiaryRepository;
    private final EntitlementGrantRepository grantRepository;
    private final EntitlementLedgerRepository ledgerRepository;
    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final CustomerMemberLinkRepository linkRepository;
    private final BookingRepository bookingRepository;

    // ── Lecture ──────────────────────────────────────────────────────────────

    /** Les passes de l'organisation qui ne sont pas clos. */
    @Transactional(readOnly = true)
    public List<PassSharingDtos.PassSummary> passes(Customer company) {
        List<Pass> passes = new ArrayList<>();
        for (PassStatus status : PassStatus.values()) {
            if (!CLOSED.contains(status)) {
                passes.addAll(passRepository.findAllByOwnerTypeAndOwnerCodeAndStatus(
                        SubscriberType.CUSTOMER.name(), company.getCustomerId(), status.name()));
            }
        }
        passes.sort(Comparator.comparing(Pass::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return passes.stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public PassSharingDtos.PassView view(Customer company, String passNumber) {
        return view(company, pass(company, passNumber));
    }

    /** Les passes de ses organisations auxquels le membre a acces · ce qu'il peut prendre. */
    @Transactional(readOnly = true)
    public List<PassSharingDtos.MyPassAccess> myAccesses(Member member) {
        List<PassSharingDtos.MyPassAccess> accesses = new ArrayList<>();
        java.util.Set<String> linked = new java.util.HashSet<>();
        linkRepository.findActiveCompanyCodes(member.getMemberId()).forEach(code -> linked.add(code.toUpperCase()));
        Instant now = Instant.now();
        for (PassBeneficiary access : beneficiaryRepository.findOpenAccesses(MEMBER, member.getMemberId(), SubscriberType.CUSTOMER)) {
            Pass pass = access.getPass();
            if (pass.getOwnerCode() == null || !linked.contains(pass.getOwnerCode().toUpperCase())
                    || !CompanyPassLocator.usable(access, now)) {
                continue;
            }
            String companyName = customerRepository.findByCustomerId(pass.getOwnerCode())
                    .map(Customer::getCompanyName).orElse(pass.getOwnerCode());
            accesses.add(new PassSharingDtos.MyPassAccess(pass.getOwnerCode(), companyName, pass.getPassNumber(),
                    pass.getName(), access.getValidUntil() != null ? access.getValidUntil() : pass.getValidUntil(),
                    access.getMaxUsesForBeneficiary(), access.getUsedCount(), entitlements(pass)));
        }
        return accesses;
    }

    // ── Acces ────────────────────────────────────────────────────────────────

    public PassSharingDtos.PassView addBeneficiary(Customer company, String passNumber,
                                                   PassSharingDtos.AddBeneficiaryRequest request, String actor) {
        Pass pass = pass(company, passNumber);
        if (CLOSED.contains(pass.getStatus())) {
            throw new BadRequestException("Ce pass est clos (" + pass.getStatus() + ") : on n'y ouvre plus d'accès.");
        }
        Member member = memberRepository.findByMemberIdAndDeletedFalse(request.memberId().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable : " + request.memberId()));
        if (linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE).isEmpty()) {
            throw new BadRequestException(member.getDisplayName() + " n'est pas rattaché à " + company.getCompanyName()
                    + " : rattachez-le avant de lui ouvrir le pass.");
        }
        if (request.validFrom() != null && request.validUntil() != null && request.validUntil().isBefore(request.validFrom())) {
            throw new BadRequestException("La fin de l'accès précède son début.");
        }
        PassBeneficiary existing = beneficiaryRepository.findAllByPassId(pass.getId()).stream()
                .filter(b -> MEMBER.equalsIgnoreCase(b.getBeneficiaryType()) && member.getMemberId().equalsIgnoreCase(b.getBeneficiaryCode()))
                .findFirst().orElse(null);
        if (existing != null && existing.getRevokedAt() == null) {
            throw new BadRequestException(member.getDisplayName() + " a déjà accès à ce pass.");
        }
        // Un acces revoque est rouvert plutot que double · la personne garde une seule ligne par pass,
        // celle que lit le controle a l'entree.
        PassBeneficiary access = existing != null ? existing : PassBeneficiary.builder()
                .pass(pass).beneficiaryType(MEMBER).beneficiaryCode(member.getMemberId())
                .role(PassBeneficiaryRole.AUTHORISED_USER).usedCount(0).build();
        access.setMaxUsesForBeneficiary(request.maxUses());
        access.setValidFrom(request.validFrom());
        access.setValidUntil(request.validUntil());
        access.setAddedBy(actor);
        if (existing != null) {
            access.setAddedAt(Instant.now());
            access.setUsedCount(0);
            access.setRevokedAt(null);
            access.setRevokedBy(null);
            access.setRevokedReason(null);
        }
        beneficiaryRepository.save(access);
        return view(company, pass);
    }

    /**
     * Revoque l'acces · ses reservations a venir sur le pass sont signalees, pas annulees.
     *
     * @param accessId numero court de l'acces
     */
    public PassSharingDtos.RevokeResult revokeBeneficiary(Customer company, String passNumber, Long accessId,
                                                          String reason, String actor) {
        Pass pass = pass(company, passNumber);
        PassBeneficiary access = beneficiaryRepository.findByShortId(accessId)
                .filter(b -> b.getPass().getId().equals(pass.getId()) && b.getRevokedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Accès introuvable : " + accessId));
        return revoke(access, StringUtils.hasText(reason) ? reason.trim() : "Accès retiré", actor);
    }

    /** A la fin d'un rattachement · tous les acces du membre aux passes de l'organisation. */
    public List<String> revokeOnLinkEnd(Customer company, Member member, String reason, String actor) {
        List<String> future = new ArrayList<>();
        for (PassBeneficiary access : beneficiaryRepository.findOpenAccessesWithOwner(MEMBER, member.getMemberId(),
                SubscriberType.CUSTOMER, company.getCustomerId())) {
            future.addAll(revoke(access, reason, actor).futureBookings());
        }
        return future;
    }

    private PassSharingDtos.RevokeResult revoke(PassBeneficiary access, String reason, String actor) {
        access.setRevokedAt(Instant.now());
        access.setRevokedBy(actor);
        access.setRevokedReason(reason == null ? null : reason.length() > 255 ? reason.substring(0, 255) : reason);
        beneficiaryRepository.save(access);
        List<String> future = bookingRepository.findFutureBookingNumbersOnPass(access.getPass().getPassNumber(),
                access.getBeneficiaryCode(), LocalDateTime.now());
        String warning = null;
        if (!future.isEmpty()) {
            warning = future.size() + " réservation(s) à venir prise(s) sur ce pass : " + String.join(", ", future)
                    + ". Elles ne sont pas annulées ; à traiter par l'administration.";
            log.warn("Acces {} au pass {} revoque ({}) · reservations a venir a traiter : {}", access.getShortId(),
                    access.getPass().getPassNumber(), access.getBeneficiaryCode(), future);
        }
        return new PassSharingDtos.RevokeResult(access.getShortId(), access.getBeneficiaryCode(), future, warning);
    }

    // ── Interne ──────────────────────────────────────────────────────────────

    private PassSharingDtos.PassView view(Customer company, Pass pass) {
        Map<String, List<PassSharingDtos.BeneficiaryConsumption>> consumed = new LinkedHashMap<>();
        for (Object[] row : ledgerRepository.sumConsumedByBeneficiaryOnPass(pass.getId())) {
            String code = String.valueOf(row[1]).toUpperCase();
            consumed.computeIfAbsent(code, k -> new ArrayList<>())
                    .add(new PassSharingDtos.BeneficiaryConsumption(String.valueOf(row[2]), decimal(row[3])));
        }
        List<PassSharingDtos.BeneficiaryView> beneficiaries = new ArrayList<>();
        Instant now = Instant.now();
        for (PassBeneficiary access : beneficiaryRepository.findAllByPassId(pass.getId())) {
            String name = MEMBER.equalsIgnoreCase(access.getBeneficiaryType())
                    ? memberRepository.findByMemberIdAndDeletedFalse(access.getBeneficiaryCode())
                    .map(Member::getDisplayName).orElse(access.getBeneficiaryCode())
                    : access.getBeneficiaryCode();
            beneficiaries.add(new PassSharingDtos.BeneficiaryView(access.getShortId(), access.getBeneficiaryCode(), name,
                    access.usableAt(now), access.getMaxUsesForBeneficiary(), access.getUsedCount(), access.getValidFrom(),
                    access.getValidUntil(), access.getAddedAt(), access.getAddedBy(), access.getRevokedAt(),
                    access.getRevokedReason(),
                    consumed.getOrDefault(access.getBeneficiaryCode().toUpperCase(), List.of())));
        }
        beneficiaries.sort(Comparator.comparing((PassSharingDtos.BeneficiaryView b) -> !b.open())
                .thenComparing(PassSharingDtos.BeneficiaryView::memberName, Comparator.nullsLast(String::compareToIgnoreCase)));
        return new PassSharingDtos.PassView(company.getCustomerId(), summary(pass), entitlements(pass), beneficiaries);
    }

    private List<PassSharingDtos.PassEntitlement> entitlements(Pass pass) {
        List<PassSharingDtos.PassEntitlement> lines = new ArrayList<>();
        for (EntitlementGrant grant : grantRepository.findAllByPassId(pass.getId())) {
            EntitlementDefinition definition = grant.getEntitlementDefinition();
            lines.add(new PassSharingDtos.PassEntitlement(definition == null ? null : definition.getCode(),
                    definition == null ? null : definition.getName(),
                    definition == null || definition.getUnit() == null ? null : definition.getUnit().name(),
                    Boolean.TRUE.equals(grant.getUnlimited()), grant.getQuantityGranted(), grant.getQuantityRemaining()));
        }
        return lines;
    }

    private PassSharingDtos.PassSummary summary(Pass pass) {
        return new PassSharingDtos.PassSummary(pass.getPassNumber(), pass.getName(),
                pass.getPassType() == null ? null : pass.getPassType().name(),
                pass.getStatus() == null ? null : pass.getStatus().name(), pass.getValidFrom(), pass.getValidUntil(),
                pass.getMaxUses(), pass.getUsedCount(), beneficiaryRepository.countActive(pass.getId()));
    }

    /** Le pass, s'il est bien celui de l'organisation · 404 sinon. */
    private Pass pass(Customer company, String passNumber) {
        return passRepository.findByPassNumber(passNumber == null ? "" : passNumber.trim())
                .filter(p -> p.getOwnerType() == SubscriberType.CUSTOMER && company.getCustomerId().equalsIgnoreCase(p.getOwnerCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Pass introuvable pour cette organisation : " + passNumber));
    }

    private static BigDecimal decimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString());
    }
}
