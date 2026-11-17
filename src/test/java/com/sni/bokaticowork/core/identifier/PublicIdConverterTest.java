package com.sni.bokaticowork.core.identifier;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.web.format.WebConversionService;
import org.springframework.boot.autoconfigure.web.format.DateTimeFormatters;

import static org.assertj.core.api.Assertions.assertThat;

/** Le convertisseur des chemins passe avant celui des nombres de Spring. */
class PublicIdConverterTest {

    @Test
    void aPathCodeIsDecoded() {
        WebConversionService conversion = new WebConversionService(new DateTimeFormatters());
        conversion.addConverter(String.class, Long.class, PublicId::decode);
        String code = PublicId.encode("VR", 7_319_298_661_696_688_123L);
        assertThat(conversion.convert(code, Long.class)).isEqualTo(7_319_298_661_696_688_123L);
        assertThat(conversion.convert("42", Long.class)).isEqualTo(42L);
    }
}
