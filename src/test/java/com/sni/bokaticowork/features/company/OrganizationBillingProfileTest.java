package com.sni.bokaticowork.features.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.client.customer.enums.CustomerStatus;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.company.enums.IdentifierType;
import com.sni.bokaticowork.features.company.enums.OrganizationType;
import com.sni.bokaticowork.features.company.enums.VatRegime;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.model.BusinessEntityIdentifier;
import com.sni.bokaticowork.features.company.repository.BusinessEntityIdentifierRepository;
import com.sni.bokaticowork.features.company.service.support.OrganizationBillingProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Ce que la facture tire de la fiche d'une organisation · sans rien a ressaisir.
 */
class OrganizationBillingProfileTest {

    private final BusinessEntityIdentifierRepository identifiers = mock(BusinessEntityIdentifierRepository.class);
    private final OrganizationBillingProfile profile = new OrganizationBillingProfile(identifiers, new ObjectMapper());

    private Customer company(BusinessEntity legal) {
        Customer customer = Customer.builder().customerId("CUS-COM-1").type(CustomerType.COMPANY)
                .companyName("Association Mboté").status(CustomerStatus.ACTIVE)
                .billingEmail("compta@mbote.cg").paymentTermsDays(30).creditLimit(new BigDecimal("500000"))
                .businessEntity(legal).build();
        return customer;
    }

    @Test
    @DisplayName("Seuls les identifiants saisis sont repris, NIU et RCCM en tete")
    void onlyEnteredIdentifiers() throws Exception {
        BusinessEntity legal = BusinessEntity.builder().organizationType(OrganizationType.ASSOCIATION)
                .vatRegime(VatRegime.SUBJECT).build();
        when(identifiers.findAllByBusinessEntityOrderByIdAsc(legal)).thenReturn(List.of(
                BusinessEntityIdentifier.builder().type(IdentifierType.RECEPISSE).label("N° de récépissé").value("245/MID/DGAT").build(),
                BusinessEntityIdentifier.builder().type(IdentifierType.NIU).label("NIU").value("P200711000047346").build()));

        var terms = profile.of(company(legal)).orElseThrow();

        assertThat(terms.niu()).isEqualTo("P200711000047346");
        assertThat(profile.read(terms.identifiersJson())).extracting(OrganizationBillingProfile.IdentifierLine::type)
                .containsExactly("NIU", "RECEPISSE");
        assertThat(terms.paymentTermsDays()).isEqualTo(30);
        assertThat(terms.billingEmail()).isEqualTo("compta@mbote.cg");
        assertThat(terms.vatExempt()).isFalse();
    }

    @Test
    @DisplayName("Sans identifiant saisi, rien n'est imprime")
    void noIdentifierNothingPrinted() {
        BusinessEntity legal = BusinessEntity.builder().organizationType(OrganizationType.INSTITUTION_PUBLIQUE)
                .vatRegime(VatRegime.SUBJECT).build();
        when(identifiers.findAllByBusinessEntityOrderByIdAsc(legal)).thenReturn(List.of());

        var terms = profile.of(company(legal)).orElseThrow();

        assertThat(terms.niu()).isNull();
        assertThat(terms.identifiersJson()).isNull();
    }

    @Test
    @DisplayName("Exoneree : la mention suit ; bloquee : le motif suit")
    void exemptAndBlocked() {
        BusinessEntity legal = BusinessEntity.builder().organizationType(OrganizationType.ONG)
                .vatRegime(VatRegime.EXEMPT).vatExemptionNote("Accord d'établissement n° 12/2021").build();
        when(identifiers.findAllByBusinessEntityOrderByIdAsc(legal)).thenReturn(List.of());
        Customer customer = company(legal);
        customer.setStatus(CustomerStatus.SUSPENDED);
        customer.setBlockedReason("Impayés de septembre");

        var terms = profile.of(customer).orElseThrow();

        assertThat(terms.vatExempt()).isTrue();
        assertThat(terms.vatExemptionNote()).isEqualTo("Accord d'établissement n° 12/2021");
        assertThat(terms.blocked()).isTrue();
        assertThat(terms.blockedReason()).isEqualTo("Impayés de septembre");
    }

    @Test
    @DisplayName("Une personne n'a pas de conditions d'organisation")
    void personHasNoTerms() {
        Customer person = Customer.builder().customerId("CUS-PER-1").type(CustomerType.PERSON).build();
        assertThat(profile.of(person)).isEmpty();
    }
}
