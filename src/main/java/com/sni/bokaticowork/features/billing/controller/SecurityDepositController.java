package com.sni.bokaticowork.features.billing.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.billing.dto.SecurityDepositDtos;
import com.sni.bokaticowork.features.billing.service.implementation.SecurityDepositService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * Lot C7 : depots de garantie (cautions). Registre alimente a l'emission des factures qui portent
 * une caution ; restitution, retenue motivee ou imputation sur une facture du client.
 */
@RestController
@RequestMapping(ApiPath.V1 + "/billing")
@RequiredArgsConstructor
public class SecurityDepositController {

    private final SecurityDepositService deposits;

    /** Cautions d'un client, ou toutes les cautions ouvertes ({@code open=true}, par defaut). */
    @GetMapping("/security-deposits")
    public ResponseEntity<List<SecurityDepositDtos.DepositView>> list(@RequestParam(required = false) String customerType,
                                                                      @RequestParam(required = false) String customerCode,
                                                                      @RequestParam(defaultValue = "true") boolean open) {
        return ResponseEntity.ok(deposits.list(customerType, customerCode, open));
    }

    /** Decision CD10 : cautions facturees avant le lot C7 (en produit, avec TVA), a reclasser par le comptable. */
    @GetMapping("/security-deposits/legacy")
    public ResponseEntity<List<SecurityDepositDtos.LegacyLine>> legacy() {
        return ResponseEntity.ok(deposits.legacy());
    }

    @GetMapping("/security-deposits/{depositNumber}")
    public ResponseEntity<SecurityDepositDtos.DepositView> get(@PathVariable String depositNumber) {
        return ResponseEntity.ok(deposits.get(depositNumber));
    }

    /** Restitution, retenue (motif requis) ou imputation sur une facture ouverte du client. */
    @PostMapping("/security-deposits/{depositNumber}/movements")
    public ResponseEntity<SecurityDepositDtos.DepositView> move(@PathVariable String depositNumber,
                                                                @Valid @RequestBody SecurityDepositDtos.MovementRequest request) {
        return ResponseEntity.ok(deposits.move(depositNumber, request));
    }

    /** Marque (ou non) un article du catalogue comme caution. */
    @PutMapping("/catalog/{itemCode}/security-deposit")
    public ResponseEntity<Map<String, Object>> flag(@PathVariable String itemCode, @Valid @RequestBody SecurityDepositDtos.FlagRequest request) {
        deposits.flagCatalogItem(itemCode, request.securityDeposit());
        return ResponseEntity.ok(Map.of("itemCode", itemCode, "securityDeposit", request.securityDeposit()));
    }
}
