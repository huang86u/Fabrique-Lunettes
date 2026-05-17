package fr.miage.client;

import fr.miage.shared.Commande;
import fr.miage.shared.FormatteurMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
    private final Consumer<Boolean> connectionStateConsumer;
    private final Map<String, Consumer<String>> topicHandlers = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    private MqttAsyncClient client;

    public FrontendMqttService(
            FrontendConfiguration configuration,
            Consumer<String> connectionStatusConsumer,
            Consumer<Boolean> connectionStateConsumer
    ) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.connectionStatusConsumer = Objects.requireNonNull(connectionStatusConsumer, "connectionStatusConsumer");
        this.connectionStateConsumer = Objects.requireNonNull(connectionStateConsumer, "connectionStateConsumer");
    }

    public synchronized void start() {
        try {
            ensureClient();

            if (client.isConnected()) {
                ensureBackendPongSubscription();
                connectionStateConsumer.accept(true);
                connectionStatusConsumer.accept("Connecte au broker");
                return;
            }

            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);

            client.connect(options).waitForCompletion();
            ensureBackendPongSubscription();
            connectionStateConsumer.accept(true);
            connectionStatusConsumer.accept("Connecte au broker");
        } catch (MqttException exception) {
            connectionStateConsumer.accept(false);
            throw new IllegalStateException("Impossible de se connecter au broker MQTT", exception);
        }
    }

    public boolean isConnected() {
        return client != null && client.isConnected();
    }

    public void sendOrder(
            String orderId,
            Commande commande,
            Runnable onValidated,
            Consumer<String> onCancelled,
            Consumer<String> onStatus,
            Consumer<String> onDelivery,
            Consumer<String> onError
    ) {
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("Le client MQTT n'est pas connecte");
        }

        try {
            subscribeToOrderUpdates(orderId, onValidated, onCancelled, onStatus, onDelivery, onError);

            String payload = FormatteurMessage.encoder(commande);
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish("orders/" + orderId, message).waitForCompletion();
        } catch (MqttException exception) {
            clearOrderTracking(orderId);
            throw new IllegalStateException("Impossible d'envoyer la commande", exception);
        }
    }

    public void checkSerial(String serial, Consumer<String> onResponse) {
        if (client == null || !client.isConnected()) {
            throw new IllegalStateException("Le client MQTT n'est pas connecte");
        }

        try {
            String responseTopic = serialResponseTopic(serial);
            subscribeToTopic(responseTopic, payload -> {
                onResponse.accept(payload);
                unsubscribe(responseTopic);
            });

            MqttMessage message = new MqttMessage(new byte[0]);
            message.setQos(1);
            client.publish(serialCheckTopic(serial), message).waitForCompletion();
        } catch (MqttException exception) {
            unsubscribe(serialResponseTopic(serial));
            throw new IllegalStateException("Impossible de verifier le numero de serie", exception);
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
            connectionStateConsumer.accept(false);
        } catch (MqttException exception) {
            connectionStateConsumer.accept(false);
            connectionStatusConsumer.accept("Deconnexion incomplete");
        }
    }

    private final class FrontendCallback implements MqttCallbackExtended {
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            resubscribeTrackedTopics();
            connectionStateConsumer.accept(true);
            connectionStatusConsumer.accept(reconnect ? "Connexion restauree" : "Connecte au broker");
        }

        @Override
        public void connectionLost(Throwable cause) {
            connectionStateConsumer.accept(false);
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

            if (topic.endsWith("/cancelled") || topic.endsWith("/delivery") || topic.endsWith("/error")) {
                clearOrderTracking(extractOrderId(topic));
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
        }
    }

    private void ensureClient() throws MqttException {
        if (client != null) {
            return;
        }

        client = new MqttAsyncClient(configuration.brokerUrl(), configuration.clientId(), new MemoryPersistence());
        client.setCallback(new FrontendCallback());
    }

    private void subscribeToOrderUpdates(
            String orderId,
            Runnable onValidated,
            Consumer<String> onCancelled,
            Consumer<String> onStatus,
            Consumer<String> onDelivery,
            Consumer<String> onError
    )
            throws MqttException {
        subscribeToTopic(validatedTopic(orderId), ignoredPayload -> onValidated.run());
        subscribeToTopic(cancelledTopic(orderId), onCancelled);
        subscribeToTopic(statusTopic(orderId), onStatus);
        subscribeToTopic(deliveryTopic(orderId), onDelivery);
        subscribeToTopic(errorTopic(orderId), onError);
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
        unsubscribe(statusTopic(orderId));
        unsubscribe(deliveryTopic(orderId));
        unsubscribe(errorTopic(orderId));
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

    public synchronized boolean pingBackend(long timeoutMillis) {
        if (client == null || !client.isConnected()) {
            return false;
        }

        try {
            ensureBackendPongSubscription();
            CompletableFuture<String> promise = new CompletableFuture<>();
            pendingResponses.put(configuration.clientId(), promise);

            String payload = configuration.clientId();
            MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            message.setQos(1);
            client.publish("backend/ping", message).waitForCompletion();

            String response = promise.get(timeoutMillis, TimeUnit.MILLISECONDS);
            return configuration.clientId().equals(response);
        } catch (Exception exception) {
            return false;
        } finally {
            pendingResponses.remove(configuration.clientId());
        }
    }

    private void ensureBackendPongSubscription() throws MqttException {
        if (topicHandlers.containsKey("backend/pong")) {
            return;
        }

        subscribeToTopic("backend/pong", payload -> {
            CompletableFuture<String> response = pendingResponses.remove(payload);
            if (response != null) {
                response.complete(payload);
            }
        });
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

    private String statusTopic(String orderId) {
        return "orders/" + orderId + "/status";
    }

    private String deliveryTopic(String orderId) {
        return "orders/" + orderId + "/delivery";
    }

    private String errorTopic(String orderId) {
        return "orders/" + orderId + "/error";
    }

    private String serialCheckTopic(String serial) {
        return "serials/" + serial + "/check";
    }

    private String serialResponseTopic(String serial) {
        return "serials/" + serial;
    }
}
