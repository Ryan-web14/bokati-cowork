package com.sni.bokaticowork.core.outbox.service.implementation;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxRecipientResolver {

    private final CustomerRepository customerRepository;
    private final MemberRepository memberRepository;
    private final BusinessRepository businessRepository;

    public Recipient resolveByOwnerId(DocumentOwnerType ownerType, Long ownerId) {
        if (ownerType == null || ownerId == null) {
            return null;
        }

        return switch (ownerType) {
            case MEMBER -> memberRepository.findById(ownerId)
                    .map(member -> new Recipient(member.getDisplayName(), member.getEmail()))
                    .orElse(null);
            case CUSTOMER -> customerRepository.findById(ownerId)
                    .map(customer -> new Recipient(customerDisplayName(customer), customer.getEmail()))
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
                    .map(customer -> new Recipient(customerDisplayName(customer), customer.getEmail()))
                    .orElse(null);
            case BUSINESS -> businessRepository.findByCode(normalizedCode)
                    .map(business -> new Recipient(business.getName(), business.getEmail()))
                    .orElse(null);
            default -> null;
        };
    }

    private String customerDisplayName(Customer customer) {
        if (customer.getCompanyName() != null && !customer.getCompanyName().isBlank()) {
            return customer.getCompanyName();
        }
        String firstName = customer.getFirstname() == null ? "" : customer.getFirstname();
        String lastName = customer.getLastname() == null ? "" : customer.getLastname();
        return (firstName + " " + lastName).trim();
    }

    public record Recipient(String displayName, String email) {
    }
}
