using frontend.Services;
using frontend.ViewModels;
using Microsoft.Extensions.Logging;

namespace frontend
{
    public static class MauiProgram
    {
        public static MauiApp CreateMauiApp()
        {
            var builder = MauiApp.CreateBuilder();
            builder
                .UseMauiApp<App>()
                .ConfigureFonts(fonts =>
                {
                    // Existing Montserrat fonts bundled in Resources/Fonts
                    fonts.AddFont("Montserrat-VariableFont_wght.ttf", "MontserratRegular");
                    fonts.AddFont("Montserrat-VariableFont_wght.ttf", "MontserratBold");
                    fonts.AddFont("Montserrat-Italic-VariableFont_wght.ttf", "MontserratItalic");
                });

#if DEBUG
            builder.Logging.AddDebug();
#endif

            // Register Dependency Injection Services
            builder.Services.AddSingleton<HttpClient>();
            builder.Services.AddSingleton<ApiService>();
            builder.Services.AddSingleton<MainViewModel>();
            builder.Services.AddTransient<MainPage>();
            builder.Services.AddTransient<ParkDetailViewModel>();
            builder.Services.AddTransient<ParkDetailPage>();

            return builder.Build();
        }
    }
}
