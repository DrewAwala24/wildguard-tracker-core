namespace frontend.Models;

/// <summary>
/// Represents ambient weather conditions for a specific Kenyan national park / conservancy.
/// </summary>
public class ParkWeatherCard
{
    public string Park      { get; set; } = string.Empty;
    public string Emoji     { get; set; } = "☀️";
    public int    TempC     { get; set; }
    public string Condition { get; set; } = string.Empty;
    public int    Humidity  { get; set; }
    public int    WindKmh   { get; set; }

    public string TempDisplay     => $"{TempC}°C";
    public string HumidityDisplay => $"💧 {Humidity}%";
    public string WindDisplay     => $"💨 {WindKmh} km/h";
    /// <summary>Short park label (first word only) to keep the card compact.</summary>
    public string ShortPark => Park.Split(' ')[0];
}
