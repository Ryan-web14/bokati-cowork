package com.sni.bokaticowork.features.document.template.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.PieceFields;
import com.sni.bokaticowork.features.document.template.dto.DocumentTemplateDtos.PieceFieldsRequest;
import com.sni.bokaticowork.features.document.template.service.DocumentDataSources;
import com.sni.bokaticowork.features.document.template.service.DocumentFieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * Champs ajoutes d'une piece (lot D3) : lecture et saisie, sur la route de la piece elle-meme.
 * Les droits sont donc ceux de la piece : saisie d'un mouvement de caisse pour un bon de caisse
 * ({@code CASH_MOVEMENT:CREATE}), demande de fonds pour les pieces de fonds ({@code FUND_REQUEST:CREATE}).
 */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class DocumentFieldController {

    private static final Set<String> REQUEST_TEMPLATES = Set.of("FUND_REQUEST", "JUSTIFICATION_SHEET");

    private final DocumentFieldService fields;
    private final DocumentDataSources pieces;

    @GetMapping("/cash-registers/movements/{number}/fields")
    public ResponseEntity<PieceFields> cash(@PathVariable String number) {
        return ResponseEntity.ok(fields.get(pieces.cashTemplate(number), number));
    }

    @PutMapping("/cash-registers/movements/{number}/fields")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "CAPTURE_FIELDS", ressource = "document_field_value")
    public ResponseEntity<PieceFields> saveCash(@PathVariable String number, @RequestBody PieceFieldsRequest request) {
        return ResponseEntity.ok(fields.save(pieces.cashTemplate(number), number, request.values()));
    }

    @GetMapping("/fund-needs/{number}/fields")
    public ResponseEntity<PieceFields> need(@PathVariable String number) {
        pieces.requireNeed(number);
        return ResponseEntity.ok(fields.get("NEED_STATEMENT", number));
    }

    @PutMapping("/fund-needs/{number}/fields")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "CAPTURE_FIELDS", ressource = "document_field_value")
    public ResponseEntity<PieceFields> saveNeed(@PathVariable String number, @RequestBody PieceFieldsRequest request) {
        pieces.requireNeed(number);
        return ResponseEntity.ok(fields.save("NEED_STATEMENT", number, request.values()));
    }

    /** {@code template} : FUND_REQUEST (par defaut) ou JUSTIFICATION_SHEET pour la fiche de justification. */
    @GetMapping("/fund-requests/{number}/fields")
    public ResponseEntity<PieceFields> request(@PathVariable String number, @RequestParam(defaultValue = "FUND_REQUEST") String template) {
        pieces.requireRequest(number);
        return ResponseEntity.ok(fields.get(requestTemplate(template), number));
    }

    @PutMapping("/fund-requests/{number}/fields")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "CAPTURE_FIELDS", ressource = "document_field_value")
    public ResponseEntity<PieceFields> saveRequest(@PathVariable String number, @RequestParam(defaultValue = "FUND_REQUEST") String template,
                                                   @RequestBody PieceFieldsRequest request) {
        pieces.requireRequest(number);
        return ResponseEntity.ok(fields.save(requestTemplate(template), number, request.values()));
    }

    /** Bon de decaissement : les valeurs sont rattachees au mouvement de caisse du decaissement. */
    @GetMapping("/fund-requests/{number}/disbursements/{movement}/fields")
    public ResponseEntity<PieceFields> disbursement(@PathVariable String number, @PathVariable String movement) {
        pieces.requireRequest(number);
        return ResponseEntity.ok(fields.get("DISBURSEMENT_VOUCHER", movement));
    }

    @PutMapping("/fund-requests/{number}/disbursements/{movement}/fields")
    @Audited(module = "DOCUMENT_TEMPLATE", action = "CAPTURE_FIELDS", ressource = "document_field_value")
    public ResponseEntity<PieceFields> saveDisbursement(@PathVariable String number, @PathVariable String movement,
                                                        @RequestBody PieceFieldsRequest request) {
        pieces.requireRequest(number);
        return ResponseEntity.ok(fields.save("DISBURSEMENT_VOUCHER", movement, request.values()));
    }

    private static String requestTemplate(String template) {
        String code = template == null ? "FUND_REQUEST" : template.trim().toUpperCase();
        if (!REQUEST_TEMPLATES.contains(code)) {
            throw BadRequestException.coded("AUCUN_CHAMP", "Modèle sans champs sur une demande de fonds : " + template);
        }
        return code;
    }
}
