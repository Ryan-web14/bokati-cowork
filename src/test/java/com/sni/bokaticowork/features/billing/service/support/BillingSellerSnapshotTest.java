package com.sni.bokaticowork.features.billing.service.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.core.identity.OperatorIdentity;
import com.sni.bokaticowork.core.identity.OperatorIdentityService;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Instantane vendeur · ecrit a l'emission, jamais sur une piece verrouillee ou deja figee. */
class BillingSellerSnapshotTest {

    private static final OperatorIdentity OPERATOR = new OperatorIdentity(
            "ELLE A OSE", "SARL", "M123", "CG-PNR-01-2017-A11-00472",
            "84 Boulevard du Général Charles de Gaulle, centre-ville, Pointe-Noire", "Pointe-Noire",
            "05 204 2552", "coworkspace@elleaose.com", null, null);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OperatorIdentityService identityService = mock(OperatorIdentityService.class);
    private final BillingSellerSnapshot snapshot = new BillingSellerSnapshot(identityService, objectMapper);

    @Test
    @DisplayName("A l'emission, les colonnes seller_* recoivent l'identite de l'exploitant")
    void writesTheSellerColumns() {
        when(identityService.current()).thenReturn(OPERATOR);
        BillingDocument document = new BillingDocument();

        snapshot.applyIfMissing(document);

        assertThat(document.getSellerName()).isEqualTo("ELLE A OSE");
        assertThat(document.getSellerNiu()).isEqualTo("M123");
        assertThat(document.getSellerPhone()).isEqualTo("05 204 2552");
        assertThat(document.getSellerEmail()).isEqualTo("coworkspace@elleaose.com");
        assertThat(document.getSellerAddressJson())
                .isEqualTo("[\"84 Boulevard du Général Charles de Gaulle, centre-ville, Pointe-Noire\"]");
    }

    @Test
    @DisplayName("Une piece verrouillee n'est jamais reecrite")
    void neverRewritesALockedDocument() {
        BillingDocument document = new BillingDocument();
        document.setLocked(true);

        snapshot.applyIfMissing(document);

        assertThat(document.getSellerName()).isNull();
        verifyNoInteractions(identityService);
    }

    @Test
    @DisplayName("Un instantane existant est garde tel quel")
    void keepsAnExistingSnapshot() {
        BillingDocument document = new BillingDocument();
        document.setSellerName("ANCIEN NOM");
        document.setSellerEmail("ancien@exemple.cg");

        snapshot.applyIfMissing(document);

        assertThat(document.getSellerName()).isEqualTo("ANCIEN NOM");
        assertThat(document.getSellerEmail()).isEqualTo("ancien@exemple.cg");
        verifyNoInteractions(identityService);
    }

    @Test
    @DisplayName("L'identite figee relit l'adresse et complete le RCCM par l'identite courante")
    void frozenIdentityReadsTheSnapshot() {
        OperatorIdentity frozen = BillingSellerSnapshot.frozen(
                "ANCIEN NOM", null, "[\"1 rue Ancienne, Pointe-Noire\"]", "06 000 00 00", null,
                OPERATOR, objectMapper);

        assertThat(frozen.name()).isEqualTo("ANCIEN NOM");
        assertThat(frozen.address()).isEqualTo("1 rue Ancienne, Pointe-Noire");
        assertThat(frozen.phone()).isEqualTo("06 000 00 00");
        assertThat(frozen.rccm()).isEqualTo("CG-PNR-01-2017-A11-00472");
        assertThat(frozen.niu()).isEqualTo("M123");
        assertThat(frozen.email()).isEqualTo(OperatorIdentity.DEFAULT_EMAIL);
        assertThat(BillingSellerSnapshot.frozen(null, null, null, null, null, OPERATOR, objectMapper)).isNull();
    }
}
