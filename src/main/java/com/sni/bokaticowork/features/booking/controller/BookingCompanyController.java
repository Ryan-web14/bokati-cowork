package com.sni.bokaticowork.features.booking.controller;

import com.sni.bokaticowork.core.utils.path.ApiPath;
import com.sni.bokaticowork.features.booking.service.support.BookingCompanyAccounts;
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

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

/** Lot B9e : conditions de reservation des entreprises (budget, releve mensuel, forfait d'heures). */
@RestController
@RequestMapping(ApiPath.V1 + "/bookings/companies")
@RequiredArgsConstructor
public class BookingCompanyController {

    private final BookingCompanyAccounts accounts;

    public record TermsRequest(Boolean monthlyStatement, BigDecimal monthlyBudget, Integer monthlyHours) {
    }

    public record BudgetRequest(BigDecimal monthlyBudget) {
    }

    @GetMapping("/{businessCode}/terms")
    public ResponseEntity<BookingCompanyAccounts.Terms> terms(@PathVariable String businessCode) {
        return ResponseEntity.ok(accounts.terms(businessCode));
    }

    @PutMapping("/{businessCode}/terms")
    public ResponseEntity<BookingCompanyAccounts.Terms> saveTerms(@PathVariable String businessCode, @RequestBody TermsRequest request) {
        return ResponseEntity.ok(accounts.saveTerms(businessCode, request.monthlyStatement(), request.monthlyBudget(), request.monthlyHours()));
    }

    @GetMapping("/{businessCode}/budgets")
    public ResponseEntity<List<BookingCompanyAccounts.MemberBudget>> budgets(@PathVariable String businessCode) {
        return ResponseEntity.ok(accounts.memberBudgets(businessCode));
    }

    /** Budget mensuel d'un collaborateur ; 0 ou vide le retire. */
    @PutMapping("/{businessCode}/budgets/{memberId}")
    public ResponseEntity<List<BookingCompanyAccounts.MemberBudget>> setBudget(@PathVariable String businessCode, @PathVariable String memberId,
                                                                               @RequestBody BudgetRequest request) {
        accounts.setMemberBudget(businessCode, memberId, request.monthlyBudget());
        return ResponseEntity.ok(accounts.memberBudgets(businessCode));
    }

    /** Consommation du mois (AAAA-MM, mois en cours par defaut) : depense, budget, heures couvertes et restantes. */
    @GetMapping("/{businessCode}/usage")
    public ResponseEntity<BookingCompanyAccounts.Usage> usage(@PathVariable String businessCode, @RequestParam(required = false) String month) {
        return ResponseEntity.ok(accounts.usage(businessCode, month == null || month.isBlank() ? null : YearMonth.parse(month.trim())));
    }

    /** Releves mensuels a emettre (mois precedent par defaut) ; une entreprise deja facturee pour ce mois est sautee. */
    @PostMapping("/statements/run")
    public ResponseEntity<List<BookingCompanyAccounts.Statement>> runStatements(@RequestParam(required = false) String month) {
        return ResponseEntity.ok(accounts.runStatements(month == null || month.isBlank() ? null : YearMonth.parse(month.trim())));
    }
}
