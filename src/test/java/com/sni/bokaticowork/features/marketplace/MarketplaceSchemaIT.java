package com.sni.bokaticowork.features.marketplace;

import com.sni.bokaticowork.features.company.wallet.repository.CompanyWalletSpendRepository;
import com.sni.bokaticowork.features.marketplace.model.MarketplaceOrder;
import com.sni.bokaticowork.features.marketplace.repository.MarketplaceOfferRepository;
import com.sni.bokaticowork.features.marketplace.repository.MarketplaceOrderRepository;
import com.sni.bokaticowork.features.payment.repository.PaymentTransactionRepository;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La marketplace tient sur le schema de production · colonnes, index et requetes.
 */
@org.springframework.test.context.TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class MarketplaceSchemaIT extends PostgresIntegrationTestBase {

    @Autowired private DataSource dataSource;
    @Autowired private MarketplaceOfferRepository offerRepository;
    @Autowired private MarketplaceOrderRepository orderRepository;
    @Autowired private PaymentTransactionRepository transactionRepository;
    @Autowired private CompanyWalletSpendRepository spendRepository;

    @Test
    @DisplayName("Sequences semees, un seul panier par membre, sorties de stock en attente indexees")
    void schema() {
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM sequence_definition WHERE code IN ('marketplace_offer', 'marketplace_order')", Integer.class))
                .isEqualTo(2);
        List<String> orderIndexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'marketplace_order'", String.class);
        assertThat(orderIndexes).contains("uk_marketplace_order_cart", "uk_marketplace_order_number");
        List<String> lineIndexes = jdbc.queryForList(
                "SELECT indexname FROM pg_indexes WHERE tablename = 'marketplace_order_line'", String.class);
        assertThat(lineIndexes).contains("idx_marketplace_order_line_stock_pending");
    }

    @Test
    @DisplayName("Les requetes du catalogue, des commandes et du suivi des paiements s'executent")
    void queriesRun() {
        PageRequest page = PageRequest.of(0, 5);
        assertThat(offerRepository.search(Boolean.TRUE, "boissons")).isEmpty();
        assertThat(offerRepository.search(null, null)).isEmpty();
        assertThat(orderRepository.findCart("MBR-NONE")).isEmpty();
        assertThat(orderRepository.findOfMember("MBR-NONE", page)).isEmpty();
        assertThat(orderRepository.findOfCompany("CUS-NONE", page)).isEmpty();
        assertThat(orderRepository.search(null, page)).isEmpty();
        assertThat(orderRepository.search(MarketplaceOrder.Status.PAID, page)).isEmpty();
        assertThat(orderRepository.findPendingNumbers(page)).isEmpty();
        assertThat(orderRepository.findStockPendingNumbers(page)).isEmpty();
        assertThat(orderRepository.findStockPending(page)).isEmpty();
        assertThat(transactionRepository.existsInFlightForDocument("FAC-NONE", Instant.now().minusSeconds(3600))).isFalse();
        assertThat(spendRepository.existsPendingForDocument("FAC-NONE")).isFalse();
    }
}
