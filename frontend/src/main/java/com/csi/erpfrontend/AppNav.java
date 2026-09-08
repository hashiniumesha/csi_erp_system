package com.csi.erpfrontend;

/**
 * Lets any module's own "Refresh" button reload the module currently on
 * screen, without every module needing to manage its own swap-into-parent
 * plumbing. DashboardShell is the only thing that actually owns the content
 * area, so it's the only thing that registers a handler here (whenever the
 * user navigates to a module, via setRefreshHandler) - every module just
 * calls refreshCurrent() and doesn't need to know how re-rendering itself
 * actually happens.
 */
final class AppNav {
    private AppNav() {}

    private static Runnable refreshHandler;

    static void setRefreshHandler(Runnable handler) {
        refreshHandler = handler;
    }

    static void refreshCurrent() {
        if (refreshHandler != null) refreshHandler.run();
    }
}
