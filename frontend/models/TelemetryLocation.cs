namespace frontend.Models;

public class TelemetryLocation
{
    public long Id { get; set; }
    public string AnimalId { get; set; }
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public DateTime Timestamp { get; set; }
}