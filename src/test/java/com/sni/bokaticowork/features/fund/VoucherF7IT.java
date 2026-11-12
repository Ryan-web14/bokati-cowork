package com.sni.bokaticowork.features.fund;

import com.sni.bokaticowork.core.generator.qrcode.QrCodeRenderer;
import com.sni.bokaticowork.features.fund.voucher.VoucherService;
import com.sni.bokaticowork.support.PostgresIntegrationTestBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thymeleaf.spring6.SpringTemplateEngine;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Lot F7 sur le schema de production : numeros de pieces sans trou, reimpressions, protection. */
@Import(VoucherService.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=validate")
class VoucherF7IT extends PostgresIntegrationTestBase {

    @Autowired private VoucherService vouchers;
    @Autowired private DataSource dataSource;
    @MockitoBean private SpringTemplateEngine templateEngine;
    @MockitoBean private QrCodeRenderer qrCodeRenderer;

    @Test
    @DisplayName("Un numero par fait, consecutif par type, reimpression comptee, piece non modifiable")
    void numbering() {
        String source = "SRC-" + UUID.randomUUID();
        VoucherService.Issued first = vouchers.issueAndPrint("TST", "TEST", source, new BigDecimal("1000"), "Test", "a@elleaose.com");
        VoucherService.Issued again = vouchers.issueAndPrint("TST", "TEST", source, new BigDecimal("1000"), "Test", "a@elleaose.com");
        VoucherService.Issued next = vouchers.issueAndPrint("TST", "TEST", source + "-2", new BigDecimal("2000"), "Test", "a@elleaose.com");

        assertThat(first.number()).matches("TST-\\d{6}-\\d{6}");
        assertThat(again.number()).isEqualTo(first.number());
        assertThat(again.printCount()).isEqualTo(1);
        int a = Integer.parseInt(first.number().substring(first.number().length() - 6));
        int b = Integer.parseInt(next.number().substring(next.number().length() - 6));
        assertThat(b).isEqualTo(a + 1);

        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        assertThatThrownBy(() -> jdbc.update("UPDATE acc_voucher SET amount = 1 WHERE number = ?", first.number())).hasMessageContaining("ne se modifie pas");
    }

    @Test
    @DisplayName("Le compteur de pieces n'avance que d'un pas")
    void counterIsProtected() {
        vouchers.issueAndPrint("TSU", "TEST", "SRC-" + UUID.randomUUID(), BigDecimal.ONE, null, "a@elleaose.com");
        assertThatThrownBy(() -> new JdbcTemplate(dataSource).update("UPDATE acc_voucher_counter SET last_number = last_number + 3 WHERE voucher_type = 'TSU'"))
                .hasMessageContaining("un numero a la fois");
    }
}
