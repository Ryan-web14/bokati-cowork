package com.sni.bokaticowork.features.billing.repository;

import com.sni.bokaticowork.features.billing.model.SecurityDepositMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/** Lot C7 : mouvements des depots de garantie. */
@Repository
public interface SecurityDepositMovementRepository extends JpaRepository<SecurityDepositMovement, Long> {

    List<SecurityDepositMovement> findByDepositIdOrderByCreatedAtAsc(Long depositId);

    Optional<SecurityDepositMovement> findByMovementNumber(String movementNumber);
}
