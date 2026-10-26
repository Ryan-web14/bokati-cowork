package com.sni.bokaticowork.security.registration;

/**
 * Le domaine d'une adresse peut-il recevoir du courrier.
 *
 * <p>Separe de {@link EmailDomainPolicy} pour que la politique se teste sans reseau.</p>
 */
public interface MailDomainResolver {

    enum Verdict {
        /** Un serveur de courrier repond pour ce domaine (MX, ou a defaut une adresse). */
        RECEIVES_MAIL,
        /** Le domaine n'existe pas, ou n'a ni MX ni adresse · aucun courrier n'y arrivera. */
        NO_MAIL,
        /** Le DNS n'a pas repondu · on ne sait pas, et on ne refuse pas sur une panne. */
        UNKNOWN
    }

    Verdict resolve(String domain);
}
