package com.proyecto.dto;

import lombok.Data;

@Data
public class WeatherRequestDTO {
    private String origin;
    private String destination;
    private String travelDate; // formato: yyyy-MM-dd
    private String transport;  // carro, moto, a pie
}
