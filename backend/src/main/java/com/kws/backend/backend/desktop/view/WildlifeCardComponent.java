package com.kws.backend.backend.desktop.view;

import com.kws.backend.backend.dto.AnimalDto;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.InputStream;
import java.util.function.Consumer;

public class WildlifeCardComponent extends HBox {

    private final AnimalDto animal;

    public WildlifeCardComponent(AnimalDto animal, Consumer<AnimalDto> clickListener) {
        this.animal = animal;

        setAlignment(Pos.CENTER_LEFT);
        setSpacing(12);
        setPadding(new Insets(10, 12, 10, 12));
        setStyle("-fx-background-color: #161F2E; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-cursor: hand;");

        setOnMouseEntered(e -> setStyle("-fx-background-color: #1E293B; -fx-background-radius: 8px; -fx-border-color: #38BDF8; -fx-border-radius: 8px; -fx-cursor: hand;"));
        setOnMouseExited(e -> setStyle("-fx-background-color: #161F2E; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-cursor: hand;"));

        setOnMouseClicked(e -> {
            if (clickListener != null) {
                clickListener.accept(animal);
            }
        });

        // 1. Photo / Icon
        ImageView imageView = new ImageView();
        imageView.setFitWidth(48);
        imageView.setFitHeight(48);
        imageView.setPreserveRatio(true);

        try {
            String imgName = animal.getImageFileName() != null ? animal.getImageFileName() : "kws_logo.jpg";
            InputStream is = getClass().getResourceAsStream("/images/" + imgName);
            if (is != null) {
                imageView.setImage(new Image(is));
            }
        } catch (Exception ignored) {}

        // 2. Details Column
        VBox details = new VBox(3);
        HBox.setHgrow(details, Priority.ALWAYS);

        HBox nameRow = new HBox(6);
        nameRow.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label(animal.getName());
        nameLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        Label iucnBadge = new Label(animal.getIucnStatus() != null ? animal.getIucnStatus() : "VU");
        String iucnBg = switch (iucnBadge.getText()) {
            case "CR" -> "#DC2626"; // Crimson
            case "EN" -> "#EA580C"; // Orange
            case "VU" -> "#D97706"; // Amber
            case "NT" -> "#2563EB"; // Blue
            default -> "#16A34A";   // Green
        };
        iucnBadge.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 8px; -fx-font-weight: bold; -fx-background-color: " + iucnBg + "; -fx-text-fill: white; -fx-padding: 2px 4px; -fx-background-radius: 3px;");

        nameRow.getChildren().addAll(nameLabel, iucnBadge);

        Label speciesCollar = new Label(animal.getSpecies() + " • " + animal.getCollarId());
        speciesCollar.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: #94A3B8;");

        Label locationLabel = new Label(String.format("📍 %s • %.4f°, %.4f°",
                animal.getParkName() != null ? animal.getParkName() : "Sanctuary",
                animal.getLatitude() != null ? animal.getLatitude() : 0.0,
                animal.getLongitude() != null ? animal.getLongitude() : 0.0));
        locationLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 9px; -fx-text-fill: #64748B;");

        details.getChildren().addAll(nameRow, speciesCollar, locationLabel);

        // 3. Telemetry & Battery Column
        VBox telemetryCol = new VBox(4);
        telemetryCol.setAlignment(Pos.CENTER_RIGHT);

        int battery = animal.getCollarBattery() != null ? animal.getCollarBattery() : 95;
        String batColor = (battery > 60) ? "#10B981" : ((battery > 25) ? "#F59E0B" : "#EF4444");

        Label batLabel = new Label("⚡ " + battery + "%");
        batLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: " + batColor + ";");

        ProgressBar batBar = new ProgressBar(battery / 100.0);
        batBar.setPrefWidth(60);
        batBar.setStyle("-fx-accent: " + batColor + ";");

        String status = animal.getStatus() != null ? animal.getStatus() : "Active";
        Label statusBadge = new Label(status);
        String statusBg = "Alert".equalsIgnoreCase(status) ? "#7F1D1D" : ("Low Battery".equalsIgnoreCase(status) ? "#78350F" : "#064E3B");
        String statusText = "Alert".equalsIgnoreCase(status) ? "#FCA5A5" : ("Low Battery".equalsIgnoreCase(status) ? "#FDE68A" : "#6EE7B7");
        statusBadge.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 8px; -fx-font-weight: bold; -fx-background-color: " + statusBg + "; -fx-text-fill: " + statusText + "; -fx-padding: 2px 6px; -fx-background-radius: 4px;");

        telemetryCol.getChildren().addAll(batLabel, batBar, statusBadge);

        getChildren().addAll(imageView, details, telemetryCol);
    }

    public AnimalDto getAnimal() {
        return animal;
    }
}
