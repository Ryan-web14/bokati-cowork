package com.sni.bokaticowork.features.fund.voucher;

import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;

/** Pieces PDF des demandes de fonds (lot F7) et verification publique d'une piece par son QR. */
@RestController
@RequiredArgsConstructor
public class VoucherController {

    private final FundVoucherService fundVouchers;
    private final VoucherService vouchers;

    @GetMapping(ApiPath.V1 + "/fund-needs/{number}/pdf")
    public ResponseEntity<byte[]> need(@PathVariable String number) {
        return pdf(number, fundVouchers.need(number));
    }

    @GetMapping(ApiPath.V1 + "/fund-requests/{number}/pdf")
    public ResponseEntity<byte[]> request(@PathVariable String number) {
        return pdf(number, fundVouchers.request(number));
    }

    @GetMapping(ApiPath.V1 + "/fund-requests/{number}/disbursements/{movementNumber}/pdf")
    public ResponseEntity<byte[]> disbursement(@PathVariable String number, @PathVariable String movementNumber) {
        return pdf(movementNumber, fundVouchers.disbursement(number, movementNumber));
    }

    @GetMapping(ApiPath.V1 + "/fund-requests/{number}/justification/pdf")
    public ResponseEntity<byte[]> justification(@PathVariable String number) {
        return pdf(number + "-justification", fundVouchers.justification(number));
    }

    /** Ce que montre le QR d'une piece : numero, montant, date d'emission, etat. */
    public record Verification(String number, String type, BigDecimal amount, Instant issuedAt, int printCount, String status) {
    }

    @GetMapping("/verify/voucher/{number}")
    public ResponseEntity<Verification> verify(@PathVariable String number) {
        VoucherService.Issued issued = vouchers.find(number.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Piece inconnue : " + number));
        return ResponseEntity.ok(new Verification(issued.number(), issued.type(), issued.amount(), issued.issuedAt(), issued.printCount(),
                issued.cancelledBy() == null ? "VALIDE" : "ANNULEE PAR " + issued.cancelledBy()));
    }

    private static ResponseEntity<byte[]> pdf(String name, byte[] content) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + ".pdf\"").body(content);
    }
}
