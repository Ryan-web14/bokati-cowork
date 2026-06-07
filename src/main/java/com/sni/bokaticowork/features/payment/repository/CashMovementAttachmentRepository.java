package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.CashMovementAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CashMovementAttachmentRepository extends JpaRepository<CashMovementAttachment, Long> {

    List<CashMovementAttachment> findByCashMovement_IdOrderByUploadedAtDesc(Long cashMovementId);
}
