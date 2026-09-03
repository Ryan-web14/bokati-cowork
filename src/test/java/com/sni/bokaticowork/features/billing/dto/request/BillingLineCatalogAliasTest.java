package com.sni.bokaticowork.features.billing.dto.request;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L'interface designe l'article du catalogue par {@code catalogSourceCode}, l'API par
 * {@code itemCode}.
 *
 * <p>Un ecart de nom sur ce champ n'echoue pas : la propriete inconnue est ignoree, la ligne
 * n'est rattachee a aucun article, et le plancher de prix, le taux de remise maximal, la
 * categorie et l'unite cessent tous de s'appliquer · sans le moindre message. C'est precisement
 * ce qui s'est produit en production, ou une remise de 40 % passait sans declencher le moindre
 * garde-fou.
 *
 * <p>D'ou ces tests : l'alias ne doit pas se perdre au fil des refontes de DTO.
 */
class BillingLineCatalogAliasTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldAcceptTheFrontEndSpellingOnCreation() throws Exception {
        String json = """
                {
                  "lineType": "SERVICE",
                  "description": "Salle test plancher",
                  "quantity": 1,
                  "unitPrice": 15000,
                  "catalogSourceType": "SERVICE_CATALOG",
                  "catalogSourceCode": "SVC-000003"
                }
                """;

        CreateBillingDocumentLineRequest line =
                objectMapper.readValue(json, CreateBillingDocumentLineRequest.class);

        assertThat(line.itemCode()).isEqualTo("SVC-000003");
        assertThat(line.sourceType()).isEqualTo("SERVICE_CATALOG");
    }

    @Test
    void shouldStillAcceptTheApiSpelling() throws Exception {
        String json = """
                {
                  "description": "Salle",
                  "unitPrice": 15000,
                  "itemCode": "SVC-000003",
                  "sourceType": "BOOKING"
                }
                """;

        CreateBillingDocumentLineRequest line =
                objectMapper.readValue(json, CreateBillingDocumentLineRequest.class);

        assertThat(line.itemCode()).isEqualTo("SVC-000003");
        assertThat(line.sourceType()).isEqualTo("BOOKING");
    }

    @Test
    void shouldAcceptTheAliasOnUpdateToo() throws Exception {
        // La modification recoit la meme charge utile · l'alias doit y valoir aussi, sans quoi
        // le rattachement se perdrait au premier enregistrement.
        String json = """
                {
                  "action": "UPSERT",
                  "lineOrder": 1,
                  "description": "Salle",
                  "unitPrice": 15000,
                  "catalogSourceCode": "SVC-000003"
                }
                """;

        UpdateBillingDocumentLineRequest line =
                objectMapper.readValue(json, UpdateBillingDocumentLineRequest.class);

        assertThat(line.itemCode()).isEqualTo("SVC-000003");
    }

    @Test
    void shouldLeaveTheItemCodeEmptyWhenNeitherSpellingIsPresent() throws Exception {
        String json = """
                { "description": "Prestation libre", "unitPrice": 15000 }
                """;

        assertThat(objectMapper.readValue(json, CreateBillingDocumentLineRequest.class).itemCode())
                .isNull();
    }
}
