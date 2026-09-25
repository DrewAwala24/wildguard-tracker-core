using frontend.Models;
using frontend.ViewModels;

namespace frontend;

public partial class ParkDetailPage : ContentPage
{
    private readonly ParkDetailViewModel _viewModel;

    public ParkDetailPage(ParkDetailViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel;
        BindingContext = _viewModel;

        _viewModel.PropertyChanged += (s, e) =>
        {
            if (e.PropertyName == nameof(ParkDetailViewModel.HtmlMapContent))
            {
                MainThread.BeginInvokeOnMainThread(() =>
                {
                    if (!string.IsNullOrEmpty(_viewModel.HtmlMapContent))
                    {
                        SectorMapView.Source = new HtmlWebViewSource { Html = _viewModel.HtmlMapContent };
                    }
                });
            }
        };
    }

    public async Task LoadParkAsync(string parkName, IEnumerable<AnimalDto>? animals = null, IEnumerable<PatrolUnitDto>? patrols = null)
    {
        await _viewModel.InitializeAsync(parkName, animals, patrols);
        if (!string.IsNullOrEmpty(_viewModel.HtmlMapContent))
        {
            MainThread.BeginInvokeOnMainThread(() =>
            {
                SectorMapView.Source = new HtmlWebViewSource { Html = _viewModel.HtmlMapContent };
            });
        }
    }
}
