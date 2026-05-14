package fr.miage.backend;

import bernard_flou.Fabricateur;
import fr.miage.backend.usine.Usine;
import fr.miage.shared.Commande;
import fr.miage.shared.FormatteurMessage;
import fr.miage.shared.TypeLunette;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MqttServer implements MqttCallback {
    private final Usine usine = new Usine();
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

        if (topic.startsWith("orders/") && topic.split("/").length == 2) {
            String orderId = topic.split("/")[1];
            traiterCommande(orderId, payload);
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
        produireEtExpedier(orderId, commande);
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

    private void produireEtExpedier(String orderId, Commande commande) {
        try {
            List<Fabricateur.Lunette> lunettes = usine.produire(commande);
            List<String> serials = extraireNumerosSerie(lunettes);
            publierMessage(serialsTopic(orderId), String.join(",", serials));
            publierMessage(shippedTopic(orderId), construireMessageExpedition(orderId, lunettes.size()));
        } catch (RuntimeException exception) {
            publierMessage(cancelledTopic(orderId), "Fabrication interrompue : " + exception.getMessage());
        }
    }

    private List<String> extraireNumerosSerie(List<Fabricateur.Lunette> lunettes) {
        List<String> serials = new ArrayList<>();
        for (Fabricateur.Lunette lunette : lunettes) {
            serials.add(lunette.serial);
        }
        return serials;
    }

    private String construireMessageExpedition(String orderId, int quantite) {
        String prefix = orderId.length() >= 8 ? orderId.substring(0, 8).toUpperCase() : orderId.toUpperCase();
        return "Expedition confirmee : " + quantite + " paire(s) - ref EXP-" + prefix;
    }

    private String validatedTopic(String orderId) { return "orders/" + orderId + "/validated"; }
    private String cancelledTopic(String orderId) { return "orders/" + orderId + "/cancelled"; }
    private String serialsTopic(String orderId) { return "orders/" + orderId + "/serials"; }
    private String shippedTopic(String orderId) { return "orders/" + orderId + "/shipped"; }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    }
}
