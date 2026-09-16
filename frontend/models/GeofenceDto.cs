using System.Text.Json.Serialization;

namespace frontend.Models;

public class GeofenceDto
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("zoneName")]
    public string ZoneName { get; set; } = string.Empty;

    [JsonPropertyName("zoneType")]
    public string ZoneType { get; set; } = "PARK"; // PARK, COMMUNITY_BUFFER, PRIVATE_FARM

    [JsonPropertyName("centerLatitude")]
    public double CenterLatitude { get; set; }

    [JsonPropertyName("centerLongitude")]
    public double CenterLongitude { get; set; }

    [JsonPropertyName("coordinates")]
    public List<double[]> Coordinates { get; set; } = new();

    [JsonPropertyName("colorHex")]
    public string ColorHex { get; set; } = "#4CAF50"; // Green for park, Amber for buffer, Red for farm
}
