package com.sni.bokaticowork.core.settings.platform;

import java.util.List;

/**
 * Des parametres qui gardent leur propre table · ils rejoignent l'ecran, les droits et le journal,
 * mais se lisent et s'ecrivent chez leur module. Leur prise d'effet est toujours immediate.
 */
public interface PlatformSettingProvider {

    /**
     * @param value   la valeur en vigueur
     * @param options les valeurs permises d'une liste de choix · vide sinon
     */
    record ProvidedSetting(PlatformSettingCatalog.Definition definition, String value, List<String> options) {
    }

    List<ProvidedSetting> settings();

    /** Controle et enregistre · refuse par une {@code BadRequestException}. */
    void update(String key, String value);
}
