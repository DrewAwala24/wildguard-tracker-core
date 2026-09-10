using System.Collections.ObjectModel;
using System.Windows.Input;
using frontend.Models;
using frontend.Services;

namespace frontend.ViewModels;

public class MainViewModel : BindableObject
{
    private readonly ApiService _apiService;
    private bool _isRefreshing;

    public ObservableCollection<AnimalDto> Animals { get; set; } = new();
    public ObservableCollection<TelemetryLocation> TelemetryLogs { get; set; } = new();

    public ICommand LoadDataCommand { get; }

    public bool IsRefreshing
    {
        get => _isRefreshing;
        set { _isRefreshing = value; OnPropertyChanged(); }
    }

    public MainViewModel(ApiService apiService)
    {
        _apiService = apiService;
        LoadDataCommand = new Command(async () => await LoadDataAsync());

        // Initial load
        _ = LoadDataAsync();
    }

    private async Task LoadDataAsync()
    {
        if (IsRefreshing) return;
        IsRefreshing = true;

        try
        {
            var animals = await _apiService.GetAnimalsAsync();
            Animals.Clear();
            if (animals != null)
            {
                foreach (var animal in animals)
                    Animals.Add(animal);
            }

            var telemetry = await _apiService.GetTelemetryAsync();
            TelemetryLogs.Clear();
            if (telemetry != null)
            {
                foreach (var log in telemetry)
                    TelemetryLogs.Add(log);
            }
        }
        catch (Exception ex)
        {
            System.Diagnostics.Debug.WriteLine($"Error loading data: {ex.Message}");
        }
        finally
        {
            IsRefreshing = false;
        }
    }
}