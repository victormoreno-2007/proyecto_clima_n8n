package com.proyecto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponseDTO {
    private String origin;
    private String destination;
    private String travelDate;
    private String transport;

    private Double originTemperature;
    private String originCondition;

    private Double destinationTemperature;
    private String destinationCondition;

    private String recommendation;
}
