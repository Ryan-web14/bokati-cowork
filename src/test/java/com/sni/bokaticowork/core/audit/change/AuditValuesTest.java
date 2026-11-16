package com.sni.bokaticowork.core.audit.change;

import com.sni.bokaticowork.core.audit.service.implementation.AuditServiceImplCsvAccess;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Lot A1 : donnees sensibles jamais ecrites, valeurs courtes, changements fusionnes et bornes. */
class AuditValuesTest {

    @Test
    void sensitiveFieldsAreRecognisedByTheirWords() {
        assertThat(List.of("password", "passwordHash", "resetToken", "otpCode", "pinCode", "apiKey", "api_key",
                "bokati.infobip.api-key", "app.security.jwt.secret", "clientSecret", "cardNumber"))
                .allMatch(AuditValues::isSensitive);
        assertThat(List.of("shippingAddress", "mapping", "opinion", "documentNumber", "email", "status", "tokenized"))
                .noneMatch(AuditValues::isSensitive);
    }

    @Test
    void valuesAreMaskedShortenedAndNamed() {
        assertThat(AuditValues.render("password", "secret-value")).isEqualTo(AuditValues.MASK);
        assertThat(AuditValues.render("notes", "x".repeat(500))).asString().hasSize(AuditValues.MAX_TEXT + 3).endsWith("...");
        assertThat(AuditValues.render("status", Thread.State.NEW)).isEqualTo("NEW");
        assertThat(AuditValues.render("total", 12)).isEqualTo(12);
        assertThat(AuditValues.render("photo", new byte[]{1, 2, 3})).isEqualTo("(binaire, 3 octets)");
        assertThat(AuditValues.isIgnored("updatedAt")).isTrue();
        assertThat(AuditValues.isIgnored("version")).isTrue();
    }

    @Test
    void repeatedWritesKeepTheFirstBeforeAndTheLastAfter() {
        AuditChanges.begin();
        try {
            List<AuditChanges.Change> first = AuditChanges.record("BillingDocument", "FAC-1", "UPDATE", true);
            AuditChanges.field(first, "status", "DRAFT", "ISSUED");
            List<AuditChanges.Change> second = AuditChanges.record("BillingDocument", "FAC-1", "UPDATE", true);
            AuditChanges.field(second, "status", "ISSUED", "PAID");
        } finally {
            AuditChanges.Result result = AuditChanges.end();
            assertThat(result.changes()).hasSize(1);
            assertThat(result.changes().getFirst().fields().get("status")).containsEntry("before", "DRAFT").containsEntry("after", "PAID");
            assertThat(result.primary().entityCode()).isEqualTo("FAC-1");
            assertThat(AuditChanges.active()).isFalse();
        }
    }

    @Test
    void nestedActionsEachSeeTheirChangesAndTheLimitIsCounted() {
        AuditChanges.begin();
        AuditChanges.begin();
        for (int i = 0; i < AuditChanges.MAX_CHANGES + 5; i++) {
            AuditChanges.record("LedgerEntry", "L-" + i, "CREATE", false);
        }
        AuditChanges.Result inner = AuditChanges.end();
        AuditChanges.Result outer = AuditChanges.end();
        assertThat(inner.changes()).hasSize(AuditChanges.MAX_CHANGES);
        assertThat(inner.omitted()).isEqualTo(5);
        assertThat(outer.changes()).hasSize(AuditChanges.MAX_CHANGES);
        assertThat(inner.toDiff()).containsEntry("omitted", 5);
    }

    @Test
    void theCsvLineSummarisesTheChanges() {
        Map<String, Object> diff = Map.of("changes", List.of(Map.of(
                "operation", "UPDATE", "entityType", "BillingDocument", "entityCode", "FAC-1",
                "fields", Map.of("status", Map.of("before", "DRAFT", "after", "ISSUED")))));
        assertThat(AuditServiceImplCsvAccess.changes(diff)).isEqualTo("UPDATE BillingDocument FAC-1 : status DRAFT -> ISSUED");
    }
}
