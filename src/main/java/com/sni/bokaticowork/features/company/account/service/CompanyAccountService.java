package com.sni.bokaticowork.features.company.account.service;

import com.sni.bokaticowork.core.baseClasses.dto.request.AddressRequest;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.core.baseClasses.model.Currency;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.support.BillingReceivables;
import com.sni.bokaticowork.features.client.customer.dto.request.CustomerRequest;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.customer.repository.CustomerRepository;
import com.sni.bokaticowork.features.client.customer.service.interfaces.CustomerService;
import com.sni.bokaticowork.features.company.account.dto.CompanyDtos;
import com.sni.bokaticowork.features.company.enums.IdentifierType;
import com.sni.bokaticowork.features.company.enums.LegalForm;
import com.sni.bokaticowork.features.company.enums.OrganizationType;
import com.sni.bokaticowork.features.company.enums.Status;
import com.sni.bokaticowork.features.company.enums.VatRegime;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.model.BusinessEntityIdentifier;
import com.sni.bokaticowork.features.company.repository.BusinessEntityIdentifierRepository;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import com.sni.bokaticowork.features.company.service.support.OrganizationIdentifiers;
import com.sni.bokaticowork.features.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Le centre de gestion des entreprises · une organisation cliente reellement utilisable.
 *
 * <p>Une fiche, deux tables : le client {@code COMPANY} est le compte commercial (factures,
 * abonnements, reservations), {@code business_entity} son profil legal (type d'organisation,
 * identifiants, representant, regime de TVA). Elles se creent et se lisent ensemble, et la facture
 * lit la fiche au lieu de faire ressaisir ses identifiants · voir {@code OrganizationBillingProfile}.</p>
 *
 * <p>Tous les types d'organisation sont admis, et aucun identifiant n'est obligatoire : une
 * association n'a pas de RCCM, un ministere pas toujours de NIU. Un identifiant saisi est verifie
 * la ou son format est connu, et imprime ; un identifiant absent ne bloque rien et n'apparait pas.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CompanyAccountService {

    private static final String DEFAULT_CURRENCY = "XAF";

    private final CustomerService customerService;
    private final CustomerRepository customerRepository;
    private final BusinessRepository businessRepository;
    private final BusinessEntityIdentifierRepository identifierRepository;
    private final AddressService addressService;
    private final CurrencyService currencyService;
    private final BillingDocumentRepository documentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final com.sni.bokaticowork.features.company.membership.repository.CustomerMemberLinkRepository linkRepository;

    // ── Creation ─────────────────────────────────────────────────────────────

    public CompanyDtos.CompanyResponse create(CompanyDtos.CreateCompanyRequest request, String actor) {
        List<NormalizedIdentifier> identifiers = normalizeIdentifiers(request.identifiers(), null);
        String legalForm = resolveLegalForm(request.organizationType(), request.legalForm());

        Customer customer = customerService.createCustomer(CustomerRequest.builder()
                .type(CustomerType.COMPANY.name())
                .companyName(request.name().trim())
                .email(trimToNull(request.email()))
                .billingEmail(trimToNull(request.billingEmail()))
                .phone(request.phone())
                .address(request.address())
                .build());

        BusinessEntity legal = BusinessEntity.builder()
                .code(generateBusinessCode(request.name()))
                .name(request.name().trim())
                .organizationType(request.organizationType())
                .legalForm(legalForm)
                .activity(trimToNull(request.activity()))
                .legalRepresentativeName(trimToNull(request.legalRepresentativeName()))
                .legalRepresentativeTitle(trimToNull(request.legalRepresentativeTitle()))
                .vatRegime(request.vatRegime() == null ? VatRegime.SUBJECT : request.vatRegime())
                .vatExemptionNote(trimToNull(request.vatExemptionNote()))
                .phone(customer.getPhone())
                .email(customer.getEmail())
                .address(customer.getAddress())
                .baseCurrency(resolveCurrency(request.currencyCode()))
                .status(Status.ACTIVE)
                .created_by(actor)
                .build();
        ensureExemptionNote(legal);
        applyIdentifierColumns(legal, identifiers);
        businessRepository.save(legal);
        saveIdentifiers(legal, identifiers);

        customer.setBusinessEntity(legal);
        customer.setDefaultInvoiceLinkMode(com.sni.bokaticowork.features.company.enums.InvoiceLinkMode.ON_CHOICE);
        customer.setWebsite(trimToNull(request.website()));
        customer.setPaymentTermsDays(request.paymentTermsDays());
        customer.setCreditLimit(request.creditLimit());
        if (request.billingAddress() != null) {
            customer.setBillingAddress(addressService.createAddress(request.billingAddress()));
        }
        customerRepository.save(customer);
        return toResponse(customer);
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CompanyDtos.CompanyResponse get(String customerId) {
        return toResponse(company(customerId));
    }

    @Transactional(readOnly = true)
    public PaginatedResponse<CompanyDtos.CompanySummary> search(String q, String status, Pageable pageable) {
        String pattern = StringUtils.hasText(q) ? "%" + q.trim().toLowerCase(Locale.ROOT) + "%" : null;
        CustomerStatus customerStatus = parseStatus(status);
        return new PaginatedResponse<>(customerRepository.searchCompanies(pattern, customerStatus, pageable)
                .map(this::toSummary));
    }

    // ── Mise a jour par bloc ─────────────────────────────────────────────────

    public CompanyDtos.CompanyResponse updateLegal(String customerId, CompanyDtos.UpdateLegalRequest request, String actor) {
        Customer customer = company(customerId);
        BusinessEntity legal = legalProfile(customer, actor);
        if (request.organizationType() != null) {
            legal.setOrganizationType(request.organizationType());
        }
        if (StringUtils.hasText(request.name())) {
            legal.setName(request.name().trim());
            customer.setCompanyName(request.name().trim());
        }
        if (request.legalForm() != null || request.organizationType() != null) {
            legal.setLegalForm(resolveLegalForm(legal.getOrganizationType(),
                    request.legalForm() != null ? request.legalForm() : legal.getLegalForm()));
        }
        if (request.activity() != null) legal.setActivity(trimToNull(request.activity()));
        if (request.legalRepresentativeName() != null) legal.setLegalRepresentativeName(trimToNull(request.legalRepresentativeName()));
        if (request.legalRepresentativeTitle() != null) legal.setLegalRepresentativeTitle(trimToNull(request.legalRepresentativeTitle()));
        if (request.vatRegime() != null) legal.setVatRegime(request.vatRegime());
        if (request.vatExemptionNote() != null) legal.setVatExemptionNote(trimToNull(request.vatExemptionNote()));
        ensureExemptionNote(legal);
        if (request.identifiers() != null) {
            List<NormalizedIdentifier> identifiers = normalizeIdentifiers(request.identifiers(), legal);
            applyIdentifierColumns(legal, identifiers);
            identifierRepository.deleteAllByBusinessEntity(legal);
            identifierRepository.flush();
            saveIdentifiers(legal, identifiers);
        }
        legal.setUpdated_by(actor);
        businessRepository.save(legal);
        customerRepository.save(customer);
        return toResponse(customer);
    }

    public CompanyDtos.CompanyResponse updateBilling(String customerId, CompanyDtos.UpdateBillingRequest request) {
        Customer customer = company(customerId);
        if (request.billingEmail() != null) customer.setBillingEmail(trimToNull(request.billingEmail()));
        if (request.paymentTermsDays() != null) customer.setPaymentTermsDays(request.paymentTermsDays());
        if (Boolean.TRUE.equals(request.removeCreditLimit())) {
            customer.setCreditLimit(null);
        } else if (request.creditLimit() != null) {
            customer.setCreditLimit(request.creditLimit());
        }
        if (request.billingAddress() != null) {
            customer.setBillingAddress(saveAddress(customer.getBillingAddress(), request.billingAddress()));
        }
        customerRepository.save(customer);
        return toResponse(customer);
    }

    public CompanyDtos.CompanyResponse updateContact(String customerId, CompanyDtos.UpdateContactRequest request) {
        Customer customer = company(customerId);
        if (request.email() != null) {
            String email = trimToNull(request.email());
            if (email != null && !email.equalsIgnoreCase(customer.getEmail())
                    && Boolean.TRUE.equals(customerRepository.existsByEmailAndIdNot(email.toLowerCase(Locale.ROOT), customer.getId()))) {
                throw new BadRequestException("Un autre client utilise déjà le courriel " + email + ".");
            }
            customer.setEmail(email == null ? null : email.toLowerCase(Locale.ROOT));
        }
        if (StringUtils.hasText(request.phone())) customer.setPhone(request.phone().trim());
        if (request.website() != null) customer.setWebsite(trimToNull(request.website()));
        if (request.address() != null) {
            customer.setAddress(saveAddress(customer.getAddress(), request.address()));
        }
        customerRepository.save(customer);
        return toResponse(customer);
    }

    // ── Statut motive ────────────────────────────────────────────────────────

    /** Bloque l'organisation · plus aucune facture ne lui est emise, avec le motif dans le refus. */
    public CompanyDtos.CompanyResponse block(String customerId, String reason, String actor) {
        Customer customer = company(customerId);
        customer.setStatus(CustomerStatus.SUSPENDED);
        customer.setBlockedReason(reason.trim());
        customer.setBlockedBy(actor);
        customer.setBlockedAt(Instant.now());
        customerRepository.save(customer);
        return toResponse(customer);
    }

    public CompanyDtos.CompanyResponse unblock(String customerId) {
        Customer customer = company(customerId);
        if (customer.getStatus() != CustomerStatus.SUSPENDED) {
            throw new BadRequestException("L'organisation n'est pas bloquée.");
        }
        customer.setStatus(CustomerStatus.ACTIVE);
        customer.setBlockedReason(null);
        customer.setBlockedBy(null);
        customer.setBlockedAt(null);
        customerRepository.save(customer);
        return toResponse(customer);
    }

    /** Relie un profil legal existant · pour rapprocher une fiche creee avant le centre de gestion. */
    public CompanyDtos.CompanyResponse linkLegalProfile(String customerId, String businessCode) {
        Customer customer = company(customerId);
        BusinessEntity legal = businessRepository.findByCode(businessCode.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Profil légal introuvable : " + businessCode));
        if (legal.isOperator()) {
            throw new BadRequestException("Ce profil est celui de l'exploitant de l'espace, pas d'un client.");
        }
        customerRepository.findFirstByBusinessEntity_IdAndIdNot(legal.getId(), customer.getId())
                .ifPresent(other -> {
                    throw new BadRequestException("Ce profil légal est déjà relié au client " + other.getCustomerId() + ".");
                });
        customer.setBusinessEntity(legal);
        customerRepository.save(customer);
        return toResponse(customer);
    }

    // ── Identifiants ─────────────────────────────────────────────────────────

    private record NormalizedIdentifier(IdentifierType type, String label, String value) {
    }

    /**
     * Verifie et range les identifiants saisis · tous les refus a la fois, chacun avec le format attendu.
     */
    private List<NormalizedIdentifier> normalizeIdentifiers(List<CompanyDtos.IdentifierRequest> requested, BusinessEntity self) {
        if (requested == null) {
            return List.of();
        }
        List<NormalizedIdentifier> out = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<IdentifierType> seen = EnumSet.noneOf(IdentifierType.class);
        for (CompanyDtos.IdentifierRequest identifier : requested) {
            if (identifier == null || identifier.type() == null || !StringUtils.hasText(identifier.value())) {
                continue;
            }
            IdentifierType type = identifier.type();
            if (type != IdentifierType.AUTRE && !seen.add(type)) {
                errors.add("L'identifiant " + type.getDefaultLabel() + " est saisi deux fois.");
                continue;
            }
            String value = OrganizationIdentifiers.normalize(type, identifier.value());
            if (OrganizationIdentifiers.check(type, value) == OrganizationIdentifiers.Verdict.INVALID) {
                errors.add(type.getDefaultLabel() + " « " + identifier.value().trim() + " » non reconnu : "
                        + OrganizationIdentifiers.expected(type) + ".");
                continue;
            }
            if (type == IdentifierType.AUTRE && !StringUtils.hasText(identifier.label())) {
                errors.add("Un identifiant « autre » doit porter son libellé.");
                continue;
            }
            if (type == IdentifierType.NIU) {
                businessRepository.findByNiuNumber(value)
                        .filter(other -> self == null || !other.getId().equals(self.getId()))
                        .ifPresent(other -> errors.add("Le NIU " + value + " est déjà celui de " + other.getName() + "."));
            }
            if (type == IdentifierType.RCCM) {
                businessRepository.findByRccmNumber(value)
                        .filter(other -> self == null || !other.getId().equals(self.getId()))
                        .ifPresent(other -> errors.add("Le RCCM " + value + " est déjà celui de " + other.getName() + "."));
            }
            String label = StringUtils.hasText(identifier.label()) ? identifier.label().trim() : type.getDefaultLabel();
            out.add(new NormalizedIdentifier(type, label, value));
        }
        if (!errors.isEmpty()) {
            throw new BadRequestException(String.join(" ", errors));
        }
        return out;
    }

    /** Les colonnes historiques suivent la table · leurs lecteurs actuels continuent de fonctionner. */
    private static void applyIdentifierColumns(BusinessEntity legal, List<NormalizedIdentifier> identifiers) {
        legal.setNiuNumber(identifiers.stream().filter(i -> i.type() == IdentifierType.NIU)
                .map(NormalizedIdentifier::value).findFirst().orElse(null));
        legal.setRccmNumber(identifiers.stream().filter(i -> i.type() == IdentifierType.RCCM)
                .map(NormalizedIdentifier::value).findFirst().orElse(null));
    }

    private void saveIdentifiers(BusinessEntity legal, List<NormalizedIdentifier> identifiers) {
        for (NormalizedIdentifier identifier : identifiers) {
            identifierRepository.save(BusinessEntityIdentifier.builder()
                    .businessEntity(legal)
                    .type(identifier.type())
                    .label(identifier.label())
                    .value(identifier.value())
                    .build());
        }
    }

    // ── Aides ────────────────────────────────────────────────────────────────

    private Customer company(String customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId == null ? "" : customerId.trim())
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Organisation introuvable : " + customerId));
        if (customer.getType() != CustomerType.COMPANY) {
            throw new BadRequestException("Le client " + customerId + " est une personne, pas une organisation.");
        }
        return customer;
    }

    /** Le profil legal du client · cree a la volee pour une fiche anterieure au centre de gestion. */
    private BusinessEntity legalProfile(Customer customer, String actor) {
        if (customer.getBusinessEntity() != null) {
            return customer.getBusinessEntity();
        }
        BusinessEntity legal = BusinessEntity.builder()
                .code(generateBusinessCode(customer.getCompanyName()))
                .name(StringUtils.hasText(customer.getCompanyName()) ? customer.getCompanyName() : customer.getCustomerId())
                .organizationType(OrganizationType.SOCIETE)
                .legalForm(OrganizationType.SOCIETE.name())
                .phone(StringUtils.hasText(customer.getPhone()) ? customer.getPhone() : "-")
                .email(customer.getEmail())
                .address(customer.getAddress())
                .baseCurrency(resolveCurrency(null))
                .status(Status.ACTIVE)
                .created_by(actor)
                .build();
        businessRepository.save(legal);
        customer.setBusinessEntity(legal);
        return legal;
    }

    private static void ensureExemptionNote(BusinessEntity legal) {
        if (legal.getVatRegime() == VatRegime.EXEMPT && !StringUtils.hasText(legal.getVatExemptionNote())) {
            throw new BadRequestException("Une organisation exonérée de TVA doit porter sa mention d'exonération "
                    + "(texte ou décision), imprimée sur ses factures.");
        }
    }

    /** La forme juridique precise d'une societe, verifiee · le type d'organisation sinon. */
    private static String resolveLegalForm(OrganizationType type, String legalForm) {
        if (!StringUtils.hasText(legalForm)) {
            return type.name();
        }
        String upper = legalForm.trim().toUpperCase(Locale.ROOT);
        if (type == OrganizationType.SOCIETE) {
            try {
                return LegalForm.valueOf(upper).name();
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Forme juridique inconnue : " + legalForm
                        + ". Formes admises : SARL, SARLU, SA, SAU, SAS, SASU, SNC, SCS, SP, SCOOP, SCI.");
            }
        }
        return upper.length() > 60 ? upper.substring(0, 60) : upper;
    }

    private Currency resolveCurrency(String code) {
        String currency = StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : DEFAULT_CURRENCY;
        try {
            return currencyService.serviceCurrencyByCode(currency);
        } catch (ResourceNotFoundException ex) {
            throw new BadRequestException("Devise inconnue : " + currency, ex);
        }
    }

    private Address saveAddress(Address current, AddressRequest request) {
        return current == null ? addressService.createAddress(request) : addressService.updateAddress(current.getId(), request);
    }

    private String generateBusinessCode(String name) {
        String letters = name == null ? "" : name.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        String prefix = letters.length() < 3 ? (letters + "ORG").substring(0, 3) : letters.substring(0, 3);
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String code;
        do {
            code = prefix + date + ThreadLocalRandom.current().nextInt(100, 1000);
        } while (businessRepository.existsByCode(code));
        return code;
    }

    private static CustomerStatus parseStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }
        try {
            return CustomerStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Statut inconnu : " + status);
        }
    }

    private BigDecimal outstanding(Customer customer) {
        Set<BillingDocumentStatus> excluded = new HashSet<>(BillingReceivables.NOT_ISSUED);
        excluded.addAll(BillingReceivables.SETTLED);
        return documentRepository.sumOutstanding("CUSTOMER", customer.getCustomerId(),
                BillingReceivables.RECEIVABLE_TYPES, excluded);
    }

    private CompanyDtos.CompanyResponse toResponse(Customer customer) {
        BusinessEntity legal = customer.getBusinessEntity();
        List<CompanyDtos.IdentifierResponse> identifiers = new ArrayList<>();
        if (legal != null) {
            for (BusinessEntityIdentifier identifier : identifierRepository.findAllByBusinessEntityOrderByIdAsc(legal)) {
                OrganizationIdentifiers.Verdict verdict = OrganizationIdentifiers.check(identifier.getType(), identifier.getValue());
                identifiers.add(new CompanyDtos.IdentifierResponse(identifier.getType(), identifier.getLabel(),
                        identifier.getValue(), verdict == OrganizationIdentifiers.Verdict.LEGACY ? "LEGACY" : "STANDARD"));
            }
        }
        OrganizationType type = legal == null ? null : legal.getOrganizationType();
        return new CompanyDtos.CompanyResponse(
                customer.getCustomerId(),
                customer.getCompanyName(),
                type,
                type == null ? null : type.getLabel(),
                legal == null ? null : legal.getLegalForm(),
                legal == null ? null : legal.getActivity(),
                identifiers,
                type == null ? List.of() : type.getSuggestedIdentifiers(),
                legal == null ? null : legal.getLegalRepresentativeName(),
                legal == null ? null : legal.getLegalRepresentativeTitle(),
                legal == null ? null : legal.getVatRegime(),
                legal == null ? null : legal.getVatExemptionNote(),
                customer.getEmail(),
                customer.getBillingEmail(),
                customer.getPhone(),
                customer.getWebsite(),
                address(customer.getAddress()),
                address(customer.getBillingAddress()),
                customer.getPaymentTermsDays(),
                customer.getCreditLimit(),
                outstanding(customer),
                customer.getStatus() == null ? null : customer.getStatus().name(),
                customer.getBlockedReason(),
                customer.getBlockedBy(),
                customer.getBlockedAt(),
                legal == null ? null : legal.getCode(),
                // Les membres rattaches · le payeur par defaut n'est plus le critere depuis la tranche 1.
                linkRepository.countByCustomerAndStatus(customer, com.sni.bokaticowork.features.company.enums.MemberLinkStatus.ACTIVE),
                subscriptionRepository.findAllCurrentActive("CUSTOMER", customer.getCustomerId(), LocalDate.now()).size(),
                customer.getCreatedAt());
    }

    private CompanyDtos.CompanySummary toSummary(Customer customer) {
        BusinessEntity legal = customer.getBusinessEntity();
        return new CompanyDtos.CompanySummary(
                customer.getCustomerId(),
                customer.getCompanyName(),
                legal == null ? null : legal.getOrganizationType(),
                legal == null ? null : legal.getNiuNumber(),
                legal == null ? null : legal.getRccmNumber(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getStatus() == null ? null : customer.getStatus().name(),
                outstanding(customer));
    }

    private static CompanyDtos.AddressResponse address(Address address) {
        if (address == null) {
            return null;
        }
        return new CompanyDtos.AddressResponse(address.getStreetNumber(), address.getStreetName(), address.getDistrict(),
                address.getCity(), address.getCountry() == null ? null : address.getCountry().getName());
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
