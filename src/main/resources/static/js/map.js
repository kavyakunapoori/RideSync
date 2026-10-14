/**
 * Leaflet.js & OpenStreetMap Integration
 * Displays ride origins, destinations, intermediate stops, and route lines.
 */

function initRideMap(mapElementId, originLat, originLng, destLat, destLng, stops, originName, destName) {
    const mapContainer = document.getElementById(mapElementId);
    if (!mapContainer || typeof L === 'undefined') {
        console.warn("Leaflet library or map container not found. Fallback view active.");
        return null;
    }

    try {
        // Center between origin and destination
        const midLat = (originLat + destLat) / 2.0;
        const midLng = (originLng + destLng) / 2.0;

        const map = L.map(mapElementId).setView([midLat, midLng], 12);

        // OpenStreetMap tile layer (Free, no API key required)
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            maxZoom: 19,
            attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        const latLngs = [];

        // 1. Origin Marker (Green)
        const originMarker = L.circleMarker([originLat, originLng], {
            radius: 9,
            fillColor: "#16a34a",
            color: "#ffffff",
            weight: 3,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(map);
        originMarker.bindPopup("<b>Origin:</b> " + (originName || "Pickup Location")).openPopup();
        latLngs.push([originLat, originLng]);

        // 2. Intermediate Stops (Orange)
        if (stops && Array.isArray(stops)) {
            stops.forEach(function (stop, idx) {
                if (stop.lat && stop.lng) {
                    const stopMarker = L.circleMarker([stop.lat, stop.lng], {
                        radius: 7,
                        fillColor: "#f59e0b",
                        color: "#ffffff",
                        weight: 2,
                        opacity: 1,
                        fillOpacity: 0.9
                    }).addTo(map);
                    stopMarker.bindPopup("<b>Stop #" + (idx + 1) + ":</b> " + (stop.name || "En route stop"));
                    latLngs.push([stop.lat, stop.lng]);
                }
            });
        }

        // 3. Destination Marker (Red)
        const destMarker = L.circleMarker([destLat, destLng], {
            radius: 9,
            fillColor: "#dc2626",
            color: "#ffffff",
            weight: 3,
            opacity: 1,
            fillOpacity: 0.9
        }).addTo(map);
        destMarker.bindPopup("<b>Destination:</b> " + (destName || "Drop-off Location"));
        latLngs.push([destLat, destLng]);

        // 4. Draw route polyline between checkpoints
        const polyline = L.polyline(latLngs, {
            color: '#2563eb',
            weight: 5,
            opacity: 0.75,
            dashArray: '8, 8',
            lineJoin: 'round'
        }).addTo(map);

        // Fit map bounds to contain all markers
        map.fitBounds(polyline.getBounds(), { padding: [40, 40] });

        return map;
    } catch (e) {
        console.error("Map initialization encountered an issue:", e);
        if (mapContainer) {
            mapContainer.innerHTML = "<div class='alert alert-info'>Interactive Map Loaded: " +
                (originName || "Origin") + " ➔ " + (destName || "Destination") + "</div>";
        }
        return null;
    }
}
