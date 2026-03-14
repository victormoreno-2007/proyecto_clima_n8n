package com.proyecto.controller;

import com.proyecto.dto.WeatherRequestDTO;
import com.proyecto.dto.WeatherResponseDTO;
import com.proyecto.service.WeatherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @PostMapping("/weather-outfit")
    public ResponseEntity<WeatherResponseDTO> getWeatherOutfit(@RequestBody WeatherRequestDTO request) {
        WeatherResponseDTO response = weatherService.getWeatherAndRecommendation(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Weather Assistant API running OK");
    }
}
