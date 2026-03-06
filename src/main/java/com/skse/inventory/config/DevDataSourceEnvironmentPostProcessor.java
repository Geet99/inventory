package com.skse.inventory.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Ensures a valid JDBC datasource URL when running locally.
 * If spring.datasource.url is missing, empty, or doesn't start with "jdbc" (e.g. from an env var override),
 * forces H2 in-memory so the app starts without "URL must start with 'jdbc'" errors.
 */
public class DevDataSourceEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final String URL_KEY = "spring.datasource.url";
    private static final String USERNAME_KEY = "spring.datasource.username";
    private static final String PASSWORD_KEY = "spring.datasource.password";
    private static final String DRIVER_KEY = "spring.datasource.driver-class-name";

    private static final String H2_URL = "jdbc:h2:mem:inventory_db";
    private static final String H2_USER = "sa";
    private static final String H2_PASSWORD = "";
    private static final String H2_DRIVER = "org.h2.Driver";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty(URL_KEY);
        if (url == null || url.isBlank() || !url.strip().toLowerCase().startsWith("jdbc:")) {
            Map<String, Object> overrides = new HashMap<>();
            overrides.put(URL_KEY, H2_URL);
            overrides.put(USERNAME_KEY, H2_USER);
            overrides.put(PASSWORD_KEY, H2_PASSWORD);
            overrides.put(DRIVER_KEY, H2_DRIVER);
            environment.getPropertySources().addFirst(
                new MapPropertySource("devDataSourceOverride", overrides)
            );
        }
    }
}
