using System.Collections.ObjectModel;
using System.Diagnostics;
using System.Text.Json;
using System.Windows.Input;
using frontend.Models;
using frontend.Services;

namespace frontend.ViewModels;

public class MainViewModel : BindableObject
{
    private readonly ApiService _apiService;
    private readonly Random _movementRng = new();
    private readonly Dictionary<string, double> _movementHeadings = new(StringComparer.OrdinalIgnoreCase);
    private IDispatcherTimer? _liveMovementTimer;
    private int _liveMovementTickGate;
    private bool _isRefreshing;
    private string _activeAnimalsCount = "0 Tracked";
    private string _telemetryLogsCount = "0 Recorded";
    private string _selectedPark = "All Kenya";
    private string _selectedSpecies = "All Species";
    private bool _hasActiveAlerts;
    private string _activeAlertsSummary = string.Empty;
    private bool _isRegistrationModalOpen;

    // Conservation & Analytics
    private string _conservationSummary = string.Empty;
    private string _collarHealthSummary = string.Empty;
    private string _alertsCountDisplay = "0 Active";
    private string _patrolsCountDisplay = "0 Units";
    private string _parksCountDisplay = "0 Parks";

    // Resolve Modal Bindings
    private bool _isResolveModalOpen;
    private string _resolutionNotes = string.Empty;
    private IncidentAlertDto? _currentResolvingIncident;

    // Toast Notification for SMS Broadcast
    private string _smsNotificationToast = string.Empty;
    private bool _showSmsToast;

    // Registration Form Bindings
    private string _newAnimalName = string.Empty;
    private string _newAnimalSpecies = "African Elephant";
    private string _newCollarId = string.Empty;
    private string _newParkName = "Amboseli NP";
    private string _newAnimalSex = "Female";

    // Selected Animal for Breadcrumb Trails
    private AnimalDto? _selectedAnimal;
    public List<TelemetryTrailDto> SelectedTrail { get; set; } = new();

    // Map HTML Content for WebView
    private string _mapHtml = string.Empty;
    private readonly HtmlWebViewSource _mapHtmlSource = new();
    private string _liveFeedStatus = "LIVE collar feed startingâ€¦";

    // Source master lists
    public List<AnimalDto> MasterAnimals { get; } = new();
    public List<TelemetryLocation> MasterTelemetry { get; } = new();
    public List<GeofenceDto> Geofences { get; } = new();

    // Observable UI collections
    public ObservableCollection<AnimalDto> FilteredAnimals { get; } = new();
    public ObservableCollection<TelemetryLocation> FilteredTelemetryLogs { get; } = new();
    public ObservableCollection<IncidentAlertDto> ActiveIncidents { get; } = new();
    public ObservableCollection<PatrolUnitDto> PatrolUnits { get; } = new();
    public ObservableCollection<ConservationSpeciesCard> ConservationCards { get; } = new();
    public ObservableCollection<ParkWeatherCard> ParkWeatherCards { get; } = new();
    public ObservableCollection<ActivityLogEntry> ActivityLog { get; } = new();

    // Commands
    public ICommand LoadDataCommand { get; }
    public ICommand SelectParkCommand { get; }
    public ICommand SelectSpeciesCommand { get; }
    public ICommand DispatchPatrolCommand { get; }
    public ICommand BroadcastSmsCommand { get; }
    public ICommand OpenResolveModalCommand { get; }
    public ICommand CloseResolveModalCommand { get; }
    public ICommand SubmitResolveCommand { get; }
    public ICommand SelectAnimalTrailCommand { get; }
    public ICommand OpenRegistrationCommand { get; }
    public ICommand CloseRegistrationCommand { get; }
    public ICommand SubmitRegistrationCommand { get; }
    public ICommand ToggleSimulationPlaybackCommand { get; }
    public ICommand SetSimulationSpeedCommand { get; }
    public ICommand ToggleNightOpsModeCommand { get; }

    // Set by MainPage code-behind after construction (opens detail panel / JS inject)
    public ICommand? SelectAnimalDetailCommand { get; set; }
    public Func<string, Task>? InjectLiveMapScript { get; set; }

    private bool _isNightOpsMode;
    public bool IsNightOpsMode
    {
        get => _isNightOpsMode;
        set
        {
            if (_isNightOpsMode != value)
            {
                _isNightOpsMode = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(NightOpsButtonText));
                OnPropertyChanged(nameof(NightOpsBadgeText));
            }
        }
    }

    public string NightOpsButtonText => IsNightOpsMode ? "☀️ Day Ops" : "🌙 Night Ops";
    public string NightOpsBadgeText => IsNightOpsMode ? "🌙 NIGHT RADAR ACTIVE" : "☀️ DAYLIGHT OPS";

    private bool _isSimulationPlaying = true;
    public bool IsSimulationPlaying
    {
        get => _isSimulationPlaying;
        set
        {
            if (_isSimulationPlaying != value)
            {
                _isSimulationPlaying = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(SimulationPlaybackButtonText));
            }
        }
    }

    public string SimulationPlaybackButtonText => IsSimulationPlaying ? "⏸ Pause Tracking" : "▶ Resume Tracking";

    private double _simulationSpeedMultiplier = 1.0;
    public double SimulationSpeedMultiplier
    {
        get => _simulationSpeedMultiplier;
        set
        {
            if (Math.Abs(_simulationSpeedMultiplier - value) > 0.01)
            {
                _simulationSpeedMultiplier = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(SimulationSpeedLabel));
            }
        }
    }

    public string SimulationSpeedLabel => $"{SimulationSpeedMultiplier:F0}x Speed";
    public string SimulationModeBadge => "🛡️ Park Fences Active • Nairobi City Safe";

    public bool IsRefreshing
    {
        get => _isRefreshing;
        set
        {
            if (_isRefreshing != value)
            {
                _isRefreshing = value;
                OnPropertyChanged();
                ((Command)LoadDataCommand).ChangeCanExecute();
            }
        }
    }

    public string ActiveAnimalsCount
    {
        get => _activeAnimalsCount;
        set { if (_activeAnimalsCount != value) { _activeAnimalsCount = value; OnPropertyChanged(); } }
    }

    public string TelemetryLogsCount
    {
        get => _telemetryLogsCount;
        set { if (_telemetryLogsCount != value) { _telemetryLogsCount = value; OnPropertyChanged(); } }
    }

    public string SelectedPark
    {
        get => _selectedPark;
        set
        {
            if (_selectedPark != value)
            {
                _selectedPark = value;
                OnPropertyChanged();
                ApplyFilter();
                UpdateMapHtml();
            }
        }
    }

    public string SelectedSpecies
    {
        get => _selectedSpecies;
        set
        {
            if (_selectedSpecies != value)
            {
                _selectedSpecies = value;
                OnPropertyChanged();
                ApplyFilter();
                UpdateMapHtml();
            }
        }
    }

    public bool HasActiveAlerts
    {
        get => _hasActiveAlerts;
        set { if (_hasActiveAlerts != value) { _hasActiveAlerts = value; OnPropertyChanged(); } }
    }

    public string ActiveAlertsSummary
    {
        get => _activeAlertsSummary;
        set { if (_activeAlertsSummary != value) { _activeAlertsSummary = value; OnPropertyChanged(); } }
    }

    public bool IsRegistrationModalOpen
    {
        get => _isRegistrationModalOpen;
        set { if (_isRegistrationModalOpen != value) { _isRegistrationModalOpen = value; OnPropertyChanged(); } }
    }

    public bool IsResolveModalOpen
    {
        get => _isResolveModalOpen;
        set { if (_isResolveModalOpen != value) { _isResolveModalOpen = value; OnPropertyChanged(); } }
    }

    public string ResolutionNotes
    {
        get => _resolutionNotes;
        set { if (_resolutionNotes != value) { _resolutionNotes = value; OnPropertyChanged(); } }
    }

    public string SmsNotificationToast
    {
        get => _smsNotificationToast;
        set { if (_smsNotificationToast != value) { _smsNotificationToast = value; OnPropertyChanged(); } }
    }

    public bool ShowSmsToast
    {
        get => _showSmsToast;
        set { if (_showSmsToast != value) { _showSmsToast = value; OnPropertyChanged(); } }
    }

    public AnimalDto? SelectedAnimal
    {
        get => _selectedAnimal;
        set { if (_selectedAnimal != value) { _selectedAnimal = value; OnPropertyChanged(); } }
    }

    public string NewAnimalName
    {
        get => _newAnimalName;
        set { if (_newAnimalName != value) { _newAnimalName = value; OnPropertyChanged(); } }
    }

    public string NewAnimalSpecies
    {
        get => _newAnimalSpecies;
        set { if (_newAnimalSpecies != value) { _newAnimalSpecies = value; OnPropertyChanged(); } }
    }

    public string NewCollarId
    {
        get => _newCollarId;
        set { if (_newCollarId != value) { _newCollarId = value; OnPropertyChanged(); } }
    }

    public string NewParkName
    {
        get => _newParkName;
        set { if (_newParkName != value) { _newParkName = value; OnPropertyChanged(); } }
    }

    public string NewAnimalSex
    {
        get => _newAnimalSex;
        set { if (_newAnimalSex != value) { _newAnimalSex = value; OnPropertyChanged(); } }
    }

    public string LiveFeedStatus
    {
        get => _liveFeedStatus;
        set { if (_liveFeedStatus != value) { _liveFeedStatus = value; OnPropertyChanged(); } }
    }

    public string ConservationSummary
    {
        get => _conservationSummary;
        set { if (_conservationSummary != value) { _conservationSummary = value; OnPropertyChanged(); } }
    }

    public string CollarHealthSummary
    {
        get => _collarHealthSummary;
        set { if (_collarHealthSummary != value) { _collarHealthSummary = value; OnPropertyChanged(); } }
    }

    public string AlertsCountDisplay
    {
        get => _alertsCountDisplay;
        set { if (_alertsCountDisplay != value) { _alertsCountDisplay = value; OnPropertyChanged(); } }
    }

    public string PatrolsCountDisplay
    {
        get => _patrolsCountDisplay;
        set { if (_patrolsCountDisplay != value) { _patrolsCountDisplay = value; OnPropertyChanged(); } }
    }

    public string ParksCountDisplay
    {
        get => _parksCountDisplay;
        set { if (_parksCountDisplay != value) { _parksCountDisplay = value; OnPropertyChanged(); } }
    }

    public string MapHtml
    {
        get => _mapHtml;
        set
        {
            if (_mapHtml != value)
            {
                _mapHtml = value;
                _mapHtmlSource.Html = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(MapHtmlSource));
            }
        }
    }

    public HtmlWebViewSource MapHtmlSource => _mapHtmlSource;

    public ICommand OpenParkDetailCommand { get; }

    public MainViewModel(ApiService apiService)
    {
        _apiService = apiService ?? throw new ArgumentNullException(nameof(apiService));

        LoadDataCommand = new Command(async () => await LoadDataAsync(), () => !IsRefreshing);
        SelectParkCommand = new Command<string>(park => SelectedPark = park);
        SelectSpeciesCommand = new Command<string>(species => SelectedSpecies = species);
        OpenParkDetailCommand = new Command<string>(async park => await OpenParkDetailPageAsync(park));
        DispatchPatrolCommand = new Command<IncidentAlertDto>(async alert => await DispatchPatrolAsync(alert));
        BroadcastSmsCommand = new Command<IncidentAlertDto>(async alert => await BroadcastCommunitySmsAsync(alert));
        OpenResolveModalCommand = new Command<IncidentAlertDto>(OpenResolveModal);
        CloseResolveModalCommand = new Command(() => IsResolveModalOpen = false);
        SubmitResolveCommand = new Command(async () => await SubmitResolveAsync());
        SelectAnimalTrailCommand = new Command<AnimalDto>(async animal => await SelectAnimalTrailAsync(animal));

        OpenRegistrationCommand = new Command(() =>
        {
            NewAnimalName = string.Empty;
            NewAnimalSpecies = "African Elephant";
            NewCollarId = $"KWS-COLLAR-{DateTime.UtcNow:fff}";
            NewParkName = SelectedPark != "All Kenya" ? SelectedPark : "Amboseli NP";
            NewAnimalSex = "Female";
            IsRegistrationModalOpen = true;
        });

        CloseRegistrationCommand = new Command(() => IsRegistrationModalOpen = false);
        SubmitRegistrationCommand = new Command(async () => await SubmitRegistrationAsync());

        ToggleSimulationPlaybackCommand = new Command(() => IsSimulationPlaying = !IsSimulationPlaying);
        SetSimulationSpeedCommand = new Command<string>(speedStr =>
        {
            if (double.TryParse(speedStr, System.Globalization.NumberStyles.Any, System.Globalization.CultureInfo.InvariantCulture, out var mult))
            {
                SimulationSpeedMultiplier = mult;
            }
        });

        ToggleNightOpsModeCommand = new Command(async () =>
        {
            IsNightOpsMode = !IsNightOpsMode;
            if (InjectLiveMapScript != null)
            {
                await InjectLiveMapScript($"setTacticalNightOps({(IsNightOpsMode ? "true" : "false")})");
            }
        });

        MainThread.BeginInvokeOnMainThread(StartLiveMovementSimulation);
    }

    public async Task OpenParkDetailPageAsync(string? parkName)
    {
        if (string.IsNullOrWhiteSpace(parkName) || parkName.Equals("All Kenya", StringComparison.OrdinalIgnoreCase))
        {
            parkName = !string.IsNullOrWhiteSpace(SelectedPark) && !SelectedPark.Equals("All Kenya", StringComparison.OrdinalIgnoreCase)
                ? SelectedPark
                : "Amboseli National Park";
        }

        try
        {
            var detailVm = new ParkDetailViewModel(_apiService);
            var detailPage = new ParkDetailPage(detailVm);
            await detailPage.LoadParkAsync(parkName, MasterAnimals, PatrolUnits);

            if (Application.Current?.Windows.FirstOrDefault()?.Page is NavigationPage navPage)
            {
                await navPage.PushAsync(detailPage);
            }
            else if (Shell.Current != null)
            {
                await Shell.Current.Navigation.PushAsync(detailPage);
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"[MainViewModel] Failed to open ParkDetailPage: {ex.Message}");
        }
    }

    public async Task LoadDataAsync()
    {
        if (IsRefreshing) return;
        IsRefreshing = true;

        try
        {
            Debug.WriteLine("[MainViewModel] Loading Kenya wildlife operations data...");

            var animalsTask = _apiService.GetAnimalsAsync();
            var telemetryTask = _apiService.GetTelemetryAsync();
            var geofencesTask = _apiService.GetGeofencesAsync();
            var patrolsTask = _apiService.GetPatrolUnitsAsync();

            await Task.WhenAll(animalsTask, telemetryTask, geofencesTask, patrolsTask);

            var animals = await animalsTask;
            var telemetry = await telemetryTask;
            var geofences = await geofencesTask;
            var patrols = await patrolsTask;

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                MasterAnimals.Clear();
                if (animals != null) MasterAnimals.AddRange(animals);

                MasterTelemetry.Clear();
                if (telemetry != null) MasterTelemetry.AddRange(telemetry);

                Geofences.Clear();
                if (geofences != null) Geofences.AddRange(geofences);

                PatrolUnits.Clear();
                if (patrols != null)
                {
                    foreach (var p in patrols) PatrolUnits.Add(p);
                }

                EvaluateGeofenceBreaches();
                ApplyFilter();
                UpdateMapHtml();
                RefreshAnalytics();

                ActiveAnimalsCount = $"{MasterAnimals.Count} Monitored";
                TelemetryLogsCount = $"{MasterTelemetry.Count} Recorded";

                Debug.WriteLine($"[MainViewModel] Data ready: {MasterAnimals.Count} animals, {MasterTelemetry.Count} telemetry pings, {PatrolUnits.Count} patrols across Kenya.");
            });
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[MainViewModel] Error loading data: {ex.Message}");
        }
        finally
        {
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                IsRefreshing = false;
            });
        }
    }

    private void ApplyFilter()
    {
        FilteredAnimals.Clear();
        FilteredTelemetryLogs.Clear();

        var animals = MasterAnimals.AsEnumerable();

        if (SelectedPark != "All Kenya")
        {
            animals = animals.Where(a => a.ParkName.Equals(SelectedPark, StringComparison.OrdinalIgnoreCase));
        }

        if (SelectedSpecies != "All Species")
        {
            animals = animals.Where(a => a.Species.Equals(SelectedSpecies, StringComparison.OrdinalIgnoreCase));
        }

        foreach (var animal in animals)
        {
            FilteredAnimals.Add(animal);
        }

        var visibleCollarIds = FilteredAnimals.Select(a => a.CollarId).ToHashSet(StringComparer.OrdinalIgnoreCase);

        var logs = MasterTelemetry
            .Where(t => !string.IsNullOrEmpty(t.AnimalId) && visibleCollarIds.Contains(t.AnimalId))
            .OrderByDescending(t => t.Timestamp)
            .Take(30);

        foreach (var log in logs)
        {
            FilteredTelemetryLogs.Add(log);
        }
    }

    private void EvaluateGeofenceBreaches()
    {
        ActiveIncidents.Clear();

        foreach (var animal in MasterAnimals)
        {
            if (animal.IsBreaching || animal.Status == "Alert")
            {
                var zone = animal.ParkName.Contains("Amboseli") ? "Kimana Community Dispersal Area" : $"{animal.ParkName} Community Buffer";
                ActiveIncidents.Add(new IncidentAlertDto
                {
                    AnimalName = animal.Name,
                    Species    = animal.Species,
                    CollarId   = animal.CollarId,
                    ParkName   = animal.ParkName,
                    ZoneName   = zone,
                    Severity   = "CRITICAL",
                    Message    = $"{animal.Species} '{animal.Name}' ({animal.CollarId}) crossed perimeter into community buffer.",
                    Latitude   = animal.Latitude,
                    Longitude  = animal.Longitude,
                    Timestamp  = DateTime.UtcNow.AddMinutes(-5)
                });

                // Push breach to activity log
                ActivityLog.Insert(0, new ActivityLogEntry
                {
                    Emoji    = "⚠️",
                    Title    = $"BREACH — {animal.Name}",
                    Detail   = $"{animal.Species} entered {zone}",
                    TimeStamp= DateTime.Now.ToString("HH:mm:ss"),
                    Tag      = "BREACH",
                    TagColor = "#FF1744"
                });
            }
        }

        HasActiveAlerts = ActiveIncidents.Count > 0;
        ActiveAlertsSummary = HasActiveAlerts
            ? $"{ActiveIncidents.Count} Geofence Breach Alert(s) Active"
            : string.Empty;
    }

    private async Task DispatchPatrolAsync(IncidentAlertDto alert)
    {
        if (alert == null) return;

        // Find best available patrol unit for the park sector
        var assignedUnit = PatrolUnits.FirstOrDefault(p => p.Sector.Contains(alert.ParkName) && p.Status == "AVAILABLE")
                           ?? PatrolUnits.FirstOrDefault(p => p.Status == "AVAILABLE")
                           ?? PatrolUnits.FirstOrDefault();

        if (assignedUnit != null)
        {
            assignedUnit.Status = "DISPATCHED";
            alert.DispatchedPatrolUnit = $"{assignedUnit.Name} ({assignedUnit.CallSign})";
            await _apiService.DispatchPatrolAsync(assignedUnit.Id, alert.Latitude, alert.Longitude);
        }
        else
        {
            alert.DispatchedPatrolUnit = $"KWS Rapid-Response Unit (Sector {alert.ParkName})";
        }

        alert.IsDispatched = true;
        OnPropertyChanged(nameof(ActiveIncidents));
        OnPropertyChanged(nameof(PatrolUnits));
        UpdateMapHtml();

        Debug.WriteLine($"[MainViewModel] Dispatched {alert.DispatchedPatrolUnit} for incident: {alert.Message}");
    }

    private async Task BroadcastCommunitySmsAsync(IncidentAlertDto alert)
    {
        if (alert == null) return;

        string corridor = alert.ZoneName;
        string message = $"KWS EARLY-WARNING ALERT: {alert.Species} '{alert.AnimalName}' entered {corridor}. KWS quick-response patrol dispatched. Please stay clear.";

        bool success = await _apiService.BroadcastCommunitySmsAsync(corridor, message);

        alert.IsSmsBroadcasted = true;
        alert.SmsBroadcastDetails = "SMS delivered to registered community farmers via KWS Gateway";

        SmsNotificationToast = $"Emergency SMS broadcast sent to local farmers in {corridor} via KWS Africa's Talking Gateway.";
        ShowSmsToast = true;

        OnPropertyChanged(nameof(ActiveIncidents));
        OnPropertyChanged(nameof(ShowSmsToast));
        OnPropertyChanged(nameof(SmsNotificationToast));

        // Auto-hide toast after 5 seconds
        _ = Task.Run(async () =>
        {
            await Task.Delay(5000);
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                ShowSmsToast = false;
                OnPropertyChanged(nameof(ShowSmsToast));
            });
        });
    }

    private void OpenResolveModal(IncidentAlertDto alert)
    {
        _currentResolvingIncident = alert;
        ResolutionNotes = $"Wildlife successfully herded back to protected reserve by KWS field unit. Zero damage to local farms.";
        IsResolveModalOpen = true;
    }

    private async Task SubmitResolveAsync()
    {
        if (_currentResolvingIncident != null)
        {
            _currentResolvingIncident.IsResolved = true;
            _currentResolvingIncident.ResolutionNotes = ResolutionNotes;

            long incId = long.TryParse(_currentResolvingIncident.Id, out var parsed) ? parsed : 1;
            await _apiService.ResolveIncidentAsync(incId, ResolutionNotes);

            // Also reset animal breach flag locally
            var animal = MasterAnimals.FirstOrDefault(a => a.CollarId == _currentResolvingIncident.CollarId);
            if (animal != null)
            {
                animal.IsBreaching = false;
                animal.Status = "Active";
            }

            ActiveIncidents.Remove(_currentResolvingIncident);
            HasActiveAlerts = ActiveIncidents.Count > 0;
            ActiveAlertsSummary = HasActiveAlerts ? $"{ActiveIncidents.Count} Geofence Breach Alert(s) Active" : string.Empty;

            IsResolveModalOpen = false;
            OnPropertyChanged(nameof(ActiveIncidents));
            UpdateMapHtml();
        }
    }

    private async Task SelectAnimalTrailAsync(AnimalDto animal)
    {
        if (animal == null) return;
        SelectedAnimal = animal;

        var trail = await _apiService.GetAnimalTrailAsync(animal.CollarId);
        SelectedTrail = trail ?? new List<TelemetryTrailDto>();

        // Center map onto the animal
        SelectedPark = animal.ParkName;
        UpdateMapHtml();
    }

    private async Task SubmitRegistrationAsync()
    {
        if (string.IsNullOrWhiteSpace(NewAnimalName) || string.IsNullOrWhiteSpace(NewCollarId))
        {
            return;
        }

        // Determine coordinates based on chosen park
        double lat = -2.6527;
        double lng = 37.2606;

        switch (NewParkName)
        {
            case "Tsavo East NP": lat = -2.8500; lng = 38.6500; break;
            case "Maasai Mara": lat = -1.4917; lng = 35.1444; break;
            case "Nairobi NP": lat = -1.3733; lng = 36.8589; break;
            case "Ol Pejeta": lat = 0.0389; lng = 36.9639; break;
            default: break;
        }

        var newAnimal = new AnimalDto
        {
            Id = MasterAnimals.Count + 1,
            Name = NewAnimalName,
            Species = NewAnimalSpecies,
            CollarId = NewCollarId,
            ParkName = NewParkName,
            Sex = NewAnimalSex,
            CollarBattery = 100,
            Latitude = lat,
            Longitude = lng,
            Status = "Active",
            IsBreaching = false
        };

        bool savedToApi = await _apiService.RegisterAnimalAsync(newAnimal);
        Debug.WriteLine($"[MainViewModel] Register animal API call result: {savedToApi}");

        MasterAnimals.Add(newAnimal);
        ApplyFilter();
        UpdateMapHtml();

        ActiveAnimalsCount = $"{MasterAnimals.Count} Monitored";
        IsRegistrationModalOpen = false;
    }

    private void StartLiveMovementSimulation()
    {
        if (_liveMovementTimer != null)
        {
            return;
        }

        var dispatcher = Application.Current?.Dispatcher;
        if (dispatcher == null)
        {
            MainThread.BeginInvokeOnMainThread(StartLiveMovementSimulation);
            return;
        }

        _liveMovementTimer = dispatcher.CreateTimer();
        _liveMovementTimer.Interval = TimeSpan.FromSeconds(3);
        _liveMovementTimer.IsRepeating = true;
        _liveMovementTimer.Tick += OnLiveMovementTick;
        _liveMovementTimer.Start();
        Debug.WriteLine("[MainViewModel] Live wildlife movement simulation started.");
    }

    private async void OnLiveMovementTick(object? sender, EventArgs e)
    {
        if (Interlocked.Exchange(ref _liveMovementTickGate, 1) == 1)
        {
            return;
        }

        try
        {
            await AdvanceLiveAnimalPositionsAsync();
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[MainViewModel] Live movement tick failed: {ex.Message}");
        }
        finally
        {
            Interlocked.Exchange(ref _liveMovementTickGate, 0);
        }
    }

    private readonly Dictionary<string, int> _animalWaypointIndices = new(StringComparer.OrdinalIgnoreCase);
    private readonly Dictionary<string, int> _animalRestCounters = new(StringComparer.OrdinalIgnoreCase);
    private double _airwingAngle = 0.0;

    private static readonly (double Lat, double Lng)[] NairobiParkPolygon = new[]
    {
        (-1.3500, 36.7760), // KWS HQ
        (-1.3465, 36.7950), // Langata Sanctuary
        (-1.3420, 36.8200), // Southern Bypass Fence Line
        (-1.3425, 36.8450), // South of Wilson Airport
        (-1.3440, 36.8720), // South of South C / Nairobi West
        (-1.3520, 36.8980), // Mombasa Road / Depot
        (-1.3620, 36.9250), // Syokimau / SGR
        (-1.3900, 36.9500), // Athi Basin East
        (-1.4250, 36.9380), // Athi Confluence
        (-1.4380, 36.8900), // Mbagathi River Border
        (-1.4300, 36.8500), // Southern Savanna
        (-1.4150, 36.8150), // Silole Gorge
        (-1.3950, 36.7800), // Kingfisher Gorge
        (-1.3700, 36.7650), // Western Boundary
        (-1.3500, 36.7760)
    };

    private static readonly (double Lat, double Lng)[] AmboseliParkPolygon = new[]
    {
        (-2.5800, 37.1800),
        (-2.5800, 37.3500),
        (-2.7200, 37.3500),
        (-2.7200, 37.1800),
        (-2.5800, 37.1800)
    };

    private static readonly (double Lat, double Lng)[] MaasaiMaraPolygon = new[]
    {
        (-1.3800, 34.9800),
        (-1.3800, 35.3200),
        (-1.6200, 35.3200),
        (-1.6200, 34.9800),
        (-1.3800, 34.9800)
    };

    private static readonly (double Lat, double Lng)[] TsavoEastPolygon = new[]
    {
        (-2.4000, 38.3000),
        (-2.4000, 39.0000),
        (-3.3000, 39.0000),
        (-3.3000, 38.3000),
        (-2.4000, 38.3000)
    };

    private static readonly (double Lat, double Lng)[] OlPejetaPolygon = new[]
    {
        (0.0000, 36.8800),
        (0.1000, 36.8800),
        (0.1000, 37.0500),
        (0.0000, 37.0500),
        (0.0000, 36.8800)
    };

    private static readonly Dictionary<string, (double Lat, double Lng, string Name)[]> ParkWaypoints = new(StringComparer.OrdinalIgnoreCase)
    {
        ["Nairobi NP"] = new[]
        {
            (-1.3850, 36.8720, "Hippo Pools & Mbagathi River"),
            (-1.3820, 36.8250, "Nagolomon Dam"),
            (-1.3650, 36.8400, "Hyena Dam Plains"),
            (-1.4050, 36.8900, "Athi River Basin"),
            (-1.3700, 36.8000, "Central Rhino Sanctuary")
        },
        ["Amboseli NP"] = new[]
        {
            (-2.6650, 37.2650, "Enkongo Narok Swamp"),
            (-2.6500, 37.3100, "Ol Tukai Springs"),
            (-2.6780, 37.2400, "Observation Hill"),
            (-2.6300, 37.2200, "Amboseli Basin")
        },
        ["Maasai Mara"] = new[]
        {
            (-1.4420, 35.2150, "Talek River Plains"),
            (-1.5100, 35.0350, "Mara River Crossing"),
            (-1.3900, 35.1100, "Musiara Marsh"),
            (-1.4850, 35.1280, "Rhino Ridge")
        },
        ["Tsavo East NP"] = new[]
        {
            (-2.8124, 38.6210, "Galana River Rapids"),
            (-3.1150, 38.8250, "Aruba Dam"),
            (-3.1800, 38.5200, "Mudanda Rock"),
            (-3.1250, 38.5410, "Satao Waterhole")
        },
        ["Ol Pejeta"] = new[]
        {
            (0.0420, 36.9450, "Sweetwaters Plains"),
            (0.0250, 36.9200, "Ewaso Nyiro Riverbed"),
            (0.0600, 36.9700, "Pelican Dam")
        }
    };

    private static bool IsPointInPolygon(double lat, double lng, (double Lat, double Lng)[] poly)
    {
        if (poly == null || poly.Length < 3) return true;
        bool inside = false;
        int j = poly.Length - 1;
        for (int i = 0; i < poly.Length; i++)
        {
            if ((poly[i].Lng > lng) != (poly[j].Lng > lng) &&
                lat < (poly[j].Lat - poly[i].Lat) * (lng - poly[i].Lng) / (poly[j].Lng - poly[i].Lng) + poly[i].Lat)
            {
                inside = !inside;
            }
            j = i;
        }
        return inside;
    }

    private static double DistanceKm(double lat1, double lng1, double lat2, double lng2)
    {
        double dLat = (lat2 - lat1) * Math.PI / 180.0;
        double dLng = (lng2 - lng1) * Math.PI / 180.0;
        double a = Math.Sin(dLat / 2) * Math.Sin(dLat / 2) +
                   Math.Cos(lat1 * Math.PI / 180.0) * Math.Cos(lat2 * Math.PI / 180.0) *
                   Math.Sin(dLng / 2) * Math.Sin(dLng / 2);
        return 6371.0 * 2.0 * Math.Atan2(Math.Sqrt(a), Math.Sqrt(1.0 - a));
    }

    private static string HeadingToCompass(double headingRadians)
    {
        double deg = (headingRadians * 180.0 / Math.PI) % 360.0;
        if (deg < 0) deg += 360.0;
        string[] cardinals = { "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW" };
        int idx = (int)Math.Round(deg / 22.5) % 16;
        return $"{cardinals[idx]} {deg:F0}°";
    }

    private static string CalculateFenceStatus(double lat, double lng, string parkName)
    {
        if (!string.Equals(parkName, "Nairobi NP", StringComparison.OrdinalIgnoreCase))
        {
            return "Inside Sanctuary";
        }

        // Distance to northern electric fence (latitude approx -1.3420)
        double distToFenceKm = Math.Max(0.05, (-1.3420 - lat) * 111.0);
        if (distToFenceKm < 0.6)
        {
            return $"⚡ Near Northern Fence ({distToFenceKm * 1000:F0}m from fence)";
        }
        return $"🛡️ Safe in Sanctuary ({distToFenceKm:F1} km from City fence)";
    }

    private static double ResolveRealisticSpeed(string species)
    {
        var s = (species ?? string.Empty).ToLowerInvariant();
        if (s.Contains("cheetah")) return 7.5;
        if (s.Contains("lion")) return 4.2;
        if (s.Contains("elephant")) return 3.2;
        if (s.Contains("rhino")) return 2.4;
        return 3.0;
    }

    private static string ResolveBehaviorState(string species, string targetWpName, double distKm)
    {
        var s = (species ?? string.Empty).ToLowerInvariant();
        if (distKm < 0.6)
        {
            return $"💧 Watering near {targetWpName}";
        }
        if (s.Contains("rhino")) return $"🌿 Browsing Acacia Bushland";
        if (s.Contains("elephant")) return $"🐘 Family Corridor Foraging";
        if (s.Contains("lion")) return $"🐾 Pride Patrol towards {targetWpName}";
        if (s.Contains("cheetah")) return $"🐆 Open Plains Scan";
        return $"🌿 Grazing in Savannah";
    }

    private static (double Lat, double Lng)[] ResolveParkPolygon(string parkName)
    {
        return parkName switch
        {
            "Nairobi NP" => NairobiParkPolygon,
            "Amboseli NP" => AmboseliParkPolygon,
            "Maasai Mara" => MaasaiMaraPolygon,
            "Tsavo East NP" => TsavoEastPolygon,
            "Ol Pejeta" => OlPejetaPolygon,
            _ => NairobiParkPolygon
        };
    }

    private static double ResolveStepLength(string species)
    {
        var s = (species ?? string.Empty).ToLowerInvariant();
        if (s.Contains("cheetah")) return 0.0030;
        if (s.Contains("lion")) return 0.0022;
        if (s.Contains("rhino")) return 0.0012;
        if (s.Contains("elephant")) return 0.0018;
        return 0.0016;
    }

    private async Task AdvanceLiveAnimalPositionsAsync()
    {
        if (!IsSimulationPlaying || MasterAnimals.Count == 0)
        {
            return;
        }

        List<object> positions = new();
        PatrolUnitDto? airwingUnit = null;

        await MainThread.InvokeOnMainThreadAsync(() =>
        {
            foreach (var animal in MasterAnimals)
            {
                ApplyRealisticSimulatedMovement(animal);

                double currentHeading = _movementHeadings.TryGetValue(animal.CollarId, out var h) ? h : 0;
                double predStep = ResolveStepLength(animal.Species) * 0.45 * 6.0;
                double predLat = animal.Latitude + Math.Cos(currentHeading) * predStep;
                double predLng = animal.Longitude + Math.Sin(currentHeading) * predStep / Math.Max(0.1, Math.Cos(animal.Latitude * Math.PI / 180.0));

                positions.Add(new
                {
                    collarId = animal.CollarId,
                    latitude = animal.Latitude,
                    longitude = animal.Longitude,
                    speedDisplay = animal.SpeedDisplay,
                    headingCompass = animal.HeadingCompass,
                    behaviorState = animal.BehaviorState,
                    fenceDistance = animal.FenceDistance,
                    predictedCorridor = animal.PredictedCorridor,
                    predictedLat = predLat,
                    predictedLng = predLng,
                    solarVoltageDisplay = animal.SolarVoltageDisplay,
                    collarTempDisplay = animal.CollarTempDisplay,
                    signalStrength = animal.SignalStrength
                });
            }

            // Animate KWS Airwing Aerial Surveillance Flight
            airwingUnit = PatrolUnits.FirstOrDefault(p => p.UnitType == "AIRWING");
            if (airwingUnit != null)
            {
                double airStep = 0.0028 * SimulationSpeedMultiplier;
                airwingUnit.Latitude += Math.Cos(_airwingAngle) * airStep * 0.7;
                airwingUnit.Longitude += Math.Sin(_airwingAngle) * airStep;
                _airwingAngle += 0.06;
            }

            RecordLiveTelemetrySample();
            LiveFeedStatus = $"LIVE • {DateTime.Now:HH:mm:ss} • {MasterAnimals.Count} collars tracked ({SimulationSpeedMultiplier:F0}x)";
            TelemetryLogsCount = $"{MasterTelemetry.Count} Recorded";
        });

        var payload = JsonSerializer.Serialize(positions);
        var script = $"updateAnimalPositions({payload})";

        await MainThread.InvokeOnMainThreadAsync(async () =>
        {
            var inject = InjectLiveMapScript;
            if (inject != null)
            {
                await inject(script);

                if (airwingUnit != null)
                {
                    var airScript = $"updateAirwingFlight({airwingUnit.Latitude.ToString(System.Globalization.CultureInfo.InvariantCulture)}, {airwingUnit.Longitude.ToString(System.Globalization.CultureInfo.InvariantCulture)}, 115, {airwingUnit.AltitudeFt}, {airwingUnit.SpeedKmh.ToString(System.Globalization.CultureInfo.InvariantCulture)})";
                    await inject(airScript);
                }
            }
        });
    }

    private void ApplyRealisticSimulatedMovement(AnimalDto animal)
    {
        if (string.Equals(animal.Status, "IMMOBILE", StringComparison.OrdinalIgnoreCase))
        {
            animal.SpeedKmh = 0.0;
            animal.BehaviorState = "⚠️ Immobile (Monitoring)";
            return;
        }

        // Species resting behavior (Lions rest periodically)
        bool isLion = animal.Species.Contains("Lion", StringComparison.OrdinalIgnoreCase);
        if (isLion)
        {
            if (_animalRestCounters.TryGetValue(animal.CollarId, out var restRemaining) && restRemaining > 0)
            {
                _animalRestCounters[animal.CollarId] = restRemaining - 1;
                animal.SpeedKmh = 0.0;
                animal.BehaviorState = "💤 Resting in Savannah Shade";
                animal.FenceDistance = CalculateFenceStatus(animal.Latitude, animal.Longitude, animal.ParkName);
                return;
            }

            // 15% chance to start resting
            if (_movementRng.NextDouble() < 0.15)
            {
                _animalRestCounters[animal.CollarId] = _movementRng.Next(3, 6);
                animal.SpeedKmh = 0.0;
                animal.BehaviorState = "💤 Resting in Savannah Shade";
                animal.FenceDistance = CalculateFenceStatus(animal.Latitude, animal.Longitude, animal.ParkName);
                return;
            }
        }

        // Find park waypoints
        string parkKey = animal.ParkName;
        if (!ParkWaypoints.ContainsKey(parkKey))
        {
            parkKey = "Nairobi NP";
        }
        var waypoints = ParkWaypoints[parkKey];

        if (!_animalWaypointIndices.TryGetValue(animal.CollarId, out var currentWpIndex) || currentWpIndex >= waypoints.Length)
        {
            currentWpIndex = _movementRng.Next(waypoints.Length);
            _animalWaypointIndices[animal.CollarId] = currentWpIndex;
        }

        var targetWp = waypoints[currentWpIndex];
        double distToWp = DistanceKm(animal.Latitude, animal.Longitude, targetWp.Lat, targetWp.Lng);

        // If arrived close to waypoint (< 400m), choose next waypoint in circuit
        if (distToWp < 0.40)
        {
            currentWpIndex = (currentWpIndex + 1) % waypoints.Length;
            _animalWaypointIndices[animal.CollarId] = currentWpIndex;
            targetWp = waypoints[currentWpIndex];
        }

        // Calculate heading to target waypoint
        double dLat = targetWp.Lat - animal.Latitude;
        double dLng = (targetWp.Lng - animal.Longitude) * Math.Cos(animal.Latitude * Math.PI / 180.0);
        double desiredHeading = Math.Atan2(dLng, dLat);

        // Smooth steering towards waypoint with momentum
        if (!_movementHeadings.TryGetValue(animal.CollarId, out var currentHeading))
        {
            currentHeading = desiredHeading;
        }

        double angleDiff = Math.Atan2(Math.Sin(desiredHeading - currentHeading), Math.Cos(desiredHeading - currentHeading));
        currentHeading += angleDiff * 0.25; // 25% smooth turn towards target
        currentHeading += (_movementRng.NextDouble() - 0.5) * 0.18; // gentle natural wander

        // CRITICAL: Nairobi NP Northern Electric Fence Avoidance (Never enter Nairobi City!)
        if (string.Equals(animal.ParkName, "Nairobi NP", StringComparison.OrdinalIgnoreCase))
        {
            // If heading north towards the city fence (-1.350 to -1.342)
            if (animal.Latitude > -1.352)
            {
                // Repel away from fence towards southern riverine plains (-1.385, 36.850)
                double southHeading = Math.PI * 0.65; // South-southeast towards Mbagathi river & Athi basin
                double fenceAngleDiff = Math.Atan2(Math.Sin(southHeading - currentHeading), Math.Cos(southHeading - currentHeading));
                currentHeading += fenceAngleDiff * 0.60;
            }
        }

        _movementHeadings[animal.CollarId] = currentHeading;

        // Realistic speeds by species
        double baseSpeedKmh = ResolveRealisticSpeed(animal.Species);
        animal.SpeedKmh = baseSpeedKmh * (0.85 + _movementRng.NextDouble() * 0.3);

        // Visual step scaling for live tactical dashboard
        double step = ResolveStepLength(animal.Species) * 0.45 * SimulationSpeedMultiplier;
        double nextLat = animal.Latitude + Math.Cos(currentHeading) * step;
        double nextLng = animal.Longitude + Math.Sin(currentHeading) * step / Math.Max(0.1, Math.Cos(animal.Latitude * Math.PI / 180.0));

        // Polygon Containment Validation
        var polygon = ResolveParkPolygon(animal.ParkName);
        bool isInside = IsPointInPolygon(nextLat, nextLng, polygon);

        // Hard northern limit for Nairobi NP: latitude MUST NOT exceed -1.3425
        if (string.Equals(animal.ParkName, "Nairobi NP", StringComparison.OrdinalIgnoreCase))
        {
            if (nextLat > -1.3430)
            {
                isInside = false;
            }
        }

        if (isInside)
        {
            animal.Latitude = nextLat;
            animal.Longitude = nextLng;
        }
        else
        {
            // Reverse heading and gently move towards park center / waypoint
            _movementHeadings[animal.CollarId] = currentHeading + Math.PI;
            double centerLat = targetWp.Lat;
            double centerLng = targetWp.Lng;
            animal.Latitude = animal.Latitude + (centerLat - animal.Latitude) * 0.08;
            animal.Longitude = animal.Longitude + (centerLng - animal.Longitude) * 0.08;
        }

        // Update telemetry properties
        animal.HeadingCompass = HeadingToCompass(_movementHeadings[animal.CollarId]);
        animal.BehaviorState = ResolveBehaviorState(animal.Species, targetWp.Name, distToWp);
        animal.FenceDistance = CalculateFenceStatus(animal.Latitude, animal.Longitude, animal.ParkName);
        int etaMin = Math.Max(4, (int)(distToWp / (Math.Max(0.5, animal.SpeedKmh) / 60.0)));
        animal.PredictedCorridor = $"Projected toward {targetWp.Name} (ETA ~{etaMin} min)";
        if (string.Equals(animal.ParkName, "Nairobi NP", StringComparison.OrdinalIgnoreCase) && animal.Latitude > -1.352)
        {
            animal.PredictedCorridor = "⚠️ Repelling South from Northern City Fence";
        }
    }

    private void RefreshAnalytics()
    {
        // --- Conservation Species Cards ---
        ConservationCards.Clear();
        var speciesGroups = MasterAnimals
            .GroupBy(a => a.Species)
            .OrderBy(g => g.Key);

        foreach (var grp in speciesGroups)
        {
            string iucn = ResolveIucnStatus(grp.Key);
            string iucnColor = ResolveIucnColor(iucn);
            int avgBattery = (int)grp.Average(a => a.CollarBattery);
            ConservationCards.Add(new ConservationSpeciesCard
            {
                Species = grp.Key,
                Count = grp.Count(),
                IucnStatus = iucn,
                IucnColor = iucnColor,
                AvgBattery = avgBattery,
                Parks = string.Join(", ", grp.Select(a => a.ParkName).Distinct().Take(2))
            });
        }

        // --- Park Weather Cards (deterministic based on park) ---
        ParkWeatherCards.Clear();
        var activeParkNames = MasterAnimals.Select(a => a.ParkName).Distinct().ToList();
        foreach (var park in activeParkNames)
        {
            ParkWeatherCards.Add(BuildParkWeather(park));
        }

        // --- KPI Counters ---
        AlertsCountDisplay = $"{ActiveIncidents.Count} Active";
        PatrolsCountDisplay = $"{PatrolUnits.Count} Units";
        ParksCountDisplay = $"{activeParkNames.Count} Parks";

        // --- Collar Health Summary ---
        if (MasterAnimals.Count > 0)
        {
            int critical = MasterAnimals.Count(a => a.CollarBattery < 20);
            int warning  = MasterAnimals.Count(a => a.CollarBattery >= 20 && a.CollarBattery < 50);
            int healthy  = MasterAnimals.Count(a => a.CollarBattery >= 50);
            CollarHealthSummary = $"🟢 Healthy: {healthy}   🟡 Warning: {warning}   🔴 Critical: {critical}";
        }
    }

    private static string ResolveIucnStatus(string species)
    {
        var s = species.ToLowerInvariant();
        if (s.Contains("black rhino") || s.Contains("eastern black")) return "CR";
        if (s.Contains("elephant")) return "VU";
        if (s.Contains("cheetah")) return "VU";
        if (s.Contains("lion")) return "VU";
        if (s.Contains("giraffe")) return "VU";
        if (s.Contains("wild dog") || s.Contains("lycaon")) return "EN";
        if (s.Contains("pangolin")) return "CR";
        return "LC";
    }

    private static string ResolveIucnColor(string iucn) => iucn switch
    {
        "CR" => "#FF1744",
        "EN" => "#FF5722",
        "VU" => "#FF9800",
        "NT" => "#FFEB3B",
        _    => "#4CAF50"
    };

    private static ParkWeatherCard BuildParkWeather(string park)
    {
        // Realistic Kenyan park micro-climates (semi-static data, refreshed on load)
        return park switch
        {
            "Amboseli NP"  => new ParkWeatherCard { Park = park, Emoji = "⛅", TempC = 28, Condition = "Partly Cloudy", Humidity = 55, WindKmh = 14 },
            "Tsavo East NP"=> new ParkWeatherCard { Park = park, Emoji = "☀️", TempC = 35, Condition = "Hot & Dry",      Humidity = 30, WindKmh = 22 },
            "Tsavo West NP"=> new ParkWeatherCard { Park = park, Emoji = "🌤", TempC = 30, Condition = "Clear Savannah", Humidity = 45, WindKmh = 11 },
            "Maasai Mara"  => new ParkWeatherCard { Park = park, Emoji = "🌧", TempC = 22, Condition = "Light Showers",  Humidity = 78, WindKmh = 18 },
            "Nairobi NP"   => new ParkWeatherCard { Park = park, Emoji = "🌦", TempC = 24, Condition = "Warm & Breezy",  Humidity = 62, WindKmh = 12 },
            "Ol Pejeta"    => new ParkWeatherCard { Park = park, Emoji = "☀️", TempC = 27, Condition = "Sunny Equatorial",Humidity = 50, WindKmh = 9 },
            "Samburu"      => new ParkWeatherCard { Park = park, Emoji = "☀️", TempC = 33, Condition = "Arid & Hot",     Humidity = 28, WindKmh = 25 },
            "Lake Nakuru"  => new ParkWeatherCard { Park = park, Emoji = "⛅", TempC = 21, Condition = "Rift Valley Mist",Humidity = 72, WindKmh = 8 },
            "Aberdare"     => new ParkWeatherCard { Park = park, Emoji = "🌧", TempC = 16, Condition = "Highland Rain",  Humidity = 88, WindKmh = 15 },
            "Mount Kenya"  => new ParkWeatherCard { Park = park, Emoji = "❄️", TempC = 9,  Condition = "Alpine Cold",    Humidity = 80, WindKmh = 20 },
            "Lewa"         => new ParkWeatherCard { Park = park, Emoji = "⛅", TempC = 25, Condition = "Scattered Cloud", Humidity = 55, WindKmh = 10 },
            _              => new ParkWeatherCard { Park = park, Emoji = "☀️", TempC = 28, Condition = "Savannah Sun",   Humidity = 48, WindKmh = 13 }
        };
    }

    private void RecordLiveTelemetrySample()
    {
        if (MasterAnimals.Count == 0) return;

        var animal = MasterAnimals[_movementRng.Next(MasterAnimals.Count)];
        MasterTelemetry.Insert(0, new TelemetryLocation
        {
            Id = DateTime.UtcNow.Ticks,
            AnimalId = animal.CollarId,
            CollarId = animal.CollarId,
            Latitude = animal.Latitude,
            Longitude = animal.Longitude,
            Timestamp = DateTime.UtcNow,
            Animal = animal
        });

        while (MasterTelemetry.Count > 80)
            MasterTelemetry.RemoveAt(MasterTelemetry.Count - 1);

        FilteredTelemetryLogs.Clear();
        var visibleCollarIds = FilteredAnimals.Select(a => a.CollarId).ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var log in MasterTelemetry
                     .Where(t => !string.IsNullOrEmpty(t.AnimalId) && visibleCollarIds.Contains(t.AnimalId))
                     .Take(30))
        {
            FilteredTelemetryLogs.Add(log);
        }

        // Push to activity log (keep newest 40)
        ActivityLog.Insert(0, new ActivityLogEntry
        {
            Emoji    = "📡",
            Title    = $"{animal.Name} — GPS Fix",
            Detail   = $"{animal.Species} • {animal.ParkName} • {animal.BehaviorState}",
            TimeStamp= DateTime.Now.ToString("HH:mm:ss"),
            Tag      = "TELEMETRY",
            TagColor = "#00E5FF"
        });
        while (ActivityLog.Count > 40)
            ActivityLog.RemoveAt(ActivityLog.Count - 1);
    }

    private void UpdateMapHtml()
    {
        double centerLat = 0.5000;
        double centerLng = 37.8500;
        int zoomLevel = 6;

        switch (SelectedPark)
        {
            case "Amboseli NP": centerLat = -2.6527; centerLng = 37.2606; zoomLevel = 11; break;
            case "Tsavo East NP": centerLat = -2.8500; centerLng = 38.6500; zoomLevel = 9; break;
            case "Tsavo West NP": centerLat = -3.1000; centerLng = 38.0500; zoomLevel = 10; break;
            case "Maasai Mara": centerLat = -1.4917; centerLng = 35.1444; zoomLevel = 10; break;
            case "Mara North": centerLat = -1.2800; centerLng = 35.1500; zoomLevel = 11; break;
            case "Nairobi NP": centerLat = -1.3733; centerLng = 36.8589; zoomLevel = 12; break;
            case "Ol Pejeta": centerLat = 0.0389; centerLng = 36.9639; zoomLevel = 12; break;
            case "Lewa": centerLat = 0.2500; centerLng = 37.4500; zoomLevel = 12; break;
            case "Lake Nakuru": centerLat = -0.3700; centerLng = 36.0800; zoomLevel = 12; break;
            case "Samburu": centerLat = 0.6200; centerLng = 37.5300; zoomLevel = 11; break;
            case "Aberdare": centerLat = -0.5500; centerLng = 36.7200; zoomLevel = 11; break;
            case "Mount Kenya": centerLat = -0.1500; centerLng = 37.3200; zoomLevel = 11; break;
            default: break;
        }

        MapHtml = OpsMapHtmlBuilder.Build(
            centerLat,
            centerLng,
            zoomLevel,
            FilteredAnimals.ToList(),
            Geofences.ToList(),
            PatrolUnits.ToList(),
            ActiveIncidents.ToList(),
            SelectedTrail ?? new List<TelemetryTrailDto>());
    }
}
