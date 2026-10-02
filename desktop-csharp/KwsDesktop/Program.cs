using Avalonia;
using Avalonia.Fonts.Inter;
using Avalonia.WebView.Desktop;
using KwsDesktop;

AppBuilder
    .Configure<App>()
    .UsePlatformDetect()
    .WithInterFont()
    .UseDesktopWebView()
    .StartWithClassicDesktopLifetime(args);
