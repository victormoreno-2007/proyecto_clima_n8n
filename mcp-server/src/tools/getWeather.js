import fetch from "node-fetch";

const N8N_WEBHOOK_URL = "https://renunciative-velma-spartanly.ngrok-free.dev/workflow/E91wk5Fqs7W1MDmS";

/**
 * Tool: consultar_clima_ruta
 * Llama al webhook de n8n con los datos del viaje
 * y devuelve el reporte generado por el AI Agent.
 */
export async function consultarClimaRuta({ origin, destination, travelDate, transport }) {
  const response = await fetch(N8N_WEBHOOK_URL, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      "Lugar de origen": origin,
      "Destino": destination,
      "Dia de viaje": travelDate,
      "Metodo de transporte": capitalize(transport)
    })
  });

  if (!response.ok) {
    throw new Error(`Error al llamar a n8n: ${response.status}`);
  }

  const data = await response.json();
  return data;
}

function capitalize(text) {
  if (!text) return text;
  const lower = text.toLowerCase();
  if (lower === "a pie") return "A pie";
  return lower.charAt(0).toUpperCase() + lower.slice(1);
}
