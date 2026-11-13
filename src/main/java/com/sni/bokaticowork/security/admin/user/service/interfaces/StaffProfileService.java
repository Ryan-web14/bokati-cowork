package com.sni.bokaticowork.security.admin.user.service.interfaces;

import com.sni.bokaticowork.security.admin.user.dto.request.UpdateStaffProfileRequest;
import com.sni.bokaticowork.security.admin.user.dto.response.StaffProfileResponse;

import java.util.Optional;

/** Fiche collaborateur et auteur courant des pieces (lot B12). */
public interface StaffProfileService {

    /** Auteur d'une action, repris du jeton : identifiant de connexion, nom affiche, fonction. */
    record Author(String login, String name, String title) {
        public static final Author SYSTEM = new Author("SYSTEM", "Système", null);
    }

    /** L'utilisateur connecte ; {@link Author#SYSTEM} hors d'une requete authentifiee. */
    Author currentAuthor();

    /** Auteur a partir d'un identifiant de connexion (courriel) ; nom = identifiant s'il est inconnu. */
    Author authorOf(String login);

    /** Signature enregistree de l'utilisateur, en data URI. */
    Optional<String> signatureOf(String login);

    StaffProfileResponse get(String userCode);

    StaffProfileResponse update(String userCode, UpdateStaffProfileRequest request);

    StaffProfileResponse me();

    StaffProfileResponse updateMySignature(String signatureImageBase64);

    StaffProfileResponse clearMySignature();
}
