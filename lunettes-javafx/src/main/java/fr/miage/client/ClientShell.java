package fr.miage.client;

import fr.miage.shared.Commande;
import fr.miage.shared.TypeLunette;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ClientShell {
    private static final List<ProductDefinition> PRODUCTS = List.of(
            new ProductDefinition(
                    TypeLunette.BANANA,
                    "Bananaaaa",
                    89.99,
                    "Nouveau",
                    "Design iconique des annees 50, parfait pour un look vintage et decontracte."
            ),
            new ProductDefinition(
                    TypeLunette.CHATGPT,
                    "BlaBlaBla",
                    74.99,
                    "",
                    "Lunettes style aviateur avec verres polarises et monture en metal dore."
            ),
            new ProductDefinition(
                    TypeLunette.LE_CHAT,
                    "Miaousse",
                    129.99,
                    "-10%",
                    "Lunettes de vue sophistiquees avec monture fine et design contemporain."
            ),
            new ProductDefinition(
                    TypeLunette.CLAUDE,
                    "Claude",
                    99.99,
                    "Bestseller",
                    "Monture ultra-legere et resistante, ideale pour les activites sportives."
            )
    );

    private final BorderPane root = new BorderPane();
    private final StackPane contentHost = new StackPane();
    private final List<Button> navigationButtons = new ArrayList<>();
    private final Map<TypeLunette, IntegerProperty> selectedQuantities = new EnumMap<>(TypeLunette.class);
    private final StringProperty connectionStatus = new SimpleStringProperty("Connexion en cours...");
    private final StringProperty publishStatus = new SimpleStringProperty("Aucune commande envoyee");
    private final StringProperty currentOrderId = new SimpleStringProperty("-");
    private final FrontendConfiguration configuration;
    private final FrontendMqttService mqttService;
    private VBox homeView;
    private VBox catalogueView;
    private VBox orderView;
    private VBox statusView;

    public ClientShell() {
        root.getStyleClass().add("app-shell");
        contentHost.getStyleClass().add("content-host");
        configuration = FrontendConfiguration.fromProperties();
        mqttService = new FrontendMqttService(configuration, status ->
                Platform.runLater(() -> connectionStatus.set(status))
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

    private VBox createSidebar() {
        Label eyebrow = new Label("FABRIQUE");
        eyebrow.getStyleClass().add("sidebar-eyebrow");

        Label title = new Label("Maison\nlunettes");
        title.getStyleClass().add("sidebar-title");

        Label subtitle = new Label(
                "Retrouvez les collections, preparez votre commande et consultez son suivi au meme endroit."
        );
        subtitle.setWrapText(true);
        subtitle.getStyleClass().add("sidebar-copy");

        VBox brandBlock = new VBox(10, eyebrow, title, subtitle);
        brandBlock.getStyleClass().add("sidebar-section");

        Label connectionTitle = new Label("Connexion");
        connectionTitle.getStyleClass().add("sidebar-section-title");

        Label connectionValue = createValueLabel(connectionStatus.get(), "status-pill");
        connectionValue.textProperty().bind(connectionStatus);

        Label brokerLabel = createContentLabel("Broker : " + configuration.brokerUrl());

        VBox connectionBlock = new VBox(8, connectionTitle, connectionValue, brokerLabel);
        connectionBlock.getStyleClass().add("sidebar-section");

        Button homeButton = createNavButton("Accueil", ViewKey.HOME);
        Button catalogueButton = createNavButton("Catalogue", ViewKey.CATALOGUE);
        Button orderButton = createNavButton("Commande", ViewKey.ORDER);
        Button statusButton = createNavButton("Statut", ViewKey.STATUS);

        VBox navigationBlock = new VBox(10, homeButton, catalogueButton, orderButton, statusButton);
        navigationBlock.getStyleClass().add("sidebar-section");

        Label footerTitle = new Label("Selection du jour");
        footerTitle.getStyleClass().add("sidebar-section-title");

        Label footerCopy = new Label(
                "Choisissez un modele, composez votre demande et gardez un oeil sur son avancement."
        );
        footerCopy.setWrapText(true);
        footerCopy.getStyleClass().add("sidebar-copy");

        VBox footerBlock = new VBox(8, footerTitle, footerCopy);
        footerBlock.getStyleClass().add("sidebar-section");

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox sidebar = new VBox(24, brandBlock, connectionBlock, navigationBlock, spacer, footerBlock);
        sidebar.setPadding(new Insets(28, 24, 28, 24));
        sidebar.setPrefWidth(280);
        sidebar.getStyleClass().add("sidebar");
        return sidebar;
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
        };
    }

    private VBox createHomeView() {
        Label pageTitle = new Label("Bienvenue");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label(
                "Accedez rapidement aux collections, preparez votre commande et consultez son statut depuis un seul espace."
        );
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        HBox summaryCards = new HBox(
                16,
                createSummaryCard("Catalogue", PRODUCTS.size() + " modeles disponibles"),
                createBoundSummaryCard("Selection", this::selectionOverviewText),
                createBoundSummaryCard("Connexion", connectionStatus::get)
        );
        HBox.setHgrow(summaryCards.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(summaryCards.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(summaryCards.getChildren().get(2), Priority.ALWAYS);

        VBox milestoneCard = createSectionCard(
                "Esprit de la maison",
                "Des lignes claires, une navigation simple et un parcours fluide pour preparer chaque commande."
        );

        VBox sharedInfoCard = createSectionCard(
                "Collections",
                "Retrouvez les modeles de la fabrique, leurs details et les informations utiles avant de finaliser votre choix."
        );

        HBox lowerRow = new HBox(16, milestoneCard, sharedInfoCard);
        HBox.setHgrow(milestoneCard, Priority.ALWAYS);
        HBox.setHgrow(sharedInfoCard, Priority.ALWAYS);

        return createPage("Accueil", pageTitle, pageCopy, summaryCards, lowerRow);
    }

    private VBox createCataloguePlaceholder() {
        Label pageTitle = new Label("Catalogue");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label(
                "Explorez les collections disponibles et choisissez la paire qui vous correspond."
        );
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        FlowPane productGrid = new FlowPane(18, 18);
        productGrid.getStyleClass().add("product-grid");
        productGrid.setPrefWrapLength(860);

        for (ProductDefinition product : PRODUCTS) {
            productGrid.getChildren().add(createProductCard(product));
        }

        VBox selectionCard = createSectionCard(
                "Selection",
                createContentLabel("Composez librement votre panier avant de passer a la commande."),
                createValueLabel(selectionOverviewText(), "summary-value")
        );
        Label selectionLabel = (Label) selectionCard.getChildren().get(selectionCard.getChildren().size() - 1);
        selectionLabel.textProperty().bind(Bindings.createStringBinding(this::selectionOverviewText, spinnerDependencies()));

        return createPage("Catalogue", pageTitle, pageCopy, productGrid, selectionCard);
    }

    private VBox createOrderPlaceholder() {
        Label pageTitle = new Label("Commande");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label(
                "Preparez votre demande avant de l'envoyer a l'atelier."
        );
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        Button sendOrderButton = new Button("Envoyer la commande");
        sendOrderButton.getStyleClass().addAll("action-button", "primary-button");
        sendOrderButton.setOnAction(event -> sendCurrentOrder());

        Label publishStatusLabel = createValueLabel(publishStatus.get(), "status-copy");
        publishStatusLabel.textProperty().bind(publishStatus);

        Label orderIdLabel = createValueLabel(currentOrderId.get(), "status-copy");
        orderIdLabel.textProperty().bind(Bindings.concat("Reference : ", currentOrderId));

        VBox placeholder = createSectionCard(
                "Recapitulatif",
                createContentLabel("Retrouvez ici les paires choisies avant validation."),
                createValueLabel(orderSummaryText(), "order-summary"),
                sendOrderButton,
                publishStatusLabel,
                orderIdLabel
        );
        Label orderSummary = (Label) placeholder.getChildren().get(2);
        orderSummary.textProperty().bind(Bindings.createStringBinding(this::orderSummaryText, spinnerDependencies()));

        return createPage("Commande", pageTitle, pageCopy, placeholder);
    }

    private VBox createStatusPlaceholder() {
        Label pageTitle = new Label("Statut");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label(
                "Suivez l'avancement de votre demande et consultez les derniers retours."
        );
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        VBox placeholder = createSectionCard(
                "Suivi",
                createContentLabel("Dernier etat de la commande en cours."),
                createValueLabel(connectionStatus.get(), "status-copy"),
                createValueLabel(currentOrderId.get(), "status-copy"),
                createValueLabel(publishStatus.get(), "status-copy")
        );
        ((Label) placeholder.getChildren().get(2)).textProperty().bind(Bindings.concat("Connexion : ", connectionStatus));
        ((Label) placeholder.getChildren().get(3)).textProperty().bind(Bindings.concat("Reference : ", currentOrderId));
        ((Label) placeholder.getChildren().get(4)).textProperty().bind(publishStatus);

        return createPage("Statut", pageTitle, pageCopy, placeholder);
    }

    private VBox createPage(String headerLabel, Node... sections) {
        Label header = new Label(headerLabel);
        header.getStyleClass().add("page-label");

        VBox content = new VBox(20);
        content.getChildren().add(header);
        content.getChildren().addAll(sections);
        content.setPadding(new Insets(28, 32, 28, 32));
        content.getStyleClass().add("page-content");
        return content;
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

        Label quantityLabel = new Label("Quantite");
        quantityLabel.getStyleClass().add("field-label");

        VBox quantityBox = new VBox(8, quantityLabel, quantitySpinner);
        quantityBox.getStyleClass().add("quantity-box");

        VBox card = new VBox(12, header, nameLabel, descriptionLabel, priceLabel, quantityBox);
        card.setPrefWidth(260);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("product-card");
        return card;
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

    private Observable[] spinnerDependencies() {
        return selectedQuantities.values().stream()
                .map(quantity -> (Observable) quantity)
                .toArray(Observable[]::new);
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
            return "Aucune paire selectionnee";
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
            return "Aucune paire n'a encore ete ajoutee a la commande.";
        }

        builder.append("\n\nTotal estime : ").append(formatPrice(totalPrice));
        return builder.toString();
    }

    private String formatPrice(double price) {
        return String.format("%.2f EUR", price);
    }

    private void connectToBroker() {
        try {
            mqttService.start();
            publishStatus.set("Connexion prete pour l'envoi");
        } catch (RuntimeException exception) {
            connectionStatus.set("Connexion impossible");
            publishStatus.set("Impossible de joindre le broker");
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
            publishStatus.set("Selectionnez au moins une paire avant l'envoi");
            currentOrderId.set("-");
            return;
        }

        String orderId = UUID.randomUUID().toString();
        currentOrderId.set(orderId);

        try {
            mqttService.sendOrder(orderId, new Commande(orderLines));
            publishStatus.set("Commande envoyee a l'atelier");
        } catch (RuntimeException exception) {
            publishStatus.set("Envoi impossible : " + exception.getMessage());
        }
    }

    private enum ViewKey {
        HOME,
        CATALOGUE,
        ORDER,
        STATUS
    }
}
