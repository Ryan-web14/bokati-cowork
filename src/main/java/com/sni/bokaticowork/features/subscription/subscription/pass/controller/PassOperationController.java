package com.sni.bokaticowork.features.subscription.subscription.pass.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassUsageType;
import com.sni.bokaticowork.features.subscription.subscription.pass.enums.PassValidationChannel;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassTransfer;
import com.sni.bokaticowork.features.subscription.subscription.pass.model.PassUsage;
import com.sni.bokaticowork.features.subscription.subscription.pass.service.PassTransferService;
import com.sni.bokaticowork.features.subscription.subscription.pass.service.PassUsageService;
import com.sni.bokaticowork.features.subscription.subscription.pass.service.PassValidationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Le parcours d'usage d'un pass.
 *
 * <p>Validation, consommation, contre-passation et transfert. La validation est volontairement
 * exposee a part de la consommation : une borne peut vouloir afficher « ce pass vous ouvre
 * l'acces » avant que quiconque n'ait franchi la porte.</p>
 */
@RestController
@RequestMapping(ApiPath.V1 + "/passes")
@RequiredArgsConstructor
public class PassOperationController {

    private final PassValidationService validationService;
    private final PassUsageService usageService;
    private final PassTransferService transferService;

    public record ValidateRequest(
            String passNumber,
            String credentialValue,
            String bearerType,
            String bearerCode,
            String locationCode,
            String resourceTypeCode
    ) {
    }

    public record UsePassRequest(
            String passNumber,
            String credentialValue,
            String bearerType,
            String bearerCode,
            PassUsageType usageType,
            String locationCode,
            String resourceCode,
            String resourceTypeCode,
            PassValidationChannel channel,
            String validatedBy,
            String referenceType,
            String referenceCode
    ) {
    }

    public record TransferRequest(
            @NotNull SubscriberType toOwnerType,
            @NotBlank String toOwnerCode,
            String reason,
            BigDecimal transferFee
    ) {
    }

    /** Repond oui ou non, sans rien consommer. */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestBody ValidateRequest request) {
        PassValidationService.ValidationResult result = validationService.validate(
                new PassValidationService.ValidationRequest(
                        request.passNumber(), request.credentialValue(), request.bearerType(),
                        request.bearerCode(), request.locationCode(), request.resourceTypeCode(), null));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("valid", result.valid());
        body.put("reason", result.reason());
        body.put("passNumber", result.pass() == null ? null : result.pass().getPassNumber());
        body.put("remainingUses", result.remainingUses());
        return ResponseEntity.ok(body);
    }

    /** Valide puis consomme. Un refus rend son motif, en francais. */
    @PostMapping("/use")
    public ResponseEntity<Map<String, Object>> use(@RequestBody UsePassRequest request) {
        PassUsage usage = usageService.use(new PassUsageService.UseRequest(
                request.passNumber(), request.credentialValue(), request.bearerType(), request.bearerCode(),
                request.usageType(), request.locationCode(), request.resourceCode(), request.resourceTypeCode(),
                request.channel(), request.validatedBy(), request.referenceType(), request.referenceCode(), null));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("usageNumber", usage.getUsageNumber());
        body.put("passNumber", usage.getPass().getPassNumber());
        body.put("remainingUses", remaining(usage));
        return ResponseEntity.ok(body);
    }

    @PostMapping("/usages/{usageNumber}/reverse")
    public ResponseEntity<Map<String, String>> reverse(@PathVariable String usageNumber,
                                                       @RequestParam String reason,
                                                       @RequestParam(required = false) String reversedBy) {
        PassUsage usage = usageService.reverse(usageNumber, reason, reversedBy);
        return ResponseEntity.ok(Map.of("usageNumber", usage.getUsageNumber(), "status", "REVERSED"));
    }

    // -----------------------------------------------------------------------------------------
    // Transfert
    // -----------------------------------------------------------------------------------------

    @PostMapping("/{passNumber}/transfers")
    public ResponseEntity<Map<String, String>> requestTransfer(@PathVariable String passNumber,
                                                               @Valid @RequestBody TransferRequest request,
                                                               @RequestParam(required = false) String requestedBy) {
        PassTransfer transfer = transferService.request(passNumber, request.toOwnerType(),
                request.toOwnerCode(), request.reason(), request.transferFee(), requestedBy);
        return ResponseEntity.ok(Map.of(
                "transferNumber", transfer.getTransferNumber(),
                "status", transfer.getStatus().name()));
    }

    /** C'est ici, et seulement ici, que le pass change de main. */
    @PostMapping("/transfers/{transferNumber}/accept")
    public ResponseEntity<Map<String, String>> accept(@PathVariable String transferNumber,
                                                      @RequestParam(required = false) String acceptedBy) {
        PassTransfer transfer = transferService.accept(transferNumber, acceptedBy);
        return ResponseEntity.ok(Map.of(
                "transferNumber", transfer.getTransferNumber(),
                "status", transfer.getStatus().name()));
    }

    @PostMapping("/transfers/{transferNumber}/reject")
    public ResponseEntity<Map<String, String>> reject(@PathVariable String transferNumber,
                                                      @RequestParam(required = false) String reason) {
        PassTransfer transfer = transferService.reject(transferNumber, reason);
        return ResponseEntity.ok(Map.of("status", transfer.getStatus().name()));
    }

    @DeleteMapping("/transfers/{transferNumber}")
    public ResponseEntity<Map<String, String>> cancel(@PathVariable String transferNumber,
                                                      @RequestParam(required = false) String reason) {
        PassTransfer transfer = transferService.cancel(transferNumber, reason);
        return ResponseEntity.ok(Map.of("status", transfer.getStatus().name()));
    }

    /** Solde restant, ou {@code -1} pour un pass sans plafond d'utilisations. */
    private int remaining(PassUsage usage) {
        Integer maxUses = usage.getPass().getMaxUses();
        if (maxUses == null) {
            return -1;
        }
        Integer used = usage.getPass().getUsedCount();
        return Math.max(0, maxUses - (used == null ? 0 : used));
    }
}
