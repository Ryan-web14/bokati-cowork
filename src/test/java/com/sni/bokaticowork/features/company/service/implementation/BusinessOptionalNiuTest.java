package com.sni.bokaticowork.features.company.service.implementation;

import com.sni.bokaticowork.core.baseClasses.service.interfaces.AddressService;
import com.sni.bokaticowork.core.baseClasses.service.interfaces.CurrencyService;
import com.sni.bokaticowork.core.exception.customs.ValidationException;
import com.sni.bokaticowork.features.company.dto.request.BusinessEntityRequest;
import com.sni.bokaticowork.features.company.enums.LegalForm;
import com.sni.bokaticowork.features.company.mapper.interfaces.BusinessMapper;
import com.sni.bokaticowork.features.company.model.BusinessEntity;
import com.sni.bokaticowork.features.company.repository.BusinessRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

/**
 * Le NIU est facultatif · toutes les entites n'en detiennent pas au moment de leur creation.
 *
 * <p>Il etait exige, et l'espace lui-meme n'en a pas · sa fiche ne pouvait donc pas etre
 * enregistree, ce qui bloquait la generation de <b>tous</b> les contrats, pour les membres
 * particuliers comme pour les entreprises.</p>
 */
class BusinessOptionalNiuTest {

    private BusinessServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new BusinessServiceImpl(
                mock(BusinessMapper.class),
                mock(BusinessRepository.class),
                mock(AddressService.class),
                mock(CurrencyService.class));
    }

    private BusinessEntityRequest request(String niu) {
        BusinessEntityRequest request = new BusinessEntityRequest();
        request.setName("ETS ELLE A OSE");
        request.setLegalForm("ETABLISSEMENT");
        request.setNiuNumber(niu);
        request.setRccmNumber("CG-PNR-01-2017-A11-00472");
        request.setPhone("+242 05 204 25 51");
        request.setEmail("coworkspace@elleaose.com");
        request.setBaseCurrencyCode("XAF");
        return request;
    }

    @SuppressWarnings("unchecked")
    private List<String> validate(BusinessEntityRequest request) {
        return (List<String>) ReflectionTestUtils.invokeMethod(service, "validateBusiness", request);
    }

    @ParameterizedTest
    @DisplayName("Un NIU absent ou vide ne produit aucune erreur")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void anAbsentNiuIsAccepted(String niu) {
        assertThat(validate(request(niu))).isEmpty();
    }

    @Test
    @DisplayName("Un NIU fourni mais mal forme reste refuse")
    void aMalformedNiuIsStillRefused() {
        assertThat(validate(request("pas-un-niu")))
                .containsExactly("Invalid business request, the NIU number is not valid");
    }

    @Test
    @DisplayName("Un NIU fourni et bien forme est accepte")
    void awellFormedNiuIsAccepted() {
        // Motif congolais · une lettre P ou E suivie de quinze chiffres.
        assertThat(validate(request("P123456789012345"))).isEmpty();
    }

    @Test
    @DisplayName("La forme juridique Etablissement existe · elle manquait a la liste")
    void theEtablissementLegalFormExists() {
        // La liste ne portait que des formes societaires. Le RCCM de l'espace est en A11, soit un
        // commercant personne physique · sa fiche etait donc refusee a la creation.
        assertThat(LegalForm.valueOf("ETABLISSEMENT")).isNotNull();
        assertThat(validate(request(null))).isEmpty();
    }

    @Test
    @DisplayName("La mise a jour ne tombe pas quand aucun des deux cotes n'a de NIU")
    void updatingWithoutAnyNiuDoesNotBreak() {
        // La comparaison appelait getNiuNumber().equalsIgnoreCase(...) des deux cotes · deux NPE
        // des que le NIU est devenu facultatif.
        BusinessEntity stored = BusinessEntity.builder()
                .code("ELLEAOSE").name("ETS ELLE A OSE").niuNumber(null)
                .rccmNumber("CG-PNR-01-2017-A11-00472").build();

        assertThatCode(() -> ReflectionTestUtils.invokeMethod(
                service, "validateBusiness", request(null))).doesNotThrowAnyException();
        assertThat(stored.getNiuNumber()).isNull();
    }

    @Test
    @DisplayName("Les autres regles restent · un nom vide est toujours refuse")
    void theOtherRulesStillApply() {
        BusinessEntityRequest request = request(null);
        request.setName("   ");

        assertThat(validate(request)).isNotEmpty();
    }

    @Test
    @DisplayName("Une requete nulle est signalee, pas tolere en silence")
    void aNullRequestIsReported() {
        assertThat(validate(null)).isNotEmpty();
    }

    @Test
    @DisplayName("Une forme juridique inconnue est toujours refusee")
    void anUnknownLegalFormIsRefused() {
        BusinessEntityRequest request = request(null);
        request.setLegalForm("GIE");

        assertThat(validate(request))
                .contains("Invalid business request, the legal form is not valid");
    }

    @Test
    @DisplayName("ValidationException n'est pas levee pour un NIU absent a la creation")
    void creationDoesNotRejectAnAbsentNiu() {
        assertThatCode(() -> {
            List<String> errors = validate(request(null));
            if (!errors.isEmpty()) {
                throw new ValidationException("Invalid business request", errors);
            }
        }).doesNotThrowAnyException();
    }
}
