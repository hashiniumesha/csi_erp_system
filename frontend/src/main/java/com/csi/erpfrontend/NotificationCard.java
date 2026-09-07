package com.csi.erpfrontend;

import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONObject;

/**
 * Renders one ApprovalRequestController "RequestSummary" as its own small,
 * status-coloured card - used by both RawMaterialView's "My Requests" and
 * MessagesView, so a request looks the same wherever it's shown instead of
 * each screen building its own version.
 */
public class NotificationCard {

    public static VBox build(JSONObject r) {
        String type = r.optString("requestType", "?");
        JSONObject payload = new JSONObject(r.optString("payloadJson", "{}"));
        String description = switch (type) {
            case "CREATE" -> "Add raw material: " + payload.optString("name", "?");
            case "UPDATE" -> "Update raw material #" + r.optInt("entityId") + " → " + payload.optString("name", "?");
            case "DELETE" -> "Delete raw material #" + r.optInt("entityId");
            default -> type;
        };

        Label descLabel = new Label(description);
        descLabel.getStyleClass().add("notification-title");
        descLabel.setWrapText(true);

        String status = r.optString("status", "Pending");
        Label statusPill = new Label(status);
        statusPill.getStyleClass().add(switch (status) {
            case "Approved" -> "status-success";
            case "Rejected" -> "status-error";
            default -> "status-pending";
        });

        HBox headerRow = new HBox(12, descLabel, statusPill);

        VBox card = new VBox(6, headerRow);
        card.getStyleClass().addAll("notification-card", switch (status) {
            case "Approved" -> "notification-card-approved";
            case "Rejected" -> "notification-card-rejected";
            default -> "notification-card-pending";
        });

        String note = r.optString("adminNote", "");
        if (!note.isBlank()) {
            Label noteLabel = new Label(note);
            noteLabel.getStyleClass().add("muted-label");
            noteLabel.setWrapText(true);
            card.getChildren().add(noteLabel);
        }
        return card;
    }
}
