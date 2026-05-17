package fr.miage.backend;

import bernard_flou.Fabricateur;
import fr.miage.backend.usine.Usine;
import fr.miage.shared.Commande;
import fr.miage.shared.FormatteurMessage;
import fr.miage.shared.TypeLunette;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttServer implements MqttCallback {
    private final Usine usine = new Usine();
    private final Map<String, String> registreNumerosSerie = new ConcurrentHashMap<>();
    private MqttClient client;

    public void start(String brokerUrl, String clientId) {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(this);

            System.out.println("Connexion au broker MQTT : " + brokerUrl + " ...");
            client.connect(options);
            System.out.println("Connecte !");

            client.subscribe("orders/+");
            System.out.println("Abonne au topic : 'orders/+'...");
            client.subscribe("serials/+/check");
            System.out.println("Abonne au topic : 'serials/+/check'...");
            client.subscribe("backend/ping");
            System.out.println("Abonne au topic : 'backend/ping'...");
        } catch (MqttException exception) {
            System.err.println("Erreur de connexion au broker MQTT !");
            exception.printStackTrace();
        }
    }

    private void publierMessage(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            System.out.println("Message publie sur le topic [" + topic + "] : " + payload);
        } catch (MqttException exception) {
            System.err.println("Erreur lors de la publication du message !");
            exception.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connexion perdue avec le broker !");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        System.out.println("Message recu sur le topic [" + topic + "] : " + payload);

        if (topic.equals("backend/ping")) {
            publierMessage("backend/pong", payload);
            return;
        }

        String[] morceauxTopic = topic.split("/");

        if (morceauxTopic.length == 2 && "orders".equals(morceauxTopic[0])) {
            String orderId = morceauxTopic[1];
            Thread threadCommande = new Thread(
                    () -> traiterCommande(orderId, payload),
                    "commande-" + orderId
            );
            threadCommande.start();
            return;
        }

        if (morceauxTopic.length == 3
                && "serials".equals(morceauxTopic[0])
                && "check".equals(morceauxTopic[2])) {
            verifierNumeroSerie(morceauxTopic[1]);
        }
    }

    private void traiterCommande(String orderId, String payload) {
        Commande commande = FormatteurMessage.decoder(payload);
        boolean isValid = validerCommande(commande);

        if (!isValid) {
            System.out.println("Commande " + orderId + " invalide !");
            publierMessage(cancelledTopic(orderId), "Commande invalide : verifiez les quantites.");
            return;
        }

        System.out.println("Commande " + orderId + " validee !");
        publierMessage(validatedTopic(orderId), "");
        produireEtLivrer(orderId, commande);
    }

    private boolean validerCommande(Commande commande) {
        if (commande.lignes().isEmpty()) {
            return false;
        }

        int total = commande.getQuantiteTotale();
        if (total <= 0) {
            return false;
        }

        for (Map.Entry<TypeLunette, Integer> entry : commande.lignes().entrySet()) {
            int quantity = entry.getValue();
            if (quantity < 0 || quantity >= 10) {
                return false;
            }
        }

        return true;
    }

    private void produireEtLivrer(String orderId, Commande commande) {
        try {
            List<Fabricateur.Lunette> lunettes;
            synchronized (usine) {
                publierMessage(statusTopic(orderId), "processing");
                lunettes = usine.produire(commande);
            }
            enregistrerNumerosSerie(lunettes);
            publierMessage(statusTopic(orderId), "processed");
            publierMessage(deliveryTopic(orderId), construireMessageLivraison(lunettes));
        } catch (RuntimeException exception) {
            publierMessage(errorTopic(orderId), "Fabrication interrompue : " + exception.getMessage());
        }
    }

    private void enregistrerNumerosSerie(List<Fabricateur.Lunette> lunettes) {
        for (Fabricateur.Lunette lunette : lunettes) {
            registreNumerosSerie.put(lunette.serial, lunette.type.name());
        }
    }

    private String construireMessageLivraison(List<Fabricateur.Lunette> lunettes) {
        List<String> lignes = new ArrayList<>();
        for (Fabricateur.Lunette lunette : lunettes) {
            lignes.add(lunette.type.name() + ":" + lunette.serial);
        }
        return String.join(",", lignes);
    }

    private void verifierNumeroSerie(String numeroSerie) {
        String type = registreNumerosSerie.getOrDefault(numeroSerie, "invalid");
        publierMessage(serialResponseTopic(numeroSerie), type);
    }

    private String validatedTopic(String orderId) { return "orders/" + orderId + "/validated"; }
    private String cancelledTopic(String orderId) { return "orders/" + orderId + "/cancelled"; }
    private String statusTopic(String orderId) { return "orders/" + orderId + "/status"; }
    private String deliveryTopic(String orderId) { return "orders/" + orderId + "/delivery"; }
    private String errorTopic(String orderId) { return "orders/" + orderId + "/error"; }
    private String serialResponseTopic(String numeroSerie) { return "serials/" + numeroSerie; }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
