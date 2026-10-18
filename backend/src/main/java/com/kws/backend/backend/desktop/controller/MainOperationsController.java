package com.kws.backend.backend.desktop.controller;

import com.kws.backend.backend.desktop.view.*;
import com.kws.backend.backend.dto.AnimalDto;
import com.kws.backend.backend.dto.ParkWeatherDto;
import com.kws.backend.backend.dto.PatrolUnitDto;
import com.kws.backend.backend.dto.TelemetryTrailDto;
import com.kws.backend.backend.model.*;
import com.kws.backend.backend.repository.EcologicalLandmarkRepository;
import com.kws.backend.backend.repository.GeofenceRepository;
import com.kws.backend.backend.repository.ParkProfileRepository;
import com.kws.backend.backend.service.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
public class MainOperationsController {

    private final AnimalService animalService;
    private final TelemetryService telemetryService;
    private final TelemetrySimulationService simulationService;
    private final IncidentService incidentService;
    private final PatrolService patrolService;
    private final WeatherService weatherService;
    private final UserService userService;
    private final AuditService auditService;
    private final GeofenceRepository geofenceRepository;
    private final EcologicalLandmarkRepository landmarkRepository;
    private final ParkProfileRepository parkProfileRepository;

    private Stage primaryStage;
    private ScheduledExecutorService pollingExecutor;

    // View Components
    private BorderPane rootLayout;
    private StackPane contentStack;
    private GisMapCanvas gisMapCanvas;
    private KpiCardComponent kpiAnimals;
    private KpiCardComponent kpiPatrols;
    private KpiCardComponent kpiAlerts;
    private KpiCardComponent kpiFixes;
    private BatteryGaugeComponent batteryGauge;
    private WeatherStripComponent weatherStrip;
    private AlertBannerComponent alertBanner;
    private ActivityFeedComponent activityFeed;
    private VBox wildlifeRosterContainer;
    private Label userBadgeLabel;
    private Label clockLabel;
    private Button playPauseBtn;

    // Filters
    private String selectedPark = "All Kenya";
    private String selectedSpecies = "All Species";
    private String searchQuery = "";

    // Data Cache
    private List<AnimalDto> cachedAnimals = new ArrayList<>();
    private List<PatrolUnitDto> cachedPatrols = new ArrayList<>();
    private List<IncidentLog> cachedIncidents = new ArrayList<>();
    private List<GeofenceZone> cachedGeofences = new ArrayList<>();
    private List<EcologicalLandmark> cachedLandmarks = new ArrayList<>();
    private List<ParkProfile> cachedParkProfiles = new ArrayList<>();
    private Map<String, List<TelemetryTrailDto>> cachedTrails = new HashMap<>();

    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public MainOperationsController(AnimalService animalService,
                                    TelemetryService telemetryService,
                                    TelemetrySimulationService simulationService,
                                    IncidentService incidentService,
                                    PatrolService patrolService,
                                    WeatherService weatherService,
                                    UserService userService,
                                    AuditService auditService,
                                    GeofenceRepository geofenceRepository,
                                    EcologicalLandmarkRepository landmarkRepository,
                                    ParkProfileRepository parkProfileRepository) {
        this.animalService = animalService;
        this.telemetryService = telemetryService;
        this.simulationService = simulationService;
        this.incidentService = incidentService;
        this.patrolService = patrolService;
        this.weatherService = weatherService;
        this.userService = userService;
        this.auditService = auditService;
        this.geofenceRepository = geofenceRepository;
        this.landmarkRepository = landmarkRepository;
        this.parkProfileRepository = parkProfileRepository;
    }

    public void initialize(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("🦁 WildGuard Tracker — Kenya Wildlife Service Operations Command");
        stage.setMinWidth(1280);
        stage.setMinHeight(800);

        buildUi();
        loadStaticReferenceData();
        startBackgroundPolling();
    }

    public void shutdown() {
        if (pollingExecutor != null && !pollingExecutor.isShutdown()) {
            pollingExecutor.shutdownNow();
        }
    }

    private void buildUi() {
        rootLayout = new BorderPane();
        rootLayout.setStyle("-fx-background-color: #0B0F17;");

        // 1. Top Header
        rootLayout.setTop(buildHeader());

        // 2. Navigation Sidebar
        rootLayout.setLeft(buildSidebar());

        // 3. Central Content Stack (holds interchangeable tabs)
        contentStack = new StackPane();
        contentStack.getChildren().add(buildDashboardView());
        rootLayout.setCenter(contentStack);

        Scene scene = new Scene(rootLayout, 1440, 900);
        primaryStage.setScene(scene);
    }

    // ── Header & Simulation Controls ──────────────────────────────────────────
    private Node buildHeader() {
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B; -fx-border-width: 0 0 1px 0;");

        // Emblem & Title
        Label emblem = new Label("🦁");
        emblem.setStyle("-fx-font-size: 22px;");

        VBox titleBox = new VBox(1);
        Label title = new Label("WILDGUARD TRACKER");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFD700; -fx-letter-spacing: 1px;");

        Label subtitle = new Label("KENYA WILDLIFE SERVICE — OPERATIONS COMMAND");
        subtitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        titleBox.getChildren().addAll(title, subtitle);

        // Connection Badge
        Label dbBadge = new Label("● PostGIS 16 Connected");
        dbBadge.setStyle("-fx-font-family: 'Monospaced', monospace; -fx-font-size: 10px; -fx-text-fill: #10B981; -fx-background-color: #064E3B; -fx-padding: 3px 8px; -fx-background-radius: 12px;");

        // Spacer
        Region spacer1 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);

        // Simulation Controls
        HBox simBox = new HBox(8);
        simBox.setAlignment(Pos.CENTER);
        simBox.setPadding(new Insets(4, 10, 4, 10));
        simBox.setStyle("-fx-background-color: #161F2E; -fx-background-radius: 6px; -fx-border-color: #1E293B; -fx-border-radius: 6px;");

        playPauseBtn = new Button(simulationService.isSimulationActive() ? "⏸ Pause" : "▶ Resume");
        playPauseBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #F8FAFC; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand;");
        playPauseBtn.setOnAction(e -> {
            boolean active = !simulationService.isSimulationActive();
            simulationService.setSimulationActive(active);
            playPauseBtn.setText(active ? "⏸ Pause" : "▶ Resume");
        });

        Button stepBtn = new Button("⏭ Step");
        stepBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #38BDF8; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand;");
        stepBtn.setOnAction(e -> simulationService.simulateStep());

        ComboBox<String> speedCombo = new ComboBox<>();
        speedCombo.getItems().addAll("1x Speed", "2x Speed", "4x Speed");
        speedCombo.setValue("1x Speed");
        speedCombo.setStyle("-fx-background-color: #1E293B; -fx-text-fill: white; -fx-font-size: 10px;");
        speedCombo.setOnAction(e -> {
            String val = speedCombo.getValue();
            if (val != null) {
                if (val.startsWith("1")) simulationService.setSimulationSpeedMultiplier(1.0);
                else if (val.startsWith("2")) simulationService.setSimulationSpeedMultiplier(2.0);
                else if (val.startsWith("4")) simulationService.setSimulationSpeedMultiplier(4.0);
            }
        });

        simBox.getChildren().addAll(playPauseBtn, stepBtn, speedCombo);

        // Night Ops Toggle
        Button nightOpsBtn = new Button("🌙 Night Radar");
        nightOpsBtn.setStyle("-fx-background-color: #161F2E; -fx-text-fill: #CBD5E1; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-cursor: hand;");
        nightOpsBtn.setOnAction(e -> {
            boolean night = nightOpsBtn.getText().contains("Night");
            gisMapCanvas.setNightOpsMode(!night);
            nightOpsBtn.setText(night ? "☀️ Daylight Ops" : "🌙 Night Radar");
        });

        // Live Clock
        clockLabel = new Label(LocalDateTime.now().format(CLOCK_FMT) + " EAT");
        clockLabel.setStyle("-fx-font-family: 'Monospaced', monospace; -fx-font-size: 11px; -fx-text-fill: #94A3B8;");

        // Authenticated User & Logout
        UserAccount user = userService.getCurrentUser();
        String userDisplay = user != null ? (user.getFullName() != null ? user.getFullName() : user.getUsername()) + " [" + user.getRole() + "]" : "Commander [ACTIVE]";
        userBadgeLabel = new Label("👤 " + userDisplay);
        userBadgeLabel.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #38BDF8; -fx-background-color: #0C4A6E; -fx-padding: 4px 8px; -fx-background-radius: 4px;");

        Button logoutBtn = new Button("Logout");
        logoutBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #EF4444; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-cursor: hand;");
        logoutBtn.setOnAction(e -> {
            userService.logout();
            AuthDialogController authDialog = new AuthDialogController(userService);
            UserAccount newUser = authDialog.showLoginDialog(primaryStage);
            if (newUser != null) {
                userBadgeLabel.setText("👤 " + (newUser.getFullName() != null ? newUser.getFullName() : newUser.getUsername()) + " [" + newUser.getRole() + "]");
            }
        });

        header.getChildren().addAll(emblem, titleBox, dbBadge, spacer1, simBox, nightOpsBtn, clockLabel, userBadgeLabel, logoutBtn);
        return header;
    }

    // ── Navigation Sidebar ────────────────────────────────────────────────────
    private Node buildSidebar() {
        VBox sidebar = new VBox(6);
        sidebar.setPrefWidth(210);
        sidebar.setPadding(new Insets(16, 12, 16, 12));
        sidebar.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B; -fx-border-width: 0 1px 0 0;");

        Label navSection = new Label("TACTICAL NAVIGATION");
        navSection.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 9px; -fx-font-weight: bold; -fx-text-fill: #64748B; -fx-padding: 0 0 6px 8px;");

        sidebar.getChildren().add(navSection);

        sidebar.getChildren().addAll(
                createNavButton("📊 Operations Command", () -> switchView(buildDashboardView())),
                createNavButton("🗺️ Interactive GIS Map", () -> switchView(buildFullscreenMapView())),
                createNavButton("📋 Wildlife & Collars", () -> switchView(buildWildlifeRegistryView())),
                createNavButton("🚨 Incidents & HWC", () -> switchView(buildIncidentManagementView())),
                createNavButton("🛡️ Patrol Units & Radar", () -> switchView(buildPatrolCoordinationView())),
                createNavButton("🏞️ Sector Dossiers", () -> switchView(buildParkDossiersView())),
                createNavButton("📜 Operations Activity Feed", () -> switchView(buildActivityAuditView()))
        );

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        // Quick Emergency Broadcast Action
        Button smsBtn = new Button("📢 Emergency SMS Broadcast");
        smsBtn.setMaxWidth(Double.MAX_VALUE);
        smsBtn.setStyle("-fx-background-color: #78350F; -fx-text-fill: #FDE68A; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px; -fx-background-radius: 4px;");
        smsBtn.setOnAction(e -> openSmsBroadcastModal());
        sidebar.getChildren().add(smsBtn);

        return sidebar;
    }

    private Button createNavButton(String label, Runnable action) {
        Button btn = new Button(label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 10px; -fx-cursor: hand; -fx-background-radius: 4px;");

        btn.setOnMouseEntered(e -> btn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #F8FAFC; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 10px; -fx-cursor: hand; -fx-background-radius: 4px;"));
        btn.setOnMouseExited(e -> btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #94A3B8; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 8px 10px; -fx-cursor: hand; -fx-background-radius: 4px;"));

        btn.setOnAction(e -> action.run());
        return btn;
    }

    private void switchView(Node newView) {
        contentStack.getChildren().clear();
        contentStack.getChildren().add(newView);
    }

    // ── 1. Operations Dashboard View ──────────────────────────────────────────
    private Node buildDashboardView() {
        VBox dashboard = new VBox(12);
        dashboard.setPadding(new Insets(14));
        dashboard.setStyle("-fx-background-color: #0B0F17;");

        // KPI Row
        HBox kpiRow = new HBox(12);
        kpiRow.setAlignment(Pos.CENTER_LEFT);

        kpiAnimals = new KpiCardComponent("ACTIVE WILDLIFE", "34 Tracked", "100% Monitored", "🦁", "#10B981");
        kpiPatrols = new KpiCardComponent("PATROL DEPLOYMENTS", "7 Units", "5 Available • 2 Active", "🛡️", "#38BDF8");
        kpiAlerts = new KpiCardComponent("ACTIVE BREACH ALERTS", "0 Active", "Sanctuaries Secure", "⚠️", "#EF4444");
        kpiFixes = new KpiCardComponent("GPS TELEMETRY FIXES", "1,248 Fixes", "12s Polling Active", "🛰️", "#F59E0B");

        HBox.setHgrow(kpiAnimals, Priority.ALWAYS);
        HBox.setHgrow(kpiPatrols, Priority.ALWAYS);
        HBox.setHgrow(kpiAlerts, Priority.ALWAYS);
        HBox.setHgrow(kpiFixes, Priority.ALWAYS);

        kpiRow.getChildren().addAll(kpiAnimals, kpiPatrols, kpiAlerts, kpiFixes);

        // Battery Health Gauge
        batteryGauge = new BatteryGaugeComponent();

        // Critical Alert Banner
        alertBanner = new AlertBannerComponent(
                this::openDispatchModalForIncident,
                incident -> {
                    incidentService.acknowledgeIncident(incident.getId(), "COMMANDER");
                    alertBanner.clearAlert();
                }
        );

        // Weather Strip
        weatherStrip = new WeatherStripComponent();
        weatherStrip.setOnParkClicked(park -> {
            selectedPark = park;
            applyFilters();
            gisMapCanvas.centerOn(getParkCenterLat(park), getParkCenterLng(park), 350.0);
        });

        // Quick Filter Chips Row
        HBox filterChips = buildQuickFilterChips();

        // Main Center Area: Map + Roster Split
        SplitPane centerSplit = new SplitPane();
        centerSplit.setStyle("-fx-box-border: transparent; -fx-background-color: transparent;");
        VBox.setVgrow(centerSplit, Priority.ALWAYS);

        // Left / Center: Interactive Map
        VBox mapWrapper = new VBox(6);
        gisMapCanvas = new GisMapCanvas(850, 520);
        VBox.setVgrow(gisMapCanvas, Priority.ALWAYS);
        gisMapCanvas.setAnimalSelectionListener(this::openAnimalDetailDialog);

        // Map Toolbar
        HBox mapBar = new HBox(10);
        mapBar.setAlignment(Pos.CENTER_LEFT);
        mapBar.setPadding(new Insets(4, 8, 4, 8));
        mapBar.setStyle("-fx-background-color: #111827; -fx-background-radius: 4px;");

        Label mapTitle = new Label("KENYA SPATIAL TACTICAL RADAR");
        mapTitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");
        HBox.setHgrow(mapTitle, Priority.ALWAYS);

        Button resetBtn = new Button("Kenya Center");
        resetBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1; -fx-font-size: 10px; -fx-cursor: hand;");
        resetBtn.setOnAction(e -> gisMapCanvas.resetToKenya());

        Button zoomInBtn = new Button("+");
        zoomInBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        zoomInBtn.setOnAction(e -> gisMapCanvas.zoomIn());

        Button zoomOutBtn = new Button("-");
        zoomOutBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand;");
        zoomOutBtn.setOnAction(e -> gisMapCanvas.zoomOut());

        mapBar.getChildren().addAll(mapTitle, resetBtn, zoomInBtn, zoomOutBtn);
        mapWrapper.getChildren().addAll(mapBar, gisMapCanvas);

        // Right Panel: Wildlife Roster Drawer
        VBox rosterWrapper = new VBox(8);
        rosterWrapper.setPrefWidth(380);
        rosterWrapper.setPadding(new Insets(8));
        rosterWrapper.setStyle("-fx-background-color: #111827; -fx-background-radius: 8px; -fx-border-color: #1E293B; -fx-border-radius: 8px;");

        HBox rosterHeader = new HBox(8);
        rosterHeader.setAlignment(Pos.CENTER_LEFT);

        Label rosterTitle = new Label("COLLARED WILDLIFE ROSTER");
        rosterTitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");
        HBox.setHgrow(rosterTitle, Priority.ALWAYS);

        Button addCollarBtn = new Button("+ Add Collar");
        addCollarBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 4px;");
        addCollarBtn.setOnAction(e -> openAddCollarDialog());

        rosterHeader.getChildren().addAll(rosterTitle, addCollarBtn);

        // Search Input
        TextField searchField = new TextField();
        searchField.setPromptText("Search collar tag, animal name, park...");
        searchField.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white; -fx-border-color: #1E293B; -fx-border-radius: 4px; -fx-font-size: 11px;");
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            searchQuery = newV != null ? newV.trim().toLowerCase() : "";
            applyFilters();
        });

        // Roster Scroll List
        wildlifeRosterContainer = new VBox(6);
        ScrollPane rosterScroll = new ScrollPane(wildlifeRosterContainer);
        rosterScroll.setFitToWidth(true);
        rosterScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        VBox.setVgrow(rosterScroll, Priority.ALWAYS);

        rosterWrapper.getChildren().addAll(rosterHeader, searchField, rosterScroll);

        centerSplit.getItems().addAll(mapWrapper, rosterWrapper);
        centerSplit.setDividerPositions(0.68);

        // Bottom Activity Feed
        activityFeed = new ActivityFeedComponent();

        dashboard.getChildren().addAll(kpiRow, alertBanner, batteryGauge, weatherStrip, filterChips, centerSplit, activityFeed);

        ScrollPane mainScroll = new ScrollPane(dashboard);
        mainScroll.setFitToWidth(true);
        mainScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent;");
        return mainScroll;
    }

    private HBox buildQuickFilterChips() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);

        Label label = new Label("FILTER ECOSYSTEM:");
        label.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #64748B;");

        bar.getChildren().add(label);

        String[] parks = {"All Kenya", "Amboseli NP", "Tsavo East NP", "Tsavo West NP", "Maasai Mara", "Nairobi NP", "Ol Pejeta", "Lake Nakuru", "Samburu"};
        for (String p : parks) {
            Button chip = new Button(p);
            chip.setStyle("-fx-background-color: " + (selectedPark.equals(p) ? "#38BDF8" : "#1E293B") + "; -fx-text-fill: " + (selectedPark.equals(p) ? "#0F172A" : "#CBD5E1") + "; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 12px; -fx-padding: 3px 10px;");
            chip.setOnAction(e -> {
                selectedPark = p;
                applyFilters();
                if (!"All Kenya".equals(p)) {
                    gisMapCanvas.centerOn(getParkCenterLat(p), getParkCenterLng(p), 320.0);
                } else {
                    gisMapCanvas.resetToKenya();
                }
            });
            bar.getChildren().add(chip);
        }
        return bar;
    }

    // ── 2. Fullscreen GIS Map View ────────────────────────────────────────────
    private Node buildFullscreenMapView() {
        VBox view = new VBox(10);
        view.setPadding(new Insets(14));
        view.setStyle("-fx-background-color: #0B0F17;");

        HBox layerBar = new HBox(14);
        layerBar.setAlignment(Pos.CENTER_LEFT);
        layerBar.setPadding(new Insets(8, 14, 8, 14));
        layerBar.setStyle("-fx-background-color: #111827; -fx-background-radius: 6px; -fx-border-color: #1E293B;");

        Label layerTitle = new Label("TACTICAL GIS LAYERS:");
        layerTitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 10px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        CheckBox cbParks = new CheckBox("Protected Parks");
        cbParks.setSelected(true);
        cbParks.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbBuffers = new CheckBox("Community Buffers");
        cbBuffers.setSelected(true);
        cbBuffers.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbFence = new CheckBox("Nairobi Electric Fence");
        cbFence.setSelected(true);
        cbFence.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbLandmarks = new CheckBox("Waterholes & Dams");
        cbLandmarks.setSelected(true);
        cbLandmarks.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbWildlife = new CheckBox("Wildlife Pins");
        cbWildlife.setSelected(true);
        cbWildlife.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbTrails = new CheckBox("GPS Trails");
        cbTrails.setSelected(true);
        cbTrails.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbPatrols = new CheckBox("Patrol Units");
        cbPatrols.setSelected(true);
        cbPatrols.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        CheckBox cbIncidents = new CheckBox("HWC Incidents");
        cbIncidents.setSelected(true);
        cbIncidents.setStyle("-fx-text-fill: #CBD5E1; -fx-font-size: 10px;");

        Runnable updateLayers = () -> gisMapCanvas.setLayers(
                cbParks.isSelected(), cbBuffers.isSelected(), cbFence.isSelected(),
                cbLandmarks.isSelected(), cbWildlife.isSelected(), cbTrails.isSelected(),
                cbPatrols.isSelected(), cbIncidents.isSelected()
        );

        cbParks.setOnAction(e -> updateLayers.run());
        cbBuffers.setOnAction(e -> updateLayers.run());
        cbFence.setOnAction(e -> updateLayers.run());
        cbLandmarks.setOnAction(e -> updateLayers.run());
        cbWildlife.setOnAction(e -> updateLayers.run());
        cbTrails.setOnAction(e -> updateLayers.run());
        cbPatrols.setOnAction(e -> updateLayers.run());
        cbIncidents.setOnAction(e -> updateLayers.run());

        layerBar.getChildren().addAll(layerTitle, cbParks, cbBuffers, cbFence, cbLandmarks, cbWildlife, cbTrails, cbPatrols, cbIncidents);

        VBox.setVgrow(gisMapCanvas, Priority.ALWAYS);
        view.getChildren().addAll(layerBar, gisMapCanvas);
        return view;
    }

    // ── 3. Wildlife Registry & Collars View ────────────────────────────────────
    private Node buildWildlifeRegistryView() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(16));
        view.setStyle("-fx-background-color: #0B0F17;");

        HBox topRow = new HBox(12);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📋 FIELD COLLAR DEPLOYMENT & WILDLIFE REGISTRY");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button registerBtn = new Button("+ Register New Collared Animal");
        registerBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 14px; -fx-background-radius: 4px;");
        registerBtn.setOnAction(e -> openAddCollarDialog());

        topRow.getChildren().addAll(title, registerBtn);

        // Table
        TableView<AnimalDto> table = new TableView<>();
        table.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AnimalDto, String> colCollar = new TableColumn<>("Collar ID");
        colCollar.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCollarId()));
        colCollar.setPrefWidth(120);

        TableColumn<AnimalDto, String> colName = new TableColumn<>("Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colName.setPrefWidth(160);

        TableColumn<AnimalDto, String> colSpecies = new TableColumn<>("Species");
        colSpecies.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSpecies()));
        colSpecies.setPrefWidth(160);

        TableColumn<AnimalDto, String> colPark = new TableColumn<>("Assigned Park");
        colPark.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getParkName()));
        colPark.setPrefWidth(140);

        TableColumn<AnimalDto, String> colSex = new TableColumn<>("Sex");
        colSex.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSex()));
        colSex.setPrefWidth(80);

        TableColumn<AnimalDto, String> colIucn = new TableColumn<>("IUCN Status");
        colIucn.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIucnStatus()));
        colIucn.setPrefWidth(90);

        TableColumn<AnimalDto, String> colBattery = new TableColumn<>("Collar Battery");
        colBattery.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCollarBattery() + "%"));
        colBattery.setPrefWidth(100);

        TableColumn<AnimalDto, String> colSpeed = new TableColumn<>("Speed (km/h)");
        colSpeed.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.1f", d.getValue().getSpeedKmh() != null ? d.getValue().getSpeedKmh() : 0.0)));
        colSpeed.setPrefWidth(90);

        TableColumn<AnimalDto, String> colStatus = new TableColumn<>("Operational Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setPrefWidth(120);

        table.getColumns().addAll(colCollar, colName, colSpecies, colPark, colSex, colIucn, colBattery, colSpeed, colStatus);
        table.getItems().addAll(cachedAnimals);

        table.setRowFactory(tv -> {
            TableRow<AnimalDto> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && (!row.isEmpty())) {
                    openAnimalDetailDialog(row.getItem());
                }
            });
            return row;
        });

        view.getChildren().addAll(topRow, table);
        return view;
    }

    // ── 4. Incidents & HWC Management View ────────────────────────────────────
    private Node buildIncidentManagementView() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(16));
        view.setStyle("-fx-background-color: #0B0F17;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🚨 HUMAN-WILDLIFE CONFLICT & GEOFENCE BREACH QUEUE");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");
        HBox.setHgrow(title, Priority.ALWAYS);

        Button broadcastBtn = new Button("📢 Broadcast Community SMS");
        broadcastBtn.setStyle("-fx-background-color: #78350F; -fx-text-fill: #FDE68A; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6px 12px;");
        broadcastBtn.setOnAction(e -> openSmsBroadcastModal());

        header.getChildren().addAll(title, broadcastBtn);

        TableView<IncidentLog> table = new TableView<>();
        table.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<IncidentLog, String> colCode = new TableColumn<>("Code");
        colCode.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getIncidentCode() != null ? d.getValue().getIncidentCode() : "#" + d.getValue().getId()));
        colCode.setPrefWidth(110);

        TableColumn<IncidentLog, String> colAnimal = new TableColumn<>("Animal");
        colAnimal.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getAnimalName() + " (" + d.getValue().getCollarId() + ")"));
        colAnimal.setPrefWidth(180);

        TableColumn<IncidentLog, String> colZone = new TableColumn<>("Breached Zone / Corridor");
        colZone.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getZoneName()));
        colZone.setPrefWidth(200);

        TableColumn<IncidentLog, String> colSeverity = new TableColumn<>("Severity");
        colSeverity.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSeverity()));
        colSeverity.setPrefWidth(90);

        TableColumn<IncidentLog, String> colStatus = new TableColumn<>("Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setPrefWidth(110);

        TableColumn<IncidentLog, String> colMessage = new TableColumn<>("Message / Narrative");
        colMessage.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMessage()));
        colMessage.setPrefWidth(280);

        TableColumn<IncidentLog, String> colUnit = new TableColumn<>("Dispatched Unit");
        colUnit.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDispatchedUnit() != null ? d.getValue().getDispatchedUnit() : "None"));
        colUnit.setPrefWidth(130);

        table.getColumns().addAll(colCode, colAnimal, colZone, colSeverity, colStatus, colMessage, colUnit);
        table.getItems().addAll(cachedIncidents);

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button ackBtn = new Button("👁️ Acknowledge Selected");
        ackBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-cursor: hand;");
        ackBtn.setOnAction(e -> {
            IncidentLog selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                incidentService.acknowledgeIncident(selected.getId(), "COMMANDER");
                refreshData();
            }
        });

        Button dispatchBtn = new Button("⚡ Dispatch Patrol Unit");
        dispatchBtn.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 14px; -fx-background-radius: 4px;");
        dispatchBtn.setOnAction(e -> {
            IncidentLog selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openDispatchModalForIncident(selected);
            }
        });

        Button resolveBtn = new Button("✅ Resolve Incident");
        resolveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8px 14px; -fx-background-radius: 4px;");
        resolveBtn.setOnAction(e -> {
            IncidentLog selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                openResolveModal(selected);
            }
        });

        actionRow.getChildren().addAll(ackBtn, dispatchBtn, resolveBtn);

        view.getChildren().addAll(header, table, actionRow);
        return view;
    }

    // ── 5. Patrol Coordination View ───────────────────────────────────────────
    private Node buildPatrolCoordinationView() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(16));
        view.setStyle("-fx-background-color: #0B0F17;");

        Label title = new Label("🛡️ ACTIVE FIELD PATROLS & RAPID DISPATCH RADAR");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #38BDF8;");

        TableView<PatrolUnitDto> table = new TableView<>();
        table.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<PatrolUnitDto, String> colCallSign = new TableColumn<>("Call Sign");
        colCallSign.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getCallSign()));
        colCallSign.setPrefWidth(120);

        TableColumn<PatrolUnitDto, String> colName = new TableColumn<>("Unit Name");
        colName.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getName()));
        colName.setPrefWidth(240);

        TableColumn<PatrolUnitDto, String> colType = new TableColumn<>("Vehicle Type");
        colType.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUnitType()));
        colType.setPrefWidth(120);

        TableColumn<PatrolUnitDto, String> colSector = new TableColumn<>("Stationed Sector");
        colSector.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getSector()));
        colSector.setPrefWidth(140);

        TableColumn<PatrolUnitDto, String> colStatus = new TableColumn<>("Deployment Status");
        colStatus.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getStatus()));
        colStatus.setPrefWidth(140);

        TableColumn<PatrolUnitDto, String> colPos = new TableColumn<>("Coordinates");
        colPos.setCellValueFactory(d -> new SimpleStringProperty(String.format("%.4f°, %.4f°", d.getValue().getLatitude(), d.getValue().getLongitude())));
        colPos.setPrefWidth(160);

        table.getColumns().addAll(colCallSign, colName, colType, colSector, colStatus, colPos);
        table.getItems().addAll(cachedPatrols);

        HBox actionRow = new HBox(12);
        actionRow.setAlignment(Pos.CENTER_RIGHT);

        Button makeAvailBtn = new Button("Set Status: AVAILABLE");
        makeAvailBtn.setStyle("-fx-background-color: #064E3B; -fx-text-fill: #6EE7B7; -fx-font-size: 11px; -fx-cursor: hand;");
        makeAvailBtn.setOnAction(e -> {
            PatrolUnitDto selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                patrolService.updateStatus(selected.getId(), "AVAILABLE");
                refreshData();
            }
        });

        Button onPatrolBtn = new Button("Set Status: ON_PATROL");
        onPatrolBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #38BDF8; -fx-font-size: 11px; -fx-cursor: hand;");
        onPatrolBtn.setOnAction(e -> {
            PatrolUnitDto selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                patrolService.updateStatus(selected.getId(), "ON_PATROL");
                refreshData();
            }
        });

        actionRow.getChildren().addAll(makeAvailBtn, onPatrolBtn);

        view.getChildren().addAll(title, table, actionRow);
        return view;
    }

    // ── 6. Park Sector Dossiers View ──────────────────────────────────────────
    private Node buildParkDossiersView() {
        SplitPane split = new SplitPane();
        split.setStyle("-fx-background-color: #0B0F17;");

        // Left List
        VBox left = new VBox(8);
        left.setPadding(new Insets(14));
        left.setPrefWidth(260);
        left.setStyle("-fx-background-color: #111827;");

        Label listTitle = new Label("MONITORED ECOSYSTEMS");
        listTitle.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #94A3B8;");

        ListView<ParkProfile> parkListView = new ListView<>();
        parkListView.setStyle("-fx-background-color: #161F2E;");
        VBox.setVgrow(parkListView, Priority.ALWAYS);
        parkListView.getItems().addAll(cachedParkProfiles);
        parkListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(ParkProfile item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getParkName() + "\n(" + item.getCounty() + ")");
                    setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #F8FAFC; -fx-padding: 6px;");
                }
            }
        });

        left.getChildren().addAll(listTitle, parkListView);

        // Right Detail View
        VBox detailBox = new VBox(14);
        detailBox.setPadding(new Insets(20));
        detailBox.setStyle("-fx-background-color: #0B0F17;");

        Label placeholder = new Label("Select an ecosystem sector on the left to view detailed operational dossier.");
        placeholder.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 12px; -fx-text-fill: #64748B;");
        detailBox.getChildren().add(placeholder);

        parkListView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, selected) -> {
            if (selected != null) {
                detailBox.getChildren().clear();

                Label pName = new Label(selected.getParkName());
                pName.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #FFD700;");

                Label pCounty = new Label(selected.getCounty() + " • Established: " + selected.getEstablishedYear() + " • Area: " + selected.getAreaSqKm() + " km²");
                pCounty.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 12px; -fx-text-fill: #94A3B8;");

                Label pThreat = new Label("SECURITY THREAT LEVEL: " + selected.getThreatLevel());
                pThreat.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #EF4444; -fx-background-color: #7F1D1D; -fx-padding: 4px 8px; -fx-background-radius: 4px;");

                Label pDesc = new Label(selected.getDescription());
                pDesc.setWrapText(true);
                pDesc.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #E2E8F0; -fx-line-spacing: 3px;");

                Label pWaterholes = new Label("💧 Key Ecological Waterholes & Swamps:\n" + selected.getKeyWaterholes());
                pWaterholes.setWrapText(true);
                pWaterholes.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #38BDF8;");

                Label pSpecies = new Label("🦁 Target Conservation Species:\n" + selected.getKeySpecies());
                pSpecies.setWrapText(true);
                pSpecies.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-text-fill: #10B981;");

                Button focusMapBtn = new Button("🎯 Center GIS Radar on this Sector");
                focusMapBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #38BDF8; -fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 11px; -fx-cursor: hand;");
                focusMapBtn.setOnAction(e -> {
                    switchView(buildDashboardView());
                    gisMapCanvas.centerOn(selected.getCenterLat(), selected.getCenterLng(), 340.0);
                });

                detailBox.getChildren().addAll(pName, pCounty, pThreat, pDesc, pWaterholes, pSpecies, focusMapBtn);
            }
        });

        split.getItems().addAll(left, detailBox);
        split.setDividerPositions(0.3);
        return split;
    }

    // ── 7. Activity & Audit Logs View ─────────────────────────────────────────
    private Node buildActivityAuditView() {
        VBox view = new VBox(14);
        view.setPadding(new Insets(16));
        view.setStyle("-fx-background-color: #0B0F17;");

        Label title = new Label("📜 AUDITABLE SYSTEM ACTIVITY & SECURITY LOGS");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        TableView<AuditLog> table = new TableView<>();
        table.setStyle("-fx-background-color: #111827; -fx-border-color: #1E293B;");
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<AuditLog, String> colTime = new TableColumn<>("Timestamp");
        colTime.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getTimestamp() != null ? d.getValue().getTimestamp().format(CLOCK_FMT) : ""));
        colTime.setPrefWidth(160);

        TableColumn<AuditLog, String> colUser = new TableColumn<>("Operator");
        colUser.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        colUser.setPrefWidth(120);

        TableColumn<AuditLog, String> colAction = new TableColumn<>("Action Type");
        colAction.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getActionType()));
        colAction.setPrefWidth(160);

        TableColumn<AuditLog, String> colDesc = new TableColumn<>("Operational Description");
        colDesc.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescription()));
        colDesc.setPrefWidth(400);

        table.getColumns().addAll(colTime, colUser, colAction, colDesc);
        table.getItems().addAll(auditService.getRecentAuditLogs());

        view.getChildren().addAll(title, table);
        return view;
    }

    // ── Dialogs & Modals ──────────────────────────────────────────────────────
    private void openAddCollarDialog() {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Register New Collared Wildlife");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: #111827;");

        Label title = new Label("📋 Register Animal Collar");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #F8FAFC;");

        Label err = new Label();
        err.setStyle("-fx-text-fill: #EF4444; -fx-font-size: 10px;");
        err.setVisible(false);

        TextField nameField = new TextField();
        nameField.setPromptText("Animal Name (e.g. Echo's Bull)");
        nameField.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        ComboBox<String> speciesCombo = new ComboBox<>();
        speciesCombo.getItems().addAll(
                "African Bush Elephant", "Eastern Black Rhino", "Northern White Rhino",
                "Southern White Rhino", "African Lion", "Cheetah", "Maasai Giraffe",
                "Rothschild's Giraffe", "Grevy's Zebra", "Plains Zebra", "Mountain Bongo", "Sitatunga"
        );
        speciesCombo.setValue("African Bush Elephant");
        speciesCombo.setMaxWidth(Double.MAX_VALUE);
        speciesCombo.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        TextField collarField = new TextField();
        collarField.setPromptText("Collar ID (e.g. KWS-AMB-ELE99)");
        collarField.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        ComboBox<String> parkCombo = new ComboBox<>();
        parkCombo.getItems().addAll(
                "Amboseli NP", "Tsavo East NP", "Tsavo West NP", "Maasai Mara",
                "Nairobi NP", "Ol Pejeta", "Lake Nakuru", "Samburu", "Aberdare", "Mount Kenya", "Lewa"
        );
        parkCombo.setValue("Amboseli NP");
        parkCombo.setMaxWidth(Double.MAX_VALUE);
        parkCombo.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        ComboBox<String> sexCombo = new ComboBox<>();
        sexCombo.getItems().addAll("Male", "Female");
        sexCombo.setValue("Female");
        sexCombo.setMaxWidth(Double.MAX_VALUE);
        sexCombo.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1;");
        cancelBtn.setOnAction(e -> dialog.close());

        Button saveBtn = new Button("Save to Database");
        saveBtn.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        saveBtn.setOnAction(e -> {
            try {
                AnimalDto dto = new AnimalDto();
                dto.setName(nameField.getText());
                dto.setSpecies(speciesCombo.getValue());
                dto.setCollarId(collarField.getText());
                dto.setParkName(parkCombo.getValue());
                dto.setSex(sexCombo.getValue());

                animalService.registerAnimal(dto, userService.getCurrentUser() != null ? userService.getCurrentUser().getUsername() : "COMMANDER");
                refreshData();
                dialog.close();
            } catch (Exception ex) {
                err.setText("❌ " + ex.getMessage());
                err.setVisible(true);
            }
        });

        btnBar.getChildren().addAll(cancelBtn, saveBtn);

        root.getChildren().addAll(title, err, new Label("Animal Name:"), nameField,
                new Label("Species:"), speciesCombo, new Label("Collar ID:"), collarField,
                new Label("Assigned Park:"), parkCombo, new Label("Sex:"), sexCombo, btnBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openDispatchModalForIncident(IncidentLog inc) {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Dispatch Patrol Unit");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: #111827;");

        Label title = new Label("⚡ RAPID DISPATCH ORDER");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #EF4444;");

        Label incInfo = new Label("Incident: " + (inc.getIncidentCode() != null ? inc.getIncidentCode() : "#" + inc.getId()) + " (" + inc.getAnimalName() + ")");
        incInfo.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");

        Label unitLabel = new Label("Select Available Patrol Unit:");
        ComboBox<PatrolUnitDto> unitCombo = new ComboBox<>();
        List<PatrolUnitDto> avail = cachedPatrols.stream().filter(p -> "AVAILABLE".equalsIgnoreCase(p.getStatus())).collect(Collectors.toList());
        unitCombo.getItems().addAll(avail);
        if (!avail.isEmpty()) unitCombo.getSelectionModel().selectFirst();
        unitCombo.setMaxWidth(Double.MAX_VALUE);
        unitCombo.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        TextField notesField = new TextField("Rapid interception of breached collar along perimeter");
        notesField.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #CBD5E1;");
        cancel.setOnAction(e -> dialog.close());

        Button dispatch = new Button("Confirm Dispatch Order");
        dispatch.setStyle("-fx-background-color: #DC2626; -fx-text-fill: white; -fx-font-weight: bold;");
        dispatch.setOnAction(e -> {
            PatrolUnitDto u = unitCombo.getValue();
            if (u != null) {
                patrolService.createDispatch(u.getId(), inc.getId(), inc.getLatitude(), inc.getLongitude(), inc.getParkName(), notesField.getText(), "COMMANDER");
                refreshData();
                dialog.close();
            }
        });

        btnBar.getChildren().addAll(cancel, dispatch);
        root.getChildren().addAll(title, incInfo, unitLabel, unitCombo, new Label("Operational Notes:"), notesField, btnBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openResolveModal(IncidentLog inc) {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Resolve Human-Wildlife Conflict Incident");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: #111827;");

        Label title = new Label("✅ Incident Resolution");
        title.setStyle("-fx-font-family: 'Montserrat', sans-serif; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #10B981;");

        TextArea notes = new TextArea("Field patrol unit successfully guided animal back to sanctuary. Perimeter fence integrity verified.");
        notes.setWrapText(true);
        notes.setStyle("-fx-background-color: #161F2E; -fx-text-fill: white;");

        HBox btnBar = new HBox(10);
        btnBar.setAlignment(Pos.CENTER_RIGHT);

        Button cancel = new Button("Cancel");
        cancel.setOnAction(e -> dialog.close());

        Button resolve = new Button("Mark Resolved");
        resolve.setStyle("-fx-background-color: #10B981; -fx-text-fill: white; -fx-font-weight: bold;");
        resolve.setOnAction(e -> {
            incidentService.resolveIncident(inc.getId(), notes.getText(), "COMMANDER");
            refreshData();
            dialog.close();
        });

        btnBar.getChildren().addAll(cancel, resolve);
        root.getChildren().addAll(title, new Label("Incident: " + inc.getAnimalName()), notes, btnBar);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openAnimalDetailDialog(AnimalDto animal) {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(animal.getName() + " — Tactical Animal Profile");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(420);
        root.setStyle("-fx-background-color: #111827;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        ImageView iv = new ImageView();
        iv.setFitWidth(64);
        iv.setFitHeight(64);
        iv.setPreserveRatio(true);
        try {
            InputStream is = getClass().getResourceAsStream("/images/" + animal.getImageFileName());
            if (is != null) iv.setImage(new Image(is));
        } catch (Exception ignored) {}

        VBox titles = new VBox(2);
        Label name = new Label(animal.getName());
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #FFD700;");
        Label sub = new Label(animal.getSpecies() + " • " + animal.getCollarId() + " (" + animal.getIucnStatus() + ")");
        sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #94A3B8;");
        titles.getChildren().addAll(name, sub);
        header.getChildren().addAll(iv, titles);

        VBox stats = new VBox(6);
        stats.setPadding(new Insets(10));
        stats.setStyle("-fx-background-color: #161F2E; -fx-background-radius: 6px;");

        stats.getChildren().addAll(
                new Label("📍 GPS Fix: " + String.format("%.4f°N, %.4f°E", animal.getLatitude(), animal.getLongitude())),
                new Label("⚡ Battery & Health: " + animal.getCollarBattery() + "% (" + animal.getSolarVoltageDisplay() + ")"),
                new Label("💨 Movement: " + String.format("%.1f km/h [Heading: %s]", animal.getSpeedKmh() != null ? animal.getSpeedKmh() : 0.0, animal.getHeadingCompass())),
                new Label("💤 Behavior: " + animal.getBehaviorState()),
                new Label("🛡️ Sanctuary Proximity: " + animal.getFenceDistance())
        );

        for (Node n : stats.getChildren()) {
            if (n instanceof Label l) l.setStyle("-fx-font-size: 11px; -fx-text-fill: #CBD5E1;");
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button centerMapBtn = new Button("🎯 Center Map");
        centerMapBtn.setStyle("-fx-background-color: #1E293B; -fx-text-fill: #38BDF8;");
        centerMapBtn.setOnAction(e -> {
            gisMapCanvas.centerOn(animal.getLatitude(), animal.getLongitude(), 380.0);
            gisMapCanvas.setSelectedCollarId(animal.getCollarId());
            dialog.close();
        });

        Button retireBtn = new Button("Retire Collar");
        retireBtn.setStyle("-fx-background-color: #7F1D1D; -fx-text-fill: #FCA5A5;");
        retireBtn.setOnAction(e -> {
            animalService.retireCollar(animal.getCollarId(), "COMMANDER");
            refreshData();
            dialog.close();
        });

        Button close = new Button("Close");
        close.setOnAction(e -> dialog.close());

        actions.getChildren().addAll(centerMapBtn, retireBtn, close);
        root.getChildren().addAll(header, stats, actions);

        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private void openSmsBroadcastModal() {
        Stage dialog = new Stage();
        dialog.initOwner(primaryStage);
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Emergency Community SMS Gateway");

        VBox root = new VBox(12);
        root.setPadding(new Insets(20));
        root.setPrefWidth(380);
        root.setStyle("-fx-background-color: #111827;");

        Label title = new Label("📢 KWS BUFFER COMMUNITY SMS DISPATCH");
        title.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #F59E0B;");

        ComboBox<String> corridorCombo = new ComboBox<>();
        corridorCombo.getItems().addAll(
                "Kimana Community Agricultural Buffer (54 pastoralists)",
                "Southern Kitengela Dispersal Corridor (38 farmers)",
                "Tsavo Voi SGR Community Boundary (42 rangers & farms)"
        );
        corridorCombo.setValue("Kimana Community Agricultural Buffer (54 pastoralists)");
        corridorCombo.setMaxWidth(Double.MAX_VALUE);

        TextArea msgArea = new TextArea("KWS ALERT: Bull elephant approaching farmland sector. Keep livestock safe. KWS rangers dispatched.");
        msgArea.setWrapText(true);

        Button send = new Button("Send Emergency SMS Broadcast");
        send.setStyle("-fx-background-color: #F59E0B; -fx-text-fill: #0F172A; -fx-font-weight: bold;");
        send.setOnAction(e -> {
            incidentService.broadcastCommunitySms(corridorCombo.getValue(), msgArea.getText());
            dialog.close();
        });

        root.getChildren().addAll(title, corridorCombo, msgArea, send);
        Scene scene = new Scene(root);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    // ── Background Polling & Data Refresh ─────────────────────────────────────
    private void loadStaticReferenceData() {
        try {
            cachedGeofences = geofenceRepository.findAll();
            cachedLandmarks = landmarkRepository.findAll();
            cachedParkProfiles = parkProfileRepository.findAll();
            refreshData();
        } catch (Exception e) {
            log.error("Failed to load reference GIS data: {}", e.getMessage(), e);
        }
    }

    private void startBackgroundPolling() {
        pollingExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "kws-desktop-poller");
            t.setDaemon(true);
            return t;
        });

        pollingExecutor.scheduleAtFixedRate(this::refreshData, 1, 3, TimeUnit.SECONDS);
    }

    private void refreshData() {
        try {
            List<AnimalDto> animals = animalService.getAllAnimals();
            List<PatrolUnitDto> patrols = patrolService.getAllPatrols();
            List<IncidentLog> incidents = incidentService.getAllIncidentEntities();
            List<AuditLog> auditLogs = auditService.getRecentAuditLogs();
            List<ParkWeatherDto> weather = weatherService.getAllWeather();

            // Fetch trails
            Map<String, List<TelemetryTrailDto>> trails = new HashMap<>();
            for (AnimalDto a : animals) {
                trails.put(a.getCollarId(), telemetryService.getAnimalTrail(a.getCollarId()));
            }

            Platform.runLater(() -> {
                cachedAnimals = animals;
                cachedPatrols = patrols;
                cachedIncidents = incidents;
                cachedTrails = trails;

                // Update Header clock
                if (clockLabel != null) {
                    clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT) + " EAT");
                }

                // Update KPIs
                long activeAlertsCount = incidents.stream().filter(i -> !"RESOLVED".equalsIgnoreCase(i.getStatus())).count();
                if (kpiAnimals != null) kpiAnimals.updateValue(animals.size() + " Tracked", "100% Online");
                if (kpiPatrols != null) {
                    long avail = patrols.stream().filter(p -> "AVAILABLE".equalsIgnoreCase(p.getStatus())).count();
                    kpiPatrols.updateValue(patrols.size() + " Units", avail + " Available • " + (patrols.size() - avail) + " Active");
                }
                if (kpiAlerts != null) {
                    kpiAlerts.updateValue(activeAlertsCount + " Active", activeAlertsCount > 0 ? "Breaches Reported" : "Sanctuaries Secure");
                }
                if (kpiFixes != null) {
                    long fixes = telemetryService.getAllTelemetry().size();
                    kpiFixes.updateValue(fixes + " Fixes", "12s Polling Active");
                }

                // Update Battery Gauge
                if (batteryGauge != null) {
                    int good = 0, mod = 0, low = 0;
                    double sum = 0;
                    for (AnimalDto a : animals) {
                        int bat = a.getCollarBattery() != null ? a.getCollarBattery() : 95;
                        sum += bat;
                        if (bat > 70) good++;
                        else if (bat >= 20) mod++;
                        else low++;
                    }
                    double avg = animals.isEmpty() ? 100 : sum / animals.size();
                    batteryGauge.updateHealth(good, mod, low, avg);
                }

                // Update Critical Alert Banner
                if (alertBanner != null) {
                    Optional<IncidentLog> critical = incidents.stream()
                            .filter(i -> "ACTIVE".equalsIgnoreCase(i.getStatus()))
                            .findFirst();
                    critical.ifPresentOrElse(alertBanner::showAlert, alertBanner::clearAlert);
                }

                // Update Weather Strip
                if (weatherStrip != null) {
                    weatherStrip.updateWeather(weather);
                }

                // Update Activity Feed
                if (activityFeed != null) {
                    activityFeed.updateFeed(auditLogs);
                }

                // Update Map
                if (gisMapCanvas != null) {
                    gisMapCanvas.updateData(cachedGeofences, cachedLandmarks, animals, patrols, incidents, trails);
                }

                // Apply Filters to Wildlife Roster
                applyFilters();
            });
        } catch (Exception e) {
            log.error("Error refreshing operations data: {}", e.getMessage());
        }
    }

    private void applyFilters() {
        if (wildlifeRosterContainer == null) return;
        wildlifeRosterContainer.getChildren().clear();

        List<AnimalDto> filtered = cachedAnimals.stream().filter(a -> {
            boolean matchesPark = "All Kenya".equalsIgnoreCase(selectedPark) ||
                    (a.getParkName() != null && a.getParkName().equalsIgnoreCase(selectedPark));

            boolean matchesSpecies = "All Species".equalsIgnoreCase(selectedSpecies) ||
                    (a.getSpecies() != null && a.getSpecies().toLowerCase().contains(selectedSpecies.toLowerCase()));

            boolean matchesSearch = searchQuery.isEmpty() ||
                    a.getName().toLowerCase().contains(searchQuery) ||
                    a.getCollarId().toLowerCase().contains(searchQuery) ||
                    (a.getSpecies() != null && a.getSpecies().toLowerCase().contains(searchQuery)) ||
                    (a.getParkName() != null && a.getParkName().toLowerCase().contains(searchQuery));

            return matchesPark && matchesSpecies && matchesSearch;
        }).collect(Collectors.toList());

        for (AnimalDto animal : filtered) {
            WildlifeCardComponent card = new WildlifeCardComponent(animal, this::openAnimalDetailDialog);
            wildlifeRosterContainer.getChildren().add(card);
        }
    }

    private double getParkCenterLat(String park) {
        return switch (park) {
            case "Amboseli NP" -> -2.65;
            case "Tsavo East NP" -> -2.77;
            case "Tsavo West NP" -> -3.32;
            case "Maasai Mara" -> -1.50;
            case "Nairobi NP" -> -1.37;
            case "Ol Pejeta" -> 0.03;
            case "Lake Nakuru" -> -0.37;
            case "Samburu" -> 0.62;
            case "Aberdare" -> -0.42;
            case "Mount Kenya" -> -0.15;
            case "Lewa" -> 0.20;
            case "Saiwa Swamp" -> 1.09;
            case "Mount Elgon" -> 1.13;
            default -> -0.50;
        };
    }

    private double getParkCenterLng(String park) {
        return switch (park) {
            case "Amboseli NP" -> 37.26;
            case "Tsavo East NP" -> 38.77;
            case "Tsavo West NP" -> 38.00;
            case "Maasai Mara" -> 35.15;
            case "Nairobi NP" -> 36.86;
            case "Ol Pejeta" -> 36.95;
            case "Lake Nakuru" -> 36.08;
            case "Samburu" -> 37.53;
            case "Aberdare" -> 36.70;
            case "Mount Kenya" -> 37.30;
            case "Lewa" -> 37.45;
            case "Saiwa Swamp" -> 35.12;
            case "Mount Elgon" -> 34.58;
            default -> 37.50;
        };
    }
}
