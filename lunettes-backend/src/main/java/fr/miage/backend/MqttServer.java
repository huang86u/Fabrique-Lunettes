package fr.miage.backend;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import fr.miage.shared.FormatteurMessage;
import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;


public class MqttServer implements MqttCallback {

    private MqttClient client;

    public void start(String brokerUrl, String clientId) {
        try {
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);

            client.setCallback(this);

            System.out.println("Connexion au broker MQTT : " + brokerUrl + " ...");
            client.connect(options);
            System.out.println("Connecté !");


            // S'abonner au topic "orders/+" pour recevoir les commandes
            client.subscribe("orders/+");
            System.out.println("Abonné au topic : 'orders/+'...");

        } catch (MqttException e) {
            System.err.println("Erreur de connexion au broker MQTT !");
            e.printStackTrace();
        }
    }

    // Méthode pour publier un message
    private void publierMessage(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // Assure une livraison au moins une fois
            client.publish(topic, message);
            System.out.println("Message publié sur le topic [" + topic + "] : " + payload);
        } catch (MqttException e) {
            System.err.println("Erreur lors de la publication du message !");
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connexion perdue avec le broker !");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        String payload = new String(message.getPayload());
        System.out.println("Message reçu sur le topic [" + topic + "] : " + new String(message.getPayload()));


        // Traiter uniquement les messages du topic "orders/{orderId}" 
        if(topic.startsWith("orders/") && topic.split("/").length == 2) {
            String orderId = topic.split("/")[1];
            traiterCommande(orderId, payload);
        }
    }

    private void traiterCommande(String orderId, String payload) {
        //Décodage de la commande reçue
        Commande commande = FormatteurMessage.decoder(payload);

        //Validation selon les règles du prof
        boolean isValid = validerCommande(commande);

        if (isValid) {
            System.out.println("Commande " + orderId + " validée !");
            publierMessage("orders/" + orderId + "/validated", ""); 

        } else {
            System.out.println("Commande " + orderId + " invalide !");
            publierMessage("orders/" + orderId + "/cancelled", "Commande invalide : vérifiez les quantités.");
        }
    }

    private boolean validerCommande(Commande commande) {
        if (commande.lignes().isEmpty()) return false;
        
        int total = commande.getQuantiteTotale();
        if (total <= 0) return false; // la quantité totale doit être strictement supérieure à 0

        for (Map.Entry<TypeLunette, Integer> entry : commande.lignes().entrySet()) {
            int qte = entry.getValue();
            //la quantité de chaque type doit être comprise entre 0 (inclu) et 10 (exclu)
            if (qte < 0 || qte >= 10) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

}