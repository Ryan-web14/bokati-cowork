package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.contract.dto.request.ContractPartyRequest;
import com.sni.bokaticowork.features.contract.dto.request.CreateContractRequest;
import com.sni.bokaticowork.features.contract.dto.request.GenerateContractRequest;
import com.sni.bokaticowork.features.contract.dto.response.ContractResponse;
import com.sni.bokaticowork.features.contract.enums.ContractPartyRole;
import com.sni.bokaticowork.features.contract.enums.ContractRenewalType;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractGenerationService;
import com.sni.bokaticowork.features.contract.service.interfaces.ContractService;
import com.sni.bokaticowork.features.document.documentMaster.dto.response.DocumentResponse;
import com.sni.bokaticowork.features.document.documentMaster.enums.DocumentOwnerType;
import com.sni.bokaticowork.features.subscription.addon.model.SubscriptionAddon;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.PlanVersion;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SubscriptionContractSupport {

    private static final Long SYSTEM_ACTOR_ID = 0L;

    private final ContractService contractService;
    private final ContractGenerationService contractGenerationService;
    private final SubscriptionOwnerResolver ownerResolver;
    private final SubscriptionContractTemplatePolicyResolver templatePolicyResolver;

    public String createAndSignForSubscription(Subscription subscription) {
        DocumentOwnerType ownerType = toDocumentOwnerType(subscription.getSubscriberType());
        OwnerContractView owner = ownerView(subscription);
        SubscriptionContractTemplatePolicyResolver.ContractTemplatePolicy policy = templatePolicyResolver.nonRefundablePolicy();
        String templateCode = policy.templateCode();
        String title = "Contrat abonnement " + subscription.getSubscriptionNumber();
        String description = subscriptionDescription(subscription);

        CreateContractRequest createRequest = baseCreateRequest(
                ownerType,
                subscription.getSubscriberCode(),
                businessCode(ownerType, subscription.getSubscriberCode()),
                templateCode,
                title,
                description,
                subscription.getStartDate(),
                subscription.getStartDate(),
                subscription.getCurrentPeriodEnd(),
                Boolean.TRUE.equals(subscription.getAutoRenew()) ? ContractRenewalType.AUTO_RENEW : ContractRenewalType.FIXED_TERM,
                owner
        );
        ContractResponse contract = contractService.create(createRequest);
        DocumentResponse document = contractGenerationService.generatePdf(baseGenerateRequest(
                ownerType,
                subscription.getSubscriberCode(),
                businessCode(ownerType, subscription.getSubscriberCode()),
                templateCode,
                title,
                description,
                subscription.getStartDate(),
                subscription.getStartDate(),
                subscription.getCurrentPeriodEnd(),
                owner,
                policy.clauses(),
                withSystemOperatorSignature(Map.of(
                        "contractPolicyCode", policy.code(),
                        "sourceType", "SUBSCRIPTION",
                        "subscriptionNumber", subscription.getSubscriptionNumber(),
                        "planCode", planCode(subscription.getPlanVersion()),
                        "planVersion", planVersion(subscription.getPlanVersion()),
                        "billingCycle", subscription.getBillingCycle() == null ? "" : subscription.getBillingCycle().name(),
                        "currency", nullSafe(subscription.getCurrency()),
                        "totalAmount", amount(subscription.getTotalAmount())
                ))
        ));
        contractService.markGenerated(contract.getContractCode(), document.getCode());
        return contractService.markSigned(contract.getContractCode(), document.getCode()).getContractCode();
    }

    public String createAndSignForAddon(SubscriptionAddon addon) {
        Subscription subscription = addon.getSubscription();
        DocumentOwnerType ownerType = toDocumentOwnerType(subscription.getSubscriberType());
        OwnerContractView owner = ownerView(subscription);
        SubscriptionContractTemplatePolicyResolver.ContractTemplatePolicy policy = templatePolicyResolver.nonRefundablePolicy();
        String templateCode = policy.templateCode();
        String title = "Contrat add-on " + addon.getId();
        String description = addonDescription(addon);

        CreateContractRequest createRequest = baseCreateRequest(
                ownerType,
                subscription.getSubscriberCode(),
                businessCode(ownerType, subscription.getSubscriberCode()),
                templateCode,
                title,
                description,
                addon.getStartsAt(),
                addon.getStartsAt(),
                addon.getEndsAt(),
                addon.getEndsAt() == null ? ContractRenewalType.NONE : ContractRenewalType.FIXED_TERM,
                owner
        );
        ContractResponse contract = contractService.create(createRequest);
        DocumentResponse document = contractGenerationService.generatePdf(baseGenerateRequest(
                ownerType,
                subscription.getSubscriberCode(),
                businessCode(ownerType, subscription.getSubscriberCode()),
                templateCode,
                title,
                description,
                addon.getStartsAt(),
                addon.getStartsAt(),
                addon.getEndsAt(),
                owner,
                policy.clauses(),
                withSystemOperatorSignature(Map.of(
                        "contractPolicyCode", policy.code(),
                        "sourceType", "SUBSCRIPTION_ADDON",
                        "subscriptionNumber", subscription.getSubscriptionNumber(),
                        "addonId", String.valueOf(addon.getId()),
                        "planCode", planCode(addon.getPlanVersion()),
                        "planVersion", planVersion(addon.getPlanVersion()),
                        "quantity", addon.getQuantity() == null ? "" : String.valueOf(addon.getQuantity()),
                        "currency", nullSafe(addon.getCurrency()),
                        "unitPrice", amount(addon.getUnitPrice())
                ))
        ));
        contractService.markGenerated(contract.getContractCode(), document.getCode());
        return contractService.markSigned(contract.getContractCode(), document.getCode()).getContractCode();
    }

    public String createAndSignForPass(Pass pass) {
        DocumentOwnerType ownerType = toDocumentOwnerType(pass.getOwnerType());
        OwnerContractView owner = ownerView(pass);
        SubscriptionContractTemplatePolicyResolver.ContractTemplatePolicy policy = templatePolicyResolver.nonRefundablePolicy();
        String templateCode = policy.templateCode();
        String title = "Contrat pass " + pass.getPassNumber();
        String description = passDescription(pass);
        LocalDate startDate = toLocalDate(pass.getValidFrom());
        LocalDate endDate = toLocalDate(pass.getValidUntil());

        CreateContractRequest createRequest = baseCreateRequest(
                ownerType,
                pass.getOwnerCode(),
                businessCode(ownerType, pass.getOwnerCode()),
                templateCode,
                title,
                description,
                startDate,
                startDate,
                endDate,
                endDate == null ? ContractRenewalType.NONE : ContractRenewalType.FIXED_TERM,
                owner
        );
        ContractResponse contract = contractService.create(createRequest);
        DocumentResponse document = contractGenerationService.generatePdf(baseGenerateRequest(
                ownerType,
                pass.getOwnerCode(),
                businessCode(ownerType, pass.getOwnerCode()),
                templateCode,
                title,
                description,
                startDate,
                startDate,
                endDate,
                owner,
                policy.clauses(),
                withSystemOperatorSignature(Map.of(
                        "contractPolicyCode", policy.code(),
                        "sourceType", "PASS",
                        "passNumber", pass.getPassNumber(),
                        "passType", pass.getPassType() == null ? "" : pass.getPassType().name(),
                        "subscriptionNumber", pass.getSubscription() == null ? "" : pass.getSubscription().getSubscriptionNumber(),
                        "planCode", planCode(pass.getPlanVersion()),
                        "planVersion", planVersion(pass.getPlanVersion())
                ))
        ));
        contractService.markGenerated(contract.getContractCode(), document.getCode());
        return contractService.markSigned(contract.getContractCode(), document.getCode()).getContractCode();
    }

    private CreateContractRequest baseCreateRequest(DocumentOwnerType ownerType,
                                                    String ownerCode,
                                                    String businessCode,
                                                    String templateCode,
                                                    String title,
                                                    String description,
                                                    LocalDate effectiveDate,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    ContractRenewalType renewalType,
                                                    OwnerContractView owner) {
        CreateContractRequest request = new CreateContractRequest();
        request.setOwnerType(ownerType);
        request.setOwnerCode(ownerCode);
        request.setBusinessCode(businessCode);
        request.setTemplateCode(templateCode);
        request.setTitle(title);
        request.setDescription(description);
        request.setEffectiveDate(effectiveDate);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setRenewalType(renewalType);
        request.setCreatedBy(SYSTEM_ACTOR_ID);
        request.setParties(List.of(signatory(ownerType, ownerCode, owner)));
        return request;
    }

    private GenerateContractRequest baseGenerateRequest(DocumentOwnerType ownerType,
                                                        String ownerCode,
                                                        String businessCode,
                                                        String templateCode,
                                                        String title,
                                                        String description,
                                                        LocalDate effectiveDate,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        OwnerContractView owner,
                                                        List<String> clauses,
                                                        Map<String, String> variables) {
        GenerateContractRequest request = new GenerateContractRequest();
        request.setOwnerType(ownerType);
        request.setOwnerCode(ownerCode);
        request.setBusinessCode(businessCode);
        request.setTemplateCode(templateCode);
        request.setTitle(title);
        request.setDescription(description);
        request.setUploadedBy(SYSTEM_ACTOR_ID);
        request.setEffectiveDate(effectiveDate);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setSignatoryName(owner.displayName());
        request.setSignatoryRole("SIGNATORY");
        request.setVariables(variables);
        request.setClauses(clauses == null ? List.of() : clauses);
        return request;
    }

    private ContractPartyRequest signatory(DocumentOwnerType ownerType, String ownerCode, OwnerContractView owner) {
        ContractPartyRequest request = new ContractPartyRequest();
        request.setPartyType(ownerType);
        request.setPartyCode(ownerCode);
        request.setDisplayName(owner.displayName());
        request.setEmail(owner.email());
        request.setPhone(owner.phone());
        request.setRole(ContractPartyRole.SIGNATORY);
        request.setSignOrder(1);
        request.setMustSign(Boolean.TRUE);
        return request;
    }

    private OwnerContractView ownerView(Subscription subscription) {
        return switch (subscription.getSubscriberType()) {
            case MEMBER -> fromMember(subscription.getMember(), subscription.getSubscriberCode());
            case CUSTOMER -> fromCustomer(subscription.getCustomer(), subscription.getSubscriberCode());
            case BUSINESS_ENTITY -> fromBusiness(subscription.getBusinessEntity(), subscription.getSubscriberCode());
        };
    }

    private OwnerContractView ownerView(Pass pass) {
        return switch (pass.getOwnerType()) {
            case MEMBER -> {
                Subscription subscription = pass.getSubscription();
                if (subscription != null && subscription.getMember() != null) {
                    yield fromMember(subscription.getMember(), pass.getOwnerCode());
                }
                yield ownerView(ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode()));
            }
            case CUSTOMER -> {
                Subscription subscription = pass.getSubscription();
                if (subscription != null && subscription.getCustomer() != null) {
                    yield fromCustomer(subscription.getCustomer(), pass.getOwnerCode());
                }
                yield ownerView(ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode()));
            }
            case BUSINESS_ENTITY -> {
                Subscription subscription = pass.getSubscription();
                if (subscription != null && subscription.getBusinessEntity() != null) {
                    yield fromBusiness(subscription.getBusinessEntity(), pass.getOwnerCode());
                }
                yield ownerView(ownerResolver.resolve(pass.getOwnerType(), pass.getOwnerCode()));
            }
        };
    }

    private OwnerContractView ownerView(SubscriptionOwnerResolver.Owner owner) {
        if (owner.member() != null) {
            return fromMember(owner.member(), owner.code());
        }
        if (owner.customer() != null) {
            return fromCustomer(owner.customer(), owner.code());
        }
        if (owner.businessEntity() != null) {
            return fromBusiness(owner.businessEntity(), owner.code());
        }
        return fallbackOwner(owner.code());
    }

    private OwnerContractView fromMember(Member member, String ownerCode) {
        if (member == null) {
            return fallbackOwner(ownerCode);
        }
        return new OwnerContractView(
                fallback(member.getDisplayName(), member.getMemberId()),
                member.getEmail(),
                member.getPhone()
        );
    }

    private OwnerContractView fromCustomer(Customer customer, String ownerCode) {
        if (customer == null) {
            return fallbackOwner(ownerCode);
        }
        String individualName = (nullSafe(customer.getFirstname()) + " " + nullSafe(customer.getLastname())).trim();
        return new OwnerContractView(
                fallback(customer.getCompanyName(), fallback(individualName, customer.getCustomerId())),
                fallback(customer.getBillingEmail(), customer.getEmail()),
                customer.getPhone()
        );
    }

    private OwnerContractView fromBusiness(BusinessEntity business, String ownerCode) {
        if (business == null) {
            return fallbackOwner(ownerCode);
        }
        return new OwnerContractView(
                fallback(business.getName(), business.getCode()),
                business.getEmail(),
                business.getPhone()
        );
    }

    private OwnerContractView fallbackOwner(String ownerCode) {
        return new OwnerContractView(ownerCode, null, null);
    }

    private DocumentOwnerType toDocumentOwnerType(SubscriberType type) {
        return switch (type) {
            case MEMBER -> DocumentOwnerType.MEMBER;
            case CUSTOMER -> DocumentOwnerType.CUSTOMER;
            case BUSINESS_ENTITY -> DocumentOwnerType.BUSINESS;
        };
    }

    private String businessCode(DocumentOwnerType ownerType, String ownerCode) {
        return ownerType == DocumentOwnerType.BUSINESS ? ownerCode : null;
    }

    private String subscriptionDescription(Subscription subscription) {
        PlanVersion planVersion = subscription.getPlanVersion();
        return "Souscription " + subscription.getSubscriptionNumber()
                + " au plan " + fallback(planVersion == null ? null : planVersion.getName(), planCode(planVersion))
                + ", cycle " + (subscription.getBillingCycle() == null ? "non defini" : subscription.getBillingCycle().name())
                + ", montant " + amount(subscription.getTotalAmount()) + " " + nullSafe(subscription.getCurrency()) + ".";
    }

    private String passDescription(Pass pass) {
        String linkedSubscription = pass.getSubscription() == null
                ? ""
                : " Lie a la souscription " + pass.getSubscription().getSubscriptionNumber() + ".";
        return "Emission du pass " + pass.getPassNumber()
                + " (" + (pass.getPassType() == null ? "type non defini" : pass.getPassType().name()) + ")"
                + " pour " + pass.getName() + "." + linkedSubscription;
    }

    private String addonDescription(SubscriptionAddon addon) {
        return "Activation de l'add-on " + fallback(addon.getPlanVersion().getName(), planCode(addon.getPlanVersion()))
                + " pour la souscription " + addon.getSubscription().getSubscriptionNumber()
                + ", quantite " + addon.getQuantity()
                + ", prix unitaire " + amount(addon.getUnitPrice()) + " " + nullSafe(addon.getCurrency()) + ".";
    }

    private LocalDate toLocalDate(Instant instant) {
        return instant == null ? null : LocalDate.ofInstant(instant, ZoneOffset.UTC);
    }

    private String planCode(PlanVersion planVersion) {
        return planVersion == null || planVersion.getPlan() == null ? "" : nullSafe(planVersion.getPlan().getCode());
    }

    private String planVersion(PlanVersion planVersion) {
        return planVersion == null || planVersion.getVersionNumber() == null ? "" : String.valueOf(planVersion.getVersionNumber());
    }

    private String amount(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String nullSafe(String value) {
        return value == null ? "" : value.trim();
    }

    private Map<String, String> withSystemOperatorSignature(Map<String, String> variables) {
        Map<String, String> enriched = new java.util.LinkedHashMap<>(variables);
        enriched.put("operatorName", "ELLE A OSE");
        enriched.put("operatorSignatureName", "ELLE A OSE");
        enriched.put("operatorSignatureRole", "Signature automatique du système");
        enriched.put("operatorSignedAt", LocalDate.now().toString());
        return enriched;
    }

    private record OwnerContractView(String displayName, String email, String phone) {
    }
}
