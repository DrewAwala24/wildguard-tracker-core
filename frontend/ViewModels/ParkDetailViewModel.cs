using System.Collections.ObjectModel;
using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Windows.Input;
using frontend.Models;
using frontend.Services;

namespace frontend.ViewModels;

public class ParkDetailViewModel : INotifyPropertyChanged
{
    private readonly ApiService _apiService;
    private ParkProfileDto _profile = new();
    private string _weatherDisplay = "28°C 🌤️ Clear • Humidity: 42% • Wind: 14 km/h";
    private string _residentWildlifeCount = "0 Animals";
    private string _activePatrolsCount = "0 Units";
    private string _htmlMapContent = string.Empty;
    private bool _isBusy;

    public event PropertyChangedEventHandler? PropertyChanged;

    public ObservableCollection<AnimalDto> ResidentAnimals { get; } = new();
    public ObservableCollection<PatrolUnitDto> AssignedPatrols { get; } = new();

    public ParkProfileDto Profile
    {
        get => _profile;
        set
        {
            if (_profile != value)
            {
                _profile = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(ThreatColor));
            }
        }
    }

    public string WeatherDisplay
    {
        get => _weatherDisplay;
        set { _weatherDisplay = value; OnPropertyChanged(); }
    }

    public string ResidentWildlifeCount
    {
        get => _residentWildlifeCount;
        set { _residentWildlifeCount = value; OnPropertyChanged(); }
    }

    public string ActivePatrolsCount
    {
        get => _activePatrolsCount;
        set { _activePatrolsCount = value; OnPropertyChanged(); }
    }

    public string HtmlMapContent
    {
        get => _htmlMapContent;
        set { _htmlMapContent = value; OnPropertyChanged(); }
    }

    public bool IsBusy
    {
        get => _isBusy;
        set { _isBusy = value; OnPropertyChanged(); }
    }

    public Color ThreatColor
    {
        get
        {
            if (_profile.ThreatLevel.StartsWith("CRITICAL", StringComparison.OrdinalIgnoreCase))
                return Color.FromArgb("#FF5252");
            if (_profile.ThreatLevel.StartsWith("HIGH", StringComparison.OrdinalIgnoreCase))
                return Color.FromArgb("#FF9800");
            return Color.FromArgb("#FFD700");
        }
    }

    public ICommand GoBackCommand { get; }
    public ICommand RefreshSectorCommand { get; }
    public ICommand DispatchSectorPatrolCommand { get; }

    public ParkDetailViewModel(ApiService apiService)
    {
        _apiService = apiService;
        GoBackCommand = new Command(async () => await NavigateBackAsync());
        RefreshSectorCommand = new Command(async () => await RefreshAsync());
        DispatchSectorPatrolCommand = new Command<PatrolUnitDto>(async unit => await DispatchPatrolAsync(unit));
    }

    public async Task InitializeAsync(string parkName, IEnumerable<AnimalDto>? currentAnimals = null, IEnumerable<PatrolUnitDto>? currentPatrols = null)
    {
        IsBusy = true;
        try
        {
            var profile = await _apiService.GetParkProfileByNameAsync(parkName);
            if (profile != null)
            {
                Profile = profile;
            }
            else
            {
                Profile = new ParkProfileDto
                {
                    ParkName = parkName,
                    Code = parkName.ToUpper().Replace(" ", "_"),
                    County = "Protected Kenyan Ecosystem",
                    AreaSqKm = 500,
                    EcosystemType = "Savannah & Woodland Habitat",
                    EstablishedYear = 1970,
                    RangerHq = "Regional KWS Sector Command",
                    FenceType = "Managed Boundary Buffer",
                    ThreatLevel = "MODERATE - Active Monitoring",
                    KeyWaterholes = "Sector Natural Waterholes & River Basin",
                    KeySpecies = "Elephants, Lions, Rhinos, Buffalo, Giraffes",
                    Description = $"Official Kenya Wildlife Service operational sector for {parkName}.",
                    CenterLat = -1.5,
                    CenterLng = 37.0,
                    DefaultZoom = 11
                };
            }

            // Populate resident animals
            ResidentAnimals.Clear();
            List<AnimalDto> allAnimals;
            if (currentAnimals != null && currentAnimals.Any())
            {
                allAnimals = currentAnimals.ToList();
            }
            else
            {
                allAnimals = await _apiService.GetAnimalsAsync();
            }

            var filteredAnimals = allAnimals
                .Where(a => MatchesPark(a.ParkName, Profile.ParkName))
                .ToList();

            foreach (var animal in filteredAnimals)
            {
                ResidentAnimals.Add(animal);
            }
            ResidentWildlifeCount = $"{ResidentAnimals.Count} Monitored Individuals";

            // Populate assigned patrols
            AssignedPatrols.Clear();
            List<PatrolUnitDto> allPatrols;
            if (currentPatrols != null && currentPatrols.Any())
            {
                allPatrols = currentPatrols.ToList();
            }
            else
            {
                allPatrols = ApiService.GetDefaultPatrolUnits();
            }

            var filteredPatrols = allPatrols
                .Where(p => MatchesPark(p.Sector, Profile.ParkName))
                .ToList();

            if (!filteredPatrols.Any())
            {
                // Assign a dedicated standby unit if none explicitly stationed
                filteredPatrols.Add(new PatrolUnitDto
                {
                    Id = 99,
                    Name = $"{Profile.ParkName} Rapid Response Cruiser",
                    CallSign = $"PATROL-{Profile.Code.Substring(0, Math.Min(3, Profile.Code.Length))}-01",
                    UnitType = "LAND_CRUISER",
                    Latitude = Profile.CenterLat,
                    Longitude = Profile.CenterLng,
                    Status = "AVAILABLE",
                    Sector = Profile.ParkName
                });
            }

            foreach (var p in filteredPatrols)
            {
                AssignedPatrols.Add(p);
            }
            ActivePatrolsCount = $"{AssignedPatrols.Count} Active Units";

            // Generate Weather
            WeatherDisplay = GenerateWeatherForPark(Profile.ParkName);

            // Generate Sector Map HTML
            BuildSectorMap();
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"[ParkDetailViewModel] Error initializing: {ex.Message}");
        }
        finally
        {
            IsBusy = false;
        }
    }

    private void BuildSectorMap()
    {
        var geofences = ApiService.GetVerifiedKenyaGeofences()
            .Where(g => MatchesPark(g.ZoneName, Profile.ParkName))
            .ToList();

        if (!geofences.Any())
        {
            geofences = ApiService.GetVerifiedKenyaGeofences();
        }

        HtmlMapContent = OpsMapHtmlBuilder.Build(
            Profile.CenterLat,
            Profile.CenterLng,
            Profile.DefaultZoom,
            ResidentAnimals.ToList(),
            geofences,
            AssignedPatrols.ToList(),
            new List<IncidentAlertDto>(),
            new List<TelemetryTrailDto>()
        );
    }

    private async Task DispatchPatrolAsync(PatrolUnitDto? unit)
    {
        if (unit == null) return;
        unit.Status = "DISPATCHED";
        OnPropertyChanged(nameof(AssignedPatrols));
        if (Application.Current?.Windows.FirstOrDefault()?.Page is Page page)
        {
            await page.DisplayAlertAsync(
                "🚨 Rapid Patrol Dispatched",
                $"Unit {unit.CallSign} ({unit.Name}) has been tasked with high-priority sector reconnaissance in {Profile.ParkName}.",
                "Acknowledged");
        }
    }

    private async Task RefreshAsync()
    {
        await InitializeAsync(Profile.ParkName);
    }

    private async Task NavigateBackAsync()
    {
        if (Application.Current?.Windows.FirstOrDefault()?.Page is NavigationPage navPage)
        {
            await navPage.PopAsync();
        }
        else if (Shell.Current != null)
        {
            await Shell.Current.GoToAsync("..");
        }
    }

    private static bool MatchesPark(string source, string target)
    {
        if (string.IsNullOrWhiteSpace(source) || string.IsNullOrWhiteSpace(target)) return false;
        var s = source.ToLowerInvariant().Replace("national park", "").Replace("national reserve", "").Replace("conservancy", "").Trim();
        var t = target.ToLowerInvariant().Replace("national park", "").Replace("national reserve", "").Replace("conservancy", "").Trim();
        return s.Contains(t) || t.Contains(s);
    }

    private static string GenerateWeatherForPark(string parkName)
    {
        var p = parkName.ToLowerInvariant();
        if (p.Contains("amboseli")) return "29°C 🌤️ Warm & Clear • Humidity: 38% • Wind: 16 km/h NE • Kilimanjaro Visible";
        if (p.Contains("tsavo east")) return "32°C ☀️ Hot & Dry • Humidity: 30% • Wind: 20 km/h E • Galana River Flowing";
        if (p.Contains("tsavo west")) return "28°C ⛅ Broken Clouds • Humidity: 46% • Wind: 12 km/h SE • Mzima Springs Clear";
        if (p.Contains("mara")) return "24°C 🌦️ Afternoon Shower Risk • Humidity: 62% • Wind: 10 km/h SW • River Crossings Active";
        if (p.Contains("nairobi")) return "22°C ⛅ Mild Savannah Breeze • Humidity: 54% • Wind: 15 km/h E • City Horizon Clear";
        if (p.Contains("ol pejeta")) return "21°C 🌤️ Cool Plateau Breeze • Humidity: 50% • Wind: 14 km/h N • Ewaso Nyiro River Stable";
        return "26°C 🌤️ Clear Conditions • Humidity: 45% • Wind: 14 km/h";
    }

    protected void OnPropertyChanged([CallerMemberName] string? propertyName = null)
    {
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}
