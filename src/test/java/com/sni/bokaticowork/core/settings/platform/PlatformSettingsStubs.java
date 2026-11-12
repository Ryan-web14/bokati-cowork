package com.sni.bokaticowork.core.settings.platform;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Un {@code PlatformSettings} simule rend la valeur de repli de l'appelant, comme le vrai sans
 * valeur enregistree : sans cela, un mock rend null ou 0 et fausse les services relus a chaud.
 * A appeler avant les bouchons propres au test, qui restent prioritaires.
 */
public final class PlatformSettingsStubs {

    private PlatformSettingsStubs() {
    }

    public static PlatformSettings fallbacks(PlatformSettings settings) {
        lenient().when(settings.text(anyString(), any())).thenAnswer(call -> call.getArgument(1));
        lenient().when(settings.number(anyString(), anyLong())).thenAnswer(call -> call.getArgument(1));
        lenient().when(settings.integer(anyString(), anyInt())).thenAnswer(call -> call.getArgument(1));
        lenient().when(settings.bool(anyString(), anyBoolean())).thenAnswer(call -> call.getArgument(1));
        lenient().when(settings.decimal(anyString(), any(BigDecimal.class))).thenAnswer(call -> call.getArgument(1));
        lenient().when(settings.decimal(anyString(), anyDouble())).thenAnswer(call -> call.getArgument(1));
        return settings;
    }
}
