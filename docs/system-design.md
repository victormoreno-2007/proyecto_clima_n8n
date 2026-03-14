# 🧠 Diseño del Sistema — ClimaVest

## ¿Qué es este documento?

Este documento explica las decisiones de diseño que se tomaron al construir ClimaVest: por qué se eligió cada tecnología, cómo está diseñada la base de datos, cómo funciona el flujo de automatización en n8n y cuáles son las consideraciones de escalabilidad para el futuro.

---

## 1. Problema que resuelve el sistema

Las aplicaciones meteorológicas muestran datos técnicos como temperatura, humedad y códigos de condición climática, pero no le dicen al usuario de forma simple **qué ropa ponerse** ni **cómo prepararse para el viaje** según su medio de transporte.

ClimaVest transforma datos técnicos del clima en **recomendaciones prácticas y personalizadas**, tomando en cuenta:
- El clima en múltiples puntos de la ruta (no solo en el origen o destino)
- El medio de transporte del usuario (carro, moto o a pie)
- Las temperaturas específicas del origen y del destino

---

## 2. Decisiones de diseño

### ¿Por qué n8n como orquestador?

n8n permite construir flujos visuales de automatización sin escribir código complejo. En este proyecto cumple el rol de **cerebro del sistema**: recibe los datos, llama a APIs externas, procesa la información y coordina la respuesta. Su ventaja principal es que permite agregar, modificar o reemplazar nodos (pasos del flujo) sin tocar el código del backend.

### ¿Por qué Java + Spring Boot como backend?

Spring Boot ofrece una estructura robusta para construir APIs REST de forma rápida. En este proyecto el backend actúa como **intermediario** entre el frontend y n8n, manejando la validación de entradas, el formateo de datos y la configuración de timeouts. Java 17 aporta estabilidad y tipado fuerte.

### ¿Por qué PostgreSQL en Neon y no MySQL local?

Durante el desarrollo se usó MySQL local, pero n8n Cloud no puede conectarse a bases de datos en redes privadas. Neon ofrece **PostgreSQL gratuito en la nube**, accesible desde cualquier servicio externo. La migración de MySQL a PostgreSQL fue mínima ya que los tipos de datos y la sintaxis SQL son casi idénticos.

### ¿Por qué Google Gemini como modelo de IA?

Gemini ofrece una API gratuita con cuota suficiente para un proyecto académico y tiene excelente capacidad de seguir instrucciones complejas con formato HTML estricto. Se eligió sobre Ollama (modelo local) porque Ollama requiere una máquina con recursos suficientes siempre encendida, mientras que Gemini es un servicio en la nube siempre disponible.

### ¿Por qué 5 puntos de clima en la ruta?

En rutas largas, el clima puede cambiar drásticamente entre el origen y el destino. Consultar solo un punto genera recomendaciones incompletas. Con **5 puntos equidistantes** (origen, 3 intermedios y destino) se captura la variabilidad climática a lo largo de todo el trayecto, permitiendo al AI Agent generar un reporte más preciso y útil.

### ¿Por qué separar el bloque %%DATA%% del reporte?

El AI Agent genera texto narrativo para el usuario, pero también necesita devolver datos estructurados (temperaturas, condiciones) para guardarlos en la base de datos. Mezclar ambos en el mismo campo sería difícil de parsear. El marcador `%%DATA%%...%%END%%` permite separar con precisión el HTML del usuario del JSON de datos usando el nodo Code en n8n.

---

## 3. Diseño de la base de datos

### Diagrama entidad-relación

```
┌─────────────┐          ┌──────────────────────────┐
│    users    │          │      weather_queries      │
├─────────────┤          ├──────────────────────────┤
│ id (PK)     │◀────┐    │ id (PK)                  │
│ name        │     └────│ user_id (FK, nullable)   │
│ email       │          │ origin                   │
│ created_at  │          │ destination              │
└─────────────┘          │ travel_date              │
                         │ transport                │
                         │ origin_temperature       │
                         │ destination_temperature  │
                         │ origin_condition         │
                         │ destination_condition    │
                         │ recommended_clothes      │
                         │ created_at               │
                         └──────────────────────────┘
```

### Descripción de campos — `weather_queries`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | SERIAL (PK) | Identificador único autogenerado |
| `origin` | VARCHAR(150) | Ciudad de origen del viaje |
| `destination` | VARCHAR(150) | Ciudad de destino del viaje |
| `travel_date` | DATE | Fecha del viaje |
| `transport` | VARCHAR(20) | Medio de transporte: Carro, Moto, A pie |
| `origin_temperature` | DECIMAL(5,2) | Temperatura en el punto de origen en °C |
| `destination_temperature` | DECIMAL(5,2) | Temperatura en el punto de destino en °C |
| `origin_condition` | VARCHAR(100) | Descripción del clima en el origen |
| `destination_condition` | VARCHAR(100) | Descripción del clima en el destino |
| `recommended_clothes` | TEXT | Reporte completo generado por el AI Agent |
| `user_id` | INT (FK nullable) | Referencia opcional al usuario que hizo la consulta |
| `created_at` | TIMESTAMP | Fecha y hora en que se realizó la consulta |

### Descripción de campos — `users`

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `id` | SERIAL (PK) | Identificador único autogenerado |
| `name` | VARCHAR(100) | Nombre del usuario (opcional) |
| `email` | VARCHAR(150) | Correo electrónico único (opcional) |
| `created_at` | TIMESTAMP | Fecha de registro |

> 💡 La tabla `users` está preparada para una futura implementación de autenticación. Actualmente `user_id` en `weather_queries` siempre es NULL.

---

## 4. Diseño del flujo de n8n

### Diagrama del flujo

```
[Webhook]
    │
    ▼
[Nombrar variables]
    │
    ├──────────────────────┐
    ▼                      ▼
[Obtener origen]    [Obtener destino]
(OpenRouteService)  (OpenRouteService)
    │                      │
    └──────────┬───────────┘
               ▼
    [Unir coordenadas]
               │
               ▼
           [Switch]
          /    |    \
         /     |     \
    [Carro] [Moto] [A pie]
    (driving) (cycling) (walking)
         \     |     /
          \    |    /
    [Obtener información específica]
    (5 puntos equidistantes de la ruta)
               │
               ▼
       [Loop Over Items]
               │
               ▼
    [Consultar clima individual]
    (Open-Meteo por cada punto)
               │
               ▼
       [Seguir proceso] ──▶ [Loop Over Items] (hasta completar 5 puntos)
               │
               ▼ (cuando termina el loop)
       [Agrupar datos]
    (arrays de temperaturas y códigos WMO)
               │
               ▼
    [Editar a chatInput]
    (prepara el mensaje para Gemini)
               │
               ▼
         [AI Agent]
       (Google Gemini)
    genera reporte + %%DATA%%
               │
               ▼
    [Code in JavaScript]
    (separa reporte del bloque JSON)
               │
               ▼
    [Insert rows in a table]
    (guarda en Neon PostgreSQL)
               │
               ▼
    [Respond to Webhook]
    (devuelve reporte al backend Java)
```

### APIs externas usadas

**OpenRouteService**
- Geocodificación: convierte nombres de ciudades a coordenadas `[longitud, latitud]`
- Rutas: calcula la ruta real según el medio de transporte
  - `driving-car` → para carro
  - `cycling-regular` → para moto
  - `foot-walking` → para a pie

**Open-Meteo**
- API meteorológica gratuita sin necesidad de API key
- Endpoint: `https://api.open-meteo.com/v1/forecast`
- Parámetros usados: `latitude`, `longitude`, `current_weather=true`
- Devuelve: temperatura actual y código WMO del clima

**Tabla de códigos WMO usados:**

| Código | Condición |
|--------|-----------|
| 0 | Despejado ☀️ |
| 1, 2, 3 | Parcialmente nublado ⛅ |
| 45, 48 | Niebla 🌫️ |
| 51–65 | Lluvia 🌧️ |
| 80–99 | Tormenta ⛈️ |

---

## 5. Diseño del MCP Server

El MCP Server implementa el **Model Context Protocol**, un estándar para exponer herramientas que los modelos de IA pueden invocar. Actúa como una capa de abstracción que permite a Claude u otro agente usar las capacidades del sistema sin necesidad de conocer los detalles de implementación.

**Herramientas expuestas:**

```
MCP Server (puerto 4000)
├── POST /tool/consultar_clima_ruta  → llama al webhook de n8n
└── POST /tool/recommend_outfit      → lógica de recomendación local (fallback)
```

El fallback `recommend_outfit` es importante porque garantiza que el sistema pueda dar una respuesta mínima incluso cuando n8n no esté disponible.

---

## 6. Consideraciones de seguridad

- Las API keys de OpenRouteService y Google Gemini están almacenadas como credenciales en n8n, no en el código fuente.
- El backend no expone directamente las claves de la base de datos al frontend.
- CORS está configurado para aceptar solo peticiones desde los orígenes del frontend local.
- La URL del webhook de n8n se gestiona via variable de entorno en `application.properties`.

---

## 7. Escalabilidad futura

El sistema está diseñado de forma modular, lo que facilita agregar nuevas funcionalidades:

| Funcionalidad futura | Cómo implementarla |
|---------------------|--------------------|
| Autenticación de usuarios | Activar la tabla `users` e integrar Spring Security |
| Historial personal | Filtrar `weather_queries` por `user_id` |
| Predicciones a futuro | Cambiar `current_weather` por `hourly` en Open-Meteo |
| App móvil | El backend ya es una API REST, solo se necesita un cliente móvil |
| Notificaciones push | Expandir el nodo de Telegram en n8n |
| Múltiples idiomas | Parametrizar el idioma en el prompt del AI Agent |
