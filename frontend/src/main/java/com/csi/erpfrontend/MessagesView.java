package com.csi.erpfrontend;

import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Central place for the approval-request workflow's visible side: Admin
 * sees and acts on the pending queue here (rather than a separate,
 * disconnected "Approvals" screen), and every role can see their own
 * request history and status. Ties directly into ApprovalRequestController
 * - nothing here is its own storage or notification system.
 */
public class MessagesView {

    public static Node build() {
        Label title = new Label("Messages / Notifications");
        title.getStyleClass().add("page-title");

        VBox layout = new VBox(20, title);
        layout.setPadding(new Insets(28));

        if (Session.isAdmin()) {
            layout.getChildren().add(buildPendingApprovalsCard());
        }
        layout.getChildren().add(buildMyRequestsCard());

        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.getStyleClass().add("page-bg");
        return scrollPane;
    }

    private static VBox buildPendingApprovalsCard() {
        Label sectionTitle = new Label("Pending Approvals");
        sectionTitle.getStyleClass().add("section-title");

        VBox list = new VBox(10);
        refreshPending(list);

        VBox card = new VBox(10, sectionTitle, list);
        card.getStyleClass().add("card");
        return card;
    }

    private static void refreshPending(VBox list) {
        list.getChildren().clear();
        try {
            JSONArray pending = ApiClient.getArray("/api/approval-requests/pending");
            if (pending.isEmpty()) {
                Label empty = new Label("No pending requests.");
                empty.getStyleClass().add("muted-label");
                list.getChildren().add(empty);
            }
            for (int i = 0; i < pending.length(); i++) {
                list.getChildren().add(buildPendingRow(pending.getJSONObject(i), list));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load pending requests: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }
    }

    private static VBox buildPendingRow(JSONObject r, VBox parentList) {
        Integer requestId = r.optInt("requestId");
        String type = r.optString("requestType", "?");
        JSONObject payload = new JSONObject(r.optString("payloadJson", "{}"));
        String description = switch (type) {
            case "CREATE" -> "Add raw material: " + payload.optString("name", "?")
                    + " (reorder level " + payload.opt("reorderLevel") + ")";
            case "UPDATE" -> "Update raw material #" + r.optInt("entityId") + " → "
                    + payload.optString("name", "?") + " (reorder level " + payload.opt("reorderLevel") + ")";
            case "DELETE" -> "Delete raw material #" + r.optInt("entityId");
            default -> type;
        };

        Label descLabel = new Label(description);
        descLabel.setWrapText(true);
        Label byLabel = new Label("Requested by " + r.optString("requestedByName", "?") + " · " + r.optString("requestedAt", ""));
        byLabel.getStyleClass().add("muted-label");

        TextField noteField = new TextField();
        noteField.setPromptText("Note (optional, shown to requester if rejected)");
        noteField.getStyleClass().add("field-wide");

        Label statusLabel = new Label();
        statusLabel.setWrapText(true);
        statusLabel.setVisible(false);
        statusLabel.setManaged(false);

        Button approveButton = new Button("Approve");
        approveButton.getStyleClass().add("button-primary");
        Button rejectButton = new Button("Reject");
        rejectButton.getStyleClass().add("button-danger");

        approveButton.setOnAction(e -> {
            try {
                JSONObject body = new JSONObject();
                body.put("reviewedByUserId", Session.getUserId());
                ApiClient.post("/api/approval-requests/" + requestId + "/approve", body);
                refreshPending(parentList);
            } catch (ApiClient.ApiException ex) {
                statusLabel.setText(ex.getMessage());
                statusLabel.getStyleClass().setAll("status-error");
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        rejectButton.setOnAction(e -> {
            try {
                JSONObject body = new JSONObject();
                body.put("reviewedByUserId", Session.getUserId());
                body.put("adminNote", noteField.getText());
                ApiClient.post("/api/approval-requests/" + requestId + "/reject", body);
                refreshPending(parentList);
            } catch (ApiClient.ApiException ex) {
                statusLabel.setText(ex.getMessage());
                statusLabel.getStyleClass().setAll("status-error");
                statusLabel.setVisible(true);
                statusLabel.setManaged(true);
            }
        });

        HBox buttonRow = new HBox(10, approveButton, rejectButton);

        VBox row = new VBox(6, descLabel, byLabel, noteField, buttonRow, statusLabel);
        row.getStyleClass().addAll("notification-card", "notification-card-pending");
        return row;
    }

    private static VBox buildMyRequestsCard() {
        Label sectionTitle = new Label("My Requests");
        sectionTitle.getStyleClass().add("section-title");

        VBox list = new VBox(12);
        try {
            JSONArray mine = ApiClient.getArray("/api/approval-requests/mine?userId=" + Session.getUserId());
            if (mine.isEmpty()) {
                Label empty = new Label("No requests yet.");
                empty.getStyleClass().add("muted-label");
                list.getChildren().add(empty);
            }
            for (int i = 0; i < mine.length(); i++) {
                list.getChildren().add(NotificationCard.build(mine.getJSONObject(i)));
            }
        } catch (ApiClient.ApiException e) {
            Label error = new Label("Couldn't load your requests: " + e.getMessage());
            error.getStyleClass().add("status-error");
            list.getChildren().add(error);
        }

        VBox card = new VBox(10, sectionTitle, list);
        card.getStyleClass().add("card");
        return card;
    }

}
