using System.Collections.ObjectModel;
using System.Diagnostics;
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

    public ObservableCollection<AnimalDto> Animals { get; } = new();
    public ObservableCollection<TelemetryLocation> TelemetryLogs { get; } = new();

    public ICommand LoadDataCommand { get; }

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
        set
        {
            if (_activeAnimalsCount != value)
            {
                _activeAnimalsCount = value;
                OnPropertyChanged();
            }
        }
    }

    public string TelemetryLogsCount
    {
        get => _telemetryLogsCount;
        set
        {
            if (_telemetryLogsCount != value)
            {
                _telemetryLogsCount = value;
                OnPropertyChanged();
            }
        }
    }

    public MainViewModel(ApiService apiService)
    {
        _apiService = apiService ?? throw new ArgumentNullException(nameof(apiService));
        LoadDataCommand = new Command(async () => await LoadDataAsync(), () => !IsRefreshing);

        // Do not execute unawaited async tasks in constructor.
        // Data loading is initiated cleanly by the page lifecycle (OnAppearing) or pull-to-refresh (RefreshView).
    }

    public async Task LoadDataAsync()
    {
        if (IsRefreshing) return;
        IsRefreshing = true;

        try
        {
            Debug.WriteLine("[MainViewModel] Loading wildlife and telemetry data...");

            var animalsTask = _apiService.GetAnimalsAsync();
            var telemetryTask = _apiService.GetTelemetryAsync();

            await Task.WhenAll(animalsTask, telemetryTask);

            var animals = await animalsTask;
            var telemetry = await telemetryTask;

            // Dispatch collection mutations to the UI thread to prevent WinUI/MAUI marshalling crashes
            await MainThread.InvokeOnMainThreadAsync(() =>
            {
                Animals.Clear();
                if (animals != null)
                {
                    foreach (var animal in animals)
                    {
                        Animals.Add(animal);
                    }
                }
                ActiveAnimalsCount = $"{Animals.Count} Tracked";

                TelemetryLogs.Clear();
                if (telemetry != null)
                {
                    foreach (var log in telemetry)
                    {
                        TelemetryLogs.Add(log);
                    }
                }
                TelemetryLogsCount = $"{TelemetryLogs.Count} Recorded";

                Debug.WriteLine($"[MainViewModel] Updated UI: {Animals.Count} animals, {TelemetryLogs.Count} telemetry logs.");
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
}