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
        .leaflet-popup-content-wrapper {{ background: #1e2b10; color: #fff; border: 1.5px solid #FFD700; border-radius: 8px; box-shadow: 0 4px 14px rgba(0,0,0,0.5); }}
        .leaflet-popup-tip {{ background: #1e2b10; }}
        .leaflet-control-layers {{ background: #1e2b10; color: #fff; border: 1px solid #FFD700; border-radius: 6px; font-size: 11px; }}
        .popup-title {{ font-size: 13px; font-weight: bold; color: #FFD700; margin-bottom: 3px; display: flex; align-items: center; gap: 4px; }}
        .popup-species {{ font-size: 10px; color: #7CFF6B; font-weight: 600; text-transform: uppercase; margin-bottom: 4px; }}
        .popup-desc {{ font-size: 11px; color: #e2e8f0; line-height: 1.45; }}
        .popup-chip {{ display: inline-block; background: rgba(255,215,0,0.15); border: 1px solid #FFD700; color: #FFD700; padding: 1px 6px; border-radius: 4px; font-size: 10px; margin-top: 4px; }}
        .pulse-marker {{ width: 14px; height: 14px; background: #FFD700; border: 2px solid #fff; border-radius: 50%; box-shadow: 0 0 10px #FFD700; }}
        .alert-marker {{ width: 16px; height: 16px; background: #FF5252; border: 2px solid #fff; border-radius: 50%; animation: pulse 1.2s infinite; }}
        .warning-marker {{ width: 14px; height: 14px; background: #FF9800; border: 2px solid #fff; border-radius: 50%; }}
        .patrol-marker {{ width: 16px; height: 16px; background: #00E5FF; border: 2px solid #fff; border-radius: 3px; transform: rotate(45deg); }}
        .patrol-dispatched-marker {{ width: 18px; height: 18px; background: #FFD700; border: 2px solid #FF5252; border-radius: 3px; transform: rotate(45deg); animation: pulse 1s infinite; }}
        .landmark-marker {{ width: 8px; height: 8px; background: #00E5FF; border: 1.5px solid #fff; border-radius: 50%; opacity: 0.85; }}
        @keyframes pulse {{ 0% {{ box-shadow: 0 0 0 0 rgba(255,82,82,.7); }} 70% {{ box-shadow: 0 0 0 10px rgba(255,82,82,0); }} 100% {{ box-shadow: 0 0 0 0 rgba(255,82,82,0); }} }}
        #live-badge {{ position: absolute; z-index: 1000; left: 10px; bottom: 12px; background: rgba(16,24,8,.92); color: #7CFF6B; font-size: 11px; font-weight: 700; padding: 6px 10px; border-radius: 6px; border: 1px solid #7CFF6B; box-shadow: 0 2px 8px rgba(0,0,0,0.4); }}
        #fence-badge {{ position: absolute; z-index: 1000; right: 10px; bottom: 12px; background: rgba(26,18,8,.92); color: #FFD700; font-size: 10px; font-weight: 600; padding: 5px 9px; border-radius: 6px; border: 1px solid #FF9800; }}
    </style>
</head>
<body>
    <div id='map'></div>
    <div id='live-badge'>● LIVE COLLAR FIXES</div>
    <div id='fence-badge'>⚡ SANCTUARY FENCES ACTIVE</div>
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
        var trails = {{}};
        var trailLines = {{}};

        // Key ecological waterholes and landmarks
        var parkLandmarks = [
            {{ name: 'Hippo Pools & Mbagathi River', lat: -1.3880, lng: 36.8720, park: 'Nairobi NP' }},
            {{ name: 'Nagolomon Dam', lat: -1.3820, lng: 36.8250, park: 'Nairobi NP' }},
            {{ name: 'Hyena Dam Plains', lat: -1.3650, lng: 36.8400, park: 'Nairobi NP' }},
            {{ name: 'Athi River Basin', lat: -1.4050, lng: 36.8900, park: 'Nairobi NP' }},
            {{ name: 'KWS Ivory Burning Site', lat: -1.3560, lng: 36.7820, park: 'Nairobi NP' }},
            {{ name: 'Enkongo Narok Swamp', lat: -2.6650, lng: 37.2650, park: 'Amboseli NP' }},
            {{ name: 'Ol Tukai Springs', lat: -2.6500, lng: 37.3100, park: 'Amboseli NP' }},
            {{ name: 'Aruba Dam', lat: -3.1150, lng: 38.8250, park: 'Tsavo East NP' }},
            {{ name: 'Galana River Rapids', lat: -2.8124, lng: 38.6210, park: 'Tsavo East NP' }},
            {{ name: 'Talek River Crossing', lat: -1.4420, lng: 35.2150, park: 'Maasai Mara' }},
            {{ name: 'Mara River Marsh', lat: -1.5100, lng: 35.0350, park: 'Maasai Mara' }}
        ];

        // Draw Park Geofences
        geofences.forEach(function(zone) {{
            if (zone.coordinates && zone.coordinates.length > 0) {{
                L.polygon(zone.coordinates, {{
                    color: zone.colorHex || '#4CAF50',
                    weight: zone.zoneType === 'COMMUNITY_BUFFER' ? 2 : 3,
                    dashArray: zone.zoneType === 'COMMUNITY_BUFFER' ? '5, 5' : null,
                    fillOpacity: zone.zoneType === 'COMMUNITY_BUFFER' ? 0.12 : 0.20
                }}).addTo(map).bindTooltip('<b>' + zone.zoneName + '</b>', {{ sticky: true }});
            }}
        }});

        // Draw Nairobi NP Northern Perimeter Fence (Explicitly separating park from Nairobi city)
        var nairobiCityFence = [
            [-1.3500, 36.7760], // KWS HQ
            [-1.3465, 36.7950], // Langata Sanctuary
            [-1.3420, 36.8200], // Southern Bypass
            [-1.3425, 36.8450], // South of Wilson Airport
            [-1.3440, 36.8720], // South of South C / Nairobi West
            [-1.3520, 36.8980], // Mombasa Road / Depot
            [-1.3620, 36.9250]  // Syokimau
        ];
        L.polyline(nairobiCityFence, {{
            color: '#FFD700',
            weight: 3.5,
            dashArray: '6, 6',
            opacity: 0.95
        }}).addTo(map).bindTooltip('⚡ <b>Nairobi NP Northern Perimeter Electric Fence</b><br>Separates Wildlife Sanctuary from Nairobi City', {{ sticky: true }});

        // Draw subtle ecological landmarks
        parkLandmarks.forEach(function(lm) {{
            L.marker([lm.lat, lm.lng], {{
                icon: L.divIcon({{ className: 'custom-lm', html: '<div class=""landmark-marker""></div>', iconSize: [8, 8], iconAnchor: [4, 4] }})
            }}).addTo(map).bindTooltip('💧 <b>' + lm.name + '</b> (' + lm.park + ')', {{ direction: 'top', opacity: 0.85 }});
        }});

        // Draw selected GPS Trail if present
        if (trailCoords && trailCoords.length > 1) {{
            L.polyline(trailCoords.map(function(t) {{ return [t.latitude, t.longitude]; }}), {{
                color: '#00E5FF', weight: 4, opacity: 0.9, dashArray: '6, 6'
            }}).addTo(map);
        }}

        function buildPopupHtml(animal) {{
            var speedText = animal.speedDisplay || (animal.speedKmh ? animal.speedKmh.toFixed(1) + ' km/h' : '2.1 km/h');
            var headingText = animal.headingCompass || 'SE 125°';
            var behavior = animal.behaviorState || '🌿 Foraging in Sanctuary';
            var fenceText = animal.fenceDistance || 'Safe inside park';

            return '<div class=""popup-title"">' + animal.name + '</div>' +
                   '<div class=""popup-species"">' + animal.species + ' • ' + animal.parkName + '</div>' +
                   '<div class=""popup-desc"">' +
                   '<b>Collar:</b> ' + animal.collarId + '<br>' +
                   '<b>Speed:</b> ' + speedText + ' (' + headingText + ')<br>' +
                   '<b>Activity:</b> ' + behavior + '<br>' +
                   '<span class=""popup-chip"">🛡️ ' + fenceText + '</span>' +
                   '</div>';
        }}

        // Render Animals
        animals.forEach(function(animal) {{
            var isAlert = animal.isBreaching || animal.status === 'Alert';
            var isLowBattery = animal.collarBattery < 20 || animal.status === 'Low Battery';
            var markerClass = isAlert ? 'alert-marker' : (isLowBattery || animal.status === 'IMMOBILE' ? 'warning-marker' : 'pulse-marker');
            var marker = L.marker([animal.latitude, animal.longitude], {{
                icon: L.divIcon({{ className: 'custom-pin', html: '<div class=""' + markerClass + '""></div>', iconSize: [16, 16], iconAnchor: [8, 8] }})
            }}).addTo(map);

            marker.bindPopup(buildPopupHtml(animal));
            animalMarkers[animal.collarId] = marker;
            trails[animal.collarId] = [[animal.latitude, animal.longitude]];
        }});

        var markerList = Object.keys(animalMarkers).map(function(k) {{ return animalMarkers[k]; }});
        if (markerList.length > 0) {{
            map.fitBounds(L.featureGroup(markerList).getBounds().pad(0.35));
        }}

        // Render Patrol Units
        patrols.forEach(function(patrol) {{
            var isDispatched = patrol.status === 'DISPATCHED';
            L.marker([patrol.latitude, patrol.longitude], {{
                icon: L.divIcon({{ className: 'custom-patrol-pin', html: '<div class=""' + (isDispatched ? 'patrol-dispatched-marker' : 'patrol-marker') + '""></div>', iconSize: [18, 18], iconAnchor: [9, 9] }})
            }}).addTo(map).bindPopup('<div class=""popup-title"" style=""color:#00E5FF;"">' + patrol.name + '</div><div class=""popup-desc""><b>Sector:</b> ' + patrol.sector + '<br><b>Status:</b> ' + patrol.status + '</div>');
        }});

        // Render Incident Vectors
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

        // Smooth Marker Gliding Animation
        function animateMarker(marker, toLat, toLng) {{
            var from = marker.getLatLng();
            var start = null;
            var duration = 2200;
            function frame(ts) {{
                if (!start) start = ts;
                var progress = Math.min(1, (ts - start) / duration);
                var ease = 0.5 - Math.cos(progress * Math.PI) / 2; // smooth ease-in-out
                marker.setLatLng([from.lat + (toLat - from.lat) * ease, from.lng + (toLng - from.lng) * ease]);
                if (progress < 1) requestAnimationFrame(frame);
            }}
            requestAnimationFrame(frame);
        }}

        // Live Position Update bridge called by C# ViewModel
        window.updateAnimalPositions = function(updates) {{
            if (!updates || !updates.length) return 'ok';
            updates.forEach(function(u) {{
                var marker = animalMarkers[u.collarId];
                var animal = animals.find(function(a) {{ return a.collarId === u.collarId; }});
                if (animal) {{
                    animal.latitude = u.latitude;
                    animal.longitude = u.longitude;
                    if (u.speedDisplay) animal.speedDisplay = u.speedDisplay;
                    if (u.headingCompass) animal.headingCompass = u.headingCompass;
                    if (u.behaviorState) animal.behaviorState = u.behaviorState;
                    if (u.fenceDistance) animal.fenceDistance = u.fenceDistance;
                }}
                if (marker) {{
                    animateMarker(marker, u.latitude, u.longitude);
                    if (animal && marker.getPopup()) {{
                        marker.setPopupContent(buildPopupHtml(animal));
                    }}
                }}

                if (!trails[u.collarId]) trails[u.collarId] = [];
                trails[u.collarId].push([u.latitude, u.longitude]);
                if (trails[u.collarId].length > 25) trails[u.collarId].shift();

                if (trails[u.collarId].length > 1) {{
                    if (!trailLines[u.collarId]) {{
                        trailLines[u.collarId] = L.polyline(trails[u.collarId], {{ color: '#FFD700', weight: 2.5, opacity: 0.75 }}).addTo(map);
                    }} else {{
                        trailLines[u.collarId].setLatLngs(trails[u.collarId]);
                    }}
                }}
            }});

            var badge = document.getElementById('live-badge');
            if (badge) badge.textContent = '● LIVE COLLAR FIXES  ' + new Date().toLocaleTimeString();
            return 'ok';
        }};
    </script>
</body>
</html>";
    }
}
