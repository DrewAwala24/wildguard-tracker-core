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

        [JsonPropertyName("latitude")]
        public double Latitude { get; set; }

        [JsonPropertyName("longitude")]
        public double Longitude { get; set; }

        [JsonPropertyName("status")]
        public string Status { get; set; } = string.Empty;
    }
}