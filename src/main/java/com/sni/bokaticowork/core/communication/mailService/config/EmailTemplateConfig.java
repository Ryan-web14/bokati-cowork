package com.sni.bokaticowork.core.communication.mailService.config;


import com.sni.bokaticowork.core.communication.mailService.support.EmailBranding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
@EnableConfigurationProperties(MicrosoftGraphMailProperties.class)
public class EmailTemplateConfig {

    @Bean(name = "emailTemplateResolver")
    public ITemplateResolver thymeleafTemplateResolver(){
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/email/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCheckExistence(true);
        return resolver;
    }

    /**
     * Spring Boot ajoute au moteur tous les beans de type {@code IDialect} · une declaration ici
     * suffit a exposer {@code #branding} partout, sans toucher aux services d'envoi.
     */
    @Bean
    public BrandingDialect brandingDialect(EmailBranding emailBranding) {
        return new BrandingDialect(emailBranding);
    }

    //@Bean(name = "emailTemplateEngine")
    public SpringTemplateEngine emailTemplateEngine(ITemplateResolver resolver) {
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }


}
