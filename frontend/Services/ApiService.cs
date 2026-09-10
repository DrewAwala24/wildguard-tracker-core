using System.Net.Http.Json;
using frontend.Models;

namespace frontend.Services;

public class ApiService
{
    private readonly HttpClient _httpClient;
    private const string BaseUrl = "http://10.0.2.2:8080"; // Use "http://localhost:8080" if testing on Windows machine directly

    public ApiService(HttpClient httpClient)
    {
        _httpClient = httpClient;
    }

    public async Task<List<AnimalDto>> GetAnimalsAsync()
    {
        try
        {
            return await _httpClient.GetFromJsonAsync<List<AnimalDto>>($"{BaseUrl}/api/animals");
        }
        catch
        {
            return new List<AnimalDto>();
        }
    }

    public async Task<List<TelemetryLocation>> GetTelemetryAsync()
    {
        try
        {
            return await _httpClient.GetFromJsonAsync<List<TelemetryLocation>>($"{BaseUrl}/api/telemetry");
        }
        catch
        {
            return new List<TelemetryLocation>();
        }
    }
}