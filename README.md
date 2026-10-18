# 🐾 WildGuard Tracker — Kenya Wildlife Service Operations Command

[![Platform](https://img.shields.io/badge/Platform-Native%20JavaFX%20Desktop-512BD4)](https://openjfx.io)
[![Backend](https://img.shields.io/badge/Backend-Spring%20Boot%203%20%7C%20Java%2017%2B-6DB33F)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-PostgreSQL%2016%20%2B%20PostGIS-336791)](https://postgis.net/)
[![Spatial](https://img.shields.io/badge/GIS-PostGIS%20%2B%20JTS%20Topology-FFD700)](https://locationtech.github.io/jts/)
[![Migrations](https://img.shields.io/badge/Schema-Flyway%20Migrations-CC0200)](https://flywaydb.org/)

**WildGuard Tracker** is an enterprise spatial monitoring and dispatch desktop application engineered for the **Kenya Wildlife Service (KWS)**. It provides field commanders, dispatchers, and rangers with real-time collar telemetry tracking, PostGIS-powered geofence monitoring, early-warning Human-Wildlife Conflict (HWC) breach alerts, and rapid-response patrol coordination across Kenya's flagship national parks and conservancies.

The application is built entirely as a **single-JVM Native Java Desktop Application (JavaFX + Spring Boot + PostGIS)** with zero dependencies on external C#/.NET runtimes or web wrappers.

---

## 🌍 Monitored Kenyan Ecosystems

| Sector / Park | Ecosystem & County | Target Species | Key Operational Mission |
| :--- | :--- | :--- | :--- |
| **Amboseli National Park** | Kajiado / Mt. Kilimanjaro Basin | African Elephants (*Loxodonta africana*) | Monitoring herds (*Echo's Matriarch*, *Mutula Bull*) along the southern Kimana community agricultural buffer corridor. |
| **Tsavo East & West National Parks** | Taita-Taveta / Galana River | Red Elephants, Lions (*Panthera leo*) | Vast wilderness tracking (*Galana Red Bull*, *Satao Pride*) across Voi dispersal corridors and SGR crossings. |
| **Maasai Mara National Reserve** | Narok / Mara River Basin | Apex Predators (Lions, Cheetahs) | Mara Predator Project (*Kipsing Male*, *Talek River Female*) and community conservancy boundary monitoring. |
| **Nairobi National Park** | Nairobi / Athi-Kapiti Plains | Eastern Black Rhinos (*Diceros bicornis*) | High-security rhino sanctuary (*Mukurwe Black Rhino*) along the unfenced southern Kitengela dispersal zone. |
| **Ol Pejeta Conservancy** | Laikipia Plateau | Black & Northern White Rhinos | Dedicated anti-poaching security tracking (*Baraka Rhino*) on the high-plateau conservancy. |
| **Lake Nakuru National Park** | Nakuru / Great Rift Valley | White Rhinos, Rothschild's Giraffes | Fenced rhino and avian sanctuary monitoring. |
| **Samburu National Reserve** | Samburu / Ewaso Ng'iro Basin | Grevy's Zebras, Lions | Northern arid range conservation. |
| **Aberdare National Park** | Nyeri / Aberdare Range | Mountain Bongos, Elephants | High-altitude rainforest patrol tracking. |
| **Mount Kenya National Park** | Meru, Nyeri, Kirinyaga | Mountain Bongos, Tuskers | Montane forest ecosystem protection. |
| **Lewa Wildlife Conservancy** | Meru / Northern Slopes | Black Rhinos, Grevy's Zebras | UNESCO World Heritage community conservancy. |
| **Saiwa Swamp National Park** | Trans-Nzoia / Cherangani Hills | Sitatunga Antelopes | Wetland sanctuary conservation. |
| **Mount Elgon National Park** | Bungoma / Western Border | Cave Elephants | Volcanic slopes and subterranean salt cave monitoring. |

---

## ⚡ Core Architecture & Capabilities

### 1. 🗺️ Native JavaFX GIS Operations Map & Tactical Overlays
- **100% Native Vector GIS Engine (`GisMapCanvas`):** Custom high-performance JavaFX Canvas renderer that converts PostGIS WGS-84 geometries directly to screen coordinates with smooth panning, zooming, and day/night tactical radar modes.
- **Sovereign Border & Sector Geometry:** Renders Kenya's national border, 13 national park polygons, and high-conflict community buffer corridors.
- **Nairobi NP Northern Electric Fence:** Explicit polyline with strict repelling logic to protect urban frontiers.
- **Ecological Landmark Markers:** Waterholes, dams, and river crossings (Nagolomon Dam, Hippo Pools, Galana River Crossing, Aruba Dam).
- **Real-Time Collar Blips & Historical Trails:** Color-coded pins with pulsing breach alert rings and rolling 25-point breadcrumb trails.

### 2. 🦁 Realistic Sanctuary-Bound Wildlife Simulation
- **Waypoint Circuit Navigation:** Wildlife follow species-appropriate paths between feeding grounds, waterholes, and river crossings.
- **PostGIS Polygon Containment:** Every proposed step is spatially verified. Animals approaching boundaries are steered back into protected zones.
- **Species-Specific Velocity & Behavior:**
  - 🦁 **Lions** — rest 15% of the time, patrol at ~4.2 km/h.
  - 🐘 **Elephants** — family foraging at ~3.2 km/h.
  - 🦏 **Rhinos** — deliberate browsing at ~2.4 km/h.
  - 🐆 **Cheetahs** — open plains hunting at ~7.5 km/h.
  - 🦒 **Giraffes & Zebras** — herd grazing at ~3-3.8 km/h.
- **Interactive Controls:** Pause, resume, manual single-step tick, and 1× / 2× / 4× speed multipliers.

- **Waypoint circuit navigation:** Each animal follows a species-appropriate circuit of real ecological waypoints (waterholes, dams, river crossings, seasonal grazing areas). Animals automatically advance to the next waypoint on arrival.
- **Polygon containment enforcement:** Every proposed movement step is validated against an accurate park polygon. Animals that approach a boundary are steered back toward the nearest waypoint inside the sanctuary — they **never** escape into the city or surrounding areas.
- **Nairobi NP northern fence hard limit:** A dedicated latitude guard (`< -1.3430°`) and heading-repulsion algorithm actively pushes animals southward away from the electric fence line and into the Mbagathi River basin and Athi plains.
- **Species-specific behavior:**
  - **Lions**— rest 15% of the time (`Resting`), patrol at ~4.2 km/h.
  - **Elephants** — family corridor foraging at ~3.2 km/h.
  - **Rhinos** — slow deliberate browser at ~2.4 km/h.
  - **Cheetahs** — fastest animal; open plains scanning at ~7.5 km/h.
  - **Zebras/Wildebeest/Buffalo** — herd grazing at ~3 km/h.
- **Heading momentum (smooth steering):** Animals turn gradually (25% angular correction per tick) with gentle natural wander noise — no teleporting or 90° snaps.
- **Simulation controls:** Play/Pause button and 1×/2×/4× speed multiplier exposed in the XAML toolbar.
- **Live fence proximity display:** Nairobi NP animals report exact distance to the northern electric fence (`Near Northern Fence (230m from fence)` or `Safe in Sanctuary (2.1 km from City fence)`).

### 3. 🚨 PostGIS Geofencing, Breach Alerts & Incident Lifecycle
- Automated spatial checks (`ST_Contains`, `ST_Intersects`, `ST_DWithin`) against buffer corridors.
- **Incident Lifecycle:** Auto-detection → Acknowledgment by commanders → Patrol Dispatch → Resolution with notes.
- **Duplicate Alert Suppression:** Prevents alert flooding when wildlife remains inside a buffer zone.

### 4. 🛡️ Ranger Patrols & Dispatch Coordination
- Tracking of ground cruisers (Land Cruisers) and airwing aircraft with call signs, sector assignments, and coordinates.
- Transactional dispatch workflow linking ranger units directly to active incidents.

### 5. 🔐 Role-Based Access Control & Auditing
- Secure authentication with BCrypt password hashing.
- Standard operational roles: `ADMINISTRATOR`, `OPERATIONS_COMMANDER`, `DISPATCHER`, `RANGER`, `ANALYST`.
- Persistent audit logs recording all registrations, dispatches, and resolutions.

---

## 🏗️ Repository Structure

```
KWS/
├── backend/
│   ├── build.gradle                             # Gradle build configuration
│   └── src/
│       ├── main/
│       │   ├── java/com/kws/backend/backend/
│       │   │   ├── BackendApplication.java       # Unified Spring Boot & JavaFX entry point
│       │   │   ├── config/                       # SpatialConfig, SecurityConfig, DataInitializer
│       │   │   ├── desktop/
│       │   │   │   ├── JavaFxApplication.java    # JavaFX Application lifecycle
│       │   │   │   ├── controller/               # MainOperationsController, AuthDialogController
│       │   │   │   └── view/                     # GisMapCanvas, WildlifeCardComponent, etc.
│       │   │   ├── model/                        # JPA Entities (Animal, GeofenceZone, IncidentLog, etc.)
│       │   │   ├── repository/                   # Spring Data JPA & PostGIS Repositories
│       │   │   └── service/                      # Telemetry, Simulation, Incident, Patrol services
│       │   └── resources/
│       │       ├── application.properties        # Database & Flyway configuration
│       │       ├── db/migration/                 # V1 Schema, V2 Reference Data
│       │       ├── fonts/                        # Montserrat TrueType fonts
│       │       └── images/                       # Authentic wildlife photographic assets
│       └── test/                                 # Comprehensive JUnit 5 & PostGIS test suite
├── launch-desktop.sh                             # Executable launcher script
└── kws-wildguard.desktop                         # Linux Desktop Application entry
```

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK) 17+ LTS** (OpenJDK or Oracle JDK)
- **PostgreSQL 15+** with the **PostGIS** extension
- **Bash / Linux / macOS / Windows with WSL**

### 1. Database Setup
```sql
CREATE DATABASE kws_tracker_db;
\c kws_tracker_db;
CREATE EXTENSION IF NOT EXISTS postgis;
```

### 2. Configure Credentials
Edit `backend/src/main/resources/application.properties` (or set environment variables):
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/kws_tracker_db
spring.datasource.username=postgres
spring.datasource.password=YOUR_PASSWORD
```

### 3. Launching the Application
Execute the unified desktop launcher:
```bash
./launch-desktop.sh
```

Or build and run directly with Gradle:
```bash
cd backend
./gradlew bootJar
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

### Pre-Configured Operations Accounts
| Username | Password | Role |
| :--- | :--- | :--- |
| `commander` | `kws123` | Operations Commander |
| `admin` | `kws123` | Administrator |
| `dispatcher` | `kws123` | Dispatcher |
| `ranger` | `kws123` | Field Ranger |
| `analyst` | `kws123` | Wildlife Biologist |

---

## 🧪 Running Automated Tests
```bash
cd backend
./gradlew test
```
The test suite validates:
- Spatial boundary containment and PostGIS intersections
- Wildlife collar CRUD and historical telemetry retention
- Geofence entry/exit alert triggers and duplicate suppression
- Patrol unit dispatch order lifecycle and availability state transitions
- BCrypt authentication, role-based authorization, and user activation
- Flyway database schema initialization and migrations

---

## 🛡️ License & Acknowledgments
Built for wildlife conservation operations. Dedicated to field rangers, conservation biologists, and anti-poaching patrol units safeguarding Kenya's natural heritage.
