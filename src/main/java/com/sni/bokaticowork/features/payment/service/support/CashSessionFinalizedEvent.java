package com.sni.bokaticowork.features.payment.service.support;

/**
 * Une session de caisse vient d'etre close pour de bon · par le caissier, a la validation d'un
 * ecart, ou par le systeme (seuil d'une caisse automatique, fin de mois).
 *
 * @param bySystem     close par le systeme, sans comptage
 * @param cause        ce qui a motive la cloture, imprime dans le rapport envoye
 * @param renewedAs    la session ouverte a sa suite, s'il y en a une
 */
public record CashSessionFinalizedEvent(String sessionNumber, boolean bySystem, String cause, String renewedAs) {
}
