using System.Text.Json;
using System.Text.Json.Serialization;

namespace frontend.Models;

public class GeofenceDto
{
    [JsonPropertyName("id")]
    public long Id { get; set; }

    [JsonPropertyName("zoneName")]
    public string ZoneName { get; set; } = string.Empty;

    [JsonPropertyName("zoneType")]
    public string ZoneType { get; set; } = "PARK"; // PARK, CONSERVANCY, COMMUNITY_BUFFER, PRIVATE_FARM

    [JsonPropertyName("centerLatitude")]
    public double CenterLatitude { get; set; }

    [JsonPropertyName("centerLongitude")]
    public double CenterLongitude { get; set; }

    [JsonPropertyName("coordinates")]
    public List<double[]> Coordinates { get; set; } = new();

    [JsonPropertyName("colorHex")]
    public string ColorHex { get; set; } = "#4CAF50"; // Green for park, Amber for buffer, Teal for conservancy

    [JsonPropertyName("boundary")]
    public JsonElement? BoundaryJson { get; set; }

    // Converts backend PostGIS GeoJSON [lng, lat] to Leaflet [lat, lng]
    public void NormalizeCoordinates()
    {
        if (Coordinates.Count == 0 && BoundaryJson.HasValue && BoundaryJson.Value.ValueKind == JsonValueKind.Object)
        {
            try
            {
                if (BoundaryJson.Value.TryGetProperty("coordinates", out var coordsProp) &&
                    coordsProp.ValueKind == JsonValueKind.Array)
                {
                    var outerRing = coordsProp.EnumerateArray().FirstOrDefault();
                    if (outerRing.ValueKind == JsonValueKind.Array)
                    {
                        foreach (var pt in outerRing.EnumerateArray())
                        {
                            if (pt.ValueKind == JsonValueKind.Array)
                            {
                                var arr = pt.EnumerateArray().ToArray();
                                if (arr.Length >= 2)
                                {
                                    double lng = arr[0].GetDouble();
                                    double lat = arr[1].GetDouble();
                                    // GeoJSON is [lng, lat]; Leaflet expects [lat, lng]
                                    Coordinates.Add(new[] { lat, lng });
                                }
                            }
                        }
                    }
                }
            }
            catch
            {
                // Fallback gracefully
            }
        }

        // Set color according to zone type
        if (string.Equals(ZoneType, "COMMUNITY_BUFFER", StringComparison.OrdinalIgnoreCase))
        {
            ColorHex = "#FF9800";
        }
        else if (string.Equals(ZoneType, "CONSERVANCY", StringComparison.OrdinalIgnoreCase))
        {
            ColorHex = "#00E5FF";
        }
        else
        {
            ColorHex = "#4CAF50";
        }

        // Auto-compute center if not set
        if ((CenterLatitude == 0 || CenterLongitude == 0) && Coordinates.Count > 0)
        {
            CenterLatitude = Coordinates.Average(c => c[0]);
            CenterLongitude = Coordinates.Average(c => c[1]);
        }
    }
}
