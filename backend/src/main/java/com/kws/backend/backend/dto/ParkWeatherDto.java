package com.kws.backend.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ambient meteorological conditions for Kenyan national parks and conservancies.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParkWeatherDto {
    private String park;
    private String emoji;
    private int tempC;
    private String condition;
    private int humidity;
    private int windKmh;

    public String getTempDisplay() {
        return tempC + "°C";
    }

    public String getHumidityDisplay() {
        return "💧 " + humidity + "%";
    }

    public String getWindDisplay() {
        return "💨 " + windKmh + " km/h";
    }

    public String getShortPark() {
        if (park == null || park.isBlank()) return "";
        return park.split(" ")[0];
    }
}
