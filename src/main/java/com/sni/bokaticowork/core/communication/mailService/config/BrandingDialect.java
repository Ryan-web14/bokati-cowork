package com.sni.bokaticowork.core.communication.mailService.config;

import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import org.thymeleaf.context.IExpressionContext;
import org.thymeleaf.dialect.AbstractDialect;
import org.thymeleaf.dialect.IExpressionObjectDialect;
import org.thymeleaf.expression.IExpressionObjectFactory;

import java.util.Set;

/**
 * Expose {@code #branding} a tous les gabarits.
 *
 * <p>Une reference de bean · {@code ${@emailBranding.logoUrl()}} · ne fonctionne pas ici : les
 * services d'envoi construisent un {@code org.thymeleaf.context.Context} nu, sans resolveur de
 * beans, et le rendu echoue avec
 * {@code EL1057E: No bean resolver registered in the context}. Constate a l'execution.
 *
 * <p>Un objet d'expression, lui, ne depend d'aucun contexte Spring. Une seule declaration ici, et
 * la vingtaine de services qui envoient des courriels n'ont rien a changer.
 */
public class BrandingDialect extends AbstractDialect implements IExpressionObjectDialect {

    private static final String NAME = "branding";
    private static final Set<String> EXPOSED = Set.of(NAME);

    private final EmailBranding branding;

    public BrandingDialect(EmailBranding branding) {
        super("Branding");
        this.branding = branding;
    }

    @Override
    public IExpressionObjectFactory getExpressionObjectFactory() {
        return new IExpressionObjectFactory() {
            @Override
            public Set<String> getAllExpressionObjectNames() {
                return EXPOSED;
            }

            @Override
            public Object buildObject(IExpressionContext context, String expressionObjectName) {
                return NAME.equals(expressionObjectName) ? branding : null;
            }

            @Override
            public boolean isCacheable(String expressionObjectName) {
                // L'adresse du logo ne change pas en cours d'execution.
                return true;
            }
        };
    }
}
