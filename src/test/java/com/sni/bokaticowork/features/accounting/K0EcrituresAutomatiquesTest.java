package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.accounting.model.AccPostingRule;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lot K0 de la revue comptable : evaluation des schemas et migration des schemas. */
class K0EcrituresAutomatiquesTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PostingTemplate template = new PostingTemplate(mapper, List.of());

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }

    @Test
    @DisplayName("A1 : l'echeance ecrite comme un chemin nu est lue dans l'evenement")
    void dueDateAsPath() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("INV").journal("VT").lines("""
                [{"side":"DEBIT","account":"411","amount":"total","thirdParty":"C1","dueDate":"dueDate"},
                 {"side":"CREDIT","account":"706","amount":"total"}]""").build();
        PostingTemplate.Plan plan = template.evaluate(rule, json("{\"total\":1000,\"dueDate\":\"2026-12-31\"}"), false);
        assertThat(plan.lines().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 12, 31));

        PostingTemplate.Plan without = template.evaluate(rule, json("{\"total\":1000}"), false);
        assertThat(without.lines().get(0).dueDate()).isNull();

        assertThatThrownBy(() -> template.evaluate(rule, json("{\"total\":1000,\"dueDate\":\"fin du mois\"}"), false))
                .hasMessageContaining("Échéance illisible");
    }

    @Test
    @DisplayName("M7 : dans une repetition, un montant absent de l'element ne prend plus le total de l'evenement")
    void foreachAmountReadsTheItemOnly() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("INV").journal("VT").lines("""
                [{"side":"DEBIT","account":"411","amount":"amount","thirdParty":"C1"},
                 {"side":"CREDIT","account":"{account}","amount":"amount","foreach":"lines"}]""").build();
        PostingTemplate.Plan plan = template.evaluate(rule,
                json("{\"amount\":1000,\"lines\":[{\"account\":\"706\",\"amount\":1000},{\"account\":\"7063\"}]}"), false);
        assertThat(plan.lines()).extracting(line -> line.accountNumber()).containsExactly("411", "706");
        assertThat(plan.balanced()).isTrue();
    }

    @Test
    @DisplayName("M7 : « a-b » sans espaces est une soustraction")
    void subtractionWithoutSpaces() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("X").journal("OD").lines("""
                [{"side":"DEBIT","account":"571","amount":"gross-fee"},
                 {"side":"CREDIT","account":"471","amount":"gross - fee"}]""").build();
        PostingTemplate.Plan plan = template.evaluate(rule, json("{\"gross\":500,\"fee\":50}"), false);
        assertThat(plan.lines().get(0).debit()).isEqualByComparingTo("450");
        assertThat(plan.balanced()).isTrue();
    }

    @Test
    @DisplayName("A10 : un fait a montant nul ne produit aucune ligne (le moteur l'ecarte) ; une seule ligne reste refusee")
    void zeroAmountProducesNoLine() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("X").journal("VT").lines("""
                [{"side":"DEBIT","account":"411","amount":"total","thirdParty":"C1"},
                 {"side":"CREDIT","account":"706","amount":"net"}]""").build();
        assertThat(template.evaluate(rule, json("{\"total\":0,\"net\":0}"), false).lines()).isEmpty();
        assertThatThrownBy(() -> template.evaluate(rule, json("{\"total\":100,\"net\":0}"), false))
                .hasMessageContaining("ne produit que 1");
    }

    @Test
    @DisplayName("A7 : schema de facture migre, une ligne de TVA par taux avec son code et sa base")
    void vatPerRate() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("SALES_INVOICE").journal("VT").lines("""
                [{"side":"DEBIT","account":"{clientAccount}","amount":"totalAmount","thirdParty":"{customerCode}","reference":"{documentNumber}","dueDate":"dueDate"},
                 {"side":"CREDIT","account":"{account}","amount":"amount","foreach":"lines","label":"{label}","thirdParty":"{?thirdParty}"},
                 {"side":"CREDIT","account":"{account}","amount":"amount","foreach":"vatLines","label":"TVA {?rateLabel}","reference":"{documentNumber}","taxCode":"{?taxCode}","taxBase":"base"}]""")
                .build();
        JsonNode event = json("""
                {"documentNumber":"FAC-1","customerCode":"C1","clientAccount":"411","totalAmount":1280000,"dueDate":"2026-12-01",
                 "lines":[{"account":"706","amount":1000000,"label":"Bureau"},{"account":"165","amount":100000,"label":"Caution","thirdParty":"C1"}],
                 "vatLines":[{"account":"4431","amount":162000,"base":900000,"taxCode":"TVA18","rateLabel":"18 %"},
                             {"account":"4431","amount":18000,"base":100000,"taxCode":"TVA18.9","rateLabel":"18.9 %"}]}""");
        PostingTemplate.Plan plan = template.evaluate(rule, event, false);
        assertThat(plan.balanced()).isTrue();
        assertThat(plan.lines()).extracting(line -> line.taxCode()).containsExactly(null, null, null, "TVA18", "TVA18.9");
        assertThat(plan.lines().get(2).thirdPartyCode()).isEqualTo("C1");
        assertThat(plan.lines().get(1).thirdPartyCode()).isNull();
        assertThat(plan.lines().get(3).taxBase()).isEqualByComparingTo("900000");
        assertThat(plan.lines().get(3).reference()).isEqualTo("FAC-1");
        assertThat(plan.lines().get(0).dueDate()).isEqualTo(LocalDate.of(2026, 12, 1));
    }

    @Test
    @DisplayName("Migration K0 : identique en dev et en prod, sans tiret long, nouveaux schemas presents")
    void migrationTwins() throws Exception {
        String dev = Files.readString(Path.of("src/main/resources/db/migration/V400__comptabilite_k0_ecritures_automatiques.sql"));
        String prod = Files.readString(Path.of("src/main/resources/db/migration-prod/V394__comptabilite_k0_ecritures_automatiques.sql"));
        assertThat(prod).isEqualTo(dev);
        assertThat(dev).doesNotContain("—").doesNotContain("–");
        assertThat(dev).contains("'SALES_DEPOSIT_CREDIT_NOTE'", "'SALES_DEPOSIT_REMAINDER_CANCELLED'", "'SALES_CREDIT_NOTE_APPLIED'",
                "applied_to_document_number", "'4711'");
        // Les fragments remplaces existent bien dans les schemas d'origine.
        String sales = Files.readString(Path.of("src/main/resources/db/migration/V299__comptabilite_ventes_encaissements_f4.sql"));
        String vat = Files.readString(Path.of("src/main/resources/db/migration/V307__fiscalite_tva_f11.sql"));
        assertThat(sales).contains("{\"side\":\"CREDIT\",\"account\":\"{account}\",\"amount\":\"amount\",\"foreach\":\"lines\",\"label\":\"{label}\"}");
        assertThat(vat).contains("{\"side\":\"CREDIT\",\"account\":\"{vatAccount}\",\"amount\":\"vatAmount\",\"label\":\"TVA\",\"reference\":\"{documentNumber}\",\"taxCode\":\"{?vatCode}\",\"taxBase\":\"vatBase\"}");
    }
}
