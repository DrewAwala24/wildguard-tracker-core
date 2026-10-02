// ═══════════════════════════════════════════════════════════════
//  KWS WildGuard — Operations Command Frontend Engine
//  Spatial wildlife tracking with Kenya border, park detail pages,
//  live telemetry, geofencing, and patrol dispatch.
// ═══════════════════════════════════════════════════════════════

// ── State ────────────────────────────────────────────────────────
let map, parkDetailMap;
let layerGroupGeofences, layerGroupAnimals, layerGroupPatrols, layerGroupTrail;
let kenyaBorderLayer;

let animalsData   = [];
let geofencesData = [];
let patrolsData   = [];
let parksData     = [];
let incidentsData = [];

let activeSector   = 'ALL';
let activeTab      = 'wildlife';
let searchQuery    = '';
let refreshTimer   = null;
let currentPark    = null;
let parkDetailMapInitialized = false;

// ── Photo map ────────────────────────────────────────────────────
const PHOTO_MAP = {
  'KWS-AMB-ELE01': '/images/mutula_bull.jpg',
  'KWS-AMB-ELE04': '/images/mutula_bull.jpg',
  'KWS-TSV-ELE12': '/images/mutula_bull.jpg',
  'KWS-TSV-LIO03': '/images/satao.jpg',
  'KWS-MAR-LIO07': '/images/kipsing.jpg',
  'KWS-MAR-CHT02': '/images/talek.jpg',
  'KWS-NBI-RHN01': '/images/mukurwe.jpg',
  'KWS-NBI-LIO05': '/images/simba.jpg',
  'KWS-OLP-RHN03': '/images/baraka_rh.jpg',
  'KWS-AMB-GIR02': '/images/kibo.jpg',
  'KWS-NBI-ZEB08': '/images/zuri.jpg',
};
const photo = id => PHOTO_MAP[id] || '/images/baraka.jpg';

// ── Kenya border (simplified from Natural Earth 1:10m) ───────────
// [longitude, latitude] in GeoJSON format
const KENYA_GEOJSON = {
  type: 'Feature',
  properties: { name: 'Kenya' },
  geometry: {
    type: 'Polygon',
    coordinates: [[
      [34.078, -1.026], [34.079, -0.955], [34.094, -0.660],
      [34.143, -0.381], [34.171, -0.004], [34.166,  0.389],
      [34.072,  0.653], [34.047,  1.018], [34.367,  1.149],
      [34.795,  1.225], [35.032,  1.234], [35.306,  1.300],
      [35.482,  1.320], [35.742,  1.333], [36.076,  1.266],
      [36.244,  1.173], [36.640,  1.175], [36.880,  1.120],
      [37.180,  1.130], [37.562,  1.220], [37.913,  1.417],
      [38.079,  1.642], [38.430,  1.800], [38.619,  1.850],
      [39.082,  2.120], [39.347,  2.601], [39.530,  3.272],
      [39.568,  3.620], [39.684,  4.001], [39.866,  4.469],
      [40.076,  4.643], [40.322,  4.549], [40.580,  4.399],
      [40.870,  4.169], [41.139,  3.940], [41.533,  3.718],
      [41.872,  3.472], [41.912,  3.213], [41.870,  2.965],
      [41.538,  2.410], [41.200,  1.872], [41.040,  1.377],
      [40.890,  0.931], [40.720,  0.382], [40.679,  0.000],
      [40.964, -0.519], [41.360, -1.180], [41.562, -1.733],
      [41.663, -2.221], [41.746, -2.731], [41.824, -3.279],
      [41.548, -3.452], [41.213, -3.676], [40.778, -3.870],
      [40.435, -4.018], [40.218, -4.251], [40.019, -4.500],
      [39.716, -4.677], [39.449, -4.729], [39.166, -4.789],
      [38.960, -4.769], [38.713, -4.728], [38.371, -4.678],
      [38.092, -4.601], [37.840, -4.529], [37.558, -4.472],
      [37.219, -4.322], [36.962, -4.147], [36.681, -3.983],
      [36.430, -3.828], [36.181, -3.782], [35.899, -3.729],
      [35.659, -3.634], [35.358, -3.581], [35.022, -3.581],
      [34.727, -3.581], [34.473, -3.444], [34.238, -3.283],
      [34.019, -3.099], [33.959, -2.796], [33.929, -2.498],
      [33.921, -2.182], [33.894, -1.847], [33.912, -1.601],
      [34.050, -1.297], [34.078, -1.026]
    ]]
  }
};

// ── Initialisation ───────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
  initMap();
  setupEventListeners();
  refreshAllData();
  refreshTimer = setInterval(refreshAllData, 7000);
});

// ── Map Setup ────────────────────────────────────────────────────
function initMap() {
  map = L.map('operations-map', {
    center: [-1.4, 37.5],
    zoom: 6,
    zoomControl: false,
    attributionControl: true
  });

  L.control.zoom({ position: 'topright' }).addTo(map);

  const satellite = L.tileLayer(
    'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
    { attribution: 'Esri · Maxar · KWS Sovereign Domain', maxZoom: 18 }
  );
  const night = L.tileLayer(
    'https://{s}.basemaps.cartocdn.com/dark_all/{z}/{y}/{x}{r}.png',
    { attribution: '© CartoDB · Tactical Night Ops', maxZoom: 19 }
  );
  const osm = L.tileLayer(
    'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
    { attribution: '© OpenStreetMap contributors', maxZoom: 19 }
  );

  satellite.addTo(map);
  L.control.layers({ 'Satellite': satellite, 'Night Ops': night, 'Streets': osm }, null,
    { position: 'topright' }).addTo(map);

  layerGroupGeofences = L.layerGroup().addTo(map);
  layerGroupPatrols   = L.layerGroup().addTo(map);
  layerGroupAnimals   = L.layerGroup().addTo(map);
  layerGroupTrail     = L.layerGroup().addTo(map);

  // Kenya national border
  renderKenyaBorder();

  // Ecological landmarks
  addLandmarks();
}

function renderKenyaBorder() {
  kenyaBorderLayer = L.geoJSON(KENYA_GEOJSON, {
    style: {
      color: '#e8b520',
      weight: 2.5,
      opacity: 0.8,
      fillColor: 'transparent',
      fillOpacity: 0,
      dashArray: '10, 6',
      lineCap: 'round',
      lineJoin: 'round'
    }
  }).addTo(map);

  kenyaBorderLayer.bindTooltip('🇰🇪 Republic of Kenya', {
    permanent: false,
    direction: 'top',
    className: 'kenya-tooltip'
  });

  // Attempt to fetch higher-resolution border asynchronously
  fetch('https://raw.githubusercontent.com/johan/world.geo.json/master/countries/KEN.geo.json')
    .then(r => r.ok ? r.json() : null)
    .then(geojson => {
      if (geojson && kenyaBorderLayer) {
        map.removeLayer(kenyaBorderLayer);
        kenyaBorderLayer = L.geoJSON(geojson, {
          style: {
            color: '#e8b520', weight: 2, opacity: 0.75,
            fillColor: 'transparent', fillOpacity: 0,
            dashArray: '8, 5'
          }
        }).addTo(map);
      }
    })
    .catch(() => { /* keep embedded fallback */ });
}

function addLandmarks() {
  const landmarks = [
    { name: 'Enkongo Narok Swamp',      lat: -2.660, lng: 37.240 },
    { name: 'Mara River Crossing',      lat: -1.520, lng: 35.080 },
    { name: 'Aruba Dam',                lat: -3.350, lng: 38.680 },
    { name: 'Mzima Springs',            lat: -2.980, lng: 38.010 },
    { name: 'Galana River',             lat: -2.850, lng: 38.650 },
    { name: 'Nagolomon Dam',            lat: -1.382, lng: 36.825 },
    { name: 'Hippo Pools · Mbagathi',  lat: -1.388, lng: 36.872 },
  ];

  const lmIcon = L.divIcon({
    className: '',
    html: `<div style="
      width:8px;height:8px;border-radius:50%;
      background:#22d3ee;border:1.5px solid #fff;
      box-shadow:0 0 6px rgba(34,211,238,0.5);
    "></div>`,
    iconSize: [8, 8], iconAnchor: [4, 4]
  });

  landmarks.forEach(lm => {
    L.marker([lm.lat, lm.lng], { icon: lmIcon })
      .bindPopup(`<div style="font-weight:700;color:#22d3ee;font-size:12px;">📍 ${lm.name}</div>`)
      .addTo(map);
  });
}

// ── Data Fetching ─────────────────────────────────────────────────
async function refreshAllData() {
  await Promise.allSettled([
    fetchAnimals(), fetchGeofences(), fetchPatrols(),
    fetchParks(), fetchIncidents()
  ]);
  updateKPIs();
  renderCurrentTab();
  renderMapMarkers();
}

async function fetchAnimals() {
  try {
    const r = await fetch('/api/animals');
    if (r.ok) animalsData = await r.json();
  } catch (_) {}
}
async function fetchGeofences() {
  try {
    const r = await fetch('/api/geofences');
    if (r.ok) { geofencesData = await r.json(); renderGeofences(); }
  } catch (_) {}
}
async function fetchPatrols() {
  try {
    const r = await fetch('/api/patrols');
    if (r.ok) patrolsData = await r.json();
  } catch (_) {}
}
async function fetchParks() {
  try {
    const r = await fetch('/api/parks');
    if (r.ok) parksData = await r.json();
  } catch (_) {}
}
async function fetchIncidents() {
  try {
    const r = await fetch('/api/incidents');
    if (r.ok) incidentsData = await r.json();
  } catch (_) {}
}

// ── KPI Bar ───────────────────────────────────────────────────────
function updateKPIs() {
  document.getElementById('kpi-animals').textContent = animalsData.length;
  document.getElementById('kpi-patrols').textContent = patrolsData.length;

  const breaches = animalsData.filter(a => a.isBreaching).length;
  const alertEl = document.getElementById('kpi-alerts');
  alertEl.textContent = breaches;
  alertEl.className = breaches > 0
    ? 'kpi__value kpi__value--alert'
    : 'kpi__value kpi__value--safe';

  const avgBattery = animalsData.length
    ? Math.round(animalsData.reduce((s, a) => s + (a.collarBattery || 90), 0) / animalsData.length)
    : 95;
  document.getElementById('kpi-battery').textContent = `${avgBattery}%`;
}

// ── Geofences ─────────────────────────────────────────────────────
function renderGeofences() {
  layerGroupGeofences.clearLayers();
  geofencesData.forEach(z => {
    if (!z.coordinates?.length) return;
    const colors = {
      PARK:             { c: '#15803d', f: 0.12 },
      NATIONAL_RESERVE: { c: '#15803d', f: 0.10 },
      COMMUNITY_BUFFER: { c: '#d97706', f: 0.18, dash: '8,6' },
      FARMLAND:         { c: '#dc2626', f: 0.22 },
      PRIVATE_FARM:     { c: '#dc2626', f: 0.22 },
    };
    const cfg = colors[z.zoneType] || colors.PARK;
    L.polygon(z.coordinates, {
      color: cfg.c, weight: 2,
      fillColor: cfg.c, fillOpacity: cfg.f,
      dashArray: cfg.dash || null
    })
    .bindPopup(`
      <div style="font-weight:700;color:${cfg.c};font-size:13px;">🛡️ ${z.zoneName}</div>
      <div style="font-size:11px;color:#8fa887;margin:3px 0 8px 0;">Zone Type: <b>${z.zoneType}</b></div>
      <button onclick="openParkDetailByName('${z.zoneName.replace(/'/g, "\\'")}')"
              style="background:#e8b520;color:#070d04;font-size:11px;font-weight:700;border:none;
                     border-radius:5px;padding:6px 12px;cursor:pointer;width:100%;letter-spacing:0.5px;">
        View Park Details &amp; Wildlife &rarr;
      </button>
    `)
    .addTo(layerGroupGeofences);
  });
}

// ── Map Markers ───────────────────────────────────────────────────
function renderMapMarkers() {
  layerGroupAnimals.clearLayers();
  layerGroupPatrols.clearLayers();

  const visible = animalsData.filter(a => sectorMatch(a.parkName, activeSector));

  visible.forEach(a => {
    if (a.latitude == null) return;

    const alert = a.isBreaching;
    const html = alert
      ? `<div class="collar-alert"></div>`
      : `<div class="collar-pulse"></div>`;

    const icon = L.divIcon({ className: '', html, iconSize: [16,16], iconAnchor: [8,8] });
    const m = L.marker([a.latitude, a.longitude], { icon });

    m.bindPopup(buildAnimalPopup(a), { maxWidth: 240 });
    m.addTo(layerGroupAnimals);
  });

  patrolsData.forEach(p => {
    if (p.latitude == null) return;
    const isAir = p.unitType === 'AIRWING';
    const html = isAir
      ? `<div style="font-size:18px;line-height:1;">🛩️</div>`
      : `<div class="patrol-marker"></div>`;

    const icon = L.divIcon({ className: '', html, iconSize: [18,18], iconAnchor: [9,9] });
    L.marker([p.latitude, p.longitude], { icon })
      .bindPopup(`
        <div style="font-weight:700;color:#22d3ee;font-size:13px;">${isAir?'🛩️':'🚙'} ${p.name}</div>
        <div style="font-size:10px;color:#8fa887;">Callsign: <b>${p.callSign}</b></div>
        <div style="font-size:10px;color:#8fa887;">Status: <b style="color:#e8b520;">${p.status}</b></div>
        <div style="font-size:10px;color:#8fa887;">Sector: ${p.sector}</div>
        <div style="margin-top:8px;">
          <button onclick="dispatchPatrolQuick(${p.id})"
            style="background:#22d3ee;color:#070d04;font-size:10px;font-weight:700;border:none;
                   padding:4px 10px;border-radius:6px;cursor:pointer;">Dispatch Unit</button>
        </div>
      `)
      .addTo(layerGroupPatrols);
  });
}

function buildAnimalPopup(a) {
  const photoUrl = photo(a.collarId);
  const battPct  = a.collarBattery || 92;
  const battColor = battPct > 50 ? '#22c55e' : battPct > 20 ? '#f59e0b' : '#ef4444';

  return `
    <div style="display:flex;gap:10px;align-items:flex-start;min-width:210px;">
      <img src="${photoUrl}" style="width:48px;height:48px;border-radius:6px;object-fit:cover;border:1px solid #c9960e;flex-shrink:0;" />
      <div style="flex:1;min-width:0;">
        <div style="font-weight:700;color:#e8b520;font-size:13px;margin-bottom:2px;">${a.name}</div>
        <div style="font-size:10px;color:#8fa887;">${a.species}</div>
        <div style="font-size:10px;color:#c9960e;font-family:monospace;">${a.collarId}</div>
        <div style="font-size:10px;color:#8fa887;margin-top:2px;">📍 ${a.parkName || 'Kenya'}</div>
        <div style="margin-top:5px;">
          ${a.isBreaching
            ? '<span style="color:#ef4444;font-weight:700;font-size:10px;">⚠️ PERIMETER BREACH</span>'
            : '<span style="color:#22c55e;font-weight:700;font-size:10px;">🛡️ WITHIN SANCTUARY</span>'}
        </div>
        <div style="font-size:10px;color:${battColor};margin-top:3px;">🔋 ${battPct}%</div>
      </div>
    </div>
    <div style="margin-top:8px;display:flex;gap:6px;justify-content:flex-end;">
      <button onclick="window.traceTrail('${a.collarId}')"
        style="background:#c9960e;color:#070d04;font-size:10px;font-weight:700;
               border:none;padding:4px 10px;border-radius:6px;cursor:pointer;">GPS Trail</button>
    </div>
  `;
}

// ── GPS Trail ─────────────────────────────────────────────────────
window.traceTrail = async function(collarId) {
  try {
    const r = await fetch(`/api/telemetry/animal/${collarId}/trail`);
    if (!r.ok) return;
    const trail = await r.json();
    layerGroupTrail.clearLayers();
    if (trail?.length > 1) {
      const pts = trail.map(p => [p.latitude, p.longitude]);
      // Gradient trail: draw segments
      for (let i = 0; i < pts.length - 1; i++) {
        const alpha = 0.25 + (i / pts.length) * 0.75;
        L.polyline([pts[i], pts[i+1]], {
          color: `rgba(232,181,32,${alpha})`,
          weight: 3,
          lineCap: 'round'
        }).addTo(layerGroupTrail);
      }
      // End-point marker
      L.circleMarker(pts[pts.length - 1], {
        radius: 5, fillColor: '#e8b520', color: '#fff',
        weight: 2, fillOpacity: 1
      }).addTo(layerGroupTrail);

      map.fitBounds(L.latLngBounds(pts), { padding: [50, 50] });
    }
  } catch (_) {}
};

// ── Tab Rendering ─────────────────────────────────────────────────
function renderCurrentTab() {
  const c = document.getElementById('panel-body');
  c.innerHTML = '';

  if (activeTab === 'wildlife') renderWildlifeTab(c);
  else if (activeTab === 'patrols') renderPatrolsTab(c);
  else if (activeTab === 'alerts')  renderAlertsTab(c);
  else if (activeTab === 'parks')   renderParksTab(c);
}

// Sector-match helper
function sectorMatch(parkName, sector) {
  if (sector === 'ALL') return true;
  return (parkName || '').toLowerCase().includes(sector.toLowerCase());
}

// Search-match helper
function searchMatch(animal, q) {
  if (!q) return true;
  const s = q.toLowerCase();
  return [animal.name, animal.species, animal.collarId, animal.parkName]
    .some(v => (v || '').toLowerCase().includes(s));
}

// ── Wildlife Tab ──────────────────────────────────────────────────
function renderWildlifeTab(c) {
  const filtered = animalsData
    .filter(a => sectorMatch(a.parkName, activeSector) && searchMatch(a, searchQuery));

  if (!filtered.length) {
    c.innerHTML = emptyState('🦁', 'No animals in this sector', 'Try selecting a different sector or clearing your search.');
    return;
  }

  // Sort: breaching first, then alphabetical
  filtered.sort((a, b) => (b.isBreaching ? 1 : 0) - (a.isBreaching ? 1 : 0) || a.name.localeCompare(b.name));

  filtered.forEach(a => {
    const card = document.createElement('div');
    card.className = `wildlife-card${a.isBreaching ? ' is-breaching' : ''}`;

    const battPct = a.collarBattery || 92;
    const battColor = battPct > 50 ? 'var(--emerald-bright)' : battPct > 20 ? '#f59e0b' : 'var(--crimson-bright)';

    card.innerHTML = `
      <img src="${photo(a.collarId)}" class="wildlife-photo" alt="${a.name}" loading="lazy">
      <div class="wildlife-info">
        <div style="display:flex;align-items:baseline;justify-content:space-between;gap:4px;">
          <span class="wildlife-name">${a.name}</span>
          <span class="wildlife-collar">${a.collarId}</span>
        </div>
        <div class="wildlife-species">${a.species} &bull; ${a.parkName || 'Kenya'}</div>
        <div class="wildlife-footer">
          <span class="badge ${a.isBreaching ? 'badge--breach' : 'badge--safe'}">
            ${a.isBreaching ? '⚠️ BREACH' : '🛡️ SAFE'}
          </span>
          <span class="battery-bar" style="color:${battColor}">
            <span class="battery-icon" style="color:${battColor}">
              <span class="battery-fill" style="width:${battPct}%;background:${battColor};"></span>
            </span>
            ${battPct}%
          </span>
        </div>
      </div>
    `;

    card.addEventListener('click', () => {
      if (a.latitude != null) {
        map.flyTo([a.latitude, a.longitude], 13, { duration: 1.4 });
        window.traceTrail(a.collarId);
      }
    });

    c.appendChild(card);
  });
}

// ── Patrols Tab ───────────────────────────────────────────────────
function renderPatrolsTab(c) {
  const filtered = patrolsData.filter(p =>
    sectorMatch(p.sector, activeSector) &&
    (!searchQuery || p.name.toLowerCase().includes(searchQuery.toLowerCase()))
  );

  if (!filtered.length) {
    c.innerHTML = emptyState('🚙', 'No patrol units deployed', 'No active patrols match the current filter.');
    return;
  }

  filtered.forEach(p => {
    const isAir = p.unitType === 'AIRWING';
    const card  = document.createElement('div');
    card.className = 'patrol-card';

    card.innerHTML = `
      <div class="patrol-header">
        <span class="patrol-name">${isAir ? '🛩️' : '🚙'} ${p.name}</span>
        <span class="patrol-callsign">${p.callSign}</span>
      </div>
      <div class="patrol-meta">
        Sector: <b>${p.sector}</b>
        &bull; Status: <b style="color:var(--gold-bright)">${p.status}</b>
      </div>
      <div class="patrol-actions">
        <button class="btn btn--ghost" style="font-size:11px;padding:4px 10px;"
          onclick="focusPatrol(${p.latitude},${p.longitude})">Locate</button>
        <button class="btn btn--primary" style="font-size:11px;padding:4px 10px;"
          onclick="dispatchPatrolQuick(${p.id})">Dispatch</button>
      </div>
    `;
    c.appendChild(card);
  });
}

// ── Alerts Tab ────────────────────────────────────────────────────
function renderAlertsTab(c) {
  const breaching = animalsData.filter(a => a.isBreaching);

  if (!breaching.length && !incidentsData.length) {
    c.innerHTML = emptyState('🛡️', 'All Sectors Secure', 'No active perimeter breaches or wildlife conflicts detected.');
    return;
  }

  breaching.forEach(a => {
    const card = document.createElement('div');
    card.className = 'incident-card';
    card.innerHTML = `
      <div class="incident-title">
        <span>🚨 BREACH — ${a.name}</span>
        <span class="badge badge--breach">CRITICAL</span>
      </div>
      <div class="incident-desc">
        ${a.species} (${a.collarId}) has exited the protected zone boundary in <b>${a.parkName}</b>.
        Community SMS alert recommended.
      </div>
      <div class="incident-actions">
        <button class="btn btn--danger" style="font-size:11px;padding:4px 10px;"
          onclick="broadcastSms('${a.parkName}')">Broadcast SMS</button>
        <button class="btn btn--primary" style="font-size:11px;padding:4px 10px;"
          onclick="map.flyTo([${a.latitude},${a.longitude}],13,{duration:1.2})">Zoom Threat</button>
      </div>
    `;
    c.appendChild(card);
  });
}

// ── Parks Tab ─────────────────────────────────────────────────────
function renderParksTab(c) {
  if (!parksData.length) {
    c.innerHTML = emptyState('🏞️', 'Loading Park Profiles…', 'Fetching park data from the PostGIS database.');
    return;
  }

  const filtered = parksData.filter(p =>
    !searchQuery || p.parkName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  if (!filtered.length) {
    c.innerHTML = emptyState('🏞️', 'No parks found', 'Try clearing your search query.');
    return;
  }

  filtered.forEach(p => {
    const card = document.createElement('div');
    card.className = 'park-card';

    const threatClass = {
      LOW: 'threat-low', MODERATE: 'threat-moderate', HIGH: 'threat-high'
    }[p.threatLevel?.toUpperCase()] || 'threat-moderate';

    // Count animals in this park
    const animalCount = animalsInPark(p).length;
    const patrolCount = patrolsInPark(p).length;

    card.innerHTML = `
      <div class="park-card__name">${p.parkName}</div>
      <div class="park-card__sub">${p.county} County &bull; Est. ${p.establishedYear}</div>
      <div class="park-card__meta">
        <div class="park-card__meta-item"><b>Area:</b> ${p.areaSqKm} km²</div>
        <div class="park-card__meta-item"><b>Collared:</b> ${animalCount} animals</div>
        <div class="park-card__meta-item"><b>HQ:</b> ${p.rangerHq}</div>
        <div class="park-card__meta-item"><b>Patrols:</b> ${patrolCount} units</div>
      </div>
      <span class="threat-chip ${threatClass}">⚠️ ${p.threatLevel} THREAT</span>
    `;

    card.addEventListener('click', () => openParkDetail(p));
    c.appendChild(card);
  });
}

// ── Park helpers ──────────────────────────────────────────────────
function animalsInPark(park) {
  const key = extractParkKey(park.parkName);
  return animalsData.filter(a => extractAnimalParkKey(a.parkName) === key);
}

function patrolsInPark(park) {
  const pn = (park.parkName || '').toLowerCase();
  return patrolsData.filter(p => {
    const s = (p.sector || '').toLowerCase();
    // Match on first significant word of park name
    const words = pn.split(/\s+/).filter(w => w.length > 3);
    return words.some(w => s.includes(w));
  });
}

function extractParkKey(name) {
  const n = (name || '').toLowerCase();
  if (n.includes('amboseli'))  return 'AMB';
  if (n.includes('tsavo east') || (n.includes('tsavo') && !n.includes('west'))) return 'TSV';
  if (n.includes('tsavo west')) return 'TSVW';
  if (n.includes('mara') || n.includes('maasai')) return 'MAR';
  if (n.includes('nairobi'))   return 'NBI';
  if (n.includes('pejeta'))    return 'OLP';
  return name;
}

function extractAnimalParkKey(parkName) {
  const n = (parkName || '').toLowerCase();
  if (n.includes('amboseli'))  return 'AMB';
  if (n.includes('tsavo'))     return 'TSV';
  if (n.includes('mara'))      return 'MAR';
  if (n.includes('nairobi'))   return 'NBI';
  if (n.includes('pejeta') || n.includes('ol p')) return 'OLP';
  return parkName;
}

// ── Park Detail View ──────────────────────────────────────────────
function openParkDetail(park) {
  currentPark = park;

  // Update header
  document.getElementById('pd-code').textContent = deriveParkCode(park.parkName);
  document.getElementById('pd-name').textContent = park.parkName;
  document.getElementById('pd-county').textContent = `${park.county} County, Kenya`;

  // Threat badge
  const threatClass = {
    LOW: 'threat-low', MODERATE: 'threat-moderate', HIGH: 'threat-high'
  }[park.threatLevel?.toUpperCase()] || 'threat-moderate';
  document.getElementById('pd-threat-container').innerHTML =
    `<span class="threat-chip ${threatClass}">⚠️ ${park.threatLevel} THREAT</span>`;

  // Stats
  document.getElementById('pd-area').textContent = park.areaSqKm || '--';
  document.getElementById('pd-established').textContent = park.establishedYear || '--';

  const parkAnimals  = animalsInPark(park);
  const parkPatrols  = patrolsInPark(park);

  document.getElementById('pd-animal-count').textContent = parkAnimals.length;
  document.getElementById('pd-patrol-count').textContent = parkPatrols.length;
  document.getElementById('pd-fence').textContent = park.fenceType || park.boundaryType || 'Patrolled';

  // Brief
  document.getElementById('pd-description').textContent =
    park.description || `${park.parkName} is a ${park.ecosystemType || 'savannah'} ecosystem protected area in ${park.county} County, Kenya. It covers ${park.areaSqKm} km² and is home to diverse wildlife including ${park.keySpecies || 'the Big Five'}.`;

  // Info grid
  document.getElementById('pd-hq').textContent        = park.rangerHq || '--';
  document.getElementById('pd-ecosystem').textContent  = park.ecosystemType || '--';
  document.getElementById('pd-waterholes').textContent = park.keyWaterholes || '--';
  document.getElementById('pd-keyspecies').textContent = park.keySpecies || '--';

  // Badges
  document.getElementById('pd-animal-badge').textContent = parkAnimals.length;
  document.getElementById('pd-patrol-badge').textContent = parkPatrols.length;

  // Render animals roster
  renderParkAnimals(parkAnimals);
  renderParkPatrols(parkPatrols);

  // Switch view
  document.getElementById('view-dashboard').classList.add('pushed');
  document.getElementById('view-park-detail').classList.add('view--active');

  // Init or update park map
  setTimeout(() => initOrUpdateParkMap(park, parkAnimals), 250);
}

window.closeParkDetail = function () {
  document.getElementById('view-dashboard').classList.remove('pushed');
  document.getElementById('view-park-detail').classList.remove('view--active');
  currentPark = null;
};

window.openParkDetailByName = function (zoneName) {
  if (!zoneName) return;
  const p = parksData.find(pk =>
    pk.parkName.toLowerCase().includes(zoneName.toLowerCase()) ||
    zoneName.toLowerCase().includes(pk.parkName.toLowerCase()) ||
    extractParkKey(pk.parkName) === extractParkKey(zoneName)
  );
  if (p) {
    openParkDetail(p);
  }
};

function renderParkAnimals(animals) {
  const container = document.getElementById('pd-animals-list');
  if (!animals.length) {
    container.innerHTML = `<div class="empty-state"><span class="empty-state__icon">🦁</span><span class="empty-state__sub">No collared animals registered in this park.</span></div>`;
    return;
  }

  container.innerHTML = animals.map(a => `
    <div class="pd-animal-row" onclick="focusParkAnimal(${a.latitude},${a.longitude})">
      <img src="${photo(a.collarId)}" class="pd-animal-photo" alt="${a.name}" loading="lazy">
      <div style="flex:1;min-width:0;">
        <div class="pd-animal-name">${a.name}</div>
        <div class="pd-animal-species">${a.species}</div>
        <div class="pd-animal-collar">${a.collarId}</div>
      </div>
      <div style="flex-shrink:0;">
        <span class="badge ${a.isBreaching ? 'badge--breach' : 'badge--safe'}">
          ${a.isBreaching ? '⚠️' : '🛡️'}
        </span>
        <div style="font-size:10px;color:var(--text-tertiary);text-align:right;margin-top:2px;">🔋 ${a.collarBattery || 92}%</div>
      </div>
    </div>
  `).join('');
}

function renderParkPatrols(patrols) {
  const container = document.getElementById('pd-patrols-list');
  if (!patrols.length) {
    container.innerHTML = `<div class="empty-state"><span class="empty-state__icon">🚙</span><span class="empty-state__sub">No patrol units assigned to this sector.</span></div>`;
    return;
  }

  container.innerHTML = patrols.map(p => `
    <div class="pd-patrol-row">
      <div>
        <div class="pd-patrol-name">${p.unitType === 'AIRWING' ? '🛩️' : '🚙'} ${p.name}</div>
        <div class="pd-patrol-meta">${p.callSign} &bull; Status: <b style="color:var(--gold-bright)">${p.status}</b></div>
      </div>
      <span class="badge badge--active">${p.status}</span>
    </div>
  `).join('');
}

window.focusParkAnimal = function(lat, lng) {
  if (lat != null && parkDetailMap) {
    parkDetailMap.flyTo([lat, lng], 13, { duration: 1.2 });
  }
};

// ── Park Detail Map ───────────────────────────────────────────────
function initOrUpdateParkMap(park, animals) {
  const center = [park.centerLat || -1.4, park.centerLng || 37.5];
  const zoom   = park.defaultZoom || 10;

  if (!parkDetailMapInitialized) {
    parkDetailMap = L.map('park-detail-map', {
      center, zoom,
      zoomControl: false,
      attributionControl: false
    });

    L.control.zoom({ position: 'topright' }).addTo(parkDetailMap);

    L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { attribution: '', maxZoom: 18 }
    ).addTo(parkDetailMap);

    parkDetailMapInitialized = true;
  } else {
    parkDetailMap.flyTo(center, zoom, { duration: 1.5 });
  }

  // Remove existing animal layers from park map
  if (parkDetailMap._parkAnimalLayer) {
    parkDetailMap.removeLayer(parkDetailMap._parkAnimalLayer);
  }

  const animalLayerGroup = L.layerGroup().addTo(parkDetailMap);
  parkDetailMap._parkAnimalLayer = animalLayerGroup;

  // Render geofences relevant to this park
  geofencesData.forEach(z => {
    if (!z.coordinates?.length) return;
    const zoneParkName = (z.zoneName || '').toLowerCase();
    const parkKey = extractParkKey(park.parkName).toLowerCase();
    if (!zoneParkName.includes(parkKey) && !isParkRelatedZone(z, park)) return;

    L.polygon(z.coordinates, {
      color: '#15803d', weight: 2, fillColor: '#15803d', fillOpacity: 0.12
    }).bindPopup(`🛡️ ${z.zoneName}`).addTo(animalLayerGroup);
  });

  // Render animals in this park on the park map
  animals.forEach(a => {
    if (a.latitude == null) return;
    const icon = L.divIcon({
      className: '',
      html: `<div class="${a.isBreaching ? 'collar-alert' : 'collar-pulse'}"></div>`,
      iconSize: [16,16], iconAnchor: [8,8]
    });
    L.marker([a.latitude, a.longitude], { icon })
      .bindPopup(buildAnimalPopup(a))
      .addTo(animalLayerGroup);
  });

  // Fit to animals if present
  if (animals.length > 0) {
    const bounds = animals
      .filter(a => a.latitude != null)
      .map(a => [a.latitude, a.longitude]);
    if (bounds.length) {
      setTimeout(() => {
        parkDetailMap.fitBounds(L.latLngBounds(bounds), { padding: [60, 60] });
      }, 300);
    }
  }
}

function isParkRelatedZone(zone, park) {
  const zn = (zone.zoneName || '').toLowerCase();
  const pk = (park.parkName || '').toLowerCase();
  const words = pk.split(/\s+/).filter(w => w.length > 4);
  return words.some(w => zn.includes(w));
}

// ── Sector & Tab Events ───────────────────────────────────────────
function setupEventListeners() {
  // Panel tabs
  document.querySelectorAll('.tab').forEach(t => {
    t.addEventListener('click', e => {
      document.querySelectorAll('.tab').forEach(x => {
        x.classList.remove('tab--active');
        x.setAttribute('aria-selected', 'false');
      });
      e.target.classList.add('tab--active');
      e.target.setAttribute('aria-selected', 'true');
      activeTab = e.target.dataset.tab;
      renderCurrentTab();
    });
  });

  // Sector chips
  document.querySelectorAll('.chip').forEach(ch => {
    ch.addEventListener('click', e => {
      document.querySelectorAll('.chip').forEach(x => x.classList.remove('chip--active'));
      e.target.classList.add('chip--active');
      activeSector = e.target.dataset.sector;
      renderCurrentTab();
      renderMapMarkers();

      // Pan map to sector
      const match = parksData.find(p => sectorMatch(p.parkName, activeSector));
      if (match?.centerLat) {
        map.flyTo([match.centerLat, match.centerLng], match.defaultZoom || 10, { duration: 1.3 });
      } else if (activeSector === 'ALL') {
        map.flyTo([-1.4, 37.5], 6, { duration: 1.2 });
      }
    });
  });

  // Search
  document.getElementById('panel-search').addEventListener('input', e => {
    searchQuery = e.target.value.trim();
    renderCurrentTab();
  });

  // Simulate step
  document.getElementById('btn-simulate-step').addEventListener('click', async () => {
    try {
      const r = await fetch('/api/telemetry/simulate', { method: 'POST' });
      if (r.ok) await refreshAllData();
    } catch (_) {}
  });

  // Register collar modal
  const modal = document.getElementById('register-modal');
  document.getElementById('btn-open-register').addEventListener('click', () =>
    modal.classList.add('open'));
  document.getElementById('btn-close-modal').addEventListener('click', () =>
    modal.classList.remove('open'));
  document.getElementById('btn-cancel-modal').addEventListener('click', () =>
    modal.classList.remove('open'));

  modal.addEventListener('click', e => {
    if (e.target === modal) modal.classList.remove('open');
  });

  document.getElementById('form-register').addEventListener('submit', async e => {
    e.preventDefault();
    const payload = {
      name:     document.getElementById('inp-name').value,
      species:  document.getElementById('inp-species').value,
      collarId: document.getElementById('inp-collar').value,
      parkName: document.getElementById('inp-park').value,
      sex:      document.getElementById('inp-sex').value,
    };
    try {
      const r = await fetch('/api/animals', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      });
      if (r.ok) {
        modal.classList.remove('open');
        document.getElementById('form-register').reset();
        await refreshAllData();
      }
    } catch (_) {}
  });
}

// ── Utility ───────────────────────────────────────────────────────
function emptyState(icon, title, sub) {
  return `
    <div class="empty-state">
      <span class="empty-state__icon">${icon}</span>
      <span class="empty-state__title">${title}</span>
      <span class="empty-state__sub">${sub}</span>
    </div>
  `;
}

function deriveParkCode(name) {
  const map = {
    amboseli: 'AMB', tsavo: 'TSV', mara: 'MAR', maasai: 'MAR',
    nairobi: 'NBI', pejeta: 'OLP'
  };
  const n = (name || '').toLowerCase();
  return Object.entries(map).find(([k]) => n.includes(k))?.[1] || 'KWS';
}

// ── Global helpers (called from inline HTML) ──────────────────────
window.focusPatrol = function(lat, lng) {
  if (lat && lng) map.flyTo([lat, lng], 13, { duration: 1.2 });
};

window.dispatchPatrolQuick = async function(id) {
  try {
    const r = await fetch(`/api/patrols/${id}/dispatch`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ latitude: -2.68, longitude: 37.36 }),
    });
    if (r.ok) {
      await fetchPatrols();
      renderCurrentTab();
      renderMapMarkers();
    }
  } catch (_) {}
};

window.broadcastSms = async function(corridor) {
  try {
    await fetch('/api/incidents/broadcast', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        corridor: corridor || 'Agricultural Buffer',
        message: 'KWS Alert: Collared wildlife breach detected. Please stay vigilant.',
      }),
    });
  } catch (_) {}
};
