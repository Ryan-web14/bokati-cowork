package com.sni.bokaticowork.features.payment.provider.pawaypay;

/**
 * La cle de signature est illisible · l'appel financier n'est pas parti.
 *
 * <p>Rien n'a ete envoye a PawaPay, aucun prelevement n'est possible : l'issue est un echec net,
 * pas une issue inconnue a relire pendant 24 heures.</p>
 */
public class PawapaySigningUnavailableException extends IllegalStateException {

    public PawapaySigningUnavailableException(String reason) {
        super("Signature PawaPay indisponible : " + reason);
    }

    /** Retrouve l'exception dans une chaine de causes, quel que soit l'emballage du client HTTP. */
    static PawapaySigningUnavailableException find(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof PawapaySigningUnavailableException signing) {
                return signing;
            }
        }
        return null;
    }
}
