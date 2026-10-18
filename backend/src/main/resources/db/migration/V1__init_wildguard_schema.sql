-- =========================================================================
-- WildGuard Tracker — Flyway Migration V1: Database Schema
-- Kenya Wildlife Service Operations Command
-- Authoritative PostgreSQL 16 + PostGIS Spatial Schema
-- =========================================================================

CREATE EXTENSION IF NOT EXISTS postgis;

-- ── 1. Users, Roles & Permissions ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(40) NOT NULL,
    full_name VARCHAR(120),
    contact_email VARCHAR(120),
    active BOOLEAN DEFAULT TRUE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    last_login_at TIMESTAMP
);

ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_email VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

-- ── 2. Species Registry ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS species (
    id BIGSERIAL PRIMARY KEY,
    common_name VARCHAR(100) NOT NULL UNIQUE,
    scientific_name VARCHAR(120) NOT NULL,
    iucn_status VARCHAR(10) NOT NULL, -- CR, EN, VU, NT, LC
    typical_speed_kmh DOUBLE PRECISION DEFAULT 4.0 NOT NULL,
    habitat VARCHAR(150),
    diet_type VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── 3. Park Profiles & Sector Dossiers ────────────────────────────────────
CREATE TABLE IF NOT EXISTS park_profiles (
    id BIGSERIAL PRIMARY KEY,
    park_name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(30) NOT NULL,
    county VARCHAR(120) NOT NULL,
    area_sq_km DOUBLE PRECISION NOT NULL,
    ecosystem_type VARCHAR(150) NOT NULL,
    established_year INT NOT NULL,
    ranger_hq VARCHAR(120) NOT NULL,
    fence_type VARCHAR(120) NOT NULL,
    threat_level VARCHAR(50) NOT NULL,
    key_waterholes TEXT NOT NULL,
    key_species TEXT NOT NULL,
    description TEXT NOT NULL,
    center_lat DOUBLE PRECISION NOT NULL,
    center_lng DOUBLE PRECISION NOT NULL,
    default_zoom INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── 4. Vector Geofences ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS geofence_zones (
    id BIGSERIAL PRIMARY KEY,
    zone_name VARCHAR(120) NOT NULL UNIQUE,
    zone_type VARCHAR(40) NOT NULL, -- PARK, COMMUNITY_BUFFER, HIGH_CONFLICT, SANCTUARY, ELECTRIC_FENCE
    park_name VARCHAR(100),
    boundary geometry(Polygon, 4326) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── 5. Ecological Landmarks ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS ecological_landmarks (
    id BIGSERIAL PRIMARY KEY,
    landmark_name VARCHAR(120) NOT NULL,
    landmark_type VARCHAR(50) NOT NULL, -- WATERHOLE, DAM, RIVER_CROSSING, RIDGE, SWAMP, OBS_POST
    park_name VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    location geometry(Point, 4326) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── 6. Animals Registry ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS animals (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    species VARCHAR(100) NOT NULL,
    collar_id VARCHAR(50) NOT NULL UNIQUE,
    park_name VARCHAR(100) NOT NULL,
    sex VARCHAR(20) NOT NULL,
    iucn_status VARCHAR(10) NOT NULL,
    active BOOLEAN DEFAULT TRUE NOT NULL,
    notes TEXT,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Alter table if columns missing
ALTER TABLE animals ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE NOT NULL;
ALTER TABLE animals ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE animals ADD COLUMN IF NOT EXISTS registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- ── 7. GPS Telemetry Observations ─────────────────────────────────────────
CREATE TABLE IF NOT EXISTS telemetry_locations (
    id BIGSERIAL PRIMARY KEY,
    animal_id BIGINT NOT NULL REFERENCES animals(id) ON DELETE CASCADE,
    location geometry(Point, 4326) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    speed_kmh DOUBLE PRECISION DEFAULT 0.0,
    heading DOUBLE PRECISION DEFAULT 0.0,
    battery_level DOUBLE PRECISION DEFAULT 100.0,
    activity_state VARCHAR(60) DEFAULT 'ACTIVE',
    data_source VARCHAR(30) DEFAULT 'SIMULATED', -- LIVE, SIMULATED
    timestamp TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS speed_kmh DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS heading DOUBLE PRECISION DEFAULT 0.0;
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS battery_level DOUBLE PRECISION DEFAULT 100.0;
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS activity_state VARCHAR(60) DEFAULT 'ACTIVE';
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS data_source VARCHAR(30) DEFAULT 'SIMULATED';
ALTER TABLE telemetry_locations ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- ── 8. Patrol Units ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS patrol_units (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    call_sign VARCHAR(50) NOT NULL UNIQUE,
    unit_type VARCHAR(40) NOT NULL, -- LAND_CRUISER, AIRWING, DRONE, FOOT_PATROL
    status VARCHAR(40) NOT NULL,    -- AVAILABLE, DISPATCHED, ON_PATROL, MAINTENANCE
    sector VARCHAR(100) NOT NULL,
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    current_location geometry(Point, 4326),
    last_status_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE patrol_units ADD COLUMN IF NOT EXISTS current_location geometry(Point, 4326);
ALTER TABLE patrol_units ADD COLUMN IF NOT EXISTS last_status_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;

-- ── 9. Human-Wildlife Conflict Incidents & Geofence Alerts ─────────────────
CREATE TABLE IF NOT EXISTS incident_logs (
    id BIGSERIAL PRIMARY KEY,
    incident_code VARCHAR(50),
    animal_name VARCHAR(100) NOT NULL,
    collar_id VARCHAR(50) NOT NULL,
    zone_name VARCHAR(120),
    park_name VARCHAR(100),
    severity VARCHAR(30) NOT NULL, -- CRITICAL, WARNING, INFO
    status VARCHAR(30) NOT NULL,   -- ACTIVE, ACKNOWLEDGED, DISPATCHED, RESOLVED
    message TEXT NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    location geometry(Point, 4326),
    resolution_notes TEXT,
    dispatched_unit VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    acknowledged_at TIMESTAMP,
    resolved_at TIMESTAMP
);

ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS incident_code VARCHAR(50);
ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS park_name VARCHAR(100);
ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS latitude DOUBLE PRECISION;
ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;
ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS location geometry(Point, 4326);
ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS acknowledged_at TIMESTAMP;
ALTER TABLE incident_logs ADD COLUMN IF NOT EXISTS resolved_at TIMESTAMP;

-- ── 10. Patrol Dispatches ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS patrol_dispatches (
    id BIGSERIAL PRIMARY KEY,
    dispatch_code VARCHAR(50) NOT NULL UNIQUE,
    incident_id BIGINT REFERENCES incident_logs(id) ON DELETE SET NULL,
    patrol_unit_id BIGINT NOT NULL REFERENCES patrol_units(id) ON DELETE CASCADE,
    call_sign VARCHAR(50) NOT NULL,
    target_park VARCHAR(100) NOT NULL,
    target_latitude DOUBLE PRECISION NOT NULL,
    target_longitude DOUBLE PRECISION NOT NULL,
    status VARCHAR(40) NOT NULL, -- DISPATCHED, EN_ROUTE, ON_SCENE, COMPLETED, CANCELLED
    commander_username VARCHAR(60) NOT NULL,
    dispatch_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    completion_time TIMESTAMP,
    notes TEXT
);

-- ── 11. Weather & Environmental Observations ──────────────────────────────
CREATE TABLE IF NOT EXISTS weather_observations (
    id BIGSERIAL PRIMARY KEY,
    park_name VARCHAR(100) NOT NULL,
    temperature_c DOUBLE PRECISION NOT NULL,
    conditions VARCHAR(80) NOT NULL,
    humidity_percent INT NOT NULL,
    wind_speed_kmh DOUBLE PRECISION NOT NULL,
    observation_source VARCHAR(60) NOT NULL, -- CACHED_METEO, FALLBACK, SENSOR
    observation_time TIMESTAMP NOT NULL,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── 12. System Audit Logs ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(60) NOT NULL,
    action_type VARCHAR(60) NOT NULL, -- USER_LOGIN, ANIMAL_REGISTERED, ANIMAL_UPDATED, PATROL_DISPATCHED, INCIDENT_RESOLVED, etc.
    entity_type VARCHAR(60),
    entity_id VARCHAR(60),
    description TEXT NOT NULL,
    ip_address VARCHAR(50) DEFAULT '127.0.0.1',
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- ── 13. Spatial and B-Tree Indexes ─────────────────────────────────────────
CREATE INDEX IF NOT EXISTS idx_geofence_boundary ON geofence_zones USING GIST (boundary);
CREATE INDEX IF NOT EXISTS idx_telemetry_location ON telemetry_locations USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_landmarks_location ON ecological_landmarks USING GIST (location);
CREATE INDEX IF NOT EXISTS idx_patrol_location ON patrol_units USING GIST (current_location);
CREATE INDEX IF NOT EXISTS idx_incident_location ON incident_logs USING GIST (location);

CREATE INDEX IF NOT EXISTS idx_telemetry_animal_time ON telemetry_locations (animal_id, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_telemetry_time ON telemetry_locations (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_incident_status_time ON incident_logs (status, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_animals_collar ON animals (collar_id);
CREATE INDEX IF NOT EXISTS idx_animals_park ON animals (park_name);
CREATE INDEX IF NOT EXISTS idx_patrol_status ON patrol_units (status);
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs (timestamp DESC);
