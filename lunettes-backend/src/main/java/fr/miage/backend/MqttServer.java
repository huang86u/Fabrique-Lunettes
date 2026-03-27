package fr.miage.backend;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

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

        } catch (MqttException e) {
            System.err.println("Erreur de connexion au broker MQTT !");
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        System.out.println("Connexion perdue avec le broker !");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Message reçu sur le topic [" + topic + "] : " + new String(message.getPayload()));
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}