package fi.metropolia.simulation.view.gui;

import fi.metropolia.simulation.csv.CsvExporter;
import fi.metropolia.simulation.model.SimulationEngine;
import fi.metropolia.simulation.model.Survivor;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainApp extends Application {

    private SimulationEngine engine;
    private Thread simThread;

    private GraphPane graphPane;
    private CheckBox showIcons;

    private TextField tfSimTime;
    private TextField tfArrivalMean;

    private Button btnStart, btnPause, btnStep, btnStop, btnFaster, btnSlower, btnExport;

    private TextField wtMedical, wtRegistration, wtCommunication, wtSupplies,
            wtAccommodation, wtAdultShelter, wtChildShelter;

    private Label lblTotalProcessed, lblAvgTotalTime, lblAvgWaiting;

    private final AtomicBoolean paused = new AtomicBoolean(false);

    @Override
    public void start(Stage stage) {
        stage.setTitle("ResQ");

        graphPane = new GraphPane();
        VBox controls = buildControls();

        BorderPane root = new BorderPane();
        root.setLeft(graphPane);
        root.setRight(controls);
        BorderPane.setMargin(graphPane, new Insets(8));
        BorderPane.setMargin(controls, new Insets(8));

        Scene scene = new Scene(root, 1200, 720);
        URL css = getClass().getResource("/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.show();

        stage.setOnCloseRequest(e -> {
            stopSimulation(false);
            Platform.exit();
            System.exit(0);
        });
    }

    private VBox buildControls() {
        tfSimTime = new TextField("480");
        tfArrivalMean = new TextField("20");
        tfSimTime.setPrefColumnCount(6);
        tfArrivalMean.setPrefColumnCount(6);

        GridPane inputs = new GridPane();
        inputs.setHgap(8); inputs.setVgap(6);
        inputs.add(new Label("Simulation time"), 0, 0);
        inputs.add(tfSimTime, 1, 0);
        inputs.add(new Label("Arrival mean"), 0, 1);
        inputs.add(tfArrivalMean, 1, 1);

        btnStart = new Button("Start");           btnStart.setId("btnStart");
        btnPause = new Button("Pause");           btnPause.setId("btnPause");
        btnStep = new Button("Step");             btnStep.setId("btnStep");
        btnStop = new Button("Stop");             btnStop.setId("btnStop");
        btnFaster = new Button("Speed up");       btnFaster.setId("btnFaster");
        btnSlower = new Button("Slow down");      btnSlower.setId("btnSlower");
        btnExport = new Button("Export CSV");     btnExport.setId("btnExport");

        HBox row1 = new HBox(8, btnStart, btnPause, btnStep, btnStop);
        HBox row2 = new HBox(8, btnExport, btnFaster, btnSlower);
        row1.setAlignment(Pos.CENTER_LEFT);
        row2.setAlignment(Pos.CENTER_LEFT);

        showIcons = new CheckBox("Show Survivor icons");
        showIcons.setSelected(true);
        showIcons.selectedProperty().addListener((obs, a, b) -> graphPane.setIconsVisible(b));

        VBox waitingBox = buildWaitingTimesBox();
        VBox statsBox = buildOverallStatsBox();

        wireControls();

        VBox controls = new VBox(12,
                new Label("Simulation"), inputs,
                row1, row2,
                showIcons,
                waitingBox,
                statsBox
        );
        controls.setPrefWidth(360);
        controls.setFillWidth(true);
        return controls;
    }

    private VBox buildWaitingTimesBox() {
        wtMedical = roTf();
        wtRegistration = roTf();
        wtCommunication = roTf();
        wtSupplies = roTf();
        wtAccommodation = roTf();
        wtAdultShelter = roTf();
        wtChildShelter = roTf();

        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(6);
        int r = 0;
        gp.add(new Label("emergency unit"), 0, r); gp.add(wtMedical, 1, r++);
        gp.add(new Label("Registration Desk"), 0, r); gp.add(wtRegistration, 1, r++);
        gp.add(new Label("Communication center"), 0, r); gp.add(wtCommunication, 1, r++);
        gp.add(new Label("Supplies"), 0, r); gp.add(wtSupplies, 1, r++);
        gp.add(new Label("Accommodation"), 0, r); gp.add(wtAccommodation, 1, r++);
        gp.add(new Label("Adult Accommodation"), 0, r); gp.add(wtAdultShelter, 1, r++);
        gp.add(new Label("Child Accommodation"), 0, r); gp.add(wtChildShelter, 1, r++);
        TitledPane tp = new TitledPane("Maximum waiting time", gp);
        tp.setCollapsible(false);
        return new VBox(tp);
    }

    private VBox buildOverallStatsBox() {
        lblTotalProcessed = new Label("-");
        lblAvgTotalTime = new Label("-");
        lblAvgWaiting = new Label("-");
        GridPane gp = new GridPane();
        gp.setHgap(8); gp.setVgap(6);
        int r = 0;
        gp.add(new Label("Total survivors processed:"), 0, r); gp.add(lblTotalProcessed, 1, r++);
        gp.add(new Label("Average total time in camp:"), 0, r); gp.add(lblAvgTotalTime, 1, r++);
        gp.add(new Label("Average waiting time:"), 0, r); gp.add(lblAvgWaiting, 1, r++);
        TitledPane tp = new TitledPane("Overall Simulation Statistics", gp);
        tp.setCollapsible(false);
        return new VBox(tp);
    }

    private TextField roTf() {
        TextField tf = new TextField();
        tf.setEditable(false);
        tf.setPrefColumnCount(8);
        return tf;
    }

    private void wireControls() {
        btnStart.setOnAction(e -> startSimulation());
        btnStop.setOnAction(e -> stopSimulation(true));
        btnExport.setOnAction(e -> exportCsv());
        btnFaster.setOnAction(e -> {
            if (engine != null) engine.setAnimationSpeedMultiplier(engine.getAnimationSpeedMultiplier() * 1.25);
            graphPane.bumpSpeed(1.25);
        });
        btnSlower.setOnAction(e -> {
            if (engine != null) engine.setAnimationSpeedMultiplier(engine.getAnimationSpeedMultiplier() / 1.25);
            graphPane.bumpSpeed(1.0 / 1.25);
        });
        btnPause.setOnAction(e -> togglePause());
        btnStep.setOnAction(e -> stepOnce());
    }

    private void startSimulation() {
        if (simThread != null && simThread.isAlive()) return;

        graphPane.reset();
        clearStats();

        double simMinutes  = parseOrDefault(tfSimTime.getText(), 480.0);
        double arrivalMean = parseOrDefault(tfArrivalMean.getText(), 20.0);

        engine = new SimulationEngine(arrivalMean);
        engine.setSimulationDuration(simMinutes);
        graphPane.bindToEngine(engine);

        simThread = new Thread(engine::startSimulation, "sim-thread");
        simThread.setDaemon(true);
        simThread.start();

        // When the simulation ends, show where the current CSV was written
        new Thread(() -> {
            try { simThread.join(); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                graphPane.freezeAllAnimations();
                fillStatsFromEngine();
                File auto = engine.getAutoExportFile();
                if (auto != null) {
                    new Alert(Alert.AlertType.INFORMATION,
                            "Saved current CSV to:\n" + auto.getAbsolutePath(),
                            ButtonType.OK).showAndWait();
                }
            });
        }, "sim-waiter").start();

        paused.set(false);
        btnPause.setText("Pause");
    }

    private void stopSimulation(boolean resetGraph) {
        if (simThread != null && simThread.isAlive()) {
            simThread.interrupt();
        }
        simThread = null;
        paused.set(false);
        btnPause.setText("Pause");
        if (resetGraph) graphPane.reset();
    }

    private void togglePause() {
        if (engine == null) return;
        if (paused.compareAndSet(false, true)) {
            engine.setBaseDelayMs(Integer.MAX_VALUE);
            btnPause.setText("Resume");
        } else {
            engine.setBaseDelayMs(200);
            if (simThread != null) simThread.interrupt();
            paused.set(false);
            btnPause.setText("Pause");
        }
    }

    private void stepOnce() {
        if (engine == null) return;
        if (!paused.get()) togglePause();
        if (simThread != null) simThread.interrupt();
    }

    /** Manual export — delete previous CSVs and write ONLY current rows + stats (2-decimal). */
    private void exportCsv() {
        if (engine == null) {
            new Alert(Alert.AlertType.WARNING, "Start the simulation first.").showAndWait();
            return;
        }
        List<Survivor> all = engine.getAllSurvivors();
        List<Survivor> processed = engine.getFullyProcessedSurvivors();

        File out = CsvExporter.writeOnlyCurrentWithStats(all, processed);  // <-- fixed call
        new Alert(Alert.AlertType.INFORMATION,
                "Saved current CSV to:\n" + out.getAbsolutePath(),
                ButtonType.OK).showAndWait();
    }

    private void fillStatsFromEngine() {
        if (engine == null) return;
        var meters = ServiceMeters.fromEngine(engine);

        wtMedical.setText(formatMin(meters.medicalMaxWait));
        wtRegistration.setText(formatMin(meters.registrationMaxWait));
        wtCommunication.setText(formatMin(meters.communicationMaxWait));
        wtSupplies.setText(formatMin(meters.suppliesMaxWait));
        wtAccommodation.setText(formatMin(meters.accommodationMaxWait));
        wtAdultShelter.setText(formatMin(meters.adultShelterMaxWait));
        wtChildShelter.setText(formatMin(meters.childShelterMaxWait));

        List<Survivor> done = engine.getFullyProcessedSurvivors();
        int n = done.size();
        double sumTotal = 0, sumWait = 0;
        for (Survivor s : done) { sumTotal += s.getTotalTimeInCamp(); sumWait += s.getTotalWaitingTime(); }
        lblTotalProcessed.setText(Integer.toString(n));
        lblAvgTotalTime.setText(n > 0 ? String.format("%.2f minutes", sumTotal / n) : "-");
        lblAvgWaiting.setText(n > 0 ? String.format("%.2f minutes", sumWait / n) : "-");
    }

    private String formatMin(double v) { return String.format("%.2f", v); }
    private void clearStats() {
        wtMedical.clear(); wtRegistration.clear(); wtCommunication.clear();
        wtSupplies.clear(); wtAccommodation.clear(); wtAdultShelter.clear(); wtChildShelter.clear();
        lblTotalProcessed.setText("-"); lblAvgTotalTime.setText("-"); lblAvgWaiting.setText("-");
    }
    private double parseOrDefault(String txt, double def) { try { return Double.parseDouble(txt.trim()); } catch (Exception e) { return def; } }

    @Override public void stop() { stopSimulation(false); Platform.exit(); System.exit(0); }
    public static void main(String[] args) { launch(args); }
}
