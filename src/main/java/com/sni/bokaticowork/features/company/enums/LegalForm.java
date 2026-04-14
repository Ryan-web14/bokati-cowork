package com.sni.bokaticowork.features.company.enums;


public enum LegalForm {

    SARL("Société Anonyme à responsabilité limitée"),
    SAS("Société à Responsabilité Limitée"),
    SARLU("Société à Responsabilité Limitée Unipersonnelle"),
    SNC("Société en Nom Collectif"),
    SCS("Société en Commandite SimplE"),
    SP("Société en Participation"),
    SCOOP("Société Coopérative"),
    SCI("Société Civile Immobilière"),
    SA("Société Anonyme"),
    SAU("Société Anonyme Unipersonnelle"),
    SASU("Société par Actions Simplifiée Unipersonnelle");


    LegalForm(String s) {
    };

    public String getCompleteForm(){
        return this.name();
    }
}
