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

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                Debug.WriteLine($"[ApiService] Failed to fetch animals. HTTP {(int)response.StatusCode} ({response.ReasonPhrase}): {errorContent}");
                return new List<AnimalDto>();
            }

            var animals = await response.Content.ReadFromJsonAsync<List<AnimalDto>>(JsonOptions);
            Debug.WriteLine($"[ApiService] Retrieved {animals?.Count ?? 0} animals successfully.");
            return animals ?? new List<AnimalDto>();
        }
        catch (HttpRequestException ex)
        {
            Debug.WriteLine($"[ApiService] Network/HTTP error fetching animals: {ex.Message}");
            return new List<AnimalDto>();
        }
        catch (JsonException ex)
        {
            Debug.WriteLine($"[ApiService] JSON deserialization error for animals: {ex.Message}");
            return new List<AnimalDto>();
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Unexpected exception in GetAnimalsAsync: {ex}");
            return new List<AnimalDto>();
        }
    }

    public async Task<List<TelemetryLocation>> GetTelemetryAsync()
    {
        try
        {
            Debug.WriteLine($"[ApiService] Fetching telemetry from {BaseUrl}/api/telemetry");
            var response = await _httpClient.GetAsync($"{BaseUrl}/api/telemetry");

            if (!response.IsSuccessStatusCode)
            {
                var errorContent = await response.Content.ReadAsStringAsync();
                Debug.WriteLine($"[ApiService] Failed to fetch telemetry. HTTP {(int)response.StatusCode} ({response.ReasonPhrase}): {errorContent}");
                return new List<TelemetryLocation>();
            }

            var telemetryList = await response.Content.ReadFromJsonAsync<List<TelemetryLocation>>(JsonOptions);
            if (telemetryList != null)
            {
                // Normalize nested entity or GeoJSON properties if backend returns JPA entities
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

            Debug.WriteLine($"[ApiService] Retrieved {telemetryList?.Count ?? 0} telemetry records successfully.");
            return telemetryList ?? new List<TelemetryLocation>();
        }
        catch (HttpRequestException ex)
        {
            Debug.WriteLine($"[ApiService] Network/HTTP error fetching telemetry: {ex.Message}");
            return new List<TelemetryLocation>();
        }
        catch (JsonException ex)
        {
            Debug.WriteLine($"[ApiService] JSON deserialization error for telemetry: {ex.Message}");
            return new List<TelemetryLocation>();
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[ApiService] Unexpected exception in GetTelemetryAsync: {ex}");
            return new List<TelemetryLocation>();
        }
    }
}