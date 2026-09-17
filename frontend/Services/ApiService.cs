using System.Diagnostics;
using System.Net.Http.Json;
using System.Text.Json;
using frontend.Models;

namespace frontend.Services;

public class ApiService
{
    private readonly HttpClient _httpClient;
    private const string BaseUrl = "http://localhost:8080";

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
            // Nairobi National Park
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
                    new[] { -1.32, 36.80 },
                    new[] { -1.32, 36.92 },
                    new[] { -1.42, 36.92 },
                    new[] { -1.42, 36.80 },
                    new[] { -1.32, 36.80 }
                }
            },
            // Kitengela Dispersal Area (South of Nairobi NP)
            new()
            {
                Id = 4,
                ZoneName = "Kitengela Dispersal Zone",
                ZoneType = "COMMUNITY_BUFFER",
                CenterLatitude = -1.45,
                CenterLongitude = 36.88,
                ColorHex = "#FF5722",
                Coordinates = new List<double[]>
                {
                    new[] { -1.42, 36.82 },
                    new[] { -1.42, 36.95 },
                    new[] { -1.50, 36.95 },
                    new[] { -1.50, 36.82 },
                    new[] { -1.42, 36.82 }
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
                ZoneType = "PARK",
                CenterLatitude = 0.0389,
                CenterLongitude = 36.9639,
                ColorHex = "#4CAF50",
                Coordinates = new List<double[]>
                {
                    new[] { 0.00, 36.88 },
                    new[] { 0.00, 37.03 },
                    new[] { 0.08, 37.03 },
                    new[] { 0.08, 36.88 },
                    new[] { 0.00, 36.88 }
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
            new() { Id = 3, Name = "Mara Predator Rapid Response", CallSign = "PATROL-MAR-02", UnitType = "LAND_CRUISER", Latitude = -1.4500, Longitude = 35.1500, Status = "AVAILABLE", Sector = "Maasai Mara" },
            new() { Id = 4, Name = "KWS Airwing Recon Cessna", CallSign = "AIRWING-KWS-09", UnitType = "AIRWING", Latitude = -1.3650, Longitude = 36.8500, Status = "AVAILABLE", Sector = "Nairobi NP" }
        };
    }
}