# 🗺️ ClimaVest — Asistente Inteligente de Viaje

ClimaVest es un asistente inteligente que consulta el clima en múltiples puntos de una ruta y genera recomendaciones de vestimenta personalizadas usando inteligencia artificial, adaptadas al medio de transporte del usuario.

---

## ¿Qué hace?

El usuario ingresa su ciudad de origen, destino, fecha de viaje y medio de transporte. El sistema consulta el clima en **5 puntos de la ruta real**, analiza las condiciones meteorológicas con códigos WMO y genera un reporte narrativo en HTML con recomendaciones específicas según el transporte elegido (carro, moto o a pie).

---

## Tecnologías

| Capa | Tecnología |
|------|------------|
| Frontend | HTML5 + CSS3 + JavaScript |
| Backend | Java 17 + Spring Boot 3.2.5 |
| Automatización | n8n (self-hosted) |
| Inteligencia Artificial | Google Gemini (AI Agent en n8n) |
| Base de datos | PostgreSQL en Neon (nube) |
| MCP Server | Node.js + Express |
| Rutas | OpenRouteService API |
| Clima | Open-Meteo API (gratuita, sin API key) |

---

## Estructura del proyecto

```
proyecto_n8n/
├── frontend/           → Interfaz web (HTML, CSS, JS)
├── proyecto-back/      → API REST en Spring Boot
├── mcp-server/         → Servidor MCP en Node.js
├── database/           → Script SQL de la base de datos
└── docs/               → Documentación técnica
```

---

## Flujo del sistema

```
Usuario llena formulario (origen, destino, fecha, transporte)
        │
        ▼
Frontend hace POST a Spring Boot :3001
        │
        ▼
Spring Boot reenvía al webhook de n8n
        │
        ▼
n8n geocodifica origen y destino (OpenRouteService)
        │
        ▼
n8n calcula la ruta según el transporte elegido
        │
        ▼
n8n extrae 5 puntos y consulta Open-Meteo en cada uno
        │
        ▼
Google Gemini genera reporte HTML + datos estructurados
        │
        ▼
Nodo Postgres inserta el registro en Neon
        │
        ▼
Frontend muestra el reporte al usuario ✅
```

---

## Instalación y configuración

### 1. Base de datos
Crear las tablas en Neon ejecutando el script:
```
database/schema.sql
```

### 2. Backend Java
Configurar `proyecto-back/src/main/resources/application.properties`:
```properties
server.port=3001
n8n.webhook.url=https://TU-URL-NGROK.ngrok-free.dev/webhook/clima-ruta
```
Compilar y ejecutar:
```bash
cd proyecto-back
mvn spring-boot:run
```

### 3. MCP Server
```bash
cd mcp-server
npm install
npm run dev
```

### 4. Frontend
Abrir `frontend/index.html` con Live Server en VS Code (puerto 5500).

### 5. n8n
Importar el flujo y configurar las credenciales de:
- OpenRouteService (API key)
- Google Gemini (API key)
- PostgreSQL (cadena de conexión de Neon)

> ⚠️ La URL del webhook de n8n cambia cada vez que se reinicia ngrok. Actualizar en `application.properties` y en `mcp-server/src/tools/getWeather.js`.

---

## Endpoints del backend

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/weather-outfit` | Consulta clima y genera reporte de vestimenta |
| GET | `/api/health` | Verifica que el servidor esté activo |

## Endpoints del MCP Server

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/tool/consultar_clima_ruta` | Llama al webhook de n8n |
| POST | `/tool/recommend_outfit` | Recomendación local sin n8n (fallback) |
| GET | `/tools` | Lista las herramientas disponibles |
| GET | `/health` | Verifica que el MCP Server esté activo |

---

## Documentación

- [`docs/architecture.md`](docs/architecture.md) — Arquitectura del sistema
- [`docs/api-spec.md`](docs/api-spec.md) — Especificación de la API
- [`docs/system-design.md`](docs/system-design.md) — Diseño y decisiones técnicas
