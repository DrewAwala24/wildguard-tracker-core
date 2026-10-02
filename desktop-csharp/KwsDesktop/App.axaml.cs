using Avalonia;
using Avalonia.Controls.ApplicationLifetimes;
using Avalonia.Markup.Xaml;
using KwsDesktop.Views;
using KwsDesktop.Services;

using AvaloniaWebView;

namespace KwsDesktop;

public class App : Application
{
    private BackendLauncher? _backend;

    public override void RegisterServices()
    {
        base.RegisterServices();
        AvaloniaWebViewBuilder.Initialize(default);
    }

    public override void Initialize()
    {
        AvaloniaXamlLoader.Load(this);
    }

    public override async void OnFrameworkInitializationCompleted()
    {
        if (ApplicationLifetime is IClassicDesktopStyleApplicationLifetime desktop)
        {
            // Show splash while backend starts
            var splash = new SplashWindow();
            desktop.MainWindow = splash;
            splash.Show();

            // Launch the Java Spring Boot backend
            _backend = new BackendLauncher();
            bool started = await _backend.StartAndWaitAsync(
                onStatusUpdate: msg => splash.UpdateStatus(msg)
            );

            // Open main window regardless (backend might already be running)
            var mainWindow = new MainWindow();
            desktop.MainWindow = mainWindow;
            mainWindow.Show();
            splash.Close();

            // Kill backend on app exit
            desktop.ShutdownRequested += (_, _) => _backend?.Stop();
        }

        base.OnFrameworkInitializationCompleted();
    }
}
