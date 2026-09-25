namespace frontend.Models;

/// <summary>
/// Represents a per-species conservation summary card shown in the dashboard.
/// </summary>
public class ConservationSpeciesCard
{
    public string Species   { get; set; } = string.Empty;
    public int    Count     { get; set; }
    public string IucnStatus{ get; set; } = "LC";   // CR, EN, VU, NT, LC
    public string IucnColor { get; set; } = "#4CAF50";
    public int    AvgBattery{ get; set; }
    public string Parks     { get; set; } = string.Empty;

    /// <summary>Human-readable IUCN full name for display.</summary>
    public string IucnLabel => IucnStatus switch
    {
        "CR" => "Critically Endangered",
        "EN" => "Endangered",
        "VU" => "Vulnerable",
        "NT" => "Near Threatened",
        _    => "Least Concern"
    };

    public string BatteryDisplay => $"🔋 {AvgBattery}%";
    public string CountDisplay   => $"{Count} Collared";
}
