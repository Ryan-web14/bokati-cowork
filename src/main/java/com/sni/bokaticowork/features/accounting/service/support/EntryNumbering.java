package com.sni.bokaticowork.features.accounting.service.support;

import com.sni.bokaticowork.features.accounting.enums.EntryStatus;
import com.sni.bokaticowork.features.accounting.enums.NumberingReset;
import com.sni.bokaticowork.features.accounting.model.AccEntry;
import com.sni.bokaticowork.features.accounting.model.AccEntryLine;
import com.sni.bokaticowork.features.accounting.model.AccJournal;
import com.sni.bokaticowork.features.accounting.model.AccNumberingCounter;
import com.sni.bokaticowork.features.accounting.repository.AccEntryRepository;
import com.sni.bokaticowork.features.accounting.repository.AccNumberingCounterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;

/**
 * Numerotation continue et protegee des pieces (plan, section 4.8).
 *
 * <p>Le numero definitif s'attribue a la validation seulement, dans la meme transaction : le
 * compteur du journal est verrouille, incremente et ecrit avec la piece. Si la validation echoue,
 * la transaction est annulee et le numero n'est pas consomme. Le moteur de sequences de
 * {@code core/generator}, qui prend ses numeros dans une transaction separee, n'est donc pas
 * utilise ici : il laisserait des trous.</p>
 *
 * <p>Chaque piece validee garde l'empreinte de son contenu et de la piece precedente du journal
 * dans l'exercice ; toute alteration en base casse la chaine.</p>
 */
@Component
@RequiredArgsConstructor
public class EntryNumbering {

    public static final EnumSet<EntryStatus> NUMBERED = EnumSet.of(EntryStatus.VALIDATED, EntryStatus.REVERSED);

    private final AccNumberingCounterRepository counterRepository;
    private final AccEntryRepository entryRepository;
    private final AccountingSettings settings;

    /** Attribue le numero definitif ; doit etre appele dans la transaction de validation. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void assign(AccEntry entry, AccJournal journal, List<AccEntryLine> lines) {
        String period = journal.getNumberingReset() == NumberingReset.PERIOD ? entry.getPeriodCode() : "";
        AccNumberingCounter counter = counterRepository.lock(journal.getCode(), entry.getFiscalYearCode(), period)
                .orElseGet(() -> {
                    counterRepository.saveAndFlush(AccNumberingCounter.builder().journalCode(journal.getCode())
                            .fiscalYearCode(entry.getFiscalYearCode()).periodCode(period).lastNumber(0).build());
                    return counterRepository.lock(journal.getCode(), entry.getFiscalYearCode(), period).orElseThrow();
                });
        int next = counter.getLastNumber() + 1;
        counter.setLastNumber(next);
        counterRepository.save(counter);
        String sequence = String.format("%0" + settings.numberingWidth() + "d", next);
        String number = journal.getCode() + "-" + (period.isEmpty() ? entry.getFiscalYearCode() : period) + "-" + sequence;
        entry.setSequenceNumber(next);
        entry.setEntryNumber(number);
        entry.setPreviousHash(previousHash(journal.getCode(), entry.getFiscalYearCode(), entry.getPeriodCode(), period, next));
        entry.setContentHash(hash(entry, lines, entry.getPreviousHash()));
    }

    private String previousHash(String journal, String fiscalYear, String periodCode, String counterPeriod, int sequence) {
        if (sequence <= 1) {
            return genesis(journal, counterPeriod.isEmpty() ? fiscalYear : periodCode);
        }
        return entryRepository.findNumbered(journal, fiscalYear, NUMBERED).stream()
                .filter(previous -> previous.getSequenceNumber() != null && previous.getSequenceNumber() == sequence - 1
                        && (counterPeriod.isEmpty() || periodCode.equals(previous.getPeriodCode())))
                .map(AccEntry::getContentHash)
                .findFirst()
                .orElse(genesis(journal, counterPeriod.isEmpty() ? fiscalYear : periodCode));
    }

    public static String genesis(String journal, String scope) {
        return sha256("GENESIS|" + journal + "|" + scope);
    }

    /** Empreinte du contenu d'une piece, chainee a l'empreinte precedente. */
    public static String hash(AccEntry entry, List<AccEntryLine> lines, String previousHash) {
        StringBuilder content = new StringBuilder();
        content.append(entry.getEntryNumber()).append('|').append(entry.getJournalCode()).append('|')
                .append(entry.getEntryDate()).append('|').append(entry.getLabel()).append('|')
                .append(plain(entry.getTotalDebit())).append('|').append(plain(entry.getTotalCredit())).append('|')
                .append(previousHash);
        for (AccEntryLine line : lines) {
            content.append('|').append(line.getLineNo()).append(':').append(line.getAccountNumber()).append(':')
                    .append(line.getThirdPartyCode() == null ? "" : line.getThirdPartyCode()).append(':')
                    .append(plain(line.getDebit())).append(':').append(plain(line.getCredit()));
        }
        return sha256(content.toString());
    }

    private static String plain(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
