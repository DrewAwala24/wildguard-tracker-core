using System.Text.Json.Serialization;

namespace frontend.Models;

public class IncidentAlertDto
{
    [JsonPropertyName("id")]
    public string Id { get; set; } = Guid.NewGuid().ToString()[..8];

    [JsonPropertyName("animalName")]
    public string AnimalName { get; set; } = string.Empty;

    [JsonPropertyName("species")]
    public string Species { get; set; } = string.Empty;

    [JsonPropertyName("collarId")]
    public string CollarId { get; set; } = string.Empty;

    [JsonPropertyName("parkName")]
    public string ParkName { get; set; } = string.Empty;

    [JsonPropertyName("zoneName")]
    public string ZoneName { get; set; } = string.Empty;

    [JsonPropertyName("severity")]
    public string Severity { get; set; } = "CRITICAL"; // CRITICAL, WARNING, ADVISORY

    [JsonPropertyName("message")]
    public string Message { get; set; } = string.Empty;

    [JsonPropertyName("latitude")]
    public double Latitude { get; set; }

    [JsonPropertyName("longitude")]
    public double Longitude { get; set; }

    [JsonPropertyName("timestamp")]
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;

    [JsonPropertyName("isDispatched")]
    public bool IsDispatched { get; set; } = false;

    [JsonPropertyName("dispatchedPatrolUnit")]
    public string DispatchedPatrolUnit { get; set; } = string.Empty;
}
