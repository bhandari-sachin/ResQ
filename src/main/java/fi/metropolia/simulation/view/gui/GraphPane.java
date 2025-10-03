package fi.metropolia.simulation.view.gui;

import fi.metropolia.simulation.model.SimulationEngine;
import fi.metropolia.simulation.model.Survivor;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Visualization panel (left side).
 * - Black background
 * - Green hollow circles for service points, white dot for Entry point
 * - White labels (centered above each circle, not overlapping)
 * - Survivors (stick figures) move along the edges
 * - Legend box in the top-right with colored figure samples
 */
public class GraphPane extends Pane {

    private final double W = 780, H = 680;

    // Node coordinates
    private final Point ENTRY          = p( 70, 300);
    private final Point REGISTRATION   = p(240, 320);
    private final Point EMERGENCY      = p(240, 190);
    private final Point COMMUNICATION  = p(460, 260);
    private final Point SUPPLIES       = p(460, 380);
    private final Point ACCOMMODATION  = p(600, 500);
    private final Point CHILD_SHELTER  = p(350, 520);
    private final Point ADULT_SHELTER  = p(350, 620);

    private final double R_ENTRY = 4;
    private final double R_NODE  = 20;
    private final double ARROW   = 10;
    private final Color  EDGE    = Color.WHITE;
    private final Color  NODE    = Color.LIMEGREEN;

    // Layers
    private final Group edges = new Group();
    private final Group nodes = new Group();
    private final Group labels = new Group();
    private final Group legend = new Group();
    private final Group peopleLayer = new Group();

    private SimulationEngine engine;
    private final List<Survivor> observed = new CopyOnWriteArrayList<>();

    // Track all active animations so we can pause/freeze them at the end
    private final List<Animation> activeAnimations = new ArrayList<>();

    public GraphPane() {
        setMinSize(W, H);
        setPrefSize(W, H);
        setMaxSize(W, H);
        setPadding(new Insets(8));
        setStyle("-fx-background-color: black;");

        drawGraph();
        getChildren().addAll(edges, nodes, labels, legend, peopleLayer);

        Timeline poll = new Timeline(new KeyFrame(Duration.millis(350), e -> tick()));
        poll.setCycleCount(Animation.INDEFINITE);
        poll.play();
    }

    public void bindToEngine(SimulationEngine engine) {
        this.engine = engine;
        this.observed.clear();
    }

    public void setIconsVisible(boolean v) { peopleLayer.setVisible(v); }

    public void bumpSpeed(double factor) {
        double newRate = Math.max(0.2, Math.min(8.0, SurvivorIcon.globalRate.get() * factor));
        SurvivorIcon.globalRate.set(newRate);
    }

    /** Clears survivors and stops animations (used when pressing Stop / new run). */
    public void reset() {
        Platform.runLater(() -> {
            // stop any running animations
            for (Animation a : activeAnimations) {
                try { a.stop(); } catch (Exception ignored) {}
            }
            activeAnimations.clear();

            peopleLayer.getChildren().clear();
            observed.clear();
        });
    }

    /** Freeze survivors exactly where they are (pause animations in place). */
    public void freezeAllAnimations() {
        Platform.runLater(() -> {
            for (Animation a : activeAnimations) {
                try { a.pause(); } catch (Exception ignored) {}
            }
        });
    }

    /* ===================== Drawing ===================== */

    private void drawGraph() {
        edges.getChildren().clear();
        nodes.getChildren().clear();
        labels.getChildren().clear();
        legend.getChildren().clear();

        // Entry point
        Circle entryDot = new Circle(ENTRY.x, ENTRY.y, R_ENTRY, Color.WHITE);
        nodes.getChildren().add(entryDot);
        labels.getChildren().add(labelAbove(ENTRY, "Entry point"));

        // Edges
        edge(ENTRY, EMERGENCY);
        edge(ENTRY, REGISTRATION);
        edge(EMERGENCY, REGISTRATION);
        edge(REGISTRATION, COMMUNICATION);
        edge(COMMUNICATION, SUPPLIES);
        edge(REGISTRATION, SUPPLIES);
        edge(SUPPLIES, ACCOMMODATION);
        edge(ACCOMMODATION, CHILD_SHELTER);
        edge(ACCOMMODATION, ADULT_SHELTER);

        // Service circles
        nodes.getChildren().addAll(
                node(EMERGENCY),
                node(REGISTRATION),
                node(COMMUNICATION),
                node(SUPPLIES),
                node(ACCOMMODATION),
                node(CHILD_SHELTER),
                node(ADULT_SHELTER)
        );

        // Labels (centered above, no overlap)
        labels.getChildren().addAll(
                labelAbove(EMERGENCY, "emergency\nunit"),
                labelAbove(REGISTRATION, "Registration\ndesk"),
                labelAbove(COMMUNICATION, "communication\ncenter"),
                labelAbove(SUPPLIES, "Supplies\ncenter"),
                labelAbove(ACCOMMODATION, "Accommodation\ncenter"),
                labelAbove(CHILD_SHELTER, "Child\nAccommodation"),
                labelAbove(ADULT_SHELTER, "Adult\nAccommodation")
        );

        drawLegend();
    }

    private void drawLegend() {
        final double LEGEND_W = 170;
        final double LEGEND_H = 120;
        final double MARGIN_R = 20;
        final double x = W - LEGEND_W - MARGIN_R; // anchor to right inside panel
        final double y = 30;

        Rectangle box = new Rectangle(x, y, LEGEND_W, LEGEND_H);
        box.setFill(Color.TRANSPARENT);
        box.setStroke(Color.WHITE);
        box.setStrokeWidth(3);

        Text title = new Text(x + 12, y + 22, "Legend");
        title.setFill(Color.WHITE);
        title.setFont(Font.font(16));

        double row1 = y + 48;
        double row2 = y + 74;
        double row3 = y + 100;
        double left = x + 12;

        Group itemChild   = legendItem(left, row1, Color.GOLD,      "Child");
        Group itemHealthy = legendItem(left, row2, Color.ROYALBLUE, "Healthy Adults");
        Group itemInjured = legendItem(left, row3, Color.CRIMSON,   "Injured Adults");

        legend.getChildren().addAll(box, title, itemChild, itemHealthy, itemInjured);
    }

    /** Kept for compatibility; display removed intentionally. */
    public void setArrivalMeanReadout(double meanMinutes) {
        // no-op
    }

    private Group legendItem(double x, double baselineY, Color bodyColor, String label) {
        Group g = new Group();
        double cx = x + 6; double cy = baselineY - 6;

        Circle head = new Circle(cx, cy - 7, 3, Color.WHITE);

        Line torso = new Line(cx, cy - 3, cx, cy + 5);
        torso.setStroke(bodyColor); torso.setStrokeWidth(2.0);

        Line armL = new Line(cx, cy - 1, cx - 5, cy + 2);
        armL.setStroke(bodyColor); armL.setStrokeWidth(1.6);

        Line armR = new Line(cx, cy - 1, cx + 5, cy + 2);
        armR.setStroke(bodyColor); armR.setStrokeWidth(1.6);

        Line legL = new Line(cx, cy + 5, cx - 4, cy + 10);
        legL.setStroke(bodyColor); legL.setStrokeWidth(1.6);

        Line legR = new Line(cx, cy + 5, cx + 4, cy + 10);
        legR.setStroke(bodyColor); legR.setStrokeWidth(1.6);

        Text t = new Text(x + 18, baselineY, label);
        t.setFill(Color.WHITE); t.setFont(Font.font(14));

        g.getChildren().addAll(head, torso, armL, armR, legL, legR, t);
        return g;
    }

    private Group node(Point p) {
        Circle c = new Circle(p.x, p.y, R_NODE);
        c.setStroke(NODE); c.setStrokeWidth(2.0); c.setFill(Color.TRANSPARENT);
        return new Group(c);
    }

    private Text labelAbove(Point p, String text) {
        final double pad = 10;
        Text t = new Text(text);
        t.setFill(Color.WHITE);
        t.setFont(Font.font(14));
        t.setTextAlignment(TextAlignment.CENTER);
        t.applyCss();
        Bounds b = t.getLayoutBounds();
        t.setX(p.x - b.getWidth() / 2.0);
        t.setY(p.y - R_NODE - pad - b.getHeight());
        return t;
    }

    private void edge(Point from, Point to) {
        double rFrom = (from == ENTRY ? R_ENTRY : R_NODE);
        double rTo   = (to   == ENTRY ? R_ENTRY : R_NODE);
        double dx = to.x - from.x, dy = to.y - from.y;
        double len = Math.hypot(dx, dy);
        double ux = dx / len, uy = dy / len;

        double sx = from.x + ux * (rFrom + 2);
        double sy = from.y + uy * (rFrom + 2);
        double ex = to.x - ux * (rTo + ARROW + 2);
        double ey = to.y - uy * (rTo + ARROW + 2);

        Line l = new Line(sx, sy, ex, ey);
        l.setStroke(EDGE); l.setStrokeWidth(2.0);

        Polygon head = arrowHead(ex, ey, ux, uy, ARROW, EDGE);
        edges.getChildren().addAll(l, head);
    }

    private Polygon arrowHead(double ex, double ey, double ux, double uy, double size, Color color) {
        double leftx  = ex - ux * size + (-uy) * size * 0.6;
        double lefty  = ey - uy * size + ( ux) * size * 0.6;
        double rightx = ex - ux * size + ( uy) * size * 0.6;
        double righty = ey - uy * size + (-ux) * size * 0.6;
        Polygon tri = new Polygon(ex, ey, leftx, lefty, rightx, righty);
        tri.setFill(color);
        return tri;
    }

    /* ===================== Animation ===================== */

    private void tick() {
        if (engine == null) return;
        List<Survivor> all = engine.getAllSurvivors();
        if (all.isEmpty()) return;

        int have = observed.size();
        for (int i = have; i < all.size(); i++) {
            Survivor s = all.get(i);
            observed.add(s);
            spawnIconFor(s);
        }
    }

    private void spawnIconFor(Survivor s) {
        Group person = SurvivorIcon.createFor(s);
        peopleLayer.getChildren().add(person);

        List<Point> path = new ArrayList<>();
        boolean needsMedical = s.requiresMedicalTreatment();
        if (needsMedical) { path.add(EMERGENCY); path.add(REGISTRATION); }
        else { path.add(REGISTRATION); }

        boolean isAdult = (s.getAgeCategory() == Survivor.AgeCategory.ADULT);
        if (isAdult && s.requestsCommunicationService()) path.add(COMMUNICATION);

        path.add(SUPPLIES); path.add(ACCOMMODATION);
        path.add(isAdult ? ADULT_SHELTER : CHILD_SHELTER);

        double legMs = 1800; // base travel time between points
        SequentialTransition seq = new SequentialTransition();
        Point prev = ENTRY;
        for (Point target : path) {
            seq.getChildren().add(move(prev, target, person, Duration.millis(legMs)));
            prev = target;
        }
        // scale with Speed up / Slow down
        seq.rateProperty().bind(SurvivorIcon.globalRate);

        // Remember this animation so we can freeze it at the end
        activeAnimations.add(seq);

        seq.play();
    }

    private PathTransition move(Point from, Point to, Group who, Duration base) {
        Path p = new Path(new MoveTo(from.x, from.y), new LineTo(to.x, to.y));
        PathTransition pt = new PathTransition(base, p, who);
        pt.setInterpolator(Interpolator.LINEAR);
        return pt;
    }

    private static class Point { final double x,y; Point(double x,double y){this.x=x;this.y=y;} }
    private static Point p(double x,double y){ return new Point(x,y); }
}
