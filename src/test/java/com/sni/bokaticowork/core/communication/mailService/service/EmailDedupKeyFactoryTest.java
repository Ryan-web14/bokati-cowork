package com.sni.bokaticowork.core.communication.mailService.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class EmailDedupKeyFactoryTest {

    private final EmailDedupKeyFactory factory = factory(true, 86400, 3600);

    @Test
    void shouldProduceTheSameKeyForTheSameLogicalEmail() {
        EmailDedupKeyFactory.Keys first = factory.build("jane@example.com", "Facture INV-001", "BILLING", "INV-001");
        EmailDedupKeyFactory.Keys second = factory.build("jane@example.com", "Facture INV-001", "BILLING", "INV-001");

        assertThat(first.key()).isEqualTo(second.key());
    }

    @Test
    void shouldIgnoreRecipientCasingAndSurroundingWhitespace() {
        EmailDedupKeyFactory.Keys plain = factory.build("jane@example.com", "Rappel", null, null);
        EmailDedupKeyFactory.Keys noisy = factory.build("  JANE@Example.COM ", "Rappel", null, null);

        assertThat(plain.key()).isEqualTo(noisy.key());
    }

    @Test
    void shouldSeparateDifferentRecipientsSubjectsAndBusinessRecords() {
        EmailDedupKeyFactory.Keys base = factory.build("jane@example.com", "Facture INV-001", "BILLING", "INV-001");

        assertThat(factory.build("john@example.com", "Facture INV-001", "BILLING", "INV-001").key())
                .isNotEqualTo(base.key());
        assertThat(factory.build("jane@example.com", "Rappel de paiement", "BILLING", "INV-001").key())
                .isNotEqualTo(base.key());
        assertThat(factory.build("jane@example.com", "Facture INV-001", "BILLING", "INV-002").key())
                .isNotEqualTo(base.key());
    }

    @Test
    void shouldLookUpTheCurrentAndPreviousBucketSoWindowBoundariesDoNotLeak() {
        EmailDedupKeyFactory.Keys keys = factory.build("jane@example.com", "Rappel", null, null);

        assertThat(keys.lookupKeys()).hasSize(2);
        assertThat(keys.lookupKeys().getFirst()).isEqualTo(keys.key());
        assertThat(keys.lookupKeys().get(1)).isNotEqualTo(keys.key());
    }

    @Test
    void shouldKeyBusinessMailSeparatelyFromGenericMail() {
        // Same recipient and subject: only the business reference distinguishes them, and it must,
        // otherwise a transactional mail and a loose notification would suppress each other.
        EmailDedupKeyFactory.Keys business = factory.build("jane@example.com", "Facture", "BILLING", "INV-001");
        EmailDedupKeyFactory.Keys generic = factory.build("jane@example.com", "Facture", null, null);

        assertThat(business.key()).isNotEqualTo(generic.key());
    }

    @Test
    void shouldTreatAPartialBusinessReferenceAsGeneric() {
        EmailDedupKeyFactory.Keys typeOnly = factory.build("jane@example.com", "Facture", "BILLING", null);
        EmailDedupKeyFactory.Keys generic = factory.build("jane@example.com", "Facture", null, null);

        assertThat(typeOnly.key()).isEqualTo(generic.key());
    }

    @Test
    void shouldDisableDeduplicationWhenTurnedOffOrWhenTheWindowIsZero() {
        assertThat(factory(false, 86400, 3600).build("jane@example.com", "Rappel", null, null).active()).isFalse();
        assertThat(factory(true, 86400, 0).build("jane@example.com", "Rappel", null, null).active()).isFalse();
        assertThat(factory(true, 0, 3600).build("jane@example.com", "Facture", "BILLING", "INV-001").active()).isFalse();
    }

    @Test
    void shouldDisableDeduplicationWhenThereIsNoRecipient() {
        assertThat(factory.build("  ", "Rappel", null, null).active()).isFalse();
        assertThat(factory.build(null, "Rappel", null, null).active()).isFalse();
    }

    private EmailDedupKeyFactory factory(boolean enabled, long businessWindow, long genericWindow) {
        EmailDedupKeyFactory instance = new EmailDedupKeyFactory();
        ReflectionTestUtils.setField(instance, "enabled", enabled);
        ReflectionTestUtils.setField(instance, "businessWindowSeconds", businessWindow);
        ReflectionTestUtils.setField(instance, "genericWindowSeconds", genericWindow);
        return instance;
    }
}
