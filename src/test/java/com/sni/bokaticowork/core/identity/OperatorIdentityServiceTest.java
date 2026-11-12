package com.sni.bokaticowork.core.identity;

import com.sni.bokaticowork.core.baseClasses.model.Address;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Identite de l'exploitant · une seule source, avec repli sur les valeurs historiques. */
class OperatorIdentityServiceTest {

    private final BusinessRepository repository = mock(BusinessRepository.class);
    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-11-12T08:00:00Z"));
    private final Clock clock = new Clock() {
        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    };
    private final OperatorIdentityService service = new OperatorIdentityService(repository, clock);

    @Test
    @DisplayName("Sans fiche exploitant, les valeurs historiques des gabarits s'impriment")
    void withoutOperatorTheDefaultsApply() {
        when(repository.findFirstByOperatorTrueAndDeletedFalse()).thenReturn(Optional.empty());

        OperatorIdentity identity = service.current();

        assertThat(identity.name()).isEqualTo("ELLE A OSE");
        assertThat(identity.rccmLine()).isEqualTo("RCCM CG-PNR-01-2017-A11-00472");
        assertThat(identity.address()).isEqualTo("84 Boulevard du Général Charles de Gaulle, centre-ville, Pointe-Noire");
        assertThat(identity.contactLine()).isEqualTo("coworkspace@elleaose.com · 05 204 2552");
        assertThat(identity.city()).isEqualTo("Pointe-Noire");
    }

    @Test
    @DisplayName("La fiche exploitant prime, et chaque champ vide retombe sur sa valeur par defaut")
    void theOperatorCardWinsFieldByField() {
        when(repository.findFirstByOperatorTrueAndDeletedFalse()).thenReturn(Optional.of(operator()));

        OperatorIdentity identity = service.current();

        assertThat(identity.name()).isEqualTo("ELLE A OSE");
        assertThat(identity.legalForm()).isEqualTo("SARL");
        assertThat(identity.niu()).isEqualTo("M0123456789");
        assertThat(identity.address()).isEqualTo("12 Avenue de l'Indépendance, Centre-ville, Brazzaville");
        assertThat(identity.city()).isEqualTo("Brazzaville");
        assertThat(identity.phone()).isEqualTo("06 000 0000");
        // RCCM et courriel absents de la fiche · repli.
        assertThat(identity.rccm()).isEqualTo(OperatorIdentity.DEFAULT_RCCM);
        assertThat(identity.email()).isEqualTo(OperatorIdentity.DEFAULT_EMAIL);
        assertThat(identity.registrationLine()).isEqualTo("RCCM CG-PNR-01-2017-A11-00472 · NIU M0123456789");
        assertThat(identity.representativeName()).isEqualTo("Mme Hope MADHONE");
    }

    @Test
    @DisplayName("La fiche est gardee une minute, puis relue")
    void theCardIsCachedForAMinute() {
        when(repository.findFirstByOperatorTrueAndDeletedFalse()).thenReturn(Optional.of(operator()));

        service.current();
        now.set(now.get().plusSeconds(30));
        service.current();
        verify(repository, times(1)).findFirstByOperatorTrueAndDeletedFalse();

        now.set(now.get().plusSeconds(31));
        service.current();
        verify(repository, times(2)).findFirstByOperatorTrueAndDeletedFalse();
    }

    @Test
    @DisplayName("Une modification de la fiche invalide le cache sans attendre")
    void aChangeInvalidatesTheCache() {
        when(repository.findFirstByOperatorTrueAndDeletedFalse()).thenReturn(Optional.of(operator()));
        service.current();

        new OperatorIdentityChangeListener().changed(new BusinessEntity());
        service.current();

        verify(repository, times(2)).findFirstByOperatorTrueAndDeletedFalse();
    }

    @Test
    @DisplayName("Une base illisible ne bloque pas l'emission · identite par defaut")
    void aFailingRepositoryGivesTheDefaults() {
        when(repository.findFirstByOperatorTrueAndDeletedFalse()).thenThrow(new IllegalStateException("base indisponible"));

        assertThat(service.current()).isEqualTo(OperatorIdentity.defaults());
    }

    private static BusinessEntity operator() {
        BusinessEntity business = new BusinessEntity();
        business.setName("ELLE A OSE");
        business.setLegalForm("SARL");
        business.setNiuNumber("M0123456789");
        business.setRccmNumber("  ");
        business.setPhone("06 000 0000");
        business.setLegalRepresentativeName("Mme Hope MADHONE");
        Address address = new Address();
        address.setStreetNumber("12");
        address.setStreetName("Avenue de l'Indépendance");
        address.setDistrict("Centre-ville");
        address.setCity("Brazzaville");
        business.setAddress(address);
        business.setOperator(true);
        return business;
    }
}
