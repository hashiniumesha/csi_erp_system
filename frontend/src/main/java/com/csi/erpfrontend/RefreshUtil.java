package com.csi.erpfrontend;

import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.util.Duration;

/**
 * Every module's Refresh button uses this so the visual "yes, it actually
 * reloaded" confirmation looks and behaves identically everywhere, instead
 * of each screen inventing its own version.
 */
final class RefreshUtil {
    private RefreshUtil() {}

    static Button newButton() {
        Button button = new Button("⟳ Refresh");
        button.getStyleClass().add("button-secondary");
        return button;
    }

    // Briefly relabels the button to confirm the reload happened, rather
    // than the click having no visible effect if the data underneath
    // happened not to change.
    static void flash(Button button) {
        String original = button.getText();
        button.setText("✓ Refreshed");
        button.setDisable(true);
        PauseTransition pause = new PauseTransition(Duration.seconds(1.1));
        pause.setOnFinished(e -> {
            button.setText(original);
            button.setDisable(false);
        });
        pause.play();
    }
}
