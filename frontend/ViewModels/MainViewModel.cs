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

    // Set by MainPage code-behind after construction (opens detail panel / JS inject)
    public ICommand? SelectAnimalDetailCommand { get; set; }
    public Func<string, Task>? InjectLiveMapScript { get; set; }

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

    public MainViewModel(ApiService apiService)
    {
        _apiService = apiService ?? throw new ArgumentNullException(nameof(apiService));

        LoadDataCommand = new Command(async () => await LoadDataAsync(), () => !IsRefreshing);
        SelectParkCommand = new Command<string>(park => SelectedPark = park);
        SelectSpeciesCommand = new Command<string>(species => SelectedSpecies = species);
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

        MainThread.BeginInvokeOnMainThread(StartLiveMovementSimulation);
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

        // Check for animals flagged as breaching or positioned near community boundaries
        foreach (var animal in MasterAnimals)
        {
            if (animal.IsBreaching || animal.Status == "Alert")
            {
                ActiveIncidents.Add(new IncidentAlertDto
                {
                    AnimalName = animal.Name,
                    Species = animal.Species,
                    CollarId = animal.CollarId,
                    ParkName = animal.ParkName,
                    ZoneName = animal.ParkName.Contains("Amboseli") ? "Kimana Community Dispersal Area" : $"{animal.ParkName} Community Buffer",
                    Severity = "CRITICAL",
                    Message = $"{animal.Species} '{animal.Name}' ({animal.CollarId}) crossed perimeter into community buffer.",
                    Latitude = animal.Latitude,
                    Longitude = animal.Longitude,
                    Timestamp = DateTime.UtcNow.AddMinutes(-5)
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

    private async Task AdvanceLiveAnimalPositionsAsync()
    {
        if (MasterAnimals.Count == 0)
        {
            return;
        }

        List<object> positions = new();

        await MainThread.InvokeOnMainThreadAsync(() =>
        {
            foreach (var animal in MasterAnimals)
            {
                ApplySimulatedMovement(animal);
                positions.Add(new
                {
                    collarId = animal.CollarId,
                    latitude = animal.Latitude,
                    longitude = animal.Longitude
                });
            }

            RecordLiveTelemetrySample();
            LiveFeedStatus = $"LIVE Â· {DateTime.Now:HH:mm:ss}  {MasterAnimals.Count} collars moving";
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
            }
        });
    }

    private void ApplySimulatedMovement(AnimalDto animal)
    {
        if (string.Equals(animal.Status, "IMMOBILE", StringComparison.OrdinalIgnoreCase))
        {
            return;
        }

        if (!_movementHeadings.TryGetValue(animal.CollarId, out var heading))
        {
            heading = _movementRng.NextDouble() * Math.PI * 2.0;
        }

        heading += (_movementRng.NextDouble() - 0.5) * 0.55;
        _movementHeadings[animal.CollarId] = heading;

        var step = ResolveStepLength(animal.Species);
        var nextLat = animal.Latitude + Math.Cos(heading) * step;
        var nextLng = animal.Longitude + Math.Sin(heading) * step;

        var bounds = ResolveParkBounds(animal);
        var clampedLat = Math.Clamp(nextLat, bounds.MinLat, bounds.MaxLat);
        var clampedLng = Math.Clamp(nextLng, bounds.MinLng, bounds.MaxLng);

        if (Math.Abs(clampedLat - nextLat) > double.Epsilon || Math.Abs(clampedLng - nextLng) > double.Epsilon)
        {
            _movementHeadings[animal.CollarId] = heading + Math.PI;
        }

        animal.Latitude = clampedLat;
        animal.Longitude = clampedLng;
    }

    private static double ResolveStepLength(string species)
    {
        if (species.Contains("Cheetah", StringComparison.OrdinalIgnoreCase))
            return 0.0030;
        if (species.Contains("Lion", StringComparison.OrdinalIgnoreCase))
            return 0.0022;
        if (species.Contains("Rhino", StringComparison.OrdinalIgnoreCase))
            return 0.0011;
        if (species.Contains("Elephant", StringComparison.OrdinalIgnoreCase))
            return 0.0018;
        return 0.0016;
    }

    private static (double MinLat, double MaxLat, double MinLng, double MaxLng) ResolveParkBounds(AnimalDto animal)
    {
        (double MinLat, double MaxLat, double MinLng, double MaxLng) core = animal.ParkName switch
        {
            "Amboseli NP" => (-2.78, -2.55, 37.15, 37.40),
            "Tsavo East NP" => (-3.40, -2.30, 38.35, 39.10),
            "Maasai Mara" => (-1.65, -1.30, 34.90, 35.40),
            "Nairobi NP" => (-1.42, -1.32, 36.80, 36.90),
            "Ol Pejeta" => (-0.05, 0.10, 36.88, 37.05),
            _ => (-4.70, 1.20, 33.90, 41.90)
        };

        if (animal.IsBreaching)
        {
            return (core.MinLat - 0.045, core.MaxLat + 0.045, core.MinLng - 0.045, core.MaxLng + 0.045);
        }

        return core;
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
        {
            MasterTelemetry.RemoveAt(MasterTelemetry.Count - 1);
        }

        FilteredTelemetryLogs.Clear();
        var visibleCollarIds = FilteredAnimals.Select(a => a.CollarId).ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var log in MasterTelemetry
                     .Where(t => !string.IsNullOrEmpty(t.AnimalId) && visibleCollarIds.Contains(t.AnimalId))
                     .Take(30))
        {
            FilteredTelemetryLogs.Add(log);
        }
    }

    private void UpdateMapHtml()
    {
        double centerLat = -1.286389;
        double centerLng = 36.817223;
        int zoomLevel = 7;

        switch (SelectedPark)
        {
            case "Amboseli NP": centerLat = -2.6527; centerLng = 37.2606; zoomLevel = 11; break;
            case "Tsavo East NP": centerLat = -2.8500; centerLng = 38.6500; zoomLevel = 9; break;
            case "Maasai Mara": centerLat = -1.4917; centerLng = 35.1444; zoomLevel = 10; break;
            case "Nairobi NP": centerLat = -1.3733; centerLng = 36.8589; zoomLevel = 12; break;
            case "Ol Pejeta": centerLat = 0.0389; centerLng = 36.9639; zoomLevel = 12; break;
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
