using System.Text.Json.Serialization;

namespace frontend.Models
{
    public class AnimalDto
    {
        [JsonPropertyName("id")]
        public long Id { get; set; }

        [JsonPropertyName("name")]
        public string Name { get; set; } = string.Empty;

        [JsonPropertyName("species")]
        public string Species { get; set; } = string.Empty;

        [JsonPropertyName("collarId")]
        public string CollarId { get; set; } = string.Empty;

        [JsonPropertyName("parkName")]
        public string ParkName { get; set; } = "Tsavo East NP";

        [JsonPropertyName("sex")]
        public string Sex { get; set; } = "Unknown";

        [JsonPropertyName("collarBattery")]
        public int CollarBattery { get; set; } = 95;

        [JsonPropertyName("latitude")]
        public double Latitude { get; set; }

        [JsonPropertyName("longitude")]
        public double Longitude { get; set; }

        [JsonPropertyName("status")]
        public string Status { get; set; } = "Active";

        [JsonPropertyName("isBreaching")]
        public bool IsBreaching { get; set; } = false;
    }
}