package com.sni.bokaticowork.features.payment.service.implementation;

import com.sni.bokaticowork.features.payment.dto.request.CreateCashRegisterRequest;
import com.sni.bokaticowork.features.payment.dto.request.CreateCashVoucherRequest;
import com.sni.bokaticowork.features.payment.dto.response.CashMovementResponse;
import com.sni.bokaticowork.features.payment.enums.CashDocumentType;
import com.sni.bokaticowork.features.payment.enums.CashFlowDirection;
import com.sni.bokaticowork.features.payment.enums.CashMovementType;
import com.sni.bokaticowork.features.payment.enums.CashSessionStatus;
import com.sni.bokaticowork.features.payment.mapper.interfaces.CashRegisterMapper;
import com.sni.bokaticowork.features.payment.model.CashMovement;
import com.sni.bokaticowork.features.payment.model.CashRegister;
import com.sni.bokaticowork.features.payment.model.CashSession;
import com.sni.bokaticowork.features.payment.repository.CashMovementRepository;
import com.sni.bokaticowork.features.payment.repository.CashRegisterRepository;
import com.sni.bokaticowork.features.payment.repository.CashSessionRepository;
import com.sni.bokaticowork.features.payment.service.support.CashSessionSummarySupport;
import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CashRegisterServiceImplTest {

    @Mock
    private CashRegisterRepository registerRepository;

    @Mock
    private CashSessionRepository sessionRepository;

    @Mock
    private CashMovementRepository movementRepository;

    @Mock
    private SequenceGeneratorFacade sequenceGenerator;

    @Mock
    private CashSessionSummarySupport summarySupport;

    @Mock
    private CashRegisterMapper mapper;

    @InjectMocks
    private CashRegisterServiceImpl service;

    @Test
    void shouldGenerateLongAutomaticCashRegisterCodes() {
        when(sequenceGenerator.next("cash_register"))
                .thenReturn("CSR-SEQ-000001", "CSR-SEQ-000002", "CSR-SEQ-000003");
        when(registerRepository.save(any(CashRegister.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toCashRegisterResponse(any(CashRegister.class))).thenAnswer(invocation -> {
            CashRegister value = invocation.getArgument(0);
            return new com.sni.bokaticowork.features.payment.dto.response.CashRegisterResponse(
                    value.getRegisterCode(),
                    value.getName(),
                    value.getLocationCode(),
                    value.getBusinessEntityCode(),
                    value.getDeviceCode(),
                    value.getActive(),
                    value.getCashControlEnabled(),
                    value.getMaxCashAmount(),
                    value.getCreatedAt(),
                    value.getUpdatedAt()
            );
        });

        var response = service.createRegister(new CreateCashRegisterRequest(
                "Caisse reception",
                "LOC-001",
                "BUS-0001",
                "POS-001",
                true,
                new BigDecimal("500000")
        ));

        assertEquals(true, response.registerCode().startsWith("CSR-"));
        assertEquals(true, response.businessEntityCode().startsWith("BIZ-"));
        assertEquals(true, response.deviceCode().startsWith("DEV-"));
    }

    @Test
    void shouldCreateEntryVoucherWithDedicatedDocumentData() {
        CashSession session = openSession();

        when(sessionRepository.findBySessionNumber("CSS-0001")).thenReturn(Optional.of(session));
        when(sequenceGenerator.next("cash_movement")).thenReturn("CSM-202604-00000001");
        when(movementRepository.save(any(CashMovement.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toCashMovementResponse(any(CashMovement.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));

        CashMovementResponse response = service.createEntryVoucher("CSS-0001", new CreateCashVoucherRequest(
                new BigDecimal("15000"),
                "BE-0001",
                "EXTERNAL_COLLECTION",
                "MANUAL_COLLECTION",
                "EXT-REF-001",
                "PARTNER",
                "PTN-001",
                "Partenaire externe",
                "Encaissement externe",
                "cashier-1",
                null,
                "{\"origin\":\"external\"}"
        ));

        verify(movementRepository).save(any(CashMovement.class));
        assertEquals(CashMovementType.CASH_IN, response.movementType());
        assertEquals(CashDocumentType.ENTRY_VOUCHER, response.documentType());
        assertEquals(CashFlowDirection.IN, response.flowDirection());
        assertEquals("BE-0001", response.documentNumber());
        assertEquals("EXTERNAL_COLLECTION", response.flowCategory());
        assertEquals("Partenaire externe", response.counterpartyName());
    }

    @Test
    void shouldListCashSessions() {
        CashSession session = openSession();
        when(sessionRepository.search(eq("CSR-0001"), eq("OPEN"), eq("cashier-1"), isNull(), isNull(), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(session), PageRequest.of(0, 20), 1));
        when(mapper.toCashSessionResponse(any(CashSession.class))).thenAnswer(invocation -> {
            CashSession value = invocation.getArgument(0);
            return new com.sni.bokaticowork.features.payment.dto.response.CashSessionResponse(
                    value.getSessionNumber(),
                    value.getCashRegister().getRegisterCode(),
                    value.getStatus(),
                    value.getOpenedBy(),
                    value.getClosedBy(),
                    value.getReviewedBy(),
                    value.getOpeningAmount(),
                    value.getClosingAmount(),
                    value.getExpectedClosingAmount(),
                    value.getCountedClosingAmount(),
                    value.getVarianceAmount(),
                    value.getVarianceReason(),
                    value.getOpenedAt(),
                    value.getClosingRequestedAt(),
                    value.getClosedAt()
            );
        });

        var response = service.listSessions("CSR-0001", CashSessionStatus.OPEN, "cashier-1", null, null, PageRequest.of(0, 20));

        assertEquals(1, response.getData().size());
        assertEquals("CSS-0001", response.getData().get(0).sessionNumber());
    }

    @Test
    void shouldReturnOverviewMetrics() {
        when(sessionRepository.overviewCounts("CSR-0001", "BUS-001", null, null))
                .thenReturn(new Object[]{2L, 1L, 1L, 1L, 3L, new BigDecimal("2500.00")});
        when(movementRepository.overviewAmounts("CSR-0001", "BUS-001", null, null))
                .thenReturn(new Object[]{8L, new BigDecimal("10000.00"), new BigDecimal("50000.00"), new BigDecimal("5000.00"),
                        new BigDecimal("7000.00"), new BigDecimal("3000.00"), new BigDecimal("1500.00")});

        var response = service.overviewMetrics("CSR-0001", "BUS-001", null, null);

        assertEquals(2L, response.registerCount());
        assertEquals(1L, response.openSessionCount());
        assertEquals(new BigDecimal("60500.00"), response.netCashPosition());
        assertEquals(new BigDecimal("2500.00"), response.pendingVarianceAmount());
    }

    @Test
    void shouldRejectExitVoucherWithoutReason() {
        CashSession session = openSession();
        when(sessionRepository.findBySessionNumber("CSS-0001")).thenReturn(Optional.of(session));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.createExitVoucher("CSS-0001", new CreateCashVoucherRequest(
                new BigDecimal("12000"),
                "BS-0001",
                "SUPPLIER_PAYMENT",
                null,
                null,
                "SUPPLIER",
                "SUP-001",
                "Fournisseur test",
                null,
                "cashier-1",
                null,
                null
        )));

        assertEquals("Reason is required for this cash movement type", ex.getMessage());
    }

    private CashSession openSession() {
        CashRegister register = CashRegister.builder()
                .registerCode("CSR-0001")
                .name("Caisse principale")
                .active(true)
                .cashControlEnabled(true)
                .businessEntityCode("BUS-001")
                .build();
        return CashSession.builder()
                .sessionNumber("CSS-0001")
                .cashRegister(register)
                .status(CashSessionStatus.OPEN)
                .openedBy("cashier-1")
                .openingAmount(BigDecimal.ZERO)
                .openedAt(Instant.parse("2026-04-26T10:00:00Z"))
                .build();
    }

    private CashMovementResponse toResponse(CashMovement movement) {
        return new CashMovementResponse(
                movement.getMovementNumber(),
                movement.getCashSession().getSessionNumber(),
                movement.getCashSession().getCashRegister().getRegisterCode(),
                movement.getCashSession().getCashRegister().getName(),
                movement.getCashSession().getCashRegister().getBusinessEntityCode(),
                movement.getCashSession().getCashRegister().getDeviceCode(),
                movement.getCashSession().getStatus(),
                movement.getCashSession().getOpenedBy(),
                movement.getCashSession().getOpenedAt(),
                movement.getMovementType(),
                movement.getMovementType() == CashMovementType.CASH_OUT ? CashFlowDirection.OUT : CashFlowDirection.IN,
                movement.getAmount(),
                movement.getCurrency(),
                movement.getDocumentType(),
                movement.getDocumentNumber(),
                movement.getFlowCategory(),
                movement.getReferenceType(),
                movement.getReferenceCode(),
                movement.getCounterpartyType(),
                movement.getCounterpartyCode(),
                movement.getCounterpartyName(),
                movement.getReason(),
                movement.getCreatedBy(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                movement.getMetadataJson(),
                movement.getStatus(),
                null,
                movement.getBatchId(),
                movement.getRunningBalance(),
                movement.getExchangeRate(),
                movement.getChannel(),
                movement.getDeviceCode(),
                movement.getDeviceIp(),
                movement.getSubCategory(),
                movement.getTags(),
                movement.getRiskScore(),
                movement.getRequiresSignature(),
                movement.getSignedBy(),
                movement.getSignedAt(),
                movement.getPrintedAt(),
                List.of(),
                Instant.now()
        );
    }
}
