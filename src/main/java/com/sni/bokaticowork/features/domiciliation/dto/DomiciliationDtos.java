package com.sni.bokaticowork.features.domiciliation.dto;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationAddress;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationContract;
import com.sni.bokaticowork.features.domiciliation.model.DomiciliationRegistration;
import com.sni.bokaticowork.features.domiciliation.model.MailItem;
import com.sni.bokaticowork.features.domiciliation.model.MailItemEvent;
import com.sni.bokaticowork.features.domiciliation.model.ServiceDefinition;
import com.sni.bokaticowork.features.domiciliation.model.SubscriptionService;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillingCycle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** Les contrats du catalogue de services et de la domiciliation. */
public final class DomiciliationDtos {

    private DomiciliationDtos() {
    }

    // ---- Catalogue ------------------------------------------------------------------------

    public record ServiceDefinitionRequest(
            @NotBlank String code, @NotBlank String name, String description,
            @NotNull ServiceDefinition.Category serviceCategory, ServiceDefinition.DeliveryMode deliveryMode,
            Boolean requiresContract, Integer requiresKycLevel, Boolean requiresPhysicalResource, Boolean hasRegulatoryObligations,
            BillingCycle defaultBillingCycle, Integer defaultNoticePeriodDays, Integer defaultCommitmentMonths,
            BigDecimal unitPrice, String currency, String usageEntitlementCode, Boolean active
    ) {
        public ServiceDefinition toEntity() {
            return ServiceDefinition.builder()
                    .code(code.trim()).name(name.trim()).description(description).serviceCategory(serviceCategory)
                    .deliveryMode(deliveryMode == null ? ServiceDefinition.DeliveryMode.CONTINUOUS : deliveryMode)
                    .requiresContract(Boolean.TRUE.equals(requiresContract)).requiresKycLevel(requiresKycLevel)
                    .requiresPhysicalResource(Boolean.TRUE.equals(requiresPhysicalResource))
                    .hasRegulatoryObligations(Boolean.TRUE.equals(hasRegulatoryObligations))
                    .defaultBillingCycle(defaultBillingCycle).defaultNoticePeriodDays(defaultNoticePeriodDays)
                    .defaultCommitmentMonths(defaultCommitmentMonths).unitPrice(unitPrice)
                    .currency(currency == null ? "XAF" : currency.trim().toUpperCase())
                    .usageEntitlementCode(usageEntitlementCode).active(active == null || active)
                    .build();
        }
    }

    public record ServiceDefinitionView(String code, String name, String description, ServiceDefinition.Category serviceCategory,
                                        ServiceDefinition.DeliveryMode deliveryMode, Boolean requiresContract, Integer requiresKycLevel,
                                        Boolean requiresPhysicalResource, Boolean hasRegulatoryObligations, BillingCycle defaultBillingCycle,
                                        Integer defaultNoticePeriodDays, Integer defaultCommitmentMonths, BigDecimal unitPrice, String currency,
                                        String usageEntitlementCode, Boolean active) {
        public static ServiceDefinitionView of(ServiceDefinition d) {
            return new ServiceDefinitionView(d.getCode(), d.getName(), d.getDescription(), d.getServiceCategory(), d.getDeliveryMode(),
                    d.getRequiresContract(), d.getRequiresKycLevel(), d.getRequiresPhysicalResource(), d.getHasRegulatoryObligations(),
                    d.getDefaultBillingCycle(), d.getDefaultNoticePeriodDays(), d.getDefaultCommitmentMonths(), d.getUnitPrice(),
                    d.getCurrency(), d.getUsageEntitlementCode(), d.getActive());
        }
    }

    public record SubscribeServiceRequest(@NotBlank String serviceCode, Integer quantity, BigDecimal unitPrice, String metadataJson) {
    }

    public record SubscriptionServiceView(String serviceNumber, String subscriptionNumber, String serviceCode, String serviceName,
                                          SubscriptionService.Status status, Integer quantity, BigDecimal unitPrice, String currency,
                                          Instant activatedAt, Instant suspendedAt, Instant terminatedAt, Instant createdAt) {
        public static SubscriptionServiceView of(SubscriptionService s) {
            return new SubscriptionServiceView(s.getServiceNumber(), s.getSubscription().getSubscriptionNumber(),
                    s.getServiceDefinition().getCode(), s.getServiceDefinition().getName(), s.getStatus(), s.getQuantity(),
                    s.getUnitPrice(), s.getCurrency(), s.getActivatedAt(), s.getSuspendedAt(), s.getTerminatedAt(), s.getCreatedAt());
        }
    }

    // ---- Registre des adresses -----------------------------------------------------------

    public record RegisterAddressRequest(@NotBlank String label, @NotNull AddressRequest address, Boolean fiscalCapable,
                                         Integer maxOccupants, String notes) {
    }

    public record UpdateAddressRequest(String label, Boolean fiscalCapable, Integer maxOccupants, Boolean active, String notes) {
    }

    public record AddressView(String code, String label, String streetNumber, String streetName, String district, String city,
                              String countryCode, Boolean fiscalCapable, Integer maxOccupants, long occupants, Boolean active, String notes) {
        public static AddressView of(DomiciliationAddress a, long occupants) {
            var address = a.getAddress();
            return new AddressView(a.getCode(), a.getLabel(), address.getStreetNumber(), address.getStreetName(), address.getDistrict(),
                    address.getCity(), address.getCountry() == null ? null : address.getCountry().getCountryCode(),
                    a.getFiscalCapable(), a.getMaxOccupants(), occupants, a.getActive(), a.getNotes());
        }
    }

    // ---- Contrat ----------------------------------------------------------------------------

    public record OpenContractRequest(
            @NotBlank String subscriptionNumber,
            @NotBlank String legalName, String legalForm, String registrationNumber, String taxNumber, String legalRepresentativeName,
            @NotBlank String addressCode, String suiteNumber,
            @NotNull BillingCycle billingCycle, Integer commitmentMonths, Integer noticePeriodDays, LocalDate startDate,
            DomiciliationContract.MailForwardingMode mailForwardingMode
    ) {
    }

    public record SignedRequest(@NotBlank String signedDocumentCode) {
    }

    public record SubmitRegistrationRequest(String administrationOffice, BigDecimal stampDutyAmount, BigDecimal registrationFeeAmount,
                                            DomiciliationRegistration.PaidBy paidBy, Boolean rebilled) {
    }

    public record ConfirmRegistrationRequest(@NotBlank String administrationReference, LocalDate registrationDate, LocalDate expiresAt,
                                             String receiptDocumentCode, @NotBlank String registeredDocumentCode) {
    }

    public record RejectRegistrationRequest(@NotBlank String reason) {
    }

    public record CertificateRequest(@NotNull DomiciliationContract.CertificateScope scope) {
    }

    public record CommitmentRequest(BillingCycle billingCycle, Integer commitmentMonths) {
    }

    public record TerminateRequest(@NotBlank String reason, LocalDate effectiveDate) {
    }

    public record ContractView(
            String contractNumber, String subscriptionNumber, String serviceNumber, String legalName, String legalForm,
            String registrationNumber, String taxNumber, String legalRepresentativeName,
            String addressCode, String addressLabel, String suiteNumber,
            BillingCycle billingCycle, Integer commitmentMonths, Boolean fiscalAddressEligible,
            Instant fiscalAddressGrantedAt, Instant fiscalAddressRevokedAt, String fiscalAddressRevocationReason,
            DomiciliationContract.Status status, LocalDate startDate, LocalDate endDate, Integer noticePeriodDays,
            String contractDocumentCode, String signedDocumentCode, Instant signedAt,
            String certificateDocumentCode, DomiciliationContract.CertificateScope certificateScope, Instant certificateIssuedAt,
            LocalDate certificateValidUntil, boolean certificateInForce, Instant certificateRevokedAt, String certificateRevocationReason,
            DomiciliationContract.MailForwardingMode mailForwardingMode,
            Instant terminatedAt, String terminationReason, LocalDate retainDocumentsUntil, Instant createdAt
    ) {
        public static ContractView of(DomiciliationContract c) {
            return new ContractView(c.getContractNumber(), c.getSubscription().getSubscriptionNumber(),
                    c.getSubscriptionService().getServiceNumber(), c.getLegalName(), c.getLegalForm(), c.getRegistrationNumber(),
                    c.getTaxNumber(), c.getLegalRepresentativeName(), c.getAssignedAddress().getCode(), c.getAssignedAddress().getLabel(),
                    c.getSuiteNumber(), c.getBillingCycle(), c.getCommitmentMonths(), c.getFiscalAddressEligible(),
                    c.getFiscalAddressGrantedAt(), c.getFiscalAddressRevokedAt(), c.getFiscalAddressRevocationReason(),
                    c.getStatus(), c.getStartDate(), c.getEndDate(), c.getNoticePeriodDays(), c.getContractDocumentCode(),
                    c.getSignedDocumentCode(), c.getSignedAt(), c.getCertificateDocumentCode(), c.getCertificateScope(),
                    c.getCertificateIssuedAt(), c.getCertificateValidUntil(), c.certificateInForce(), c.getCertificateRevokedAt(),
                    c.getCertificateRevocationReason(), c.getMailForwardingMode(), c.getTerminatedAt(), c.getTerminationReason(),
                    c.getRetainDocumentsUntil(), c.getCreatedAt());
        }
    }

    public record RegistrationView(String registrationNumber, String contractNumber, DomiciliationRegistration.Status status,
                                   Instant submittedAt, String submittedBy, String administrationOffice, String administrationReference,
                                   LocalDate registrationDate, BigDecimal stampDutyAmount, BigDecimal registrationFeeAmount,
                                   BigDecimal totalDutyAmount, String currency, DomiciliationRegistration.PaidBy paidBy, Boolean rebilled,
                                   String receiptDocumentCode, String registeredDocumentCode, LocalDate expiresAt, String rejectionReason,
                                   Instant createdAt) {
        public static RegistrationView of(DomiciliationRegistration r) {
            return new RegistrationView(r.getRegistrationNumber(), r.getContract().getContractNumber(), r.getStatus(), r.getSubmittedAt(),
                    r.getSubmittedBy(), r.getAdministrationOffice(), r.getAdministrationReference(), r.getRegistrationDate(),
                    r.getStampDutyAmount(), r.getRegistrationFeeAmount(), r.getTotalDutyAmount(), r.getCurrency(), r.getPaidBy(),
                    r.getRebilled(), r.getReceiptDocumentCode(), r.getRegisteredDocumentCode(), r.getExpiresAt(), r.getRejectionReason(),
                    r.getCreatedAt());
        }
    }

    // ---- Courrier ---------------------------------------------------------------------------

    public record ReceiveMailRequest(@NotNull MailItem.Type mailType, String senderName, String senderReference,
                                     Integer weightGrams, String dimensions, String notes) {
    }

    public record ScanRequest(@NotBlank String scanDocumentCode) {
    }

    public record CollectRequest(@NotBlank String collectedBy, String collectorIdDocument, String collectorSignatureUrl) {
    }

    public record ForwardRequest(String trackingNumber, BigDecimal transportCost) {
    }

    public record ReasonRequest(@NotBlank String reason) {
    }

    public record MailItemView(String itemNumber, String contractNumber, MailItem.Type mailType, String senderName,
                               String senderReference, Instant receivedAt, String receivedBy, Integer weightGrams, String dimensions,
                               MailItem.Status status, String scanDocumentCode, Instant notifiedAt, Instant collectedAt,
                               String collectedBy, String collectorIdDocument, String handedOverBy, Instant forwardedAt,
                               String forwardingTrackingNumber, BigDecimal forwardingCost, LocalDate storageDeadline,
                               Boolean billable, BigDecimal billedAmount, String notes) {
        public static MailItemView of(MailItem m) {
            return new MailItemView(m.getItemNumber(), m.getContract().getContractNumber(), m.getMailType(), m.getSenderName(),
                    m.getSenderReference(), m.getReceivedAt(), m.getReceivedBy(), m.getWeightGrams(), m.getDimensions(), m.getStatus(),
                    m.getScanDocumentCode(), m.getNotifiedAt(), m.getCollectedAt(), m.getCollectedBy(), m.getCollectorIdDocument(),
                    m.getHandedOverBy(), m.getForwardedAt(), m.getForwardingTrackingNumber(), m.getForwardingCost(),
                    m.getStorageDeadline(), m.getBillable(), m.getBilledAmount(), m.getNotes());
        }
    }

    public record MailEventView(String eventType, String actor, String details, Instant occurredAt) {
        public static MailEventView of(MailItemEvent e) {
            return new MailEventView(e.getEventType(), e.getActor(), e.getDetails(), e.getOccurredAt());
        }
    }
}
