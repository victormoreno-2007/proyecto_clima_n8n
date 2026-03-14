package com.proyecto.repository;

import com.proyecto.model.WeatherQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WeatherQueryRepository extends JpaRepository<WeatherQuery, Long> {

    List<WeatherQuery> findByOriginIgnoreCaseOrderByCreatedAtDesc(String origin);

    List<WeatherQuery> findByDestinationIgnoreCaseOrderByCreatedAtDesc(String destination);
}
