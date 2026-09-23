using System.Text.Json;
using frontend.Models;

namespace frontend.Services;

public static class OpsMapHtmlBuilder
{
    public static string Build(
        double centerLat,
        double centerLng,
        int zoomLevel,
        IReadOnlyList<AnimalDto> animals,
        IReadOnlyList<GeofenceDto> geofences,
        IReadOnlyList<PatrolUnitDto> patrols,
        IReadOnlyList<IncidentAlertDto> incidents,
        IReadOnlyList<TelemetryTrailDto> trail)
    {
        var animalsJson = JsonSerializer.Serialize(animals);
        var geofencesJson = JsonSerializer.Serialize(geofences);
        var patrolsJson = JsonSerializer.Serialize(patrols);
        var incidentsJson = JsonSerializer.Serialize(incidents);
        var trailJson = JsonSerializer.Serialize(trail);

        return $@"<!DOCTYPE html>
<html>
<head>
    <meta charset='utf-8' />
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />
    <style>
        html, body, #map {{ height: 100%; width: 100%; margin: 0; padding: 0; background-color: #1b2610; font-family: Segoe UI, sans-serif; }}
        .leaflet-popup-content-wrapper {{ background: #2b3815; color: #fff; border: 1px solid #FFD700; border-radius: 8px; }}
        .leaflet-popup-tip {{ background: #2b3815; }}
        .leaflet-control-layers {{ background: #2b3815; color: #fff; border: 1px solid #FFD700; }}
        .popup-title {{ font-size: 14px; font-weight: bold; color: #FFD700; margin-bottom: 4px; }}
        .popup-desc {{ font-size: 11px; color: #e0e0e0; line-height: 1.4; }}
        .pulse-marker {{ width: 14px; height: 14px; background: #FFD700; border: 2px solid #fff; border-radius: 50%; box-shadow: 0 0 8px #FFD700; }}
        .alert-marker {{ width: 16px; height: 16px; background: #FF5252; border: 2px solid #fff; border-radius: 50%; animation: pulse 1.2s infinite; }}
        .warning-marker {{ width: 14px; height: 14px; background: #FF9800; border: 2px solid #fff; border-radius: 50%; }}
        .patrol-marker {{ width: 16px; height: 16px; background: #00E5FF; border: 2px solid #fff; border-radius: 3px; transform: rotate(45deg); }}
        .patrol-dispatched-marker {{ width: 18px; height: 18px; background: #FFD700; border: 2px solid #FF5252; border-radius: 3px; transform: rotate(45deg); animation: pulse 1s infinite; }}
        @keyframes pulse {{ 0% {{ box-shadow: 0 0 0 0 rgba(255,82,82,.7); }} 70% {{ box-shadow: 0 0 0 10px rgba(255,82,82,0); }} 100% {{ box-shadow: 0 0 0 0 rgba(255,82,82,0); }} }}
        #live-badge {{ position: absolute; z-index: 1000; left: 10px; bottom: 12px; background: rgba(16,24,8,.88); color: #7CFF6B; font-size: 11px; font-weight: 700; padding: 6px 10px; border-radius: 6px; border: 1px solid #7CFF6B; }}
    </style>
</head>
<body>
    <div id='map'></div>
    <div id='live-badge'>● LIVE COLLAR FEED</div>
    <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
    <script>
        var map = L.map('map', {{ zoomControl: false }}).setView([{centerLat.ToString(System.Globalization.CultureInfo.InvariantCulture)}, {centerLng.ToString(System.Globalization.CultureInfo.InvariantCulture)}], {zoomLevel});
        L.control.zoom({{ position: 'topright' }}).addTo(map);

        var satellite = L.tileLayer('https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{{z}}/{{y}}/{{x}}', {{
            attribution: 'Kenya Wildlife Service • Esri / Maxar',
            maxZoom: 18
        }});
        var streets = L.tileLayer('https://tile.openstreetmap.org/{{z}}/{{x}}/{{y}}.png', {{
            attribution: 'Kenya Wildlife Service • OpenStreetMap',
            maxZoom: 19
        }});
        var topo = L.tileLayer('https://{{s}}.tile.opentopomap.org/{{z}}/{{x}}/{{y}}.png', {{
            attribution: 'Kenya Wildlife Service • OpenTopoMap',
            maxZoom: 17
        }});
        satellite.addTo(map);
        L.control.layers({{ 'Satellite': satellite, 'Streets': streets, 'Terrain': topo }}, null, {{ position: 'topleft', collapsed: true }}).addTo(map);

        var geofences = {geofencesJson};
        var animals = {animalsJson};
        var patrols = {patrolsJson};
        var incidents = {incidentsJson};
        var trailCoords = {trailJson};
        var animalMarkers = {{}};
        var headings = {{}};
        var trails = {{}};
        var trailLines = {{}};
        var parkBounds = {{
            'Amboseli NP': {{ minLat: -2.78, maxLat: -2.55, minLng: 37.15, maxLng: 37.40 }},
            'Tsavo East NP': {{ minLat: -3.40, maxLat: -2.30, minLng: 38.35, maxLng: 39.10 }},
            'Maasai Mara': {{ minLat: -1.65, maxLat: -1.30, minLng: 34.90, maxLng: 35.40 }},
            'Nairobi NP': {{ minLat: -1.42, maxLat: -1.32, minLng: 36.80, maxLng: 36.90 }},
            'Ol Pejeta': {{ minLat: -0.05, maxLat: 0.10, minLng: 36.88, maxLng: 37.05 }}
        }};

        geofences.forEach(function(zone) {{
            if (zone.coordinates && zone.coordinates.length > 0) {{
                L.polygon(zone.coordinates, {{
                    color: zone.colorHex || '#4CAF50',
                    weight: zone.zoneType === 'COMMUNITY_BUFFER' ? 2 : 3,
                    dashArray: zone.zoneType === 'COMMUNITY_BUFFER' ? '5, 5' : null,
                    fillOpacity: zone.zoneType === 'COMMUNITY_BUFFER' ? 0.15 : 0.22
                }}).addTo(map).bindTooltip('<b>' + zone.zoneName + '</b>', {{ sticky: true }});
            }}
        }});

        if (trailCoords && trailCoords.length > 1) {{
            L.polyline(trailCoords.map(function(t) {{ return [t.latitude, t.longitude]; }}), {{
                color: '#00E5FF', weight: 4, opacity: 0.9, dashArray: '6, 6'
            }}).addTo(map);
        }}

        animals.forEach(function(animal) {{
            var isAlert = animal.isBreaching || animal.status === 'Alert';
            var isLowBattery = animal.collarBattery < 20 || animal.status === 'Low Battery';
            var markerClass = isAlert ? 'alert-marker' : (isLowBattery || animal.status === 'IMMOBILE' ? 'warning-marker' : 'pulse-marker');
            var marker = L.marker([animal.latitude, animal.longitude], {{
                icon: L.divIcon({{ className: 'custom-pin', html: '<div class=""' + markerClass + '""></div>', iconSize: [16, 16], iconAnchor: [8, 8] }})
            }}).addTo(map);
            marker.bindPopup('<div class=""popup-title"">' + animal.name + ' (' + animal.species + ')</div><div class=""popup-desc""><b>Collar:</b> ' + animal.collarId + '<br><b>Park:</b> ' + animal.parkName + '</div>');
            animalMarkers[animal.collarId] = marker;
            trails[animal.collarId] = [[animal.latitude, animal.longitude]];
            headings[animal.collarId] = Math.random() * Math.PI * 2;
        }});

        var markerList = Object.keys(animalMarkers).map(function(k) {{ return animalMarkers[k]; }});
        if (markerList.length > 0) {{
            map.fitBounds(L.featureGroup(markerList).getBounds().pad(0.4));
        }}

        patrols.forEach(function(patrol) {{
            var isDispatched = patrol.status === 'DISPATCHED';
            L.marker([patrol.latitude, patrol.longitude], {{
                icon: L.divIcon({{ className: 'custom-patrol-pin', html: '<div class=""' + (isDispatched ? 'patrol-dispatched-marker' : 'patrol-marker') + '""></div>', iconSize: [18, 18], iconAnchor: [9, 9] }})
            }}).addTo(map).bindPopup('<div class=""popup-title"" style=""color:#00E5FF;"">' + patrol.name + '</div>');
        }});

        incidents.forEach(function(inc) {{
            if (inc.isDispatched && patrols && patrols.length > 0) {{
                var assigned = patrols.find(function(p) {{ return p.status === 'DISPATCHED'; }}) || patrols[0];
                if (assigned) {{
                    L.polyline([[assigned.latitude, assigned.longitude], [inc.latitude, inc.longitude]], {{
                        color: '#FF9800', weight: 3, dashArray: '8, 8', opacity: 0.95
                    }}).addTo(map);
                }}
            }}
        }});

        function clamp(v, a, b) {{ return Math.max(a, Math.min(b, v)); }}
        function boundsFor(animal) {{
            var b = parkBounds[animal.parkName] || {{ minLat: -4.7, maxLat: 1.2, minLng: 33.9, maxLng: 41.9 }};
            if (animal.isBreaching) {{
                return {{ minLat: b.minLat - 0.05, maxLat: b.maxLat + 0.05, minLng: b.minLng - 0.05, maxLng: b.maxLng + 0.05 }};
            }}
            return b;
        }}
        function stepLength(species) {{
            var s = (species || '').toLowerCase();
            if (s.indexOf('cheetah') >= 0) return 0.0030;
            if (s.indexOf('lion') >= 0) return 0.0022;
            if (s.indexOf('rhino') >= 0) return 0.0011;
            if (s.indexOf('elephant') >= 0) return 0.0018;
            return 0.0016;
        }}
        function animateMarker(marker, toLat, toLng) {{
            var from = marker.getLatLng();
            var start = null;
            function frame(ts) {{
                if (!start) start = ts;
                var t = Math.min(1, (ts - start) / 1600);
                marker.setLatLng([from.lat + (toLat - from.lat) * t, from.lng + (toLng - from.lng) * t]);
                if (t < 1) requestAnimationFrame(frame);
            }}
            requestAnimationFrame(frame);
        }}

        function tickSimulation() {{
            animals.forEach(function(animal) {{
                if (animal.status === 'IMMOBILE') return;
                var id = animal.collarId;
                headings[id] += (Math.random() - 0.5) * 0.8;
                var step = stepLength(animal.species);
                var nextLat = animal.latitude + Math.cos(headings[id]) * step;
                var nextLng = animal.longitude + Math.sin(headings[id]) * step;
                var b = boundsFor(animal);
                var lat = clamp(nextLat, b.minLat, b.maxLat);
                var lng = clamp(nextLng, b.minLng, b.maxLng);
                if (lat !== nextLat || lng !== nextLng) headings[id] += Math.PI;
                animal.latitude = lat;
                animal.longitude = lng;
                var marker = animalMarkers[id];
                if (marker) animateMarker(marker, lat, lng);
                trails[id].push([lat, lng]);
                if (trails[id].length > 20) trails[id].shift();
                if (trails[id].length > 1) {{
                    if (!trailLines[id]) {{
                        trailLines[id] = L.polyline(trails[id], {{ color: '#FFD700', weight: 2.5, opacity: 0.8 }}).addTo(map);
                    }} else {{
                        trailLines[id].setLatLngs(trails[id]);
                    }}
                }}
            }});
            var badge = document.getElementById('live-badge');
            if (badge) badge.textContent = '● LIVE COLLAR FEED  ' + new Date().toLocaleTimeString();
        }}

        window.updateAnimalPositions = function(updates) {{
            if (!updates || !updates.length) return 'ok';
            updates.forEach(function(u) {{
                var marker = animalMarkers[u.collarId];
                var animal = animals.find(function(a) {{ return a.collarId === u.collarId; }});
                if (animal) {{ animal.latitude = u.latitude; animal.longitude = u.longitude; }}
                if (marker) marker.setLatLng([u.latitude, u.longitude]);
            }});
            return 'ok';
        }};

        setInterval(tickSimulation, 2500);
        setTimeout(tickSimulation, 600);
    </script>
</body>
</html>";
    }
}
