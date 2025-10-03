package fi.metropolia.simulation.view.gui;

import fi.metropolia.simulation.model.Survivor;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

/**
 * Small walking stick figure with head, arms and legs.
 * Colors per spec:
 *  - CHILD -> yellow
 *  - ADULT injured -> red
 *  - ADULT healthy -> blue
 *
 * The animation rate of all icons is shared via globalRate.
 */
public final class SurvivorIcon {

    /** Shared rate for all figure animations (hooked to Speed up / Slow down). */
    public static final DoubleProperty globalRate = new SimpleDoubleProperty(1.0);

    private SurvivorIcon() {}

    public static Group createFor(Survivor s) {
        Color bodyColor;
        if (s.getAgeCategory() == Survivor.AgeCategory.CHILD) {
            bodyColor = Color.GOLD;          // yellow
        } else if (s.getHealthCondition() == Survivor.HealthCondition.INJURED) {
            bodyColor = Color.CRIMSON;       // red
        } else {
            bodyColor = Color.ROYALBLUE;     // blue
        }

        // head (white so it shows on black background)
        Circle head = new Circle(0, -10, 4);
        head.setFill(Color.WHITE);

        // torso & limbs in the type color
        Line torso = new Line(0, -6, 0, 6);
        torso.setStroke(bodyColor);
        torso.setStrokeWidth(3.0);

        Line armL = new Line(0, -2, -6, 2);
        armL.setStroke(bodyColor);
        armL.setStrokeWidth(2.0);
        Line armR = new Line(0, -2, 6, 2);
        armR.setStroke(bodyColor);
        armR.setStrokeWidth(2.0);

        Line legL = new Line(0, 6, -5, 12);
        legL.setStroke(bodyColor);
        legL.setStrokeWidth(2.0);
        Line legR = new Line(0, 6, 5, 12);
        legR.setStroke(bodyColor);
        legR.setStrokeWidth(2.0);

        Group g = new Group(head, torso, armL, armR, legL, legR);

        // gentle swing to suggest walking
        RotateTransition rt = new RotateTransition(Duration.millis(500), g);
        rt.setFromAngle(-8);
        rt.setToAngle(8);
        rt.setAutoReverse(true);
        rt.setCycleCount(Timeline.INDEFINITE);
        rt.rateProperty().bind(globalRate);
        rt.play();

        return g;
    }
}
