package com.sni.bokaticowork.features.marketplace.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.features.billing.dto.request.CreateCreditNoteRequest;
import com.sni.bokaticowork.features.billing.dto.response.BillingDocumentResponse;
import com.sni.bokaticowork.features.billing.model.BillingDocument;
import com.sni.bokaticowork.features.billing.repository.BillingDocumentRepository;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingDocumentService;
import com.sni.bokaticowork.features.billing.service.interfaces.BillingEmailService;
import com.sni.bokaticowork.features.billing.service.support.BillableItemInvoiceSupport;
import com.sni.bokaticowork.features.client.customer.model.Customer;
import com.sni.bokaticowork.features.client.member.model.Member;
import com.sni.bokaticowork.features.client.member.repository.repo.MemberRepository;
import com.sni.bokaticowork.features.company.membership.service.MemberBillingCompanyResolver;
import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletMailer;
import com.sni.bokaticowork.features.inventory.catalog.repository.InventoryItemRepository;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockInRequest;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
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
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Les commandes de la marketplace · du panier au retrait a l'accueil.
 *
 * <p>Validee, une commande fige ses prix, retient le stock de ses articles et emet sa facture, au
 * nom du membre ou de l'organisation qui paie. Elle a {@link #paymentWindow} pour etre reglee :
 * au-dela, et sans aucun reglement, le stock est rendu et la facture annulee. Reglee, le stock sort ;
 * il ne reste qu'a la remettre au comptoir.</p>
 *
 * <p>Une commande payee ne s'annule que par l'administration : un avoir constate le remboursement,
 * reverse au portefeuille du payeur, et ce qui n'a pas ete remis revient en stock.</p>
 */
@Slf4j
@Service
public class MarketplaceOrderService {

    public static final String SOURCE_TYPE = "MARKETPLACE_ORDER";
    private static final String SYSTEM_ACTOR = "SYSTEM_MARKETPLACE";
    private static final String PORTAL_PATH = "/marketplace/orders";

    private final MarketplaceOrderRepository orderRepository;
    private final MarketplaceCatalogService catalog;
    private final MemberRepository memberRepository;
    private final MemberBillingCompanyResolver billingCompanyResolver;
    private final InventoryConsumptionService consumptionService;
    private final StockService stockService;
    private final InventoryItemRepository itemRepository;
    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BillableItemInvoiceSupport invoiceSupport;
    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BillingEmailService billingEmailService;
    private final CompanyWalletMailer mailer;
    private final MemberInAppNotifier inAppNotifier;
    private final Clock clock;
    private final Duration paymentWindow;

    @Autowired
    public MarketplaceOrderService(MarketplaceOrderRepository orderRepository, MarketplaceCatalogService catalog,
                                   MemberRepository memberRepository, MemberBillingCompanyResolver billingCompanyResolver,
                                   InventoryConsumptionService consumptionService, StockService stockService,
                                   InventoryItemRepository itemRepository, BillableItemRepository billableItemRepository,
                                   SequenceGeneratorFacade sequenceGenerator, @Lazy BillableItemInvoiceSupport invoiceSupport,
                                   @Lazy BillingDocumentService billingDocumentService,
                                   BillingDocumentRepository billingDocumentRepository,
                                   @Lazy BillingEmailService billingEmailService, CompanyWalletMailer mailer,
                                   MemberInAppNotifier inAppNotifier,
                                   @Value("${bokati.marketplace.payment-window-minutes:30}") long paymentWindowMinutes) {
        this(orderRepository, catalog, memberRepository, billingCompanyResolver, consumptionService, stockService,
                itemRepository, billableItemRepository, sequenceGenerator, invoiceSupport, billingDocumentService,
                billingDocumentRepository, billingEmailService, mailer, inAppNotifier, Clock.systemDefaultZone(),
                Duration.ofMinutes(paymentWindowMinutes));
    }

    MarketplaceOrderService(MarketplaceOrderRepository orderRepository, MarketplaceCatalogService catalog,
                            MemberRepository memberRepository, MemberBillingCompanyResolver billingCompanyResolver,
                            InventoryConsumptionService consumptionService, StockService stockService,
                            InventoryItemRepository itemRepository, BillableItemRepository billableItemRepository,
                            SequenceGeneratorFacade sequenceGenerator, BillableItemInvoiceSupport invoiceSupport,
                            BillingDocumentService billingDocumentService, BillingDocumentRepository billingDocumentRepository,
                            BillingEmailService billingEmailService, CompanyWalletMailer mailer,
                            MemberInAppNotifier inAppNotifier, Clock clock, Duration paymentWindow) {
        this.orderRepository = orderRepository;
        this.catalog = catalog;
        this.memberRepository = memberRepository;
        this.billingCompanyResolver = billingCompanyResolver;
        this.consumptionService = consumptionService;
        this.stockService = stockService;
        this.itemRepository = itemRepository;
        this.billableItemRepository = billableItemRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.invoiceSupport = invoiceSupport;
        this.billingDocumentService = billingDocumentService;
        this.billingDocumentRepository = billingDocumentRepository;
        this.billingEmailService = billingEmailService;
        this.mailer = mailer;
        this.inAppNotifier = inAppNotifier;
        this.clock = clock;
        this.paymentWindow = paymentWindow;
    }

    // ------------------------------------------------------------------ panier

    @Transactional(readOnly = true)
    public MarketplaceDtos.OrderView cart(Member member) {
        return orderRepository.findCart(member.getMemberId())
                .map(this::view)
                .orElseGet(() -> emptyCart(member));
    }

    /** Ajoute une offre au panier · deja presente, sa quantite s'additionne. */
    @Transactional
    public MarketplaceDtos.OrderView addToCart(Member member, MarketplaceDtos.CartLineRequest request) {
        MarketplaceOffer offer = catalog.offer(request.offerId());
        ensureSellable(offer);
        MarketplaceOrder cart = orderRepository.findCart(member.getMemberId())
                .orElseGet(() -> orderRepository.saveAndFlush(MarketplaceOrder.builder()
                        .buyerMemberCode(member.getMemberId())
                        .status(MarketplaceOrder.Status.CART)
                        .currency(offer.getCurrency())
                        .build()));
        Optional<MarketplaceOrderLine> existing = cart.getLines().stream()
                .filter(line -> line.getOffer().getId().equals(offer.getId()))
                .findFirst();
        int quantity = request.quantity() + existing.map(MarketplaceOrderLine::getQuantity).orElse(0);
        ensureQuantity(offer, quantity);
        if (existing.isPresent()) {
            existing.get().setQuantity(quantity);
        } else {
            cart.getLines().add(MarketplaceOrderLine.builder()
                    .order(cart)
                    .offer(offer)
                    .title(offer.getTitle())
                    .quantity(quantity)
                    .build());
        }
        return view(orderRepository.saveAndFlush(cart));
    }

    @Transactional
    public MarketplaceDtos.OrderView updateCartLine(Member member, Long lineId, int quantity) {
        MarketplaceOrder cart = orderRepository.findCart(member.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Votre panier est vide."));
        MarketplaceOrderLine line = cartLine(cart, lineId);
        if (quantity <= 0) {
            cart.getLines().remove(line);
        } else {
            ensureQuantity(line.getOffer(), quantity);
            line.setQuantity(quantity);
        }
        return view(orderRepository.saveAndFlush(cart));
    }

    @Transactional
    public MarketplaceDtos.OrderView removeCartLine(Member member, Long lineId) {
        return updateCartLine(member, lineId, 0);
    }

    /**
     * Valide le panier · prix figes, stock retenu, facture emise au nom de celui qui paie.
     */
    @Transactional
    public MarketplaceDtos.OrderView checkout(Member member, MarketplaceDtos.CheckoutRequest request) {
        MarketplaceOrder order = orderRepository.findCart(member.getMemberId())
                .orElseThrow(() -> new BadRequestException("Votre panier est vide."));
        if (order.getLines().isEmpty()) {
            throw new BadRequestException("Votre panier est vide.");
        }
        String requested = request == null ? null : request.billingCompanyCode();
        String companyCode = billingCompanyResolver.resolve(SubscriberType.MEMBER.name(), member.getMemberId(), requested)
                .map(Customer::getCustomerId)
                .orElse(null);

        String currency = null;
        BigDecimal total = BigDecimal.ZERO;
        for (MarketplaceOrderLine line : order.getLines()) {
            MarketplaceOffer offer = line.getOffer();
            ensureSellable(offer);
            ensureQuantity(offer, line.getQuantity());
            if (currency == null) {
                currency = offer.getCurrency();
            } else if (!currency.equalsIgnoreCase(offer.getCurrency())) {
                throw new BadRequestException("Votre panier mêle plusieurs devises : validez-le en plusieurs commandes.");
            }
            BigDecimal unitPrice = catalog.priceOf(offer).orElseThrow();
            line.setTitle(offer.getTitle());
            line.setUnitPrice(unitPrice);
            line.setAmount(unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())));
            total = total.add(line.getAmount());
        }

        Instant now = clock.instant();
        String orderNumber = sequenceGenerator.next("marketplace_order");
        order.setOrderNumber(orderNumber);
        order.setBillingCompanyCode(companyCode);
        order.setCurrency(currency);
        order.setTotalAmount(total);

        // Le stock se retient sans echeance : c'est la commande qui expire, et rend alors ce qu'elle
        // retenait. Un reglement partiel arrive pendant le delai garde ainsi son stock.
        for (MarketplaceOrderLine line : order.getLines()) {
            if (line.getOffer().getKind() == MarketplaceOffer.Kind.PRODUCT) {
                line.setStockReservationCode(reserve(orderNumber, member, line));
            }
        }

        List<BillableItem> items = new ArrayList<>();
        for (MarketplaceOrderLine line : order.getLines()) {
            BillableItem item = billableItemRepository.save(BillableItem.builder()
                    .billableNumber(sequenceGenerator.next("billable_item"))
                    .sourceType(SOURCE_TYPE)
                    .sourceId(orderNumber)
                    .subscriberType(SubscriberType.MEMBER)
                    .subscriberCode(member.getMemberId())
                    .subscriberName(member.getDisplayName())
                    .subscriberEmail(member.getEmail())
                    .subscriberPhone(member.getPhone())
                    .billingCompanyCode(companyCode)
                    .description(line.getTitle() + " × " + line.getQuantity())
                    .amount(line.getAmount())
                    .currency(currency)
                    .billingPeriodStart(LocalDate.now(clock))
                    .billingPeriodEnd(LocalDate.now(clock))
                    .status(BillableItemStatus.PENDING)
                    .build());
            line.setBillableNumber(item.getBillableNumber());
            items.add(item);
        }
        BillingDocumentResponse invoice = invoiceSupport.ensureInvoicedGroup(items,
                "Commande marketplace " + orderNumber,
                "Commande " + orderNumber + " · retrait à l'accueil");
        if (invoice == null) {
            throw new BadRequestException("La facture de la commande n'a pas pu être émise.");
        }

        order.setInvoiceNumber(invoice.documentNumber());
        order.setStatus(MarketplaceOrder.Status.PENDING_PAYMENT);
        order.setPlacedAt(now);
        order.setExpiresAt(now.plus(paymentWindow));
        MarketplaceOrder saved = orderRepository.saveAndFlush(order);

        sendInvoiceAfterCommit(invoice.documentNumber());
        tell(member, saved, "MARKETPLACE_ORDER_PLACED", "Commande " + orderNumber + " validée",
                "Votre commande " + orderNumber + " est validée. Réglez la facture " + invoice.documentNumber()
                        + " dans les " + paymentWindow.toMinutes() + " minutes : passé ce délai, elle est annulée et"
                        + " les articles sont remis en vente.");
        return view(saved);
    }

    // ------------------------------------------------------------------ reglement, retrait, expiration

    /**
     * Fait avancer une commande en attente de reglement · reglee, son stock sort ; echue sans
     * aucun reglement, elle expire. Appele apres chaque paiement et a chaque passage periodique.
     *
     * @return la commande a-t-elle change
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sync(String orderNumber) {
        MarketplaceOrder order = orderRepository.lockByOrderNumber(orderNumber).orElse(null);
        if (order == null || order.getStatus() != MarketplaceOrder.Status.PENDING_PAYMENT) {
            return false;
        }
        BillingDocument invoice = invoice(order);
        if (invoice != null && settled(invoice)) {
            markPaid(order);
            return true;
        }
        if (order.getExpiresAt() != null && !order.getExpiresAt().isAfter(clock.instant())) {
            if (invoice != null && paidOn(invoice).signum() > 0) {
                // Un reglement partiel est arrive · la commande attend la suite, rien n'est rendu.
                return false;
            }
            release(order);
            cancelInvoice(order, "Commande " + orderNumber + " non réglée dans le délai");
            order.setStatus(MarketplaceOrder.Status.EXPIRED);
            order.setCancelledAt(clock.instant());
            order.setCancelledBy(SYSTEM_ACTOR);
            order.setCancellationReason("Non réglée dans le délai de " + paymentWindow.toMinutes() + " minutes");
            orderRepository.save(order);
            buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_EXPIRED",
                    "Commande " + orderNumber + " annulée",
                    "Votre commande " + orderNumber + " n'a pas été réglée dans le délai : elle est annulée et sa"
                            + " facture avec elle."));
            return true;
        }
        return false;
    }

    @Transactional(readOnly = true)
    public List<String> pendingNumbers(int limit) {
        return orderRepository.findPendingNumbers(PageRequest.of(0, limit));
    }

    /** Remise au comptoir · une commande reglee seulement. */
    @Transactional
    public MarketplaceDtos.OrderView fulfil(String orderNumber, MarketplaceDtos.FulfilRequest request, String actor) {
        MarketplaceOrder order = locked(orderNumber);
        if (order.getStatus() == MarketplaceOrder.Status.PENDING_PAYMENT) {
            // Le paiement vient peut-etre d'arriver sans que la commande l'ait encore vu.
            BillingDocument invoice = invoice(order);
            if (invoice != null && settled(invoice)) {
                markPaid(order);
            }
        }
        if (order.getStatus() != MarketplaceOrder.Status.PAID) {
            throw new BadRequestException("La commande " + orderNumber + " ne se remet pas : elle est "
                    + label(order.getStatus()) + ".");
        }
        order.setStatus(MarketplaceOrder.Status.FULFILLED);
        order.setFulfilledAt(clock.instant());
        order.setFulfilledBy(actor);
        Member buyer = buyer(order).orElse(null);
        String collectedBy = request == null ? null : request.collectedBy();
        order.setCollectedBy(StringUtils.hasText(collectedBy) ? collectedBy.trim()
                : buyer == null ? null : buyer.getDisplayName());
        MarketplaceOrder saved = orderRepository.save(order);
        if (buyer != null) {
            tell(buyer, saved, "MARKETPLACE_ORDER_FULFILLED", "Commande " + orderNumber + " remise",
                    "Votre commande " + orderNumber + " a été remise à l'accueil"
                            + (saved.getCollectedBy() == null ? "." : " à " + saved.getCollectedBy() + "."));
        }
        return view(saved);
    }

    /** Le membre renonce a une commande qu'il n'a pas encore reglee. */
    @Transactional
    public MarketplaceDtos.OrderView cancelByMember(Member member, String orderNumber, String reason) {
        MarketplaceOrder order = locked(orderNumber);
        if (!member.getMemberId().equalsIgnoreCase(order.getBuyerMemberCode())) {
            throw new ResourceNotFoundException("Commande introuvable : " + orderNumber);
        }
        if (order.getStatus() != MarketplaceOrder.Status.PENDING_PAYMENT) {
            throw new BadRequestException("La commande " + orderNumber + " ne s'annule plus depuis le portail : elle est "
                    + label(order.getStatus()) + ". Adressez-vous à l'accueil.");
        }
        BillingDocument invoice = invoice(order);
        if (invoice != null && paidOn(invoice).signum() > 0) {
            throw new BadRequestException("Un règlement a déjà été reçu pour la commande " + orderNumber
                    + " : seul l'accueil peut l'annuler.");
        }
        String motive = StringUtils.hasText(reason) ? reason.trim() : "Annulée par le membre";
        cancelUnpaid(order, motive, member.getMemberId());
        return view(order);
    }

    /**
     * L'administration annule · non reglee, la facture est annulee ; reglee, un avoir rembourse le
     * payeur sur son portefeuille. Ce qui n'a pas ete remis revient en stock, ce qui l'a ete sur
     * demande seulement.
     */
    @Transactional
    public MarketplaceDtos.OrderView cancelByStaff(String orderNumber, MarketplaceDtos.CancelRequest request, String actor) {
        MarketplaceOrder order = locked(orderNumber);
        String reason = request.reason().trim();
        switch (order.getStatus()) {
            case PENDING_PAYMENT -> {
                BillingDocument invoice = invoice(order);
                if (invoice != null && settled(invoice)) {
                    markPaid(order);
                    refundAndCancel(order, reason, actor, true);
                } else if (invoice != null && paidOn(invoice).signum() > 0) {
                    throw new BadRequestException("La commande " + orderNumber + " est partiellement réglée : "
                            + "remboursez d'abord le paiement reçu, puis annulez-la.");
                } else {
                    cancelUnpaid(order, reason, actor);
                }
            }
            case PAID -> refundAndCancel(order, reason, actor, true);
            case FULFILLED -> refundAndCancel(order, reason, actor, Boolean.TRUE.equals(request.returnToStock()));
            default -> throw new BadRequestException("La commande " + orderNumber + " ne s'annule pas : elle est "
                    + label(order.getStatus()) + ".");
        }
        return view(order);
    }

    // ------------------------------------------------------------------ lectures

    @Transactional(readOnly = true)
    public List<MarketplaceDtos.OrderView> ordersOf(Member member, int limit) {
        return orderRepository.findOfMember(member.getMemberId(), PageRequest.of(0, limit)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceDtos.OrderView orderOf(Member member, String orderNumber) {
        MarketplaceOrder order = orderRepository.findByOrderNumber(orderNumber)
                .filter(found -> member.getMemberId().equalsIgnoreCase(found.getBuyerMemberCode()))
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderNumber));
        return view(order);
    }

    @Transactional(readOnly = true)
    public List<MarketplaceDtos.OrderView> ordersOfCompany(Customer company, int limit) {
        return orderRepository.findOfCompany(company.getCustomerId(), PageRequest.of(0, limit)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<MarketplaceDtos.OrderView> search(MarketplaceOrder.Status status, int limit) {
        return orderRepository.search(status, PageRequest.of(0, limit)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceDtos.OrderView get(String orderNumber) {
        return view(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderNumber)));
    }

    // ------------------------------------------------------------------ interne

    private void markPaid(MarketplaceOrder order) {
        for (MarketplaceOrderLine line : order.getLines()) {
            if (!StringUtils.hasText(line.getStockReservationCode())) {
                continue;
            }
            try {
                consumptionService.consumeBusinessReservation(line.getStockReservationCode(), SYSTEM_ACTOR);
            } catch (RuntimeException ex) {
                // Le client a paye · la commande avance, le stock se regularise a la main.
                log.warn("Commande {} · sortie de stock de {} impossible, a regulariser : {}",
                        order.getOrderNumber(), line.getStockReservationCode(), ex.getMessage());
            }
        }
        order.setStatus(MarketplaceOrder.Status.PAID);
        order.setPaidAt(clock.instant());
        order.setExpiresAt(null);
        orderRepository.save(order);
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_PAID",
                "Commande " + order.getOrderNumber() + " réglée",
                "Votre commande " + order.getOrderNumber() + " est réglée. Elle vous attend à l'accueil."));
    }

    private void cancelUnpaid(MarketplaceOrder order, String reason, String actor) {
        release(order);
        cancelInvoice(order, "Annulation commande " + order.getOrderNumber() + " · " + reason);
        close(order, reason, actor);
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_CANCELLED",
                "Commande " + order.getOrderNumber() + " annulée",
                "Votre commande " + order.getOrderNumber() + " est annulée, sa facture avec elle."));
    }

    private void refundAndCancel(MarketplaceOrder order, String reason, String actor, boolean returnToStock) {
        BillingDocument invoice = invoice(order);
        BigDecimal paid = invoice == null ? BigDecimal.ZERO : paidOn(invoice);
        if (paid.signum() > 0) {
            String motive = "Annulation commande " + order.getOrderNumber() + " · " + reason;
            BillingDocumentResponse creditNote = billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                    new CreateCreditNoteRequest(paid, motive, Boolean.FALSE, Boolean.TRUE, null));
            billingDocumentService.refundCreditNoteToWallet(creditNote.documentNumber(), motive);
            order.setCreditNoteNumber(creditNote.documentNumber());
        }
        if (returnToStock) {
            restock(order, actor);
        }
        close(order, reason, actor);
        String paidText = paid.stripTrailingZeros().toPlainString() + " " + order.getCurrency();
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_REFUNDED",
                "Commande " + order.getOrderNumber() + " annulée et remboursée",
                "Votre commande " + order.getOrderNumber() + " est annulée. "
                        + (paid.signum() > 0 ? paidText + " sont reversés au portefeuille "
                        + (order.getBillingCompanyCode() == null ? "de votre compte." : "de l'organisation qui a payé.")
                        : "")));
    }

    private void close(MarketplaceOrder order, String reason, String actor) {
        order.setStatus(MarketplaceOrder.Status.CANCELLED);
        order.setCancelledAt(clock.instant());
        order.setCancelledBy(actor);
        order.setCancellationReason(reason);
        order.setExpiresAt(null);
        orderRepository.save(order);
    }

    private String reserve(String orderNumber, Member member, MarketplaceOrderLine line) {
        MarketplaceOffer offer = line.getOffer();
        StockReservationRequest reservation = new StockReservationRequest();
        reservation.setItemCode(offer.getInventoryItemCode());
        reservation.setLocationCode(offer.getLocationCode());
        reservation.setQuantity(BigDecimal.valueOf(line.getQuantity()));
        reservation.setReferenceType(StockReferenceType.POS_SALE);
        reservation.setReferenceCode(orderNumber);
        reservation.setReservedBy(member.getMemberId());
        return consumptionService.reserveForBusinessEvent(reservation).getReservationCode();
    }

    private void release(MarketplaceOrder order) {
        for (MarketplaceOrderLine line : order.getLines()) {
            if (!StringUtils.hasText(line.getStockReservationCode())) {
                continue;
            }
            try {
                consumptionService.releaseBusinessReservation(line.getStockReservationCode());
            } catch (RuntimeException ex) {
                log.warn("Commande {} · retenue de stock {} deja levee : {}", order.getOrderNumber(),
                        line.getStockReservationCode(), ex.getMessage());
            }
        }
    }

    /** Ce qui est deja sorti revient a l'emplacement d'ou il etait parti. */
    private void restock(MarketplaceOrder order, String actor) {
        for (MarketplaceOrderLine line : order.getLines()) {
            MarketplaceOffer offer = line.getOffer();
            if (offer.getKind() != MarketplaceOffer.Kind.PRODUCT || !StringUtils.hasText(line.getStockReservationCode())) {
                continue;
            }
            StockInRequest back = new StockInRequest();
            back.setItemCode(offer.getInventoryItemCode());
            back.setLocationCode(offer.getLocationCode());
            back.setQuantity(BigDecimal.valueOf(line.getQuantity()));
            back.setUnitCost(itemRepository.findByItemCode(offer.getInventoryItemCode())
                    .map(item -> item.getDefaultCost()).orElse(null));
            back.setReferenceType(StockReferenceType.POS_SALE);
            back.setReferenceCode(order.getOrderNumber());
            back.setReason("Retour · annulation de la commande " + order.getOrderNumber());
            back.setPerformedBy(actor);
            stockService.receive(back);
        }
    }

    /** La facture n'existe pour un autre fil qu'une fois la transaction validee. */
    private void sendInvoiceAfterCommit(String documentNumber) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            billingEmailService.sendDocumentAsync(documentNumber);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                billingEmailService.sendDocumentAsync(documentNumber);
            }
        });
    }

    private void cancelInvoice(MarketplaceOrder order, String reason) {
        if (!StringUtils.hasText(order.getInvoiceNumber())) {
            return;
        }
        billingDocumentService.cancelAndArchive(order.getInvoiceNumber(), reason);
    }

    private BillingDocument invoice(MarketplaceOrder order) {
        if (!StringUtils.hasText(order.getInvoiceNumber())) {
            return null;
        }
        return billingDocumentRepository.findByDocumentNumber(order.getInvoiceNumber()).orElse(null);
    }

    private static boolean settled(BillingDocument invoice) {
        return paidOn(invoice).signum() > 0
                && (invoice.getBalanceDue() == null || invoice.getBalanceDue().signum() <= 0);
    }

    private static BigDecimal paidOn(BillingDocument invoice) {
        return invoice.getPaidAmount() == null ? BigDecimal.ZERO : invoice.getPaidAmount();
    }

    private MarketplaceOrder locked(String orderNumber) {
        return orderRepository.lockByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderNumber));
    }

    private static MarketplaceOrderLine cartLine(MarketplaceOrder cart, Long lineId) {
        return cart.getLines().stream()
                .filter(line -> lineId.equals(line.getShortId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Ligne introuvable dans votre panier : " + lineId));
    }

    private void ensureSellable(MarketplaceOffer offer) {
        if (!catalog.sellable(offer)) {
            throw new BadRequestException("« " + offer.getTitle() + " » n'est plus en vente.");
        }
    }

    private void ensureQuantity(MarketplaceOffer offer, int quantity) {
        if (offer.getMaxPerOrder() != null && quantity > offer.getMaxPerOrder()) {
            throw new BadRequestException("« " + offer.getTitle() + " » se commande par " + offer.getMaxPerOrder()
                    + " au plus.");
        }
        BigDecimal available = catalog.availableOf(offer);
        if (available != null && available.compareTo(BigDecimal.valueOf(quantity)) < 0) {
            throw new BadRequestException("Il ne reste que " + available.stripTrailingZeros().toPlainString()
                    + " « " + offer.getTitle() + " » en stock.");
        }
    }

    private Optional<Member> buyer(MarketplaceOrder order) {
        return memberRepository.findByMemberIdAndDeletedFalse(order.getBuyerMemberCode());
    }

    private void tell(Member member, MarketplaceOrder order, String eventType, String subject, String message) {
        mailer.notify(member, subject, message, order.getOrderNumber(), PORTAL_PATH);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderNumber", order.getOrderNumber());
        payload.put("status", order.getStatus().name());
        payload.put("invoiceNumber", order.getInvoiceNumber());
        payload.put("message", message);
        inAppNotifier.notify(eventType, SOURCE_TYPE, order.getOrderNumber(), member.getEmail(), member.getDisplayName(),
                member.getMemberId(), subject, payload);
    }

    private static String label(MarketplaceOrder.Status status) {
        return switch (status) {
            case CART -> "encore au panier";
            case PENDING_PAYMENT -> "en attente de règlement";
            case PAID -> "réglée";
            case FULFILLED -> "déjà remise";
            case CANCELLED -> "annulée";
            case EXPIRED -> "expirée";
        };
    }

    private MarketplaceDtos.OrderView emptyCart(Member member) {
        return new MarketplaceDtos.OrderView(null, null, MarketplaceOrder.Status.CART, member.getMemberId(), null, "XAF",
                BigDecimal.ZERO, null, null, null, null, null, null, null, null, null, null, null, null, List.of());
    }

    MarketplaceDtos.OrderView view(MarketplaceOrder order) {
        boolean cart = order.getStatus() == MarketplaceOrder.Status.CART;
        BigDecimal total = BigDecimal.ZERO;
        List<MarketplaceDtos.LineView> lines = new ArrayList<>();
        for (MarketplaceOrderLine line : order.getLines()) {
            MarketplaceOffer offer = line.getOffer();
            // Au panier, le prix suit l'offre ; valide, il est fige sur la ligne.
            BigDecimal unitPrice = cart ? catalog.priceOf(offer).orElse(null) : line.getUnitPrice();
            BigDecimal amount = cart
                    ? (unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(line.getQuantity())))
                    : line.getAmount();
            if (amount != null) {
                total = total.add(amount);
            }
            lines.add(new MarketplaceDtos.LineView(line.getShortId(), offer.getShortId(), offer.getKind(),
                    cart ? offer.getTitle() : line.getTitle(), offer.getImageUrl(), line.getQuantity(), unitPrice, amount,
                    cart ? catalog.availableOf(offer) : null));
        }
        BigDecimal balanceDue = null;
        if (!cart) {
            balanceDue = Optional.ofNullable(invoice(order)).map(BillingDocument::getBalanceDue).orElse(null);
        }
        return new MarketplaceDtos.OrderView(order.getShortId(), order.getOrderNumber(), order.getStatus(),
                order.getBuyerMemberCode(), order.getBillingCompanyCode(), order.getCurrency(),
                cart ? total : order.getTotalAmount(), order.getInvoiceNumber(), balanceDue, order.getExpiresAt(),
                order.getPlacedAt(), order.getPaidAt(), order.getFulfilledAt(), order.getFulfilledBy(),
                order.getCollectedBy(), order.getCancelledAt(), order.getCancelledBy(), order.getCancellationReason(),
                order.getCreditNoteNumber(), lines);
    }
}
