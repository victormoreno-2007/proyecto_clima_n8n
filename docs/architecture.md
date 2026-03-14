# 🏗️ Arquitectura del Sistema — ClimaVest

## ¿Qué es este documento?

Este documento describe cómo están organizados y conectados todos los componentes que forman el sistema **ClimaVest**, un asistente inteligente de viajes que consulta el clima de una ruta y genera recomendaciones de vestimenta personalizadas.

---

## Visión general

El sistema está compuesto por **5 capas principales** que trabajan juntas de forma coordinada:

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│   Frontend  │────▶│  Backend    │────▶│    n8n      │────▶│  AI Agent   │────▶│  PostgreSQL │
│  (HTML/CSS/ │     │ (Java 17 /  │     │ (Automatiza │     │  (Google    │     │   (Neon)    │
│     JS)     │◀────│ Spring Boot)│◀────│  el flujo)  │◀────│   Gemini)   │     │             │
└─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘     └─────────────┘
```

---

## Componentes detallados

### 1. Frontend (`/frontend`)

- **Tecnología:** HTML5, CSS3 y JavaScript puro (sin frameworks)
- **Puerto:** Se sirve como archivo estático en el navegador
- **Responsabilidad:** Mostrar el formulario al usuario y presentar el reporte generado por la IA
- **Comunicación:** Hace peticiones `POST` al backend en `http://localhost:3001/api/weather-outfit`
- **Timeout configurado:** 3 minutos (180,000 ms) para esperar la respuesta

**Campos que el usuario ingresa:**
- Lugar de origen
- Lugar de destino
- Fecha del viaje
- Medio de transporte (Carro, Moto, A pie)

---

### 2. Backend (`/proyecto-back`)

- **Tecnología:** Java 17 + Spring Boot 3.2.5
- **Puerto:** `3001`
- **Responsabilidad:** Recibir la solicitud del frontend, reformatear los datos y enviarlos al webhook de n8n. Luego devuelve la respuesta al frontend.
- **Timeout configurado:** 3 minutos (180,000 ms) para esperar respuesta de n8n

**Endpoints expuestos:**

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/weather-outfit` | Recibe los datos del viaje y devuelve el reporte |
| GET | `/api/health` | Verifica que el servidor esté activo |

**Estructura de clases:**
```
com.proyecto
├── controller/   → WeatherController (recibe peticiones HTTP)
├── service/      → WeatherService (lógica de negocio)
├── dto/          → WeatherRequestDTO, WeatherResponseDTO (objetos de transferencia)
├── model/        → WeatherQuery (entidad de base de datos)
├── repository/   → WeatherQueryRepository (acceso a datos)
├── client/       → WeatherApiClient (cliente HTTP hacia n8n)
├── config/       → CorsConfig, RestTemplateConfig
└── util/         → ClothingRecommendationUtil (lógica de vestimenta de respaldo)
```

---

### 3. n8n (`/mcp-server` y configuración externa)

- **Tecnología:** n8n (plataforma de automatización de flujos)
- **Acceso:** Instancia self-hosted expuesta via ngrok
- **Responsabilidad:** Orquestar todo el flujo de datos — geocodificación, cálculo de ruta, consulta de clima en 5 puntos, generación del reporte con IA e inserción en base de datos

**Flujo interno de n8n paso a paso:**

```
1. Webhook            → Recibe los datos del viaje desde el backend Java
2. Nombrar variables  → Extrae y organiza los campos recibidos
3. Obtener origen     → Geocodifica el nombre del origen (OpenRouteService)
4. Obtener destino    → Geocodifica el nombre del destino (OpenRouteService)
5. Unir coordenadas   → Combina ambas coordenadas en un solo objeto
6. Switch             → Divide el flujo según el medio de transporte
7. Obtener Ruta       → Calcula la ruta real (driving-car / cycling / foot-walking)
8. Obtener puntos     → Extrae 5 puntos de la ruta (origen, 3 intermedios, destino)
9. Loop Over Items    → Itera sobre cada punto de la ruta
10. Consultar clima   → Consulta Open-Meteo para cada punto (temperatura + código WMO)
11. Agrupar datos     → Consolida temperaturas y códigos en arrays
12. Editar chatInput  → Prepara el mensaje para el AI Agent
13. AI Agent (Gemini) → Genera el reporte narrativo con recomendaciones
14. Code JS           → Separa el reporte HTML del bloque de datos estructurados %%DATA%%
15. Postgres (Neon)   → Inserta el registro completo en la base de datos
16. Respond Webhook   → Devuelve el reporte al backend Java
```

**APIs externas que usa n8n:**

| API | Uso |
|-----|-----|
| OpenRouteService | Geocodificación y cálculo de rutas |
| Open-Meteo | Consulta de clima en tiempo real (gratuita, sin API key) |
| Google Gemini | Generación del reporte narrativo con IA |

---

### 4. MCP Server (`/mcp-server`)

- **Tecnología:** Node.js + Express
- **Puerto:** `4000`
- **Responsabilidad:** Exponer herramientas (tools) que permiten consultar el clima y generar recomendaciones. Implementa el protocolo MCP (Model Context Protocol).

**Tools expuestas:**

| Tool | Endpoint | Descripción |
|------|----------|-------------|
| `consultar_clima_ruta` | POST `/tool/consultar_clima_ruta` | Llama al webhook de n8n con los datos del viaje |
| `recommend_outfit` | POST `/tool/recommend_outfit` | Genera recomendación de vestimenta localmente (fallback sin n8n) |

---

### 5. Base de datos (`/database`)

- **Tecnología:** PostgreSQL alojado en Neon (nube)
- **Responsabilidad:** Almacenar el historial completo de todas las consultas realizadas
- **Quién inserta:** El nodo Postgres dentro del flujo de n8n

**Tablas:**

| Tabla | Descripción |
|-------|-------------|
| `weather_queries` | Historial de todas las consultas de clima realizadas |
| `users` | Tabla opcional para futura autenticación de usuarios |

---

## Flujo completo de una consulta

```
Usuario llena el formulario
        │
        ▼
Frontend (JS) hace POST a Spring Boot :3001
        │
        ▼
Spring Boot reformatea y hace POST al webhook de n8n
        │
        ▼
n8n geocodifica origen y destino con OpenRouteService
        │
        ▼
n8n calcula la ruta según el medio de transporte
        │
        ▼
n8n extrae 5 puntos de la ruta y consulta Open-Meteo en cada uno
        │
        ▼
n8n envía temperaturas y códigos WMO al AI Agent (Gemini)
        │
        ▼
Gemini genera el reporte narrativo + bloque %%DATA%%
        │
        ▼
Nodo Code JS separa el reporte del bloque de datos
        │
        ▼
Nodo Postgres inserta el registro completo en Neon
        │
        ▼
Respond to Webhook devuelve el reporte a Spring Boot
        │
        ▼
Spring Boot devuelve el JSON al frontend
        │
        ▼
Frontend muestra el reporte al usuario ✅
```

---

## Decisiones técnicas importantes

**¿Por qué PostgreSQL en Neon y no MySQL local?**
n8n Cloud no puede conectarse a una base de datos local porque no tiene acceso a la red privada del equipo. Neon ofrece PostgreSQL gratuito accesible desde internet, lo que permite que n8n inserte directamente sin depender del backend Java.

**¿Por qué el backend Java ya no guarda en base de datos?**
Para evitar duplicados. Inicialmente el backend guardaba en MySQL local, pero al migrar a Neon con n8n como responsable del INSERT, se eliminó esa lógica del backend para que cada consulta se registre una sola vez.

**¿Por qué 5 puntos de clima en la ruta?**
Consultar solo el clima del origen o destino puede ser engañoso en rutas largas. Con 5 puntos (origen, 3 intermedios y destino) se obtiene una visión completa de las condiciones a lo largo de todo el trayecto.
