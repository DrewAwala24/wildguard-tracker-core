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
        .leaflet-control-layers {{ background: rgba(27,38,16,0.92); color: #fff; border: 1.5px solid #FFD700; border-radius: 8px; font-size: 11px; font-weight: 600; padding: 6px 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.4); }}
        .leaflet-control-layers-separator {{ border-top: 1px solid rgba(255,215,0,0.3); }}
        .popup-title {{ font-size: 13px; font-weight: bold; color: #FFD700; margin-bottom: 3px; display: flex; align-items: center; gap: 4px; }}
        .popup-species {{ font-size: 10px; color: #7CFF6B; font-weight: 600; text-transform: uppercase; margin-bottom: 4px; }}
        .popup-desc {{ font-size: 11px; color: #e2e8f0; line-height: 1.45; }}
        .popup-chip {{ display: inline-block; background: rgba(255,215,0,0.15); border: 1px solid #FFD700; color: #FFD700; padding: 1px 6px; border-radius: 4px; font-size: 10px; margin-top: 4px; }}
        .pulse-marker {{ width: 14px; height: 14px; background: #FFD700; border: 2px solid #fff; border-radius: 50%; box-shadow: 0 0 10px #FFD700; }}
        .alert-marker {{ width: 16px; height: 16px; background: #FF5252; border: 2px solid #fff; border-radius: 50%; animation: pulse 1.2s infinite; }}
        .warning-marker {{ width: 14px; height: 14px; background: #FF9800; border: 2px solid #fff; border-radius: 50%; }}
        .patrol-marker {{ width: 16px; height: 16px; background: #00E5FF; border: 2px solid #fff; border-radius: 3px; transform: rotate(45deg); }}
        .patrol-dispatched-marker {{ width: 18px; height: 18px; background: #FFD700; border: 2px solid #FF5252; border-radius: 3px; transform: rotate(45deg); animation: pulse 1s infinite; }}
        .airwing-marker {{ font-size: 19px; filter: drop-shadow(0 0 6px #00E5FF); animation: airsweep 2.5s infinite ease-in-out; text-align: center; line-height: 22px; }}
        .landmark-marker {{ width: 8px; height: 8px; background: #00E5FF; border: 1.5px solid #fff; border-radius: 50%; opacity: 0.85; }}
        @keyframes pulse {{ 0% {{ box-shadow: 0 0 0 0 rgba(255,82,82,.7); }} 70% {{ box-shadow: 0 0 0 10px rgba(255,82,82,0); }} 100% {{ box-shadow: 0 0 0 0 rgba(255,82,82,0); }} }}
        @keyframes airsweep {{ 0% {{ transform: translateY(0px) rotate(-15deg); }} 50% {{ transform: translateY(-4px) rotate(15deg); }} 100% {{ transform: translateY(0px) rotate(-15deg); }} }}
        #live-badge {{ position: absolute; z-index: 1000; left: 10px; bottom: 12px; background: rgba(16,24,8,.92); color: #7CFF6B; font-size: 11px; font-weight: 700; padding: 6px 10px; border-radius: 6px; border: 1px solid #7CFF6B; box-shadow: 0 2px 8px rgba(0,0,0,0.4); }}
        #fence-badge {{ position: absolute; z-index: 1000; right: 10px; bottom: 12px; background: rgba(26,18,8,.92); color: #FFD700; font-size: 10px; font-weight: 600; padding: 5px 9px; border-radius: 6px; border: 1px solid #FF9800; }}
        #kenya-badge {{ position: absolute; z-index: 1000; left: 50%; transform: translateX(-50%); top: 12px; background: rgba(16,24,8,.92); color: #FFD700; font-size: 11px; font-weight: 700; letter-spacing: 1.5px; padding: 6px 16px; border-radius: 20px; border: 1.5px solid rgba(255,215,0,0.6); box-shadow: 0 4px 14px rgba(0,0,0,0.6); pointer-events: none; white-space: nowrap; }}
        .country-label {{ font-size: 11px; font-weight: 800; letter-spacing: 3px; color: rgba(255,255,255,0.45); text-shadow: 0 1px 4px rgba(0,0,0,0.9); text-align: center; pointer-events: none; }}
    </style>
</head>
<body>
    <div id='map'></div>
    <div id='kenya-badge'>🇰🇪 REPUBLIC OF KENYA • KWS SOVEREIGN DOMAIN</div>
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
        var nightLayer = L.tileLayer('https://{{s}}.basemaps.cartocdn.com/dark_all/{{z}}/{{y}}/{{x}}{{r}}.png', {{
            attribution: 'CartoDB Dark Matter • KWS Tactical Night Ops',
            maxZoom: 19
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

        var geofences = {geofencesJson};
        var animals = {animalsJson};
        var patrols = {patrolsJson};
        var incidents = {incidentsJson};
        var trailCoords = {trailJson};
        var animalMarkers = {{}};
        var patrolMarkers = {{}};
        var trails = {{}};
        var trailLines = {{}};
        var predictionVectors = {{}};
        var predictionBeacons = {{}};

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

        // 🇰🇪 REPUBLIC OF KENYA NATIONAL SOVEREIGN BORDER
        var kenyaBorderCoordinates = [[5.4306, 35.2918], [5.2767, 35.1946], [4.2225, 33.9928], [4.0687, 34.0931], [4.0094, 34.062], [3.9616, 34.1344], [3.8616, 34.0885], [3.8815, 34.2156], [3.7677, 34.1718], [3.7843, 34.2455], [3.6861, 34.3026], [3.7351, 34.3699], [3.6696, 34.463], [3.5188, 34.451], [3.4919, 34.3888], [3.1828, 34.4554], [3.0992, 34.5755], [2.9261, 34.5991], [2.8226, 34.7666], [2.6978, 34.7749], [2.4725, 34.9527], [2.3979, 34.8791], [1.9242, 35.0258], [1.6652, 34.9878], [1.4163, 34.7886], [1.248, 34.8184], [1.2077, 34.6707], [1.1001, 34.5775], [1.1216, 34.5277], [0.8636, 34.4455], [0.764, 34.3132], [0.6423, 34.2756], [0.5858, 34.1376], [0.3499, 34.1003], [0.1114, 33.9139], [-0.1294, 33.9822], [-0.5618, 33.92], [-1.0011, 33.9351], [-1.0302, 34.0937], [-1.389, 34.7306], [-1.5502, 35.0166], [-2.1044, 36.0002], [-2.5264, 36.7514], [-2.8408, 37.3125], [-2.9561, 37.5203], [-2.987, 37.5677], [-3.0587, 37.6714], [-3.308, 37.7112], [-3.4384, 37.5848], [-3.5182, 37.6045], [-3.5426, 37.7441], [-3.6734, 37.7838], [-4.6672, 39.1939], [-4.5999, 39.2689], [-4.5668, 39.2439], [-4.5519, 39.3131], [-4.6315, 39.3099], [-4.6435, 39.4017], [-4.5397, 39.3755], [-4.5303, 39.4623], [-4.3992, 39.5079], [-4.4359, 39.5375], [-4.0907, 39.6735], [-4.049, 39.6369], [-4.1195, 39.5767], [-4.0146, 39.5635], [-4.0408, 39.6385], [-3.9841, 39.5967], [-3.9446, 39.6591], [-4.0588, 39.6987], [-3.9578, 39.7639], [-3.9499, 39.6865], [-3.9066, 39.6909], [-3.9495, 39.7643], [-3.818, 39.8295], [-3.6505, 39.8669], [-3.5884, 39.7713], [-3.628, 39.8697], [-3.4008, 39.9683], [-3.3473, 39.9375], [-3.312, 39.9959], [-3.3861, 39.9743], [-3.2867, 40.1195], [-3.147, 40.1247], [-2.9897, 40.2397], [-3.0338, 40.1415], [-2.6812, 40.1821], [-2.7326, 40.1964], [-2.6131, 40.2905], [-2.5277, 40.5161], [-2.5316, 40.4441], [-2.4909, 40.4892], [-2.5574, 40.6069], [-2.3892, 40.8223], [-2.3399, 40.7947], [-2.3952, 40.7591], [-2.2047, 40.6955], [-2.2604, 40.7809], [-2.1864, 40.8823], [-2.221, 40.9259], [-2.0901, 40.9125], [-2.0698, 40.8505], [-2.0106, 40.9031], [-2.0176, 40.8287], [-2.0005, 40.8721], [-1.9338, 40.7737], [-1.9503, 40.8735], [-2.0763, 40.9457], [-1.9428, 40.9615], [-1.8656, 41.0405], [-1.9464, 40.9831], [-2.0473, 41.0115], [-1.8869, 41.2255], [-1.9817, 41.2773], [-1.6584, 41.5621], [-0.8291, 40.9924], [2.8248, 40.9925], [3.1537, 41.3283], [3.9764, 41.9063], [3.943, 41.1723], [4.283, 40.7618], [3.8675, 39.8677], [3.6581, 39.7681], [3.4661, 39.553], [3.48, 39.1931], [3.5413, 39.0884], [3.5059, 38.9043], [3.6492, 38.5343], [3.5976, 38.4476], [3.6008, 38.1282], [4.3857, 37.0322], [4.45, 36.8426], [4.4562, 36.0506], [4.7824, 35.8121], [5.3201, 35.8632], [5.4229, 35.5598], [5.4306, 35.2918]];

        var kenyaBorderLayer = L.layerGroup();
        L.polygon(kenyaBorderCoordinates, {{
            color: '#1B5E20',
            weight: 3.5,
            opacity: 0.95,
            dashArray: '8, 6',
            fill: true,
            fillColor: '#2E7D32',
            fillOpacity: 0.035
        }}).addTo(kenyaBorderLayer).bindTooltip('🇰🇪 <b>Republic of Kenya</b><br><span style=""color:#7CFF6B;font-size:10px;"">KWS Sovereign Wildlife Conservation Territory</span>', {{ sticky: true }});

        L.polyline(kenyaBorderCoordinates, {{
            color: '#FFD700',
            weight: 1.5,
            opacity: 0.85,
            dashArray: '4, 10'
        }}).addTo(kenyaBorderLayer);

        var neighborLabels = [
            {{ name: 'UGANDA', lat: 1.80, lng: 33.10 }},
            {{ name: 'TANZANIA', lat: -3.80, lng: 36.00 }},
            {{ name: 'ETHIOPIA', lat: 5.60, lng: 38.20 }},
            {{ name: 'SOUTH SUDAN', lat: 5.20, lng: 33.60 }},
            {{ name: 'SOMALIA', lat: 2.20, lng: 42.40 }},
            {{ name: 'INDIAN OCEAN', lat: -3.90, lng: 41.20 }}
        ];
        neighborLabels.forEach(function(nl) {{
            L.marker([nl.lat, nl.lng], {{
                icon: L.divIcon({{
                    className: 'custom-country-label',
                    html: '<div class=""country-label"">' + nl.name + '</div>',
                    iconSize: [120, 20],
                    iconAnchor: [60, 10]
                }}),
                interactive: false
            }}).addTo(kenyaBorderLayer);
        }});
        kenyaBorderLayer.addTo(map);

        // 🎯 Poaching Risk Heatmap Layer
        var poachingRiskLayer = L.layerGroup();
        var threatHotspots = [
            {{ name: 'Tsavo East: Voi Rail Corridor', lat: -3.3900, lng: 38.5600, radius: 9500, threat: 'CRITICAL', desc: 'SGR / Highway Snare Intercept Buffer' }},
            {{ name: 'Tsavo East: Aruba Waterhole Perimeter', lat: -3.1150, lng: 38.8250, radius: 8000, threat: 'HIGH', desc: 'Dry Season Waterhole Ambush Zone' }},
            {{ name: 'Amboseli: Kimana Agricultural Buffer', lat: -2.7100, lng: 37.3600, radius: 7500, threat: 'CRITICAL', desc: 'Nocturnal Elephant Crop-Raiding Hotspot' }},
            {{ name: 'Maasai Mara: Talek & Siana Border', lat: -1.4800, lng: 35.2500, radius: 7000, threat: 'ELEVATED', desc: 'Livestock Predator Retaliation Buffer' }},
            {{ name: 'Nairobi NP: Kitengela Dispersal', lat: -1.4200, lng: 36.8800, radius: 5500, threat: 'ELEVATED', desc: 'Unfenced Southern Periphery Crossing' }}
        ];

        threatHotspots.forEach(function(th) {{
            var color = th.threat === 'CRITICAL' ? '#FF1744' : (th.threat === 'HIGH' ? '#FF5252' : '#FF9100');
            L.circle([th.lat, th.lng], {{
                radius: th.radius,
                color: color,
                fillColor: color,
                fillOpacity: 0.15,
                weight: 1.8,
                dashArray: '4, 6'
            }}).addTo(poachingRiskLayer).bindTooltip('🎯 <b>' + th.name + '</b><br><span style=""color:' + color + ';font-weight:bold;"">Threat: ' + th.threat + '</span><br>' + th.desc, {{ sticky: true }});
        }});
        poachingRiskLayer.addTo(map);

        // 🔮 AI Predictive Movement Corridors Layer
        var predictiveLayer = L.layerGroup();
        predictiveLayer.addTo(map);

        // Draw Park Geofences
        var parkGeofencesLayer = L.layerGroup();
        geofences.forEach(function(zone) {{
            if (zone.coordinates && zone.coordinates.length > 0) {{
                L.polygon(zone.coordinates, {{
                    color: zone.colorHex || '#4CAF50',
                    weight: zone.zoneType === 'COMMUNITY_BUFFER' ? 2 : 3,
                    dashArray: zone.zoneType === 'COMMUNITY_BUFFER' ? '5, 5' : null,
                    fillOpacity: zone.zoneType === 'COMMUNITY_BUFFER' ? 0.12 : 0.20
                }}).addTo(parkGeofencesLayer).bindTooltip('<b>' + zone.zoneName + '</b>', {{ sticky: true }});
            }}
        }});
        parkGeofencesLayer.addTo(map);

        // Draw Nairobi NP Northern Perimeter Fence (Separates park from Nairobi city)
        var nairobiFenceLayer = L.layerGroup();
        var nairobiCityFence = [
            [-1.3500, 36.7760],
            [-1.3465, 36.7950],
            [-1.3420, 36.8200],
            [-1.3425, 36.8450],
            [-1.3440, 36.8720],
            [-1.3520, 36.8980],
            [-1.3620, 36.9250]
        ];
        L.polyline(nairobiCityFence, {{
            color: '#FFD700',
            weight: 3.5,
            dashArray: '6, 6',
            opacity: 0.95
        }}).addTo(nairobiFenceLayer).bindTooltip('⚡ <b>Nairobi NP Northern Perimeter Electric Fence</b><br>Separates Wildlife Sanctuary from Nairobi City', {{ sticky: true }});
        nairobiFenceLayer.addTo(map);

        // Draw subtle ecological landmarks
        var landmarksLayer = L.layerGroup();
        parkLandmarks.forEach(function(lm) {{
            L.marker([lm.lat, lm.lng], {{
                icon: L.divIcon({{ className: 'custom-lm', html: '<div class=""landmark-marker""></div>', iconSize: [8, 8], iconAnchor: [4, 4] }})
            }}).addTo(landmarksLayer).bindTooltip('💧 <b>' + lm.name + '</b> (' + lm.park + ')', {{ direction: 'top', opacity: 0.85 }});
        }});
        landmarksLayer.addTo(map);

        // Layer Switcher with Base Layers + Toggleable GIS Overlays
        var overlayLayers = {{
            '🇰🇪 Kenya Sovereign Border': kenyaBorderLayer,
            '🔮 AI Predictive Corridors': predictiveLayer,
            '🎯 Poaching Risk Heatmap': poachingRiskLayer,
            '🏞️ National Park Geofences': parkGeofencesLayer,
            '⚡ Nairobi City Fence': nairobiFenceLayer,
            '💧 Ecological Landmarks': landmarksLayer
        }};
        L.control.layers({{ 'Satellite': satellite, 'Tactical Night': nightLayer, 'Streets': streets, 'Terrain': topo }}, overlayLayers, {{ position: 'topleft', collapsed: true }}).addTo(map);

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
            var predText = animal.predictedCorridor || 'Corridor stable';
            var solarText = animal.solarVoltageDisplay || '☀️ 14.1V (Solar Active)';
            var tempText = animal.collarTempDisplay || '🌡️ 27°C';
            var sigText = animal.signalStrength || '🛰️ -82 dBm';

            return '<div class=""popup-title"">' + animal.name + '</div>' +
                   '<div class=""popup-species"">' + animal.species + ' • ' + animal.parkName + '</div>' +
                   '<div class=""popup-desc"">' +
                   '<b>Collar:</b> ' + animal.collarId + ' (' + animal.collarBattery + '% battery)<br>' +
                   '<b>Speed:</b> ' + speedText + ' (' + headingText + ')<br>' +
                   '<b>Activity:</b> ' + behavior + '<br>' +
                   '<b>Predicted Corridor:</b> ' + predText + '<br>' +
                   '<b>Hardware Health:</b> ' + solarText + ' • ' + tempText + ' • ' + sigText + '<br>' +
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
        if (zoomLevel > 6 && markerList.length > 0) {{
            map.fitBounds(L.featureGroup(markerList).getBounds().pad(0.35));
        }} else if (kenyaBorderCoordinates && kenyaBorderCoordinates.length > 0) {{
            map.fitBounds(L.latLngBounds(kenyaBorderCoordinates).pad(0.04));
        }}

        // Render Patrol Units (Ground Units & Airwing Recon)
        patrols.forEach(function(patrol) {{
            var isDispatched = patrol.status === 'DISPATCHED';
            var isAirwing = patrol.unitType === 'AIRWING';
            var pinHtml = isAirwing ?
                '<div class=""airwing-marker"">✈️</div>' :
                ('<div class=""' + (isDispatched ? 'patrol-dispatched-marker' : 'patrol-marker') + '""></div>');
            var pinSize = isAirwing ? [24, 24] : [18, 18];
            var pinAnchor = isAirwing ? [12, 12] : [9, 9];

            var altText = isAirwing ? ('<br><b>Altitude:</b> ' + (patrol.altitudeFt || 2450) + ' ft MSL<br><b>Airspeed:</b> ' + (patrol.speedKmh || 165) + ' km/h') : '';
            var marker = L.marker([patrol.latitude, patrol.longitude], {{
                icon: L.divIcon({{ className: isAirwing ? 'custom-airwing-pin' : 'custom-patrol-pin', html: pinHtml, iconSize: pinSize, iconAnchor: pinAnchor }})
            }}).addTo(map).bindPopup('<div class=""popup-title"" style=""color:#00E5FF;"">' + (isAirwing ? '🦅 ' : '🚙 ') + patrol.name + '</div><div class=""popup-desc""><b>Sector:</b> ' + patrol.sector + '<br><b>Status:</b> ' + patrol.status + altText + '</div>');
            patrolMarkers[patrol.callSign || patrol.name] = marker;
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
                var ease = 0.5 - Math.cos(progress * Math.PI) / 2;
                marker.setLatLng([from.lat + (toLat - from.lat) * ease, from.lng + (toLng - from.lng) * ease]);
                if (progress < 1) requestAnimationFrame(frame);
            }}
            requestAnimationFrame(frame);
        }}

        // Night Ops Controller Bridge
        window.setTacticalNightOps = function(enable) {{
            if (enable) {{
                map.removeLayer(satellite);
                map.removeLayer(streets);
                map.removeLayer(topo);
                nightLayer.addTo(map);
                var b = document.getElementById('kenya-badge');
                if (b) {{ b.textContent = '🌙 TACTICAL NIGHT OPS • KWS NOCTURNAL SECTOR COMMAND'; b.style.color = '#FF5252'; b.style.borderColor = '#FF5252'; }}
            }} else {{
                map.removeLayer(nightLayer);
                satellite.addTo(map);
                var b = document.getElementById('kenya-badge');
                if (b) {{ b.textContent = '🇰🇪 REPUBLIC OF KENYA • KWS SOVEREIGN DOMAIN'; b.style.color = '#FFD700'; b.style.borderColor = 'rgba(255,215,0,0.6)'; }}
            }}
            return 'ok';
        }};

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
                    if (u.predictedCorridor) animal.predictedCorridor = u.predictedCorridor;
                    if (u.solarVoltageDisplay) animal.solarVoltageDisplay = u.solarVoltageDisplay;
                    if (u.collarTempDisplay) animal.collarTempDisplay = u.collarTempDisplay;
                    if (u.signalStrength) animal.signalStrength = u.signalStrength;
                }}
                if (marker) {{
                    animateMarker(marker, u.latitude, u.longitude);
                    if (animal && marker.getPopup()) {{
                        marker.setPopupContent(buildPopupHtml(animal));
                    }}
                }}

                // Update Breadcrumb Trails
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

                // Update AI Predictive Corridors
                if (u.predictedLat && u.predictedLng) {{
                    var predPoints = [[u.latitude, u.longitude], [u.predictedLat, u.predictedLng]];
                    if (!predictionVectors[u.collarId]) {{
                        predictionVectors[u.collarId] = L.polyline(predPoints, {{ color: '#00E5FF', weight: 2, dashArray: '4, 6', opacity: 0.8 }}).addTo(predictiveLayer);
                        predictionBeacons[u.collarId] = L.circleMarker([u.predictedLat, u.predictedLng], {{ radius: 4, color: '#00E5FF', fillColor: '#00E5FF', fillOpacity: 0.7 }}).addTo(predictiveLayer);
                    }} else {{
                        predictionVectors[u.collarId].setLatLngs(predPoints);
                        predictionBeacons[u.collarId].setLatLng([u.predictedLat, u.predictedLng]);
                    }}
                }}
            }});

            var badge = document.getElementById('live-badge');
            if (badge) badge.textContent = '● LIVE COLLAR FIXES  ' + new Date().toLocaleTimeString();
            return 'ok';
        }};

        // Live Air Wing Flight Bridge
        window.updateAirwingFlight = function(lat, lng, headingDeg, altFt, speedKmh) {{
            var airMarker = patrolMarkers['AIRWING-KWS-09'] || Object.values(patrolMarkers)[3];
            if (airMarker) {{
                animateMarker(airMarker, lat, lng);
            }}
            return 'ok';
        }};
    </script>
</body>
</html>";
    }
}
