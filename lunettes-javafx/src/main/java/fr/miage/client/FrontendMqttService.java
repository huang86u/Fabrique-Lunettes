package fr.miage.client;

import fr.miage.shared.Commande;
import fr.miage.shared.FormatteurMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public final class FrontendMqttService implements AutoCloseable {
    private final FrontendConfiguration configuration;
    private final Consumer<String> connectionStatusConsumer;
    private final Map<String, Consumer<String>> topicHandlers = new ConcurrentHashMap<>();
    private MqttAsyncClient client;

    public FrontendMqttService(FrontendConfiguration configuration, Consumer<String> connectionStatusConsumer) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.connectionStatusConsumer = Objects.requireNonNull(connectionStatusConsumer, "connectionStatusConsumer");
    }

    public void start() {
        try {
            client = new MqttAsyncClient(configuration.brokerUrl(), configuration.clientId(), new MemoryPersistence());
            client.setCallback(new FrontendCallback());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.connect(options).waitForCompletion();
            connectionStatusConsumer.accept("Connecte au broker");
        } catch (MqttException exception) {
            throw new IllegalStateException("Impossible de se connecter au broker MQTT", exception);
        }
    }

    public void sendOrder(String orderId, Commande commande, Runnable onValidated, Consumer<String> onCancelled) {
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("Le client MQTT n'est pas connecte");
        }

        try {
            subscribeToOrderUpdates(orderId, onValidated, onCancelled);

            String payload = FormatteurMessage.encoder(commande);
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish("orders/" + orderId, message).waitForCompletion();
        } catch (MqttException exception) {
            clearOrderTracking(orderId);
            throw new IllegalStateException("Impossible d'envoyer la commande", exception);
        }
    }

    @Override
    public void close() {
        if (client == null) {
            return;
        }

        try {
            if (client.isConnected()) {
                client.disconnect().waitForCompletion();
            }
            client.close();
        } catch (MqttException exception) {
            connectionStatusConsumer.accept("Deconnexion incomplete");
        }
    }

    private final class FrontendCallback implements MqttCallbackExtended {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            resubscribeTrackedTopics();
            connectionStatusConsumer.accept(reconnect ? "Connexion restauree" : "Connecte au broker");
        }

        @Override
        public void connectionLost(Throwable cause) {
            connectionStatusConsumer.accept("Connexion perdue");
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) {
            Consumer<String> handler = topicHandlers.get(topic);
            if (handler == null) {
                return;
            }

            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
            handler.accept(payload);

            if (topic.endsWith("/validated") || topic.endsWith("/cancelled")) {
                clearOrderTracking(extractOrderId(topic));
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    private void subscribeToOrderUpdates(String orderId, Runnable onValidated, Consumer<String> onCancelled)
            throws MqttException {
        subscribeToTopic(validatedTopic(orderId), ignoredPayload -> onValidated.run());
        subscribeToTopic(cancelledTopic(orderId), onCancelled);
    }

    private void subscribeToTopic(String topic, Consumer<String> handler) throws MqttException {
        topicHandlers.put(topic, handler);
        client.subscribe(topic, 1).waitForCompletion();
    }

    private void resubscribeTrackedTopics() {
        if (client == null || !client.isConnected()) {
            return;
        }

        for (String topic : topicHandlers.keySet()) {
            try {
                client.subscribe(topic, 1).waitForCompletion();
            } catch (MqttException exception) {
                connectionStatusConsumer.accept("Connexion active, suivi incomplet");
            }
        }
    }

    private void clearOrderTracking(String orderId) {
        unsubscribe(validatedTopic(orderId));
        unsubscribe(cancelledTopic(orderId));
    }

    private void unsubscribe(String topic) {
        topicHandlers.remove(topic);

        if (client == null || !client.isConnected()) {
            return;
        }

        try {
            client.unsubscribe(topic).waitForCompletion();
        } catch (MqttException exception) {
            connectionStatusConsumer.accept("Connexion active, nettoyage incomplet");
        }
    }

    private String extractOrderId(String topic) {
        String[] parts = topic.split("/");
        return parts.length >= 2 ? parts[1] : "";
    }

    private String validatedTopic(String orderId) {
        return "orders/" + orderId + "/validated";
    }

    private String cancelledTopic(String orderId) {
        return "orders/" + orderId + "/cancelled";
    }
}
