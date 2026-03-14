package com.proyecto.service;

import com.proyecto.dto.WeatherRequestDTO;
import com.proyecto.dto.WeatherResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class WeatherService {

    private final RestTemplate restTemplate;

    @Value("${n8n.webhook.url}")
    private String n8nWebhookUrl;

    public WeatherService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @SuppressWarnings("unchecked")
    public WeatherResponseDTO getWeatherAndRecommendation(WeatherRequestDTO request) {

        // 1. Preparar datos para enviar a n8n
        Map<String, String> body = new HashMap<>();
        body.put("Lugar de origen", request.getOrigin());
        body.put("Destino", request.getDestination());
        body.put("Dia de viaje", request.getTravelDate());
        body.put("Metodo de transporte", capitalize(request.getTransport()));

        // 2. Llamar al webhook de n8n y esperar respuesta
        Map<String, Object> n8nResponse = restTemplate.postForObject(
                n8nWebhookUrl,
                body,
                Map.class
        );

        // 3. Extraer el output — busca en "output" y como fallback en "recommended_clothes"
        String recommendation = "No se pudo generar la recomendación.";
        if (n8nResponse != null) {
            if (n8nResponse.containsKey("output")) {
                recommendation = (String) n8nResponse.get("output");
            } else if (n8nResponse.containsKey("recommended_clothes")) {
                recommendation = (String) n8nResponse.get("recommended_clothes");
            }
        }

        // 4. Retornar respuesta al frontend
        // (el guardado en base de datos lo realiza n8n directamente en Neon/PostgreSQL)
        return new WeatherResponseDTO(
                request.getOrigin(),
                request.getDestination(),
                request.getTravelDate(),
                request.getTransport(),
                null, null, null, null,
                recommendation
        );
    }

    // Capitaliza primera letra para que coincida con el Switch de n8n (Carro, Moto, A pie)
    private String capitalize(String text) {
        if (text == null || text.isEmpty()) return text;
        String lower = text.toLowerCase();
        if (lower.equals("a pie")) return "A pie";
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
