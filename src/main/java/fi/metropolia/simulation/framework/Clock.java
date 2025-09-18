package fi.metropolia.simulation.framework;

/**
 * Singleton for holding global simulation time
 */
public class Clock {
    private double clock;
    private static Clock instance;

    private Clock() {
        clock = 0;
    }

    public static Clock getInstance() {
        if (instance == null) {
            instance = new Clock();
        }
        return instance;
    }

    public void setClock(double clock) {
        this.clock = clock;
    }

    public double getClock() {
        return clock;
    }

    public void reset() {
        clock = 0;
    }
}
