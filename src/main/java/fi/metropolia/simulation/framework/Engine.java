package fi.metropolia.simulation.framework;

/**
 * Engine implements a three-phase simulator. See <a href="https://www.jstor.org/stable/2584330">Three-Phase Simulator</a>
 * <p>
 * This is a skeleton of a three-phase simulator. You need to implement abstract methods for your
 * simulation’s purpose.
 */
public abstract class Engine {
    private double simulationTime = 0;    // time when the simulation will be stopped
    private Clock clock;                  // to simplify the code (clock.getClock() instead of Clock.getInstance().getClock())
    protected EventList eventList;        // events to be processed are stored here

    /**
     * Service Points are created in fi.metropolia.simulation.model-package’s class inheriting the Engine class
     */
    public Engine() {
        clock = Clock.getInstance();    // to improve the speed of the simulation
        eventList = new EventList();
    }

    /**
     * Define how long we will run the simulation
     *
     * @param time Ending time of the simulation
     */
    public void setSimulationTime(double time) {    // define how long we will run the simulation
        simulationTime = time;
    }

    /**
     * The starting point of the simulator. Returns when the simulation ends.
     */
    public void run() {
        initialize(); // e.g., schedule the first survivor arrival

        while (simulate()) {
            Trace.out(Trace.Level.INFO, "\nA-phase: time is " + currentTime());
            clock.setClock(currentTime());

            Trace.out(Trace.Level.INFO, "\nB-phase:");
            runBEvents();

            Trace.out(Trace.Level.INFO, "\nC-phase:");
            tryCEvents();
        }

        results();
    }

    /**
     * Execute all B-events (bound to time) at the current time, removing them from the event list.
     */
    private void runBEvents() {
        while (eventList.getNextEventTime() == clock.getClock()) {
            runEvent(eventList.remove());
        }
    }

    /**
     * @return Earliest event time in the event list
     */
    private double currentTime() {
        return eventList.getNextEventTime();
    }

    /**
     * @return whether we should continue simulation
     */
    private boolean simulate() {
        return clock.getClock() < simulationTime;
    }

    /**
     * Execute event actions (e.g., removing a survivor from a queue)
     * Defined in fi.metropolia.simulation.model-package’s class inheriting Engine
     *
     * @param t The event to be executed
     */
    protected abstract void runEvent(Event t);

    /**
     * Execute all possible C-events (conditional events)
     * Defined in fi.metropolia.simulation.model-package’s class inheriting Engine
     */
    protected abstract void tryCEvents();

    /**
     * Set all data structures to initial values
     * Defined in fi.metropolia.simulation.model-package’s class inheriting Engine
     */
    protected abstract void initialize();

    /**
     * Show/analyze measurement parameters collected during the simulation.
     * Called at the end of the simulation.
     * Defined in fi.metropolia.simulation.model-package’s class inheriting Engine
     */
    protected abstract void results();
}
