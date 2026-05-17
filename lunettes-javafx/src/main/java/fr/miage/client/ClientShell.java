package fr.miage.client;

import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;

public class ClientShell {
    private static final List<ProductDefinition> PRODUCTS = List.of(
            new ProductDefinition(
                    TypeLunette.BANANA, 
                    "Bananaaaa",
                    89.99,
                    "Nouveau",
                    "Design iconique des années 50, parfait pour un look vintage et décontracté."
            ),
            new ProductDefinition(
                    TypeLunette.CHATGPT,
                    "BlaBlaBla",
                    74.99,
                    "",
                    "Lunettes style aviateur avec verres polarisés et monture en métal doré."
            ),
            new ProductDefinition(
                    TypeLunette.LE_CHAT,
                    "Miaousse",
                    129.99,
                    "-10%",
                    "Lunettes de vue sophistiquées avec monture fine et design contemporain."
            ),
            new ProductDefinition(
                    TypeLunette.CLAUDE,
                    "Claude",
                    99.99,
                    "Bestseller",
                    "Monture ultra-légère et résistante, idéale pour les activités sportives."
            )
    );

    private final BorderPane root = new BorderPane();
    private final StackPane contentHost = new StackPane();
    private final List<Button> navigationButtons = new ArrayList<>();
    private final List<OrderHistoryEntry> orderHistory = new ArrayList<>();
    private final Map<TypeLunette, IntegerProperty> selectedQuantities = new EnumMap<>(TypeLunette.class);
    private final StringProperty connectionStatus = new SimpleStringProperty("Connexion en cours...");
    private final StringProperty backendStatus = new SimpleStringProperty("Backend en cours de vérification...");
    private final StringProperty orderStatus = new SimpleStringProperty("Aucune commande en attente");
    private final StringProperty publishStatus = new SimpleStringProperty("Aucune commande envoyée");
    private final StringProperty shipmentStatus = new SimpleStringProperty("Aucune expédition suivie");
    private final StringProperty currentOrderId = new SimpleStringProperty("-");
    private final BooleanProperty connectionReady = new SimpleBooleanProperty(false);
    private final BooleanProperty backendAvailable = new SimpleBooleanProperty(false);
    private final BooleanProperty orderPending = new SimpleBooleanProperty(false);
    private final StringProperty notificationMessage = new SimpleStringProperty("");
    private final BooleanProperty notificationSuccess = new SimpleBooleanProperty(true);
    private PauseTransition notificationTimer;
    private final FrontendConfiguration configuration;
    private final FrontendMqttService mqttService;
    private Node homeView;
    private Node catalogueView;
    private Node orderView;
    private Node statusView;
    private VBox historyItems;
    private Node serialsView;
    private Node verifyView;
    private VBox serialItems;
    private final StringProperty serialCheckMessage = new SimpleStringProperty("Entrez un numéro de série pour le vérifier.");
    private final Map<String, List<String>> orderSerials = new LinkedHashMap<>();

    public ClientShell() {
        root.getStyleClass().add("app-shell");
        contentHost.getStyleClass().add("content-host");
        configuration = FrontendConfiguration.fromProperties();
        mqttService = new FrontendMqttService(
                configuration,
                status -> Platform.runLater(() -> connectionStatus.set(status)),
                connected -> Platform.runLater(() -> connectionReady.set(connected))
        );

        for (ProductDefinition product : PRODUCTS) {
            selectedQuantities.put(product.type(), new SimpleIntegerProperty(0));
        }

        root.setLeft(createSidebar());
        root.setCenter(contentHost);

        connectToBroker();
        showView(ViewKey.HOME, null);
    }

    public Parent getRoot() {
        return root;
    }

    public void shutdown() {
        mqttService.close();
    }

    private Node createSidebar() {
        Label eyebrow = new Label("FABRIQUE");
        eyebrow.getStyleClass().add("sidebar-eyebrow");

        Label title = new Label("Maison\nlunettes");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label(
                "Retrouvez les collections, préparez votre commande et consultez son suivi au même endroit."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("sidebar-copy");

        VBox brandBlock = new VBox(10, eyebrow, title, subtitle);
        brandBlock.getStyleClass().add("sidebar-section");

        Label dot = new Label("●");
        dot.getStyleClass().add(connectionReady.get() ? "status-dot-connected" : "status-dot-disconnected");
        connectionReady.addListener((obs, oldVal, newVal) -> Platform.runLater(() -> {
            dot.getStyleClass().removeAll("status-dot-connected", "status-dot-disconnected");
            dot.getStyleClass().add(newVal ? "status-dot-connected" : "status-dot-disconnected");
        }));

        Label statusLabel = createContentLabel("");
        statusLabel.textProperty().bind(connectionStatus);
        statusLabel.getStyleClass().add("sidebar-copy");

        HBox statusRow = new HBox(6, dot, statusLabel);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label brokerLabel = createContentLabel("Broker : " + configuration.brokerUrl());
        brokerLabel.getStyleClass().add("sidebar-copy");

        VBox connectionBlock = new VBox(8, statusRow, brokerLabel);
        connectionBlock.getStyleClass().add("sidebar-section");

        Button homeButton = createNavButton("⌂  Accueil", ViewKey.HOME);
        Button catalogueButton = createNavButton("◉  Catalogue", ViewKey.CATALOGUE);
        Button orderButton = createNavButton("□  Commande", ViewKey.ORDER);
        Button statusButton = createNavButton("◈  Statut", ViewKey.STATUS);
        Button serialsButton = createNavButton("≡  Numeros", ViewKey.SERIALS);
        Button verifyButton = createNavButton("✓  Vérifier", ViewKey.VERIFY);

        VBox navigationBlock = new VBox(10, homeButton, catalogueButton, orderButton, statusButton, serialsButton, verifyButton);
        navigationBlock.getStyleClass().add("sidebar-section");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(20, brandBlock, connectionBlock, navigationBlock, spacer);
        sidebar.setPadding(new Insets(24, 20, 24, 20));
        sidebar.setPrefWidth(280);
        sidebar.setPrefHeight(Double.MAX_VALUE);
        sidebar.getStyleClass().add("sidebar");

        ScrollPane sidebarScroll = new ScrollPane(sidebar);
        sidebarScroll.setFitToWidth(true);
        sidebarScroll.setFitToHeight(true);
        sidebarScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sidebarScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sidebarScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        sidebarScroll.getStyleClass().add("sidebar-scroll");
        sidebarScroll.setPrefWidth(280);
        sidebarScroll.setMaxWidth(280);

        return sidebarScroll;
    }

    private Button createNavButton(String label, ViewKey viewKey) {
        Button button = new Button(label);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(event -> showView(viewKey, button));
        navigationButtons.add(button);
        return button;
    }

    private void showView(ViewKey viewKey, Button activeButton) {
        if (activeButton != null) {
            for (Button button : navigationButtons) {
                button.getStyleClass().remove("nav-button-active");
            }
            activeButton.getStyleClass().add("nav-button-active");
        } else if (!navigationButtons.isEmpty()) {
            navigationButtons.get(0).getStyleClass().add("nav-button-active");
        }

        contentHost.getChildren().setAll(resolveView(viewKey));
    }

    private Node resolveView(ViewKey viewKey) {
        return switch (viewKey) {
            case HOME -> homeView == null ? (homeView = createHomeView()) : homeView;
            case CATALOGUE -> catalogueView == null ? (catalogueView = createCataloguePlaceholder()) : catalogueView;
            case ORDER -> orderView == null ? (orderView = createOrderPlaceholder()) : orderView;
            case STATUS -> statusView == null ? (statusView = createStatusPlaceholder()) : statusView;
            case SERIALS -> serialsView == null ? (serialsView = createSerialsView()) : serialsView;
            case VERIFY -> verifyView == null ? (verifyView = createVerifyView()) : verifyView;
        };
    }

    private ScrollPane createHomeView() {
        Label heroEmoji = new Label("🕶️");
        heroEmoji.getStyleClass().add("hero-emoji");

        Label pageTitle = new Label("Fabrique de Lunettes");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label("Commandez vos lunettes, suivez leur fabrication et recevez vos numéros de série en temps réel.");
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        VBox hero = new VBox(8, heroEmoji, pageTitle, pageCopy);
        hero.getStyleClass().add("section-card");
        hero.setPadding(new Insets(24, 28, 24, 28));

        VBox catalogCard = createSummaryCard("Catalogue", PRODUCTS.size() + " modèles disponibles");

        Label connValue = new Label();
        connValue.textProperty().bind(connectionStatus);
        connValue.getStyleClass().add("summary-value");
        connValue.setWrapText(true);
        VBox connCard = createSectionCard("Connexion", connValue);
        connCard.setPrefWidth(240);

        Label orderValue = new Label();
        orderValue.textProperty().bind(Bindings.createStringBinding(
                () -> currentOrderId.get().equals("-") ? "Aucune commande" : "Commande en cours",
                currentOrderId
        ));
        orderValue.getStyleClass().add("summary-value");
        orderValue.setWrapText(true);
        VBox orderCard = createSectionCard("Suivi actif", orderValue);
        orderCard.setPrefWidth(240);

        HBox statsRow = new HBox(16, catalogCard, connCard, orderCard);
        HBox.setHgrow(catalogCard, Priority.ALWAYS);
        HBox.setHgrow(connCard, Priority.ALWAYS);
        HBox.setHgrow(orderCard, Priority.ALWAYS);

        Button btnCatalogue = new Button("◉  Parcourir le catalogue");
        btnCatalogue.getStyleClass().addAll("action-button", "primary-button");
        btnCatalogue.setMaxWidth(Double.MAX_VALUE);
        btnCatalogue.setOnAction(e -> showView(ViewKey.CATALOGUE, navigationButtons.get(1)));

        Button btnSerials = new Button("≡  Mes numéros de série");
        btnSerials.getStyleClass().addAll("action-button", "outline-button");
        btnSerials.setMaxWidth(Double.MAX_VALUE);
        btnSerials.setOnAction(e -> showView(ViewKey.SERIALS, navigationButtons.get(4)));

        HBox actionsRow = new HBox(16, btnCatalogue, btnSerials);
        HBox.setHgrow(btnCatalogue, Priority.ALWAYS);
        HBox.setHgrow(btnSerials, Priority.ALWAYS);

        return createPage("Accueil", hero, statsRow, actionsRow);
    }

    private ScrollPane createCataloguePlaceholder() {
        Label pageTitle = new Label("Catalogue");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label("Choisissez vos modèles et composez votre commande.");
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        FlowPane productGrid = new FlowPane(18, 18);
        productGrid.getStyleClass().add("product-grid");
        productGrid.setPrefWrapLength(860);

        for (ProductDefinition product : PRODUCTS) {
            productGrid.getChildren().add(createProductCard(product));
        }

        Label selectionValue = new Label();
        selectionValue.textProperty().bind(Bindings.createStringBinding(this::selectionOverviewText, spinnerDependencies()));
        selectionValue.getStyleClass().add("status-copy");
        selectionValue.setWrapText(true);

        TextArea orderSummaryArea = createCopyableArea(orderSummaryText(), "order-summary-area");
        orderSummaryArea.textProperty().bind(Bindings.createStringBinding(this::orderSummaryText, spinnerDependencies()));

        Button orderButton = new Button("Commander maintenant");
        orderButton.getStyleClass().addAll("action-button", "primary-button");
        orderButton.setMaxWidth(Double.MAX_VALUE);
        orderButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> !hasSelectedProducts() || !connectionReady.get() || !backendAvailable.get() || orderPending.get(),
                selectionAndOrderDependencies()
        ));
        orderButton.setOnAction(event -> sendCurrentOrder());

        VBox panierCard = createSectionCard("Votre panier", selectionValue, orderSummaryArea, orderButton);

        return createPage("Catalogue", pageTitle, pageCopy, productGrid, panierCard);
    }

    private ScrollPane createOrderPlaceholder() {
        Label pageTitle = new Label("Ma commande");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label("Suivez l'avancement de votre commande en cours.");
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        // ── Etat vide ──────────────────────────────────────────────────────────
        Label emptyIcon = new Label("📦");
        emptyIcon.setStyle("-fx-font-size: 40px;");
        Label emptyTitle = new Label("Aucune commande en cours");
        emptyTitle.getStyleClass().add("section-title");
        Label emptyMsg = createContentLabel("Parcourez le catalogue et passez votre première commande.");
        Button goToCatalogue = new Button("◉  Parcourir le catalogue");
        goToCatalogue.getStyleClass().addAll("action-button", "primary-button");
        goToCatalogue.setOnAction(e -> showView(ViewKey.CATALOGUE, navigationButtons.get(1)));
        VBox emptyState = createSectionCard("Commande", emptyIcon, emptyTitle, emptyMsg, goToCatalogue);

        // ── Commande active ────────────────────────────────────────────────────
        Label refValue = new Label();
        refValue.getStyleClass().add("order-ref-short");
        refValue.textProperty().bind(currentOrderId);

        Node timeline = createStatusTimeline();

        Label statusValue = new Label();
        statusValue.textProperty().bind(orderStatus);
        statusValue.getStyleClass().add("status-copy");
        statusValue.setWrapText(true);

        Label shipValue = new Label();
        shipValue.textProperty().bind(Bindings.concat("Expedition : ", shipmentStatus));
        shipValue.getStyleClass().add("section-copy");
        shipValue.setWrapText(true);

        VBox activeState = createSectionCard("Commande en cours",
                createContentLabel("Reference"), refValue, timeline, statusValue, shipValue);

        var hasOrder = Bindings.createBooleanBinding(
                () -> !currentOrderId.get().equals("-"), currentOrderId);
        emptyState.visibleProperty().bind(hasOrder.not());
        emptyState.managedProperty().bind(hasOrder.not());
        activeState.visibleProperty().bind(hasOrder);
        activeState.managedProperty().bind(hasOrder);

        return createPage("Commande", pageTitle, pageCopy, emptyState, activeState);
    }

    private ScrollPane createStatusPlaceholder() {
        Label pageTitle = new Label("Statut");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label("État de la connexion et historique des commandes.");
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        // ── Connexion ──────────────────────────────────────────────────────────
        Label brokerDot = new Label("●");
        brokerDot.getStyleClass().add(connectionReady.get() ? "status-dot-connected" : "status-dot-disconnected");
        connectionReady.addListener((obs, o, n) -> Platform.runLater(() -> {
            brokerDot.getStyleClass().removeAll("status-dot-connected", "status-dot-disconnected");
            brokerDot.getStyleClass().add(n ? "status-dot-connected" : "status-dot-disconnected");
        }));
        Label brokerText = new Label();
        brokerText.textProperty().bind(Bindings.concat("Broker  —  ", connectionStatus));
        brokerText.getStyleClass().add("status-copy");
        HBox brokerRow = new HBox(10, brokerDot, brokerText);
        brokerRow.setAlignment(Pos.CENTER_LEFT);

        Label backendDot = new Label("●");
        backendDot.getStyleClass().add(backendAvailable.get() ? "status-dot-connected" : "status-dot-disconnected");
        backendAvailable.addListener((obs, o, n) -> Platform.runLater(() -> {
            backendDot.getStyleClass().removeAll("status-dot-connected", "status-dot-disconnected");
            backendDot.getStyleClass().add(n ? "status-dot-connected" : "status-dot-disconnected");
        }));
        Label backendText = new Label();
        backendText.textProperty().bind(Bindings.concat("Backend  —  ", backendStatus));
        backendText.getStyleClass().add("status-copy");
        HBox backendRow = new HBox(10, backendDot, backendText);
        backendRow.setAlignment(Pos.CENTER_LEFT);

        VBox healthCard = createSectionCard("Connexion", brokerRow, backendRow);

        // ── Historique ─────────────────────────────────────────────────────────
        historyItems = new VBox(10);
        historyItems.getStyleClass().add("history-list");
        refreshHistoryView();

        VBox historyCard = createSectionCard("Historique des commandes", historyItems);

        return createPage("Statut", pageTitle, pageCopy, healthCard, historyCard);
    }

    private ScrollPane createPage(String headerLabel, Node... sections) {
        Label header = new Label(headerLabel);
        header.getStyleClass().add("page-label");

        Label notificationBanner = createNotificationBanner();

        VBox content = new VBox(16);
        content.getChildren().addAll(notificationBanner, header);
        content.getChildren().addAll(sections);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.setStyle("-fx-background-color: transparent;");
        content.getStyleClass().add("page-content");

        ScrollPane pageScroll = new ScrollPane(content);
        pageScroll.setFitToWidth(true);
        pageScroll.setFitToHeight(true);
        pageScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        pageScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        pageScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        pageScroll.getStyleClass().add("page-scroll");
        return pageScroll;
    }

    private VBox createSummaryCard(String titleText, String bodyText) {
        VBox card = createSectionCard(titleText, createValueLabel(bodyText, "summary-value"));
        card.setPrefWidth(240);
        return card;
    }

    private VBox createBoundSummaryCard(String titleText, java.util.function.Supplier<String> textSupplier) {
        Label valueLabel = createValueLabel(textSupplier.get(), "summary-value");
        valueLabel.textProperty().bind(Bindings.createStringBinding(textSupplier::get, spinnerDependencies()));

        VBox card = createSectionCard(titleText, valueLabel);
        card.setPrefWidth(240);
        return card;
    }

    private VBox createSectionCard(String titleText, String bodyText) {
        return createSectionCard(titleText, createContentLabel(bodyText));
    }

    private VBox createSectionCard(String titleText, Node... contentNodes) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        VBox card = new VBox(10, title);
        card.getChildren().addAll(contentNodes);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("section-card");
        return card;
    }

    private VBox createProductCard(ProductDefinition product) {
        Node productVisual = createProductImageArea(product.type());

        Label codeLabel = new Label(product.type().name());
        codeLabel.getStyleClass().add("product-code");

        HBox header = new HBox(8, codeLabel);
        header.setAlignment(Pos.CENTER_LEFT);

        if (!product.badge().isBlank()) {
            Label badgeLabel = new Label(product.badge());
            badgeLabel.getStyleClass().add("product-badge");
            header.getChildren().add(badgeLabel);
        }

        Label nameLabel = new Label(product.displayName());
        nameLabel.getStyleClass().add("product-name");

        Label descriptionLabel = createContentLabel(product.description());
        descriptionLabel.getStyleClass().add("product-description");

        Label priceLabel = createValueLabel(formatPrice(product.price()), "product-price");

        Spinner<Integer> quantitySpinner = createQuantitySpinner(product.type());

        Label quantityLabel = new Label("Quantité");
        quantityLabel.getStyleClass().add("field-label");

        VBox quantityBox = new VBox(8, quantityLabel, quantitySpinner);
        quantityBox.getStyleClass().add("quantity-box");

        VBox textContent = new VBox(10, header, nameLabel, descriptionLabel, priceLabel, quantityBox);
        textContent.setPadding(new Insets(14, 16, 16, 16));

        VBox card = new VBox(0, productVisual, textContent);
        card.setPrefWidth(270);
        card.getStyleClass().add("product-card");
        return card;
    }

    private Node createProductImageArea(TypeLunette type) {
        String resourcePath = switch (type) {
            case BANANA -> "/banana.png";
            case CHATGPT -> "/chatgpt.png";
            case LE_CHAT -> "/le_chat.png";
            case CLAUDE -> "/claude.png";
        };

        try {
            var url = getClass().getResource(resourcePath);
            if (url != null) {
                Image image = new Image(url.toExternalForm(), 270, 160, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(270);
                imageView.setFitHeight(150);
                imageView.setPreserveRatio(true);
                imageView.setSmooth(true);

                StackPane wrapper = new StackPane(imageView);
                wrapper.setStyle(
                        "-fx-background-color: " + getProductAccentBg(type) + "; " +
                        "-fx-background-radius: 18 18 0 0; " +
                        "-fx-padding: 12;"
                );
                wrapper.setPrefHeight(160);
                wrapper.setMaxWidth(Double.MAX_VALUE);
                return wrapper;
            }
        } catch (Exception ignored) {
        }

        Label fallback = new Label(getProductEmoji(type));
        fallback.setStyle(
                "-fx-font-size: 36px; " +
                "-fx-background-color: " + getProductAccentBg(type) + "; " +
                "-fx-background-radius: 18 18 0 0; " +
                "-fx-alignment: center; " +
                "-fx-padding: 28 0;"
        );
        fallback.setMaxWidth(Double.MAX_VALUE);
        return fallback;
    }

    private String getProductEmoji(TypeLunette type) {
        return switch (type) {
            case BANANA -> "🍌";
            case CHATGPT -> "🤖";
            case LE_CHAT -> "🐱";
            case CLAUDE -> "⚡";
        };
    }

    private String getProductAccentBg(TypeLunette type) {
        return switch (type) {
            case BANANA -> "#FEF9C3";
            case CHATGPT -> "#D1FAE5";
            case LE_CHAT -> "#EDE9FE";
            case CLAUDE -> "#DBEAFE";
        };
    }

    private Spinner<Integer> createQuantitySpinner(TypeLunette type) {
        IntegerProperty quantityProperty = selectedQuantities.get(type);
        Spinner<Integer> spinner = new Spinner<>();
        SpinnerValueFactory.IntegerSpinnerValueFactory valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 9, quantityProperty.get());
        spinner.setValueFactory(valueFactory);
        valueFactory.valueProperty().bindBidirectional(quantityProperty.asObject());
        spinner.setEditable(false);
        spinner.setPrefWidth(110);
        spinner.getStyleClass().add("quantity-spinner");
        return spinner;
    }

    private Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add("section-copy");
        return label;
    }

    private Label createValueLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private TextField createCopyableField(String text, String styleClass) {
        TextField field = new TextField(text);
        field.setEditable(false);
        field.setMouseTransparent(false);
        field.setFocusTraversable(false);
        field.getStyleClass().add(styleClass);
        return field;
    }

    private TextArea createCopyableArea(String text, String styleClass) {
        TextArea area = new TextArea(text);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefRowCount(5);
        area.setMouseTransparent(false);
        area.setFocusTraversable(false);
        area.getStyleClass().add(styleClass);
        return area;
    }

    private Label createNotificationBanner() {
        Label notification = new Label();
        notification.setWrapText(true);
        notification.setVisible(false);
        notification.setManaged(false);
        notification.getStyleClass().add("notification-banner");
        notification.textProperty().bind(notificationMessage);
        notification.visibleProperty().bind(notificationMessage.isNotEmpty());
        notification.managedProperty().bind(notification.visibleProperty());

        notificationMessage.addListener((obs, oldValue, newValue) -> updateNotificationStyle(notification));
        notificationSuccess.addListener((obs, oldValue, newValue) -> updateNotificationStyle(notification));
        updateNotificationStyle(notification);
        return notification;
    }

    private void updateNotificationStyle(Label notification) {
        notification.getStyleClass().removeAll("notification-success", "notification-error");
        notification.getStyleClass().add(notificationSuccess.get() ? "notification-success" : "notification-error");
    }

    private void showNotification(String message, boolean success) {
        notificationSuccess.set(success);
        notificationMessage.set(message == null ? "" : message);
        if (notificationTimer != null) {
            notificationTimer.stop();
        }
        notificationTimer = new PauseTransition(Duration.seconds(4));
        notificationTimer.setOnFinished(event -> notificationMessage.set(""));
        notificationTimer.play();
    }

    private ScrollPane createSerialsView() {
        Label pageTitle = new Label("Numéros de série");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label("Numéros de série reçus pour chaque commande fabriquée.");
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        serialItems = new VBox(12);
        serialItems.getStyleClass().add("serial-list");
        refreshSerialsView();

        VBox serialsCard = createSectionCard("Commandes", serialItems);

        return createPage("Numeros", pageTitle, pageCopy, serialsCard);
    }

    private ScrollPane createVerifyView() {
        Label pageTitle = new Label("Vérifier un numéro");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label("Saisissez un numéro de série pour vérifier s'il a été fabriqué.");
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        TextField input = new TextField();
        input.getStyleClass().add("serial-input");
        input.setPromptText("Ex : SN-001-ABC-XYZ");

        Label result = new Label();
        result.textProperty().bind(serialCheckMessage);
        result.getStyleClass().add("status-copy");
        result.setWrapText(true);
        result.setVisible(false);
        result.managedProperty().bind(result.visibleProperty());

        Button check = new Button("✓  Vérifier");
        check.getStyleClass().addAll("action-button", "primary-button");
        check.setMaxWidth(Double.MAX_VALUE);
        check.disableProperty().bind(Bindings.createBooleanBinding(
                () -> input.getText() == null || input.getText().isBlank(),
                input.textProperty()
        ));

        check.setOnAction(e -> {
            String serial = input.getText() == null ? "" : input.getText().trim();
            serialCheckMessage.set("Verification en cours...");
            result.setVisible(true);

            try {
                mqttService.checkSerial(serial, response -> Platform.runLater(() -> {
                    if (response == null || response.isBlank() || "invalid".equalsIgnoreCase(response.trim())) {
                        serialCheckMessage.set("Numero invalide.");
                    } else {
                        serialCheckMessage.set("Numero valide : " + response.trim() + ".");
                    }
                }));
            } catch (RuntimeException exception) {
                serialCheckMessage.set("Verification impossible : " + exception.getMessage());
            }
        });

        input.textProperty().addListener((obs, o, n) -> result.setVisible(false));

        VBox verificationCard = createSectionCard("Vérification", input, check, result);

        return createPage("Vérifier", pageTitle, pageCopy, verificationCard);
    }

    private Node createStatusTimeline() {
        String[] stepNames = {"Envoyee", "Validee", "Fabriquee", "Expediee"};
        Label[] dots = new Label[4];
        Label[] texts = new Label[4];
        Region[] connectors = new Region[3];

        for (int i = 0; i < 4; i++) {
            dots[i] = new Label();
            texts[i] = new Label(stepNames[i]);
        }
        for (int i = 0; i < 3; i++) {
            connectors[i] = new Region();
            connectors[i].setPrefHeight(2);
            connectors[i].setMinWidth(20);
            HBox.setHgrow(connectors[i], Priority.ALWAYS);
        }

        Runnable update = () -> {
            int step = computeCurrentStep(orderStatus.get());
            boolean cancelled = orderStatus.get() != null && orderStatus.get().toLowerCase().contains("annul");
            for (int i = 0; i < 4; i++) {
                updateStepDot(dots[i], texts[i], i, step, cancelled);
            }
            for (int i = 0; i < 3; i++) {
                boolean done = !cancelled && i < step;
                connectors[i].setStyle("-fx-background-color: " + (done ? "#22C55E" : "#E2E8F0") + ";");
            }
        };
        orderStatus.addListener((obs, o, n) -> Platform.runLater(update));
        update.run();

        HBox timeline = new HBox();
        timeline.setAlignment(Pos.CENTER_LEFT);
        timeline.setPadding(new Insets(12, 0, 4, 0));

        for (int i = 0; i < 4; i++) {
            VBox step = new VBox(6, dots[i], texts[i]);
            step.setAlignment(Pos.TOP_CENTER);
            step.setMinWidth(64);
            timeline.getChildren().add(step);
            if (i < 3) {
                VBox wrap = new VBox(connectors[i]);
                wrap.setAlignment(Pos.CENTER);
                wrap.setPrefHeight(32);
                HBox.setHgrow(wrap, Priority.ALWAYS);
                timeline.getChildren().add(wrap);
            }
        }
        return timeline;
    }

    private int computeCurrentStep(String status) {
        if (status == null) return 0;
        String lc = status.toLowerCase();
        if (lc.contains("exped") || lc.contains("livr")) return 3;
        if (lc.contains("fabr") || lc.contains("serie") || lc.contains("termin")) return 2;
        if (lc.contains("valid")) return 1;
        return 0;
    }

    private void updateStepDot(Label dot, Label text, int index, int currentStep, boolean cancelled) {
        boolean done = !cancelled && index < currentStep;
        boolean active = !cancelled && index == currentStep;
        boolean failed = cancelled && index <= currentStep;
        String dotColor = failed ? "#EF4444" : done ? "#22C55E" : active ? "#3B82F6" : "#CBD5E1";
        String textColor = failed ? "#EF4444" : done ? "#166534" : active ? "#2563EB" : "#94A3B8";
        String fw = (done || active || failed) ? "700" : "500";
        dot.setText(done ? "✓" : failed ? "✕" : String.valueOf(index + 1));
        dot.setStyle(
                "-fx-background-color: " + dotColor + "; " +
                "-fx-text-fill: white; " +
                "-fx-font-weight: " + fw + "; " +
                "-fx-font-size: 11px; " +
                "-fx-background-radius: 999; " +
                "-fx-padding: 5 9; " +
                "-fx-min-width: 28; " +
                "-fx-alignment: center;"
        );
        text.setStyle("-fx-text-fill: " + textColor + "; -fx-font-size: 11px; -fx-font-weight: " + fw + ";");
    }

    private Observable[] spinnerDependencies() {
        return selectedQuantities.values().stream()
                .map(quantity -> (Observable) quantity)
                .toArray(Observable[]::new);
    }

    private void refreshSerialsView() {
        if (serialItems == null) {
            return;
        }

        if (orderHistory.isEmpty()) {
            serialItems.getChildren().setAll(createContentLabel("Aucune commande enregistrée pour le moment."));
            return;
        }

        List<Node> nodes = new ArrayList<>();

        for (OrderHistoryEntry entry : orderHistory) {
            nodes.add(createSerialItem(entry, orderSerials.getOrDefault(entry.orderId(), List.of())));
        }

        serialItems.getChildren().setAll(nodes);
    }

    private Observable[] selectionAndOrderDependencies() {
        Observable[] dependencies = Arrays.copyOf(spinnerDependencies(), selectedQuantities.size() + 3);
        dependencies[dependencies.length - 3] = connectionReady;
        dependencies[dependencies.length - 2] = backendAvailable;
        dependencies[dependencies.length - 1] = orderPending;
        return dependencies;
    }

    private boolean hasSelectedProducts() {
        return selectedQuantities.values().stream().anyMatch(quantity -> quantity.get() > 0);
    }

    private String selectionOverviewText() {
        List<String> selections = new ArrayList<>();
        int total = 0;

        for (ProductDefinition product : PRODUCTS) {
            int quantity = selectedQuantities.get(product.type()).get();

            if (quantity <= 0) {
                continue;
            }

            total += quantity;
            selections.add(product.displayName() + " x" + quantity);
        }

        if (total == 0) {
            return "Aucune paire selectionnée";
        }

        return total + " paire(s) choisie(s) : " + String.join(", ", selections);
    }

    private String orderSummaryText() {
        StringBuilder builder = new StringBuilder();
        double totalPrice = 0.0;

        for (ProductDefinition product : PRODUCTS) {
            int quantity = selectedQuantities.get(product.type()).get();

            if (quantity <= 0) {
                continue;
            }

            double linePrice = quantity * product.price();
            totalPrice += linePrice;

            if (!builder.isEmpty()) {
                builder.append("\n");
            }

            builder.append(product.displayName())
                    .append(" x")
                    .append(quantity)
                    .append(" - ")
                    .append(formatPrice(linePrice));
        }

        if (builder.isEmpty()) {
            return "Aucune paire n'a encore été ajoutée à la commande.";
        }

        builder.append("\n\nTotal estimé : ").append(formatPrice(totalPrice));
        return builder.toString();
    }

    private String compactOrderSummary(Map<TypeLunette, Integer> orderLines) {
        List<String> lines = new ArrayList<>();

        for (ProductDefinition product : PRODUCTS) {
            int quantity = orderLines.getOrDefault(product.type(), 0);
            if (quantity > 0) {
                lines.add(product.displayName() + " x" + quantity);
            }
        }

        return lines.isEmpty() ? "Aucune paire selectionnée" : String.join(", ", lines);
    }

    private String formatPrice(double price) {
        return String.format("%.2f EUR", price);
    }

    private void connectToBroker() {
        connectToBroker(false);
    }

    private void connectToBroker(boolean manualRetry) {
        connectionStatus.set(manualRetry ? "Reconnexion..." : "Connexion en cours...");

        if (!orderPending.get()) {
            publishStatus.set(manualRetry ? "Nouvelle tentative de connexion" : "Connexion en cours...");
        }

        try {
            mqttService.start();
            connectionReady.set(true);

            boolean backendOk = mqttService.pingBackend(2000);
            backendAvailable.set(backendOk);
            backendStatus.set(backendOk ? "Backend disponible" : "Backend indisponible");
            showNotification(backendOk ? "Connexion et backend vérifiés" : "Broker connecte, backend indisponible", backendOk);

            if (!orderPending.get()) {
                publishStatus.set(backendOk ? "Connexion prête pour l'envoi" : "Broker connecte, backend non joignable");
            }
        } catch (RuntimeException exception) {
            connectionReady.set(false);
            backendAvailable.set(false);
            backendStatus.set("Backend indisponible");
            connectionStatus.set("Connexion impossible");
            showNotification("Impossible de joindre le broker", false);

            if (!orderPending.get()) {
                publishStatus.set("Impossible de joindre le broker");
            }
        }
    }

    private void sendCurrentOrder() {
        Map<TypeLunette, Integer> orderLines = new EnumMap<>(TypeLunette.class);

        for (ProductDefinition product : PRODUCTS) {
            int quantity = selectedQuantities.get(product.type()).get();
            if (quantity > 0) {
                orderLines.put(product.type(), quantity);
            }
        }

        if (orderLines.isEmpty()) {
            orderPending.set(false);
            orderStatus.set("Aucune commande en attente");
            publishStatus.set("Sélectionnez au moins une paire avant l'envoi");
            shipmentStatus.set("Aucune expédition suivie");
            currentOrderId.set("-");
            showNotification("Sélectionnez une paire avant de commander", false);
            return;
        }

        if (!connectionReady.get()) {
            orderPending.set(false);
            orderStatus.set("Pas de connexion au broker");
            publishStatus.set("Impossible d'envoyer sans broker");
            shipmentStatus.set("Aucune expédition lancée");
            showNotification("Connexion MQTT hors ligne, impossible d'envoyer la commande", false);
            return;
        }

        if (!backendAvailable.get() && !mqttService.pingBackend(2000)) {
            backendAvailable.set(false);
            backendStatus.set("Backend indisponible");
            orderPending.set(false);
            orderStatus.set("Backend non disponible");
            publishStatus.set("Impossible d'envoyer : backend absent");
            shipmentStatus.set("Aucune expédition lancée");
            showNotification("Backend indisponible, activez le backend avant d'envoyer la commande", false);
            return;
        }

        String orderId = UUID.randomUUID().toString();
        currentOrderId.set(orderId);

        try {
            mqttService.sendOrder(
                    orderId,
                    new Commande(orderLines),
                    () -> onOrderValidated(orderId),
                    reason -> onOrderCancelled(orderId, reason),
                    status -> onOrderStatus(orderId, status),
                    delivery -> onDeliveryReceived(orderId, delivery),
                    error -> onOrderError(orderId, error)
            );
            orderPending.set(true);
            orderStatus.set("En attente de validation");
            publishStatus.set("Commande envoyée a l'atelier");
            shipmentStatus.set("Expedition en attente");
            recordOrderHistory(orderId, compactOrderSummary(orderLines), "En attente de validation", "Commande envoyée à l'atelier");
            showNotification("Commande envoyée, attente de validation", true);
        } catch (RuntimeException exception) {
            orderPending.set(false);
            orderStatus.set("Envoi non confirmé");
            publishStatus.set("Envoi impossible : " + exception.getMessage());
            shipmentStatus.set("Expedition non lancée");
            showNotification("Échec de l'envoi : " + exception.getMessage(), false);
        }
    }

    private void onDeliveryReceived(String orderId, String payload) {
        Platform.runLater(() -> {
            List<String> serials = Arrays.stream(payload.split(","))
                    .map(String::trim)
                    .map(this::extractSerialFromDeliveryLine)
                    .filter(serial -> !serial.isBlank())
                    .toList();
            orderSerials.put(orderId, serials);
            updateOrderHistory(orderId, "Commande expediée", serials.size() + " numéro(s) de série reçu(s)");

            if (orderId.equals(currentOrderId.get())) {
                orderPending.set(false);
                orderStatus.set("Commande expediée");
                publishStatus.set(serials.size() + " numéro(s) de série reçus");
                shipmentStatus.set("Livraison recue");
                showNotification("La commande a ete livree", true);
            }

            refreshSerialsView();
        });
    }

    private String extractSerialFromDeliveryLine(String deliveryLine) {
        int separatorIndex = deliveryLine.indexOf(':');
        if (separatorIndex < 0 || separatorIndex == deliveryLine.length() - 1) {
            return deliveryLine;
        }
        return deliveryLine.substring(separatorIndex + 1).trim();
    }

    private void onOrderValidated(String orderId) {
        Platform.runLater(() -> {
            updateOrderHistory(orderId, "Commande validée", "Commande acceptée par l'atelier");

            if (orderId.equals(currentOrderId.get())) {
                orderStatus.set("Commande validée");
                publishStatus.set("Commande acceptée par l'atelier");
                shipmentStatus.set("Expedition en attente de fabrication");
                showNotification("La commande a ete validée par l'atelier", true);
            }
        });
    }

    private void onOrderCancelled(String orderId, String reason) {
        Platform.runLater(() -> {
            String detail = reason == null || reason.isBlank()
                    ? "Commande annulée par l'atelier"
                    : reason;

            updateOrderHistory(orderId, "Commande annulée", detail);

            if (orderId.equals(currentOrderId.get())) {
                orderPending.set(false);
                orderStatus.set("Commande annulée");
                publishStatus.set(detail);
                shipmentStatus.set("Aucune expédition lancée");
                showNotification("La commande a été annulée : " + detail, false);
            }
        });
    }

    private void onOrderStatus(String orderId, String status) {
        Platform.runLater(() -> {
            String normalizedStatus = status == null ? "" : status.trim();
            String detail = switch (normalizedStatus) {
                case "processing" -> "Fabrication demarree";
                case "processed" -> "Fabrication terminee";
                default -> "Statut recu : " + normalizedStatus;
            };

            updateOrderHistory(orderId, detail, detail);

            if (orderId.equals(currentOrderId.get())) {
                orderStatus.set(detail);
                publishStatus.set(detail);
                shipmentStatus.set(detail);
            }
        });
    }

    private void onOrderError(String orderId, String payload) {
        Platform.runLater(() -> {
            String detail = payload == null || payload.isBlank()
                    ? "Erreur pendant le traitement de la commande"
                    : payload;

            updateOrderHistory(orderId, "Erreur commande", detail);

            if (orderId.equals(currentOrderId.get())) {
                orderPending.set(false);
                orderStatus.set("Erreur commande");
                publishStatus.set(detail);
                shipmentStatus.set("Aucune livraison");
                showNotification("Erreur commande : " + detail, false);
            }
        });
    }

    private void recordOrderHistory(String orderId, String summary, String status, String detail) {
        orderHistory.add(0, new OrderHistoryEntry(orderId, summary, status, detail));
        refreshHistoryView();
        refreshSerialsView();
    }

    private void updateOrderHistory(String orderId, String status, String detail) {
        for (int index = 0; index < orderHistory.size(); index++) {
            OrderHistoryEntry entry = orderHistory.get(index);
            if (!entry.orderId().equals(orderId)) {
                continue;
            }

            orderHistory.set(index, new OrderHistoryEntry(orderId, entry.summary(), status, detail));
            refreshHistoryView();
            refreshSerialsView();
            return;
        }
    }

    private OrderHistoryEntry findOrderHistoryEntry(String orderId) {
        for (OrderHistoryEntry entry : orderHistory) {
            if (entry.orderId().equals(orderId)) {
                return entry;
            }
        }

        return null;
    }

    private void refreshHistoryView() {
        if (historyItems == null) {
            return;
        }

        if (orderHistory.isEmpty()) {
            historyItems.getChildren().setAll(createContentLabel("Aucune commande enregistrée pour le moment."));
            return;
        }

        List<Node> nodes = new ArrayList<>();
        for (OrderHistoryEntry entry : orderHistory) {
            nodes.add(createHistoryItem(entry));
        }
        historyItems.getChildren().setAll(nodes);
    }

    private VBox createHistoryItem(OrderHistoryEntry entry) {
        String shortId = entry.orderId().length() > 13
                ? entry.orderId().substring(0, 13) + "…"
                : entry.orderId();

        Label referenceLabel = new Label(shortId);
        referenceLabel.getStyleClass().add("order-ref-short");

        Label statusBadge = createStatusBadge(entry.status());

        HBox headerRow = new HBox(10, referenceLabel, statusBadge);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label summaryLabel = createContentLabel(entry.summary());
        summaryLabel.getStyleClass().add("history-summary");

        Label detailLabel = createContentLabel(entry.detail());
        detailLabel.getStyleClass().add("history-detail");

        VBox item = new VBox(8, headerRow, summaryLabel, detailLabel);
        item.getStyleClass().add("history-item");
        return item;
    }

    private VBox createSerialItem(OrderHistoryEntry entry, List<String> serials) {
        String shortId = entry.orderId().length() > 13
                ? entry.orderId().substring(0, 13) + "…"
                : entry.orderId();

        Label referenceLabel = new Label(shortId);
        referenceLabel.getStyleClass().add("order-ref-short");

        Label statusBadge = createStatusBadge(entry.status());

        HBox headerRow = new HBox(10, referenceLabel, statusBadge);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label summaryLabel = createContentLabel(entry.summary());
        summaryLabel.getStyleClass().add("history-summary");

        VBox serialValues = new VBox(6);
        serialValues.getStyleClass().add("serial-values");

        if (serials.isEmpty()) {
            Label emptyLabel = createContentLabel("Aucun numéro reçu pour cette commande.");
            emptyLabel.getStyleClass().add("serial-empty");
            serialValues.getChildren().add(emptyLabel);
        } else {
            Label countLabel = new Label(serials.size() + " numéro(s) de série reçu(s)");
            countLabel.getStyleClass().add("serials-count-label");
            serialValues.getChildren().add(countLabel);
            for (String serial : serials) {
                serialValues.getChildren().add(createSerialChip(serial));
            }
        }

        VBox item = new VBox(10, headerRow, summaryLabel, serialValues);
        item.getStyleClass().add("history-item");
        return item;
    }

    private HBox createSerialChip(String serial) {
        Label serialLabel = new Label(serial);
        serialLabel.getStyleClass().add("serial-chip");
        serialLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(serialLabel, Priority.ALWAYS);

        Button copyBtn = new Button("⎘");
        copyBtn.getStyleClass().add("copy-button");
        copyBtn.setOnAction(e -> {
            copyToClipboard(serial);
            copyBtn.setText("✓");
            PauseTransition pt = new PauseTransition(Duration.seconds(1.5));
            pt.setOnFinished(ev -> copyBtn.setText("⎘"));
            pt.play();
        });

        HBox chip = new HBox(8, serialLabel, copyBtn);
        chip.setAlignment(Pos.CENTER_LEFT);
        return chip;
    }

    private Label createStatusBadge(String status) {
        Label badge = new Label(status);
        String lc = status == null ? "" : status.toLowerCase();
        if (lc.contains("annul") || lc.contains("echec")) {
            badge.getStyleClass().add("status-badge-error");
        } else if (lc.contains("exped") || lc.contains("livr")) {
            badge.getStyleClass().add("status-badge-shipped");
        } else if (lc.contains("valid") || lc.contains("fabr") || lc.contains("serie") || lc.contains("termin")) {
            badge.getStyleClass().add("status-badge-ok");
        } else {
            badge.getStyleClass().add("status-badge-pending");
        }
        return badge;
    }

    private void copyToClipboard(String text) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        clipboard.setContent(content);
    }

    private enum ViewKey {
        HOME,
        CATALOGUE,
        ORDER,
        STATUS,
        SERIALS,
        VERIFY
    }

    private record OrderHistoryEntry(String orderId, String summary, String status, String detail) {
    }
}
