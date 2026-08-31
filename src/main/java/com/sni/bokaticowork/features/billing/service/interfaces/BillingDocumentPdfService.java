package com.sni.bokaticowork.features.billing.service.interfaces;

import com.sni.bokaticowork.features.billing.enums.BillingDocumentStatus;

public interface BillingDocumentPdfService {

    byte[] generatePdf(String documentNumber);

    /**
     * Compare un fichier a la version canonique du document · repond a la seule question qui
     * compte dans un litige : « ce fichier est-il celui que vous avez emis ? »
     */
    PdfComparison compare(String documentNumber, byte[] candidate);

    /**
     * @param sealed      le document dispose d'une version figee · sans elle, aucune comparaison
     *                    n'est possible et l'absence de correspondance ne prouverait rien
     * @param match       le fichier fourni est exactement la version canonique
     * @param expected    empreinte de la version canonique, {@code null} si le document n'est pas scelle
     * @param provided    empreinte du fichier fourni
     * @param status      statut courant · un document annule est servi avec un filigrane, sa copie
     *                    telechargee differe donc legitimement de la version canonique
     */
    record PdfComparison(boolean sealed,
                         boolean match,
                         String expected,
                         String provided,
                         BillingDocumentStatus status) {

        /** Le document est annule · la copie servie porte un filigrane et ne peut pas correspondre. */
        public boolean watermarked() {
            return status == BillingDocumentStatus.CANCELLED || status == BillingDocumentStatus.VOIDED;
        }
    }
}
