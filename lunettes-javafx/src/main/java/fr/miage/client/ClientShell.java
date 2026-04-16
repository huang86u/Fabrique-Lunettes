package fr.miage.client;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class ClientShell {
    private final BorderPane root = new BorderPane();
    private final StackPane contentHost = new StackPane();
    private final List<Button> navigationButtons = new ArrayList<>();

    public ClientShell() {
        root.getStyleClass().add("app-shell");
        contentHost.getStyleClass().add("content-host");

        root.setLeft(createSidebar());
        root.setCenter(contentHost);

        showView(ViewKey.HOME, null);
    }

    public Parent getRoot() {
        return root;
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

        VBox sidebar = new VBox(24, brandBlock, navigationBlock, spacer, footerBlock);
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

        contentHost.getChildren().setAll(switch (viewKey) {
            case HOME -> createHomeView();
            case CATALOGUE -> createCataloguePlaceholder();
            case ORDER -> createOrderPlaceholder();
            case STATUS -> createStatusPlaceholder();
        });
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
                createSummaryCard("Catalogue", "Parcourez les modeles et comparez les styles disponibles."),
                createSummaryCard("Commande", "Regroupez vos choix avant validation."),
                createSummaryCard("Statut", "Consultez les dernieres nouvelles de votre demande.")
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

        VBox placeholder = createSectionCard(
                "Selection",
                "Les modeles, leurs details et les quantites apparaitront ici."
        );

        return createPage("Catalogue", pageTitle, pageCopy, placeholder);
    }

    private VBox createOrderPlaceholder() {
        Label pageTitle = new Label("Commande");
        pageTitle.getStyleClass().add("page-title");

        Label pageCopy = new Label(
                "Preparez votre demande avant de l'envoyer a l'atelier."
        );
        pageCopy.setWrapText(true);
        pageCopy.getStyleClass().add("page-copy");

        VBox placeholder = createSectionCard(
                "Recapitulatif",
                "Le contenu de votre commande apparaitra ici."
        );

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
                "Les mises a jour de commande apparaitront ici."
        );

        return createPage("Statut", pageTitle, pageCopy, placeholder);
    }

    private VBox createPage(String headerLabel, javafx.scene.Node... sections) {
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
        VBox card = createSectionCard(titleText, bodyText);
        card.setPrefWidth(240);
        return card;
    }

    private VBox createSectionCard(String titleText, String bodyText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");

        Label body = new Label(bodyText);
        body.setWrapText(true);
        body.getStyleClass().add("section-copy");

        VBox card = new VBox(10, title, body);
        card.setAlignment(Pos.TOP_LEFT);
        card.setPadding(new Insets(18));
        card.getStyleClass().add("section-card");
        return card;
    }

    private enum ViewKey {
        HOME,
        CATALOGUE,
        ORDER,
        STATUS
    }
}
