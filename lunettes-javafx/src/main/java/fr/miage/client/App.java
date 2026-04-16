package fr.miage.client;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    private ClientShell clientShell;

    @Override
    public void start(Stage stage) {
        clientShell = new ClientShell();

        Scene scene = new Scene(clientShell.getRoot(), 1180, 760);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());

        stage.setTitle("Fabrique de lunettes");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (clientShell != null) {
            clientShell.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
