package com.sni.bokaticowork.core.exception.customs;

import com.sni.bokaticowork.core.utils.error.ErrorCode;

/** Connexion a l'espace d'administration par un compte sans role d'equipe (membre, client). */
public class BackOfficeAccessDeniedException extends BaseException {

    public BackOfficeAccessDeniedException(String message) {
        super(message, ErrorCode.BACK_OFFICE_ACCESS_DENIED);
    }
}
