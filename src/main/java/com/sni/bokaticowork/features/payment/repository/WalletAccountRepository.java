package com.sni.bokaticowork.features.payment.repository;

import com.sni.bokaticowork.features.payment.model.WalletAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WalletAccountRepository extends JpaRepository<WalletAccount, Long> {

    @Query(nativeQuery = true, value = "SELECT * FROM wallet_account WHERE wallet_number = :walletNumber")
    Optional<WalletAccount> findByWalletNumber(@Param("walletNumber") String walletNumber);

    @Query(nativeQuery = true, value = """
            SELECT *
            FROM wallet_account
            WHERE owner_type = :ownerType
              AND owner_code = :ownerCode
              AND currency = :currency
            """)
    Optional<WalletAccount> findByOwnerAndCurrency(@Param("ownerType") String ownerType,
                                                   @Param("ownerCode") String ownerCode,
                                                   @Param("currency") String currency);

    @Query(value = """
            SELECT *
            FROM wallet_account
            WHERE (:ownerType IS NULL OR owner_type = :ownerType)
              AND (:ownerCode IS NULL OR owner_code = :ownerCode)
            ORDER BY created_at DESC
            """,
           countQuery = """
            SELECT COUNT(*)
            FROM wallet_account
            WHERE (:ownerType IS NULL OR owner_type = :ownerType)
              AND (:ownerCode IS NULL OR owner_code = :ownerCode)
            """,
           nativeQuery = true)
    Page<WalletAccount> list(@Param("ownerType") String ownerType,
                             @Param("ownerCode") String ownerCode,
                             Pageable pageable);
}
