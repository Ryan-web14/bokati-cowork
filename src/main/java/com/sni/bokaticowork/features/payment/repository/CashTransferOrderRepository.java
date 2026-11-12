package com.sni.bokaticowork.features.payment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CashTransferOrderRepository extends JpaRepository<com.sni.bokaticowork.features.payment.model.CashTransferOrder, Long> {

    Optional<com.sni.bokaticowork.features.payment.model.CashTransferOrder> findByTransferNumber(String transferNumber);

    List<com.sni.bokaticowork.features.payment.model.CashTransferOrder> findByStatusOrderBySentAtAsc(com.sni.bokaticowork.features.payment.enums.CashTransferStatus status);

    @Query("SELECT t FROM CashTransferOrder t WHERE (t.fromRegister = :registerCode OR t.toRegister = :registerCode) ORDER BY t.sentAt DESC")
    List<com.sni.bokaticowork.features.payment.model.CashTransferOrder> findByRegister(@Param("registerCode") String registerCode);

    @Query("SELECT t FROM CashTransferOrder t WHERE t.toRegister = :registerCode AND t.kind = :kind AND t.status IN :statuses ORDER BY t.sentAt DESC")
    List<com.sni.bokaticowork.features.payment.model.CashTransferOrder> findIncoming(@Param("registerCode") String registerCode, @Param("kind") com.sni.bokaticowork.features.payment.enums.CashTransferKind kind, @Param("statuses") java.util.Collection<com.sni.bokaticowork.features.payment.enums.CashTransferStatus> statuses);
}
