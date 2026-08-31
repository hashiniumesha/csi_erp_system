package com.csi.erpfrontend;

/**
 * Holds the currently logged-in user for this desktop session. A single
 * static holder is appropriate here because the JavaFX app only ever
 * represents one signed-in user at a time (one Stage, one login).
 */
public class Session {

    private static Integer userId;
    private static String username;
    private static String fullName;
    private static String roleName;

    public static void set(int userId, String username, String fullName, String roleName) {
        Session.userId = userId;
        Session.username = username;
        Session.fullName = fullName;
        Session.roleName = roleName;
    }

    public static void clear() {
        userId = null;
        username = null;
        fullName = null;
        roleName = null;
    }

    public static Integer getUserId() { return userId; }
    public static String getUsername() { return username; }
    public static String getFullName() { return fullName; }
    public static String getRoleName() { return roleName; }

    public static boolean isAdmin() { return "Admin".equals(roleName); }
}
