using System.Diagnostics;
using System.Net.Http.Json;
using System.Text.Json;
using frontend.Models;

namespace frontend.Services;

public class ApiService
{
    private readonly HttpClient _httpClient;
    public const string BaseUrl = "http://localhost:8080";

    private static readonly JsonSerializerOptions JsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        NumberHandling = System.Text.Json.Serialization.JsonNumberHandling.AllowReadingFromString
    };

    public ApiService(HttpClient httpClient)
    {
        _httpClient = httpClient ?? new HttpClient();
        if (_httpClient.BaseAddress == null)
        {
            _httpClient.BaseAddress = new Uri(BaseUrl);
        }
        _httpClient.Timeout = TimeSpan.FromSeconds(10);
    }

    public async Task<List<AnimalDto>> GetAnimalsAsync()
    {
        try
        {
            Debug.WriteLine($"[ApiService] Fetching animals from {BaseUrl}/api/animals");
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/animals");

            if (response.IsSuccessStatusCode)
            {
                var animals = await response.Content.ReadFromJsonAsync<List<AnimalDto>>(JsonOptions);
                if (animals != null && animals.Count > 0)
                {
                    // Enrich any missing Kenyan park metadata
                    EnsureKenyaParkMetadata(animals);
                    Debug.WriteLine($"[ApiService] Retrieved {animals.Count} animals from API.");
                    return animals;
                }
            }
            else
            {
                Debug.WriteLine($"[ApiService] Animal endpoint returned HTTP {response.StatusCode}. Falling back to default Kenyan wildlife registry.");
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] API request failed ({ex.Message}). Using default Kenyan wildlife registry.");
        }

        return GetDefaultKenyaWildlife();
    }

    public async Task<List<TelemetryLocation>> GetTelemetryAsync()
    {
        try
        {
            Debug.WriteLine($"[ApiService] Fetching telemetry from {BaseUrl}/api/telemetry");
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/telemetry");

            if (response.IsSuccessStatusCode)
            {
                var telemetryList = await response.Content.ReadFromJsonAsync<List<TelemetryLocation>>(JsonOptions);
                if (telemetryList != null && telemetryList.Count > 0)
                {
                    NormalizeTelemetry(telemetryList);
                    Debug.WriteLine($"[ApiService] Retrieved {telemetryList.Count} telemetry logs from API.");
                    return telemetryList;
                }
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Telemetry API call failed ({ex.Message}). Using default Kenyan telemetry dataset.");
        }

        return GetDefaultKenyaTelemetry();
    }

    public async Task<bool> RegisterAnimalAsync(AnimalDto newAnimal)
    {
        try
        {
            Debug.WriteLine($"[ApiService] Registering new animal: {newAnimal.Name} ({newAnimal.CollarId})");
            var response = await _httpClient.PostAsJsonAsync($"{BaseUrl}/api/animals", newAnimal);
            return response.IsSuccessStatusCode;
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to post animal to backend: {ex.Message}");
            return false;
        }
    }

    public async Task<List<GeofenceDto>> GetGeofencesAsync()
    {
        try
        {
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/geofences");
            if (response.IsSuccessStatusCode)
            {
                var zones = await response.Content.ReadFromJsonAsync<List<GeofenceDto>>(JsonOptions);
                if (zones != null && zones.Count > 0)
                {
                    foreach (var z in zones)
                    {
                        z.NormalizeCoordinates();
                    }
                    return zones;
                }
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to fetch geofences: {ex.Message}. Loading verified Kenyan parks.");
        }

        return GetVerifiedKenyaGeofences();
    }

    public async Task<List<PatrolUnitDto>> GetPatrolUnitsAsync()
    {
        try
        {
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/patrols");
            if (response.IsSuccessStatusCode)
            {
                var units = await response.Content.ReadFromJsonAsync<List<PatrolUnitDto>>(JsonOptions);
                if (units != null && units.Count > 0)
                {
                    return units;
                }
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to fetch patrol units: {ex.Message}. Using default KWS units.");
        }

        return GetDefaultPatrolUnits();
    }

    public async Task<bool> DispatchPatrolAsync(long patrolId, double targetLat, double targetLng)
    {
        try
        {
            var payload = new { latitude = targetLat, longitude = targetLng };
            var response = await _httpClient.PostAsJsonAsync($"{BaseUrl}/api/patrols/{patrolId}/dispatch", payload);
            return response.IsSuccessStatusCode;
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to dispatch patrol unit {patrolId}: {ex.Message}");
            return false;
        }
    }

    public async Task<bool> BroadcastCommunitySmsAsync(string corridor, string message)
    {
        try
        {
            var payload = new Dictionary<string, string>
            {
                { "corridor", corridor },
                { "message", message }
            };
            var response = await _httpClient.PostAsJsonAsync($"{BaseUrl}/api/incidents/broadcast", payload);
            return response.IsSuccessStatusCode;
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to broadcast community SMS: {ex.Message}");
            return false;
        }
    }

    public async Task<bool> ResolveIncidentAsync(long incidentId, string notes)
    {
        try
        {
            var payload = new Dictionary<string, string> { { "notes", notes } };
            var response = await _httpClient.PostAsJsonAsync($"{BaseUrl}/api/incidents/{incidentId}/resolve", payload);
            return response.IsSuccessStatusCode;
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to resolve incident {incidentId}: {ex.Message}");
            return false;
        }
    }

    public async Task<List<TelemetryTrailDto>> GetAnimalTrailAsync(string collarId)
    {
        try
        {
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/telemetry/animal/{collarId}/trail");
            if (response.IsSuccessStatusCode)
            {
                var trail = await response.Content.ReadFromJsonAsync<List<TelemetryTrailDto>>(JsonOptions);
                if (trail != null && trail.Count > 0)
                {
                    return trail;
                }
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Failed to fetch trail for {collarId}: {ex.Message}");
        }

        return new List<TelemetryTrailDto>();
    }

    public async Task<bool> TriggerSimulationStepAsync()
    {
        try
        {
            var response = await _httpClient.PostAsync($"{BaseUrl}/api/telemetry/simulate", null);
            return response.IsSuccessStatusCode;
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Simulation trigger failed: {ex.Message}");
            return false;
        }
    }

    private void NormalizeTelemetry(List<TelemetryLocation> telemetryList)
    {
        foreach (var item in telemetryList)
        {
            if (string.IsNullOrEmpty(item.AnimalId) && item.Animal != null)
            {
                item.AnimalId = !string.IsNullOrEmpty(item.Animal.CollarId) 
                    ? item.Animal.CollarId 
                    : item.Animal.Id.ToString();
            }

            if (item.Latitude == 0 && item.Longitude == 0 && item.RawLocation.HasValue)
            {
                if (item.RawLocation.Value.TryGetProperty("coordinates", out var coords) && coords.GetArrayLength() >= 2)
                {
                    item.Longitude = coords[0].GetDouble();
                    item.Latitude = coords[1].GetDouble();
                }
            }
        }
    }

    private void EnsureKenyaParkMetadata(List<AnimalDto> animals)
    {
        foreach (var a in animals)
        {
            if (string.IsNullOrWhiteSpace(a.ParkName) || a.ParkName == "Tsavo East NP")
            {
                // Assign realistic Kenyan park based on coordinates or species
                if (a.Latitude < -2.4 && a.Longitude < 37.8)
                    a.ParkName = "Amboseli NP";
                else if (a.Latitude < -2.0 && a.Longitude >= 37.8)
                    a.ParkName = "Tsavo East NP";
                else if (a.Latitude < -1.0 && a.Longitude < 36.0)
                    a.ParkName = "Maasai Mara";
                else if (a.Latitude < -1.2 && a.Latitude > -1.5 && a.Longitude > 36.6 && a.Longitude < 37.0)
                    a.ParkName = "Nairobi NP";
                else if (a.Latitude >= 0.0)
                    a.ParkName = "Ol Pejeta";
            }
        }
    }

    public static List<AnimalDto> GetDefaultKenyaWildlife()
    {
        return new List<AnimalDto>
        {
            new()
            {
                Id = 1,
                Name = "Echo's Matriarch",
                Species = "African Elephant",
                CollarId = "KWS-AMB-ELE01",
                ParkName = "Amboseli NP",
                Sex = "Female",
                CollarBattery = 94,
                Latitude = -2.6531,
                Longitude = 37.2625,
                Status = "Active",
                IsBreaching = false
            },
            new()
            {
                Id = 2,
                Name = "Mutula Bull",
                Species = "African Elephant",
                CollarId = "KWS-AMB-ELE04",
                ParkName = "Amboseli NP",
                Sex = "Male",
                CollarBattery = 88,
                Latitude = -2.6950, // Edge of Kimana Corridor
                Longitude = 37.3320,
                Status = "Alert",
                IsBreaching = true
            },
            new()
            {
                Id = 3,
                Name = "Galana Red Bull",
                Species = "African Elephant",
                CollarId = "KWS-TSV-ELE12",
                ParkName = "Tsavo East NP",
                Sex = "Male",
                CollarBattery = 91,
                Latitude = -2.8124,
                Longitude = 38.6210,
                Status = "Active",
                IsBreaching = false
            },
            new()
            {
                Id = 4,
                Name = "Satao Pride Leader",
                Species = "Lion",
                CollarId = "KWS-TSV-LIO05",
                ParkName = "Tsavo East NP",
                Sex = "Female",
                CollarBattery = 83,
                Latitude = -3.1250,
                Longitude = 38.5410,
                Status = "Active",
                IsBreaching = false
            },
            new()
            {
                Id = 5,
                Name = "Kipsing Male",
                Species = "Lion",
                CollarId = "KWS-MAR-LIO02",
                ParkName = "Maasai Mara",
                Sex = "Male",
                CollarBattery = 79,
                Latitude = -1.4850,
                Longitude = 35.1280,
                Status = "Active",
                IsBreaching = false
            },
            new()
            {
                Id = 6,
                Name = "Talek River Female",
                Species = "Cheetah",
                CollarId = "KWS-MAR-CHT01",
                ParkName = "Maasai Mara",
                Sex = "Female",
                CollarBattery = 96,
                Latitude = -1.4420,
                Longitude = 35.2150,
                Status = "Active",
                IsBreaching = false
            },
            new()
            {
                Id = 7,
                Name = "Mukurwe Black Rhino",
                Species = "Black Rhino",
                CollarId = "KWS-NBI-RHI03",
                ParkName = "Nairobi NP",
                Sex = "Male",
                CollarBattery = 89,
                Latitude = -1.3780,
                Longitude = 36.8420,
                Status = "Active",
                IsBreaching = false
            },
            new()
            {
                Id = 8,
                Name = "Baraka Rhino",
                Species = "Eastern Black Rhino",
                CollarId = "KWS-OLP-RHI01",
                ParkName = "Ol Pejeta",
                Sex = "Male",
                CollarBattery = 98,
                Latitude = 0.0420,
                Longitude = 36.9450,
                Status = "Active",
                IsBreaching = false
            }
        };
    }

    public static List<TelemetryLocation> GetDefaultKenyaTelemetry()
    {
        var now = DateTime.UtcNow;
        return new List<TelemetryLocation>
        {
            new() { Id = 101, AnimalId = "KWS-AMB-ELE04", Latitude = -2.6950, Longitude = 37.3320, Timestamp = now.AddMinutes(-3) },
            new() { Id = 102, AnimalId = "KWS-AMB-ELE01", Latitude = -2.6531, Longitude = 37.2625, Timestamp = now.AddMinutes(-12) },
            new() { Id = 103, AnimalId = "KWS-NBI-RHI03", Latitude = -1.3780, Longitude = 36.8420, Timestamp = now.AddMinutes(-25) },
            new() { Id = 104, AnimalId = "KWS-TSV-ELE12", Latitude = -2.8124, Longitude = 38.6210, Timestamp = now.AddMinutes(-42) },
            new() { Id = 105, AnimalId = "KWS-MAR-LIO02", Latitude = -1.4850, Longitude = 35.1280, Timestamp = now.AddMinutes(-55) },
            new() { Id = 106, AnimalId = "KWS-OLP-RHI01", Latitude = 0.0420, Longitude = 36.9450, Timestamp = now.AddMinutes(-68) },
            new() { Id = 107, AnimalId = "KWS-TSV-LIO05", Latitude = -3.1250, Longitude = 38.5410, Timestamp = now.AddHours(-2) },
            new() { Id = 108, AnimalId = "KWS-MAR-CHT01", Latitude = -1.4420, Longitude = 35.2150, Timestamp = now.AddHours(-3) }
        };
    }

    public static List<GeofenceDto> GetVerifiedKenyaGeofences()
    {
        return new List<GeofenceDto>
        {
            // Amboseli National Park
            new()
            {
                Id = 1,
                ZoneName = "Amboseli National Park",
                ZoneType = "PARK",
                CenterLatitude = -2.6527,
                CenterLongitude = 37.2606,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -2.58, 37.18 },
                    new[] { -2.58, 37.35 },
                    new[] { -2.72, 37.35 },
                    new[] { -2.72, 37.18 },
                    new[] { -2.58, 37.18 }
                }
            },
            // Kimana Community Dispersal Area (Amboseli Buffer)
            new()
            {
                Id = 2,
                ZoneName = "Kimana Wildlife Corridor (Buffer)",
                ZoneType = "COMMUNITY_BUFFER",
                CenterLatitude = -2.70,
                CenterLongitude = 37.38,
                ColorHex = "#FF9800",
                Coordinates = new List<double[]>
                {
                    new[] { -2.68, 37.35 },
                    new[] { -2.68, 37.45 },
                    new[] { -2.75, 37.45 },
                    new[] { -2.75, 37.35 },
                    new[] { -2.68, 37.35 }
                }
            },
            // Nairobi National Park (Accurate perimeter strictly south of Lang'ata / Wilson Airport fence)
            new()
            {
                Id = 3,
                ZoneName = "Nairobi National Park",
                ZoneType = "PARK",
                CenterLatitude = -1.3733,
                CenterLongitude = 36.8589,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -1.3500, 36.7760 }, // KWS Headquarters / Main Gate
                    new[] { -1.3465, 36.7950 }, // Langata Forest Sanctuary
                    new[] { -1.3420, 36.8200 }, // Southern Bypass Fence Line
                    new[] { -1.3425, 36.8450 }, // South of Wilson Airport Perimeter
                    new[] { -1.3440, 36.8720 }, // South of South C / Nairobi West
                    new[] { -1.3520, 36.8980 }, // Mombasa Road / Inland Container Depot
                    new[] { -1.3620, 36.9250 }, // Syokimau / SGR Boundary
                    new[] { -1.3900, 36.9500 }, // Athi River Basin (East)
                    new[] { -1.4250, 36.9380 }, // Athi River Confluence
                    new[] { -1.4380, 36.8900 }, // Mbagathi River / Kitengela Migration Border
                    new[] { -1.4300, 36.8500 }, // Southern Riverine Savanna
                    new[] { -1.4150, 36.8150 }, // Silole Sanctuary / Gorge
                    new[] { -1.3950, 36.7800 }, // Kingfisher Gorge
                    new[] { -1.3700, 36.7650 }, // Western Park Boundary
                    new[] { -1.3500, 36.7760 }  // Close perimeter at KWS HQ
                }
            },
            // Kitengela Dispersal Area (South of Nairobi NP across Mbagathi River)
            new()
            {
                Id = 4,
                ZoneName = "Kitengela Dispersal Zone",
                ZoneType = "COMMUNITY_BUFFER",
                CenterLatitude = -1.455,
                CenterLongitude = 36.885,
                ColorHex = "#FF5722",
                Coordinates = new List<double[]>
                {
                    new[] { -1.4380, 36.8900 },
                    new[] { -1.4250, 36.9380 },
                    new[] { -1.5000, 36.9500 },
                    new[] { -1.5050, 36.8500 },
                    new[] { -1.4380, 36.8900 }
                }
            },
            // Maasai Mara National Reserve
            new()
            {
                Id = 5,
                ZoneName = "Maasai Mara National Reserve",
                ZoneType = "PARK",
                CenterLatitude = -1.4917,
                CenterLongitude = 35.1444,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -1.38, 34.98 },
                    new[] { -1.38, 35.32 },
                    new[] { -1.62, 35.32 },
                    new[] { -1.62, 34.98 },
                    new[] { -1.38, 34.98 }
                }
            },
            // Tsavo East National Park
            new()
            {
                Id = 6,
                ZoneName = "Tsavo East National Park",
                ZoneType = "PARK",
                CenterLatitude = -2.85,
                CenterLongitude = 38.65,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -2.40, 38.30 },
                    new[] { -2.40, 39.00 },
                    new[] { -3.30, 39.00 },
                    new[] { -3.30, 38.30 },
                    new[] { -2.40, 38.30 }
                }
            },
            // Ol Pejeta Conservancy
            new()
            {
                Id = 7,
                ZoneName = "Ol Pejeta Conservancy",
                ZoneType = "CONSERVANCY",
                CenterLatitude = 0.0389,
                CenterLongitude = 36.9639,
                ColorHex = "#00E5FF",
                Coordinates = new List<double[]>
                {
                    new[] { 0.00, 36.88 },
                    new[] { 0.00, 37.05 },
                    new[] { 0.10, 37.05 },
                    new[] { 0.10, 36.88 },
                    new[] { 0.00, 36.88 }
                }
            },
            // Tsavo West National Park
            new()
            {
                Id = 8,
                ZoneName = "Tsavo West National Park",
                ZoneType = "PARK",
                CenterLatitude = -3.10,
                CenterLongitude = 38.05,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -2.75, 37.80 },
                    new[] { -2.75, 38.35 },
                    new[] { -3.60, 38.35 },
                    new[] { -3.60, 37.80 },
                    new[] { -2.75, 37.80 }
                }
            },
            // Mara North Conservancy
            new()
            {
                Id = 9,
                ZoneName = "Mara North Conservancy",
                ZoneType = "CONSERVANCY",
                CenterLatitude = -1.28,
                CenterLongitude = 35.15,
                ColorHex = "#00E5FF",
                Coordinates = new List<double[]>
                {
                    new[] { -1.18, 35.05 },
                    new[] { -1.18, 35.25 },
                    new[] { -1.38, 35.25 },
                    new[] { -1.38, 35.05 },
                    new[] { -1.18, 35.05 }
                }
            },
            // Lewa Wildlife Conservancy
            new()
            {
                Id = 10,
                ZoneName = "Lewa Wildlife Conservancy",
                ZoneType = "CONSERVANCY",
                CenterLatitude = 0.25,
                CenterLongitude = 37.45,
                ColorHex = "#00E5FF",
                Coordinates = new List<double[]>
                {
                    new[] { 0.18, 37.38 },
                    new[] { 0.18, 37.55 },
                    new[] { 0.32, 37.55 },
                    new[] { 0.32, 37.38 },
                    new[] { 0.18, 37.38 }
                }
            },
            // Lake Nakuru National Park
            new()
            {
                Id = 11,
                ZoneName = "Lake Nakuru National Park",
                ZoneType = "PARK",
                CenterLatitude = -0.37,
                CenterLongitude = 36.08,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -0.32, 36.04 },
                    new[] { -0.32, 36.14 },
                    new[] { -0.45, 36.14 },
                    new[] { -0.45, 36.04 },
                    new[] { -0.32, 36.04 }
                }
            },
            // Samburu National Reserve
            new()
            {
                Id = 12,
                ZoneName = "Samburu National Reserve",
                ZoneType = "PARK",
                CenterLatitude = 0.62,
                CenterLongitude = 37.53,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { 0.55, 37.45 },
                    new[] { 0.55, 37.65 },
                    new[] { 0.70, 37.65 },
                    new[] { 0.70, 37.45 },
                    new[] { 0.55, 37.45 }
                }
            },
            // Aberdare National Park
            new()
            {
                Id = 13,
                ZoneName = "Aberdare National Park",
                ZoneType = "PARK",
                CenterLatitude = -0.55,
                CenterLongitude = 36.72,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -0.35, 36.65 },
                    new[] { -0.35, 36.85 },
                    new[] { -0.75, 36.85 },
                    new[] { -0.75, 36.65 },
                    new[] { -0.35, 36.65 }
                }
            },
            // Mount Kenya National Park
            new()
            {
                Id = 14,
                ZoneName = "Mount Kenya National Park",
                ZoneType = "PARK",
                CenterLatitude = -0.15,
                CenterLongitude = 37.32,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { -0.05, 37.20 },
                    new[] { -0.05, 37.45 },
                    new[] { -0.28, 37.45 },
                    new[] { -0.28, 37.20 },
                    new[] { -0.05, 37.20 }
                }
            }
        };
    }

    public static List<PatrolUnitDto> GetDefaultPatrolUnits()
    {
        return new List<PatrolUnitDto>
        {
            new() { Id = 1, Name = "Amboseli Rhino & Elephant Rapid Unit", CallSign = "PATROL-AMB-01", UnitType = "LAND_CRUISER", Latitude = -2.6210, Longitude = 37.2450, Status = "AVAILABLE", Sector = "Amboseli NP" },
            new() { Id = 2, Name = "Tsavo East Strike Team", CallSign = "PATROL-TSV-03", UnitType = "LAND_CRUISER", Latitude = -2.8540, Longitude = 38.6010, Status = "AVAILABLE", Sector = "Tsavo East NP" },
            new() { Id = 3, Name = "Mara Predator Rapid Response", CallSign = "PATROL-MAR-02", UnitType = "LAND_CRUISER", Latitude = -1.4500, Longitude = 35.1500, Status = "AVAILABLE", Sector = "Maasai Mara", AltitudeFt = 0, SpeedKmh = 45.0 },
            new() { Id = 4, Name = "KWS Airwing Recon Cessna", CallSign = "AIRWING-KWS-09", UnitType = "AIRWING", Latitude = -1.3650, Longitude = 36.8500, Status = "ON_PATROL", Sector = "Nairobi NP", AltitudeFt = 2450, SpeedKmh = 165.0 }
        };
    }

    public async Task<List<ParkProfileDto>> GetParkProfilesAsync()
    {
        try
        {
            Debug.WriteLine($"[ApiService] Fetching park profiles from {BaseUrl}/api/parks");
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/parks");
            if (response.IsSuccessStatusCode)
            {
                var profiles = await response.Content.ReadFromJsonAsync<List<ParkProfileDto>>(JsonOptions);
                if (profiles != null && profiles.Count > 0)
                {
                    Debug.WriteLine($"[ApiService] Retrieved {profiles.Count} park profiles from backend.");
                    return profiles;
                }
            }
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Park profiles API request failed ({ex.Message}). Using local park profiles dataset.");
        }

        return GetDefaultParkProfiles();
    }

    public async Task<ParkProfileDto?> GetParkProfileByNameAsync(string parkName)
    {
        var profiles = await GetParkProfilesAsync();
        return profiles.FirstOrDefault(p =>
            p.ParkName.Equals(parkName, StringComparison.OrdinalIgnoreCase) ||
            p.Code.Equals(parkName, StringComparison.OrdinalIgnoreCase) ||
            parkName.Contains(p.ParkName, StringComparison.OrdinalIgnoreCase) ||
            p.ParkName.Contains(parkName, StringComparison.OrdinalIgnoreCase));
    }

    public static List<ParkProfileDto> GetDefaultParkProfiles()
    {
        return new List<ParkProfileDto>
        {
            new()
            {
                Id = 1,
                ParkName = "Amboseli National Park",
                Code = "AMBOSELI",
                County = "Kajiado County",
                AreaSqKm = 392.0,
                EcosystemType = "Savannah Plains, Acacia Woodland & Freshwater Swamps",
                EstablishedYear = 1974,
                RangerHq = "Ol Tukai Ranger Command Post",
                FenceType = "Partially Fenced (Southern Kimana Corridor Buffer)",
                ThreatLevel = "MODERATE - Human-Wildlife Conflict Buffer",
                KeyWaterholes = "Enkongo Narok Swamp, Ol Okenya Marsh, Lake Amboseli Basin, Observation Hill Waterhole",
                KeySpecies = "African Bush Elephant, Lion, Cheetah, Maasai Giraffe, Cape Buffalo, Spotted Hyena",
                Description = "Famous for being the best place in the world to get close to free-ranging elephants against the backdrop of Mount Kilimanjaro. Critical focus is the southern Kimana community corridor to prevent crop-raiding.",
                CenterLat = -2.65,
                CenterLng = 37.26,
                DefaultZoom = 11
            },
            new()
            {
                Id = 2,
                ParkName = "Tsavo East National Park",
                Code = "TSAVO_EAST",
                County = "Taita-Taveta, Kitui & Tana River Counties",
                AreaSqKm = 13747.0,
                EcosystemType = "Semi-Arid Bushland, Savannah & Riverine Galana Basin",
                EstablishedYear = 1948,
                RangerHq = "Voi Gate Headquarters & Lugard Falls Patrol Post",
                FenceType = "Open Dispersal (SGR Wildlife Underpasses & Highway Crossings)",
                ThreatLevel = "HIGH - Vast Area Poaching & SGR Dispersal Risk",
                KeyWaterholes = "Aruba Dam, Galana River Rapids, Mudanda Rock Water catchment, Lugard Falls Pool",
                KeySpecies = "Red Dust Elephants, Tsavo Maneless Lions, Black Rhino, Hirola, Lesser Kudu",
                Description = "One of the oldest and largest national parks in Kenya. Renowned for massive herds of dust-red elephants that roll in the volcanic soil, and vital monitoring of wildlife movement across the Standard Gauge Railway.",
                CenterLat = -2.77,
                CenterLng = 38.77,
                DefaultZoom = 10
            },
            new()
            {
                Id = 3,
                ParkName = "Tsavo West National Park",
                Code = "TSAVO_WEST",
                County = "Taita-Taveta County",
                AreaSqKm = 9065.0,
                EcosystemType = "Rugged Volcanic Ridges, Mzima Springs & Acacia Savannah",
                EstablishedYear = 1948,
                RangerHq = "Ngulia Rhino Sanctuary Command Post",
                FenceType = "Specialized Rhino Sanctuary Electric Grid + Open Park",
                ThreatLevel = "CRITICAL - Intensive Rhino Anti-Poaching Sanctuary",
                KeyWaterholes = "Mzima Springs Natural Oasis, Lake Jipe Wetlands, Ngulia Water Pan, Shetani Lava Pools",
                KeySpecies = "Eastern Black Rhino, African Leopard, Hippopotamus, Crocodile, Wild Dog",
                Description = "Characterized by rugged mountainous landscapes, the volcanic Shetani lava flow, and crystal-clear Mzima Springs. Features the heavily fortified Ngulia Rhino Sanctuary dedicated to endangered Eastern Black Rhinos.",
                CenterLat = -3.32,
                CenterLng = 38.00,
                DefaultZoom = 10
            },
            new()
            {
                Id = 4,
                ParkName = "Maasai Mara National Reserve",
                Code = "MAASAI_MARA",
                County = "Narok County",
                AreaSqKm = 1510.0,
                EcosystemType = "Rolling Grassland Savannah & Riverine Woodland",
                EstablishedYear = 1961,
                RangerHq = "Sekenani Gate & Mara Triangle HQ",
                FenceType = "Unfenced Greater Mara Ecosystem & Community Conservancies",
                ThreatLevel = "HIGH - Livestock Grazing Encroachment & Predator Conflict",
                KeyWaterholes = "Mara River Crossing Points, Talek River Junction, Sand River Basin, Musiara Marsh",
                KeySpecies = "Lions (Mara Predator Project), Cheetahs, Leopards, Elephants, Great Migration Wildebeest & Zebra",
                Description = "Globally renowned for exceptional predator densities and the Great Wildebeest Migration across the Mara River. Monitored for predator-livestock conflict along adjacent community conservancy borders.",
                CenterLat = -1.50,
                CenterLng = 35.15,
                DefaultZoom = 11
            },
            new()
            {
                Id = 5,
                ParkName = "Nairobi National Park",
                Code = "NAIROBI_NP",
                County = "Nairobi City / Kajiado County",
                AreaSqKm = 117.0,
                EcosystemType = "Open Grass Plains, Acacia Bush & Riverine Gorge",
                EstablishedYear = 1946,
                RangerHq = "KWS Central Command Headquarters & Ivory Burning Site Post",
                FenceType = "Electrified Northern/Eastern City Perimeter (Unfenced South Kitengela Corridor)",
                ThreatLevel = "HIGH - Urban Encroachment & Southern Kitengela Buffer Pressures",
                KeyWaterholes = "Nagolomon Dam, Hippo Pools (Mbagathi River), Athi Basin Waterholes, Hyena Dam",
                KeySpecies = "Eastern Black Rhino Sanctuary, Lion Pride, Leopard, Cheetah, Maasai Giraffe, Cape Buffalo",
                Description = "The only protected wildlife national park on Earth sharing a direct border with a capital metropolis. High-security sanctuary for Black Rhinos, requiring strict northern electric fence integrity.",
                CenterLat = -1.37,
                CenterLng = 36.86,
                DefaultZoom = 12
            },
            new()
            {
                Id = 6,
                ParkName = "Ol Pejeta Conservancy",
                Code = "OL_PEJETA",
                County = "Laikipia County",
                AreaSqKm = 364.0,
                EcosystemType = "High-Plateau Savannah, Ewaso Nyiro River Basin & Acacia Plains",
                EstablishedYear = 1988,
                RangerHq = "Morani Command Post & Sweetwaters Research Base",
                FenceType = "Smart Solar-Powered High-Security Perimeter Fence",
                ThreatLevel = "CRITICAL - Last Northern White Rhinos Sanctuary Protection",
                KeyWaterholes = "Ewaso Nyiro River Pools, Sweetwaters Dam, Morani Waterhole, Pelican Dam",
                KeySpecies = "Northern White Rhinos (Najin & Fatu), Black Rhinos, Grevy's Zebra, African Wild Dog, Chimpanzees",
                Description = "East Africa's largest Black Rhino sanctuary and home to the world's last two surviving Northern White Rhinos. Operates high-tech perimeter sensors, elite K9 anti-poaching units, and drone surveillance.",
                CenterLat = 0.03,
                CenterLng = 36.95,
                DefaultZoom = 12
            }
        };
    }
}