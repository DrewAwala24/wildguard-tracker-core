using System.Text.Json.Serialization;

namespace frontend.Models;

public class ParkProfileDto
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("parkName")]
    public string ParkName { get; set; } = string.Empty;

    [JsonPropertyName("code")]
    public string Code { get; set; } = string.Empty;

    [JsonPropertyName("county")]
    public string County { get; set; } = string.Empty;

    [JsonPropertyName("areaSqKm")]
    public double AreaSqKm { get; set; }

    [JsonPropertyName("ecosystemType")]
    public string EcosystemType { get; set; } = string.Empty;

    [JsonPropertyName("establishedYear")]
    public int EstablishedYear { get; set; }

    [JsonPropertyName("rangerHq")]
    public string RangerHq { get; set; } = string.Empty;

    [JsonPropertyName("fenceType")]
    public string FenceType { get; set; } = string.Empty;

    [JsonPropertyName("threatLevel")]
    public string ThreatLevel { get; set; } = string.Empty;

    [JsonPropertyName("keyWaterholes")]
    public string KeyWaterholes { get; set; } = string.Empty;

    [JsonPropertyName("keySpecies")]
    public string KeySpecies { get; set; } = string.Empty;

    [JsonPropertyName("description")]
    public string Description { get; set; } = string.Empty;

    [JsonPropertyName("centerLat")]
    public double CenterLat { get; set; }

    [JsonPropertyName("centerLng")]
    public double CenterLng { get; set; }

    [JsonPropertyName("defaultZoom")]
    public int DefaultZoom { get; set; } = 11;
}
