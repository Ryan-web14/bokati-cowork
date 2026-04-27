package com.sni.bokaticowork.features.subscription.repository;

import com.sni.bokaticowork.features.subscription.subscription.model.PassTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PassTransactionRepository extends JpaRepository<PassTransaction, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM subscription_pass_transaction WHERE pass_id = :passId ORDER BY created_at ASC")
    List<PassTransaction> findAllByPassOrderByCreatedAtAsc(@Param("passId") Long passId);
}
