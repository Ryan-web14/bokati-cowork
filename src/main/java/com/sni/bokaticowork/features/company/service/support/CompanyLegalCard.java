package com.sni.bokaticowork.features.company.service.support;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.enums.MemberLinkRole;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * La fiche legale d'une organisation, telle que contrats et domiciliation la lisent · plutot que
 * de la faire ressaisir a chaque document.
 *
 * <p>Et ceux qui signent pour elle : son representant legal, et les membres rattaches dont le lien
 * porte le drapeau {@code can_sign}. Le signataire est designe, plus devine a partir de ce que la
 * demande fournit.</p>
 */
@Component
@RequiredArgsConstructor
public class CompanyLegalCard {

    private final OrganizationBillingProfile billingProfile;
    private final CustomerMemberLinkRepository linkRepository;

    /**
     * @param representativeTitle sa qualite (gerant, directeur general...) · vide si non saisie
     */
    public record Card(String customerId, String name, String legalForm, String rccm, String niu,
                       String representativeName, String representativeTitle, String address,
                       List<OrganizationBillingProfile.IdentifierLine> identifiers) {
    }

    /**
     * Une personne qui peut signer pour l'organisation.
     *
     * @param memberId vide pour le representant legal saisi sur la fiche sans compte membre
     */
    public record Signatory(String name, String title, String email, String memberId, boolean legalRepresentative) {
    }

    /** La fiche d'une organisation · vide pour une personne ou un client sans fiche legale. */
    public Optional<Card> of(Customer customer) {
        if (customer == null || customer.getType() != CustomerType.COMPANY) {
            return Optional.empty();
        }
        BusinessEntity legal = customer.getBusinessEntity();
        String name = StringUtils.hasText(customer.getCompanyName()) ? customer.getCompanyName().trim()
                : legal != null && StringUtils.hasText(legal.getName()) ? legal.getName().trim() : customer.getCustomerId();
        if (legal == null) {
            return Optional.of(new Card(customer.getCustomerId(), name, null, null, null, null, null,
                    format(customer.getAddress()), List.of()));
        }
        List<OrganizationBillingProfile.IdentifierLine> identifiers = billingProfile.identifiers(legal);
        return Optional.of(new Card(customer.getCustomerId(), name, trim(legal.getLegalForm()),
                identifier(identifiers, "RCCM", legal.getRccmNumber()), identifier(identifiers, "NIU", legal.getNiuNumber()),
                trim(legal.getLegalRepresentativeName()), trim(legal.getLegalRepresentativeTitle()),
                format(legal.getAddress() != null ? legal.getAddress() : customer.getAddress()), identifiers));
    }

    /** Le representant legal d'abord, puis les membres autorises a signer, dans l'ordre de leur rattachement. */
    public List<Signatory> signatories(Customer customer) {
        List<Signatory> signatories = new ArrayList<>();
        of(customer).filter(card -> StringUtils.hasText(card.representativeName())).ifPresent(card ->
                signatories.add(new Signatory(card.representativeName(),
                        StringUtils.hasText(card.representativeTitle()) ? card.representativeTitle() : "Représentant légal",
                        null, null, true)));
        if (customer == null) {
            return signatories;
        }
        for (CustomerMemberLink link : linkRepository.findAllByCustomerAndStatusOrderByStartedAtDesc(customer, MemberLinkStatus.ACTIVE)) {
            if (!link.isCanSign() || link.getMember() == null) {
                continue;
            }
            Member member = link.getMember();
            boolean representative = !signatories.isEmpty() && signatories.getFirst().legalRepresentative()
                    && sameName(signatories.getFirst().name(), member.getDisplayName());
            if (representative) {
                // Le representant a aussi un compte · on garde une seule ligne, avec son adresse.
                Signatory first = signatories.removeFirst();
                signatories.addFirst(new Signatory(first.name(), first.title(), member.getEmail(), member.getMemberId(), true));
                continue;
            }
            signatories.add(new Signatory(member.getDisplayName(), link.getRole() == MemberLinkRole.OWNER ? "Gérant" : "Mandataire",
                    member.getEmail(), member.getMemberId(), false));
        }
        return signatories;
    }

    /**
     * La personne est-elle designee pour signer · par son adresse (membre autorise ou representant
     * ayant un compte) ou, pour le representant saisi sans compte, par son nom.
     */
    public Optional<Signatory> designated(Customer customer, String email, String name) {
        return signatories(customer).stream()
                .filter(s -> (StringUtils.hasText(email) && StringUtils.hasText(s.email()) && s.email().equalsIgnoreCase(email.trim()))
                        || (s.legalRepresentative() && !StringUtils.hasText(s.email()) && sameName(s.name(), name)))
                .findFirst();
    }

    private static boolean sameName(String a, String b) {
        if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
            return false;
        }
        return normalize(a).equals(normalize(b));
    }

    private static String normalize(String value) {
        return java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String identifier(List<OrganizationBillingProfile.IdentifierLine> lines, String type, String fallback) {
        return lines.stream().filter(line -> type.equalsIgnoreCase(line.type())).map(OrganizationBillingProfile.IdentifierLine::value)
                .filter(StringUtils::hasText).findFirst().orElse(trim(fallback));
    }

    public static String format(Address address) {
        if (address == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.hasText(address.getStreetNumber())) sb.append(address.getStreetNumber()).append(" ");
        if (StringUtils.hasText(address.getStreetName())) sb.append(address.getStreetName()).append(", ");
        if (StringUtils.hasText(address.getDistrict())) sb.append(address.getDistrict()).append(", ");
        if (StringUtils.hasText(address.getCity())) sb.append(address.getCity());
        String result = sb.toString().replaceAll(",\\s*$", "").trim();
        return result.isEmpty() ? null : result;
    }

    private static String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
