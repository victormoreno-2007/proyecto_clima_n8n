const API_URL = "http://localhost:3001/api/weather-outfit";

let selectedTransport = null;

function selectTransport(btn) {
  document.querySelectorAll(".transport-btn").forEach(b => b.classList.remove("selected"));
  btn.classList.add("selected");
  selectedTransport = btn.dataset.value;
}

async function consultWeather() {
  const origin      = document.getElementById("originInput").value.trim();
  const destination = document.getElementById("destinationInput").value.trim();
  const travelDate  = document.getElementById("dateInput").value;
  const errorMsg    = document.getElementById("errorMsg");

  if (!origin || !destination || !travelDate || !selectedTransport) {
    errorMsg.textContent = "⚠️ Por favor completa todos los campos y selecciona un transporte.";
    errorMsg.classList.remove("hidden");
    return;
  }
  errorMsg.classList.add("hidden");

  showLoading(true);
  hideResult();

  try {
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 180_000); // 3 minutos

    const response = await fetch(API_URL, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ origin, destination, travelDate, transport: selectedTransport }),
      signal: controller.signal
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      const errorData = await response.text();
      console.error("Error del servidor:", errorData);
      throw new Error("Error al procesar la solicitud.");
    }

    const data = await response.json();
    showResult(data);

  } catch (error) {
    console.error(error);
    if (error.name === "AbortError") {
      errorMsg.textContent = "⚠️ La consulta tardó demasiado. Verifica que n8n esté activo.";
    } else {
      errorMsg.textContent = "⚠️ " + (error.message || "Error al consultar el clima.");
    }
    errorMsg.classList.remove("hidden");
  } finally {
    showLoading(false);
  }
}

function formatRecommendation(text) {
  if (!text) return "";
  return text
    .replace(/\*\*(.*?)\*\*/g, "<b>$1</b>")  // **texto** → <b>texto</b>
    .replace(/\*(.*?)\*/g, "<b>$1</b>")        // *texto* → <b>texto</b>
    .replace(/\n/g, "<br>");                    // saltos de línea → <br>
}

function showResult(data) {
  document.getElementById("transportBadge").textContent = iconTransport(data.transport) + " " + data.transport;
  document.getElementById("travelDate").textContent = "📅 " + formatDate(data.travelDate);
  document.getElementById("originName").textContent = "📍 " + data.origin;
  document.getElementById("destName").textContent = "🏁 " + data.destination;

  const recEl = document.getElementById("recommendation");
  recEl.innerHTML = formatRecommendation(data.recommendation);

  document.getElementById("result").classList.remove("hidden");
}

function hideResult() {
  document.getElementById("result").classList.add("hidden");
}

function showLoading(show) {
  const loader = document.getElementById("loading");
  const loadingText = document.querySelector(".loading-section p");
  if (show) {
    loader.classList.remove("hidden");
    const messages = [
      "Consultando el clima de tu ruta...",
      "Calculando puntos intermedios...",
      "Analizando condiciones del clima...",
      "Generando recomendaciones con IA..."
    ];
    let i = 0;
    window._loadingInterval = setInterval(() => {
      if (loadingText) loadingText.textContent = messages[i % messages.length];
      i++;
    }, 3000);
  } else {
    loader.classList.add("hidden");
    if (window._loadingInterval) clearInterval(window._loadingInterval);
  }
}

function resetForm() {
  hideResult();
  document.getElementById("originInput").value = "";
  document.getElementById("destinationInput").value = "";
  document.getElementById("dateInput").value = "";
  document.querySelectorAll(".transport-btn").forEach(b => b.classList.remove("selected"));
  selectedTransport = null;
  document.getElementById("errorMsg").classList.add("hidden");
  document.getElementById("originInput").focus();
}

function formatDate(dateStr) {
  const [year, month, day] = dateStr.split("-");
  const months = ["Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"];
  return `${day} ${months[parseInt(month)-1]} ${year}`;
}

function iconTransport(transport) {
  if (!transport) return "";
  const t = transport.toLowerCase();
  if (t.includes("moto")) return "🏍️";
  if (t.includes("carro") || t.includes("coche")) return "🚗";
  if (t.includes("pie")) return "🚶";
  return "🚌";
}

document.addEventListener("DOMContentLoaded", () => {
  ["originInput","destinationInput","dateInput"].forEach(id => {
    document.getElementById(id).addEventListener("keydown", e => {
      if (e.key === "Enter") consultWeather();
    });
  });
});
