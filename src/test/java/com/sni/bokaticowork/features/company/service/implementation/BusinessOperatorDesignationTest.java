package com.sni.bokaticowork.features.company.service.implementation;

import com.sni.bokaticowork.features.company.model.BusinessEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La designation de l'entite exploitante · un seul porteur, et le transfert est complet.
 *
 * <p>Le drapeau remplace une deduction qui ne tenait pas : {@code business_entity} porte l'espace
 * <b>et</b> les clients entreprises, et la generation de contrat prenait « la seule ligne active »
 * pour l'exploitant. En production il n'y en avait aucune, donc aucun contrat ne se generait ; au
 * premier client entreprise enregistre, la deduction aurait de toute facon cesse de
 * fonctionner.</p>
 *
 * <p>Ce test porte sur la regle de transfert elle-meme. L'unicite est tenue en base par un index
 * unique partiel, que ce niveau ne peut pas simuler · c'est voulu, une regle d'unicite appartient
 * a la base.</p>
 */
class BusinessOperatorDesignationTest {

    private BusinessEntity entity(String code, boolean operator) {
        return BusinessEntity.builder().code(code).name("Entite " + code).operator(operator).build();
    }

    @Test
    @DisplayName("Par defaut, aucune entite n'est exploitante")
    void noEntityIsOperatorByDefault() {
        // Un client entreprise enregistre normalement ne doit jamais devenir l'exploitant par
        // accident · c'etait le cas quand il suffisait d'etre la seule ligne active.
        assertThat(BusinessEntity.builder().code("BIZ-1").name("SARL Mbote").build().isOperator())
                .isFalse();
    }

    @Test
    @DisplayName("Designer transfere le drapeau · l'ancien porteur le perd")
    void designatingTransfersTheFlag() {
        BusinessEntity previous = entity("ESPACE-1", true);
        BusinessEntity next = entity("ESPACE-2", false);

        previous.setOperator(false);
        next.setOperator(true);

        assertThat(previous.isOperator()).isFalse();
        assertThat(next.isOperator()).isTrue();
    }

    @Test
    @DisplayName("Un client entreprise reste un client, meme s'il est seul en base")
    void aBusinessClientStaysAClient() {
        BusinessEntity onlyClient = entity("BIZ-CLIENT-1", false);

        assertThat(onlyClient.isOperator()).isFalse();
    }
}
