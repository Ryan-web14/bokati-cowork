package com.sni.bokaticowork.features.marketplace.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.client.customer.enums.CustomerType;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver;
import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletMailer;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.response.StockReservationResponse;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryConsumptionService;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.StockService;
import com.sni.bokaticowork.features.marketplace.dto.MarketplaceDtos;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOffer;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrderLine;
import com.sni.bokaticowork.features.marketplace.repository.MarketplaceOrderRepository;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Une commande de la marketplace, du panier au retrait · stock retenu, facture, reglement,
 * expiration et annulation.
 */
class MarketplaceOrderServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

    private final MarketplaceOrderRepository orders = mock(MarketplaceOrderRepository.class);
    private final MarketplaceCatalogService catalog = mock(MarketplaceCatalogService.class);
    private final MemberRepository members = mock(MemberRepository.class);
    private final MemberBillingCompanyResolver companyResolver = mock(MemberBillingCompanyResolver.class);
    private final InventoryConsumptionService consumption = mock(InventoryConsumptionService.class);
    private final StockService stock = mock(StockService.class);
    private final InventoryItemRepository items = mock(InventoryItemRepository.class);
    private final BillableItemRepository billables = mock(BillableItemRepository.class);
    private final SequenceGeneratorFacade sequences = mock(SequenceGeneratorFacade.class);
    private final BillableItemInvoiceSupport invoicing = mock(BillableItemInvoiceSupport.class);
    private final BillingDocumentService billing = mock(BillingDocumentService.class);
    private final BillingDocumentRepository documents = mock(BillingDocumentRepository.class);
    private final MarketplaceOrderService service = new MarketplaceOrderService(orders, catalog, members, companyResolver,
            consumption, stock, items, billables, sequences, invoicing, billing, documents, mock(BillingEmailService.class),
            mock(CompanyWalletMailer.class), mock(MemberInAppNotifier.class), Clock.fixed(NOW, ZoneOffset.UTC),
            Duration.ofMinutes(30));

    private Member awa;
    private MarketplaceOffer coffee;
    private MarketplaceOffer printing;
    private final AtomicInteger counter = new AtomicInteger();

    @BeforeEach
    void setUp() {
        awa = new Member();
        awa.setMemberId("MBR-1");
        awa.setFirstname("Awa");
        awa.setLastname("Mabiala");
        awa.setEmail("awa@acme.cg");
        when(members.findByMemberIdAndDeletedFalse("MBR-1")).thenReturn(Optional.of(awa));

        coffee = MarketplaceOffer.builder().id(1L).shortId(1L).kind(MarketplaceOffer.Kind.PRODUCT).title("Café")
                .inventoryItemCode("ITM-CAFE").locationCode("LOC-ACCUEIL").currency("XAF").published(true).build();
        printing = MarketplaceOffer.builder().id(2L).shortId(2L).kind(MarketplaceOffer.Kind.SERVICE).title("Impression A4")
                .serviceItemCode("SRV-IMP").currency("XAF").published(true).build();
        when(catalog.sellable(any())).thenReturn(true);
        when(catalog.priceOf(coffee)).thenReturn(Optional.of(BigDecimal.valueOf(1500)));
        when(catalog.priceOf(printing)).thenReturn(Optional.of(BigDecimal.valueOf(100)));
        when(catalog.availableOf(coffee)).thenReturn(BigDecimal.valueOf(10));
        when(catalog.availableOf(printing)).thenReturn(null);
        when(catalog.offer(1L)).thenReturn(coffee);

        when(sequences.next("marketplace_order")).thenReturn("MKC-2026-000001");
        when(sequences.next("billable_item")).thenAnswer(call -> "BIL-" + counter.incrementAndGet());
        when(billables.save(any(BillableItem.class))).thenAnswer(call -> call.getArgument(0));
        when(orders.saveAndFlush(any(MarketplaceOrder.class))).thenAnswer(call -> call.getArgument(0));
        when(orders.save(any(MarketplaceOrder.class))).thenAnswer(call -> call.getArgument(0));
        StockReservationResponse reservation = StockReservationResponse.builder().reservationCode("RSV-1").build();
        when(consumption.reserveForBusinessEvent(any())).thenReturn(reservation);
    }

    private MarketplaceOrder cart() {
        MarketplaceOrder cart = MarketplaceOrder.builder().id(10L).shortId(10L).buyerMemberCode("MBR-1")
                .status(MarketplaceOrder.Status.CART).lines(new ArrayList<>()).build();
        cart.getLines().add(MarketplaceOrderLine.builder().id(11L).shortId(11L).order(cart).offer(coffee).title("Café").quantity(2).build());
        cart.getLines().add(MarketplaceOrderLine.builder().id(12L).shortId(12L).order(cart).offer(printing).title("Impression A4").quantity(10).build());
        return cart;
    }

    private MarketplaceOrder placed(MarketplaceOrder.Status status) {
        MarketplaceOrder order = MarketplaceOrder.builder().id(20L).shortId(20L).orderNumber("MKC-2026-000001")
                .buyerMemberCode("MBR-1").status(status).currency("XAF").totalAmount(BigDecimal.valueOf(4000))
                .invoiceNumber("FAC-1").expiresAt(NOW.plusSeconds(600)).lines(new ArrayList<>()).build();
        order.getLines().add(MarketplaceOrderLine.builder().id(21L).shortId(21L).order(order).offer(coffee).title("Café")
                .quantity(2).unitPrice(BigDecimal.valueOf(1500)).amount(BigDecimal.valueOf(3000)).stockReservationCode("RSV-1").build());
        order.getLines().add(MarketplaceOrderLine.builder().id(22L).shortId(22L).order(order).offer(printing).title("Impression A4")
                .quantity(10).unitPrice(BigDecimal.valueOf(100)).amount(BigDecimal.valueOf(1000)).build());
        when(orders.lockByOrderNumber("MKC-2026-000001")).thenReturn(Optional.of(order));
        return order;
    }

    private void invoice(long paid, long balance) {
        BillingDocument invoice = BillingDocument.builder().documentNumber("FAC-1").paidAmount(BigDecimal.valueOf(paid))
                .balanceDue(BigDecimal.valueOf(balance)).totalAmount(BigDecimal.valueOf(4000)).build();
        when(documents.findByDocumentNumber("FAC-1")).thenReturn(Optional.of(invoice));
    }

    @Test
    @DisplayName("Validation : prix figes, stock retenu pour l'article seul, facture au nom de l'organisation qui paie")
    void checkout() {
        MarketplaceOrder cart = cart();
        when(orders.findCart("MBR-1")).thenReturn(Optional.of(cart));
        Customer acme = Customer.builder().customerId("CUS-COM-1").companyName("SARL Acme").type(CustomerType.COMPANY).build();
        when(companyResolver.resolve("MEMBER", "MBR-1", "CUS-COM-1")).thenReturn(Optional.of(acme));
        BillingDocumentResponse issued = mock(BillingDocumentResponse.class);
        when(issued.documentNumber()).thenReturn("FAC-1");
        when(invoicing.ensureInvoicedGroup(anyList(), anyString(), anyString())).thenReturn(issued);

        MarketplaceDtos.OrderView view = service.checkout(awa, new MarketplaceDtos.CheckoutRequest("CUS-COM-1"));

        assertThat(view.status()).isEqualTo(MarketplaceOrder.Status.PENDING_PAYMENT);
        assertThat(view.orderNumber()).isEqualTo("MKC-2026-000001");
        assertThat(view.totalAmount()).isEqualByComparingTo("4000");
        assertThat(view.billingCompanyCode()).isEqualTo("CUS-COM-1");
        assertThat(view.invoiceNumber()).isEqualTo("FAC-1");
        assertThat(view.expiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));

        ArgumentCaptor<StockReservationRequest> reservation = ArgumentCaptor.forClass(StockReservationRequest.class);
        verify(consumption).reserveForBusinessEvent(reservation.capture());
        assertThat(reservation.getValue().getItemCode()).isEqualTo("ITM-CAFE");
        assertThat(reservation.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(reservation.getValue().getReferenceType()).isEqualTo(StockReferenceType.POS_SALE);
        assertThat(reservation.getValue().getExpiresAt()).isNull();
        assertThat(cart.getLines().getFirst().getStockReservationCode()).isEqualTo("RSV-1");
        assertThat(cart.getLines().get(1).getStockReservationCode()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BillableItem>> billed = ArgumentCaptor.forClass(List.class);
        verify(invoicing).ensureInvoicedGroup(billed.capture(), anyString(), anyString());
        assertThat(billed.getValue()).hasSize(2).allSatisfy(item -> {
            assertThat(item.getSourceType()).isEqualTo("MARKETPLACE_ORDER");
            assertThat(item.getSourceId()).isEqualTo("MKC-2026-000001");
            assertThat(item.getBillingCompanyCode()).isEqualTo("CUS-COM-1");
            assertThat(item.getSubscriberCode()).isEqualTo("MBR-1");
        });
        assertThat(billed.getValue()).extracting(BillableItem::getAmount)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(BigDecimal.valueOf(3000), BigDecimal.valueOf(1000));
    }

    @Test
    @DisplayName("Facture soldee : le stock sort, la commande attend son retrait")
    void paidConsumesStock() {
        MarketplaceOrder order = placed(MarketplaceOrder.Status.PENDING_PAYMENT);
        invoice(4000, 0);

        assertThat(service.sync("MKC-2026-000001")).isTrue();

        verify(consumption).consumeBusinessReservation(eq("RSV-1"), anyString());
        assertThat(order.getStatus()).isEqualTo(MarketplaceOrder.Status.PAID);
        assertThat(order.getPaidAt()).isEqualTo(NOW);
        assertThat(order.getExpiresAt()).isNull();
    }

    @Test
    @DisplayName("Echue sans reglement : stock rendu, facture annulee · echue mais en partie reglee : elle attend")
    void expiry() {
        MarketplaceOrder order = placed(MarketplaceOrder.Status.PENDING_PAYMENT);
        order.setExpiresAt(NOW.minusSeconds(1));
        invoice(1000, 3000);

        assertThat(service.sync("MKC-2026-000001")).isFalse();
        verify(consumption, never()).releaseBusinessReservation(anyString());

        invoice(0, 4000);
        assertThat(service.sync("MKC-2026-000001")).isTrue();

        verify(consumption).releaseBusinessReservation("RSV-1");
        verify(billing).cancelAndArchive(eq("FAC-1"), anyString());
        assertThat(order.getStatus()).isEqualTo(MarketplaceOrder.Status.EXPIRED);
    }

    @Test
    @DisplayName("Annulation d'une commande payee : avoir valide, reverse au portefeuille, articles remis en stock")
    void staffCancelsPaidOrder() {
        MarketplaceOrder order = placed(MarketplaceOrder.Status.PAID);
        invoice(4000, 0);
        BillingDocumentResponse creditNote = mock(BillingDocumentResponse.class);
        when(creditNote.documentNumber()).thenReturn("AV-1");
        when(billing.createCreditNote(eq("FAC-1"), any())).thenReturn(creditNote);

        service.cancelByStaff("MKC-2026-000001", new MarketplaceDtos.CancelRequest("Rupture fournisseur", null), "admin@sni");

        ArgumentCaptor<CreateCreditNoteRequest> request = ArgumentCaptor.forClass(CreateCreditNoteRequest.class);
        verify(billing).createCreditNote(eq("FAC-1"), request.capture());
        assertThat(request.getValue().amount()).isEqualByComparingTo("4000");
        assertThat(request.getValue().applyImmediately()).isFalse();
        assertThat(request.getValue().validateImmediately()).isTrue();
        verify(billing).refundCreditNoteToWallet(eq("AV-1"), anyString());
        ArgumentCaptor<StockInRequest> back = ArgumentCaptor.forClass(StockInRequest.class);
        verify(stock).receive(back.capture());
        assertThat(back.getValue().getItemCode()).isEqualTo("ITM-CAFE");
        assertThat(back.getValue().getLocationCode()).isEqualTo("LOC-ACCUEIL");
        assertThat(back.getValue().getQuantity()).isEqualByComparingTo("2");
        assertThat(order.getStatus()).isEqualTo(MarketplaceOrder.Status.CANCELLED);
        assertThat(order.getCreditNoteNumber()).isEqualTo("AV-1");
        assertThat(order.getCancelledBy()).isEqualTo("admin@sni");
    }

    @Test
    @DisplayName("Une commande deja remise ne revient en stock que sur demande")
    void fulfilledOrderStaysOutOfStock() {
        placed(MarketplaceOrder.Status.FULFILLED);
        invoice(4000, 0);
        BillingDocumentResponse creditNote = mock(BillingDocumentResponse.class);
        when(creditNote.documentNumber()).thenReturn("AV-1");
        when(billing.createCreditNote(eq("FAC-1"), any())).thenReturn(creditNote);

        service.cancelByStaff("MKC-2026-000001", new MarketplaceDtos.CancelRequest("Produit défectueux", false), "admin@sni");

        verify(billing).refundCreditNoteToWallet(eq("AV-1"), anyString());
        verify(stock, never()).receive(any());
    }

    @Test
    @DisplayName("Le membre annule une commande non reglee · pas une commande deja en partie reglee")
    void memberCancel() {
        MarketplaceOrder order = placed(MarketplaceOrder.Status.PENDING_PAYMENT);
        invoice(500, 3500);
        assertThatThrownBy(() -> service.cancelByMember(awa, "MKC-2026-000001", null))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("accueil");

        invoice(0, 4000);
        service.cancelByMember(awa, "MKC-2026-000001", null);

        verify(consumption).releaseBusinessReservation("RSV-1");
        verify(billing).cancelAndArchive(eq("FAC-1"), anyString());
        assertThat(order.getStatus()).isEqualTo(MarketplaceOrder.Status.CANCELLED);
    }

    @Test
    @DisplayName("Remise au comptoir : une commande reglee seulement, avec le nom de qui la retire")
    void fulfil() {
        MarketplaceOrder order = placed(MarketplaceOrder.Status.PENDING_PAYMENT);
        invoice(0, 4000);
        assertThatThrownBy(() -> service.fulfil("MKC-2026-000001", null, "accueil"))
                .isInstanceOf(BadRequestException.class);

        order.setStatus(MarketplaceOrder.Status.PAID);
        service.fulfil("MKC-2026-000001", new MarketplaceDtos.FulfilRequest("Jean Mbemba"), "accueil");

        assertThat(order.getStatus()).isEqualTo(MarketplaceOrder.Status.FULFILLED);
        assertThat(order.getCollectedBy()).isEqualTo("Jean Mbemba");
        assertThat(order.getFulfilledBy()).isEqualTo("accueil");
    }

    @Test
    @DisplayName("Panier : la quantite s'additionne, sans depasser le plafond par commande ni le stock")
    void cartLimits() {
        MarketplaceOrder cart = cart();
        when(orders.findCart("MBR-1")).thenReturn(Optional.of(cart));

        service.addToCart(awa, new MarketplaceDtos.CartLineRequest(1L, 3));
        assertThat(cart.getLines().getFirst().getQuantity()).isEqualTo(5);

        assertThatThrownBy(() -> service.addToCart(awa, new MarketplaceDtos.CartLineRequest(1L, 6)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("en stock");

        coffee.setMaxPerOrder(4);
        assertThatThrownBy(() -> service.addToCart(awa, new MarketplaceDtos.CartLineRequest(1L, 1)))
                .isInstanceOf(BadRequestException.class).hasMessageContaining("au plus");
    }
}
