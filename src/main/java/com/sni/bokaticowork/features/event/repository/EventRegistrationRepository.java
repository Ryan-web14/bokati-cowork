package com.sni.bokaticowork.features.event.repository;

import com.sni.bokaticowork.features.event.enums.RegistrationStatus;
import com.sni.bokaticowork.features.event.model.EventRegistration;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {

    Optional<EventRegistration> findByEventIdAndEmailIgnoreCaseAndStatusNot(
            Long eventId, String email, RegistrationStatus status);

    @Query(nativeQuery = true, value = """
            SELECT r.*
            FROM event_registration r
            WHERE (CAST(:eventId AS BIGINT) IS NULL OR r.event_id = CAST(:eventId AS BIGINT))
              AND (CAST(:status AS TEXT) IS NULL OR r.status = CAST(:status AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(r.firstname) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(r.lastname) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(r.email) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR r.phone LIKE '%' || CAST(:searchText AS TEXT) || '%'
                OR r.registration_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            ORDER BY r.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM event_registration r
            WHERE (CAST(:eventId AS BIGINT) IS NULL OR r.event_id = CAST(:eventId AS BIGINT))
              AND (CAST(:status AS TEXT) IS NULL OR r.status = CAST(:status AS TEXT))
              AND (
                CAST(:searchText AS TEXT) IS NULL
                OR LOWER(r.firstname) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(r.lastname) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR LOWER(r.email) LIKE LOWER('%' || CAST(:searchText AS TEXT) || '%')
                OR r.phone LIKE '%' || CAST(:searchText AS TEXT) || '%'
                OR r.registration_number LIKE '%' || CAST(:searchText AS TEXT) || '%'
              )
            """)
    Page<EventRegistration> search(@Param("eventId") Long eventId,
                                    @Param("status") String status,
                                    @Param("searchText") String searchText,
                                    Pageable pageable);
}
