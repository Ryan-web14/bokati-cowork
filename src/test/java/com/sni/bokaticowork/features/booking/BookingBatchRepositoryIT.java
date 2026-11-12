package com.sni.bokaticowork.features.booking;

import com.sni.bokaticowork.features.booking.enums.BookingPaymentMode;
import com.sni.bokaticowork.features.booking.model.BookingBatch;
import com.sni.bokaticowork.features.booking.repository.BookingBatchRepository;
import com.sni.bokaticowork.features.booking.repository.BookingRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Le lot de reservations multi-ressources sur le schema de production : la table, la colonne du
 * lot sur la reservation, la sequence des numeros, et les requetes de lecture.
 */
class BookingBatchRepositoryIT extends PostgresIntegrationTestBase {

    @Autowired private EntityManager entityManager;
    @Autowired private BookingBatchRepository batches;
    @Autowired private BookingRepository bookings;

    @Test
    @DisplayName("Un lot se relit par son numero et par sa cle d'idempotence")
    void batchIsReadBack() {
        entityManager.createNativeQuery("""
                INSERT INTO booking_batch (id, batch_number, idempotency_key, payment_mode, billing_company_code,
                                           booking_count, member_count, notes, created_by)
                VALUES (994000001, 'BKL-IT-1', 'cle-it-1', 'DIRECT', NULL, 4, 2, 'Seminaire', 'admin@sni-cg.com')
                """).executeUpdate();
        entityManager.clear();

        BookingBatch batch = batches.findByBatchNumber("BKL-IT-1").orElseThrow();
        assertThat(batch.getPaymentMode()).isEqualTo(BookingPaymentMode.DIRECT);
        assertThat(batch.getBookingCount()).isEqualTo(4);
        assertThat(batch.getMemberCount()).isEqualTo(2);
        assertThat(batch.getCreatedAt()).isNotNull();
        assertThat(batches.findByIdempotencyKey("cle-it-1")).isPresent();
    }

    @Test
    @DisplayName("Les reservations d'un lot se lisent par son numero, colonne et sequence en place")
    void bookingsOfABatchQueryRuns() {
        assertThat(bookings.findAllByBatchNumber("BKL-IT-ABSENT")).isEmpty();

        Number column = (Number) entityManager.createNativeQuery("""
                SELECT count(*) FROM information_schema.columns
                WHERE table_name = 'booking' AND column_name = 'batch_number'
                """).getSingleResult();
        assertThat(column.intValue()).isEqualTo(1);

        Number sequence = (Number) entityManager.createNativeQuery(
                "SELECT count(*) FROM sequence_definition WHERE code = 'booking_batch'").getSingleResult();
        assertThat(sequence.intValue()).isEqualTo(1);
    }
}
