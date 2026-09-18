package com.sni.bokaticowork.features.subscription.subscription.pass.repository;

import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassTransferStatus;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PassTransferRepository extends JpaRepository<PassTransfer, Long> {

    @Query("SELECT t FROM PassTransfer t WHERE t.transferNumber = :transferNumber")
    Optional<PassTransfer> findByNumber(@Param("transferNumber") String transferNumber);

    @Query("""
            SELECT t FROM PassTransfer t
            WHERE t.pass.id = :passId
              AND t.status = com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassTransferStatus.PENDING_ACCEPTANCE
            """)
    Optional<PassTransfer> findPending(@Param("passId") Long passId);

    @Query("SELECT COUNT(t) FROM PassTransfer t WHERE t.pass.id = :passId AND t.status = :status")
    long countByStatus(@Param("passId") Long passId, @Param("status") PassTransferStatus status);
}
