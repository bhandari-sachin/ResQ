package fi.metropolia.simulation.model;

import eduni.distributions.ContinuousGenerator;
import fi.metropolia.simulation.framework.Clock;
import fi.metropolia.simulation.framework.Event;
import fi.metropolia.simulation.framework.EventList;

import java.util.*;

/**
 * Represents a single service point in the rescue camp.
 * Manages its queue, worker capacity, schedules completion events,
 * and keeps simple statistics used by the console/GUI views.
 */
public class RescueCampServicePoint {

    private final ContinuousGenerator serviceTimeGenerator;
    private final EventList eventList;
    private final RescueCampEventType completionEventType;
    private final String name;

    private int workers = 1;
    private int busyWorkers = 0;

    // Queue of survivors waiting to be served
    private final Queue<Survivor> queue = new LinkedList<>();
    // When each survivor entered this service-point queue
    private final Map<Survivor, Double> enqueueTimes = new HashMap<>();

    // ---- Stats (queried by views) ----
    private double maxWaitingTime = 0.0; // minutes
    private double sumServiceTime = 0.0; // minutes
    private int totalServed = 0;

    public RescueCampServicePoint(ContinuousGenerator serviceTimeGenerator,
                                  EventList eventList,
                                  RescueCampEventType completionEventType,
                                  String name) {
        this.serviceTimeGenerator = serviceTimeGenerator;
        this.eventList = eventList;
        this.completionEventType = completionEventType;
        this.name = name;
    }

    /* ------------ configuration ------------ */

    public void setWorkers(int workers) { this.workers = Math.max(0, workers); }
    public int getWorkers() { return workers; }
    public String getServicePointName() { return name; }     // used by view
    public String getName() { return name; }                 // kept for any existing calls

    /* ------------ queue ops ------------ */

    public void addSurvivorToQueue(Survivor survivor) {
        queue.add(survivor);
        enqueueTimes.put(survivor, Clock.getInstance().getClock());
    }

    public boolean hasSurvivorsInQueue() { return !queue.isEmpty(); }
    public int getCurrentQueueLength() { return queue.size(); }  // used by view
    public boolean isServiceInProgress() { return busyWorkers > 0; }
    public int getBusyWorkers() { return busyWorkers; }

    /**
     * Start service for the head of the queue if a worker is free,
     * compute waiting time for that survivor (record to survivor and stats),
     * and schedule a completion event.
     */
    public void beginServiceForSurvivor() {
        if (busyWorkers >= workers || queue.isEmpty()) return;

        Survivor survivor = queue.peek(); // keep in queue until completion
        if (survivor == null) return;

        busyWorkers++;

        // Compute waiting time for this survivor at this service point
        double now = Clock.getInstance().getClock();
        Double enq = enqueueTimes.remove(survivor);
        if (enq != null) {
            double waited = Math.max(0.0, now - enq);
            // Update per-point max
            if (waited > maxWaitingTime) maxWaitingTime = waited;
            // IMPORTANT: also accumulate into the survivor so CSV column "waiting_time" is non-zero
            survivor.addWaitingTime(waited);
        }

        // Draw service time and schedule completion
        double serviceTime = serviceTimeGenerator.sample();
        sumServiceTime += serviceTime;

        double completionTime = now + serviceTime;
        // EventList API is add(Event)
        eventList.add(new Event(completionEventType, completionTime));
    }

    /**
     * Called by the engine when a completion event for this service point fires.
     * Returns the survivor that just finished (or null if none).
     */
    public Survivor removeSurvivorFromQueue() {
        if (busyWorkers <= 0) return null;
        busyWorkers--;

        Survivor finished = queue.poll();
        if (finished != null) {
            totalServed++;
        }
        return finished;
    }

    /* ------------ Statistics getters (used by views) ------------ */

    /** Total survivors whose service finished at this point. */
    public int getTotalServed() { return totalServed; }

    /** Average service time for completed services (minutes). */
    public double getAverageServiceTime() {
        return totalServed > 0 ? (sumServiceTime / totalServed) : 0.0;
    }

    /** Maximum observed waiting time in this queue (minutes). */
    public double getMaxWaitingTime() { return maxWaitingTime; }
}
