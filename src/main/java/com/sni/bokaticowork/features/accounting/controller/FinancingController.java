package com.sni.bokaticowork.features.accounting.controller;

import com.sni.bokaticowork.core.audit.aop.Audited;
import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.accounting.model.*;
import com.sni.bokaticowork.features.accounting.service.implementation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Financement, hors bilan et paie (lot F17). */
@RestController
@RequestMapping(ApiPath.V1 + "/accounting")
@RequiredArgsConstructor
public class FinancingController {

    private final LoanService loans;
    private final PartnerAccountService partners;
    private final CommitmentService commitments;
    private final PayrollService payroll;

    // ------------------------------------------------------------------ emprunts

    @GetMapping("/loans")
    public ResponseEntity<List<AccLoan>> loans() {
        return ResponseEntity.ok(loans.list());
    }

    @GetMapping("/loans/note")
    public ResponseEntity<List<LoanService.NoteLine>> loanNote(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(loans.note(date));
    }

    @GetMapping("/loans/upcoming")
    public ResponseEntity<List<Map<String, Object>>> upcoming(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(loans.upcoming(from, to));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_ACCRUE", ressource = "acc_loan")
    @PostMapping("/loans/accrue")
    public ResponseEntity<List<LoanService.Accrual>> accrue(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(loans.accrue(date));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_CREATE", ressource = "acc_loan")
    @PostMapping("/loans")
    public ResponseEntity<LoanService.LoanView> createLoan(@RequestBody LoanService.LoanRequest request) {
        return ResponseEntity.ok(loans.create(request));
    }

    @GetMapping("/loans/{code}")
    public ResponseEntity<LoanService.LoanView> loan(@PathVariable String code) {
        return ResponseEntity.ok(loans.view(code));
    }

    @GetMapping("/loans/{code}/history")
    public ResponseEntity<List<AccLoanInstallment>> history(@PathVariable String code) {
        return ResponseEntity.ok(loans.history(code));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_DISBURSE", ressource = "acc_loan")
    @PostMapping("/loans/{code}/disburse")
    public ResponseEntity<LoanService.LoanView> disburse(@PathVariable String code) {
        return ResponseEntity.ok(loans.disburse(code));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_INSTALLMENT_POST", ressource = "acc_loan_installment")
    @PostMapping("/loans/{code}/installments/{number}/post")
    public ResponseEntity<AccLoanInstallment> postInstallment(@PathVariable String code, @PathVariable int number) {
        return ResponseEntity.ok(loans.postInstallment(code, number));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_PREPAY", ressource = "acc_loan")
    @PostMapping("/loans/{code}/prepay")
    public ResponseEntity<LoanService.LoanView> prepay(@PathVariable String code, @RequestBody LoanService.Prepayment request) {
        return ResponseEntity.ok(loans.prepay(code, request));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_RATE_CHANGE", ressource = "acc_loan")
    @PostMapping("/loans/{code}/rate")
    public ResponseEntity<LoanService.LoanView> rate(@PathVariable String code, @RequestParam BigDecimal rate,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(loans.changeRate(code, rate, date));
    }

    @Audited(module = "ACCOUNTING", action = "LOAN_POSTPONE", ressource = "acc_loan")
    @PostMapping("/loans/{code}/postpone")
    public ResponseEntity<LoanService.LoanView> postpone(@PathVariable String code) {
        return ResponseEntity.ok(loans.postpone(code));
    }

    @GetMapping("/loans/{code}/export")
    public ResponseEntity<byte[]> loanCsv(@PathVariable String code) {
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + code + ".csv\"").body(loans.csv(code));
    }

    @GetMapping("/loans/{code}/pdf")
    public ResponseEntity<byte[]> loanPdf(@PathVariable String code) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + code + ".pdf\"").body(loans.pdf(code));
    }

    // ------------------------------------------------------------------ comptes courants d'associes

    @GetMapping("/partners")
    public ResponseEntity<List<AccPartnerAccount>> partners() {
        return ResponseEntity.ok(partners.list());
    }

    @GetMapping("/partners/note")
    public ResponseEntity<List<PartnerAccountService.NoteLine>> partnerNote(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(partners.note(date));
    }

    @Audited(module = "ACCOUNTING", action = "PARTNER_SAVE", ressource = "acc_partner_account")
    @PostMapping("/partners")
    public ResponseEntity<AccPartnerAccount> savePartner(@RequestBody PartnerAccountService.PartnerRequest request) {
        return ResponseEntity.ok(partners.save(request));
    }

    @Audited(module = "ACCOUNTING", action = "PARTNER_MOVEMENT", ressource = "acc_partner_account")
    @PostMapping("/partners/{code}/movements")
    public ResponseEntity<String> move(@PathVariable String code, @RequestBody PartnerAccountService.Movement movement) {
        return ResponseEntity.ok(partners.move(code, movement).getEntryNumber());
    }

    @Audited(module = "ACCOUNTING", action = "PARTNER_INTEREST", ressource = "acc_partner_interest")
    @PostMapping("/partners/{code}/interest")
    public ResponseEntity<AccPartnerInterest> interest(@PathVariable String code, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(partners.interest(code, from, to));
    }

    @GetMapping("/partners/{code}/interests")
    public ResponseEntity<List<AccPartnerInterest>> interests(@PathVariable String code) {
        return ResponseEntity.ok(partners.interests(code));
    }

    @GetMapping("/partners/{code}/statement")
    public ResponseEntity<PartnerAccountService.Statement> statement(@PathVariable String code,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(partners.statement(code, from, to));
    }

    // ------------------------------------------------------------------ hors bilan

    @GetMapping("/commitment-types")
    public ResponseEntity<List<AccCommitmentType>> commitmentTypes() {
        return ResponseEntity.ok(commitments.types());
    }

    @Audited(module = "ACCOUNTING", action = "COMMITMENT_TYPE_SAVE", ressource = "acc_commitment_type")
    @PostMapping("/commitment-types")
    public ResponseEntity<AccCommitmentType> saveType(@RequestBody AccCommitmentType type) {
        return ResponseEntity.ok(commitments.saveType(type));
    }

    @GetMapping("/commitments")
    public ResponseEntity<List<AccCommitment>> commitments() {
        return ResponseEntity.ok(commitments.list());
    }

    @GetMapping("/commitments/reconciliation")
    public ResponseEntity<CommitmentService.Reconciliation> commitmentReconciliation() {
        return ResponseEntity.ok(commitments.reconciliation());
    }

    @GetMapping("/commitments/note")
    public ResponseEntity<List<CommitmentService.NoteLine>> commitmentNote(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(commitments.note(date));
    }

    @Audited(module = "ACCOUNTING", action = "COMMITMENT_SAVE", ressource = "acc_commitment")
    @PostMapping("/commitments")
    public ResponseEntity<AccCommitment> saveCommitment(@RequestBody CommitmentService.CommitmentRequest request) {
        return ResponseEntity.ok(commitments.save(request));
    }

    @Audited(module = "ACCOUNTING", action = "COMMITMENT_RELEASE", ressource = "acc_commitment")
    @PostMapping("/commitments/{code}/release")
    public ResponseEntity<AccCommitment> release(@PathVariable String code,
                                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                 @RequestParam(required = false) String reference) {
        return ResponseEntity.ok(commitments.release(code, date, reference));
    }

    // ------------------------------------------------------------------ paie

    @GetMapping("/payroll/templates")
    public ResponseEntity<List<AccPayrollTemplate>> payrollTemplates() {
        return ResponseEntity.ok(payroll.templates());
    }

    @Audited(module = "ACCOUNTING", action = "PAYROLL_TEMPLATE_SAVE", ressource = "acc_payroll_template")
    @PostMapping("/payroll/templates")
    public ResponseEntity<AccPayrollTemplate> savePayrollTemplate(@RequestBody AccPayrollTemplate template) {
        return ResponseEntity.ok(payroll.saveTemplate(template));
    }

    @GetMapping("/payroll/rubrics")
    public ResponseEntity<List<AccPayrollRubric>> rubrics() {
        return ResponseEntity.ok(payroll.rubrics());
    }

    @Audited(module = "ACCOUNTING", action = "PAYROLL_RUBRIC_SAVE", ressource = "acc_payroll_rubric")
    @PostMapping("/payroll/rubrics")
    public ResponseEntity<AccPayrollRubric> saveRubric(@RequestBody AccPayrollRubric rubric) {
        return ResponseEntity.ok(payroll.saveRubric(rubric));
    }

    @GetMapping("/payroll/employees")
    public ResponseEntity<List<AccPayrollEmployee>> payrollEmployees() {
        return ResponseEntity.ok(payroll.employees());
    }

    @Audited(module = "ACCOUNTING", action = "PAYROLL_EMPLOYEE_SAVE", ressource = "acc_payroll_employee")
    @PostMapping("/payroll/employees")
    public ResponseEntity<AccPayrollEmployee> saveEmployee(@RequestBody AccPayrollEmployee employee) {
        return ResponseEntity.ok(payroll.saveEmployee(employee));
    }

    @GetMapping("/payroll/imports")
    public ResponseEntity<List<AccPayrollImport>> payrollImports() {
        return ResponseEntity.ok(payroll.list());
    }

    @Audited(module = "ACCOUNTING", action = "PAYROLL_IMPORT", ressource = "acc_payroll_import")
    @PostMapping(value = "/payroll/imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PayrollService.ImportView> importPayroll(@RequestParam String period, @RequestParam(required = false) String template,
                                                                   @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(payroll.importFile(period, template, file.getBytes(), file.getOriginalFilename()));
    }

    @GetMapping("/payroll/imports/{id}")
    public ResponseEntity<PayrollService.ImportView> payrollImport(@PathVariable Long id) {
        return ResponseEntity.ok(payroll.view(id));
    }

    @GetMapping("/payroll/imports/{id}/lines")
    public ResponseEntity<List<AccPayrollLine>> payrollLines(@PathVariable Long id) {
        return ResponseEntity.ok(payroll.lines(id));
    }

    @Audited(module = "ACCOUNTING", action = "PAYROLL_POST", ressource = "acc_payroll_import")
    @PostMapping("/payroll/imports/{id}/post")
    public ResponseEntity<PayrollService.ImportView> postPayroll(@PathVariable Long id) {
        return ResponseEntity.ok(payroll.post(id));
    }

    @Audited(module = "ACCOUNTING", action = "PAYROLL_PAY", ressource = "acc_payroll_import")
    @PostMapping("/payroll/imports/{id}/pay")
    public ResponseEntity<PayrollService.ImportView> payPayroll(@PathVariable Long id, @RequestBody PayrollService.PaymentRequest request) {
        return ResponseEntity.ok(payroll.pay(id, request));
    }
}
