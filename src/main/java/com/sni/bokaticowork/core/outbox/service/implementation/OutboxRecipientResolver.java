package com.sni.bokaticowork.core.outbox.service.implementation;

import com.sni.bokaticowork.core.settings.userSettings.service.MemberPreferences;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class OutboxRecipientResolver {

    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final BusinessRepository businessRepository;

    private MemberPreferences memberPreferences;

    @Autowired(required = false)
    void setMemberPreferences(MemberPreferences memberPreferences) {
        this.memberPreferences = memberPreferences;
    }

    public Recipient resolveByOwnerId(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) {
            return null;
        }

        return switch (ownerType) {
            case MEMBER -> memberRepository.findById(ownerId)
                    .map(member -> new Recipient(member.getDisplayName(), member.getEmail()))
                    .orElse(null);
            case CUSTOMER -> customerRepository.findById(ownerId)
                    .map(customer -> new Recipient(customerDisplayName(customer), customerEmail(customer)))
                    .orElse(null);
            case BUSINESS -> businessRepository.findById(ownerId)
                    .map(business -> new Recipient(business.getName(), business.getEmail()))
                    .orElse(null);
            default -> null;
        };
    }

    public Recipient resolveByOwnerCode(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || ownerCode == null || ownerCode.isBlank()) {
            return null;
        }

        String normalizedCode = ownerCode.trim();
        return switch (ownerType) {
            case MEMBER -> memberRepository.findByMemberIdAndDeletedFalse(normalizedCode)
                    .map(member -> new Recipient(member.getDisplayName(), member.getEmail()))
                    .orElse(null);
            case CUSTOMER -> customerRepository.findByCustomerId(normalizedCode)
                    .map(customer -> new Recipient(customerDisplayName(customer), customerEmail(customer)))
                    .orElse(null);
            case BUSINESS -> businessRepository.findByCode(normalizedCode)
                    .map(business -> new Recipient(business.getName(), business.getEmail()))
                    .orElse(null);
            default -> null;
        };
    }

    /**
     * Le destinataire d'une facture, d'un recu ou d'un message de facturation.
     *
     * <p>Pour un membre, ou un client personne relie a un membre, l'adresse de facturation choisie
     * dans ses preferences passe avant le courriel du compte et avant celle de la fiche client. Une
     * organisation garde la sienne. Une liste d'adresses separees par des virgules est gardee telle
     * quelle. Les contrats, la conformite et les documents passent par {@link #resolveByOwnerId}
     * et {@link #resolveByOwnerCode}, qui ne changent pas.</p>
     */
    public Recipient resolveBillingByOwnerId(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) {
            return null;
        }
        return switch (ownerType) {
            case MEMBER -> memberRepository.findById(ownerId).map(this::memberBillingRecipient).orElse(null);
            case CUSTOMER -> customerRepository.findById(ownerId).map(this::customerBillingRecipient).orElse(null);
            default -> resolveByOwnerId(ownerType, ownerId);
        };
    }

    public Recipient resolveBillingByOwnerCode(DocumentOwnerType ownerType, String ownerCode) {
        if (ownerType == null || ownerCode == null || ownerCode.isBlank()) {
            return null;
        }
        String normalizedCode = ownerCode.trim();
        return switch (ownerType) {
            case MEMBER -> memberRepository.findByMemberIdAndDeletedFalse(normalizedCode)
                    .map(this::memberBillingRecipient).orElse(null);
            case CUSTOMER -> customerRepository.findByCustomerId(normalizedCode)
                    .map(this::customerBillingRecipient).orElse(null);
            default -> resolveByOwnerCode(ownerType, normalizedCode);
        };
    }

    private Recipient memberBillingRecipient(Member member) {
        String email = memberPreferences == null
                ? member.getEmail()
                : memberPreferences.billingRecipient(member, member.getEmail());
        return new Recipient(member.getDisplayName(), email);
    }

    private Recipient customerBillingRecipient(Customer customer) {
        String fallback = customerEmail(customer);
        if (memberPreferences == null || customer.getType() != CustomerType.PERSON) {
            return new Recipient(customerDisplayName(customer), fallback);
        }
        String email = linkedMember(customer)
                .flatMap(memberPreferences::billingEmail)
                .orElse(fallback);
        return new Recipient(customerDisplayName(customer), email);
    }

    /** Le membre d'un client personne : le seul membre rattache, sinon celui de meme adresse. */
    private Optional<Member> linkedMember(Customer customer) {
        try {
            List<Member> members = memberRepository
                    .findAllByCustomerAndDeletedFalse(customer, PageRequest.of(0, 2))
                    .getContent();
            if (members.size() == 1) {
                return Optional.of(members.get(0));
            }
        } catch (RuntimeException ignored) {
            // Lecture impossible : on se rabat sur l'adresse du client.
        }
        return memberPreferences.memberByEmail(customer.getEmail());
    }

    private String customerDisplayName(Customer customer) {
        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) {
            return customer.getCompanyName();
        }
        String firstName = customer.getFirstname() == null ? "" : customer.getFirstname();
        String lastName = customer.getLastname() == null ? "" : customer.getLastname();
        return (firstName + " " + lastName).trim();
    }

    private String customerEmail(Customer customer) {
        if (customer.getBillingEmail() != null && !customer.getBillingEmail().isBlank()) {
            return customer.getBillingEmail();
        }
        return customer.getEmail();
    }

    public record Recipient(String displayName, String email) {
    }
}
