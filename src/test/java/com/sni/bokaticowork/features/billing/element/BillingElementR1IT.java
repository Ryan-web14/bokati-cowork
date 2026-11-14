package com.sni.bokaticowork.features.billing.element;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.features.billing.element.model.BillingDocumentElement;
import com.sni.bokaticowork.features.billing.element.model.ElementRef;
import com.sni.bokaticowork.features.billing.element.repository.BillingDocumentElementRepository;
import com.sni.bokaticowork.features.billing.element.service.BillingElementResolver;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Lot R1 sur une vraie base : la table des liens passe la migration et la validation du schema, et
 * chaque requete du resolveur s'execute sur les tables reelles (abonnements, pass, reservations,
 * domiciliations, tiers, factures).
 */
@Import(BillingElementResolver.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class BillingElementR1IT extends PostgresIntegrationTestBase {

    @Autowired private BillingElementResolver resolver;
    @Autowired private BillingDocumentElementRepository links;

    @Test
    void everyQueryRunsOnTheRealSchema() {
        assertThat(resolver.search("zz-introuvable", 10)).isEmpty();
        assertThat(resolver.elementsOf("MEMBER", "MBR-INTROUVABLE")).isEmpty();
        assertThat(resolver.elements("FAC-INTROUVABLE")).isEmpty();
        assertThat(resolver.invoices(new ElementRef(ElementRef.SUBSCRIPTION, "SUB-INTROUVABLE"))).isEmpty();
        assertThat(resolver.invoices(new ElementRef(ElementRef.BOOKING, "BKG-INTROUVABLE"))).isEmpty();
        for (String type : ElementRef.TYPES) {
            assertThatThrownBy(() -> resolver.element(new ElementRef(type, "INTROUVABLE"))).isInstanceOf(ResourceNotFoundException.class);
        }
        assertThatThrownBy(() -> resolver.invoices(new ElementRef(ElementRef.DOMICILIATION, "DOM-INTROUVABLE")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> resolver.search("z", 10)).isInstanceOf(BadRequestException.class);
    }

    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** Lot R4 : la trace des annulations et la regle de contrepassation D 411 / C 4719 passent la migration. */
    @Test
    void reversalTableAndPostingRuleExist() {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM payment_allocation_reversal", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT event_type FROM acc_posting_rule WHERE code = 'SALES_ALLOCATION_REVERSAL'", String.class))
                .isEqualTo("ALLOCATION_REVERSAL");
    }

    @Test
    void aLinkSetsTheActivationModeOfItsElement() {
        links.save(BillingDocumentElement.builder().documentNumber("FAC-R1-1").elementType(ElementRef.BOOKING).elementCode("BKG-R1-1")
                .activationMode(ElementRef.FIRST_PAYMENT).linkedBy("test").build());
        assertThat(resolver.elements("FAC-R1-1")).singleElement().satisfies(settled -> {
            assertThat(settled.type()).isEqualTo(ElementRef.BOOKING);
            assertThat(settled.code()).isEqualTo("BKG-R1-1");
            assertThat(settled.activationMode()).isEqualTo(ElementRef.FIRST_PAYMENT);
        });
    }
}
