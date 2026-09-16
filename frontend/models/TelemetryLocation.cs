using System.Text.Json;
using System.Text.Json.Serialization;

namespace frontend.Models;

public class TelemetryLocation
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("animalId")]
    public string? AnimalId { get; set; }

    [JsonPropertyName("collarId")]
    public string? CollarId { get; set; }

    [JsonPropertyName("animal")]
    public AnimalDto? Animal { get; set; }

    [JsonPropertyName("latitude")]
    public double Latitude { get; set; }

    [JsonPropertyName("longitude")]
    public double Longitude { get; set; }

    [JsonPropertyName("location")]
    public JsonElement? RawLocation { get; set; }

    [JsonPropertyName("timestamp")]
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;

    // Display helper property for UI bindings
    public string DisplayAnimalId =>
        !string.IsNullOrEmpty(AnimalId) ? AnimalId :
        (!string.IsNullOrEmpty(Animal?.CollarId) ? Animal.CollarId :
        (Animal?.Id.ToString() ?? Id.ToString()));
}