package com.sni.bokaticowork.features.company;

import com.sni.bokaticowork.features.company.enums.IdentifierType;
import com.sni.bokaticowork.features.company.service.support.OrganizationIdentifiers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.sni.bokaticowork.features.company.service.support.OrganizationIdentifiers.Verdict.INVALID;
import static com.sni.bokaticowork.features.company.service.support.OrganizationIdentifiers.Verdict.LEGACY;
import static com.sni.bokaticowork.features.company.service.support.OrganizationIdentifiers.Verdict.VALID;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le format d'un identifiant n'est verifie que la ou il est connu.
 */
class OrganizationIdentifiersTest {

    private static OrganizationIdentifiers.Verdict rccm(String raw) {
        return OrganizationIdentifiers.check(IdentifierType.RCCM, OrganizationIdentifiers.normalize(IdentifierType.RCCM, raw));
    }

    @Test
    @DisplayName("RCCM au format OHADA 2019, y compris saisi en minuscules ou avec des espaces")
    void ohadaRccm() {
        assertThat(rccm("CG-PNR-01-2017-A11-00472")).isEqualTo(VALID);
        assertThat(rccm("cg-bzv-01-2024-b12-00001")).isEqualTo(VALID);
        assertThat(OrganizationIdentifiers.normalize(IdentifierType.RCCM, " cg - bzv - 01 - 2024 - B12 - 00001 "))
                .isEqualTo("CG-BZV-01-2024-B12-00001");
    }

    @Test
    @DisplayName("RCCM d'avant 2019 accepte et marque, un texte quelconque refuse")
    void legacyAndInvalidRccm() {
        assertThat(rccm("CG/BZV/16 B 6261")).isEqualTo(LEGACY);
        assertThat(rccm("n'importe quoi")).isEqualTo(INVALID);
        assertThat(rccm("CG-PNR-01-17-A11-472")).isEqualTo(INVALID);
    }

    @Test
    @DisplayName("NIU selon la regle en place, espaces retires")
    void niu() {
        String niu = OrganizationIdentifiers.normalize(IdentifierType.NIU, "p 200 711 000 047 346");
        assertThat(niu).isEqualTo("P200711000047346");
        assertThat(OrganizationIdentifiers.check(IdentifierType.NIU, niu)).isEqualTo(VALID);
        assertThat(OrganizationIdentifiers.check(IdentifierType.NIU, "123")).isEqualTo(INVALID);
    }

    @Test
    @DisplayName("Recepisse, agrement, acte de creation · pris tels quels")
    void otherIdentifiersAreFreeText() {
        assertThat(OrganizationIdentifiers.check(IdentifierType.RECEPISSE,
                OrganizationIdentifiers.normalize(IdentifierType.RECEPISSE, "N° 245/MID/DGAT/DER/SAG"))).isEqualTo(VALID);
        assertThat(OrganizationIdentifiers.check(IdentifierType.ACTE_CREATION,
                OrganizationIdentifiers.normalize(IdentifierType.ACTE_CREATION, "Décret n° 2019-12 du 4 mars 2019"))).isEqualTo(VALID);
    }
}
