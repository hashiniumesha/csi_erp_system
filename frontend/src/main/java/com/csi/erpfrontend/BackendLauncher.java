package com.csi.erpfrontend;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Makes the installed app a genuine one-click desktop app instead of "start
 * a terminal, then start another terminal": if the backend isn't already
 * running, this starts it invisibly (no console window) using the same
 * Java runtime jpackage bundled for the frontend, so nothing extra needs
 * to be installed on the machine running it.
 *
 * Only does anything when running from the jpackage-built app - jpackage
 * sets the "jpackage.app-path" system property on its generated launcher,
 * which is what this looks for. Running the old way (mvnw javafx:run,
 * during development) leaves that property unset, so this silently does
 * nothing and the existing "start the backend yourself" workflow is
 * unchanged - this is purely additive for the packaged app.
 *
 * Expects the backend jar to sit at ../../backend/erp-backend.jar relative
 * to the installed .exe (see BUILD_DESKTOP_APP.md for the exact layout
 * the build script produces):
 *   dist/
 *     Ceylon Sweets Island ERP/         <- jpackage.app-path points inside here
 *       Ceylon Sweets Island ERP.exe
 *       runtime/bin/javaw.exe           <- reused to launch the backend jar too
 *     backend/
 *       erp-backend.jar
 */
final class BackendLauncher {
    private BackendLauncher() {}

    private static final String BACKEND_URL = "http://localhost:8080/api/roles";
    private static final Duration PING_TIMEOUT = Duration.ofSeconds(2);

    private static Process launchedProcess;

    /** True if something is already answering on the backend's port/path. */
    static boolean isBackendReachable() {
        try {
            HttpClient client = HttpClient.newBuilder().connectTimeout(PING_TIMEOUT).build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(BACKEND_URL)).timeout(PING_TIMEOUT).GET().build();
            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() > 0; // any response at all means something's listening
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Only meaningful when running from the packaged app (see class
     * comment). Returns true if it found and started the backend jar,
     * false if it isn't running from a package build, or couldn't find the
     * jar - either way the caller falls back to "assume the developer will
     * start it themselves".
     */
    static boolean tryStartBackend() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath == null || appPath.isBlank()) {
            return false; // dev mode (mvnw javafx:run) - not our job here
        }

        File exe = new File(appPath);
        File appImageDir = exe.getParentFile();            // ".../Ceylon Sweets Island ERP"
        File distDir = appImageDir != null ? appImageDir.getParentFile() : null; // ".../dist"
        if (distDir == null) return false;

        File backendJar = new File(distDir, "backend/erp-backend.jar");
        File javaw = new File(appImageDir, "runtime/bin/javaw.exe");
        if (!backendJar.isFile() || !javaw.isFile()) {
            return false;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(
                    javaw.getAbsolutePath(), "-jar", backendJar.getAbsolutePath());
            pb.directory(backendJar.getParentFile());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            launchedProcess = pb.start();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Blocks (call off the FX thread) until the backend answers or the timeout passes. */
    static boolean waitUntilReady(Duration timeout) {
        long deadline = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < deadline) {
            if (isBackendReachable()) return true;
            try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return false; }
        }
        return false;
    }

    /** Called on app shutdown - only tears down a backend process this launcher itself started. */
    static void stopIfWeStartedIt() {
        if (launchedProcess != null && launchedProcess.isAlive()) {
            launchedProcess.destroy();
        }
    }
}
