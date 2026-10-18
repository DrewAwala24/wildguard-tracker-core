package com.kws.backend.backend.controller;

import com.kws.backend.backend.dto.ParkWeatherDto;
import com.kws.backend.backend.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping
    public ResponseEntity<List<ParkWeatherDto>> getAllWeather() {
        return ResponseEntity.ok(weatherService.getAllWeather());
    }

    @GetMapping("/{parkName}")
    public ResponseEntity<ParkWeatherDto> getWeatherForPark(@PathVariable String parkName) {
        return ResponseEntity.ok(weatherService.getWeatherForPark(parkName));
    }
}
