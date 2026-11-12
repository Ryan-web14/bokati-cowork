package com.sni.bokaticowork.core.settings.platform;

import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Au demarrage, les valeurs modifiees passent devant {@code application.yml} · chaque {@code @Value}
 * et chaque planification les voient sans changer de code.
 *
 * <p>Tout ce qui n'est pas lu au moment de servir prend donc effet au redemarrage suivant.</p>
 *
 * <p>Rien ici ne doit empecher l'application de demarrer : une base injoignable ou une table pas
 * encore creee laisse la configuration telle quelle. Seules les cles du catalogue passent, jamais un
 * secret, et elles ont ete controlees a l'enregistrement.</p>
 */
public class PlatformSettingsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    public static final String SOURCE_NAME = "platformSettings";
    private static final String TIMEOUT_SECONDS = "5";

    private final Log log;

    public PlatformSettingsEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(PlatformSettingsEnvironmentPostProcessor.class);
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        try {
            String url = environment.getProperty("spring.datasource.url");
            if (!StringUtils.hasText(url) || !url.startsWith("jdbc:postgresql:")) {
                return;
            }
            Map<String, Object> values = read(url, environment.getProperty("spring.datasource.username"),
                    environment.getProperty("spring.datasource.password"));
            if (!values.isEmpty()) {
                environment.getPropertySources().addFirst(new MapPropertySource(SOURCE_NAME, values));
                log.info(values.size() + " parametre(s) de la plateforme charge(s)");
            }
        } catch (SQLException | RuntimeException ex) {
            log.warn("Parametres de la plateforme non charges, configuration du fichier conservee : " + ex.getMessage());
        }
    }

    private static Map<String, Object> read(String url, String username, String password) throws SQLException {
        Properties properties = new Properties();
        if (username != null) {
            properties.setProperty("user", username);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
        properties.setProperty("connectTimeout", TIMEOUT_SECONDS);
        properties.setProperty("loginTimeout", TIMEOUT_SECONDS);
        properties.setProperty("socketTimeout", TIMEOUT_SECONDS);
        Map<String, Object> values = new HashMap<>();
        try (Connection connection = DriverManager.getConnection(url, properties);
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT setting_key, value FROM platform_setting")) {
            while (rows.next()) {
                String key = rows.getString(1);
                PlatformSettingCatalog.find(key)
                        .filter(definition -> definition.type() != PlatformSettingCatalog.Type.SECRET)
                        .ifPresent(definition -> {
                            try {
                                values.put(key, rows.getString(2));
                            } catch (SQLException ex) {
                                throw new IllegalStateException(ex);
                            }
                        });
            }
        }
        return values;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
