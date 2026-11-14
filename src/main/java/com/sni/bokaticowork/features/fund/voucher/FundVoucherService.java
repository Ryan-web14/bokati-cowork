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

import com.sni.bokaticowork.features.document.template.render.DocumentData;

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

    private static final String[] TYPE_LABELS = {"Dépense directe", "Avance à justifier", "Remboursement de frais", "Paiement fournisseur",
            "Acompte au personnel", "Approvisionnement de caisse", "Remise de fonds"};

    private final FundNeedService needs;
    private final FundRequestService requests;
    private final VoucherService vouchers;
    private final CashActorResolver actors;

    private Built buildNeed(String number, boolean preview) {
        FundDtos.Need need = needs.get(number);
        VoucherService.Issued issued = issue(preview, "EDB", "FUND_NEED", need.number(), need.totalAmount(), need.subject());
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Demandeur").pair("Demandeur", need.requester()).pair("Service", need.department())
                .pair("Site", need.locationCode()).pair("Centre de coût", need.costCenter()).pair("État de besoin", need.number())
                .pair("Date", VoucherService.date(need.createdAt())));
        sections.add(new VoucherService.Section("Objet").pair("Objet", need.subject()).pair("Urgence", urgency(need.urgency().name()))
                .pair("Date souhaitée", need.wantedDate()).pair("État", label(need.status())));
        VoucherService.Section lines = new VoucherService.Section("Besoins").columns(
                List.of("N°", "Désignation", "Quantité", "Unité", "Prix estimé", "Total", "Nature", "Fournisseur suggéré"),
                List.of(false, false, true, false, true, true, false, false));
        need.lines().forEach(line -> lines.row(line.lineNo(), line.designation(), line.quantity().stripTrailingZeros().toPlainString(), line.unit(),
                VoucherService.money(line.unitPrice()), VoucherService.money(line.totalAmount()), line.natureCode(), line.supplier()));
        sections.add(lines);
        if (need.decidedBy() != null) {
            sections.add(new VoucherService.Section("Décision").pair("Décidé par", need.decidedBy()).pair("Le", VoucherService.date(need.decidedAt()))
                    .pair("Commentaire", need.decisionNote()));
        }
        List<String> follow = need.lines().stream().map(FundDtos.NeedLine::transformedRequestNumber).filter(java.util.Objects::nonNull).distinct().toList();
        if (!follow.isEmpty()) {
            sections.add(new VoucherService.Section("Suite donnée").pair("Demandes de fonds", String.join(", ", follow)));
        }
        return new Built("État de besoin", issued, sections, need.totalAmount(), null, List.of(
                new VoucherService.Signature("Demandeur", null, null),
                new VoucherService.Signature("Chef de service", need.decidedBy(), need.decidedBy() == null ? null : VoucherService.date(need.decidedAt())),
                new VoucherService.Signature("Responsable financier", null, null)));
    }

    private Built buildRequest(String number, boolean preview) {
        FundDtos.Request request = requests.get(number);
        VoucherService.Issued issued = issue(preview, "DDF", "FUND_REQUEST", request.number(), request.totalAmount(), request.subject());
        List<VoucherService.Section> sections = header(request);
        sections.add(lines(request));
        if (request.insight() != null) {
            sections.add(new VoucherService.Section("Au jour de la demande").pair("Niveaux requis", String.join(", ", request.insight().levelLabels()))
                    .pair("Avances non soldées du bénéficiaire", request.insight().openAdvances().isEmpty() ? "Aucune"
                            : String.join(" ; ", request.insight().openAdvances())));
        }
        sections.add(timeline(request));
        List<VoucherService.Signature> signatures = new ArrayList<>();
        signatures.add(new VoucherService.Signature("Demandeur", request.requester(), VoucherService.date(request.submittedAt())));
        request.timeline().stream().filter(step -> step.level() != null && step.level() > 0).forEach(step -> signatures.add(
                new VoucherService.Signature("Niveau " + step.level() + " : " + label(step.decision()), step.approver(), VoucherService.date(step.decidedAt()))));
        return new Built("Demande de fonds", issued, sections, request.totalAmount(), null, signatures);
    }

    private Built buildDisbursement(String number, String movementNumber, boolean preview) {
        FundDtos.Request request = requests.get(number);
        FundDtos.Disbursement item = request.disbursements().stream().filter(candidate -> candidate.movementNumber().equals(movementNumber))
                .filter(candidate -> candidate.kind() != FundDisbursementKind.RETURN)
                .findFirst().orElseThrow(() -> new BadRequestException("Aucun decaissement " + movementNumber + " sur la demande " + number));
        VoucherService.Issued issued = issue(preview, "BDF", "FUND_DISBURSEMENT", movementNumber, item.amount(), request.subject());
        List<VoucherService.Section> sections = header(request);
        sections.add(lines(request));
        BigDecimal approved = request.approvedAmount() == null ? request.totalAmount() : request.approvedAmount();
        sections.add(new VoucherService.Section("Décaissement").pair("Mouvement de caisse", movementNumber).pair("Session", item.sessionNumber())
                .pair("Caissière", item.createdBy()).pair("Date", VoucherService.date(item.createdAt()))
                .pair("Montant approuvé", VoucherService.money(approved) + " XAF").pair("Montant décaissé", VoucherService.money(item.amount()) + " XAF")
                .pair("Déjà décaissé (total)", VoucherService.money(request.disbursedAmount()) + " XAF")
                .pair("Reste à décaisser", VoucherService.money(request.remainingToDisburse()) + " XAF")
                .pair("Imputation", request.type() == FundRequestType.ADVANCE || request.type() == FundRequestType.STAFF_ADVANCE
                        ? "D 421 " + request.beneficiaryName() + " / C caisse" : "D charges des natures / C caisse"));
        sections.add(timeline(request));
        String commitment = request.type() == FundRequestType.ADVANCE && request.justificationDueDate() != null
                ? "Je soussigné(e) " + request.beneficiaryName() + " reconnais avoir reçu la somme ci-dessus et m'engage à la justifier avant le "
                + request.justificationDueDate() + " ou à la restituer."
                : "Je soussigné(e) " + request.beneficiaryName() + " reconnais avoir reçu la somme ci-dessus.";
        return new Built(item.kind() == FundDisbursementKind.COMPLEMENT ? "Bon de décaissement (complément)" : "Bon de décaissement de fonds",
                issued, sections, item.amount(), commitment, List.of(
                        new VoucherService.Signature("Bénéficiaire" + (item.signedByName() == null ? "" : " : " + item.signedByName()), null, null),
                        new VoucherService.Signature("Caissière", item.createdBy(), VoucherService.date(item.createdAt()))));
    }

    private Built buildJustification(String number, boolean preview) {
        FundDtos.Request request = requests.get(number);
        if (request.type() != FundRequestType.ADVANCE) {
            throw new BadRequestException("La demande " + number + " n'est pas une avance");
        }
        BigDecimal validated = request.justification().stream().filter(line -> line.status().name().equals("VALIDATED"))
                .map(FundDtos.ExpenseLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rejected = request.justification().stream().filter(line -> line.status().name().equals("REJECTED"))
                .map(FundDtos.ExpenseLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        VoucherService.Issued issued = issue(preview, "JUS", "FUND_JUSTIFICATION", request.number(), validated, request.subject());
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Avance").pair("Demande", request.number()).pair("Objet", request.subject())
                .pair("Bénéficiaire", request.beneficiaryName()).pair("Mission", request.missionDestination())
                .pair("Du", request.missionStart()).pair("Au", request.missionEnd()).pair("Échéance", request.justificationDueDate())
                .pair("Bons de décaissement", String.join(", ", request.disbursements().stream()
                        .filter(item -> item.kind() != FundDisbursementKind.RETURN).map(FundDtos.Disbursement::movementNumber).toList())));
        VoucherService.Section table = new VoucherService.Section("Dépenses").columns(
                List.of("Date", "Fournisseur", "Nature", "Désignation", "HT", "TVA", "TTC", "Pièce", "Statut"),
                List.of(false, false, false, false, true, true, true, false, false));
        request.justification().forEach(line -> table.row(line.expenseDate(), line.supplier(), line.natureCode(), line.designation(),
                VoucherService.money(line.amount().subtract(line.vatAmount())), VoucherService.money(line.vatAmount()), VoucherService.money(line.amount()),
                line.documentReference() == null ? (Boolean.TRUE.equals(line.honourStatement()) ? "Attestation" : "") : line.documentReference(),
                line.status().name().equals("VALIDATED") ? "Validée" : line.status().name().equals("REJECTED") ? "Rejetée : " + line.rejectReason() : "À valider"));
        sections.add(table);
        BigDecimal given = request.disbursedAmount().add(request.complementAmount());
        BigDecimal balance = given.subtract(validated).subtract(request.returnedAmount());
        sections.add(new VoucherService.Section("Solde").pair("Avance reçue", VoucherService.money(request.disbursedAmount()) + " XAF")
                .pair("Complément versé", VoucherService.money(request.complementAmount()) + " XAF")
                .pair("Total justifié", VoucherService.money(validated) + " XAF").pair("Total rejeté", VoucherService.money(rejected) + " XAF")
                .pair("Reliquat rendu", VoucherService.money(request.returnedAmount()) + " XAF")
                .pair(balance.signum() >= 0 ? "Reste à rendre" : "Complément dû", VoucherService.money(balance.abs()) + " XAF")
                .pair("État", label(request.status())));
        String validator = request.justification().stream().map(FundDtos.ExpenseLine::validatedBy).filter(java.util.Objects::nonNull).findFirst().orElse(null);
        return new Built("Fiche de justification d'avance", issued, sections, validated, null, List.of(
                new VoucherService.Signature("Bénéficiaire", null, null),
                new VoucherService.Signature("Comptable valideur", validator, null)));
    }

    /** Piece construite, prete a imprimer ou a montrer en apercu. */
    private record Built(String title, VoucherService.Issued issued, List<VoucherService.Section> sections, BigDecimal amount, String commitment,
                         List<VoucherService.Signature> signatures) {
    }

    public byte[] need(String number) {
        return render(buildNeed(number, false));
    }

    public byte[] request(String number) {
        return render(buildRequest(number, false));
    }

    /** Bon de decaissement d'un mouvement de la demande (decaissement ou complement). */
    public byte[] disbursement(String number, String movementNumber) {
        return render(buildDisbursement(number, movementNumber, false));
    }

    /** Fiche de justification d'une avance. */
    public byte[] justification(String number) {
        return render(buildJustification(number, false));
    }

    /**
     * Donnees d'une piece reelle pour l'apercu d'un modele, sans numero attribue ni impression
     * tracee. Pour un bon de decaissement, {@code number} vaut {@code DEMANDE/MOUVEMENT}.
     */
    @Transactional(readOnly = true)
    public DocumentData previewData(String templateCode, String number) {
        Built built = switch (templateCode) {
            case "NEED_STATEMENT" -> buildNeed(number, true);
            case "FUND_REQUEST" -> buildRequest(number, true);
            case "JUSTIFICATION_SHEET" -> buildJustification(number, true);
            case "DISBURSEMENT_VOUCHER" -> {
                String[] parts = number.split("/", 2);
                if (parts.length < 2) {
                    throw new BadRequestException("Numéro attendu : DEMANDE/MOUVEMENT");
                }
                yield buildDisbursement(parts[0], parts[1], true);
            }
            default -> throw new BadRequestException("Modèle sans pièce de fonds : " + templateCode);
        };
        return DocumentData.fromVoucher(templateCode, vouchers.view(built.title(), built.issued(), built.sections(), built.amount(),
                built.commitment(), built.signatures()), built.amount()).source(built.issued().sourceCode());
    }

    private byte[] render(Built built) {
        return vouchers.render(built.title(), built.issued(), built.sections(), built.amount(), built.commitment(), built.signatures());
    }

    private VoucherService.Issued issue(boolean preview, String type, String sourceType, String sourceCode, BigDecimal amount, String label) {
        return preview ? vouchers.peek(type, sourceCode, amount, label, actor()) : vouchers.issueAndPrint(type, sourceType, sourceCode, amount, label, actor());
    }

    private List<VoucherService.Section> header(FundDtos.Request request) {
        List<VoucherService.Section> sections = new ArrayList<>();
        sections.add(new VoucherService.Section("Demande").pair("Numéro", request.number()).pair("Type", TYPE_LABELS[request.type().ordinal()])
                .pair("Objet", request.subject()).pair("Date", VoucherService.date(request.createdAt()))
                .pair("État de besoin", request.needStatementNumber()).pair("État", label(request.status())));
        sections.add(new VoucherService.Section("Personnes").pair("Demandeur", request.requester()).pair("Service", request.requesterDepartment())
                .pair("Bénéficiaire", request.beneficiaryName()).pair("Pièce d'identité", request.beneficiaryIdNumber())
                .pair("Téléphone", request.beneficiaryPhone()).pair("Caisse de remise", request.registerCode())
                .pair("Mode de remise", request.deliveryMode()).pair("Date souhaitée", request.wantedDate())
                .pair("Mission", request.missionDestination() == null ? null : request.missionDestination() + " du " + request.missionStart()
                        + " au " + request.missionEnd())
                .pair("Dépense non prévue", Boolean.TRUE.equals(request.unplanned()) ? request.unplannedReason() : null));
        return sections;
    }

    private VoucherService.Section lines(FundDtos.Request request) {
        VoucherService.Section section = new VoucherService.Section("Lignes").columns(List.of("Nature", "Désignation", "Fournisseur", "Section", "Montant"),
                List.of(false, false, false, false, true));
        request.lines().forEach(line -> section.row(line.natureCode(), line.designation(), line.supplier(), line.section(), VoucherService.money(line.amount())));
        return section;
    }

    private VoucherService.Section timeline(FundDtos.Request request) {
        VoucherService.Section section = new VoucherService.Section("Circuit de validation").columns(List.of("Niveau", "Nom", "Décision", "Montant", "Date", "Commentaire"),
                List.of(false, false, false, true, false, false));
        request.timeline().forEach(step -> section.row(step.level() == null || step.level() == 0 ? "Soumission" : step.level(), step.approver()
                        + (step.delegatedFrom() == null ? "" : " (pour " + step.delegatedFrom() + ")"), label(step.decision()),
                VoucherService.money(step.amountAfter() != null ? step.amountAfter() : step.amountBefore()), VoucherService.date(step.decidedAt()), step.comment()));
        return section;
    }

    /** Libelle francais d'un etat ou d'une decision ; une valeur inconnue s'imprime telle quelle. */
    static String label(Enum<?> value) {
        if (value == null) {
            return null;
        }
        return switch (value.name()) {
            case "DRAFT" -> "Brouillon";
            case "SUBMITTED" -> "Soumis";
            case "VALIDATED" -> "Validé";
            case "IN_APPROVAL" -> "En validation";
            case "APPROVED" -> "Approuvé";
            case "REJECTED" -> "Rejeté";
            case "RETURNED" -> "Renvoyé pour correction";
            case "REDUCED" -> "Approuvé avec réduction";
            case "CANCELLED" -> "Annulé";
            case "EXPIRED" -> "Expiré";
            case "PARTIALLY_TRANSFORMED" -> "Transformé en partie";
            case "TRANSFORMED" -> "Transformé";
            case "PARTIALLY_DISBURSED" -> "Décaissé en partie";
            case "DISBURSED" -> "Décaissé";
            case "TO_JUSTIFY" -> "À justifier";
            case "PARTIALLY_JUSTIFIED" -> "Justifié en partie";
            case "JUSTIFIED" -> "Justifié";
            case "SETTLED" -> "Soldé";
            default -> value.name();
        };
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
