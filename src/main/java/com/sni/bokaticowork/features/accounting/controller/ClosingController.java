package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.AccConfirmationRequest;
import com.sni.bokaticowork.features.accounting.service.implementation.BalanceJustificationService;
import com.sni.bokaticowork.features.accounting.service.implementation.ClosingService;
import com.sni.bokaticowork.features.accounting.service.implementation.FinancialStatementService;
import com.sni.bokaticowork.features.accounting.service.implementation.FixedAssetReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Etats financiers et cloture d'exercice (lot F12). Les droits sont controles par les services. */
@RestController
@RequestMapping(ApiPath.V1)
@RequiredArgsConstructor
public class ClosingController {

    private final FinancialStatementService statements;
    private final ClosingService closing;
    private final FixedAssetReportService exporter;
    private final BalanceJustificationService justifications;

    public record PrepareRequest(String thirdPartyCode, LocalDate date, String comment) {
    }

    public record ReconcileRequest(String comment) {
    }

    public record TickRequest(Boolean done, String comment) {
    }

    public record WaiveRequest(String reason) {
    }

    // ------------------------------------------------------------------ etats

    @GetMapping("/accounting/statements/balance-sheet")
    public ResponseEntity<?> balanceSheet(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                          @RequestParam(required = false, defaultValue = "json") String format) {
        return render(statements.balanceSheet(date), format);
    }

    @GetMapping("/accounting/statements/income-statement")
    public ResponseEntity<?> incomeStatement(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                             @RequestParam(required = false, defaultValue = "json") String format) {
        return render(statements.incomeStatement(from, to), format);
    }

    @GetMapping("/accounting/statements/cash-flow")
    public ResponseEntity<?> cashFlow(@RequestParam(required = false) String year,
                                      @RequestParam(required = false, defaultValue = "json") String format) {
        return render(statements.cashFlow(year), format);
    }

    @GetMapping("/accounting/statements/{template}/mappings")
    public ResponseEntity<List<FinancialStatementService.MappingView>> mappings(@PathVariable String template) {
        return ResponseEntity.ok(statements.mappings(template.toUpperCase(Locale.ROOT)));
    }

    @Audited(module = "ACCOUNTING", action = "STATEMENT_MAPPING_EDIT", ressource = "acc_statement_mapping")
    @PostMapping("/accounting/statements/{template}/mappings")
    public ResponseEntity<FinancialStatementService.MappingView> addMapping(@PathVariable String template,
                                                                           @RequestBody FinancialStatementService.MappingRequest request) {
        return ResponseEntity.ok(statements.addMapping(template.toUpperCase(Locale.ROOT), request));
    }

    @Audited(module = "ACCOUNTING", action = "STATEMENT_MAPPING_DELETE", ressource = "acc_statement_mapping")
    @DeleteMapping("/accounting/statements/mappings/{id}")
    public ResponseEntity<Void> deleteMapping(@PathVariable Long id) {
        statements.deleteMapping(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ cloture

    @GetMapping("/accounting/closing/{year}")
    public ResponseEntity<ClosingService.Checklist> checklist(@PathVariable String year) {
        return ResponseEntity.ok(closing.checklist(year));
    }

    @Audited(module = "ACCOUNTING", action = "CLOSING_STEP", ressource = "acc_closing_step")
    @PostMapping("/accounting/closing/{year}/steps/{step}")
    public ResponseEntity<ClosingService.Checklist> tick(@PathVariable String year, @PathVariable String step, @RequestBody(required = false) TickRequest request) {
        boolean done = request == null || request.done() == null || request.done();
        return ResponseEntity.ok(closing.tick(year, step, done, request == null ? null : request.comment()));
    }

    @Audited(module = "ACCOUNTING", action = "CLOSING_WAIVE", ressource = "acc_closing_step")
    @PostMapping("/accounting/closing/{year}/steps/{step}/waive")
    public ResponseEntity<ClosingService.Checklist> waive(@PathVariable String year, @PathVariable String step, @RequestBody WaiveRequest request) {
        return ResponseEntity.ok(closing.waive(year, step, request == null ? null : request.reason()));
    }

    @GetMapping("/accounting/closing/{year}/opening-preview")
    public ResponseEntity<ClosingService.OpeningPreview> openingPreview(@PathVariable String year) {
        return ResponseEntity.ok(closing.previewOpening(year));
    }

    @Audited(module = "ACCOUNTING", action = "FISCAL_YEAR_CLOSE", ressource = "acc_fiscal_year")
    @PostMapping("/accounting/closing/{year}/close")
    public ResponseEntity<ClosingService.CloseResult> close(@PathVariable String year, @RequestBody(required = false) ClosingService.CloseRequest request) {
        return ResponseEntity.ok(closing.close(year, request));
    }

    // ------------------------------------------------------------------ editions definitives

    @Audited(module = "ACCOUNTING", action = "STATEMENT_FINALIZE", ressource = "acc_generated_report")
    @PostMapping("/accounting/closing/{year}/editions")
    public ResponseEntity<ClosingService.Edition> finalizeEdition(@PathVariable String year, @RequestBody ClosingService.EditionRequest request) {
        return ResponseEntity.ok(closing.finalize(year, request));
    }

    @GetMapping("/accounting/closing/{year}/editions")
    public ResponseEntity<List<ClosingService.Edition>> editions(@PathVariable String year) {
        return ResponseEntity.ok(closing.editions(year));
    }

    @GetMapping("/accounting/closing/editions/{number}/pdf")
    public ResponseEntity<byte[]> editionPdf(@PathVariable String number) {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + number + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(closing.editionPdf(number));
    }

    @GetMapping("/accounting/closing/editions/{number}/verify")
    public ResponseEntity<Map<String, Object>> verify(@PathVariable String number) {
        return ResponseEntity.ok(Map.of("number", number, "intact", closing.verify(number)));
    }

    // ------------------------------------------------------------------ notes annexes

    @GetMapping("/accounting/closing/{year}/notes")
    public ResponseEntity<List<ClosingService.NoteView>> notes(@PathVariable String year) {
        return ResponseEntity.ok(closing.notes(year));
    }

    @Audited(module = "ACCOUNTING", action = "NOTE_EDIT", ressource = "acc_note_text")
    @PutMapping("/accounting/closing/{year}/notes/{note}")
    public ResponseEntity<ClosingService.NoteView> saveNote(@PathVariable String year, @PathVariable String note,
                                                            @RequestBody ClosingService.NoteRequest request) {
        return ResponseEntity.ok(closing.saveNote(year, note, request));
    }

    @Audited(module = "ACCOUNTING", action = "NOTE_VALIDATE", ressource = "acc_note_text")
    @PostMapping("/accounting/closing/{year}/notes/{note}/validate")
    public ResponseEntity<ClosingService.NoteView> validateNote(@PathVariable String year, @PathVariable String note,
                                                                @RequestParam(required = false, defaultValue = "true") boolean validate) {
        return ResponseEntity.ok(closing.validateNote(year, note, validate));
    }

    // ------------------------------------------------------------------ justification des soldes

    @GetMapping("/accounting/accounts/{number}/balance-justification")
    public ResponseEntity<BalanceJustificationService.Justification> justification(@PathVariable String number,
                                                                                  @RequestParam(required = false) String thirdParty,
                                                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(justifications.justification(number, thirdParty, date));
    }

    @Audited(module = "ACCOUNTING", action = "BALANCE_JUSTIFICATION_PREPARE", ressource = "balance_justification")
    @PostMapping("/accounting/accounts/{number}/balance-justification")
    public ResponseEntity<BalanceJustificationService.SavedJustification> prepareJustification(@PathVariable String number,
                                                                                              @RequestBody(required = false) PrepareRequest request) {
        return ResponseEntity.ok(justifications.prepare(number, request == null ? null : request.thirdPartyCode(),
                request == null ? null : request.date(), request == null ? null : request.comment()));
    }

    @Audited(module = "ACCOUNTING", action = "BALANCE_JUSTIFICATION_REVIEW", ressource = "balance_justification")
    @PatchMapping("/accounting/balance-justifications/{id}/review")
    public ResponseEntity<BalanceJustificationService.SavedJustification> reviewJustification(@PathVariable Long id) {
        return ResponseEntity.ok(justifications.review(id));
    }

    @GetMapping("/accounting/accounts/{number}/collective")
    public ResponseEntity<BalanceJustificationService.Collective> collective(@PathVariable String number,
                                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(justifications.collective(number, date));
    }

    @GetMapping("/accounting/closing/{year}/justification-file")
    public ResponseEntity<?> justificationFile(@PathVariable String year, @RequestParam(required = false, defaultValue = "json") String format) {
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(justifications.justificationFile(year));
        }
        return table(justifications.fileTable(year), format);
    }

    // ------------------------------------------------------------------ confirmations de solde

    @Audited(module = "ACCOUNTING", action = "CONFIRMATION_SELECT", ressource = "acc_confirmation_request")
    @PostMapping("/accounting/confirmations")
    public ResponseEntity<List<AccConfirmationRequest>> selectConfirmations(@RequestBody BalanceJustificationService.SelectionRequest request) {
        return ResponseEntity.ok(justifications.select(request));
    }

    @GetMapping("/accounting/confirmations")
    public ResponseEntity<List<AccConfirmationRequest>> confirmations() {
        return ResponseEntity.ok(justifications.confirmations());
    }

    @Audited(module = "ACCOUNTING", action = "CONFIRMATION_SEND", ressource = "acc_confirmation_request")
    @PostMapping("/accounting/confirmations/{number}/send")
    public ResponseEntity<AccConfirmationRequest> sendConfirmation(@PathVariable String number,
                                                                   @RequestParam(required = false, defaultValue = "false") boolean reminder) {
        return ResponseEntity.ok(justifications.send(number, reminder));
    }

    @Audited(module = "ACCOUNTING", action = "CONFIRMATION_REPLY", ressource = "acc_confirmation_request")
    @PostMapping("/accounting/confirmations/{number}/reply")
    public ResponseEntity<AccConfirmationRequest> replyConfirmation(@PathVariable String number, @RequestBody BalanceJustificationService.ReplyRequest request) {
        return ResponseEntity.ok(justifications.reply(number, request));
    }

    @Audited(module = "ACCOUNTING", action = "CONFIRMATION_RECONCILE", ressource = "acc_confirmation_request")
    @PostMapping("/accounting/confirmations/{number}/reconcile")
    public ResponseEntity<AccConfirmationRequest> reconcileConfirmation(@PathVariable String number, @RequestBody ReconcileRequest request) {
        return ResponseEntity.ok(justifications.reconcile(number, request == null ? null : request.comment()));
    }

    @GetMapping("/accounting/confirmations/{number}/letter")
    public ResponseEntity<?> confirmationLetter(@PathVariable String number, @RequestParam(required = false, defaultValue = "pdf") String format) {
        FixedAssetReportService.Table table = justifications.letter(number);
        return "json".equalsIgnoreCase(format) ? ResponseEntity.ok(table) : table(table, format);
    }

    private ResponseEntity<?> table(FixedAssetReportService.Table table, String format) {
        return "csv".equalsIgnoreCase(format)
                ? ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + table.code() + ".csv\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(exporter.csv(table))
                : ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + table.code() + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF).body(exporter.pdf(table));
    }

    private ResponseEntity<?> render(FinancialStatementService.Statement statement, String format) {
        FixedAssetReportService.Table table = statements.table(statement);
        return switch (format.toLowerCase(Locale.ROOT)) {
            case "csv" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + table.code() + ".csv\"")
                    .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8)).body(exporter.csv(table));
            case "pdf" -> ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + table.code() + ".pdf\"")
                    .contentType(MediaType.APPLICATION_PDF).body(exporter.pdf(table));
            default -> ResponseEntity.ok(statement);
        };
    }
}
