#!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  KWS WildGuard Tracker — Desktop Launcher
#  Language Stack: Java 17+ (Spring Boot) + C# .NET 8 (Avalonia)
#  Platform: Linux (WebKit2GTK) / Windows (WebView2) / macOS
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_JAR="$SCRIPT_DIR/backend/build/libs/backend-0.0.1-SNAPSHOT.jar"
CSHARP_DIR="$SCRIPT_DIR/desktop-csharp/KwsDesktop"

echo ""
echo "  ╔══════════════════════════════════════════════════╗"
echo "  ║  🦁  KWS WildGuard Tracker — Operations Command  ║"
echo "  ║      Java Spring Boot  +  C# Avalonia UI         ║"
echo "  ╚══════════════════════════════════════════════════╝"
echo ""

# ── Step 1: Build backend if JAR is missing ──────────────────────
if [ ! -f "$BACKEND_JAR" ]; then
  echo "  [BUILD] Spring Boot JAR not found — building backend..."
  cd "$SCRIPT_DIR/backend"
  ./gradlew bootJar --quiet
  cd "$SCRIPT_DIR"
  echo "  [BUILD] ✅ Backend built successfully."
fi

# ── Step 2: Build / restore the C# Avalonia desktop app ──────────
echo "  [BUILD] Restoring C# Avalonia UI packages..."
cd "$CSHARP_DIR"
dotnet restore -v q 2>&1 | tail -3

echo "  [BUILD] Building KwsDesktop (C# / Avalonia)..."
dotnet build --configuration Release --no-restore -v q 2>&1 | tail -5

echo ""
echo "  [LAUNCH] Starting KWS WildGuard desktop application..."
echo "           The application window will open shortly."
echo "           Ctrl+C to stop all processes."
echo ""

# ── Step 3: Launch the C# desktop app (it starts the Java backend) ─
dotnet run --configuration Release --no-build
