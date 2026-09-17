using System.Globalization;

namespace frontend.Converters;

/// <summary>
/// Converts an animal Name string to a bundled MauiImage ImageSource.
///
/// Lookup strategy (in priority order):
///   1. Full name match  e.g. "Mutula Bull"   → mutula_bull.jpg
///   2. Each individual word match e.g. "Satao Pride Leader" → satao.jpg
///   3. Fallback                               → kws_logo.jpg
///
/// All asset filenames must be lowercase alphanumeric+underscore (MAUI Resizetizer rule).
/// </summary>
public class AnimalImageConverter : IValueConverter
{
    private static readonly Dictionary<string, string> _knownImages = new(StringComparer.OrdinalIgnoreCase)
    {
        // Full-name or primary-name keys → normalised asset filename
        { "baraka",      "baraka.jpg" },
        { "baraka_rh",   "baraka_rh.jpg" },
        { "kibo",        "kibo.jpg" },
        { "kipsing",     "kipsing.jpg" },
        { "mukurwe",     "mukurwe.jpg" },
        { "mutula",      "mutula_bull.jpg" },
        { "mutula bull", "mutula_bull.jpg" },
        { "satao",       "satao.jpg" },
        { "simba",       "simba.jpg" },
        { "talek",       "talek.jpg" },
        { "zuri",        "zuri.jpg" },
    };

    private const string FallbackImage = "kws_logo.jpg";

    public object? Convert(object? value, Type targetType, object? parameter, CultureInfo culture)
    {
        if (value is string name && !string.IsNullOrWhiteSpace(name))
        {
            var trimmed = name.Trim();

            // 1. Full name lookup (handles "Mutula Bull" directly)
            if (_knownImages.TryGetValue(trimmed, out var fullMatch))
                return ImageSource.FromFile(fullMatch);

            // 2. Check each word in the name — first match wins
            //    e.g. "Satao Pride Leader" → words ["Satao","Pride","Leader"] → "satao" hits
            //    e.g. "Baraka Rhino" → "baraka" hits
            //    e.g. "Mukurwe Black Rhino" → "mukurwe" hits
            //    e.g. "Talek River Female" → "talek" hits
            //    e.g. "Kipsing Male" → "kipsing" hits
            foreach (var word in trimmed.Split(' ', StringSplitOptions.RemoveEmptyEntries))
            {
                if (_knownImages.TryGetValue(word, out var wordMatch))
                    return ImageSource.FromFile(wordMatch);
            }
        }

        return ImageSource.FromFile(FallbackImage);
    }

    public object? ConvertBack(object? value, Type targetType, object? parameter, CultureInfo culture)
        => throw new NotSupportedException();
}
