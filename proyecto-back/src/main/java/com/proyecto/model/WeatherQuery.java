package com.proyecto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weather_queries")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherQuery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String origin;

    @Column(nullable = false, length = 150)
    private String destination;

    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    @Column(nullable = false, length = 20)
    private String transport;

    @Column(name = "origin_temperature", precision = 5, scale = 2)
    private BigDecimal originTemperature;

    @Column(name = "destination_temperature", precision = 5, scale = 2)
    private BigDecimal destinationTemperature;

    @Column(name = "origin_condition", length = 100)
    private String originCondition;

    @Column(name = "destination_condition", length = 100)
    private String destinationCondition;

    @Column(name = "recommended_clothes", nullable = false, columnDefinition = "TEXT")
    private String recommendedClothes;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
