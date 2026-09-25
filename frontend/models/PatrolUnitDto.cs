using System.Text.Json.Serialization;

namespace frontend.Models
{
    public class PatrolUnitDto
    {
        [JsonPropertyName("id")]
        public long Id { get; set; }

        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("callSign")]
        public string CallSign { get; set; } = string.Empty;

        [JsonPropertyName("unitType")]
        public string UnitType { get; set; } = "LAND_CRUISER";

        [JsonPropertyName("latitude")]
        public double Latitude { get; set; }

        [JsonPropertyName("longitude")]
        public double Longitude { get; set; }

        [JsonPropertyName("status")]
        public string Status { get; set; } = "AVAILABLE"; // AVAILABLE, DISPATCHED, ON_PATROL

        [JsonPropertyName("sector")]
        public string Sector { get; set; } = string.Empty;

        [JsonPropertyName("altitudeFt")]
        public int AltitudeFt { get; set; } = 0;

        [JsonPropertyName("speedKmh")]
        public double SpeedKmh { get; set; } = 45.0;
    }
}
