package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.enums.EventType;
import com.sni.bokaticowork.core.enums.NotificationChannel;
import com.sni.bokaticowork.core.enums.ReceiptPreference;
import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Le membre qui se tient derriere un destinataire de facture, de recu ou de message de paiement.
 *
 * <p>Un destinataire est un membre, ou le client personne d'un membre. Une organisation (client
 * entreprise, entite) n'a pas de membre derriere elle : elle garde sa propre adresse de facturation
 * et ses envois ne se coupent pas par les preferences d'un membre.</p>
 *
 * <p>Toute lecture qui echoue rend le comportement par defaut : adresse inchangee, envoi maintenu,
 * fuseau de l'exploitant.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemberBillingContact {

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final MemberPreferences preferences;

    /** Le membre derriere un tiers de facturation ({@code MEMBER} ou {@code CUSTOMER} personne). */
    public Optional<Member> memberOf(String partyType, String partyCode) {
        if (!StringUtils.hasText(partyType) || !StringUtils.hasText(partyCode)) {
            return Optional.empty();
        }
        try {
            String code = partyCode.trim();
            return switch (partyType.trim().toUpperCase(Locale.ROOT)) {
                case "MEMBER" -> memberRepository.findByMemberIdAndDeletedFalse(code);
                case "CUSTOMER" -> customerRepository.findByCustomerId(code).flatMap(this::memberOf);
                default -> Optional.empty();
            };
        } catch (RuntimeException ex) {
            log.debug("Membre de {} {} introuvable : {}", partyType, partyCode, ex.getMessage());
            return Optional.empty();
        }
    }

    /** Le membre d'un client personne, s'il y en a exactement un. Jamais pour une entreprise. */
    public Optional<Member> memberOf(Customer customer) {
        if (customer == null || customer.isDeleted() || customer.getType() == CustomerType.COMPANY) {
            return Optional.empty();
        }
        try {
            List<Member> members = memberRepository
                    .findAllByCustomerAndDeletedFalse(customer, PageRequest.of(0, 2))
                    .getContent();
            return members.size() == 1 ? Optional.of(members.get(0)) : Optional.empty();
        } catch (RuntimeException ex) {
            log.debug("Membre du client {} introuvable : {}", customer.getCustomerId(), ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * L'adresse ou envoyer un document de facturation a ce membre.
     *
     * <p>L'adresse de facturation des preferences passe avant l'adresse du compte et avant
     * l'adresse de facturation du client personne. Une adresse saisie a la main, differente de
     * celles du compte, est gardee : quelqu'un l'a choisie pour ce document.</p>
     *
     * @param current l'adresse connue jusqu'ici (peut etre vide ou une liste separee par des virgules)
     */
    public String recipient(Member member, String current) {
        if (member == null) {
            return current;
        }
        Optional<String> preferred = preferences.billingEmail(member);
        if (preferred.isEmpty()) {
            return current;
        }
        if (!StringUtils.hasText(current) || isAccountAddress(member, current)) {
            return preferred.get();
        }
        return current;
    }

    public String recipient(String partyType, String partyCode, String current) {
        return memberOf(partyType, partyCode).map(member -> recipient(member, current)).orElse(current);
    }

    /** Le membre accepte-t-il ce courriel ? Oui par defaut, et toujours pour un tiers sans membre. */
    public boolean allowsEmail(Member member, EventType event) {
        return member == null || preferences.allows(member, event, NotificationChannel.EMAIL);
    }

    public boolean allowsEmail(String partyType, String partyCode, EventType event) {
        return memberOf(partyType, partyCode).map(member -> allowsEmail(member, event)).orElse(true);
    }

    /** Le membre accepte-t-il ce message WhatsApp ? Oui par defaut, et toujours pour un tiers sans membre. */
    public boolean allowsWhatsApp(String partyType, String partyCode, EventType event) {
        return memberOf(partyType, partyCode)
                .map(member -> event == null || preferences.allows(member, event, NotificationChannel.WHATSAPP))
                .orElse(true);
    }

    /**
     * Le numero WhatsApp d'un tiers de facturation : son numero WhatsApp, sinon son telephone ; pour
     * un client personne sans numero, celui de son membre. Vide si rien n'est connu.
     */
    public Optional<String> whatsAppPhone(String partyType, String partyCode) {
        if (!StringUtils.hasText(partyType) || !StringUtils.hasText(partyCode)) {
            return Optional.empty();
        }
        try {
            String code = partyCode.trim();
            return switch (partyType.trim().toUpperCase(Locale.ROOT)) {
                case "MEMBER" -> memberRepository.findByMemberIdAndDeletedFalse(code).flatMap(MemberBillingContact::whatsAppPhone);
                case "CUSTOMER" -> customerRepository.findByCustomerId(code)
                        .flatMap(customer -> firstPhone(customer.getWhatsappPhone(), customer.getPhone())
                                .or(() -> memberOf(customer).flatMap(MemberBillingContact::whatsAppPhone)));
                default -> Optional.empty();
            };
        } catch (RuntimeException ex) {
            log.debug("Numero WhatsApp de {} {} introuvable : {}", partyType, partyCode, ex.getMessage());
            return Optional.empty();
        }
    }

    public static Optional<String> whatsAppPhone(Member member) {
        return member == null ? Optional.empty() : firstPhone(member.getWhatsappPhone(), member.getPhone());
    }

    public static Optional<String> firstPhone(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return Optional.of(candidate.trim());
            }
        }
        return Optional.empty();
    }

    /**
     * Le membre veut-il un recu par courriel a chaque operation de son portefeuille ? Oui par defaut ;
     * non s'il a choisi « factures seulement ». Toujours oui pour un tiers sans membre.
     */
    public boolean wantsEveryWalletReceipt(String partyType, String partyCode) {
        return memberOf(partyType, partyCode)
                .map(member -> preferences.walletReceipt(member) != ReceiptPreference.INVOICE_ONLY)
                .orElse(true);
    }

    /**
     * Les plafonds personnels de depense du portefeuille que le membre s'est fixes, et son fuseau
     * (le jour se compte chez lui). Vides pour un tiers sans membre.
     */
    public SpendingCaps spendingCaps(String partyType, String partyCode) {
        return memberOf(partyType, partyCode)
                .map(member -> new SpendingCaps(
                        preferences.dailySpendingCap(member).orElse(null),
                        preferences.weeklySpendingCap(member).orElse(null),
                        preferences.zone(member)))
                .orElse(SpendingCaps.NONE);
    }

    /** Plafonds personnels · nul signifie « pas de plafond ». */
    public record SpendingCaps(Long daily, Long weekly, ZoneId zone) {

        public static final SpendingCaps NONE = new SpendingCaps(null, null, defaultZone());

        public boolean isEmpty() {
            return daily == null && weekly == null;
        }
    }

    /** Le fuseau dans lequel afficher une date a ce tiers · celui du membre, sinon celui de l'exploitant. */
    public ZoneId zone(String partyType, String partyCode) {
        return memberOf(partyType, partyCode).map(preferences::zone).orElse(defaultZone());
    }

    public ZoneId zone(Member member) {
        return member == null ? defaultZone() : preferences.zone(member);
    }

    public static ZoneId defaultZone() {
        return ZoneId.of(MemberPreferences.DEFAULT_TIME_ZONE);
    }

    /** L'adresse donnee est-elle une adresse du compte (membre ou client personne lie) ? */
    private boolean isAccountAddress(Member member, String address) {
        String candidate = address.trim();
        List<String> known = new ArrayList<>();
        known.add(member.getEmail());
        try {
            Customer customer = member.getCustomer();
            if (customer != null && customer.getType() != CustomerType.COMPANY) {
                known.add(customer.getEmail());
                known.add(customer.getBillingEmail());
            }
        } catch (RuntimeException ex) {
            // Client non charge hors session · on s'en tient a l'adresse du membre.
            log.debug("Client du membre {} illisible : {}", member.getMemberId(), ex.getMessage());
        }
        return known.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .anyMatch(candidate::equalsIgnoreCase);
    }
}
