package com.sni.bokaticowork.core.identifier;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Identifiants lisibles et reversibles (17/11/2026). */
class PublicIdTest {

    private static final long BIG = 7_319_298_661_696_688_123L;

    private final JsonMapper mapper = JsonMapper.builder().addModule(new PublicIdJacksonModule()).build();

    public record AccVatReturn(Long id, String periodCode, Long journalEntryId, List<Long> lineIds, Long fileSize, long createdBy) {
    }

    public record LinkRequest(Long accountId, Long amount) {
    }

    @Test
    void anIdRoundTripsThroughItsCode() {
        String code = PublicId.encode("VR", BIG);
        assertThat(code).matches("VR-[0-9A-Z]{13}");
        assertThat(PublicId.decode(code)).isEqualTo(BIG);
        assertThat(PublicId.decode(code.toLowerCase())).isEqualTo(BIG);
        assertThat(PublicId.decode(code.substring(3))).isEqualTo(BIG);
        assertThat(PublicId.decode(String.valueOf(BIG))).isEqualTo(BIG);
        assertThat(PublicId.decode(PublicId.encode("X", 0L))).isZero();
        assertThat(PublicId.decode(PublicId.encode("X", Long.MAX_VALUE))).isEqualTo(Long.MAX_VALUE);
    }

    @Test
    void garbageIsRefused() {
        assertThatThrownBy(() -> PublicId.decode("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PublicId.decode("VR-ZZZZZZZZZZZZZ")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void prefixesAreReadable() {
        assertThat(PublicId.prefixFor("AccVatReturn")).isEqualTo("VR");
        assertThat(PublicId.prefixFor("fixedAssetId")).isEqualTo("FA");
        assertThat(PublicId.prefixFor("accountId")).isEqualTo("ACC");
        assertThat(PublicId.prefixFor("CashSessionResponse")).isEqualTo("CS");
    }

    @Test
    void idsAreWrittenAsCodesAndOtherNumbersStayNumbers() throws Exception {
        String json = mapper.writeValueAsString(new AccVatReturn(BIG, "2026-11", 42L, List.of(BIG, 7L), 1024L, 5L));

        assertThat(json).contains("\"id\":\"VR-").contains("\"journalEntryId\":\"JE-").contains("\"lineIds\":[\"LIN-")
                .contains("\"fileSize\":1024").contains("\"createdBy\":5");
        AccVatReturn back = mapper.readValue(json, AccVatReturn.class);
        assertThat(back.id()).isEqualTo(BIG);
        assertThat(back.journalEntryId()).isEqualTo(42L);
        assertThat(back.lineIds()).containsExactly(BIG, 7L);
    }

    @Test
    void aBigNumberWithoutAnIdNameIsStillProtected() throws Exception {
        String json = mapper.writeValueAsString(new AccVatReturn(1L, "p", null, List.of(), 1L, BIG));
        assertThat(json).contains("\"createdBy\":\"CB-");
    }

    @Test
    void bodiesAcceptCodesTextAndNumbers() throws Exception {
        String code = PublicId.encode("ACC", BIG);
        assertThat(mapper.readValue("{\"accountId\":\"" + code + "\",\"amount\":5}", LinkRequest.class).accountId()).isEqualTo(BIG);
        assertThat(mapper.readValue("{\"accountId\":\"" + BIG + "\"}", LinkRequest.class).accountId()).isEqualTo(BIG);
        assertThat(mapper.readValue("{\"accountId\":12}", LinkRequest.class).accountId()).isEqualTo(12L);
    }
}
