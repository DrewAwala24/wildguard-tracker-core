package com.kws.backend.backend.desktop.view;

import com.kws.backend.backend.dto.ParkWeatherDto;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;

public class WeatherStripComponent extends ScrollPane {

    private final HBox cardContainer;
    private Consumer<String> parkClickListener;

    public WeatherStripComponent() {
        setVbarPolicy(ScrollBarPolicy.NEVER);
        setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
        setFitToHeight(true);
        setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");

        cardContainer = new HBox(10);
        cardContainer.setPadding(new Insets(4, 4, 4, 4));
        cardContainer.setAlignment(Pos.CENTER_LEFT);

        setContent(cardContainer);
    }

    public void setOnParkClicked(Consumer<String> listener) {
        this.parkClickListener = listener;
    }

    public void updateWeather(List<ParkWeatherDto> weatherList) {
        cardContainer.getChildren().clear();
        if (weatherList == null) return;

        for (ParkWeatherDto w : weatherList) {
            VBox card = new VBox(2);
            card.setPrefWidth(150);
            card.setPadding(new Insets(8, 12, 8, 12));
            card.setStyle("-fx-background-color: #111827; -fx-background-radius: 6px; -fx-border-color: #1E293B; -fx-border-radius: 6px; -fx-cursor: hand;");

            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #1E293B; -fx-background-radius: 6px; -fx-border-color: #38BDF8; -fx-border-radius: 6px; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: #111827; -fx-background-radius: 6px; -fx-border-color: #1E293B; -fx-border-radius: 6px; -fx-cursor: hand;"));

            card.setOnMouseClicked(e -> {
                if (parkClickListener != null) {
                    parkClickListener.accept(w.getPark());
                }
            });

            HBox topRow = new HBox(6);
            topRow.setAlignment(Pos.CENTER_LEFT);
            Label emoji = new Label(w.getEmoji() != null ? w.getEmoji() : "⛅");
            emoji.setStyle("-fx-font-size: 14px;");
            Label parkName = new Label(w.getPark());
            parkName.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");
            topRow.getChildren().addAll(emoji, parkName);

            Label tempLabel = new Label(w.getTempC() + "°C • " + (w.getCondition() != null ? w.getCondition() : "Clear"));
            tempLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-text-fill: #38BDF8; -fx-font-weight: bold;");

            Label metricsLabel = new Label("💧 " + w.getHumidity() + "%  💨 " + w.getWindKmh() + " km/h");
            metricsLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 9px; -fx-text-fill: #64748B;");

            card.getChildren().addAll(topRow, tempLabel, metricsLabel);
            cardContainer.getChildren().add(card);
        }
    }
}
