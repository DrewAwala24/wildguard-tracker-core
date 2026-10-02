using Avalonia;
using Avalonia.Controls;
using Avalonia.Threading;

namespace KwsDesktop.Views;

public partial class SplashWindow : Window
{
    public SplashWindow()
    {
        InitializeComponent();
    }

    /// <summary>
    /// Thread-safe status update from the BackendLauncher.
    /// </summary>
    public void UpdateStatus(string message)
    {
        Dispatcher.UIThread.Post(() =>
        {
            if (StatusLabel is not null)
                StatusLabel.Text = message;
        });
    }
}
