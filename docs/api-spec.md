# 📡 Especificación de la API — ClimaVest

## ¿Qué es este documento?

Este documento describe todos los endpoints disponibles en el sistema ClimaVest: qué reciben, qué devuelven y cómo usarlos. Es la referencia técnica para cualquier desarrollador que quiera integrar o probar el sistema.

---

## Base URL

```
http://localhost:3001
```

> El backend Java corre localmente en el puerto 3001. Para acceso externo se requiere exponer el puerto via ngrok u otro proxy.

---

## Endpoints del Backend (Spring Boot)

---

### 1. Consultar clima y recomendación de vestimenta

Recibe los datos del viaje, los envía a n8n y devuelve el reporte generado por el AI Agent.

```
POST /api/weather-outfit
```

**Headers requeridos:**
```
Content-Type: application/json
```

**Body de la petición:**
```json
{
  "origin": "Cúcuta",
  "destination": "Bogotá",
  "travelDate": "2026-03-15",
  "transport": "carro"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `origin` | string | ✅ | Ciudad o lugar de origen del viaje |
| `destination` | string | ✅ | Ciudad o lugar de destino del viaje |
| `travelDate` | string | ✅ | Fecha del viaje en formato `yyyy-MM-dd` |
| `transport` | string | ✅ | Medio de transporte: `carro`, `moto` o `a pie` |

**Respuesta exitosa (200 OK):**
```json
{
  "origin": "Cúcuta",
  "destination": "Bogotá",
  "travelDate": "2026-03-15",
  "transport": "carro",
  "originTemperature": null,
  "originCondition": null,
  "destinationTemperature": null,
  "destinationCondition": null,
  "recommendation": "¡Buenos días Víctor! 👋🚗<br><b>🌤️ El clima en tu ruta:</b><br>Tu viaje comienza con cielos despejados..."
}
```

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `origin` | string | Origen tal como lo ingresó el usuario |
| `destination` | string | Destino tal como lo ingresó el usuario |
| `travelDate` | string | Fecha del viaje |
| `transport` | string | Medio de transporte capitalizado |
| `originTemperature` | number / null | Temperatura en el origen (actualmente null, gestionado por n8n) |
| `originCondition` | string / null | Condición climática en el origen (actualmente null) |
| `destinationTemperature` | number / null | Temperatura en el destino (actualmente null) |
| `destinationCondition` | string / null | Condición climática en el destino (actualmente null) |
| `recommendation` | string | Reporte completo generado por el AI Agent en formato HTML |

**Respuesta de error (500):**
```json
{
  "timestamp": "2026-03-15T10:00:00.000+00:00",
  "status": 500,
  "error": "Internal Server Error",
  "message": "Error al procesar la solicitud"
}
```

**Timeout:** La petición tiene un límite de 3 minutos (180,000 ms) ya que n8n puede tardar hasta 2 minutos en procesar el flujo completo.

---

### 2. Health Check

Verifica que el servidor esté corriendo correctamente.

```
GET /api/health
```

**Respuesta exitosa (200 OK):**
```
Weather Assistant API running OK
```

---

## Endpoints del MCP Server (Node.js)

El MCP Server corre en el puerto `4000` y expone herramientas independientes del backend Java.

---

### 3. Listar herramientas disponibles

```
GET /tools
```

**Respuesta exitosa (200 OK):**
```json
{
  "tools": [
    {
      "name": "consultar_clima_ruta",
      "description": "Consulta el clima de una ruta y genera recomendación de vestimenta usando n8n",
      "endpoint": "POST /tool/consultar_clima_ruta",
      "params": ["origin", "destination", "travelDate", "transport"]
    },
    {
      "name": "recommend_outfit",
      "description": "Genera recomendación de vestimenta por temperatura y condición (fallback)",
      "endpoint": "POST /tool/recommend_outfit",
      "params": ["temperature", "condition", "transport"]
    }
  ]
}
```

---

### 4. Consultar clima de ruta (vía n8n)

Llama directamente al webhook de n8n con los datos del viaje.

```
POST /tool/consultar_clima_ruta
```

**Body de la petición:**
```json
{
  "origin": "Medellín",
  "destination": "Cartagena",
  "travelDate": "2026-03-20",
  "transport": "carro"
}
```

**Respuesta exitosa (200 OK):**
```json
{
  "output": "¡Buenos días Víctor! 👋🚗 ..."
}
```

**Respuesta de error (400):**
```json
{
  "error": "Faltan campos: origin, destination, travelDate, transport"
}
```

---

### 5. Recomendar vestimenta (fallback local)

Genera una recomendación de vestimenta basada en temperatura y condición climática, sin necesidad de conectarse a n8n. Útil como respaldo cuando n8n no está disponible.

```
POST /tool/recommend_outfit
```

**Body de la petición:**
```json
{
  "temperature": 14,
  "condition": "lluvia",
  "transport": "moto"
}
```

| Campo | Tipo | Requerido | Descripción |
|-------|------|-----------|-------------|
| `temperature` | number | ✅ | Temperatura en grados Celsius |
| `condition` | string | ❌ | Condición climática: lluvia, viento, nieve, etc. |
| `transport` | string | ❌ | Medio de transporte: carro, moto, a pie |

**Respuesta exitosa (200 OK):**
```json
{
  "recommendation": "Usa chaqueta ligera o suéter. Lleva paraguas o impermeable. En moto: usa casco, chaqueta con protecciones y guantes."
}
```

**Lógica de recomendación:**

| Temperatura | Recomendación base |
|-------------|-------------------|
| Menos de 10°C | Abrigo grueso, bufanda y pantalón largo |
| Entre 10°C y 17°C | Chaqueta ligera o suéter |
| Entre 18°C y 25°C | Ropa cómoda de media estación |
| Más de 25°C | Ropa ligera y fresca |

Adicionalmente, si la condición incluye **lluvia** → paraguas o impermeable, **viento** → cortavientos, **nieve** → botas impermeables y ropa térmica.

---

### 6. Health Check del MCP Server

```
GET /health
```

**Respuesta exitosa (200 OK):**
```json
{
  "status": "MCP Server running ✅",
  "port": 4000
}
```

---

## Webhook de n8n

El backend Java se comunica directamente con n8n a través de este webhook.

```
POST https://renunciative-velma-spartanly.ngrok-free.dev/workflow/E91wk5Fqs7W1MDmS
```

**Body que envía el backend:**
```json
{
  "Lugar de origen": "Cúcuta",
  "Destino": "Bogotá",
  "Dia de viaje": "2026-03-15",
  "Metodo de transporte": "Carro"
}
```

> ⚠️ La URL del webhook cambia cada vez que se reinicia ngrok. Se configura en `application.properties` bajo la clave `n8n.webhook.url`.

**Respuesta que devuelve n8n:**
```json
{
  "recommended_clothes": "¡Buenos días Víctor! 👋🚗<br><b>🌤️ El clima en tu ruta:</b>..."
}
```

---

## Configuración de CORS

El backend tiene CORS habilitado para permitir peticiones desde el frontend local:

```java
// Orígenes permitidos: http://localhost:5500, http://127.0.0.1:5500
// Métodos permitidos: GET, POST, PUT, DELETE, OPTIONS
// Headers permitidos: todos
```

Si el frontend se sirve desde un puerto diferente, se debe actualizar la configuración en `CorsConfig.java`.
