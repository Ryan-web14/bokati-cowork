package com.sni.bokaticowork.features.fund.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ForbiddenException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.service.support.FrenchAmountWords;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.FundNeedStatus;
import com.sni.bokaticowork.features.fund.enums.FundUrgency;
import com.sni.bokaticowork.features.fund.model.FundNeedLine;
import com.sni.bokaticowork.features.fund.model.FundNeedStatement;
import com.sni.bokaticowork.features.fund.repository.FundNeedLineRepository;
import com.sni.bokaticowork.features.fund.repository.FundNeedStatementRepository;
import com.sni.bokaticowork.features.inventory.procurement.repository.SupplierRepository;
import com.sni.bokaticowork.features.inventory.procurement.suggestion.service.LinkedSupplier;
import com.sni.bokaticowork.features.payment.model.CashOperationNature;
import com.sni.bokaticowork.features.payment.repository.CashOperationNatureRepository;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Etats de besoin (lot F5, plan demandes-et-justification section 3) : brouillon, soumis, valide ou
 * rejete, puis transforme (en tout ou en partie) en demande de fonds.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FundNeedService {

    private final FundNeedStatementRepository statementRepository;
    private final FundNeedLineRepository lineRepository;
    private final CashOperationNatureRepository natureRepository;
    private final FundRequestService requests;
    private final SequenceGeneratorFacade sequences;
    private final CashActorResolver actors;
    private final SupplierRepository supplierRepository;

    public FundDtos.Need create(FundDtos.NeedRequest input) {
        FundNeedStatement statement = statementRepository.save(FundNeedStatement.builder().number(sequences.next("fund_need_statement"))
                .requester(requests.actor()).subject(input.subject().trim()).status(FundNeedStatus.DRAFT).build());
        apply(statement, input);
        return view(statement);
    }

    public FundDtos.Need update(String number, FundDtos.NeedRequest input) {
        FundNeedStatement statement = statement(number);
        if (statement.getStatus() != FundNeedStatus.DRAFT) {
            throw new BadRequestException("Seul un etat de besoin en brouillon se modifie");
        }
        owner(statement);
        statement.setSubject(input.subject().trim());
        apply(statement, input);
        return view(statement);
    }

    private void apply(FundNeedStatement statement, FundDtos.NeedRequest input) {
        statement.setDepartment(FundRequestService.trim(input.department()));
        statement.setLocationCode(FundRequestService.trim(input.locationCode()));
        statement.setCostCenter(FundRequestService.trim(input.costCenter()));
        statement.setUrgency(input.urgency() == null ? FundUrgency.NORMAL : input.urgency());
        statement.setWantedDate(input.wantedDate());
        statement.setAttachments(input.attachments());
        lineRepository.deleteByStatementId(statement.getId());
        lineRepository.flush();
        BigDecimal total = BigDecimal.ZERO;
        int no = 1;
        for (FundDtos.NeedLineRequest line : input.lines()) {
            BigDecimal quantity = line.quantity() == null ? BigDecimal.ONE : line.quantity();
            BigDecimal price = line.unitPrice() == null ? BigDecimal.ZERO : line.unitPrice();
            if (quantity.signum() <= 0 || price.signum() < 0) {
                throw new BadRequestException("Ligne " + no + " : quantite positive, prix estime positif ou nul");
            }
            BigDecimal amount = FundRequestService.money(quantity.multiply(price));
            String nature = StringUtils.hasText(line.natureCode()) ? line.natureCode().trim().toUpperCase(Locale.ROOT) : suggestNature(line.designation());
            LinkedSupplier.Resolved supplier = LinkedSupplier.resolve(supplierRepository, line.supplierCode(), line.supplier(), no);
            lineRepository.save(FundNeedLine.builder().statementId(statement.getId()).lineNo(no++).designation(line.designation().trim())
                    .quantity(quantity).unit(FundRequestService.trim(line.unit())).unitPrice(FundRequestService.money(price)).totalAmount(amount)
                    .natureCode(nature).supplier(supplier.name()).supplierCode(supplier.code()).build());
            total = total.add(amount);
        }
        statement.setTotalAmount(total);
        statementRepository.save(statement);
    }

    /** Nature suggeree par les mots de la designation (mots-cles des natures). */
    String suggestNature(String designation) {
        if (!StringUtils.hasText(designation)) {
            return null;
        }
        return guessNature(natureRepository.findAll(), designation);
    }

    /** Nature active dont un mot-cle figure dans la designation ; la plus utilisee l'emporte. */
    public static String guessNature(Collection<CashOperationNature> natures, String designation) {
        if (!StringUtils.hasText(designation)) {
            return null;
        }
        String text = designation.toLowerCase(Locale.ROOT);
        return natures.stream()
                .filter(nature -> Boolean.TRUE.equals(nature.getActive()) && StringUtils.hasText(nature.getKeywords()))
                .filter(nature -> Arrays.stream(nature.getKeywords().toLowerCase(Locale.ROOT).split("[,; ]+"))
                        .anyMatch(word -> word.length() > 2 && text.contains(word)))
                .max(Comparator.comparing(nature -> nature.getUsageCount() == null ? 0L : nature.getUsageCount()))
                .map(CashOperationNature::getCode).orElse(null);
    }

    public FundDtos.Need submit(String number) {
        FundNeedStatement statement = statement(number);
        if (statement.getStatus() != FundNeedStatus.DRAFT) {
            throw new BadRequestException("L'etat de besoin " + number + " est deja " + statement.getStatus());
        }
        owner(statement);
        statement.setStatus(FundNeedStatus.SUBMITTED);
        statement.setSubmittedAt(Instant.now());
        return view(statementRepository.save(statement));
    }

    public FundDtos.Need decide(String number, boolean validate, String note) {
        FundNeedStatement statement = statement(number);
        if (statement.getStatus() != FundNeedStatus.SUBMITTED) {
            throw new BadRequestException("L'etat de besoin " + number + " n'est pas soumis");
        }
        String actor = requests.actor();
        if (CashActorResolver.samePerson(actor, statement.getRequester())) {
            throw new ForbiddenException("On ne valide pas son propre etat de besoin");
        }
        if (!validate && !StringUtils.hasText(note)) {
            throw new BadRequestException("Indiquez le motif du rejet");
        }
        statement.setStatus(validate ? FundNeedStatus.VALIDATED : FundNeedStatus.REJECTED);
        statement.setDecidedBy(actor);
        statement.setDecidedAt(Instant.now());
        statement.setDecisionNote(FundRequestService.trim(note));
        return view(statementRepository.save(statement));
    }

    /** Transformation (toutes les lignes, ou celles choisies) en demande de fonds, en brouillon. */
    public FundDtos.Request transform(String number, FundDtos.TransformNeed input) {
        FundNeedStatement statement = statement(number);
        if (statement.getStatus() != FundNeedStatus.VALIDATED && statement.getStatus() != FundNeedStatus.PARTIALLY_TRANSFORMED) {
            throw new BadRequestException("Seul un etat de besoin valide se transforme");
        }
        owner(statement);
        List<FundNeedLine> lines = lineRepository.findByStatementIdOrderByLineNoAsc(statement.getId()).stream()
                .filter(line -> line.getTransformedRequestNumber() == null)
                .filter(line -> input.lineNos() == null || input.lineNos().isEmpty() || input.lineNos().contains(line.getLineNo()))
                .filter(line -> line.getTotalAmount().signum() > 0)
                .toList();
        if (lines.isEmpty()) {
            throw new BadRequestException("Aucune ligne chiffree restant a transformer");
        }
        FundDtos.Request request = requests.create(new FundDtos.RequestInput(input.type(), statement.getSubject(), statement.getDepartment(), null,
                null, null, null, null, null, input.registerCode(), null, statement.getWantedDate(), null, null, null, false, null,
                lines.stream().map(line -> new FundDtos.RequestLine(line.getNatureCode(), line.getDesignation(), line.getTotalAmount(),
                        line.getSupplier(), statement.getCostCenter(), line.getSupplierCode())).toList(), statement.getAttachments(), false));
        requests.request(request.number()).setNeedStatementNumber(statement.getNumber());
        lines.forEach(line -> {
            line.setTransformedRequestNumber(request.number());
            lineRepository.save(line);
        });
        boolean all = lineRepository.findByStatementIdOrderByLineNoAsc(statement.getId()).stream()
                .allMatch(line -> line.getTransformedRequestNumber() != null || line.getTotalAmount().signum() == 0);
        statement.setStatus(all ? FundNeedStatus.TRANSFORMED : FundNeedStatus.PARTIALLY_TRANSFORMED);
        statementRepository.save(statement);
        return requests.get(request.number());
    }

    @Transactional(readOnly = true)
    public FundDtos.Need get(String number) {
        return view(statement(number));
    }

    @Transactional(readOnly = true)
    public List<FundDtos.Need> mine() {
        return statementRepository.findByRequesterOrderByCreatedAtDesc(requests.actor()).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<FundDtos.Need> submitted() {
        return statementRepository.findByStatusOrderByCreatedAtAsc(FundNeedStatus.SUBMITTED).stream().map(this::view).toList();
    }

    private FundDtos.Need view(FundNeedStatement statement) {
        List<FundDtos.NeedLine> lines = lineRepository.findByStatementIdOrderByLineNoAsc(statement.getId()).stream()
                .map(line -> new FundDtos.NeedLine(line.getLineNo(), line.getDesignation(), line.getQuantity(), line.getUnit(), line.getUnitPrice(),
                        line.getTotalAmount(), line.getNatureCode(), line.getSupplier(), line.getTransformedRequestNumber(), line.getSupplierCode())).toList();
        return new FundDtos.Need(statement.getNumber(), statement.getRequester(), statement.getDepartment(), statement.getLocationCode(),
                statement.getCostCenter(), statement.getSubject(), statement.getUrgency(), statement.getWantedDate(), statement.getStatus(),
                statement.getTotalAmount(), FrenchAmountWords.withCurrency(statement.getTotalAmount(), "XAF"), statement.getSubmittedAt(),
                statement.getDecidedBy(), statement.getDecidedAt(), statement.getDecisionNote(), statement.getAttachments(), statement.getCreatedAt(), lines);
    }

    private FundNeedStatement statement(String number) {
        return statementRepository.findByNumber(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Etat de besoin introuvable : " + number));
    }

    private void owner(FundNeedStatement statement) {
        if (!CashActorResolver.samePerson(requests.actor(), statement.getRequester())) {
            throw new ForbiddenException("Seul l'auteur de l'etat de besoin agit dessus a ce stade");
        }
    }
}
