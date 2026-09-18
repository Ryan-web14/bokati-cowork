package com.sni.bokaticowork.features.payment.provider.pawaypay;

public enum CongoCorrespondent {

    MTN_MOMO_CIV("MTN Mobile Money Cote d'Ivoire", "CIV", "XOF"),
    ORANGE_CIV("Orange Money Cote d'Ivoire", "CIV", "XOF"),
    WAVE_CIV("Wave Cote d'Ivoire", "CIV", "XOF"),
    AIRTEL_COG("Airtel Money Congo", "COG", "XAF"),
    MTN_MOMO_COG("MTN Mobile Money Congo", "COG", "XAF"),
    FREE_SEN("Free Money Senegal", "SEN", "XOF"),
    ORANGE_SEN("Orange Money Senegal", "SEN", "XOF"),
    WAVE_SEN("Wave Senegal", "SEN", "XOF"),
    ORANGE_COD("Orange Money RDC", "COD", "CDF"),
    AIRTEL_COD("Airtel Money RDC", "COD", "CDF"),
    VODACOM_MPESA_COD("Vodacom M-Pesa RDC", "COD", "CDF"),
    AIRTEL_GAB("Airtel Money Gabon", "GAB", "XAF"),

    // Backward-compatible aliases kept for existing clients.
    AIRTEL_OAPI_COG("Airtel Money Congo", "COG", "XAF", "AIRTEL_COG"),
    AIRTEL_OAPI_COD("Airtel Money RDC", "COD", "CDF", "AIRTEL_COD"),
    MPESA_COD("Vodacom M-Pesa RDC", "COD", "CDF", "VODACOM_MPESA_COD");

    private final String displayName;
    private final String countryCode;
    private final String currency;
    private final String providerCode;

    CongoCorrespondent(String displayName, String countryCode, String currency) {
        this(displayName, countryCode, currency, null);
    }

    CongoCorrespondent(String displayName, String countryCode, String currency, String providerCode) {
        this.displayName = displayName;
        this.countryCode = countryCode;
        this.currency = currency;
        this.providerCode = providerCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getCurrency() {
        return currency;
    }

    public String providerCode() {
        return providerCode == null ? name() : providerCode;
    }

    public boolean selectable() {
        return providerCode == null;
    }

    /**
     * Indicatif du pays de l'operateur · ce que le MSISDN doit porter en tete.
     *
     * <p>L'operateur sait dans quel pays il est ; l'abonne, lui, tape souvent son numero tel qu'il
     * le compose localement. C'est ici qu'on complete.</p>
     */
    public String dialCode() {
        return switch (countryCode) {
            case "COG" -> "+242";
            case "COD" -> "+243";
            case "CIV" -> "+225";
            case "SEN" -> "+221";
            case "GAB" -> "+241";
            default -> null;
        };
    }

    /** Pays ou le zero de tete fait partie du numero et se garde en forme internationale. */
    public boolean keepsTrunkZero() {
        return "COG".equals(countryCode) || "CIV".equals(countryCode) || "GAB".equals(countryCode);
    }
}
