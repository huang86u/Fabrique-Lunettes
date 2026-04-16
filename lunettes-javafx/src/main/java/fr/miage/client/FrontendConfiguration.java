package fr.miage.client;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

public record FrontendConfiguration(String brokerUrl, String clientId) {

    public static FrontendConfiguration fromProperties() {
        Properties properties = new Properties();

        try (InputStream inputStream = FrontendConfiguration.class.getResourceAsStream("/frontend.properties")) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Impossible de charger frontend.properties", exception);
        }

        String brokerUrl = read(properties, "mqtt.broker.url", "tcp://localhost:1883");
        String clientId = read(properties, "mqtt.client.id", "ClientFrontend-" + UUID.randomUUID());

        return new FrontendConfiguration(brokerUrl, clientId);
    }

    private static String read(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }
}
