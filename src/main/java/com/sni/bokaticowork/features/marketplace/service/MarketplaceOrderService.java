package com.sni.bokaticowork.features.marketplace.service;

import com.sni.bokaticowork.core.exception.customs.BadRequestException;
import com.sni.bokaticowork.core.exception.customs.ResourceNotFoundException;
import com.sni.bokaticowork.core.generator.sequenceEngine.service.interfaces.SequenceGeneratorFacade;
import com.sni.bokaticowork.core.settings.platform.PlatformSettings;
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
import com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository;
import com.sni.bokaticowork.features.company.wallet.service.CompanyWalletMailer;
import com.sni.bokaticowork.features.inventory.stock.dto.request.StockReservationRequest;
import com.sni.bokaticowork.features.inventory.stock.enums.StockReferenceType;
import com.sni.bokaticowork.features.inventory.stock.service.interfaces.InventoryConsumptionService;
import com.sni.bokaticowork.features.marketplace.dto.MarketplaceDtos;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOffer;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrderLine;
import com.sni.bokaticowork.features.marketplace.repository.MarketplaceOrderRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.features.portal.notification.service.MemberInAppNotifier;
import com.sni.bokaticowork.features.subscription.repository.BillableItemRepository;
import com.sni.bokaticowork.features.subscription.subscription.enums.BillableItemStatus;
import com.sni.bokaticowork.features.subscription.subscription.enums.SubscriberType;
import com.sni.bokaticowork.features.subscription.subscription.model.BillableItem;
import com.sni.bokaticowork.security.admin.role.repository.RoleUserRepository;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Les commandes de la marketplace · du panier au retrait a l'accueil.
 *
 * <p>Validee, une commande fige ses prix, retient le stock de ses articles et emet sa facture, pour
 * le membre ou l'organisation qui paie. Elle a {@link #paymentWindow} pour etre soldee : au-dela, ce
 * qui a deja ete regle est rembourse, le stock est rendu et la facture annulee. Un paiement en cours
 * ou une depense en attente du gerant suspend l'echeance, le temps d'etre tranche. Reglee, le stock
 * sort ; il ne reste qu'a la remettre au comptoir.</p>
 *
 * <p>Une sortie de stock qui echoue ne remet pas le reglement en cause : la commande reste reglee,
 * l'administration est prevenue, la sortie est reessayee a chaque passage, et la commande ne se
 * remet pas tant qu'elle n'a pas abouti.</p>
 *
 * <p>Une commande reglee, meme en partie, ne s'annule que par l'administration : un avoir constate
 * le remboursement, reverse au portefeuille du payeur, et ce qui n'a pas ete remis revient en
 * stock.</p>
 */
@Slf4j
@Service
public class MarketplaceOrderService {

    public static final String SOURCE_TYPE = "MARKETPLACE_ORDER";
    private static final String PAYMENT_WINDOW_KEY = "bokati.marketplace.payment-window-minutes";
    private static final String SYSTEM_ACTOR = "SYSTEM_MARKETPLACE";
    private static final String PORTAL_PATH = "/marketplace/orders";
    private static final String COMPANY_WALLET_OWNER = "CUSTOMER";
    private static final List<String> ALERTED_ROLES = List.of("SUPER_ADMIN", "ADMIN", "MANAGER");
    /** Au-dela, un paiement reste sans reponse de l'operateur · il ne retient plus la commande. */
    private static final Duration IN_FLIGHT_WINDOW = Duration.ofHours(1);
    private static final int STOCK_ERROR_LENGTH = 500;

    private final MarketplaceOrderRepository orderRepository;
    private final MarketplaceCatalogService catalog;
    private final MemberRepository memberRepository;
    private final MemberBillingCompanyResolver billingCompanyResolver;
    private final InventoryConsumptionService consumptionService;
    private final MarketplaceStockGateway stockGateway;
    private final BillableItemRepository billableItemRepository;
    private final SequenceGeneratorFacade sequenceGenerator;
    private final BillableItemInvoiceSupport invoiceSupport;
    private final BillingDocumentService billingDocumentService;
    private final BillingDocumentRepository billingDocumentRepository;
    private final BillingEmailService billingEmailService;
    private final CompanyWalletSpendRepository spendRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final RoleUserRepository roleUserRepository;
    private final CompanyWalletMailer mailer;
    private final MemberInAppNotifier inAppNotifier;
    private final Clock clock;
    private final PlatformSettings settings;
    private final Duration paymentWindow;

    @Autowired
    public MarketplaceOrderService(MarketplaceOrderRepository orderRepository, MarketplaceCatalogService catalog,
                                   MemberRepository memberRepository, MemberBillingCompanyResolver billingCompanyResolver,
                                   InventoryConsumptionService consumptionService, MarketplaceStockGateway stockGateway,
                                   BillableItemRepository billableItemRepository,
                                   SequenceGeneratorFacade sequenceGenerator, @Lazy BillableItemInvoiceSupport invoiceSupport,
                                   @Lazy BillingDocumentService billingDocumentService,
                                   BillingDocumentRepository billingDocumentRepository,
                                   @Lazy BillingEmailService billingEmailService,
                                   CompanyWalletSpendRepository spendRepository,
                                   PaymentTransactionRepository transactionRepository,
                                   RoleUserRepository roleUserRepository, CompanyWalletMailer mailer,
                                   MemberInAppNotifier inAppNotifier, PlatformSettings settings,
                                   @Value("${" + PAYMENT_WINDOW_KEY + ":30}") long paymentWindowMinutes) {
        this(orderRepository, catalog, memberRepository, billingCompanyResolver, consumptionService, stockGateway,
                billableItemRepository, sequenceGenerator, invoiceSupport, billingDocumentService,
                billingDocumentRepository, billingEmailService, spendRepository, transactionRepository,
                roleUserRepository, mailer, inAppNotifier, settings, Clock.systemDefaultZone(),
                Duration.ofMinutes(paymentWindowMinutes));
    }

    MarketplaceOrderService(MarketplaceOrderRepository orderRepository, MarketplaceCatalogService catalog,
                            MemberRepository memberRepository, MemberBillingCompanyResolver billingCompanyResolver,
                            InventoryConsumptionService consumptionService, MarketplaceStockGateway stockGateway,
                            BillableItemRepository billableItemRepository,
                            SequenceGeneratorFacade sequenceGenerator, BillableItemInvoiceSupport invoiceSupport,
                            BillingDocumentService billingDocumentService, BillingDocumentRepository billingDocumentRepository,
                            BillingEmailService billingEmailService, CompanyWalletSpendRepository spendRepository,
                            PaymentTransactionRepository transactionRepository, RoleUserRepository roleUserRepository,
                            CompanyWalletMailer mailer, MemberInAppNotifier inAppNotifier, PlatformSettings settings,
                            Clock clock, Duration paymentWindow) {
        this.orderRepository = orderRepository;
        this.catalog = catalog;
        this.memberRepository = memberRepository;
        this.billingCompanyResolver = billingCompanyResolver;
        this.consumptionService = consumptionService;
        this.stockGateway = stockGateway;
        this.billableItemRepository = billableItemRepository;
        this.sequenceGenerator = sequenceGenerator;
        this.invoiceSupport = invoiceSupport;
        this.billingDocumentService = billingDocumentService;
        this.billingDocumentRepository = billingDocumentRepository;
        this.billingEmailService = billingEmailService;
        this.spendRepository = spendRepository;
        this.transactionRepository = transactionRepository;
        this.roleUserRepository = roleUserRepository;
        this.mailer = mailer;
        this.inAppNotifier = inAppNotifier;
        this.clock = clock;
        this.settings = settings;
        this.paymentWindow = paymentWindow;
    }

    /** Le delai en vigueur · un changement dans les parametres vaut pour les commandes suivantes. */
    private Duration paymentWindow() {
        return settings == null ? paymentWindow
                : Duration.ofMinutes(settings.number(PAYMENT_WINDOW_KEY, paymentWindow.toMinutes()));
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
        // retenait. Un paiement qui suspend l'echeance garde ainsi son stock.
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
        order.setExpiresAt(now.plus(paymentWindow()));
        MarketplaceOrder saved = orderRepository.saveAndFlush(order);

        sendInvoiceAfterCommit(invoice.documentNumber());
        tell(member, saved, "MARKETPLACE_ORDER_PLACED", "Commande " + orderNumber + " validée",
                "Votre commande " + orderNumber + " est validée. Réglez la facture " + invoice.documentNumber()
                        + " dans les " + paymentWindow().toMinutes() + " minutes : passé ce délai, elle est annulée et"
                        + " les articles sont remis en vente.");
        return view(saved);
    }

    // ------------------------------------------------------------------ reglement, retrait, expiration

    /**
     * Fait avancer une commande · soldee, son stock sort ; echue, elle expire ; reglee, la sortie de
     * stock restee en echec est reessayee. Appele apres chaque paiement et a chaque passage periodique.
     *
     * @return la commande a-t-elle change
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean sync(String orderNumber) {
        MarketplaceOrder order = orderRepository.lockByOrderNumber(orderNumber).orElse(null);
        if (order == null) {
            return false;
        }
        if (order.getStatus() == MarketplaceOrder.Status.PAID) {
            return retryStock(order);
        }
        if (order.getStatus() != MarketplaceOrder.Status.PENDING_PAYMENT) {
            return false;
        }
        BillingDocument invoice = invoice(order);
        if (invoice != null && settled(invoice)) {
            markPaid(order);
            return true;
        }
        if (order.getExpiresAt() == null || order.getExpiresAt().isAfter(clock.instant()) || paymentUnderway(order)) {
            return false;
        }
        expire(order, invoice);
        return true;
    }

    @Transactional(readOnly = true)
    public List<String> numbersToSync(int limit) {
        List<String> numbers = new ArrayList<>(orderRepository.findPendingNumbers(PageRequest.of(0, limit)));
        numbers.addAll(orderRepository.findStockPendingNumbers(PageRequest.of(0, limit)));
        return numbers;
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
        issueStock(order);
        if (stockPending(order)) {
            throw new BadRequestException("La commande " + orderNumber + " ne se remet pas encore : la sortie de stock de "
                    + String.join(", ", pendingTitles(order)) + " n'a pas abouti. Corrigez le stock de l'emplacement,"
                    + " puis recommencez.");
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
     * L'administration annule · sans reglement, la facture est annulee ; avec, un avoir rembourse le
     * payeur sur son portefeuille, et une facture restee en partie due est annulee aussi. Ce qui n'a
     * pas ete remis revient en stock, ce qui l'a ete sur demande seulement.
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
                    refundAndCancel(order, reason, actor, true);
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

    /** Les commandes reglees qui attendent une correction du stock. */
    @Transactional(readOnly = true)
    public List<MarketplaceDtos.OrderView> stockPending(int limit) {
        return orderRepository.findStockPending(PageRequest.of(0, limit)).stream().map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public MarketplaceDtos.OrderView get(String orderNumber) {
        return view(orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Commande introuvable : " + orderNumber)));
    }

    // ------------------------------------------------------------------ interne

    private void markPaid(MarketplaceOrder order) {
        order.setStatus(MarketplaceOrder.Status.PAID);
        order.setPaidAt(clock.instant());
        order.setExpiresAt(null);
        issueStock(order);
        orderRepository.save(order);
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_PAID",
                "Commande " + order.getOrderNumber() + " réglée",
                "Votre commande " + order.getOrderNumber() + " est réglée. Elle vous attend à l'accueil."));
    }

    /** Sort du stock ce qui doit l'etre · la premiere fois qu'une ligne echoue, l'equipe est prevenue. */
    private void issueStock(MarketplaceOrder order) {
        List<String> newFailures = new ArrayList<>();
        for (MarketplaceOrderLine line : order.getLines()) {
            if (!toIssue(line)) {
                continue;
            }
            try {
                line.setStockReservationCode(stockGateway.issue(stockLine(order, line, SYSTEM_ACTOR)));
                line.setStockConsumedAt(clock.instant());
                line.setStockError(null);
            } catch (RuntimeException ex) {
                log.warn("Commande {} · sortie de stock de {} impossible, reessayee au prochain passage : {}",
                        order.getOrderNumber(), line.getStockReservationCode(), ex.getMessage());
                if (line.getStockError() == null) {
                    newFailures.add(line.getTitle());
                }
                line.setStockError(stockError(ex));
            }
        }
        if (!newFailures.isEmpty()) {
            alertStaff(order, "Commande " + order.getOrderNumber() + " : sortie de stock à régulariser",
                    "La commande " + order.getOrderNumber() + " est réglée, mais la sortie de stock de "
                            + String.join(", ", newFailures) + " n'a pas abouti. Corrigez le stock de l'emplacement :"
                            + " la sortie est reprise automatiquement, et la commande ne peut pas être remise avant.");
        }
    }

    private boolean retryStock(MarketplaceOrder order) {
        if (!stockPending(order)) {
            return false;
        }
        issueStock(order);
        orderRepository.save(order);
        return !stockPending(order);
    }

    /** Un paiement lance ou une depense soumise au gerant se tranche avant que la commande n'expire. */
    private boolean paymentUnderway(MarketplaceOrder order) {
        String invoiceNumber = order.getInvoiceNumber();
        return StringUtils.hasText(invoiceNumber)
                && (spendRepository.existsPendingForDocument(invoiceNumber)
                || transactionRepository.existsInFlightForDocument(invoiceNumber, clock.instant().minus(IN_FLIGHT_WINDOW)));
    }

    private void expire(MarketplaceOrder order, BillingDocument invoice) {
        String orderNumber = order.getOrderNumber();
        String motive = "Commande " + orderNumber + " non réglée dans le délai";
        BigDecimal refunded = refundPaid(order, invoice, motive);
        cancelInvoice(order, motive);
        order.setStatus(MarketplaceOrder.Status.EXPIRED);
        order.setCancelledAt(clock.instant());
        order.setCancelledBy(SYSTEM_ACTOR);
        order.setCancellationReason("Non réglée dans le délai imparti");
        orderRepository.save(order);
        giveBack(order, SYSTEM_ACTOR);
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_EXPIRED",
                "Commande " + orderNumber + " annulée",
                "Votre commande " + orderNumber + " n'a pas été soldée dans le délai : elle est annulée et sa"
                        + " facture avec elle." + refundSentence(order, refunded)));
    }

    private void cancelUnpaid(MarketplaceOrder order, String reason, String actor) {
        cancelInvoice(order, "Annulation commande " + order.getOrderNumber() + " · " + reason);
        close(order, reason, actor);
        giveBack(order, actor);
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_CANCELLED",
                "Commande " + order.getOrderNumber() + " annulée",
                "Votre commande " + order.getOrderNumber() + " est annulée, sa facture avec elle."));
    }

    private void refundAndCancel(MarketplaceOrder order, String reason, String actor, boolean returnToStock) {
        BillingDocument invoice = invoice(order);
        boolean balanceLeft = invoice != null && !settled(invoice);
        String motive = "Annulation commande " + order.getOrderNumber() + " · " + reason;
        BigDecimal refunded = refundPaid(order, invoice, motive);
        if (balanceLeft) {
            // L'avoir couvre ce qui a ete regle · il reste a annuler ce qui n'a jamais ete paye.
            cancelInvoice(order, motive);
        }
        close(order, reason, actor);
        if (returnToStock) {
            giveBack(order, actor);
        }
        buyer(order).ifPresent(member -> tell(member, order, "MARKETPLACE_ORDER_REFUNDED",
                "Commande " + order.getOrderNumber() + " annulée et remboursée",
                "Votre commande " + order.getOrderNumber() + " est annulée." + refundSentence(order, refunded)));
    }

    /** Constate par un avoir ce qui a ete regle, et le reverse au portefeuille du payeur. */
    private BigDecimal refundPaid(MarketplaceOrder order, BillingDocument invoice, String motive) {
        BigDecimal paid = invoice == null ? BigDecimal.ZERO : paidOn(invoice);
        if (paid.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BillingDocumentResponse creditNote = billingDocumentService.createCreditNote(invoice.getDocumentNumber(),
                new CreateCreditNoteRequest(paid, motive, Boolean.FALSE, Boolean.TRUE, null));
        // La facture reste au nom du membre, reliee a l'organisation : l'avoir aussi. C'est donc ici
        // que se decide le portefeuille, pour que l'argent de l'organisation lui revienne.
        boolean company = StringUtils.hasText(order.getBillingCompanyCode());
        billingDocumentService.refundCreditNoteToWallet(creditNote.documentNumber(), motive,
                company ? COMPANY_WALLET_OWNER : null, company ? order.getBillingCompanyCode() : null);
        order.setCreditNoteNumber(creditNote.documentNumber());
        return paid;
    }

    private static String refundSentence(MarketplaceOrder order, BigDecimal refunded) {
        if (refunded.signum() <= 0) {
            return "";
        }
        return " " + refunded.stripTrailingZeros().toPlainString() + " " + order.getCurrency()
                + " sont reversés au portefeuille "
                + (StringUtils.hasText(order.getBillingCompanyCode()) ? "de l'organisation." : "de votre compte.");
    }

    private void close(MarketplaceOrder order, String reason, String actor) {
        order.setStatus(MarketplaceOrder.Status.CANCELLED);
        order.setCancelledAt(clock.instant());
        order.setCancelledBy(actor);
        order.setCancellationReason(reason);
        order.setExpiresAt(null);
        orderRepository.save(order);
    }

    /**
     * Rend le stock, une fois tout le reste fait · un retour impossible (article suivi par lot ou
     * numero de serie, periode close) n'empeche pas le remboursement : l'equipe le saisit a la main.
     */
    private void giveBack(MarketplaceOrder order, String actor) {
        List<String> failures = new ArrayList<>();
        for (MarketplaceOrderLine line : order.getLines()) {
            if (!StringUtils.hasText(line.getStockReservationCode())) {
                continue;
            }
            try {
                stockGateway.giveBack(stockLine(order, line, actor));
            } catch (RuntimeException ex) {
                log.warn("Commande {} · retour en stock de {} impossible : {}", order.getOrderNumber(),
                        line.getStockReservationCode(), ex.getMessage());
                line.setStockError(stockError(ex));
                failures.add(line.getTitle());
            }
        }
        if (!failures.isEmpty()) {
            orderRepository.save(order);
            alertStaff(order, "Commande " + order.getOrderNumber() + " : retour en stock à saisir",
                    "La commande " + order.getOrderNumber() + " est annulée, mais " + String.join(", ", failures)
                            + " n'a pas pu revenir en stock automatiquement. Saisissez le retour à la main.");
        }
    }

    private static MarketplaceStockGateway.StockLine stockLine(MarketplaceOrder order, MarketplaceOrderLine line, String actor) {
        MarketplaceOffer offer = line.getOffer();
        return new MarketplaceStockGateway.StockLine(line.getStockReservationCode(), offer.getInventoryItemCode(),
                offer.getLocationCode(), BigDecimal.valueOf(line.getQuantity()), order.getOrderNumber(), actor);
    }

    private static boolean toIssue(MarketplaceOrderLine line) {
        return StringUtils.hasText(line.getStockReservationCode()) && line.getStockConsumedAt() == null;
    }

    private static boolean stockPending(MarketplaceOrder order) {
        return order.getLines().stream().anyMatch(MarketplaceOrderService::toIssue);
    }

    private static List<String> pendingTitles(MarketplaceOrder order) {
        return order.getLines().stream().filter(MarketplaceOrderService::toIssue).map(MarketplaceOrderLine::getTitle).toList();
    }

    private static String stockError(RuntimeException ex) {
        String message = String.valueOf(ex.getMessage());
        return message.length() <= STOCK_ERROR_LENGTH ? message : message.substring(0, STOCK_ERROR_LENGTH);
    }

    private void alertStaff(MarketplaceOrder order, String subject, String message) {
        Set<String> recipients = new LinkedHashSet<>();
        for (String role : ALERTED_ROLES) {
            roleUserRepository.findEnabledHolderEmails(role).stream()
                    .filter(StringUtils::hasText)
                    .map(email -> email.trim().toLowerCase(Locale.ROOT))
                    .forEach(recipients::add);
        }
        recipients.forEach(email -> mailer.notifyAddress(email, "Équipe", subject, message, order.getOrderNumber()));
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
                BigDecimal.ZERO, null, null, null, null, null, null, null, null, null, null, null, null, false, List.of());
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
                    cart ? catalog.availableOf(offer) : null, line.getStockError()));
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
                order.getCreditNoteNumber(), order.getStatus() == MarketplaceOrder.Status.PAID && stockPending(order), lines);
    }
}
