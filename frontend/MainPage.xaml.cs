using frontend.Services;
using frontend.ViewModels;
using Microsoft.Extensions.DependencyInjection;

namespace frontend;

public partial class MainPage : ContentPage
{
    private readonly MainViewModel _viewModel;

    // Primary DI Constructor
    public MainPage(MainViewModel viewModel)
    {
        InitializeComponent();
        _viewModel = viewModel ?? throw new ArgumentNullException(nameof(viewModel));
        BindingContext = _viewModel;
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

        if (BindingContext is MainViewModel vm)
        {
            if (vm.LoadDataCommand.CanExecute(null))
            {
                vm.LoadDataCommand.Execute(null);
            }
        }
    }
}