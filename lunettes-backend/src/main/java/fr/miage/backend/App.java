package fr.miage.backend;

import java.io.InputStream;
import java.util.Properties;

public class App {
    public static void main(String[] args) {
        System.out.println("Démarrage de l'Usine de Lunettes...");

        Properties props = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Désolé, impossible de trouver application.properties");
                return;
            }
            props.load(input);
            String brokerUrl = props.getProperty("mqtt.broker.url");
            String clientId = props.getProperty("mqtt.client.id");

            MqttServer server = new MqttServer();
            server.start(brokerUrl, clientId);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}