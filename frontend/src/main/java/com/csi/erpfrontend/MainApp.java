package com.csi.erpfrontend;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Application entry point. Screen-building logic lives in LoginView,
 * DashboardShell, and one class per module (QCView, InventoryView,
 * SalesView) — split out so more than one person can work on the frontend
 * at once without editing the same file.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        loadFonts();
        primaryStage.getIcons().add(new Image(MainApp.class.getResourceAsStream("/csi-logo.png")));
        primaryStage.setTitle("Ceylon Sweets Island ERP");
        primaryStage.setMaximized(true);

        // Backend already up (the team's usual dev workflow: start it
        // yourself, then run the frontend) - skip straight to login, no
        // startup screen needed.
        if (BackendLauncher.isBackendReachable()) {
            goToLogin(primaryStage);
            return;
        }

        // Only the installed/packaged app gets this far and actually tries
        // to start the backend itself (see BackendLauncher) - this is what
        // makes it a genuine one-icon desktop app instead of "open two
        // terminals". If this isn't a packaged run, tryStartBackend() does
        // nothing and this falls through to the same clear error a plain
        // dev run would give when the backend isn't up.
        primaryStage.setScene(startingScene());
        primaryStage.show();

        Task<Boolean> startupTask = new Task<>() {
            @Override
            protected Boolean call() {
                BackendLauncher.tryStartBackend();
                return BackendLauncher.waitUntilReady(Duration.ofSeconds(45));
            }
        };
        startupTask.setOnSucceeded(e -> {
            if (Boolean.TRUE.equals(startupTask.getValue())) {
                goToLogin(primaryStage);
            } else {
                primaryStage.setScene(startupFailedScene());
            }
        });
        startupTask.setOnFailed(e -> primaryStage.setScene(startupFailedScene()));
        Thread thread = new Thread(startupTask, "backend-startup");
        thread.setDaemon(true);
        thread.start();
    }

    private void goToLogin(Stage stage) {
        stage.setTitle("Ceylon Sweets Island ERP - Login");
        stage.setScene(LoginView.build(stage));
    }

    private Scene startingScene() {
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setMaxSize(48, 48);
        Label label = new Label("Starting Ceylon Sweets Island ERP…");
        label.getStyleClass().add("section-title");
        Label sub = new Label("Starting the backend service for the first time can take up to a minute.");
        sub.getStyleClass().add("muted-label");
        VBox box = new VBox(16, spinner, label, sub);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("page-bg");
        Scene scene = new Scene(box, 900, 600);
        scene.getStylesheets().add(MainApp.class.getResource("/style.css").toExternalForm());
        return scene;
    }

    private Scene startupFailedScene() {
        Label title = new Label("Couldn't start the backend service");
        title.getStyleClass().add("page-title");
        Label detail = new Label(
                "This usually means MySQL isn't running on this computer, or the database hasn't been set up yet.\n\n" +
                "Things to check:\n" +
                "  • Is MySQL (the MySQL80 service) running?\n" +
                "  • Does the \"csi_erp_db\" database exist with the schema and seed data loaded?\n\n" +
                "Once that's fixed, close this window and reopen the app.");
        detail.setWrapText(true);
        detail.getStyleClass().add("muted-label");
        VBox box = new VBox(14, title, detail);
        box.setMaxWidth(560);
        box.setAlignment(Pos.CENTER_LEFT);
        box.getStyleClass().add("card");
        VBox root = new VBox(box);
        root.setAlignment(Pos.CENTER);
        root.getStyleClass().add("page-bg");
        Scene scene = new Scene(root, 900, 600);
        scene.getStylesheets().add(MainApp.class.getResource("/style.css").toExternalForm());
        return scene;
    }

    // Montserrat isn't installed on a typical Windows machine, so it's
    // bundled as a font file and registered here at startup - style.css
    // then just references the family name "Montserrat" like any installed
    // font. Loaded once at whatever the JavaFX default size is; every
    // screen's own -fx-font-size in CSS still applies on top of this.
    private void loadFonts() {
        try (InputStream is = MainApp.class.getResourceAsStream("/fonts/Montserrat-Variable.ttf")) {
            if (is != null) {
                Font.loadFont(is, 13);
            }
        } catch (Exception ignored) {
            // Falls back to the CSS font-family stack's next entry
            // (Segoe UI) - never worth failing the app over.
        }
    }

    @Override
    public void stop() {
        BackendLauncher.stopIfWeStartedIt();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
