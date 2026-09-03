package com.sni.bokaticowork.features.billing.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Signature PAdES des PDF emis.
 *
 * <p>C'est le seul mecanisme qui fait qu'un lecteur PDF signale <b>de lui-meme</b> qu'un document
 * a ete modifie, sans que le destinataire ait la moindre demarche a faire. Les protections propres
 * au format · mot de passe proprietaire, drapeaux de permission · sont declaratives et se retirent
 * en une commande ; une signature, non : elle ne se refabrique pas sans la cle.
 *
 * <h2>Certificat auto-signe</h2>
 * Avec un certificat auto-signe, le lecteur affiche « signature valide, emetteur non verifie » · 
 * bandeau jaune plutot que vert. L'essentiel est acquis pour autant : <b>le bandeau change des
 * qu'un octet bouge</b>. Passer a un certificat d'une autorite reconnue ne changera pas une ligne
 * de code, seulement la source du keystore.
 *
 * <h2>La cle</h2>
 * Le keystore ne va pas dans le depot. En production, il se fournit en base64 par variable
 * d'environnement, comme la cle de signature fiscale. Sa compromission permettrait de signer de
 * faux documents : prevoir la rotation avant la mise en service.
 */
@Data
@Component
@ConfigurationProperties(prefix = "bokati.pdf.signature")
public class PdfSignatureProperties {

    /**
     * Desactive par defaut · sans keystore configure, rien ne doit echouer. Un document non signe
     * reste couvert par le gel et l'empreinte.
     */
    private boolean enabled = false;

    /** Keystore PKCS#12 encode en base64. Prioritaire sur {@code keystorePath}. */
    private String keystoreBase64;

    /** Chemin du keystore PKCS#12 sur le disque · pratique en developpement. */
    private String keystorePath;

    private String keystorePassword;

    /** Alias de la cle dans le keystore. */
    private String alias = "bokati";

    /** Motif affiche par le lecteur PDF a cote de la signature. */
    private String reason = "Document emis par Bokati Cowork";

    private String location = "Brazzaville, Congo";

    private String contact;
}
