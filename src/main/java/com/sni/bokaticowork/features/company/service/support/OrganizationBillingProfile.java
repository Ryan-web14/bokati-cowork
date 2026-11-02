package com.sni.bokaticowork.features.company.service.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.company.enums.IdentifierType;
import com.sni.bokaticowork.features.company.enums.VatRegime;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.model.BusinessEntityIdentifier;
import com.sni.bokaticowork.features.company.repository.BusinessEntityIdentifierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ce que la fiche d'une organisation impose a sa facture · sans rien a saisir.
 *
 * <p>Une entreprise n'etait qu'un nom pour la facturation : son NIU n'arrivait sur la facture que
 * s'il etait retape dans la demande, et son RCCM, son exoneration de TVA, son delai de paiement et
 * son adresse de facturation n'y arrivaient jamais. Ils sont lus ici, sur la fiche, au moment de
 * l'emission ; seuls les identifiants saisis sont repris.</p>
 */
@Component
@RequiredArgsConstructor
public class OrganizationBillingProfile {

    private final BusinessEntityIdentifierRepository identifierRepository;
    private final ObjectMapper objectMapper;

    /** Les conditions de facturation d'une organisation · fige sur la facture a l'emission. */
    public record Terms(String customerCode,
                        String customerName,
                        String niu,
                        String identifiersJson,
                        boolean vatExempt,
                        String vatExemptionNote,
                        Integer paymentTermsDays,
                        BigDecimal creditLimit,
                        boolean blocked,
                        String blockedReason,
                        String billingEmail,
                        String billingAddressJson) {
    }

    /** Une ligne d'identifiant imprimee · libelle et valeur. */
    public record IdentifierLine(String type, String label, String value) {
    }

    /** Vide pour une personne · ses factures ne changent pas. */
    public Optional<Terms> of(Customer customer) {
        if (customer == null || customer.getType() != CustomerType.COMPANY) {
            return Optional.empty();
        }
        BusinessEntity legal = customer.getBusinessEntity();
        List<IdentifierLine> identifiers = legal == null ? List.of() : identifiers(legal);
        String niu = identifiers.stream().filter(line -> IdentifierType.NIU.name().equals(line.type()))
                .map(IdentifierLine::value).findFirst().orElse(null);
        boolean exempt = legal != null && legal.getVatRegime() == VatRegime.EXEMPT;
        Address billingAddress = customer.getBillingAddress() != null ? customer.getBillingAddress() : customer.getAddress();
        return Optional.of(new Terms(
                customer.getCustomerId(),
                customer.getCompanyName(),
                niu,
                identifiers.isEmpty() ? null : write(identifiers),
                exempt,
                exempt && legal != null ? trimToNull(legal.getVatExemptionNote()) : null,
                customer.getPaymentTermsDays(),
                customer.getCreditLimit(),
                customer.getStatus() == CustomerStatus.SUSPENDED,
                trimToNull(customer.getBlockedReason()),
                StringUtils.hasText(customer.getBillingEmail()) ? customer.getBillingEmail().trim() : null,
                addressJson(billingAddress)
        ));
    }

    /** Les identifiants saisis, dans l'ordre de saisie · le NIU et le RCCM d'abord. */
    public List<IdentifierLine> identifiers(BusinessEntity legal) {
        List<IdentifierLine> lines = new ArrayList<>();
        for (BusinessEntityIdentifier identifier : identifierRepository.findAllByBusinessEntityOrderByIdAsc(legal)) {
            if (!StringUtils.hasText(identifier.getValue())) {
                continue;
            }
            String label = StringUtils.hasText(identifier.getLabel()) ? identifier.getLabel().trim()
                    : identifier.getType().getDefaultLabel();
            lines.add(new IdentifierLine(identifier.getType().name(), label, identifier.getValue().trim()));
        }
        // Une fiche anterieure a la table des identifiants n'a que ses colonnes · on les reprend.
        if (lines.stream().noneMatch(l -> IdentifierType.NIU.name().equals(l.type())) && StringUtils.hasText(legal.getNiuNumber())) {
            lines.add(0, new IdentifierLine(IdentifierType.NIU.name(), "NIU", legal.getNiuNumber().trim()));
        }
        if (lines.stream().noneMatch(l -> IdentifierType.RCCM.name().equals(l.type())) && StringUtils.hasText(legal.getRccmNumber())) {
            lines.add(new IdentifierLine(IdentifierType.RCCM.name(), "RCCM", legal.getRccmNumber().trim()));
        }
        lines.sort((a, b) -> Integer.compare(rank(a.type()), rank(b.type())));
        return lines;
    }

    /** Relit les lignes figees sur une facture · pour le gabarit PDF. */
    public List<IdentifierLine> read(String identifiersJson) {
        if (!StringUtils.hasText(identifiersJson)) {
            return List.of();
        }
        try {
            return List.of(objectMapper.readValue(identifiersJson, IdentifierLine[].class));
        } catch (JsonProcessingException ex) {
            return List.of();
        }
    }

    private static int rank(String type) {
        return IdentifierType.NIU.name().equals(type) ? 0 : IdentifierType.RCCM.name().equals(type) ? 1 : 2;
    }

    private String addressJson(Address address) {
        if (address == null) {
            return null;
        }
        Map<String, Object> json = new LinkedHashMap<>();
        String street = String.join(" ", nonBlank(address.getStreetNumber(), address.getStreetName())).trim();
        if (!street.isEmpty()) json.put("rue", street);
        if (StringUtils.hasText(address.getDistrict())) json.put("quartier", address.getDistrict().trim());
        if (StringUtils.hasText(address.getCity())) json.put("ville", address.getCity().trim());
        if (address.getCountry() != null && StringUtils.hasText(address.getCountry().getName())) {
            json.put("pays", address.getCountry().getName().trim());
        }
        return json.isEmpty() ? null : write(json);
    }

    private static List<String> nonBlank(String... values) {
        List<String> out = new ArrayList<>();
        for (String value : values) {
            if (StringUtils.hasText(value)) out.add(value.trim());
        }
        return out;
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Profil de facturation non serialisable", ex);
        }
    }

    private static String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
