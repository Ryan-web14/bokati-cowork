package com.sni.bokaticowork.features.payment.security.enums;

/** Le code secret est-il exige. */
public enum PinRequirement {

    /** Aucun code n'est demande, ni meme propose. */
    DISABLED,

    /**
     * Facultatif · le titulaire en pose un s'il le souhaite. C'est le defaut : exiger un code pour
     * consulter un solde ou regler une facture deja connue ajoute une friction que rien ne justifie,
     * et pousse vers des codes triviaux notes quelque part.
     */
    OPTIONAL,

    /** Exige pour toute operation sortante. */
    REQUIRED
}
