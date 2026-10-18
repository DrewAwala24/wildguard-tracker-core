!/usr/bin/env bash
# ═══════════════════════════════════════════════════════════════
#  KWS WildGuard Tracker — Linux Desktop Operations Launcher
#  Stack: Java 17+ (Spring Boot + PostGIS + Native Operations GUI)
#  Platform: Linux Desktop (Ubuntu / Debian / Fedora / X11 / Wayland)
# ═══════════════════════════════════════════════════════════════

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
BACKEND_JAR="$BACKEND_DIR/build/libs/backend-0.0.1-SNAPSHOT.jar"


echo "  ╔══════════════════════════════════════════════════════════════╗"
echo "  ║  🦁  KENYA WILDLIFE SERVICE — WildGuard Operations Command  ║"
echo "  ║         Native Desktop Client & Spatial Telemetry Engine     ║"
echo "  ║                   Powered by Java 17+ & PostGIS              ║"
echo "  ╚══════════════════════════════════════════════════════════════╝"
echo ""

# ── Step 1: Verify Java runtime ───────────────────────────────────
if ! command -v java &>/dev/null; then
  echo "  [ERROR] Java is not installed or not in PATH."
  echo "          Please install Java 17+ (e.g. sudo apt install openjdk-17-jre)."
  exit 1
fi

JAVA_VER=$(java -version 2>&1 | head -1)
echo "  [RUNTIME] Using $JAVA_VER"

# ── Step 2: Build backend JAR if missing or sources updated ───────
if [ ! -f "$BACKEND_JAR" ]; then
  echo "  [BUILD] Backend JAR not found — building with Gradle..."
  cd "$BACKEND_DIR"
  ./gradlew bootJar --quiet
  cd "$SCRIPT_DIR"
  echo "  [BUILD] ✅ WildGuard Core Engine built successfully."
else
  echo "  [BUILD] ✅ Found existing executable JAR ($BACKEND_JAR)."
fi

# ── Step 3: Check PostgreSQL connection (advisory) ────────────────
if command -v nc &>/dev/null; then
  if ! nc -z -w2 127.0.0.1 5432 2>/dev/null; then
    echo "  [NOTICE] PostgreSQL port 5432 is not responding."
    echo "           Ensure PostgreSQL/PostGIS is running: sudo systemctl start postgresql"
  fi
fi

echo ""
echo "  [LAUNCH] Starting KWS WildGuard Operations Desktop…"
echo "           • Desktop Operations Window will appear on screen"
echo "           • Operations Console will auto-open at http://localhost:8080"
echo "           • Press Ctrl+C in this terminal or click Exit in the app to stop."
echo ""

# ── Step 4: Execute Native Java Desktop App ────────────────────────
exec java -jar "$BACKEND_JAR"
