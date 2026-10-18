package com.kws.backend.backend.desktop.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class KpiCardComponent extends HBox {

    private final Label titleLabel;
    private final Label valueLabel;
    private final Label subLabel;
    private final Label iconLabel;

    public KpiCardComponent(String title, String initialValue, String subtext, String icon, String accentColorHex) {
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(14);
        setPadding(new Insets(14, 18, 14, 18));
        setStyle("-fx-background-color: #111827; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px; -fx-border-width: 1px;");

        VBox textContainer = new VBox(4);
        HBox.setHgrow(textContainer, Priority.ALWAYS);

        titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #94A3B8; -fx-font-weight: bold;");

        valueLabel = new Label(initialValue);
        valueLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 22px; -fx-text-fill: #F8FAFC; -fx-font-weight: bold;");

        subLabel = new Label(subtext);
        subLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: " + accentColorHex + ";");

        textContainer.getChildren().addAll(titleLabel, valueLabel, subLabel);

        iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 24px; -fx-padding: 8px; -fx-background-color: rgba(255,255,255,0.05); -fx-background-radius: 8px;");

        getChildren().addAll(textContainer, iconLabel);
    }

    public void updateValue(String value, String subtext) {
        valueLabel.setText(value);
        if (subtext != null) {
            subLabel.setText(subtext);
        }
    }
}
