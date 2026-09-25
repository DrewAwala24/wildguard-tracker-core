using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Text.Json.Serialization;

namespace frontend.Models
{
    public class AnimalDto : INotifyPropertyChanged
    {
        public event PropertyChangedEventHandler? PropertyChanged;

        private long _id;
        private string _name = string.Empty;
        private string _species = string.Empty;
        private string _collarId = string.Empty;
        private string _parkName = "Tsavo East NP";
        private string _sex = "Unknown";
        private int _collarBattery = 95;
        private double _latitude;
        private double _longitude;
        private string _status = "Active";
        private bool _isBreaching;

        [JsonPropertyName("id")]
        public long Id
        {
            get => _id;
            set => SetField(ref _id, value);
        }

        [JsonPropertyName("name")]
        public string Name
        {
            get => _name;
            set => SetField(ref _name, value);
        }

        [JsonPropertyName("species")]
        public string Species
        {
            get => _species;
            set => SetField(ref _species, value);
        }

        [JsonPropertyName("collarId")]
        public string CollarId
        {
            get => _collarId;
            set => SetField(ref _collarId, value);
        }

        [JsonPropertyName("parkName")]
        public string ParkName
        {
            get => _parkName;
            set => SetField(ref _parkName, value);
        }

        [JsonPropertyName("sex")]
        public string Sex
        {
            get => _sex;
            set => SetField(ref _sex, value);
        }

        [JsonPropertyName("collarBattery")]
        public int CollarBattery
        {
            get => _collarBattery;
            set => SetField(ref _collarBattery, value);
        }

        [JsonPropertyName("latitude")]
        public double Latitude
        {
            get => _latitude;
            set
            {
                if (SetField(ref _latitude, value))
                    OnPropertyChanged(nameof(CoordinateDisplay));
            }
        }

        [JsonPropertyName("longitude")]
        public double Longitude
        {
            get => _longitude;
            set
            {
                if (SetField(ref _longitude, value))
                    OnPropertyChanged(nameof(CoordinateDisplay));
            }
        }

        [JsonPropertyName("status")]
        public string Status
        {
            get => _status;
            set => SetField(ref _status, value);
        }

        [JsonPropertyName("isBreaching")]
        public bool IsBreaching
        {
            get => _isBreaching;
            set => SetField(ref _isBreaching, value);
        }

        private double _speedKmh = 0.0;
        [JsonPropertyName("speedKmh")]
        public double SpeedKmh
        {
            get => _speedKmh;
            set
            {
                if (SetField(ref _speedKmh, value))
                    OnPropertyChanged(nameof(SpeedDisplay));
            }
        }

        public string SpeedDisplay => $"{SpeedKmh:F1} km/h";

        private string _headingCompass = "N 0°";
        [JsonPropertyName("headingCompass")]
        public string HeadingCompass
        {
            get => _headingCompass;
            set => SetField(ref _headingCompass, value);
        }

        private string _behaviorState = "🌿 Foraging";
        [JsonPropertyName("behaviorState")]
        public string BehaviorState
        {
            get => _behaviorState;
            set => SetField(ref _behaviorState, value);
        }

        private string _fenceDistance = "Safe in Sanctuary";
        [JsonPropertyName("fenceDistance")]
        public string FenceDistance
        {
            get => _fenceDistance;
            set => SetField(ref _fenceDistance, value);
        }

        private string _predictedCorridor = "Scanning forward corridor...";
        [JsonPropertyName("predictedCorridor")]
        public string PredictedCorridor
        {
            get => _predictedCorridor;
            set => SetField(ref _predictedCorridor, value);
        }

        private string _solarVoltageDisplay = "☀️ 14.1V (Solar Float)";
        [JsonPropertyName("solarVoltageDisplay")]
        public string SolarVoltageDisplay
        {
            get => _solarVoltageDisplay;
            set => SetField(ref _solarVoltageDisplay, value);
        }

        private string _collarTempDisplay = "🌡️ 27°C (Nominal)";
        [JsonPropertyName("collarTempDisplay")]
        public string CollarTempDisplay
        {
            get => _collarTempDisplay;
            set => SetField(ref _collarTempDisplay, value);
        }

        private string _signalStrength = "🛰️ -82 dBm (Iridium SAT)";
        [JsonPropertyName("signalStrength")]
        public string SignalStrength
        {
            get => _signalStrength;
            set => SetField(ref _signalStrength, value);
        }

        public string CoordinateDisplay => $"{Latitude:F4}°, {Longitude:F4}°";

        private bool SetField<T>(ref T field, T value, [CallerMemberName] string? propertyName = null)
        {
            if (EqualityComparer<T>.Default.Equals(field, value))
                return false;
            field = value;
            OnPropertyChanged(propertyName);
            return true;
        }

        private void OnPropertyChanged([CallerMemberName] string? propertyName = null)
            => PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));
    }
}
