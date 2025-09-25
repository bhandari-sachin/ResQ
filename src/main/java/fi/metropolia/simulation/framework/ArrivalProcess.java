package fi.metropolia.simulation.framework;

import eduni.distributions.ContinuousGenerator;

/**
 * ArrivalProcess produces the time when the next survivor arrives.
 * This is based on the current clock time and a random generator.
 */
public class ArrivalProcess {
    private final ContinuousGenerator generator;
    private final EventList eventList;
    private final IEventType type;

    /**
     * Create the arrival process for survivors.
     *
     * @param g    Random number generator for survivor inter-arrival times
     * @param tl   Simulator event list, used to insert survivor arrival events
     * @param type Event type for survivor arrival
     */
    public ArrivalProcess(ContinuousGenerator g, EventList tl, IEventType type) {
        this.generator = g;
        this.eventList = tl;
        this.type = type;
    }

    /**
     * Generate the next survivor arrival event and put it on the event list.
     */
    public void generateNextEvent() {
        Event t = new Event(type, Clock.getInstance().getClock() + generator.sample());
        eventList.add(t);
    }
}
