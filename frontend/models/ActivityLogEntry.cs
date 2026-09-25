namespace frontend.Models;

/// <summary>
/// A single entry in the real-time activity/event log panel.
/// </summary>
public class ActivityLogEntry
{
    public string Emoji     { get; set; } = "📡";
    public string Title     { get; set; } = string.Empty;
    public string Detail    { get; set; } = string.Empty;
    public string TimeStamp { get; set; } = string.Empty;
    public string TagColor  { get; set; } = "#00E5FF";   // colour of the left accent bar
    public string Tag       { get; set; } = "TELEMETRY"; // TELEMETRY | BREACH | DISPATCH | RESOLVE | SIMULATION
}
