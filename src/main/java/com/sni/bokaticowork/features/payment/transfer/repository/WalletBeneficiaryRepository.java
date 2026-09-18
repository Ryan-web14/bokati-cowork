package com.sni.bokaticowork.features.payment.transfer.repository;

import com.sni.bokaticowork.features.payment.transfer.model.WalletBeneficiary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletBeneficiaryRepository extends JpaRepository<WalletBeneficiary, Long> {

    List<WalletBeneficiary> findByOwnerWallet_IdOrderByAliasAsc(Long ownerWalletId);

    Optional<WalletBeneficiary> findByOwnerWallet_IdAndBeneficiaryWallet_Id(Long ownerWalletId, Long beneficiaryWalletId);

    Optional<WalletBeneficiary> findByIdAndOwnerWallet_Id(Long id, Long ownerWalletId);
}
