package com.sni.bokaticowork.features.company.membership.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.enums.InvoiceLinkMode;
import com.sni.bokaticowork.features.company.enums.MemberLinkStatus;
import com.sni.bokaticowork.features.company.membership.model.CustomerMemberLink;
import com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

/**
 * L'organisation pour le compte de laquelle un membre est facture · ou aucune.
 *
 * <p>Trois cas, et seulement trois :</p>
 * <ul>
 *   <li>une organisation est demandee : le membre doit y etre rattache et actif, sinon refus ;</li>
 *   <li>{@code NONE} est demande : le membre seul, meme si son lien est en mode automatique ;</li>
 *   <li>rien n'est demande : l'organisation dont le lien est en mode {@code AUTOMATIC} (il n'y en a
 *       qu'une au plus), sinon le membre seul.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MemberBillingCompanyResolver {

    public static final String NO_COMPANY = "NONE";

    private final MemberRepository memberRepository;
    private final CustomerMemberLinkRepository linkRepository;

    public Optional<Customer> resolve(String customerType, String customerCode, String requestedCompanyCode) {
        boolean member = "MEMBER".equalsIgnoreCase(customerType == null ? "" : customerType.trim());
        String requested = StringUtils.hasText(requestedCompanyCode) ? requestedCompanyCode.trim() : null;
        if (!member) {
            if (requested != null && !NO_COMPANY.equalsIgnoreCase(requested)) {
                throw new BadRequestException("Seule une facture au nom d'un membre se relie à une organisation : "
                        + "pour facturer l'organisation, mettez-la comme destinataire.");
            }
            return Optional.empty();
        }
        if (requested != null && NO_COMPANY.equalsIgnoreCase(requested)) {
            return Optional.empty();
        }
        Member payer = memberRepository.findByMemberIdAndDeletedFalse(customerCode == null ? "" : customerCode.trim())
                .orElse(null);
        if (payer == null) {
            return Optional.empty();
        }
        List<CustomerMemberLink> active = linkRepository.findAllByMemberAndStatusOrderByStartedAtDesc(payer, MemberLinkStatus.ACTIVE);
        if (requested != null) {
            return Optional.of(active.stream()
                    .filter(link -> requested.equalsIgnoreCase(link.getCustomer().getCustomerId()))
                    .map(CustomerMemberLink::getCustomer)
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException(payer.getDisplayName()
                            + " n'est pas rattaché à l'organisation " + requested + " : la facture ne peut pas être émise pour son compte.")));
        }
        return active.stream()
                .filter(link -> link.effectiveInvoiceLinkMode() == InvoiceLinkMode.AUTOMATIC)
                .map(CustomerMemberLink::getCustomer)
                .findFirst();
    }
}
