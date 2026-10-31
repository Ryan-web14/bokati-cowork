package com.sni.bokaticowork.features.company.membership.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.dto.MembershipDtos;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

/**
 * Le rattachement des membres aux organisations · qui est proprietaire, qui est employe, et depuis quand.
 *
 * <p>Un membre peut etre rattache a plusieurs organisations, une fois chacune. Ce lien ne remplace
 * pas le payeur par defaut du membre ({@code member.customer_id}) : il dit ce que chacun peut voir
 * et faire au nom de l'organisation, et comment ses factures s'y relient.</p>
 *
 * <p>Une regle traverse tout le service : un membre n'a qu'un lien en mode {@code AUTOMATIC} a la
 * fois, mode propre au lien ou herite de l'organisation. Sinon une facture automatique ne saurait
 * pas a quelle organisation se rattacher.</p>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyMembershipService {

    private final CustomerMemberLinkRepository linkRepository;
    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final CustomerService customerService;
    private final com.sni.bokaticowork.features.company.sharing.service.CompanySharingService sharingService;
    private final com.sni.bokaticowork.features.company.sharing.service.CompanyPassSharingService passSharingService;
    private final com.sni.bokaticowork.features.company.wallet.service.CompanyWalletService companyWalletService;

    // ── Administration ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MembershipDtos.LinkResponse> list(String customerId, String status) {
        Customer company = company(customerId);
        List<CustomerMemberLink> links = StringUtils.hasText(status) && "ALL".equalsIgnoreCase(status.trim())
                ? linkRepository.findAllByCustomerOrderByStartedAtDesc(company)
                : linkRepository.findAllByCustomerAndStatusOrderByStartedAtDesc(company, parseStatus(status));
        return links.stream().map(link -> toResponse(link, null, null)).toList();
    }

    public MembershipDtos.LinkResponse link(String customerId, MembershipDtos.LinkMemberRequest request, String actor) {
        Customer company = company(customerId);
        Member member = member(request.memberId());
        linkRepository.findByCustomerAndMemberAndStatus(company, member, MemberLinkStatus.ACTIVE).ifPresent(existing -> {
            throw new BadRequestException(member.getDisplayName() + " est déjà rattaché à " + company.getCompanyName()
                    + " comme " + label(existing.getRole()) + ".");
        });
        Instant startedAt = request.startedAt() == null ? Instant.now() : request.startedAt();
        if (startedAt.isAfter(Instant.now().plusSeconds(60))) {
            throw new BadRequestException("Un rattachement ne commence pas dans le futur.");
        }
        CustomerMemberLink link = CustomerMemberLink.builder()
                .customer(company)
                .member(member)
                .role(request.role())
                .status(MemberLinkStatus.ACTIVE)
                .startedAt(startedAt)
                .invoiceLinkMode(request.invoiceLinkMode())
                .canSign(Boolean.TRUE.equals(request.canSign()))
                .canCollectMail(Boolean.TRUE.equals(request.canCollectMail()))
                .createdBy(actor)
                .build();
        List<CustomerMemberLink> active = new java.util.ArrayList<>(activeLinks(member));
        active.add(link);
        ensureSingleAutomatic(member, active);
        linkRepository.save(link);
        return toResponse(link, null, null);
    }

    public MembershipDtos.LinkResponse update(String customerId, Long linkId, MembershipDtos.UpdateLinkRequest request) {
        CustomerMemberLink link = activeLink(company(customerId), linkId);
        // Charge avant de modifier · une requete apres coup viderait la modification en base, et
        // l'index des liens automatiques refuserait avant que la regle ne s'explique.
        List<CustomerMemberLink> active = activeLinks(link.getMember());
        if (request.role() != null) link.setRole(request.role());
        if (Boolean.TRUE.equals(request.useCompanyDefaultMode())) {
            link.setInvoiceLinkMode(null);
        } else if (request.invoiceLinkMode() != null) {
            link.setInvoiceLinkMode(request.invoiceLinkMode());
        }
        if (request.canSign() != null) link.setCanSign(request.canSign());
        if (request.canCollectMail() != null) link.setCanCollectMail(request.canCollectMail());
        ensureSingleAutomatic(link.getMember(), active);
        linkRepository.save(link);
        return toResponse(link, null, null);
    }

    /**
     * Met fin au lien · il reste en historique avec la date et le motif.
     *
     * <p>Si l'organisation quittee etait aussi le payeur par defaut du membre, il repasse sur sa
     * fiche personnelle, creee au besoin : sans cela ses prochaines factures iraient encore a une
     * organisation qu'il a quittee.</p>
     */
    public MembershipDtos.LinkResponse end(String customerId, Long linkId, String reason, String actor) {
        Customer company = company(customerId);
        CustomerMemberLink link = activeLink(company, linkId);
        link.setStatus(MemberLinkStatus.ENDED);
        link.setEndedAt(Instant.now());
        link.setEndedReason(reason.trim());
        linkRepository.save(link);

        Member member = link.getMember();
        // Ses places sur les abonnements de l'organisation se ferment avec le lien · ses reservations
        // a venir sur ces places sont signalees, pas annulees.
        // Ses acces aux passes de l'organisation sont revoques de meme.
        List<String> future = new java.util.ArrayList<>(
                sharingService.releaseSeatsOnLinkEnd(company, member, "Fin du rattachement : " + reason.trim()));
        future.addAll(passSharingService.revokeOnLinkEnd(company, member, "Fin du rattachement : " + reason.trim(), actor));
        // Ses autorisations de depense sur le portefeuille de l'organisation sont suspendues, ses
        // demandes en attente annulees et leurs fonds rendus.
        companyWalletService.suspendOnLinkEnd(company, member, "Fin du rattachement : " + reason.trim(), actor);
        String seatWarning = future.isEmpty() ? null
                : "Réservations à venir prises sur l'abonnement ou les passes de l'organisation, à traiter : "
                + String.join(", ", future) + ".";
        if (member.getCustomer() == null || !company.getId().equals(member.getCustomer().getId())) {
            return toResponse(link, false, seatWarning);
        }
        try {
            Customer personal = personalCustomer(member);
            member.setCustomer(personal);
            memberRepository.save(member);
            log.info("Membre {} · fin du lien avec {}, payeur par defaut repasse sur {}",
                    member.getMemberId(), company.getCustomerId(), personal.getCustomerId());
            return toResponse(link, true, seatWarning);
        } catch (RuntimeException ex) {
            // Le lien est bien termine ; seul le payeur reste a corriger, et on le dit.
            log.warn("Membre {} · fiche personnelle impossible a creer : {}", member.getMemberId(), ex.getMessage());
            return toResponse(link, false, "Le lien est terminé, mais le payeur par défaut du membre est toujours "
                    + company.getCompanyName() + " : " + ex.getMessage() + ". À corriger sur la fiche du membre."
                    + (seatWarning == null ? "" : " " + seatWarning));
        }
    }

    @Transactional(readOnly = true)
    public List<MembershipDtos.LinkResponse> companiesOf(String memberId) {
        return linkRepository.findAllByMemberOrderByStartedAtDesc(member(memberId)).stream()
                .map(link -> toResponse(link, null, null)).toList();
    }

    // ── Portail ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<MembershipDtos.MyCompany> myCompanies(Member member) {
        return linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(member, MemberLinkStatus.ACTIVE).stream()
                .map(link -> new MembershipDtos.MyCompany(
                        link.getCustomer().getCustomerId(),
                        link.getCustomer().getCompanyName(),
                        link.getRole(),
                        link.getStartedAt(),
                        link.effectiveInvoiceLinkMode(),
                        link.getRole() == MemberLinkRole.OWNER ? defaultMode(link.getCustomer()) : null))
                .toList();
    }

    /** Les membres de l'organisation · son gerant seulement, un employe recoit 404. */
    @Transactional(readOnly = true)
    public List<MembershipDtos.LinkResponse> membersForOwner(Member owner, String customerId) {
        Customer company = ownedCompany(owner, customerId);
        return linkRepository.findAllByCustomerAndStatusOrderByStartedAtDesc(company, MemberLinkStatus.ACTIVE).stream()
                .map(link -> toResponse(link, null, null)).toList();
    }

    /** Le gerant change le mode par defaut de son organisation. */
    public MembershipDtos.MyCompany setCompanyDefaultMode(Member owner, String customerId, InvoiceLinkMode mode) {
        Customer company = ownedCompany(owner, customerId);
        return applyCompanyDefaultMode(company, mode, owner);
    }

    /** Le gerant change le mode d'un employe, ou le ramene au defaut de l'organisation. */
    public MembershipDtos.LinkResponse setLinkMode(Member owner, String customerId, Long linkId,
                                                   MembershipDtos.LinkModeRequest request) {
        Customer company = ownedCompany(owner, customerId);
        return update(company.getCustomerId(), linkId,
                new MembershipDtos.UpdateLinkRequest(null, request.mode(), request.useCompanyDefaultMode(), null, null));
    }

    /** Le mode par defaut d'une organisation, depuis l'administration. */
    public InvoiceLinkMode setCompanyDefaultModeAsAdmin(String customerId, InvoiceLinkMode mode) {
        Customer company = company(customerId);
        applyCompanyDefaultMode(company, mode, null);
        return mode;
    }

    // ── Regles ───────────────────────────────────────────────────────────────

    private MembershipDtos.MyCompany applyCompanyDefaultMode(Customer company, InvoiceLinkMode mode, Member viewer) {
        company.setDefaultInvoiceLinkMode(mode);
        customerRepository.saveAndFlush(company);
        // Le changement de defaut touche tous les membres qui en heritent · aucun ne doit se
        // retrouver avec deux organisations automatiques.
        for (CustomerMemberLink link : linkRepository.findAllByCustomerAndStatusOrderByStartedAtDesc(company, MemberLinkStatus.ACTIVE)) {
            ensureSingleAutomatic(link.getMember(), activeLinks(link.getMember()));
        }
        CustomerMemberLink own = viewer == null ? null
                : linkRepository.findByCustomerAndMemberAndStatus(company, viewer, MemberLinkStatus.ACTIVE).orElse(null);
        return new MembershipDtos.MyCompany(company.getCustomerId(), company.getCompanyName(),
                own == null ? null : own.getRole(), own == null ? null : own.getStartedAt(),
                own == null ? mode : own.effectiveInvoiceLinkMode(), mode);
    }

    /**
     * Un seul lien actif en mode automatique par membre, mode herite compris.
     *
     * <p>L'index de la base ne voit que le mode propre au lien ; le mode herite de l'organisation
     * se verifie ici. Le refus annule toute l'operation.</p>
     */
    static void ensureSingleAutomatic(Member member, List<CustomerMemberLink> activeLinks) {
        List<CustomerMemberLink> automatic = activeLinks.stream()
                .filter(CustomerMemberLink::active)
                .filter(link -> link.effectiveInvoiceLinkMode() == InvoiceLinkMode.AUTOMATIC)
                .toList();
        if (automatic.size() > 1) {
            String names = String.join(" et ", automatic.stream().map(l -> l.getCustomer().getCompanyName()).toList());
            throw new BadRequestException(member.getDisplayName() + " aurait ses factures reliées automatiquement à "
                    + names + ". Un membre n'a qu'une organisation en mode automatique : passez l'une d'elles en « au choix ».");
        }
    }

    private List<CustomerMemberLink> activeLinks(Member member) {
        return linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(member, MemberLinkStatus.ACTIVE);
    }

    private Customer personalCustomer(Member member) {
        String email = StringUtils.hasText(member.getEmail()) ? member.getEmail().trim().toLowerCase(Locale.ROOT) : null;
        if (email != null) {
            Customer existing = customerRepository.findByEmailAndDeletedFalse(email)
                    .filter(c -> c.getType() == CustomerType.PERSON)
                    .orElse(null);
            if (existing != null) {
                return existing;
            }
        }
        Customer created = customerService.createCustomerForMember(CustomerRequest.builder()
                .type(CustomerType.PERSON.name())
                .firstname(member.getFirstname())
                .lastname(member.getLastname())
                .email(email)
                .phone(member.getPhone())
                .whatsappPhone(member.getWhatsappPhone())
                .build());
        created.setMember(true);
        return customerRepository.save(created);
    }

    private Customer company(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + customerId));
        if (customer.getType() != CustomerType.COMPANY) {
            throw new BadRequestException("Le client " + customerId + " est une personne : on ne rattache des membres qu'à une organisation.");
        }
        return customer;
    }

    private Member member(String memberId) {
        return memberRepository.findByMemberIdAndDeletedFalse(memberId == null ? "" : memberId.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Membre introuvable : " + memberId));
    }

    private CustomerMemberLink activeLink(Customer company, Long linkId) {
        CustomerMemberLink link = linkRepository.findByShortId(linkId)
                .filter(l -> l.getCustomer().getId().equals(company.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Rattachement introuvable : " + linkId));
        if (!link.active()) {
            throw new BadRequestException("Ce rattachement est déjà terminé.");
        }
        return link;
    }

    /** L'organisation dont ce membre est gerant · 404 sinon, sans dire si elle existe. */
    public Customer ownedCompany(Member owner, String customerId) {
        Customer company = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted() && c.getType() == CustomerType.COMPANY)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));
        return linkRepository.findByCustomerAndMemberAndStatus(company, owner, MemberLinkStatus.ACTIVE)
                .filter(link -> link.getRole() == MemberLinkRole.OWNER)
                .map(CustomerMemberLink::getCustomer)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable"));
    }

    private static InvoiceLinkMode defaultMode(Customer company) {
        return company.getDefaultInvoiceLinkMode() == null ? InvoiceLinkMode.ON_CHOICE : company.getDefaultInvoiceLinkMode();
    }

    private static MemberLinkStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return MemberLinkStatus.ACTIVE;
        }
        try {
            return MemberLinkStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Statut inconnu : " + status + ". Valeurs : ACTIVE, ENDED, ALL.");
        }
    }

    private static String label(MemberLinkRole role) {
        return role == MemberLinkRole.OWNER ? "propriétaire" : "employé";
    }

    private static MembershipDtos.LinkResponse toResponse(CustomerMemberLink link, Boolean payerChanged, String warning) {
        Member member = link.getMember();
        return new MembershipDtos.LinkResponse(
                link.getShortId(),
                link.getCustomer().getCustomerId(),
                link.getCustomer().getCompanyName(),
                member.getMemberId(),
                member.getDisplayName(),
                member.getEmail(),
                link.getRole(),
                link.getStatus(),
                link.getStartedAt(),
                link.getEndedAt(),
                link.getEndedReason(),
                link.getInvoiceLinkMode(),
                link.effectiveInvoiceLinkMode(),
                link.isCanSign(),
                link.isCanCollectMail(),
                payerChanged,
                warning);
    }
}
