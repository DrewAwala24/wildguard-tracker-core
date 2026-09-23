using System.Diagnostics;
using frontend.Converters;
using frontend.Models;
using frontend.Services;
using frontend.ViewModels;
using Microsoft.Extensions.DependencyInjection;

namespace frontend;

public partial class MainPage : ContentPage
{
    private readonly MainViewModel _viewModel;
    private readonly AnimalImageConverter _imageConverter = new();
    private AnimalDto? _currentDetailAnimal;
    private bool _isPanelOpen;
    private bool _opsMapReady;

    // Primary DI Constructor
    public MainPage(MainViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel ?? throw new ArgumentNullException(nameof(viewModel));
        BindingContext = _viewModel;

        // Wire the SelectAnimalDetailCommand so card taps open the detail panel
        _viewModel.SelectAnimalDetailCommand = new Command<AnimalDto>(async animal =>
            await OpenDetailPanelAsync(animal));
        _viewModel.InjectLiveMapScript = InjectLiveMapScriptAsync;
    }

    // Safe fallback constructor for XAML inflator, previewer, or parameterless Shell resolution
    public MainPage() : this(
        Application.Current?.Handler?.MauiContext?.Services.GetService<MainViewModel>()
        ?? IPlatformApplication.Current?.Services?.GetService<MainViewModel>()
        ?? new MainViewModel(new ApiService(new HttpClient())))
    {
    }

    protected override void OnAppearing()
    {
        base.OnAppearing();
        _viewModel.InjectLiveMapScript = InjectLiveMapScriptAsync;

        if (_viewModel.MasterAnimals.Count == 0 && _viewModel.LoadDataCommand.CanExecute(null))
        {
            _viewModel.LoadDataCommand.Execute(null);
        }
    }

    protected override void OnDisappearing()
    {
        base.OnDisappearing();
    }

    private void OnOpsMapNavigated(object? sender, WebNavigatedEventArgs e)
    {
        _opsMapReady = e.Result == WebNavigationResult.Success;
    }

    private async Task InjectLiveMapScriptAsync(string script)
    {
        if (!_opsMapReady || string.IsNullOrWhiteSpace(script) || OpsMapWebView.Handler == null)
        {
            return;
        }

        try
        {
            await OpsMapWebView.EvaluateJavaScriptAsync(script);
        }
        catch (Exception ex)
        {
            Debug.WriteLine($"[MainPage] Live map script injection failed: {ex.Message}");
        }

        if (_currentDetailAnimal != null)
        {
            DetailCoordinates.Text = $"{_currentDetailAnimal.Latitude:F5}°N, {_currentDetailAnimal.Longitude:F5}°E";
        }
    }

    // ────────────────────────────────────────────────────────────────────
    // Detail Panel: Open
    // ────────────────────────────────────────────────────────────────────
    private async Task OpenDetailPanelAsync(AnimalDto? animal)
    {
        if (animal == null) return;
        _currentDetailAnimal = animal;

        // Populate panel labels
        DetailAnimalName.Text      = animal.Name;
        DetailSpecies.Text         = animal.Species;
        DetailCollarId.Text        = animal.CollarId;
        DetailSex.Text             = animal.Sex;
        DetailPark.Text            = animal.ParkName;
        DetailBattery.Text         = $"🔋 {animal.CollarBattery}%";
        DetailCoordinates.Text     = $"{animal.Latitude:F5}°N, {animal.Longitude:F5}°E";

        // Status colour
        DetailStatus.Text          = animal.IsBreaching ? "⚠ BREACHING" : animal.Status;
        DetailStatus.TextColor     = animal.IsBreaching
            ? Color.FromArgb("#FF5252")
            : Color.FromArgb("#4CAF50");

        // Resolve photo via converter
        DetailAnimalPhoto.Source   = _imageConverter.Convert(animal.Name, typeof(ImageSource), null,
            System.Globalization.CultureInfo.CurrentCulture) as ImageSource;

        // Show scrim + panel (start off-screen to the right)
        DetailPanel.TranslationX  = 320;
        DetailPanelScrim.IsVisible = true;
        DetailPanel.IsVisible      = true;
        _isPanelOpen               = true;

        // Animate panel sliding in
        await DetailPanel.TranslateToAsync(0, 0, 280, Easing.CubicOut);
    }

    // ────────────────────────────────────────────────────────────────────
    // Detail Panel: Close
    // ────────────────────────────────────────────────────────────────────
    private async Task CloseDetailPanelAsync()
    {
        if (!_isPanelOpen) return;
        _isPanelOpen = false;

        await DetailPanel.TranslateToAsync(320, 0, 220, Easing.CubicIn);

        DetailPanel.IsVisible      = false;
        DetailPanelScrim.IsVisible = false;
    }

    // Close via ✕ button
    private async void OnDetailPanelClose(object? sender, EventArgs e)
        => await CloseDetailPanelAsync();

    // Close by tapping the dark scrim behind panel
    private async void OnDetailPanelScrimTapped(object? sender, TappedEventArgs e)
        => await CloseDetailPanelAsync();

    // ────────────────────────────────────────────────────────────────────
    // "View Trajectory Trail" button inside the detail panel
    // ────────────────────────────────────────────────────────────────────
    private async void OnViewTrajectoryTapped(object? sender, TappedEventArgs e)
    {
        if (_currentDetailAnimal == null) return;

        // Delegate to the ViewModel's existing trail logic
        if (_viewModel.SelectAnimalTrailCommand?.CanExecute(_currentDetailAnimal) == true)
        {
            _viewModel.SelectAnimalTrailCommand.Execute(_currentDetailAnimal);
        }

        await CloseDetailPanelAsync();
    }
}