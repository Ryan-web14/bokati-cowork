package com.sni.bokaticowork.features.subscription.subscription.service.support;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SubscriptionContractTemplatePolicyResolver {

    private static final String NON_REFUNDABLE_POLICY_CODE = "SUBSCRIPTION_PASS_NON_REFUNDABLE";

    private final JdbcTemplate jdbcTemplate;

    public ContractTemplatePolicy nonRefundablePolicy() {
        List<ContractTemplatePolicyHeader> headers = jdbcTemplate.query("""
                        SELECT code, template_code, name, description
                        FROM contract_template_policy
                        WHERE code = ?
                          AND active = TRUE
                        """,
                (rs, rowNum) -> new ContractTemplatePolicyHeader(
                        rs.getString("code"),
                        rs.getString("template_code"),
                        rs.getString("name"),
                        rs.getString("description")
                ),
                NON_REFUNDABLE_POLICY_CODE
        );
        if (headers.isEmpty()) {
            throw new ResourceNotFoundException("Subscription contract template policy not found: " + NON_REFUNDABLE_POLICY_CODE);
        }
        ContractTemplatePolicyHeader header = headers.get(0);
        List<String> clauses = jdbcTemplate.query("""
                        SELECT body
                        FROM contract_template_clause
                        WHERE policy_code = ?
                          AND active = TRUE
                        ORDER BY display_order ASC, id ASC
                        """,
                (rs, rowNum) -> rs.getString("body"),
                header.code()
        );
        return new ContractTemplatePolicy(
                header.code(),
                header.templateCode(),
                header.name(),
                header.description(),
                clauses
        );
    }

    private record ContractTemplatePolicyHeader(String code, String templateCode, String name, String description) {
    }

    public record ContractTemplatePolicy(String code,
                                         String templateCode,
                                         String name,
                                         String description,
                                         List<String> clauses) {
    }
}
