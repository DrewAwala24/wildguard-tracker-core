-- =========================================================================
-- Kenya Wildlife Service (KWS) - Park Profiles & Sector Dossiers Schema
-- PostgreSQL + PostGIS SQL Migration Script
-- =========================================================================

-- 1. Create park_profiles table if it does not already exist
CREATE TABLE IF NOT EXISTS park_profiles (
    id BIGSERIAL PRIMARY KEY,
    park_name VARCHAR(100) NOT NULL UNIQUE,
    code VARCHAR(20) NOT NULL,
    county VARCHAR(100) NOT NULL,
    area_sq_km DOUBLE PRECISION NOT NULL,
    ecosystem_type VARCHAR(100) NOT NULL,
    established_year INT NOT NULL,
    ranger_hq VARCHAR(100) NOT NULL,
    fence_type VARCHAR(100) NOT NULL,
    threat_level VARCHAR(50) NOT NULL,
    key_waterholes TEXT NOT NULL,
    key_species TEXT NOT NULL,
    description TEXT NOT NULL,
    center_lat DOUBLE PRECISION NOT NULL,
    center_lng DOUBLE PRECISION NOT NULL,
    default_zoom INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Insert or update default Kenya national parks and conservancies data
INSERT INTO park_profiles (
    park_name, code, county, area_sq_km, ecosystem_type, established_year,
    ranger_hq, fence_type, threat_level, key_waterholes, key_species,
    description, center_lat, center_lng, default_zoom
) VALUES 
(
    'Amboseli National Park',
    'AMBOSELI',
    'Kajiado County',
    392.0,
    'Savannah Plains, Acacia Woodland & Freshwater Swamps',
    1974,
    'Ol Tukai Ranger Command Post',
    'Partially Fenced (Southern Kimana Corridor Buffer)',
    'MODERATE - Human-Wildlife Conflict Buffer',
    'Enkongo Narok Swamp, Ol Okenya Marsh, Lake Amboseli Basin, Observation Hill Waterhole',
    'African Bush Elephant, Lion, Cheetah, Maasai Giraffe, Cape Buffalo, Spotted Hyena',
    'Famous for being the best place in the world to get close to free-ranging elephants against the backdrop of Mount Kilimanjaro. Critical focus is the southern Kimana community corridor to prevent crop-raiding.',
    -2.65,
    37.26,
    11
),
(
    'Tsavo East National Park',
    'TSAVO_EAST',
    'Taita-Taveta, Kitui & Tana River Counties',
    13747.0,
    'Semi-Arid Bushland, Savannah & Riverine Galana Basin',
    1948,
    'Voi Gate Headquarters & Lugard Falls Patrol Post',
    'Open Dispersal (SGR Wildlife Underpasses & Highway Crossings)',
    'HIGH - Vast Area Poaching & SGR Dispersal Risk',
    'Aruba Dam, Galana River Rapids, Mudanda Rock Water catchment, Lugard Falls Pool',
    'Red Dust Elephants, Tsavo Maneless Lions, Black Rhino, Hirola, Lesser Kudu',
    'One of the oldest and largest national parks in Kenya. Renowned for massive herds of dust-red elephants that roll in the volcanic soil, and vital monitoring of wildlife movement across the Standard Gauge Railway.',
    -2.77,
    38.77,
    10
),
(
    'Tsavo West National Park',
    'TSAVO_WEST',
    'Taita-Taveta County',
    9065.0,
    'Rugged Volcanic Ridges, Mzima Springs & Acacia Savannah',
    1948,
    'Ngulia Rhino Sanctuary Command Post',
    'Specialized Rhino Sanctuary Electric Grid + Open Park',
    'CRITICAL - Intensive Rhino Anti-Poaching Sanctuary',
    'Mzima Springs Natural Oasis, Lake Jipe Wetlands, Ngulia Water Pan, Shetani Lava Pools',
    'Eastern Black Rhino, African Leopard, Hippopotamus, Crocodile, Wild Dog',
    'Characterized by rugged mountainous landscapes, the volcanic Shetani lava flow, and crystal-clear Mzima Springs. Features the heavily fortified Ngulia Rhino Sanctuary dedicated to endangered Eastern Black Rhinos.',
    -3.32,
    38.00,
    10
),
(
    'Maasai Mara National Reserve',
    'MAASAI_MARA',
    'Narok County',
    1510.0,
    'Rolling Grassland Savannah & Riverine Woodland',
    1961,
    'Sekenani Gate & Mara Triangle HQ',
    'Unfenced Greater Mara Ecosystem & Community Conservancies',
    'HIGH - Livestock Grazing Encroachment & Predator Conflict',
    'Mara River Crossing Points, Talek River Junction, Sand River Basin, Musiara Marsh',
    'Lions (Mara Predator Project), Cheetahs, Leopards, Elephants, Great Migration Wildebeest & Zebra',
    'Globally renowned for exceptional predator densities and the Great Wildebeest Migration across the Mara River. Monitored for predator-livestock conflict along adjacent community conservancy borders.',
    -1.50,
    35.15,
    11
),
(
    'Nairobi National Park',
    'NAIROBI_NP',
    'Nairobi City / Kajiado County',
    117.0,
    'Open Grass Plains, Acacia Bush & Riverine Gorge',
    1946,
    'KWS Central Command Headquarters & Ivory Burning Site Post',
    'Electrified Northern/Eastern City Perimeter (Unfenced South Kitengela Corridor)',
    'HIGH - Urban Encroachment & Southern Kitengela Buffer Pressures',
    'Nagolomon Dam, Hippo Pools (Mbagathi River), Athi Basin Waterholes, Hyena Dam',
    'Eastern Black Rhino Sanctuary, Lion Pride, Leopard, Cheetah, Maasai Giraffe, Cape Buffalo',
    'The only protected wildlife national park on Earth sharing a direct border with a capital metropolis. High-security sanctuary for Black Rhinos, requiring strict northern electric fence integrity.',
    -1.37,
    36.86,
    12
),
(
    'Ol Pejeta Conservancy',
    'OL_PEJETA',
    'Laikipia County',
    364.0,
    'High-Plateau Savannah, Ewaso Nyiro River Basin & Acacia Plains',
    1988,
    'Morani Command Post & Sweetwaters Research Base',
    'Smart Solar-Powered High-Security Perimeter Fence',
    'CRITICAL - Last Northern White Rhinos Sanctuary Protection',
    'Ewaso Nyiro River Pools, Sweetwaters Dam, Morani Waterhole, Pelican Dam',
    'Northern White Rhinos (Najin & Fatu), Black Rhinos, Grevy''s Zebra, African Wild Dog, Chimpanzees',
    'East Africa''s largest Black Rhino sanctuary and home to the world''s last two surviving Northern White Rhinos. Operates high-tech perimeter sensors, elite K9 anti-poaching units, and drone surveillance.',
    0.03,
    36.95,
    12
)
ON CONFLICT (park_name) DO UPDATE SET
    code = EXCLUDED.code,
    county = EXCLUDED.county,
    area_sq_km = EXCLUDED.area_sq_km,
    ecosystem_type = EXCLUDED.ecosystem_type,
    established_year = EXCLUDED.established_year,
    ranger_hq = EXCLUDED.ranger_hq,
    fence_type = EXCLUDED.fence_type,
    threat_level = EXCLUDED.threat_level,
    key_waterholes = EXCLUDED.key_waterholes,
    key_species = EXCLUDED.key_species,
    description = EXCLUDED.description,
    center_lat = EXCLUDED.center_lat,
    center_lng = EXCLUDED.center_lng,
    default_zoom = EXCLUDED.default_zoom;
