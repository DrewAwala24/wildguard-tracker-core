package com.kws.backend.backend.desktop.view;

import com.kws.backend.backend.dto.AnimalDto;
import com.kws.backend.backend.dto.PatrolUnitDto;
import com.kws.backend.backend.dto.TelemetryTrailDto;
import com.kws.backend.backend.model.EcologicalLandmark;
import com.kws.backend.backend.model.GeofenceZone;
import com.kws.backend.backend.model.IncidentLog;
import javafx.animation.AnimationTimer;
import javafx.geometry.Point2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import java.util.*;
import java.util.function.Consumer;

/**
 * Native JavaFX High-Performance GIS Vector Map Renderer for KWS WildGuard Tracker.
 * Renders PostGIS boundaries, Kenya sovereign border, sanctuary geofences,
 * Nairobi electric fence, ecological landmarks, collar telemetry pins, and historical tracks.
 */
public class GisMapCanvas extends Canvas {

    // Projection & Viewport State
    private double centerLng = 37.50; // Central Kenya longitude
    private double centerLat = -0.50; // Central Kenya latitude
    private double zoomScale = 140.0; // Pixels per degree

    private double dragStartX, dragStartY;
    private double dragCenterLng, dragCenterLat;

    // Tactical Layers & State
    private boolean layerParks = true;
    private boolean layerBuffers = true;
    private boolean layerElectricFence = true;
    private boolean layerLandmarks = true;
    private boolean layerWildlife = true;
    private boolean layerTrails = true;
    private boolean layerPatrols = true;
    private boolean layerIncidents = true;
    private boolean nightOpsMode = true;

    // Data Models
    private List<GeofenceZone> geofenceZones = new ArrayList<>();
    private List<EcologicalLandmark> landmarks = new ArrayList<>();
    private List<AnimalDto> animals = new ArrayList<>();
    private List<PatrolUnitDto> patrolUnits = new ArrayList<>();
    private List<IncidentLog> incidents = new ArrayList<>();
    private Map<String, List<TelemetryTrailDto>> animalTrails = new HashMap<>();

    private String selectedCollarId = null;
    private AnimalDto hoveredAnimal = null;
    private double mouseX = -1, mouseY = -1;
    private Consumer<AnimalDto> animalSelectionListener;

    // Animation Pulse Timer
    private double pulsePhase = 0;
    private final AnimationTimer animationTimer;

    // Kenya Sovereign Border Approximation Coordinates
    private static final double[][] KENYA_BORDER_COORDS = {
            {34.0, 4.4}, {35.9, 4.6}, {36.0, 4.4}, {36.8, 3.6}, {38.8, 3.4},
            {39.2, 4.0}, {40.0, 4.0}, {41.9, 3.9}, {41.9, 2.0}, {41.0, 1.0},
            {41.0, 0.0}, {41.6, -1.6}, {40.7, -2.3}, {40.1, -2.9}, {39.6, -4.7},
            {39.2, -4.7}, {37.6, -3.2}, {37.2, -3.0}, {37.0, -3.1}, {36.0, -2.0},
            {35.0, -1.5}, {34.0, -1.0}, {34.0, 0.1}, {34.2, 1.0}, {34.8, 2.0},
            {34.9, 3.0}, {34.0, 4.4}
    };

    public GisMapCanvas(double width, double height) {
        super(width, height);
        initInteractions();

        animationTimer = new AnimationTimer() {
            private long lastUpdate = 0;
            @Override
            public void handle(long now) {
                if (now - lastUpdate >= 50_000_000) { // 20 FPS pulse animation
                    pulsePhase = (pulsePhase + 0.1) % (Math.PI * 2);
                    redraw();
                    lastUpdate = now;
                }
            }
        };
        animationTimer.start();
    }

    private void initInteractions() {
        setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragStartX = e.getX();
                dragStartY = e.getY();
                dragCenterLng = centerLng;
                dragCenterLat = centerLat;
            }
        });

        setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                double deltaX = e.getX() - dragStartX;
                double deltaY = e.getY() - dragStartY;
                centerLng = dragCenterLng - (deltaX / zoomScale);
                centerLat = dragCenterLat + (deltaY / zoomScale);
                clampCoordinates();
                redraw();
            }
        });

        setOnScroll((ScrollEvent e) -> {
            double zoomFactor = (e.getDeltaY() > 0) ? 1.15 : 0.85;
            double mouseLng = screenToLng(e.getX());
            double mouseLat = screenToLat(e.getY());

            zoomScale = Math.max(70.0, Math.min(2500.0, zoomScale * zoomFactor));

            // Zoom centered on cursor
            centerLng = mouseLng - (e.getX() - getWidth() / 2.0) / zoomScale;
            centerLat = mouseLat + (e.getY() - getHeight() / 2.0) / zoomScale;

            clampCoordinates();
            redraw();
        });

        setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
            findHoveredAnimal(mouseX, mouseY);
            redraw();
        });

        setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && hoveredAnimal != null) {
                selectedCollarId = hoveredAnimal.getCollarId();
                if (animalSelectionListener != null) {
                    animalSelectionListener.accept(hoveredAnimal);
                }
                redraw();
            }
        });
    }

    private void clampCoordinates() {
        centerLng = Math.max(30.0, Math.min(45.0, centerLng));
        centerLat = Math.max(-7.0, Math.min(7.0, centerLat));
    }

    public void setAnimalSelectionListener(Consumer<AnimalDto> listener) {
        this.animalSelectionListener = listener;
    }

    public void setSelectedCollarId(String collarId) {
        this.selectedCollarId = collarId;
        redraw();
    }

    public void centerOn(double lat, double lng, double targetZoom) {
        this.centerLat = lat;
        this.centerLng = lng;
        if (targetZoom > 0) {
            this.zoomScale = targetZoom;
        }
        clampCoordinates();
        redraw();
    }

    public void resetToKenya() {
        this.centerLng = 37.50;
        this.centerLat = -0.50;
        this.zoomScale = 140.0;
        redraw();
    }

    public void zoomIn() {
        this.zoomScale = Math.min(2500.0, zoomScale * 1.3);
        redraw();
    }

    public void zoomOut() {
        this.zoomScale = Math.max(70.0, zoomScale * 0.7);
        redraw();
    }

    public void setNightOpsMode(boolean nightOps) {
        this.nightOpsMode = nightOps;
        redraw();
    }

    public void setLayers(boolean parks, boolean buffers, boolean electricFence, boolean landmarks, boolean wildlife, boolean trails, boolean patrols, boolean incidents) {
        this.layerParks = parks;
        this.layerBuffers = buffers;
        this.layerElectricFence = electricFence;
        this.layerLandmarks = landmarks;
        this.layerWildlife = wildlife;
        this.layerTrails = trails;
        this.layerPatrols = patrols;
        this.layerIncidents = incidents;
        redraw();
    }

    public void updateData(List<GeofenceZone> zones, List<EcologicalLandmark> landmarks,
                           List<AnimalDto> animals, List<PatrolUnitDto> patrols,
                           List<IncidentLog> incidents, Map<String, List<TelemetryTrailDto>> trails) {
        this.geofenceZones = zones != null ? zones : new ArrayList<>();
        this.landmarks = landmarks != null ? landmarks : new ArrayList<>();
        this.animals = animals != null ? animals : new ArrayList<>();
        this.patrolUnits = patrols != null ? patrols : new ArrayList<>();
        this.incidents = incidents != null ? incidents : new ArrayList<>();
        this.animalTrails = trails != null ? trails : new HashMap<>();
        redraw();
    }

    public void redraw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = getGraphicsContext2D();

        // 1. Background Fill
        if (nightOpsMode) {
            gc.setFill(Color.web("#0B0F17")); // Obsidian Dark
        } else {
            gc.setFill(Color.web("#1A2434")); // Tactical Day Dark
        }
        gc.fillRect(0, 0, w, h);

        // 2. Latitude / Longitude Tactical Grid
        drawTacticalGrid(gc, w, h);

        // 3. Kenya Sovereign Boundary Polygon
        drawKenyaSovereignBorder(gc);

        // 4. PostGIS Geofence Vector Polygons
        if (layerParks || layerBuffers) {
            drawPostGisGeofences(gc);
        }

        // 5. Nairobi NP Northern Electric Fence Line
        if (layerElectricFence) {
            drawNairobiElectricFence(gc);
        }

        // 6. Ecological Landmarks
        if (layerLandmarks) {
            drawLandmarks(gc);
        }

        // 7. Telemetry Historical Trails
        if (layerTrails) {
            drawTrails(gc);
        }

        // 8. Patrol Units & Dispatch Vectors
        if (layerPatrols) {
            drawPatrolUnits(gc);
        }

        // 9. Active Incident Risk Beacons
        if (layerIncidents) {
            drawIncidentBeacons(gc);
        }

        // 10. Wildlife Telemetry Pins
        if (layerWildlife) {
            drawWildlifePins(gc);
        }

        // 11. Tooltip for Hovered Animal
        if (hoveredAnimal != null) {
            drawHoveredAnimalCard(gc, hoveredAnimal, mouseX, mouseY);
        }

        // 12. Tactical HUD & Coordinates Readout
        drawTacticalHud(gc, w, h);
    }

    // ── Coordinate Conversions ────────────────────────────────────────────────
    public double lngToScreen(double lng) {
        return (getWidth() / 2.0) + (lng - centerLng) * zoomScale;
    }

    public double latToScreen(double lat) {
        return (getHeight() / 2.0) - (lat - centerLat) * zoomScale;
    }

    public double screenToLng(double x) {
        return centerLng + (x - getWidth() / 2.0) / zoomScale;
    }

    public double screenToLat(double y) {
        return centerLat - (y - getHeight() / 2.0) / zoomScale;
    }

    // ── Tactical Grid ─────────────────────────────────────────────────────────
    private void drawTacticalGrid(GraphicsContext gc, double w, double h) {
        gc.setStroke(nightOpsMode ? Color.web("#1E293B", 0.6) : Color.web("#2A384E", 0.6));
        gc.setLineWidth(0.8);
        gc.setFont(Font.font("Monospaced", FontWeight.NORMAL, 10));
        gc.setFill(Color.web("#64748B", 0.7));

        double gridStep = (zoomScale > 400) ? 0.2 : (zoomScale > 200 ? 0.5 : 1.0);

        double minLng = screenToLng(0);
        double maxLng = screenToLng(w);
        double minLat = screenToLat(h);
        double maxLat = screenToLat(0);

        double startLng = Math.floor(minLng / gridStep) * gridStep;
        for (double lng = startLng; lng <= maxLng; lng += gridStep) {
            double x = lngToScreen(lng);
            gc.strokeLine(x, 0, x, h);
            gc.fillText(String.format("%.1f°E", lng), x + 4, h - 8);
        }

        double startLat = Math.floor(minLat / gridStep) * gridStep;
        for (double lat = startLat; lat <= maxLat; lat += gridStep) {
            double y = latToScreen(lat);
            gc.strokeLine(0, y, w, y);
            gc.fillText(String.format("%.1f°%s", Math.abs(lat), lat >= 0 ? "N" : "S"), 6, y - 4);
        }

        // Equator Highlight
        double equatorY = latToScreen(0.0);
        if (equatorY >= 0 && equatorY <= h) {
            gc.setStroke(Color.web("#F59E0B", 0.4));
            gc.setLineWidth(1.2);
            gc.strokeLine(0, equatorY, w, equatorY);
            gc.setFill(Color.web("#F59E0B", 0.8));
            gc.fillText("EQUATOR (0.0°)", 80, equatorY - 4);
        }
    }

    // ── Kenya Sovereign Border ────────────────────────────────────────────────
    private void drawKenyaSovereignBorder(GraphicsContext gc) {
        int n = KENYA_BORDER_COORDS.length;
        double[] xPoints = new double[n];
        double[] yPoints = new double[n];

        for (int i = 0; i < n; i++) {
            xPoints[i] = lngToScreen(KENYA_BORDER_COORDS[i][0]);
            yPoints[i] = latToScreen(KENYA_BORDER_COORDS[i][1]);
        }

        // Subtle Sovereign Fill
        gc.setFill(nightOpsMode ? Color.web("#10B981", 0.04) : Color.web("#10B981", 0.06));
        gc.fillPolygon(xPoints, yPoints, n);

        // Sovereign Gold & Emerald Border
        gc.setStroke(Color.web("#FFD700", 0.85)); // Savannah Gold
        gc.setLineWidth(1.6);
        gc.setLineDashes(8, 4);
        gc.strokePolygon(xPoints, yPoints, n);
        gc.setLineDashes(); // reset

        // Neighbor Labels
        gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#64748B", 0.8));
        gc.fillText("UGANDA", lngToScreen(33.0), latToScreen(1.5));
        gc.fillText("TANZANIA", lngToScreen(36.0), latToScreen(-3.8));
        gc.fillText("ETHIOPIA", lngToScreen(38.0), latToScreen(4.8));
        gc.fillText("SOMALIA", lngToScreen(41.8), latToScreen(2.5));
        gc.fillText("INDIAN OCEAN", lngToScreen(40.8), latToScreen(-3.5));
    }

    // ── PostGIS Geofence Vector Polygons ──────────────────────────────────────
    private void drawPostGisGeofences(GraphicsContext gc) {
        for (GeofenceZone zone : geofenceZones) {
            if (zone.getBoundary() == null) continue;

            Polygon poly = zone.getBoundary();
            Coordinate[] coords = poly.getExteriorRing().getCoordinates();
            int n = coords.length;
            if (n < 3) continue;

            double[] xPoints = new double[n];
            double[] yPoints = new double[n];
            for (int i = 0; i < n; i++) {
                xPoints[i] = lngToScreen(coords[i].x);
                yPoints[i] = latToScreen(coords[i].y);
            }

            String type = zone.getZoneType() != null ? zone.getZoneType().toUpperCase() : "PARK";
            if ("COMMUNITY_BUFFER".equals(type) && layerBuffers) {
                // Amber Dashed Corridor
                gc.setFill(Color.web("#F59E0B", 0.16));
                gc.fillPolygon(xPoints, yPoints, n);
                gc.setStroke(Color.web("#F59E0B", 0.9));
                gc.setLineWidth(1.8);
                gc.setLineDashes(6, 4);
                gc.strokePolygon(xPoints, yPoints, n);
                gc.setLineDashes();
            } else if ("HIGH_CONFLICT".equals(type)) {
                // Red Warning
                gc.setFill(Color.web("#EF4444", 0.20));
                gc.fillPolygon(xPoints, yPoints, n);
                gc.setStroke(Color.web("#EF4444", 0.95));
                gc.setLineWidth(2.0);
                gc.strokePolygon(xPoints, yPoints, n);
            } else if (layerParks) {
                // National Parks (Emerald Sanctuary Green)
                gc.setFill(Color.web("#10B981", 0.15));
                gc.fillPolygon(xPoints, yPoints, n);
                gc.setStroke(Color.web("#10B981", 0.9));
                gc.setLineWidth(1.8);
                gc.strokePolygon(xPoints, yPoints, n);
            }

            // Park Label
            if (zoomScale > 110) {
                double centerX = (xPoints[0] + xPoints[n / 2]) / 2.0;
                double centerY = (yPoints[0] + yPoints[n / 2]) / 2.0;
                gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 10));
                gc.setFill(Color.web("#E2E8F0", 0.9));
                gc.fillText(zone.getZoneName(), centerX - 25, centerY);
            }
        }
    }

    // ── Nairobi NP Northern Electric Fence ────────────────────────────────────
    private void drawNairobiElectricFence(GraphicsContext gc) {
        // Northern City perimeter fence between lng 36.76 and 36.95 at lat -1.3430
        double x1 = lngToScreen(36.76);
        double y1 = latToScreen(-1.3430);
        double x2 = lngToScreen(36.95);
        double y2 = latToScreen(-1.3430);

        gc.setStroke(Color.web("#FFD700", 1.0)); // Electrified Gold
        gc.setLineWidth(3.0);
        gc.setLineDashes(10, 5);
        gc.strokeLine(x1, y1, x2, y2);
        gc.setLineDashes();

        // Warning towers along the fence
        double step = (x2 - x1) / 5.0;
        for (int i = 0; i <= 5; i++) {
            double tx = x1 + i * step;
            gc.setFill(Color.web("#FFD700"));
            gc.fillOval(tx - 3, y1 - 3, 6, 6);
        }

        if (zoomScale > 200) {
            gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 9));
            gc.setFill(Color.web("#FFD700"));
            gc.fillText("⚡ NAIROBI NP NORTHERN ELECTRIC FENCE (CITY BOUNDARY)", x1 + 10, y1 - 6);
        }
    }

    // ── Ecological Landmarks ──────────────────────────────────────────────────
    private void drawLandmarks(GraphicsContext gc) {
        for (EcologicalLandmark lm : landmarks) {
            double x = lngToScreen(lm.getLongitude());
            double y = latToScreen(lm.getLatitude());

            Color iconColor = switch (lm.getLandmarkType()) {
                case "DAM", "WATERHOLE" -> Color.web("#38BDF8");     // Cyan
                case "RIVER_CROSSING" -> Color.web("#34D399");       // Emerald
                case "SWAMP" -> Color.web("#A78BFA");                // Purple
                case "SANCTUARY" -> Color.web("#F59E0B");            // Gold
                default -> Color.web("#94A3B8");
            };

            // Diamond pin icon
            gc.setFill(iconColor);
            gc.fillPolygon(new double[]{x, x + 4, x, x - 4}, new double[]{y - 5, y, y + 5, y}, 4);
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(0.8);
            gc.strokePolygon(new double[]{x, x + 4, x, x - 4}, new double[]{y - 5, y, y + 5, y}, 4);

            if (zoomScale > 220) {
                gc.setFont(Font.font("Montserrat", FontWeight.NORMAL, 9));
                gc.setFill(Color.web("#E2E8F0", 0.85));
                gc.fillText(lm.getLandmarkName(), x + 6, y + 3);
            }
        }
    }

    // ── Telemetry Historical Trails ───────────────────────────────────────────
    private void drawTrails(GraphicsContext gc) {
        for (Map.Entry<String, List<TelemetryTrailDto>> entry : animalTrails.entrySet()) {
            String collarId = entry.getKey();
            List<TelemetryTrailDto> trail = entry.getValue();
            if (trail == null || trail.size() < 2) continue;

            boolean isSelected = collarId.equalsIgnoreCase(selectedCollarId);
            if (!isSelected && zoomScale < 180) continue; // Only show all trails when zoomed in

            int n = trail.size();
            double[] xPoints = new double[n];
            double[] yPoints = new double[n];

            for (int i = 0; i < n; i++) {
                xPoints[i] = lngToScreen(trail.get(i).getLongitude());
                yPoints[i] = latToScreen(trail.get(i).getLatitude());
            }

            // Glowing polyline
            gc.setStroke(isSelected ? Color.web("#38BDF8", 0.95) : Color.web("#38BDF8", 0.4));
            gc.setLineWidth(isSelected ? 2.5 : 1.2);
            gc.strokePolyline(xPoints, yPoints, n);

            // Breadcrumb dots
            for (int i = 0; i < n; i++) {
                double alpha = 0.3 + 0.7 * ((double) i / n);
                gc.setFill(isSelected ? Color.web("#38BDF8", alpha) : Color.web("#94A3B8", alpha * 0.6));
                gc.fillOval(xPoints[i] - 2, yPoints[i] - 2, 4, 4);
            }
        }
    }

    // ── Patrol Units ──────────────────────────────────────────────────────────
    private void drawPatrolUnits(GraphicsContext gc) {
        for (PatrolUnitDto unit : patrolUnits) {
            double x = lngToScreen(unit.getLongitude());
            double y = latToScreen(unit.getLatitude());

            boolean isAirwing = "AIRWING".equalsIgnoreCase(unit.getUnitType());
            boolean isDispatched = "DISPATCHED".equalsIgnoreCase(unit.getStatus());

            // Unit vehicle / aircraft symbol
            gc.setFill(isDispatched ? Color.web("#EF4444") : (isAirwing ? Color.web("#38BDF8") : Color.web("#10B981")));
            if (isAirwing) {
                // Aircraft Delta Wing
                gc.fillPolygon(new double[]{x, x + 6, x - 6}, new double[]{y - 8, y + 6, y + 6}, 3);
            } else {
                // Cruiser Box
                gc.fillRect(x - 5, y - 5, 10, 10);
            }

            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.2);
            if (isAirwing) {
                gc.strokePolygon(new double[]{x, x + 6, x - 6}, new double[]{y - 8, y + 6, y + 6}, 3);
            } else {
                gc.strokeRect(x - 5, y - 5, 10, 10);
            }

            // Call Sign label
            gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 9));
            gc.setFill(Color.web("#F8FAFC"));
            gc.fillText(unit.getCallSign(), x + 8, y + 3);
        }
    }

    // ── Incident Risk Beacons ─────────────────────────────────────────────────
    private void drawIncidentBeacons(GraphicsContext gc) {
        for (IncidentLog inc : incidents) {
            if ("RESOLVED".equalsIgnoreCase(inc.getStatus())) continue;
            if (inc.getLongitude() == null || inc.getLatitude() == null) continue;

            double x = lngToScreen(inc.getLongitude());
            double y = latToScreen(inc.getLatitude());

            // Pulsing Red Rings
            double pulseRadius = 12 + Math.sin(pulsePhase) * 6;
            gc.setStroke(Color.web("#EF4444", 0.7));
            gc.setLineWidth(1.8);
            gc.strokeOval(x - pulseRadius, y - pulseRadius, pulseRadius * 2, pulseRadius * 2);

            gc.setFill(Color.web("#EF4444", 0.9));
            gc.fillOval(x - 5, y - 5, 10, 10);

            // Severity Badge
            gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 9));
            gc.setFill(Color.web("#EF4444"));
            gc.fillText("⚠️ " + (inc.getIncidentCode() != null ? inc.getIncidentCode() : "BREACH"), x + 10, y + 3);
        }
    }

    // ── Wildlife Telemetry Pins ───────────────────────────────────────────────
    private void drawWildlifePins(GraphicsContext gc) {
        for (AnimalDto animal : animals) {
            if (animal.getLongitude() == null || animal.getLatitude() == null) continue;

            double x = lngToScreen(animal.getLongitude());
            double y = latToScreen(animal.getLatitude());
            boolean isSelected = animal.getCollarId().equalsIgnoreCase(selectedCollarId);
            boolean isAlert = "Alert".equalsIgnoreCase(animal.getStatus()) || animal.getIsBreaching();
            boolean isLowBat = (animal.getCollarBattery() != null && animal.getCollarBattery() < 20);

            // Pulsing beacon on alert
            if (isAlert) {
                double r = 14 + Math.sin(pulsePhase) * 6;
                gc.setStroke(Color.web("#EF4444", 0.8));
                gc.setLineWidth(2.0);
                gc.strokeOval(x - r, y - r, r * 2, r * 2);
            }

            // Selected Halo
            if (isSelected) {
                gc.setStroke(Color.web("#38BDF8", 0.9));
                gc.setLineWidth(2.5);
                gc.strokeOval(x - 12, y - 12, 24, 24);
            }

            // Pin Base Circle
            Color pinColor = isAlert ? Color.web("#EF4444") : (isLowBat ? Color.web("#F59E0B") : Color.web("#10B981"));
            gc.setFill(pinColor);
            gc.fillOval(x - 6, y - 6, 12, 12);
            gc.setStroke(Color.WHITE);
            gc.setLineWidth(1.5);
            gc.strokeOval(x - 6, y - 6, 12, 12);

            // Direction Heading Indicator (Triangle Notch)
            if (animal.getSpeedKmh() != null && animal.getSpeedKmh() > 0.5 && animal.getHeadingCompass() != null) {
                double rad = Math.toRadians(headingCompassToDegrees(animal.getHeadingCompass()));
                double hx = x + Math.sin(rad) * 11;
                double hy = y - Math.cos(rad) * 11;
                gc.setStroke(Color.web("#FFD700"));
                gc.setLineWidth(2.0);
                gc.strokeLine(x, y, hx, hy);
            }

            // Name label
            if (isSelected || zoomScale > 200) {
                gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 10));
                gc.setFill(Color.web("#F8FAFC"));
                gc.fillText(animal.getName() + " (" + animal.getCollarId() + ")", x + 9, y - 2);
            }
        }
    }

    private double headingCompassToDegrees(String compass) {
        if (compass == null) return 0;
        return switch (compass.trim().toUpperCase()) {
            case "N" -> 0;
            case "NE" -> 45;
            case "E" -> 90;
            case "SE" -> 135;
            case "S" -> 180;
            case "SW" -> 225;
            case "W" -> 270;
            case "NW" -> 315;
            default -> 0;
        };
    }

    // ── Hover Detection & Inspect Popup ───────────────────────────────────────
    private void findHoveredAnimal(double mx, double my) {
        hoveredAnimal = null;
        for (AnimalDto a : animals) {
            if (a.getLongitude() == null || a.getLatitude() == null) continue;
            double x = lngToScreen(a.getLongitude());
            double y = latToScreen(a.getLatitude());
            if (Math.hypot(mx - x, my - y) <= 12) {
                hoveredAnimal = a;
                return;
            }
        }
    }

    private void drawHoveredAnimalCard(GraphicsContext gc, AnimalDto a, double x, double y) {
        double cardW = 230;
        double cardH = 110;
        double cardX = Math.min(getWidth() - cardW - 10, x + 15);
        double cardY = Math.max(10, Math.min(getHeight() - cardH - 10, y - 20));

        // Background
        gc.setFill(Color.web("#111827", 0.95)); // Slate-900 with blur look
        gc.fillRoundRect(cardX, cardY, cardW, cardH, 8, 8);
        gc.setStroke(Color.web("#38BDF8", 0.9));
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(cardX, cardY, cardW, cardH, 8, 8);

        // Header Title
        gc.setFont(Font.font("Montserrat", FontWeight.BOLD, 12));
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillText(a.getSpeciesIcon() + " " + a.getName(), cardX + 10, cardY + 20);

        // Subtitle: Species & Collar
        gc.setFont(Font.font("Montserrat", FontWeight.NORMAL, 10));
        gc.setFill(Color.web("#94A3B8"));
        gc.fillText(a.getSpecies() + " • " + a.getCollarId(), cardX + 10, cardY + 36);

        // Coordinates & Battery
        gc.fillText(String.format("GPS: %.4f°N, %.4f°E", a.getLatitude(), a.getLongitude()), cardX + 10, cardY + 54);
        gc.fillText(String.format("Battery: %d%% • Speed: %.1f km/h", a.getCollarBattery(), a.getSpeedKmh() != null ? a.getSpeedKmh() : 0.0), cardX + 10, cardY + 70);

        // Behavior / Proximity
        gc.setFill(Color.web("#F59E0B"));
        gc.fillText(a.getBehaviorState() != null ? a.getBehaviorState() : "Active Foraging", cardX + 10, cardY + 86);
        if (a.getFenceDistance() != null) {
            gc.fillText(a.getFenceDistance(), cardX + 10, cardY + 102);
        }
    }

    // ── Tactical HUD Overlay ──────────────────────────────────────────────────
    private void drawTacticalHud(GraphicsContext gc, double w, double h) {
        // Bottom-Right Coordinates Readout
        if (mouseX >= 0 && mouseY >= 0) {
            double curLng = screenToLng(mouseX);
            double curLat = screenToLat(mouseY);
            String coordsText = String.format("RADAR POS: %.4f°%s, %.4f°E  |  ZOOM: %.0fx",
                    Math.abs(curLat), curLat >= 0 ? "N" : "S", curLng, zoomScale / 100.0);

            gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 10));
            gc.setFill(Color.web("#0B0F17", 0.85));
            gc.fillRoundRect(w - 330, h - 28, 320, 22, 6, 6);
            gc.setStroke(Color.web("#1E293B"));
            gc.strokeRoundRect(w - 330, h - 28, 320, 22, 6, 6);

            gc.setFill(Color.web("#38BDF8"));
            gc.fillText(coordsText, w - 322, h - 13);
        }

        // Scale Indicator (Top-Left)
        gc.setFill(Color.web("#0B0F17", 0.85));
        gc.fillRoundRect(12, 12, 140, 24, 6, 6);
        gc.setStroke(Color.web("#1E293B"));
        gc.strokeRoundRect(12, 12, 140, 24, 6, 6);

        double scaleBarPixels = 50.0;
        double degrees = scaleBarPixels / zoomScale;
        double km = degrees * 111.32; // 1 degree latitude ~ 111.32 km

        gc.setStroke(Color.web("#F8FAFC"));
        gc.setLineWidth(2.0);
        gc.strokeLine(20, 24, 20 + scaleBarPixels, 24);
        gc.setFont(Font.font("Monospaced", FontWeight.BOLD, 9));
        gc.setFill(Color.web("#F8FAFC"));
        gc.fillText(String.format("— %.0f km —", km), 75, 27);
    }
}
