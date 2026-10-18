package com.kws.backend.backend.desktop.view;

import com.kws.backend.backend.model.IncidentLog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

import java.util.function.Consumer;

public class AlertBannerComponent extends HBox {

    private final Label alertText;
    private final Button dispatchBtn;
    private final Button ackBtn;
    private IncidentLog currentIncident;

    public AlertBannerComponent(Consumer<IncidentLog> onDispatch, Consumer<IncidentLog> onAcknowledge) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setPadding(new Insets(10, 16, 10, 16));
        setStyle("-fx-background-color: #7F1D1D; -fx-background-radius: 6px; -fx-border-color: #EF4444; -fx-border-radius: 6px;");

        Label siren = new Label("🚨 CRITICAL ALERT:");
        siren.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #FCA5A5;");

        alertText = new Label("Geofence breach detected!");
        alertText.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #FEF2F2;");
        HBox.setHgrow(alertText, Priority.ALWAYS);

        ackBtn = new Button("👁️ Acknowledge");
        ackBtn.setStyle("-fx-background-color: #991B1B; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;");
        ackBtn.setOnAction(e -> {
            if (onAcknowledge != null && currentIncident != null) {
                onAcknowledge.accept(currentIncident);
            }
        });

        dispatchBtn = new Button("⚡ Dispatch Patrol Unit");
        dispatchBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;");
        dispatchBtn.setOnAction(e -> {
            if (onDispatch != null && currentIncident != null) {
                onDispatch.accept(currentIncident);
            }
        });

        getChildren().addAll(siren, alertText, ackBtn, dispatchBtn);
        setVisible(false);
        setManaged(false);
    }

    public void showAlert(IncidentLog incident) {
        this.currentIncident = incident;
        if (incident != null) {
            alertText.setText(String.format("%s (%s) breached %s — %s",
                    incident.getAnimalName(), incident.getCollarId(),
                    incident.getZoneName() != null ? incident.getZoneName() : "Boundary",
                    incident.getMessage() != null ? incident.getMessage() : "Immediate response recommended."));
            setVisible(true);
            setManaged(true);
        } else {
            clearAlert();
        }
    }

    public void clearAlert() {
        this.currentIncident = null;
        setVisible(false);
        setManaged(false);
    }
}
