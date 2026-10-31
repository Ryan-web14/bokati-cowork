package com.sni.bokaticowork.features.company.sharing.repository;

import com.sni.bokaticowork.features.company.sharing.model.SeatAllowance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeatAllowanceRepository extends JpaRepository<SeatAllowance, Long> {

    Optional<SeatAllowance> findBySeat_IdAndEntitlementCodeIgnoreCase(Long seatId, String entitlementCode);

    List<SeatAllowance> findAllBySeat_IdIn(Collection<Long> seatIds);
}
