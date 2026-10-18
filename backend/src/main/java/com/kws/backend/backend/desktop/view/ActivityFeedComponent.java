package com.kws.backend.backend.desktop.view;

import com.kws.backend.backend.model.AuditLog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class ActivityFeedComponent extends VBox {

    private final VBox itemsContainer;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public ActivityFeedComponent() {
        setSpacing(8);
        setPadding(new Insets(10, 14, 10, 14));
        setStyle("-fx-background-color: #111827; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px;");

        Label header = new Label("📜 OPERATIONS CHRONOLOGICAL ACTIVITY FEED");
        header.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        itemsContainer = new VBox(6);

        ScrollPane scrollPane = new ScrollPane(itemsContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        scrollPane.setPrefHeight(180);

        getChildren().addAll(header, scrollPane);
    }

    public void updateFeed(List<AuditLog> logs) {
        itemsContainer.getChildren().clear();
        if (logs == null || logs.isEmpty()) {
            Label empty = new Label("No recent field operations recorded.");
            empty.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: #64748B;");
            itemsContainer.getChildren().add(empty);
            return;
        }

        for (AuditLog log : logs) {
            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 6, 4, 6));
            row.setStyle("-fx-background-color: #161F2E; -fx-background-radius: 4px;");

            String timeStr = log.getTimestamp() != null ? log.getTimestamp().format(TIME_FMT) : "Now";
            Label timeLabel = new Label(timeStr);
            timeLabel.setStyle("-fx-font-family: 'Monospaced', monospace; -fx-font-size: 9px; -fx-text-fill: #64748B;");

            String badgeText = log.getActionType() != null ? log.getActionType() : "EVENT";
            String badgeColor = switch (badgeText) {
                case "PATROL_DISPATCHED", "INCIDENT_CREATED" -> "#EF4444";
                case "ANIMAL_REGISTERED", "USER_LOGIN" -> "#10B981";
                case "INCIDENT_RESOLVED", "INCIDENT_ACKNOWLEDGED" -> "#38BDF8";
                default -> "#F59E0B";
            };

            Label badge = new Label(badgeText);
            badge.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 8px; -fx-font-weight: bold; -fx-background-color: " + badgeColor + "33; -fx-text-fill: " + badgeColor + "; -fx-padding: 2px 4px; -fx-background-radius: 3px;");

            Label desc = new Label(log.getDescription());
            desc.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: #E2E8F0;");
            HBox.setHgrow(desc, Priority.ALWAYS);

            Label user = new Label("@" + (log.getUsername() != null ? log.getUsername() : "SYS"));
            user.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 9px; -fx-text-fill: #94A3B8;");

            row.getChildren().addAll(timeLabel, badge, desc, user);
            itemsContainer.getChildren().add(row);
        }
    }
}
