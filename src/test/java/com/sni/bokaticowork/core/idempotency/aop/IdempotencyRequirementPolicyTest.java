package com.sni.bokaticowork.core.idempotency.aop;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdempotencyRequirementPolicyTest {

    @Test
    void requiresNothingWhenConfigurationIsEmpty() {
        IdempotencyRequirementPolicy policy = new IdempotencyRequirementPolicy("");

        assertFalse(policy.isRequired("INVENTORY_STOCK_OUT"));
    }

    @Test
    void requiresNothingWhenConfigurationIsAbsent() {
        IdempotencyRequirementPolicy policy = new IdempotencyRequirementPolicy(null);

        assertFalse(policy.isRequired("INVENTORY_STOCK_OUT"));
    }

    @Test
    void requiresOnlyTheDeclaredOperations() {
        IdempotencyRequirementPolicy policy =
                new IdempotencyRequirementPolicy("INVENTORY_STOCK_IN, INVENTORY_STOCK_OUT");

        assertTrue(policy.isRequired("INVENTORY_STOCK_IN"));
        assertTrue(policy.isRequired("INVENTORY_STOCK_OUT"));
        assertFalse(policy.isRequired("INVENTORY_STOCK_TRANSFER"));
    }

    @Test
    void ignoresCaseAndSurroundingWhitespace() {
        IdempotencyRequirementPolicy policy = new IdempotencyRequirementPolicy(" inventory_stock_in ");

        assertTrue(policy.isRequired("INVENTORY_STOCK_IN"));
        assertTrue(policy.isRequired("  inventory_stock_in  "));
    }

    @Test
    void wildcardRequiresEveryOperation() {
        IdempotencyRequirementPolicy policy = new IdempotencyRequirementPolicy("*");

        assertTrue(policy.isRequired("ANY_OPERATION"));
        assertTrue(policy.isRequired(null));
    }

    @Test
    void handlesEmptyEntriesInTheList() {
        IdempotencyRequirementPolicy policy = new IdempotencyRequirementPolicy("INVENTORY_STOCK_IN,,  ,");

        assertTrue(policy.isRequired("INVENTORY_STOCK_IN"));
        assertFalse(policy.isRequired(""));
    }
}
