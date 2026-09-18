package com.sni.bokaticowork.features.subscription.subscription.pass.repository;

import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PassCredentialRepository extends JpaRepository<PassCredential, Long> {

    /** Retrouve le pass depuis ce qui a ete presente a la borne. */
    @Query("SELECT c FROM PassCredential c WHERE c.value = :value AND c.revokedAt IS NULL")
    Optional<PassCredential> findActiveByValue(@Param("value") String value);

    @Query("SELECT c FROM PassCredential c WHERE c.pass.id = :passId AND c.revokedAt IS NULL")
    List<PassCredential> findActiveByPassId(@Param("passId") Long passId);

    @Query(nativeQuery = true, value = "SELECT EXISTS(SELECT 1 FROM pass_credential WHERE value = :value AND revoked_at IS NULL)")
    boolean existsActiveValue(@Param("value") String value);
}
