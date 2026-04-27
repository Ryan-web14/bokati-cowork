package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.payment.dto.request.WalletTopUpRequest;
import com.sni.bokaticowork.features.payment.dto.response.WalletLedgerEntryResponse;
import com.sni.bokaticowork.features.payment.dto.response.WalletResponse;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.enums.WalletStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.PaymentMapper;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import com.sni.bokaticowork.features.payment.service.interfaces.WalletService;
import com.sni.bokaticowork.features.payment.service.support.WalletLedgerService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@Transactional
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final WalletLedgerService ledgerService;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final PaymentMapper mapper;

    @Override
    public WalletResponse adminTopUp(WalletTopUpRequest request) {
        WalletAccount wallet = getOrCreateWallet(request.ownerType(), request.ownerCode(), request.currency());
        ledgerService.credit(wallet, request.amount(), WalletEntryType.ADMIN_TOPUP, "ADMIN_TOPUP", request.reference(), request.reference(), request.createdBy());
        return mapper.toWalletResponse(walletRepository.findByWalletNumber(wallet.getWalletNumber()).orElse(wallet));
    }

    @Override
    public WalletResponse getOrCreate(String ownerType, String ownerCode, String currency) {
        return mapper.toWalletResponse(getOrCreateWallet(ownerType, ownerCode, currency));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse get(String walletNumber) {
        return mapper.toWalletResponse(serviceWallet(walletNumber));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<WalletResponse> list(String ownerType, String ownerCode, Pageable pageable) {
        String normalizedOwnerType = StringUtils.hasText(ownerType) ? ownerType.trim() : null;
        String normalizedOwnerCode = StringUtils.hasText(ownerCode) ? ownerCode.trim() : null;
        Pageable unsortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return new PaginatedResponse<>(walletRepository.list(normalizedOwnerType, normalizedOwnerCode, unsortedPageable)
                .map(mapper::toWalletResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginatedResponse<WalletLedgerEntryResponse> ledger(String walletNumber, Pageable pageable) {
        WalletAccount wallet = serviceWallet(walletNumber);
        return new PaginatedResponse<>(ledgerRepository.findAllByWalletIdOrderByCreatedAtDesc(wallet.getId(), pageable).map(mapper::toLedgerEntryResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public WalletAccount serviceWallet(String walletNumber) {
        if (!StringUtils.hasText(walletNumber)) {
            throw new BadRequestException("Wallet number is required");
        }
        return walletRepository.findByWalletNumber(walletNumber.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found"));
    }

    @Override
    public void credit(WalletAccount wallet, BigDecimal amount, WalletEntryType entryType, String sourceType, String sourceCode, String reference, String createdBy) {
        ledgerService.credit(wallet, amount, entryType, sourceType, sourceCode, reference, createdBy);
    }

    @Override
    public void debit(WalletAccount wallet, BigDecimal amount, WalletEntryType entryType, String sourceType, String sourceCode, String reference, String createdBy) {
        ensureUsable(wallet);
        ledgerService.debit(wallet, amount, entryType, sourceType, sourceCode, reference, createdBy);
    }

    private WalletAccount getOrCreateWallet(String ownerType, String ownerCode, String currency) {
        if (!StringUtils.hasText(ownerType) || !StringUtils.hasText(ownerCode) || !StringUtils.hasText(currency)) {
            throw new BadRequestException("Wallet owner type, owner code and currency are required");
        }
        String normalizedCurrency = currency.trim().toUpperCase();
        return walletRepository.findByOwnerAndCurrency(ownerType.trim(), ownerCode.trim(), normalizedCurrency)
                .orElseGet(() -> walletRepository.save(WalletAccount.builder()
                        .walletNumber(sequenceGenerator.next("wallet_account"))
                        .ownerType(ownerType.trim())
                        .ownerCode(ownerCode.trim())
                        .currency(normalizedCurrency)
                        .status(WalletStatus.ACTIVE)
                        .availableBalance(BigDecimal.ZERO)
                        .ledgerBalance(BigDecimal.ZERO)
                        .heldBalance(BigDecimal.ZERO)
                        .build()));
    }

    private void ensureUsable(WalletAccount wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BadRequestException("Wallet is not active");
        }
    }
}
