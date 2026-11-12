package com.sni.bokaticowork.features.contract.service.support;

import com.sni.bokaticowork.core.utils.pdf.PdfFonts;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * La police des contrats, embarquee dans le PDF.
 *
 * <p>Les gabarits demandent Spectral par un {@code <link>} vers Google Fonts. Un navigateur la
 * telecharge ; le moteur PDF, non · il retombait silencieusement sur Times. Les fichiers sont donc
 * livres avec l'application et declares au moteur.</p>
 *
 * <p>Le chargement et l'enregistrement vivent desormais dans {@link PdfFonts}, que le rendu commun
 * {@link com.sni.bokaticowork.core.utils.pdf.PdfRenderer} applique a toutes les pieces. Cette
 * classe reste comme composant Spring pour les appelants et les tests qui la citent.</p>
 */
@Component
public class ContractPdfFonts extends PdfFonts {

    public static final String FAMILY = PdfFonts.FAMILY;

    @Override
    @PostConstruct
    public void load() {
        super.load();
    }
}
