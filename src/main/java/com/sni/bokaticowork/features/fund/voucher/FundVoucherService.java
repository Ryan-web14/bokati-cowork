package com.sni.bokaticowork.features.fund.voucher;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.features.fund.dto.FundDtos;
import com.sni.bokaticowork.features.fund.enums.FundDisbursementKind;
import com.sni.bokaticowork.features.fund.enums.FundRequestType;
import com.sni.bokaticowork.features.fund.service.FundNeedService;
import com.sni.bokaticowork.features.fund.service.FundRequestService;
import com.sni.bokaticowork.features.payment.service.support.CashActorResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Pieces des demandes de fonds (lot F7) : etat de besoin ({@code EDB}), demande de fonds
 * ({@code DDF}), bon de decaissement ({@code BDF}) et fiche de justification d'avance
 * ({@code JUS}). Les bons d'entree et de sortie de caisse restent produits par le module caisse.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class FundVoucherService {

    private static final String[] TYPE_LABELS = {"Depense directe", "Avance a justifier", "Remboursement de frais", "Paiement fournisseur",
            "Acompte au personnel", "Approvisionnement de caisse", "Remise de fonds"};

    private final FundNeedService needs;
    private final FundRequestService requests;
    private final VoucherService vouchers;
    private final CashActorResolver actors;

    public byte[] need(String number) {
        FundDtos.Need need = needs.get(number);
        VoucherService.Issued issued = vouchers.issueAndPrint("EDB", "FUND_NEED", need.number(), need.totalAmount(), need.subject(), actor());
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Demandeur").pair("Demandeur", need.requester()).pair("Service", need.department())
                .pair("Site", need.locationCode()).pair("Centre de cout", need.costCenter()).pair("Etat de besoin", need.number())
                .pair("Date", VoucherService.date(need.createdAt())));
        sections.add(new VoucherService.Section("Objet").pair("Objet", need.subject()).pair("Urgence", urgency(need.urgency().name()))
                .pair("Date souhaitee", need.wantedDate()).pair("Etat", need.status()));
        VoucherService.Section lines = new VoucherService.Section("Besoins").columns(
                List.of("N°", "Designation", "Quantite", "Unite", "Prix estime", "Total", "Nature", "Fournisseur suggere"),
                List.of(false, false, true, false, true, true, false, false));
        need.lines().forEach(line -> lines.row(line.lineNo(), line.designation(), line.quantity().stripTrailingZeros().toPlainString(), line.unit(),
                VoucherService.money(line.unitPrice()), VoucherService.money(line.totalAmount()), line.natureCode(), line.supplier()));
        sections.add(lines);
        if (need.decidedBy() != null) {
            sections.add(new VoucherService.Section("Decision").pair("Decide par", need.decidedBy()).pair("Le", VoucherService.date(need.decidedAt()))
                    .pair("Commentaire", need.decisionNote()));
        }
        List<String> follow = need.lines().stream().map(FundDtos.NeedLine::transformedRequestNumber).filter(java.util.Objects::nonNull).distinct().toList();
        if (!follow.isEmpty()) {
            sections.add(new VoucherService.Section("Suite donnee").pair("Demandes de fonds", String.join(", ", follow)));
        }
        return vouchers.render("Etat de besoin", issued, sections, need.totalAmount(), null, List.of(
                new VoucherService.Signature("Demandeur", null, null),
                new VoucherService.Signature("Chef de service", need.decidedBy(), need.decidedBy() == null ? null : VoucherService.date(need.decidedAt())),
                new VoucherService.Signature("Responsable financier", null, null)));
    }

    public byte[] request(String number) {
        FundDtos.Request request = requests.get(number);
        VoucherService.Issued issued = vouchers.issueAndPrint("DDF", "FUND_REQUEST", request.number(), request.totalAmount(), request.subject(), actor());
        List<VoucherService.Section> sections = header(request);
        sections.add(lines(request));
        if (request.insight() != null) {
            sections.add(new VoucherService.Section("Au jour de la demande").pair("Niveaux requis", String.join(", ", request.insight().levelLabels()))
                    .pair("Avances non soldees du beneficiaire", request.insight().openAdvances().isEmpty() ? "Aucune"
                            : String.join(" ; ", request.insight().openAdvances())));
        }
        sections.add(timeline(request));
        List<VoucherService.Signature> signatures = new ArrayList<>();
        signatures.add(new VoucherService.Signature("Demandeur", request.requester(), VoucherService.date(request.submittedAt())));
        request.timeline().stream().filter(step -> step.level() != null && step.level() > 0).forEach(step -> signatures.add(
                new VoucherService.Signature("Niveau " + step.level() + " : " + step.decision(), step.approver(), VoucherService.date(step.decidedAt()))));
        return vouchers.render("Demande de fonds", issued, sections, request.totalAmount(), null, signatures);
    }

    /** Bon de decaissement d'un mouvement de la demande (decaissement ou complement). */
    public byte[] disbursement(String number, String movementNumber) {
        FundDtos.Request request = requests.get(number);
        FundDtos.Disbursement item = request.disbursements().stream().filter(candidate -> candidate.movementNumber().equals(movementNumber))
                .filter(candidate -> candidate.kind() != FundDisbursementKind.RETURN)
                .findFirst().orElseThrow(() -> new BadRequestException("Aucun decaissement " + movementNumber + " sur la demande " + number));
        VoucherService.Issued issued = vouchers.issueAndPrint("BDF", "FUND_DISBURSEMENT", movementNumber, item.amount(), request.subject(), actor());
        List<VoucherService.Section> sections = header(request);
        sections.add(lines(request));
        BigDecimal approved = request.approvedAmount() == null ? request.totalAmount() : request.approvedAmount();
        sections.add(new VoucherService.Section("Decaissement").pair("Mouvement de caisse", movementNumber).pair("Session", item.sessionNumber())
                .pair("Caissiere", item.createdBy()).pair("Date", VoucherService.date(item.createdAt()))
                .pair("Montant approuve", VoucherService.money(approved) + " XAF").pair("Montant decaisse", VoucherService.money(item.amount()) + " XAF")
                .pair("Deja decaisse (total)", VoucherService.money(request.disbursedAmount()) + " XAF")
                .pair("Reste a decaisser", VoucherService.money(request.remainingToDisburse()) + " XAF")
                .pair("Imputation", request.type() == FundRequestType.ADVANCE || request.type() == FundRequestType.STAFF_ADVANCE
                        ? "D 421 " + request.beneficiaryName() + " / C caisse" : "D charges des natures / C caisse"));
        sections.add(timeline(request));
        String commitment = request.type() == FundRequestType.ADVANCE && request.justificationDueDate() != null
                ? "Je soussigne(e) " + request.beneficiaryName() + " reconnais avoir recu la somme ci-dessus et m'engage a la justifier avant le "
                + request.justificationDueDate() + " ou a la restituer."
                : "Je soussigne(e) " + request.beneficiaryName() + " reconnais avoir recu la somme ci-dessus.";
        return vouchers.render(item.kind() == FundDisbursementKind.COMPLEMENT ? "Bon de decaissement (complement)" : "Bon de decaissement de fonds",
                issued, sections, item.amount(), commitment, List.of(
                        new VoucherService.Signature("Beneficiaire" + (item.signedByName() == null ? "" : " : " + item.signedByName()), null, null),
                        new VoucherService.Signature("Caissiere", item.createdBy(), VoucherService.date(item.createdAt()))));
    }

    /** Fiche de justification d'une avance. */
    public byte[] justification(String number) {
        FundDtos.Request request = requests.get(number);
        if (request.type() != FundRequestType.ADVANCE) {
            throw new BadRequestException("La demande " + number + " n'est pas une avance");
        }
        BigDecimal validated = request.justification().stream().filter(line -> line.status().name().equals("VALIDATED"))
                .map(FundDtos.ExpenseLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rejected = request.justification().stream().filter(line -> line.status().name().equals("REJECTED"))
                .map(FundDtos.ExpenseLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        VoucherService.Issued issued = vouchers.issueAndPrint("JUS", "FUND_JUSTIFICATION", request.number(), validated, request.subject(), actor());
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Avance").pair("Demande", request.number()).pair("Objet", request.subject())
                .pair("Beneficiaire", request.beneficiaryName()).pair("Mission", request.missionDestination())
                .pair("Du", request.missionStart()).pair("Au", request.missionEnd()).pair("Echeance", request.justificationDueDate())
                .pair("Bons de decaissement", String.join(", ", request.disbursements().stream()
                        .filter(item -> item.kind() != FundDisbursementKind.RETURN).map(FundDtos.Disbursement::movementNumber).toList())));
        VoucherService.Section table = new VoucherService.Section("Depenses").columns(
                List.of("Date", "Fournisseur", "Nature", "Designation", "HT", "TVA", "TTC", "Piece", "Statut"),
                List.of(false, false, false, false, true, true, true, false, false));
        request.justification().forEach(line -> table.row(line.expenseDate(), line.supplier(), line.natureCode(), line.designation(),
                VoucherService.money(line.amount().subtract(line.vatAmount())), VoucherService.money(line.vatAmount()), VoucherService.money(line.amount()),
                line.documentReference() == null ? (Boolean.TRUE.equals(line.honourStatement()) ? "Attestation" : "") : line.documentReference(),
                line.status().name().equals("VALIDATED") ? "Validee" : line.status().name().equals("REJECTED") ? "Rejetee : " + line.rejectReason() : "A valider"));
        sections.add(table);
        BigDecimal given = request.disbursedAmount().add(request.complementAmount());
        BigDecimal balance = given.subtract(validated).subtract(request.returnedAmount());
        sections.add(new VoucherService.Section("Solde").pair("Avance recue", VoucherService.money(request.disbursedAmount()) + " XAF")
                .pair("Complement verse", VoucherService.money(request.complementAmount()) + " XAF")
                .pair("Total justifie", VoucherService.money(validated) + " XAF").pair("Total rejete", VoucherService.money(rejected) + " XAF")
                .pair("Reliquat rendu", VoucherService.money(request.returnedAmount()) + " XAF")
                .pair(balance.signum() >= 0 ? "Reste a rendre" : "Complement du", VoucherService.money(balance.abs()) + " XAF")
                .pair("Etat", request.status()));
        String validator = request.justification().stream().map(FundDtos.ExpenseLine::validatedBy).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return vouchers.render("Fiche de justification d'avance", issued, sections, validated, null, List.of(
                new VoucherService.Signature("Beneficiaire", null, null),
                new VoucherService.Signature("Comptable valideur", validator, null)));
    }

    private List<VoucherService.Section> header(FundDtos.Request request) {
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Demande").pair("Numero", request.number()).pair("Type", TYPE_LABELS[request.type().ordinal()])
                .pair("Objet", request.subject()).pair("Date", VoucherService.date(request.createdAt()))
                .pair("Etat de besoin", request.needStatementNumber()).pair("Etat", request.status()));
        sections.add(new VoucherService.Section("Personnes").pair("Demandeur", request.requester()).pair("Service", request.requesterDepartment())
                .pair("Beneficiaire", request.beneficiaryName()).pair("Piece d'identite", request.beneficiaryIdNumber())
                .pair("Telephone", request.beneficiaryPhone()).pair("Caisse de remise", request.registerCode())
                .pair("Mode de remise", request.deliveryMode()).pair("Date souhaitee", request.wantedDate())
                .pair("Mission", request.missionDestination() == null ? null : request.missionDestination() + " du " + request.missionStart()
                        + " au " + request.missionEnd())
                .pair("Depense non prevue", Boolean.TRUE.equals(request.unplanned()) ? request.unplannedReason() : null));
        return sections;
    }

    private VoucherService.Section lines(FundDtos.Request request) {
        VoucherService.Section section = new VoucherService.Section("Lignes").columns(List.of("Nature", "Designation", "Fournisseur", "Section", "Montant"),
                List.of(false, false, false, false, true));
        request.lines().forEach(line -> section.row(line.natureCode(), line.designation(), line.supplier(), line.section(), VoucherService.money(line.amount())));
        return section;
    }

    private VoucherService.Section timeline(FundDtos.Request request) {
        VoucherService.Section section = new VoucherService.Section("Circuit de validation").columns(List.of("Niveau", "Nom", "Decision", "Montant", "Date", "Commentaire"),
                List.of(false, false, false, true, false, false));
        request.timeline().forEach(step -> section.row(step.level() == null || step.level() == 0 ? "Soumission" : step.level(), step.approver()
                        + (step.delegatedFrom() == null ? "" : " (pour " + step.delegatedFrom() + ")"), step.decision(),
                VoucherService.money(step.amountAfter() != null ? step.amountAfter() : step.amountBefore()), VoucherService.date(step.decidedAt()), step.comment()));
        return section;
    }

    private static String urgency(String value) {
        return switch (value) {
            case "URGENT" -> "Urgente";
            case "CRITICAL" -> "Critique";
            default -> "Normale";
        };
    }

    private String actor() {
        return actors.resolve("SYSTEM");
    }
}
