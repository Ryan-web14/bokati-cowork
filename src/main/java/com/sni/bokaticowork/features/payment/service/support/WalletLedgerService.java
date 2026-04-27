package com.sni.bokaticowork.features.payment.service.support;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.payment.enums.WalletEntryDirection;
import com.sni.bokaticowork.features.payment.enums.WalletEntryType;
import com.sni.bokaticowork.features.payment.model.WalletAccount;
import com.sni.bokaticowork.features.payment.model.WalletLedgerEntry;
import com.sni.bokaticowork.features.payment.repository.WalletAccountRepository;
import com.sni.bokaticowork.features.payment.repository.WalletLedgerEntryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class WalletLedgerService {

    private final WalletAccountRepository walletRepository;
    private final WalletLedgerEntryRepository ledgerRepository;
    private final SequenceGeneratorFacade sequenceGenerator;

    public WalletLedgerEntry credit(WalletAccount wallet, BigDecimal amount, WalletEntryType type, String sourceType, String sourceCode, String reference, String createdBy) {
        validateAmount(amount);
        BigDecimal normalized = money(amount);
        wallet.setLedgerBalance(wallet.getLedgerBalance().add(normalized));
        wallet.setAvailableBalance(wallet.getAvailableBalance().add(normalized));
        WalletAccount savedWallet = walletRepository.save(wallet);
        return ledgerRepository.save(entry(savedWallet, WalletEntryDirection.CREDIT, normalized, type, sourceType, sourceCode, reference, createdBy));
    }

    public WalletLedgerEntry debit(WalletAccount wallet, BigDecimal amount, WalletEntryType type, String sourceType, String sourceCode, String reference, String createdBy) {
        validateAmount(amount);
        BigDecimal normalized = money(amount);
        if (wallet.getAvailableBalance().compareTo(normalized) < 0) {
            throw new BadRequestException("Insufficient wallet balance");
        }
        wallet.setLedgerBalance(wallet.getLedgerBalance().subtract(normalized));
        wallet.setAvailableBalance(wallet.getAvailableBalance().subtract(normalized));
        WalletAccount savedWallet = walletRepository.save(wallet);
        return ledgerRepository.save(entry(savedWallet, WalletEntryDirection.DEBIT, normalized, type, sourceType, sourceCode, reference, createdBy));
    }

    public WalletLedgerEntry entryOnly(WalletAccount wallet, BigDecimal amount, WalletEntryType type, String sourceType, String sourceCode, String reference, String createdBy) {
        validateAmount(amount);
        WalletEntryDirection direction = type == WalletEntryType.HOLD_RELEASE ? WalletEntryDirection.CREDIT : WalletEntryDirection.DEBIT;
        return ledgerRepository.save(entry(wallet, direction, money(amount), type, sourceType, sourceCode, reference, createdBy));
    }

    private WalletLedgerEntry entry(WalletAccount wallet, WalletEntryDirection direction, BigDecimal amount, WalletEntryType type, String sourceType, String sourceCode, String reference, String createdBy) {
        return WalletLedgerEntry.builder()
                .entryNumber(sequenceGenerator.next("wallet_entry"))
                .wallet(wallet)
                .direction(direction)
                .amount(amount)
                .currency(wallet.getCurrency())
                .balanceAfter(wallet.getLedgerBalance())
                .entryType(type)
                .sourceType(trim(sourceType))
                .sourceCode(trim(sourceCode))
                .reference(trim(reference))
                .createdBy(trim(createdBy))
                .build();
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new BadRequestException("Wallet amount must be positive");
        }
    }

    private BigDecimal money(BigDecimal amount) {
        return amount.setScale(4, RoundingMode.HALF_UP);
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
