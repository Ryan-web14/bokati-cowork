package com.sni.bokaticowork.core.audit.change;

import com.sni.bokaticowork.core.settings.platform.PlatformSetting;
import com.sni.bokaticowork.core.settings.platform.PlatformSettingRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Lot A1 sur une vraie base : les ecouteurs Hibernate captent la creation, la modification (avant /
 * apres des seuls champs modifies) et la suppression d'une entite principale pendant une action
 * auditee, masquent un parametre secret, et ne captent rien hors d'une action. La migration passe la
 * validation du schema.
 */
@Import(EntityChangeListener.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class EntityChangeA1IT extends PostgresIntegrationTestBase {

    @Autowired private PlatformSettingRepository settings;
    @Autowired private TestEntityManager entityManager;

    @Test
    void creationModificationAndDeletionAreCaptured() {
        AuditChanges.begin();
        settings.saveAndFlush(PlatformSetting.builder().key("bokati.test.a1").value("10").updatedBy("awa").updatedAt(Instant.now()).build());
        AuditChanges.Result created = AuditChanges.end();

        assertThat(created.primary().entityType()).isEqualTo("PlatformSetting");
        assertThat(created.primary().entityCode()).isEqualTo("bokati.test.a1");
        assertThat(created.primary().operation()).isEqualTo("CREATE");
        assertThat(created.primary().fields()).containsKey("value").doesNotContainKey("updatedAt");

        entityManager.clear();
        AuditChanges.begin();
        PlatformSetting setting = settings.findById("bokati.test.a1").orElseThrow();
        setting.setValue("25");
        setting.setUpdatedAt(Instant.now());
        settings.saveAndFlush(setting);
        AuditChanges.Result updated = AuditChanges.end();

        AuditChanges.Change change = updated.primary();
        assertThat(change.operation()).isEqualTo("UPDATE");
        assertThat(change.fields()).containsOnlyKeys("value");
        assertThat(change.fields().get("value")).isEqualTo(Map.of("before", "10", "after", "25"));

        AuditChanges.begin();
        settings.delete(setting);
        settings.flush();
        assertThat(AuditChanges.end().primary().operation()).isEqualTo("DELETE");
    }

    @Test
    void aSecretSettingIsNeverWritten() {
        AuditChanges.begin();
        settings.saveAndFlush(PlatformSetting.builder().key("bokati.test.api-key").value("cle-tres-secrete")
                .updatedBy("awa").updatedAt(Instant.now()).build());
        AuditChanges.Result created = AuditChanges.end();

        assertThat(created.primary().fields().get("value")).containsEntry("after", AuditValues.MASK);
        assertThat(created.toDiff().toString()).doesNotContain("cle-tres-secrete");
    }

    @Test
    void nothingIsCapturedOutsideAnAuditedAction() {
        settings.saveAndFlush(PlatformSetting.builder().key("bokati.test.hors").value("1").updatedBy("awa").updatedAt(Instant.now()).build());
        assertThat(AuditChanges.active()).isFalse();
        AuditChanges.begin();
        assertThat(AuditChanges.end().isEmpty()).isTrue();
    }
}
