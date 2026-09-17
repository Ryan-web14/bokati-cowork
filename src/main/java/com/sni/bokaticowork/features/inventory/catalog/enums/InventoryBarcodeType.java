package com.sni.bokaticowork.features.inventory.catalog.enums;

/**
 * Nature d'un code-barres porte par un article.
 */
public enum InventoryBarcodeType {

    /** Code a barres europeen sur 13 chiffres. */
    EAN13(13),

    /** Code a barres europeen court sur 8 chiffres. */
    EAN8(8),

    /** Code produit nord-americain sur 12 chiffres. */
    UPC(12),

    /** Code alphanumerique de longueur libre. */
    CODE128(0),

    /** Code bidimensionnel de longueur libre. */
    QR(0),

    /** Code interne a l'entreprise. */
    INTERNAL(0),

    /** Reference du fournisseur, telle qu'imprimee sur son emballage. */
    SUPPLIER(0);

    private final int expectedDigits;

    InventoryBarcodeType(int expectedDigits) {
        this.expectedDigits = expectedDigits;
    }

    /** Nombre de chiffres attendu, ou zero lorsque la longueur est libre. */
    public int getExpectedDigits() {
        return expectedDigits;
    }

    /** Vrai lorsque la valeur doit etre exclusivement numerique et de longueur fixe. */
    public boolean isFixedNumeric() {
        return expectedDigits > 0;
    }
}
