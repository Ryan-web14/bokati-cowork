package com.sni.bokaticowork.features.billing.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class BillingCustomerSnapshotResolver {

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile organizationProfile;

    public CustomerSnapshot resolve(CreateBillingDocumentRequest request) {
        validateCustomerReference(request.customerType(), request.customerCode());
        CustomerSnapshot resolved = resolveKnownCustomer(request.customerType(), request.customerCode());
        String name = firstText(request.customerName(), resolved.customerName());
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Customer name is required when customer cannot be resolved");
        }
        String customerType = StringUtils.hasText(resolved.customerType())
                ? resolved.customerType()
                : (StringUtils.hasText(request.customerType()) ? request.customerType().trim() : "MANUAL");
        String customerCode = StringUtils.hasText(resolved.customerCode())
                ? resolved.customerCode()
                : (StringUtils.hasText(request.customerCode()) ? request.customerCode().trim() : sequenceGenerator.next("billing_customer"));
        return new CustomerSnapshot(
                customerType,
                customerCode,
                name,
                firstText(request.customerEmail(), resolved.customerEmail()),
                firstText(request.customerPhone(), resolved.customerPhone()),
                // L'adresse de la demande d'abord ; a defaut, celle de la fiche de l'organisation.
                firstText(request.billingAddressJson(), resolved.billingAddressJson()),
                resolved.organization()
        );
    }

    private CustomerSnapshot resolveKnownCustomer(String customerType, String customerCode) {
        if (!StringUtils.hasText(customerType) || !StringUtils.hasText(customerCode)) {
            return CustomerSnapshot.empty();
        }
        return switch (customerType.trim().toUpperCase()) {
            case "MEMBER" -> memberRepository.findByMemberIdAndDeletedFalse(customerCode.trim())
                    .map(this::fromMember)
                    .orElseThrow(() -> new ResourceNotFoundException("Member not found: " + customerCode));
            case "CUSTOMER" -> {
                var found = customerRepository.findByCustomerId(customerCode.trim());
                if (found.isPresent() && found.get().isDeleted()) {
                    throw new BadRequestException("Le client " + customerCode + " est supprimé et ne peut plus être facturé");
                }
                yield found.map(this::fromCustomer).orElse(CustomerSnapshot.empty());
            }
            case "BUSINESS", "BUSINESS_ENTITY" -> businessRepository.findByCode(customerCode.trim())
                    .map(this::fromBusiness)
                    .orElseThrow(() -> new ResourceNotFoundException("Business entity not found: " + customerCode));
            default -> CustomerSnapshot.empty();
        };
    }

    private void validateCustomerReference(String customerType, String customerCode) {
        if (StringUtils.hasText(customerType) && !StringUtils.hasText(customerCode)) {
            throw new BadRequestException("Customer code is required when customer type is provided");
        }
        if (!StringUtils.hasText(customerType) && StringUtils.hasText(customerCode)) {
            throw new BadRequestException("Customer type is required when customer code is provided");
        }
    }

    private CustomerSnapshot fromMember(Member member) {
        return new CustomerSnapshot("MEMBER", member.getMemberId(), fallback(member.getDisplayName(), member.getMemberId()), member.getEmail(), member.getPhone(), null);
    }

    private CustomerSnapshot fromCustomer(Customer customer) {
        String individualName = (nullSafe(customer.getFirstname()) + " " + nullSafe(customer.getLastname())).trim();
        var organization = organizationProfile.of(customer).orElse(null);
        return new CustomerSnapshot(
                "CUSTOMER",
                customer.getCustomerId(),
                fallback(customer.getCompanyName(), fallback(individualName, customer.getCustomerId())),
                firstText(customer.getBillingEmail(), customer.getEmail()),
                customer.getPhone(),
                organization == null ? null : organization.billingAddressJson(),
                organization
        );
    }

    private CustomerSnapshot fromBusiness(BusinessEntity business) {
        return new CustomerSnapshot("BUSINESS_ENTITY", business.getCode(), fallback(business.getName(), business.getCode()), business.getEmail(), business.getPhone(), null);
    }

    private String firstText(String first, String second) {
        return StringUtils.hasText(first) ? first.trim() : (StringUtils.hasText(second) ? second.trim() : null);
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * Le destinataire fige sur la facture · {@code organization} porte les conditions de la fiche
     * d'une organisation cliente (identifiants, exoneration, echeance, plafond, blocage), vide sinon.
     */
    public record CustomerSnapshot(String customerType,
                                   String customerCode,
                                   String customerName,
                                   String customerEmail,
                                   String customerPhone,
                                   String billingAddressJson,
                                   com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile.Terms organization) {

        public CustomerSnapshot(String customerType, String customerCode, String customerName,
                                String customerEmail, String customerPhone, String billingAddressJson) {
            this(customerType, customerCode, customerName, customerEmail, customerPhone, billingAddressJson, null);
        }

        static CustomerSnapshot empty() {
            return new CustomerSnapshot(null, null, null, null, null, null, null);
        }
    }
}
