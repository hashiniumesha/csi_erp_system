package com.csi.erpfrontend;

/**
 * A packaged, non-modular JavaFX app can't have its main-class be the
 * Application subclass itself - the JavaFX launcher deliberately refuses
 * ("JavaFX runtime components are missing") when it detects that, unless
 * you're launching through the module system. This indirection class is
 * the standard workaround: it doesn't extend Application, so that check
 * never triggers, and it just hands off to MainApp immediately.
 *
 * mvnw javafx:run (the day-to-day dev workflow) doesn't need this - the
 * javafx-maven-plugin launches through the module path already, where the
 * check doesn't apply. This class exists only for jpackage's
 * --main-class (see build-desktop-app.ps1).
 */
public class Launcher {
    public static void main(String[] args) {
        MainApp.main(args);
    }
}
