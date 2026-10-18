package com.kws.backend.backend.desktop.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class BatteryGaugeComponent extends VBox {

    private final ProgressBar progressBar;
    private final Label percentLabel;
    private final Label summaryLabel;

    public BatteryGaugeComponent() {
        setSpacing(6);
        setPadding(new Insets(10, 16, 10, 16));
        setStyle("-fx-background-color: #111827; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px;");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🔋 COLLAR FLEET TRANSMISSION & BATTERY HEALTH");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        HBox.setHgrow(title, Priority.ALWAYS);

        percentLabel = new Label("88% Fleet Nominal");
        percentLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        header.getChildren().addAll(title, percentLabel);

        progressBar = new ProgressBar(0.88);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setStyle("-fx-accent: #10B981;");

        summaryLabel = new Label("31 Collars >70% (Optimal) • 3 Collars 20-70% (Moderate) • 0 Critical");
        summaryLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: #64748B;");

        getChildren().addAll(header, progressBar, summaryLabel);
    }

    public void updateHealth(int goodCount, int moderateCount, int lowCount, double avgPercent) {
        double progress = Math.max(0.0, Math.min(1.0, avgPercent / 100.0));
        progressBar.setProgress(progress);

        String colorHex = (progress > 0.6) ? "#10B981" : ((progress > 0.25) ? "#F59E0B" : "#EF4444");
        progressBar.setStyle("-fx-accent: " + colorHex + ";");

        percentLabel.setText(String.format("%.0f%% Fleet Nominal", avgPercent));
        percentLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        summaryLabel.setText(String.format("%d Collars >70%% (Optimal) • %d Collars 20-70%% (Moderate) • %d Critical (<20%%)",
                goodCount, moderateCount, lowCount));
    }
}
