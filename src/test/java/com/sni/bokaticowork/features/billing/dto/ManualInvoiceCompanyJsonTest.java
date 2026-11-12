package com.sni.bokaticowork.features.billing.dto;

import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Une facture manuelle dit, comme les autres, pour quelle organisation un membre est facture. */
class ManualInvoiceCompanyJsonTest {

    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private static final String BODY = """
            {"title":"Facture manuelle","currency":"XAF","issueDate":"2026-11-12","language":"fr",
             "lines":[{"lineOrder":1,"lineType":"SERVICE","description":"Article test","quantity":1,"unitPrice":50000,
                       "discountRate":0,"taxable":true,"taxIncluded":false,"optional":false,
                       "catalogSourceType":"SERVICE_CATALOG","catalogSourceCode":"SVC-000001"}],
             "discounts":[],"clauses":[],"customerType":"MEMBER","customerCode":"MBR-202607-00000005",
             %s}""";

    @Test
    @DisplayName("companyCustomerCode est accepte, NONE compris")
    void companyCustomerCodeIsAccepted() throws Exception {
        CreateManualBillingDocumentRequest request = mapper.readValue(
                BODY.formatted("\"companyCustomerCode\":\"NONE\""), CreateManualBillingDocumentRequest.class);

        assertThat(request.companyCustomerCode()).isEqualTo("NONE");
        assertThat(request.customerCode()).isEqualTo("MBR-202607-00000005");
        assertThat(request.lines()).hasSize(1);
    }

    @Test
    @DisplayName("customerCompanyCode est accepte comme synonyme")
    void aliasIsAccepted() throws Exception {
        CreateManualBillingDocumentRequest request = mapper.readValue(
                BODY.formatted("\"customerCompanyCode\":\"CUS-COM-202611-00000012\""), CreateManualBillingDocumentRequest.class);

        assertThat(request.companyCustomerCode()).isEqualTo("CUS-COM-202611-00000012");
    }

    @Test
    @DisplayName("Sans le champ, la facture suit le mode du rattachement")
    void absentStaysNull() throws Exception {
        CreateManualBillingDocumentRequest request = mapper.readValue(
                BODY.formatted("\"poNumber\":\"BC-1\""), CreateManualBillingDocumentRequest.class);

        assertThat(request.companyCustomerCode()).isNull();
    }
}
