using System;
using System.Diagnostics;
using System.IO;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Tasks;

namespace KwsDesktop.Services;

/// <summary>
/// Manages the lifecycle of the Java Spring Boot backend process.
/// Finds the JAR relative to this executable, starts it, and polls
/// TCP port 8080 to confirm the server is ready before proceeding.
/// </summary>
public class BackendLauncher
{
    private Process? _process;
    private const int BackendPort = 8080;
    private const int MaxWaitSeconds = 60;

    /// <summary>
    /// Locates the Spring Boot JAR, starts it, and waits for port 8080
    /// to become available. Returns true on success, false on timeout.
    /// </summary>
    public async Task<bool> StartAndWaitAsync(
        Action<string>? onStatusUpdate = null,
        CancellationToken cancellationToken = default)
    {
        // ── 1. Resolve JAR path ────────────────────────────────────────
        string jarPath = FindJar();
        if (!File.Exists(jarPath))
        {
            onStatusUpdate?.Invoke($"⚠️  JAR not found at expected path. Is the backend built?\n{jarPath}");
            // Wait anyway — backend may already be running
            return await WaitForPort(onStatusUpdate, cancellationToken);
        }

        // ── 2. Check if backend already running ────────────────────────
        if (await IsPortOpen())
        {
            onStatusUpdate?.Invoke("✅  Backend already running on port 8080.");
            return true;
        }

        // ── 3. Launch the Java process ─────────────────────────────────
        onStatusUpdate?.Invoke("🚀  Starting WildGuard Tracker backend…");

        var startInfo = new ProcessStartInfo
        {
            FileName = FindJava(),
            Arguments = $"-jar \"{jarPath}\"",
            WorkingDirectory = FindProjectRoot(),
            UseShellExecute = false,
            CreateNoWindow = true,
            RedirectStandardOutput = true,
            RedirectStandardError = true
        };

        _process = new Process { StartInfo = startInfo, EnableRaisingEvents = true };

        _process.OutputDataReceived += (_, e) =>
        {
            if (e.Data is not null && e.Data.Contains("Started BackendApplication"))
                onStatusUpdate?.Invoke("✅  Backend started successfully.");
        };

        _process.Exited += (_, _) =>
            onStatusUpdate?.Invoke("⚠️  Backend process exited unexpectedly.");

        _process.Start();
        _process.BeginOutputReadLine();
        _process.BeginErrorReadLine();

        // ── 4. Poll until the HTTP server is ready ─────────────────────
        return await WaitForPort(onStatusUpdate, cancellationToken);
    }

    public void Stop()
    {
        try
        {
            if (_process is { HasExited: false })
            {
                _process.Kill(entireProcessTree: true);
                _process.WaitForExit(3000);
            }
        }
        catch { /* ignore cleanup errors */ }
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private async Task<bool> WaitForPort(
        Action<string>? onStatusUpdate,
        CancellationToken ct)
    {
        onStatusUpdate?.Invoke("⏳  Waiting for backend to be ready…");
        var deadline = DateTime.UtcNow.AddSeconds(MaxWaitSeconds);

        while (DateTime.UtcNow < deadline && !ct.IsCancellationRequested)
        {
            if (await IsPortOpen())
            {
                onStatusUpdate?.Invoke("✅  Operations dashboard is ready.");
                return true;
            }
            await Task.Delay(1000, ct);
        }

        onStatusUpdate?.Invoke("⚠️  Backend may still be starting. Opening dashboard…");
        return false;
    }

    private static async Task<bool> IsPortOpen()
    {
        try
        {
            using var tcp = new TcpClient();
            await tcp.ConnectAsync("127.0.0.1", BackendPort);
            return true;
        }
        catch { return false; }
    }

    private static string FindJar()
    {
        string root = FindProjectRoot();
        return Path.Combine(root, "backend", "build", "libs", "backend-0.0.1-SNAPSHOT.jar");
    }

    private static string FindProjectRoot()
    {
        // Walk up from the executing assembly directory until we find the project root
        // (identified by the presence of 'backend/' and 'desktop-csharp/' folders)
        var dir = new DirectoryInfo(
            AppContext.BaseDirectory ?? Directory.GetCurrentDirectory());

        while (dir != null)
        {
            if (Directory.Exists(Path.Combine(dir.FullName, "backend")) &&
                Directory.Exists(Path.Combine(dir.FullName, "desktop-csharp")))
                return dir.FullName;
            dir = dir.Parent;
        }

        // Fallback: assume we're running from the KWS project root
        return Directory.GetCurrentDirectory();
    }

    private static string FindJava()
    {
        // Prefer JAVA_HOME env, then common paths, then just "java"
        string? javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
        if (!string.IsNullOrEmpty(javaHome))
        {
            string javaBin = Path.Combine(javaHome, "bin", "java");
            if (File.Exists(javaBin)) return javaBin;
        }

        foreach (string path in new[] { "/usr/bin/java", "/usr/local/bin/java" })
            if (File.Exists(path)) return path;

        return "java"; // rely on PATH
    }
}
