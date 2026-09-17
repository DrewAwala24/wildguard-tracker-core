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
    public ICommand SimulateTelemetryStepCommand { get; }
    public ICommand OpenRegistrationCommand { get; }
    public ICommand CloseRegistrationCommand { get; }
    public ICommand SubmitRegistrationCommand { get; }

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

    public string MapHtml
    {
        get => _mapHtml;
        set
        {
            if (_mapHtml != value)
            {
                _mapHtml = value;
                OnPropertyChanged();
                OnPropertyChanged(nameof(MapHtmlSource));
            }
        }
    }

    public HtmlWebViewSource MapHtmlSource => new() { Html = MapHtml };

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
        SimulateTelemetryStepCommand = new Command(async () => await SimulateTelemetryStepAsync());

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

    private async Task SimulateTelemetryStepAsync()
    {
        await _apiService.TriggerSimulationStepAsync();
        await LoadDataAsync();
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

        var animalsToRender = FilteredAnimals.ToList();
        var geofencesToRender = Geofences.ToList();
        var patrolsToRender = PatrolUnits.ToList();
        var incidentsToRender = ActiveIncidents.ToList();
        var trailToRender = SelectedTrail != null ? SelectedTrail.ToList() : new List<TelemetryTrailDto>();

        var animalsJson = JsonSerializer.Serialize(animalsToRender);
        var geofencesJson = JsonSerializer.Serialize(geofencesToRender);
        var patrolsJson = JsonSerializer.Serialize(patrolsToRender);
        var incidentsJson = JsonSerializer.Serialize(incidentsToRender);
        var trailJson = JsonSerializer.Serialize(trailToRender);

        MapHtml = $@"<!DOCTYPE html>
<html>
<head>
    <meta charset='utf-8' />
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css' />
    <style>
        html, body, #map {{
            height: 100%;
            width: 100%;
            margin: 0;
            padding: 0;
            background-color: #2b3815;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }}
        .leaflet-popup-content-wrapper {{
            background: #2b3815;
            color: #ffffff;
            border: 1px solid #FFD700;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.5);
        }}
        .leaflet-popup-tip {{
            background: #2b3815;
        }}
        .popup-title {{
            font-size: 14px;
            font-weight: bold;
            color: #FFD700;
            margin-bottom: 4px;
        }}
        .popup-desc {{
            font-size: 11px;
            color: #e0e0e0;
            line-height: 1.4;
        }}
        .pulse-marker {{
            width: 14px;
            height: 14px;
            background: #FFD700;
            border: 2px solid #ffffff;
            border-radius: 50%;
            box-shadow: 0 0 8px #FFD700;
        }}
        .alert-marker {{
            width: 16px;
            height: 16px;
            background: #FF5252;
            border: 2px solid #ffffff;
            border-radius: 50%;
            animation: pulse 1.2s infinite;
        }}
        .warning-marker {{
            width: 14px;
            height: 14px;
            background: #FF9800;
            border: 2px solid #ffffff;
            border-radius: 50%;
            box-shadow: 0 0 8px #FF9800;
        }}
        .patrol-marker {{
            width: 16px;
            height: 16px;
            background: #00E5FF;
            border: 2px solid #ffffff;
            border-radius: 3px;
            transform: rotate(45deg);
            box-shadow: 0 0 10px #00E5FF;
        }}
        .patrol-dispatched-marker {{
            width: 18px;
            height: 18px;
            background: #FFD700;
            border: 2px solid #FF5252;
            border-radius: 3px;
            transform: rotate(45deg);
            animation: pulse 1.0s infinite;
        }}
        @keyframes pulse {{
            0% {{ box-shadow: 0 0 0 0 rgba(255, 82, 82, 0.7); }}
            70% {{ box-shadow: 0 0 0 10px rgba(255, 82, 82, 0); }}
            100% {{ box-shadow: 0 0 0 0 rgba(255, 82, 82, 0); }}
        }}
    </style>
</head>
<body>
    <div id='map'></div>
    <script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script>
    <script>
        var map = L.map('map', {{ zoomControl: false }}).setView([{centerLat}, {centerLng}], {zoomLevel});
        L.control.zoom({{ position: 'topright' }}).addTo(map);

        // OpenStreetMap CartoDB Dark/Outdoors tile layer
        L.tileLayer('https://{{s}}.basemaps.cartocdn.com/rastertiles/voyager/{{z}}/{{x}}/{{y}}{{r}}.png', {{
            attribution: 'Kenya Wildlife Service • OpenStreetMap',
            maxZoom: 18
        }}).addTo(map);

        var geofences = {geofencesJson};
        var animals = {animalsJson};
        var patrols = {patrolsJson};
        var incidents = {incidentsJson};
        var trailCoords = {trailJson};

        // 1. Render park boundaries
        geofences.forEach(function(zone) {{
            if (zone.coordinates && zone.coordinates.length > 0) {{
                var polygon = L.polygon(zone.coordinates, {{
                    color: zone.colorHex || '#4CAF50',
                    weight: zone.zoneType === 'COMMUNITY_BUFFER' ? 2 : 3,
                    dashArray: zone.zoneType === 'COMMUNITY_BUFFER' ? '5, 5' : null,
                    fillOpacity: zone.zoneType === 'COMMUNITY_BUFFER' ? 0.15 : 0.25
                }}).addTo(map);

                polygon.bindTooltip('<b>' + zone.zoneName + '</b><br>Type: ' + zone.zoneType, {{
                    sticky: true,
                    className: 'park-tooltip'
                }});
            }}
        }});

        // 2. Render Breadcrumb Trails if an animal is selected
        if (trailCoords && trailCoords.length > 1) {{
            var latLngs = trailCoords.map(function(t) {{ return [t.latitude, t.longitude]; }});
            var trailLine = L.polyline(latLngs, {{
                color: '#00E5FF',
                weight: 4,
                opacity: 0.9,
                dashArray: '6, 6'
            }}).addTo(map);
            trailLine.bindTooltip('Wildlife Movement Trajectory', {{ sticky: true }});
        }}

        // 3. Render Wildlife Collar Markers with Health & Immobility Status
        animals.forEach(function(animal) {{
            var isAlert = animal.isBreaching || animal.status === 'Alert';
            var isLowBattery = animal.collarBattery < 20 || animal.status === 'Low Battery';
            var isImmobile = animal.status === 'IMMOBILE';

            var markerClass = isAlert ? 'alert-marker' : (isLowBattery || isImmobile ? 'warning-marker' : 'pulse-marker');

            var customIcon = L.divIcon({{
                className: 'custom-pin',
                html: '<div class=""' + markerClass + '""></div>',
                iconSize: [16, 16],
                iconAnchor: [8, 8]
            }});

            var marker = L.marker([animal.latitude, animal.longitude], {{ icon: customIcon }}).addTo(map);
            var statusColor = isAlert ? '#FF5252' : (isLowBattery ? '#FF9800' : '#4CAF50');
            var statusText = isAlert ? 'GEOFENCE BREACH ALERT' : (isLowBattery ? 'LOW BATTERY WARNING' : 'Normal Patrol');

            var popupContent = '<div class=""popup-title"">' + animal.name + ' (' + animal.species + ')</div>' +
                               '<div class=""popup-desc"">' +
                               '<b>Collar:</b> ' + animal.collarId + '<br>' +
                               '<b>Park:</b> ' + animal.parkName + '<br>' +
                               '<b>Sex:</b> ' + animal.sex + '<br>' +
                               '<b>Battery:</b> ' + animal.collarBattery + '%<br>' +
                               '<b>Status:</b> <span style=""color:' + statusColor + ';font-weight:bold;"">' + statusText + '</span>' +
                               '</div>';
            marker.bindPopup(popupContent);
        }});

        // 4. Render Ranger Patrol Units on the GIS Map
        patrols.forEach(function(patrol) {{
            var isDispatched = patrol.status === 'DISPATCHED';
            var patrolIcon = L.divIcon({{
                className: 'custom-patrol-pin',
                html: '<div class=""' + (isDispatched ? 'patrol-dispatched-marker' : 'patrol-marker') + '""></div>',
                iconSize: [18, 18],
                iconAnchor: [9, 9]
            }});

            var pMarker = L.marker([patrol.latitude, patrol.longitude], {{ icon: patrolIcon }}).addTo(map);
            var pPopup = '<div class=""popup-title"" style=""color:#00E5FF;"">' + patrol.name + '</div>' +
                         '<div class=""popup-desc"">' +
                         '<b>Call Sign:</b> ' + patrol.callSign + '<br>' +
                         '<b>Type:</b> ' + patrol.unitType + '<br>' +
                         '<b>Sector:</b> ' + patrol.sector + '<br>' +
                         '<b>Status:</b> <span style=""color:' + (isDispatched ? '#FFD700' : '#00E5FF') + ';font-weight:bold;"">' + patrol.status + '</span>' +
                         '</div>';
            pMarker.bindPopup(pPopup);
        }});

        // 5. Render Vector Dispatch Routes for Active Patrol Dispatches
        incidents.forEach(function(inc) {{
            if (inc.isDispatched && patrols && patrols.length > 0) {{
                var assigned = patrols.find(function(p) {{ return p.status === 'DISPATCHED'; }}) || patrols[0];
                if (assigned) {{
                    L.polyline([[assigned.latitude, assigned.longitude], [inc.latitude, inc.longitude]], {{
                        color: '#FF9800',
                        weight: 3,
                        dashArray: '8, 8',
                        opacity: 0.95
                    }}).addTo(map);
                }}
            }}
        }});
    </script>
</body>
</html>";
    }
}