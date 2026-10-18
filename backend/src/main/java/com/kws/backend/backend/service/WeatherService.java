package com.kws.backend.backend.service;

import com.kws.backend.backend.dto.ParkWeatherDto;
import com.kws.backend.backend.model.WeatherObservation;
import com.kws.backend.backend.repository.WeatherObservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeatherService {

    private final WeatherObservationRepository weatherRepository;

    private static final Map<String, ParkWeatherDto> WEATHER_MAP = new LinkedHashMap<>();

    static {
        WEATHER_MAP.put("Amboseli NP", ParkWeatherDto.builder()
                .park("Amboseli NP")
                .emoji("⛅")
                .tempC(28)
                .condition("Partly Cloudy")
                .humidity(55)
                .windKmh(14)
                .build());

        WEATHER_MAP.put("Tsavo East NP", ParkWeatherDto.builder()
                .park("Tsavo East NP")
                .emoji("☀️")
                .tempC(35)
                .condition("Hot & Dry")
                .humidity(30)
                .windKmh(22)
                .build());

        WEATHER_MAP.put("Tsavo West NP", ParkWeatherDto.builder()
                .park("Tsavo West NP")
                .emoji("🌤")
                .tempC(30)
                .condition("Clear Savannah")
                .humidity(45)
                .windKmh(11)
                .build());

        WEATHER_MAP.put("Maasai Mara", ParkWeatherDto.builder()
                .park("Maasai Mara")
                .emoji("🌧")
                .tempC(22)
                .condition("Light Showers")
                .humidity(78)
                .windKmh(18)
                .build());

        WEATHER_MAP.put("Nairobi NP", ParkWeatherDto.builder()
                .park("Nairobi NP")
                .emoji("🌦")
                .tempC(24)
                .condition("Warm & Breezy")
                .humidity(62)
                .windKmh(12)
                .build());

        WEATHER_MAP.put("Ol Pejeta", ParkWeatherDto.builder()
                .park("Ol Pejeta")
                .emoji("☀️")
                .tempC(27)
                .condition("Sunny Equatorial")
                .humidity(50)
                .windKmh(9)
                .build());

        WEATHER_MAP.put("Lake Nakuru", ParkWeatherDto.builder()
                .park("Lake Nakuru")
                .emoji("⛅")
                .tempC(21)
                .condition("Rift Valley Mist")
                .humidity(72)
                .windKmh(8)
                .build());

        WEATHER_MAP.put("Samburu", ParkWeatherDto.builder()
                .park("Samburu")
                .emoji("☀️")
                .tempC(33)
                .condition("Arid & Hot")
                .humidity(28)
                .windKmh(25)
                .build());

        WEATHER_MAP.put("Aberdare", ParkWeatherDto.builder()
                .park("Aberdare")
                .emoji("🌧")
                .tempC(16)
                .condition("Highland Rain")
                .humidity(88)
                .windKmh(15)
                .build());

        WEATHER_MAP.put("Mount Kenya", ParkWeatherDto.builder()
                .park("Mount Kenya")
                .emoji("❄️")
                .tempC(9)
                .condition("Alpine Cold")
                .humidity(80)
                .windKmh(20)
                .build());

        WEATHER_MAP.put("Lewa", ParkWeatherDto.builder()
                .park("Lewa")
                .emoji("⛅")
                .tempC(25)
                .condition("Scattered Clouds")
                .humidity(55)
                .windKmh(10)
                .build());

        WEATHER_MAP.put("Saiwa Swamp", ParkWeatherDto.builder()
                .park("Saiwa Swamp")
                .emoji("🌧")
                .tempC(20)
                .condition("Wetland Humidity")
                .humidity(85)
                .windKmh(7)
                .build());

        WEATHER_MAP.put("Mount Elgon", ParkWeatherDto.builder()
                .park("Mount Elgon")
                .emoji("🌦")
                .tempC(15)
                .condition("Montane Mist")
                .humidity(82)
                .windKmh(12)
                .build());
    }

    public List<ParkWeatherDto> getAllWeather() {
        try {
            List<WeatherObservation> dbObs = weatherRepository.findLatestObservationPerPark();
            if (dbObs != null && !dbObs.isEmpty()) {
                List<ParkWeatherDto> dtos = new ArrayList<>();
                for (WeatherObservation obs : dbObs) {
                    dtos.add(ParkWeatherDto.builder()
                            .park(obs.getParkName())
                            .emoji(resolveWeatherEmoji(obs.getConditions()))
                            .tempC(obs.getTemperatureC() != null ? obs.getTemperatureC().intValue() : 25)
                            .condition(obs.getConditions())
                            .humidity(obs.getHumidityPercent() != null ? obs.getHumidityPercent() : 50)
                            .windKmh(obs.getWindSpeedKmh() != null ? obs.getWindSpeedKmh().intValue() : 12)
                            .build());
                }
                return dtos;
            }
        } catch (Exception e) {
            log.warn("Could not query weather observations from database, falling back: {}", e.getMessage());
        }
        return new ArrayList<>(WEATHER_MAP.values());
    }

    public ParkWeatherDto getWeatherForPark(String parkName) {
        if (parkName == null) {
            parkName = "Nairobi NP";
        }
        try {
            Optional<WeatherObservation> obs = weatherRepository.findTop1ByParkNameIgnoreCaseOrderByObservationTimeDesc(parkName);
            if (obs.isPresent()) {
                WeatherObservation o = obs.get();
                return ParkWeatherDto.builder()
                        .park(o.getParkName())
                        .emoji(resolveWeatherEmoji(o.getConditions()))
                        .tempC(o.getTemperatureC() != null ? o.getTemperatureC().intValue() : 25)
                        .condition(o.getConditions())
                        .humidity(o.getHumidityPercent() != null ? o.getHumidityPercent() : 50)
                        .windKmh(o.getWindSpeedKmh() != null ? o.getWindSpeedKmh().intValue() : 12)
                        .build();
            }
        } catch (Exception ignored) {}

        for (Map.Entry<String, ParkWeatherDto> entry : WEATHER_MAP.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(parkName) || parkName.toLowerCase().contains(entry.getKey().toLowerCase())) {
                return entry.getValue();
            }
        }
        return ParkWeatherDto.builder()
                .park(parkName)
                .emoji("☀️")
                .tempC(26)
                .condition("Savannah Sun")
                .humidity(52)
                .windKmh(12)
                .build();
    }

    private static String resolveWeatherEmoji(String condition) {
        if (condition == null) return "☀️";
        String c = condition.toLowerCase();
        if (c.contains("rain") || c.contains("shower")) return "🌧";
        if (c.contains("cloud") || c.contains("overcast")) return "⛅";
        if (c.contains("cold") || c.contains("alpine")) return "❄️";
        if (c.contains("mist") || c.contains("fog")) return "🌫️";
        if (c.contains("storm")) return "⛈️";
        return "☀️";
    }
}
