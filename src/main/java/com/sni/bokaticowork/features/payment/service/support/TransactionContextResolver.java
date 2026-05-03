package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.model.BillingDocumentLine;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentLineRepository;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.booking.model.Booking;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.repository.PassRepository;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.features.subscription.subscription.model.Pass;
import com.sni.bokaticowork.features.subscription.subscription.model.Subscription;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class TransactionContextResolver {

    private static final String BILLING_DOCUMENT_SOURCE = "BILLING_DOCUMENT";
    private static final String MULTI_BILLING_DOCUMENT_SOURCE = "MULTI_BILLING_DOCUMENT";
    private static final String BILLABLE_ITEM_SOURCE = "BILLABLE_ITEM";

    private final MemberRepository memberRepository;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BillingDocumentLineRepository billingDocumentLineRepository;
    private final BillableItemRepository billableItemRepository;
    private final BookingRepository bookingRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PassRepository passRepository;

    public PartyView resolveParty(String customerType, String customerCode) {
        if (!StringUtils.hasText(customerType) || !StringUtils.hasText(customerCode)) {
            return PartyView.empty();
        }
        String normalizedType = customerType.trim().toUpperCase(Locale.ROOT);
        String normalizedCode = customerCode.trim();
        return switch (normalizedType) {
            case "MEMBER" -> memberRepository.findByMemberIdAndDeletedFalse(normalizedCode)
                    .map(this::memberParty)
                    .orElse(PartyView.unregistered(normalizedType, normalizedCode));
            case "CUSTOMER" -> customerRepository.findByCustomerId(normalizedCode)
                    .map(this::customerParty)
                    .orElse(PartyView.unregistered(normalizedType, normalizedCode));
            case "BUSINESS_ENTITY", "BUSINESS", "COMPANY" -> businessRepository.findByCode(normalizedCode)
                    .map(this::businessParty)
                    .orElse(PartyView.unregistered(normalizedType, normalizedCode));
            default -> PartyView.unregistered(normalizedType, normalizedCode);
        };
    }

    public SourceView resolveSource(String sourceType, String sourceCode) {
        return resolveSource(sourceType, sourceCode, 0);
    }

    public SourceView resolveBillingDocumentSource(BillingDocument document) {
        if (document == null) {
            return SourceView.empty();
        }
        List<BillingDocumentLine> lines = billingDocumentLineRepository.findAllByDocumentOrderByLineOrderAscIdAsc(document);
        Set<SourceKey> distinct = new LinkedHashSet<>();
        lines.stream()
                .filter(line -> StringUtils.hasText(line.getSourceType()) && StringUtils.hasText(line.getSourceCode()))
                .forEach(line -> distinct.add(new SourceKey(line.getSourceType().trim(), line.getSourceCode().trim())));

        if (distinct.size() == 1) {
            SourceKey key = distinct.iterator().next();
            return resolveSource(key.type(), key.code(), 1);
        }
        if (distinct.size() > 1) {
            return new SourceView("MULTIPLE", document.getDocumentNumber(), "Sources multiples", false);
        }
        return resolveSource(document.getSourceType(), document.getSourceCode(), 1);
    }

    private SourceView resolveSource(String sourceType, String sourceCode, int depth) {
        if (!StringUtils.hasText(sourceType) && !StringUtils.hasText(sourceCode)) {
            return SourceView.empty();
        }
        if (depth > 4) {
            return new SourceView(clean(sourceType), clean(sourceCode), genericLabel(sourceType, sourceCode), false);
        }

        String normalizedType = clean(sourceType);
        String normalizedCode = clean(sourceCode);
        String upperType = normalizedType == null ? null : normalizedType.toUpperCase(Locale.ROOT);

        if (BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(upperType) && StringUtils.hasText(normalizedCode)) {
            return billingDocumentRepository.findByDocumentNumber(normalizedCode)
                    .map(this::resolveBillingDocumentSource)
                    .orElse(new SourceView(normalizedType, normalizedCode, genericLabel(normalizedType, normalizedCode), false));
        }
        if (MULTI_BILLING_DOCUMENT_SOURCE.equalsIgnoreCase(upperType) && StringUtils.hasText(normalizedCode)) {
            List<String> documentNumbers = splitCodes(normalizedCode);
            Set<SourceKey> distinct = new LinkedHashSet<>();
            for (String documentNumber : documentNumbers) {
                billingDocumentRepository.findByDocumentNumber(documentNumber)
                        .map(this::resolveBillingDocumentSource)
                        .filter(view -> StringUtils.hasText(view.type()) && StringUtils.hasText(view.code()))
                        .ifPresent(view -> distinct.add(new SourceKey(view.type(), view.code())));
            }
            if (distinct.size() == 1) {
                SourceKey key = distinct.iterator().next();
                return resolveSource(key.type(), key.code(), depth + 1);
            }
            return new SourceView(normalizedType, normalizedCode, "Paiement de plusieurs factures", false);
        }
        if (BILLABLE_ITEM_SOURCE.equalsIgnoreCase(upperType) && StringUtils.hasText(normalizedCode)) {
            List<String> billableNumbers = splitCodes(normalizedCode);
            Set<SourceKey> distinct = new LinkedHashSet<>();
            for (String billableNumber : billableNumbers) {
                billableItemRepository.findByBillableNumber(billableNumber)
                        .ifPresent(item -> {
                            if (StringUtils.hasText(item.getSourceType()) && StringUtils.hasText(item.getSourceId())) {
                                distinct.add(new SourceKey(item.getSourceType().trim(), item.getSourceId().trim()));
                            }
                        });
            }
            if (distinct.size() == 1) {
                SourceKey key = distinct.iterator().next();
                return resolveSource(key.type(), key.code(), depth + 1);
            }
            return new SourceView(normalizedType, normalizedCode, "Billable items", false);
        }
        if ("BOOKING".equalsIgnoreCase(upperType) && StringUtils.hasText(normalizedCode)) {
            return bookingRepository.findByBookingNumber(normalizedCode)
                    .map(this::bookingSource)
                    .orElse(new SourceView(normalizedType, normalizedCode, genericLabel(normalizedType, normalizedCode), false));
        }
        if ("PASS".equalsIgnoreCase(upperType) && StringUtils.hasText(normalizedCode)) {
            return passRepository.findByPassNumber(normalizedCode)
                    .map(this::passSource)
                    .orElse(new SourceView(normalizedType, normalizedCode, genericLabel(normalizedType, normalizedCode), false));
        }
        if (upperType != null && upperType.contains("SUBSCRIPTION") && StringUtils.hasText(normalizedCode)) {
            return subscriptionRepository.findBySubscriptionNumber(normalizedCode)
                    .map(this::subscriptionSource)
                    .orElse(new SourceView(normalizedType, normalizedCode, genericLabel(normalizedType, normalizedCode), false));
        }
        return new SourceView(normalizedType, normalizedCode, genericLabel(normalizedType, normalizedCode), false);
    }

    private PartyView memberParty(Member member) {
        return new PartyView("MEMBER", member.getMemberId(), clean(member.getDisplayName()), clean(member.getEmail()), clean(member.getPhone()), null, true);
    }

    private PartyView customerParty(Customer customer) {
        String name = customer.getType() == CustomerType.COMPANY
                ? clean(customer.getCompanyName())
                : clean(((customer.getFirstname() == null ? "" : customer.getFirstname()) + " " + (customer.getLastname() == null ? "" : customer.getLastname())).trim());
        String email = clean(customer.getBillingEmail() != null ? customer.getBillingEmail() : customer.getEmail());
        return new PartyView("CUSTOMER", customer.getCustomerId(), name, email, clean(customer.getPhone()), null, true);
    }

    private PartyView businessParty(BusinessEntity businessEntity) {
        return new PartyView("BUSINESS_ENTITY", businessEntity.getCode(), clean(businessEntity.getName()), clean(businessEntity.getEmail()), clean(businessEntity.getPhone()), null, true);
    }

    private SourceView bookingSource(Booking booking) {
        String label = "Reservation " + booking.getBookingNumber()
                + (booking.getResource() != null && StringUtils.hasText(booking.getResource().getName()) ? " - " + booking.getResource().getName() : "");
        return new SourceView("BOOKING", booking.getBookingNumber(), label, true);
    }

    private SourceView subscriptionSource(Subscription subscription) {
        String label = "Abonnement " + subscription.getSubscriptionNumber()
                + (subscription.getPlanVersion() != null && StringUtils.hasText(subscription.getPlanVersion().getName()) ? " - " + subscription.getPlanVersion().getName() : "");
        return new SourceView("SUBSCRIPTION", subscription.getSubscriptionNumber(), label, true);
    }

    private SourceView passSource(Pass pass) {
        String label = "Pass " + pass.getPassNumber()
                + (StringUtils.hasText(pass.getName()) ? " - " + pass.getName().trim() : "");
        return new SourceView("PASS", pass.getPassNumber(), label, true);
    }

    private List<String> splitCodes(String sourceCode) {
        return sourceCode == null ? List.of() : java.util.Arrays.stream(sourceCode.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();
    }

    private String genericLabel(String sourceType, String sourceCode) {
        if (!StringUtils.hasText(sourceType) && !StringUtils.hasText(sourceCode)) {
            return null;
        }
        if (!StringUtils.hasText(sourceType)) {
            return sourceCode;
        }
        if (!StringUtils.hasText(sourceCode)) {
            return humanize(sourceType);
        }
        return humanize(sourceType) + " " + sourceCode.trim();
    }

    private String clean(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String humanize(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String normalized = raw.trim().replace('_', ' ');
        normalized = normalized.replaceAll("([a-z])([A-Z])", "$1 $2");
        String lower = normalized.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private record SourceKey(String type, String code) {
    }

    public record PartyView(
            String type,
            String code,
            String name,
            String email,
            String phone,
            String billingAddressJson,
            boolean registered
    ) {
        public static PartyView empty() {
            return new PartyView(null, null, null, null, null, null, false);
        }

        public static PartyView unregistered(String type, String code) {
            return new PartyView(type, code, null, null, null, null, false);
        }
    }

    public record SourceView(
            String type,
            String code,
            String label,
            boolean registered
    ) {
        public static SourceView empty() {
            return new SourceView(null, null, null, false);
        }
    }
}
