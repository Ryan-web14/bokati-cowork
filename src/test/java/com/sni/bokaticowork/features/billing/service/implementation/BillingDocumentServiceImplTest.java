package com.sni.bokaticowork.features.billing.service.implementation;

import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.dto.response.CustomerStatementResponse;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;
import com.sni.bokaticowork.features.billing.enums.BillingDocumentType;
import com.sni.bokaticowork.features.billing.mapper.interfaces.BillingDocumentMapper;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BillingDocumentServiceImplTest {

    @Test
    void shouldUseUnsortedPageableForCustomerStatementNativeQuery() {
        BillingDocument firstDocument = document("INV-001", "100.00", "40.00", "60.00");
        BillingDocument secondDocument = document("INV-002", "50.00", "10.00", "40.00");
        BillingDocumentResponse firstResponse = response("INV-001", "100.00", "40.00", "60.00");
        BillingDocumentResponse secondResponse = response("INV-002", "50.00", "10.00", "40.00");
        AtomicReference<Pageable> capturedPageable = new AtomicReference<>();

        BillingDocumentRepository repository = repositoryProxy(
                capturedPageable,
                List.of(firstDocument, secondDocument)
        );
        BillingDocumentMapper mapper = mapperProxy(firstDocument, secondDocument, firstResponse, secondResponse);

        BillingDocumentServiceImpl service = new BillingDocumentServiceImpl(
                repository,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                mapper
        );

        CustomerStatementResponse statement = service.customerStatement(
                "MEMBER",
                "MBR-000003",
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertFalse(capturedPageable.get().getSort().isSorted());
        assertEquals(0, capturedPageable.get().getPageNumber());
        assertEquals(20, capturedPageable.get().getPageSize());
        assertEquals(new BigDecimal("150.00"), statement.totalInvoiced());
        assertEquals(new BigDecimal("50.00"), statement.totalPaid());
        assertEquals(new BigDecimal("100.00"), statement.totalBalanceDue());
        assertEquals(2, statement.documents().size());
    }

    private BillingDocumentRepository repositoryProxy(AtomicReference<Pageable> capturedPageable,
                                                      List<BillingDocument> documents) {
        return (BillingDocumentRepository) Proxy.newProxyInstance(
                BillingDocumentRepository.class.getClassLoader(),
                new Class<?>[]{BillingDocumentRepository.class},
                (proxy, method, args) -> {
                    if ("statementDocuments".equals(method.getName())) {
                        capturedPageable.set((Pageable) args[2]);
                        Pageable pageable = (Pageable) args[2];
                        return new PageImpl<>(documents, pageable, documents.size());
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "BillingDocumentRepositoryProxy";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private BillingDocumentMapper mapperProxy(BillingDocument firstDocument,
                                              BillingDocument secondDocument,
                                              BillingDocumentResponse firstResponse,
                                              BillingDocumentResponse secondResponse) {
        return (BillingDocumentMapper) Proxy.newProxyInstance(
                BillingDocumentMapper.class.getClassLoader(),
                new Class<?>[]{BillingDocumentMapper.class},
                (proxy, method, args) -> {
                    if ("toResponse".equals(method.getName())) {
                        if (args[0] == firstDocument) {
                            return firstResponse;
                        }
                        if (args[0] == secondDocument) {
                            return secondResponse;
                        }
                        throw new UnsupportedOperationException("Unknown billing document");
                    }
                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }
                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(method.getName())) {
                        return "BillingDocumentMapperProxy";
                    }
                    throw new UnsupportedOperationException(method.getName());
                }
        );
    }

    private BillingDocument document(String number, String total, String paid, String balance) {
        return BillingDocument.builder()
                .documentNumber(number)
                .documentType(BillingDocumentType.INVOICE)
                .status(BillingDocumentStatus.ISSUED)
                .customerType("MEMBER")
                .customerCode("MBR-000003")
                .customerName("Jean")
                .currency("XAF")
                .totalAmount(new BigDecimal(total))
                .paidAmount(new BigDecimal(paid))
                .balanceDue(new BigDecimal(balance))
                .issueDate(LocalDate.now())
                .build();
    }

    private BillingDocumentResponse response(String number, String total, String paid, String balance) {
        return new BillingDocumentResponse(
                number,
                BillingDocumentType.INVOICE,
                BillingDocumentStatus.ISSUED,
                "MEMBER",
                "MBR-000003",
                "Jean",
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                "XAF",
                new BigDecimal(total),
                BigDecimal.ZERO,
                new BigDecimal(total),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal(total),
                new BigDecimal(paid),
                new BigDecimal(balance),
                LocalDate.now(),
                LocalDate.now(),
                null,
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
