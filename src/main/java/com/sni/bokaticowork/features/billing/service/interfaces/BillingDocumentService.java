package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.core.templateResponse.PaginatedResponse;
import com.sni.bokaticowork.features.billing.dto.request.CreateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateInvoiceFromBillableItemsRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateManualBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.request.CreateReservationInvoiceRequest;
import com.sni.bokaticowork.features.billing.dto.request.SelectQuoteOptionsRequest;
import com.sni.bokaticowork.features.billing.dto.request.UpdateBillingDocumentRequest;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface BillingDocumentService {
    BillingDocumentResponse create(CreateBillingDocumentRequest request);
    BillingDocumentResponse update(String documentNumber, UpdateBillingDocumentRequest request);
    BillingDocumentResponse createManualInvoice(CreateManualBillingDocumentRequest request);
    BillingDocumentResponse createManualQuote(CreateManualBillingDocumentRequest request);
    BillingDocumentResponse createManualQuotation(CreateManualBillingDocumentRequest request);
    BillingDocumentResponse createInvoiceFromBillableItems(CreateInvoiceFromBillableItemsRequest request);
    BillingDocumentResponse createInvoiceFromReservation(CreateReservationInvoiceRequest request);
    BillingDocumentResponse duplicate(String documentNumber);
    BillingDocumentResponse issue(String documentNumber);
    BillingDocumentResponse send(String documentNumber);
    BillingDocumentResponse selectQuoteOptions(String quoteNumber, SelectQuoteOptionsRequest request);
    BillingDocumentResponse markViewed(String quoteNumber);
    BillingDocumentResponse startNegotiation(String quoteNumber);
    BillingDocumentResponse requestDeposit(String quoteNumber);
    BillingDocumentResponse markDepositPaid(String quoteNumber);
    BillingDocumentResponse acceptQuote(String quoteNumber);
    BillingDocumentResponse rejectQuote(String quoteNumber);
    BillingDocumentResponse convertQuoteToInvoice(String quoteNumber);
    BillingDocumentResponse acceptQuotation(String quotationNumber);
    BillingDocumentResponse rejectQuotation(String quotationNumber);
    BillingDocumentResponse convertQuotationToInvoice(String quotationNumber);
    BillingDocumentResponse createCreditNote(String invoiceNumber, CreateCreditNoteRequest request);
    BillingDocumentResponse applyCreditNote(String creditNoteNumber);
    CustomerStatementResponse customerStatement(String customerType, String customerCode, Pageable pageable);
    int markOverdueDocuments();
    BillingDocumentResponse get(String documentNumber);
    PaginatedResponse<BillingDocumentResponse> list(BillingDocumentType type,
                                                    BillingDocumentStatus status,
                                                    String customerType,
                                                    String customerCode,
                                                    String sourceType,
                                                    String sourceCode,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String searchText,
                                                    Pageable pageable);
    BillingDocument serviceByNumber(String documentNumber);
    BillingDocument applyPayment(String documentNumber, BigDecimal amount);
    BillingDocument reversePayment(String documentNumber, BigDecimal amount);
    BillingDocument cancelAndArchive(String documentNumber, String reason);
}
