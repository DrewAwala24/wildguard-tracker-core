-- =========================================================================
-- WildGuard Tracker — Flyway Migration V2: Reference Seed Data
-- Kenya Wildlife Service Operations Command
-- =========================================================================

-- ── 1. Reference Species Registry ─────────────────────────────────────────
INSERT INTO species (common_name, scientific_name, iucn_status, typical_speed_kmh, habitat, diet_type, description)
VALUES
('African Bush Elephant', 'Loxodonta africana', 'VU', 3.2, 'Savannah, Bushland & Montane Forest', 'Herbivore', 'Largest land mammal on Earth, pivotal keystone species shaping African ecosystems through foraging and seasonal corridors.'),
('Eastern Black Rhino', 'Diceros bicornis michaeli', 'CR', 2.4, 'Acacia Scrubland & Volcanic Foothills', 'Herbivore (Browser)', 'Critically endangered browser with curved horn. Protected by intensive anti-poaching security sanctuaries in Ngulia, Nairobi, and Ol Pejeta.'),
('Northern White Rhino', 'Ceratotherium simum cottoni', 'CR', 2.5, 'High-Plateau Savannah', 'Herbivore (Grazer)', 'Functionally extinct in the wild; the world’s last two individuals (Najin and Fatu) are protected under 24/7 armed watch at Ol Pejeta Conservancy.'),
('Southern White Rhino', 'Ceratotherium simum simum', 'NT', 2.6, 'Grassland Plains & Lakeside Savannah', 'Herbivore (Grazer)', 'Square-lipped grazing rhino successfully conserved across Lake Nakuru, Lewa, and Ol Pejeta.'),
('African Lion', 'Panthera leo', 'VU', 4.2, 'Open Grasslands & Savannah Woodlands', 'Carnivore', 'Apex predator hunting in prides. Monitored closely for livestock-predator conflict along community borders in Mara, Amboseli, and Nairobi.'),
('Cheetah', 'Acinonyx jubatus', 'VU', 7.5, 'Open Plains & Short-grass Savannah', 'Carnivore', 'Fastest land animal, requiring vast open territories to hunt gazelles in Maasai Mara, Nairobi NP, and Samburu.'),
('Maasai Giraffe', 'Giraffa tippelskirchi', 'VU', 3.8, 'Acacia Woodlands & Thorn Savannas', 'Herbivore (Browser)', 'Distinctive jagged-edged spots. Browses tall acacia trees across southern Kenya corridors.'),
('Rothschild''s Giraffe', 'Giraffa camelopardalis rothschildi', 'VU', 3.8, 'Rift Valley Woodlands & Forest Margins', 'Herbivore (Browser)', 'Endangered giraffe subspecies characterized by creamy unspotted lower legs, thriving in Lake Nakuru and Soysambu.'),
('Grevy''s Zebra', 'Equus grevyi', 'EN', 3.5, 'Arid & Semi-Arid Shrubland', 'Herbivore (Grazer)', 'Narrow pinstripes and large round ears. Northern Kenya specialist species centered in Samburu and Lewa.'),
('Plains Zebra', 'Equus quagga', 'LC', 3.0, 'Savannah Plains & Open Grasslands', 'Herbivore (Grazer)', 'Common zebra forming large migratory herds across Athi-Kapiti, Amboseli, and the Great Migration.'),
('Mountain Bongo', 'Tragelaphus eurycerus isaaci', 'CR', 2.2, 'Afro-Montane Cloud Forest & Bamboo Thickets', 'Herbivore (Browser)', 'Critically endangered forest antelope with striking chestnut coat and white vertical stripes, endemic to Aberdare and Mount Kenya.'),
('Sitatunga', 'Tragelaphus spekii', 'LC', 2.0, 'Wetland Marshes & Papyrus Swamps', 'Herbivore (Browser)', 'Semi-aquatic antelope with splayed hooves designed for marsh walking, protected at Saiwa Swamp National Park.'),
('Secretary Bird', 'Sagittarius serpentarius', 'EN', 5.0, 'Open Savannah & Short Grassland', 'Carnivore', 'Large terrestrial bird of prey hunting snakes, rodents, and lizards on foot across Athi and Amboseli plains.'),
('Martial Eagle', 'Polemaetus bellicosus', 'EN', 35.0, 'Savannah, Woodland & Riverine Basins', 'Apex Avian Predator', 'Africa''s largest eagle, soaring over vast ranges in Tsavo, Mara, and Amboseli.')
ON CONFLICT (common_name) DO NOTHING;

-- ── 2. Reference User Accounts with BCrypt Hashed Passwords ──────────────
-- Password for all pre-seeded accounts is 'kws123'
-- Hash: $2a$10$GE1CNQpObZqOEX8dFKtNyu2ODZt9LZnZinijAe02q0xY8Gu.f0liG
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS contact_email VARCHAR(120);
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN DEFAULT TRUE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;

INSERT INTO users (username, password, role, full_name, contact_email, active, created_at)
VALUES
('commander', '$2a$10$GE1CNQpObZqOEX8dFKtNyu2ODZt9LZnZinijAe02q0xY8Gu.f0liG', 'OPERATIONS_COMMANDER', 'Snr. Warden Peter Mwangi', 'p.mwangi@kws.go.ke', TRUE, CURRENT_TIMESTAMP),
('admin', '$2a$10$GE1CNQpObZqOEX8dFKtNyu2ODZt9LZnZinijAe02q0xY8Gu.f0liG', 'ADMINISTRATOR', 'Lead Systems Engineer Drew Awala', 'admin@kws.go.ke', TRUE, CURRENT_TIMESTAMP),
('dispatcher', '$2a$10$GE1CNQpObZqOEX8dFKtNyu2ODZt9LZnZinijAe02q0xY8Gu.f0liG', 'DISPATCHER', 'Duty Officer Faith Cherono', 'f.cherono@kws.go.ke', TRUE, CURRENT_TIMESTAMP),
('ranger', '$2a$10$GE1CNQpObZqOEX8dFKtNyu2ODZt9LZnZinijAe02q0xY8Gu.f0liG', 'RANGER', 'Field Ranger Joseph Kiprono', 'j.kiprono@kws.go.ke', TRUE, CURRENT_TIMESTAMP),
('analyst', '$2a$10$GE1CNQpObZqOEX8dFKtNyu2ODZt9LZnZinijAe02q0xY8Gu.f0liG', 'ANALYST', 'Wildlife Biologist Dr. Grace Njeri', 'g.njeri@kws.go.ke', TRUE, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- ── 3. Ecological Landmarks across Kenya Monitored Ecosystems ─────────────
INSERT INTO ecological_landmarks (landmark_name, landmark_type, park_name, latitude, longitude, location, description)
VALUES
-- Amboseli NP
('Enkongo Narok Swamp', 'SWAMP', 'Amboseli NP', -2.6650, 37.2650, ST_SetSRID(ST_Point(37.2650, -2.6650), 4326), 'Perennial freshwater marsh fed by Kilimanjaro subterranean meltwater; critical dry-season forage for elephant families.'),
('Ol Tukai Springs', 'WATERHOLE', 'Amboseli NP', -2.6500, 37.3100, ST_SetSRID(ST_Point(37.3100, -2.6500), 4326), 'Lush oasis with towering palm trees, frequent herd resting site in Amboseli heartland.'),
('Observation Hill Point', 'OBS_POST', 'Amboseli NP', -2.6780, 37.2400, ST_SetSRID(ST_Point(37.2400, -2.6780), 4326), 'Pyramidal volcanic hill providing panoramic 360-degree surveillance across Lake Amboseli basin.'),

-- Tsavo East NP
('Galana River Rapids', 'RIVER_CROSSING', 'Tsavo East NP', -2.8124, 38.6210, ST_SetSRID(ST_Point(38.6210, -2.8124), 4326), 'Rushing waters over sandstone beds along the Galana River; primary dry season watering zone for Tsavo red bulls.'),
('Aruba Dam', 'DAM', 'Tsavo East NP', -3.1150, 38.8250, ST_SetSRID(ST_Point(38.8250, -3.1150), 4326), 'Man-made reservoir built in 1952 across the Voi River; magnet for tens of thousands of waterbirds and big tuskers.'),
('Mudanda Rock Outcrop', 'OBS_POST', 'Tsavo East NP', -3.1800, 38.5200, ST_SetSRID(ST_Point(38.5200, -3.1800), 4326), 'Vast 1.5 km stratified rock inselberg that sheds water into a natural pool below; natural observation post.'),

-- Tsavo West NP
('Ngulia Rhino Sanctuary', 'SANCTUARY', 'Tsavo West NP', -3.3200, 38.0000, ST_SetSRID(ST_Point(38.0000, -3.3200), 4326), 'Heavily fortified 90 sq km electric perimeter grid protecting pure Eastern Black Rhino breeding nucleus.'),
('Mzima Springs Oasis', 'WATERHOLE', 'Tsavo West NP', -2.9850, 37.9250, ST_SetSRID(ST_Point(37.9250, -2.9850), 4326), 'Fifty million gallons of crystal-clear volcanic spring water surging daily through submerged glass observatory.'),
('Shetani Lava Flow Pools', 'WATERHOLE', 'Tsavo West NP', -3.0500, 37.9800, ST_SetSRID(ST_Point(37.9800, -3.0500), 4326), 'Porous black volcanic slag field with mineral water pans along the Chyulu Hills boundary.'),

-- Maasai Mara
('Mara River Crossing No. 4', 'RIVER_CROSSING', 'Maasai Mara', -1.5100, 35.0350, ST_SetSRID(ST_Point(35.0350, -1.5100), 4326), 'Steep riverbanks famous for dramatic Great Migration wildebeest crossings and high crocodile concentration.'),
('Talek River Confluence', 'RIVER_CROSSING', 'Maasai Mara', -1.4420, 35.2150, ST_SetSRID(ST_Point(35.2150, -1.4420), 4326), 'Predator hunting grounds and territorial line between Talek river pride and Sekenani plains.'),
('Musiara Marsh', 'SWAMP', 'Maasai Mara', -1.3900, 35.1100, ST_SetSRID(ST_Point(35.1100, -1.3900), 4326), 'Waterlogged marshland providing permanent refuge for Marsh Pride lions and high buffalo herds.'),

-- Nairobi NP
('Hippo Pools Mbagathi', 'WATERHOLE', 'Nairobi NP', -1.3850, 36.8720, ST_SetSRID(ST_Point(36.8720, -1.3850), 4326), 'Deep river gorge along the Mbagathi River forming natural southern boundary of Nairobi National Park.'),
('Nagolomon Dam', 'DAM', 'Nairobi NP', -1.3820, 36.8250, ST_SetSRID(ST_Point(36.8250, -1.3820), 4326), 'Central watering dam frequently visited by black rhinos, giraffes, and cheetahs.'),
('Hyena Dam Plains', 'DAM', 'Nairobi NP', -1.3650, 36.8400, ST_SetSRID(ST_Point(36.8400, -1.3650), 4326), 'Watering hole nestled between savannah grassland ridges near northern safari circuit.'),
('Northern Electric Fence Sector 3', 'OBS_POST', 'Nairobi NP', -1.3435, 36.8800, ST_SetSRID(ST_Point(36.8800, -1.3435), 4326), 'Tactical surveillance watchtower monitoring the northern electric fence line bordering Nairobi metropolis.'),

-- Ol Pejeta Conservancy
('Sweetwaters Waterhole', 'WATERHOLE', 'Ol Pejeta', 0.0420, 36.9450, ST_SetSRID(ST_Point(36.9450, 0.0420), 4326), 'Central floodlit waterhole situated directly outside Sweetwaters Serena research command post.'),
('Morani Rhino Sanctuary', 'SANCTUARY', 'Ol Pejeta', 0.0150, 36.9600, ST_SetSRID(ST_Point(36.9600, 0.0150), 4326), 'Protected enclosure dedicated to blind and vulnerable rhinos with specialized veterinary monitoring.'),

-- Lake Nakuru
('Baboon Cliff Lookout', 'OBS_POST', 'Lake Nakuru', -0.3550, 36.0850, ST_SetSRID(ST_Point(36.0850, -0.3550), 4326), 'High cliff promontory offering commanding panoramic views of Lake Nakuru alkaline waters.'),
('Makalia Falls Basin', 'WATERHOLE', 'Lake Nakuru', -0.4150, 36.0950, ST_SetSRID(ST_Point(36.0950, -0.4150), 4326), 'Seasonal waterfall creating lush riverine forest pocket favored by leopards and white rhinos.'),

-- Samburu
('Ewaso Nyiro Riverbed', 'RIVER_CROSSING', 'Samburu', 0.6250, 37.5350, ST_SetSRID(ST_Point(37.5350, 0.6250), 4326), 'The brown waters of Ewaso Nyiro providing the sole water source cutting through semi-desert scrub.'),
('Buffalo Springs Confluence', 'WATERHOLE', 'Samburu', 0.5950, 37.5600, ST_SetSRID(ST_Point(37.5600, 0.5950), 4326), 'Crystal-clear volcanic spring outflow creating a desert marsh for Grevy''s zebras and Beisa oryx.'),

-- Mount Elgon
('Kitum Cave Elephant Mine', 'WATERHOLE', 'Mount Elgon', 1.1350, 34.5850, ST_SetSRID(ST_Point(34.5850, 1.1350), 4326), 'Subterranean volcanic cavern where elephants venture 200m underground into pitch darkness to gouge salt deposits.')
ON CONFLICT DO NOTHING;

-- ── 4. Weather Observations Seed ──────────────────────────────────────────
INSERT INTO weather_observations (park_name, temperature_c, conditions, humidity_percent, wind_speed_kmh, observation_source, observation_time)
VALUES
('Amboseli National Park', 28.5, 'Clear Savannah Sun', 42, 16.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Tsavo East National Park', 31.0, 'Dry Heat & Light Breeze', 38, 22.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Tsavo West National Park', 29.2, 'Partly Cloudy Volcanic Ridges', 45, 14.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Maasai Mara National Reserve', 24.8, 'Scattered Cumulus Clouds', 58, 12.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Nairobi National Park', 23.5, 'Partly Cloudy & Dry', 52, 18.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Ol Pejeta Conservancy', 21.0, 'Equatorial Highland Breeze', 48, 15.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Lake Nakuru National Park', 25.2, 'Lakeside Haze & Sun', 55, 10.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Samburu National Reserve', 33.4, 'Intense Arid Sunshine', 31, 20.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Aberdare National Park', 16.8, 'Mist & Montane Drizzle', 82, 8.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Mount Kenya National Park', 12.5, 'Alpine Cold & High Winds', 75, 26.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Lewa Wildlife Conservancy', 22.8, 'Warm Plateau Sunlight', 46, 14.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Saiwa Swamp National Park', 20.4, 'Humid Riverine Forest', 86, 6.0, 'SENSOR_METEO', CURRENT_TIMESTAMP),
('Mount Elgon National Park', 18.6, 'Montane Cloud Cover', 79, 11.0, 'SENSOR_METEO', CURRENT_TIMESTAMP);
