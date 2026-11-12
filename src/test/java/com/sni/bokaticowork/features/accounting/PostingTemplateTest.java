package com.sni.bokaticowork.features.accounting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sni.bokaticowork.features.accounting.model.AccPostingRule;
import com.sni.bokaticowork.features.accounting.service.support.BasicPostingResolvers;
import com.sni.bokaticowork.features.accounting.service.support.PostingTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Evaluation des schemas d'ecriture (lot F2). */
class PostingTemplateTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private final PostingTemplate template = new PostingTemplate(mapper, List.of(
            BasicPostingResolvers.simple("CLIENT_ACCOUNT", "test", (argument, event) -> "CUS-9".equals(argument) ? "4111" : null)));

    private JsonNode json(String value) throws Exception {
        return mapper.readTree(value);
    }

    @Test
    @DisplayName("Facture : une ligne de produit par ligne de facture, TVA, reduction, tiers resolu")
    void invoice() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("INV").journal("VT").entryLabel("Facture {number}")
                .lines("""
                        [{"side":"DEBIT","account":"{CLIENT_ACCOUNT:customer}","amount":"totalTtc","thirdParty":"{customer}","reference":"{number}"},
                         {"side":"DEBIT","account":"709","amount":"discount"},
                         {"side":"CREDIT","account":"{account}","amount":"ht","foreach":"lines","label":"{designation}"},
                         {"side":"CREDIT","account":"4431","amount":"vat"}]""").build();
        JsonNode event = json("""
                {"number":"FAC-1","customer":"CUS-9","totalTtc":1100,"discount":100,"vat":200,
                 "lines":[{"account":"7061","ht":600,"designation":"Bureau"},{"account":"7063","ht":400,"designation":"Salle"},
                          {"account":"7064","ht":0,"designation":"Gratuit"}]}""");

        PostingTemplate.Plan plan = template.evaluate(rule, event, false);

        assertThat(plan.journalCode()).isEqualTo("VT");
        assertThat(plan.label()).isEqualTo("Facture FAC-1");
        assertThat(plan.balanced()).isTrue();
        assertThat(plan.lines()).extracting(line -> line.accountNumber()).containsExactly("4111", "709", "7061", "7063", "4431");
        assertThat(plan.lines().get(0).thirdPartyCode()).isEqualTo("CUS-9");
        assertThat(plan.lines().get(2).label()).isEqualTo("Bureau");
        assertThat(plan.totalDebit()).isEqualByComparingTo("1200");
    }

    @Test
    @DisplayName("Montant signe : un ajustement negatif passe de l'autre cote ; expression avec soustraction")
    void signedAndArithmetic() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("ADJ").journal("CA")
                .lines("""
                        [{"side":"DEBIT","account":"571","amount":"amount","signed":true},
                         {"side":"CREDIT","account":"471","amount":"gross - fee","signed":true},
                         {"side":"CREDIT","account":"631","amount":"fee","signed":true}]""").build();
        PostingTemplate.Plan plan = template.evaluate(rule, json("{\"amount\":-450,\"gross\":-450,\"fee\":-50}"), false);
        assertThat(plan.lines().get(0).credit()).isEqualByComparingTo("450");
        assertThat(plan.lines().get(1).debit()).isEqualByComparingTo("400");
        assertThat(plan.balanced()).isTrue();
    }

    @Test
    @DisplayName("Un montant negatif sans « signe » est refuse")
    void negativeWithoutSigned() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("X").journal("OD")
                .lines("[{\"side\":\"DEBIT\",\"account\":\"571\",\"amount\":\"amount\"},{\"side\":\"CREDIT\",\"account\":\"471\",\"amount\":\"amount\"}]")
                .build();
        assertThatThrownBy(() -> template.evaluate(rule, json("{\"amount\":-5}"), false))
                .isInstanceOf(PostingTemplate.PostingException.class).hasMessageContaining("negatif");
    }

    @Test
    @DisplayName("Conditions : egalite, liste, exclusion, presence")
    void conditions() throws Exception {
        AccPostingRule cash = AccPostingRule.builder().conditions("{\"method\":[\"CASH\",\"CHEQUE\"],\"status\":\"!CANCELLED\",\"registerCode\":\"*\"}").build();
        assertThat(template.matches(cash, json("{\"method\":\"CASH\",\"status\":\"SUCCEEDED\",\"registerCode\":\"CSR-1\"}"))).isTrue();
        assertThat(template.matches(cash, json("{\"method\":\"WALLET\",\"status\":\"SUCCEEDED\",\"registerCode\":\"CSR-1\"}"))).isFalse();
        assertThat(template.matches(cash, json("{\"method\":\"CASH\",\"status\":\"CANCELLED\",\"registerCode\":\"CSR-1\"}"))).isFalse();
        assertThat(template.matches(cash, json("{\"method\":\"CASH\",\"status\":\"SUCCEEDED\"}"))).isFalse();
    }

    @Test
    @DisplayName("Variable introuvable : echec lisible, ou repere ?nom en apercu")
    void unresolved() throws Exception {
        AccPostingRule rule = AccPostingRule.builder().code("U").journal("VT")
                .lines("[{\"side\":\"DEBIT\",\"account\":\"{CLIENT_ACCOUNT:customer}\",\"amount\":\"a\"},{\"side\":\"CREDIT\",\"account\":\"706\",\"amount\":\"a\"}]")
                .build();
        JsonNode event = json("{\"customer\":\"CUS-UNKNOWN\",\"a\":10}");
        assertThatThrownBy(() -> template.evaluate(rule, event, false)).hasMessageContaining("CLIENT_ACCOUNT:customer");
        PostingTemplate.Plan preview = template.evaluate(rule, event, true);
        assertThat(preview.lines().get(0).accountNumber()).isEqualTo("?CLIENT_ACCOUNT:customer");
        assertThat(preview.warnings()).hasSize(1);
    }
}
