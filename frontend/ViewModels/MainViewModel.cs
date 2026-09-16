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

    // Registration Form Bindings
    private string _newAnimalName = string.Empty;
    private string _newAnimalSpecies = "African Elephant";
    private string _newCollarId = string.Empty;
    private string _newParkName = "Amboseli NP";
    private string _newAnimalSex = "Female";

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

    // Commands
    public ICommand LoadDataCommand { get; }
    public ICommand SelectParkCommand { get; }
    public ICommand SelectSpeciesCommand { get; }
    public ICommand DispatchPatrolCommand { get; }
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
        DispatchPatrolCommand = new Command<IncidentAlertDto>(DispatchPatrol);

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

            await Task.WhenAll(animalsTask, telemetryTask, geofencesTask);

            var animals = await animalsTask;
            var telemetry = await telemetryTask;
            var geofences = await geofencesTask;

            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                MasterAnimals.Clear();
                if (animals != null) MasterAnimals.AddRange(animals);

                MasterTelemetry.Clear();
                if (telemetry != null) MasterTelemetry.AddRange(telemetry);

                Geofences.Clear();
                if (geofences != null) Geofences.AddRange(geofences);

                EvaluateGeofenceBreaches();
                ApplyFilter();
                UpdateMapHtml();

                ActiveAnimalsCount = $"{MasterAnimals.Count} Monitored";
                TelemetryLogsCount = $"{MasterTelemetry.Count} Recorded";

                Debug.WriteLine($"[MainViewModel] Data ready: {MasterAnimals.Count} animals, {MasterTelemetry.Count} telemetry pings across Kenya.");
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

        var animalsQuery = MasterAnimals.AsEnumerable();
        if (SelectedPark != "All Kenya")
        {
            animalsQuery = animalsQuery.Where(a => a.ParkName.Equals(SelectedPark, StringComparison.OrdinalIgnoreCase));
        }

        if (SelectedSpecies != "All Species")
        {
            animalsQuery = animalsQuery.Where(a => a.Species.Contains(SelectedSpecies, StringComparison.OrdinalIgnoreCase));
        }

        var matchingAnimals = animalsQuery.ToList();
        foreach (var animal in matchingAnimals)
        {
            FilteredAnimals.Add(animal);
        }

        var matchingCollarIds = matchingAnimals.Select(a => a.CollarId).ToHashSet(StringComparer.OrdinalIgnoreCase);
        foreach (var log in MasterTelemetry)
        {
            if (SelectedPark == "All Kenya" || matchingCollarIds.Contains(log.AnimalId ?? string.Empty))
            {
                FilteredTelemetryLogs.Add(log);
            }
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
                    ZoneName = "Kimana Agricultural Buffer Zone",
                    Severity = "CRITICAL",
                    Message = $"{animal.Species} '{animal.Name}' ({animal.CollarId}) crossed southern boundary into community buffer.",
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

    private void DispatchPatrol(IncidentAlertDto alert)
    {
        if (alert == null) return;

        alert.IsDispatched = true;
        alert.DispatchedPatrolUnit = $"KWS Quick-Response Unit (Sector {alert.ParkName})";
        OnPropertyChanged(nameof(ActiveIncidents));
        Debug.WriteLine($"[MainViewModel] Dispatched rapid patrol team for incident: {alert.Message}");
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
            Name = NewAnimalName.Trim(),
            Species = NewAnimalSpecies,
            CollarId = NewCollarId.Trim().ToUpperInvariant(),
            ParkName = NewParkName,
            Sex = NewAnimalSex,
            CollarBattery = 100,
            Latitude = lat,
            Longitude = lng,
            Status = "Active",
            IsBreaching = false
        };

        // Post to backend asynchronously
        _ = _apiService.RegisterAnimalAsync(newAnimal);

        // Add to local UI collections
        MasterAnimals.Insert(0, newAnimal);
        MasterTelemetry.Insert(0, new TelemetryLocation
        {
            Id = MasterTelemetry.Count + 100,
            AnimalId = newAnimal.CollarId,
            Latitude = newAnimal.Latitude,
            Longitude = newAnimal.Longitude,
            Timestamp = DateTime.UtcNow
        });

        ActiveAnimalsCount = $"{MasterAnimals.Count} Monitored";
        TelemetryLogsCount = $"{MasterTelemetry.Count} Recorded";

        ApplyFilter();
        UpdateMapHtml();

        IsRegistrationModalOpen = false;
        Debug.WriteLine($"[MainViewModel] Successfully registered {newAnimal.Name} to {newAnimal.ParkName}");
    }

    private void UpdateMapHtml()
    {
        // Focus coordinates based on SelectedPark
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

        var animalsJson = JsonSerializer.Serialize(animalsToRender);
        var geofencesJson = JsonSerializer.Serialize(geofencesToRender);

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

        // Render park boundaries
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

        // Render wildlife collar markers
        animals.forEach(function(animal) {{
            var isAlert = animal.isBreaching || animal.status === 'Alert';
            var customIcon = L.divIcon({{
                className: 'custom-pin',
                html: '<div class=""' + (isAlert ? 'alert-marker' : 'pulse-marker') + '""></div>',
                iconSize: [16, 16],
                iconAnchor: [8, 8]
            }});

            var marker = L.marker([animal.latitude, animal.longitude], {{ icon: customIcon }}).addTo(map);
            var popupContent = '<div class=""popup-title"">' + animal.name + ' (' + animal.species + ')</div>' +
                               '<div class=""popup-desc"">' +
                               '<b>Collar:</b> ' + animal.collarId + '<br>' +
                               '<b>Park:</b> ' + animal.parkName + '<br>' +
                               '<b>Battery:</b> ' + animal.collarBattery + '%<br>' +
                               '<b>Status:</b> <span style=""color:' + (isAlert ? '#FF5252' : '#4CAF50') + '"">' + (isAlert ? 'GEOFENCE ALERT' : 'Normal Patrol') + '</span>' +
                               '</div>';
            marker.bindPopup(popupContent);
        }});
    </script>
</body>
</html>";
    }
}