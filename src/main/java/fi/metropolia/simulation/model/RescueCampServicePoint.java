package fi.metropolia.simulation.model;

import eduni.distributions.ContinuousGenerator;
import fi.metropolia.simulation.framework.*;

import java.util.LinkedList;

public class RescueCampServicePoint {
    private LinkedList<fi.metropolia.simulation.model.Survivor> survivorQueue = new LinkedList<>();
    private ContinuousGenerator serviceTimeGenerator;
    private EventList campEventList;
    private RescueCampEventType scheduledEventType;
    private boolean serviceInProgress = false;
    private String servicePointName;

    // number of workers (counters) at this station
    private int workers = 1;

    // Performance statistics data
    private int totalSurvivorsServed = 0;
    private double cumulativeServiceTime = 0;
    private double cumulativeWaitingTime = 0;
    private int maximumQueueLength = 0;
    private double maxWaitingTimeObserved = 0;

    // Local variable to track max waiting time during service
    private double localMaxWaitingTime = 0;

    public RescueCampServicePoint(ContinuousGenerator serviceTimeGenerator, EventList campEventList,
                                  RescueCampEventType scheduledEventType, String servicePointName) {
        this.serviceTimeGenerator = serviceTimeGenerator;
        this.campEventList = campEventList;
        this.scheduledEventType = scheduledEventType;
        this.servicePointName = servicePointName;
    }

    // --- worker controls ---
    public void setWorkers(int n) { this.workers = Math.max(1, n); }
    public int getWorkers() { return workers; }

    public void addSurvivorToQueue(fi.metropolia.simulation.model.Survivor survivor) {
        survivorQueue.add(survivor);
        updateMaximumQueueLength();
    }

    public fi.metropolia.simulation.model.Survivor removeSurvivorFromQueue() {
        serviceInProgress = false;
        fi.metropolia.simulation.model.Survivor survivor = survivorQueue.poll();
        if (survivor != null) {
            totalSurvivorsServed++;
            double serviceTime = Clock.getInstance().getClock() - survivor.getCampArrivalTime();
            cumulativeServiceTime += serviceTime;
            double waitingTime = survivor.getTotalWaitingTime();
            cumulativeWaitingTime += waitingTime;
            if (waitingTime > maxWaitingTimeObserved) maxWaitingTimeObserved = waitingTime;
            if (waitingTime > localMaxWaitingTime) localMaxWaitingTime = waitingTime;
        }
        return survivor;
    }

    public void beginServiceForSurvivor() {
        if (serviceInProgress || survivorQueue.isEmpty()) return; // minimal safety
        fi.metropolia.simulation.model.Survivor currentSurvivor = survivorQueue.peek();
        serviceInProgress = true;

        double baseServiceDuration = serviceTimeGenerator.sample();
        double actualServiceDuration = calculateActualServiceTime(currentSurvivor, baseServiceDuration);

        // speed up service with more workers (simple parallelism approximation)
        actualServiceDuration = actualServiceDuration / Math.max(1, workers);
        actualServiceDuration = Math.max(0.0001, actualServiceDuration); // clamp small/negative

        recordServiceStartTime(currentSurvivor);
        Event serviceCompletionEvent =
                new Event(scheduledEventType, Clock.getInstance().getClock() + actualServiceDuration);
        campEventList.add(serviceCompletionEvent);
    }

    private double calculateActualServiceTime(fi.metropolia.simulation.model.Survivor survivor, double baseDuration) {
        double serviceDuration = baseDuration;
        switch (scheduledEventType) {
            // Supplies no longer scales with family size (no-family scenario)
            case SUPPLIES_DISTRIBUTION_COMPLETE:
                // keep generator-driven time
                break;
            // Family shelter removed; keep fixed 5 for child/adult shelters
            case CHILD_SHELTER_ASSIGNMENT_COMPLETE:
            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
                serviceDuration = 5;
                break;
            // Accommodation center uses its generator (no override needed)
            default:
                break;
        }
        return serviceDuration;
    }

    private void recordServiceStartTime(fi.metropolia.simulation.model.Survivor survivor) {
        double currentTime = Clock.getInstance().getClock();
        double waitingTime = currentTime - survivor.getCampArrivalTime();
        survivor.addWaitingTime(waitingTime);
        if (waitingTime > maxWaitingTimeObserved) maxWaitingTimeObserved = waitingTime;
        if (waitingTime > localMaxWaitingTime) localMaxWaitingTime = waitingTime;

        switch (scheduledEventType) {
            case MEDICAL_TREATMENT_COMPLETE:
                survivor.setMedicalTreatmentStartTime(currentTime);
                break;
            case REGISTRATION_COMPLETE:
                survivor.setRegistrationStartTime(currentTime);
                break;
            case COMMUNICATION_SERVICE_COMPLETE:
                survivor.setCommunicationServiceStartTime(currentTime);
                break;
            case SUPPLIES_DISTRIBUTION_COMPLETE:
                survivor.setSuppliesDistributionStartTime(currentTime);
                break;
            case ACCOMMODATION_CENTER_COMPLETE:
                survivor.setAccommodationCenterStartTime(currentTime);
                break;
            case CHILD_SHELTER_ASSIGNMENT_COMPLETE:
                survivor.setChildShelterAssignmentStartTime(currentTime);
                break;
            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
                survivor.setAdultShelterAssignmentStartTime(currentTime);
                break;
            default:
                break;
        }
    }

    private void updateMaximumQueueLength() {
        if (survivorQueue.size() > maximumQueueLength) maximumQueueLength = survivorQueue.size();
    }

    public boolean isServiceInProgress() {
        return serviceInProgress;
    }

    public boolean hasSurvivorsInQueue() {
        return !survivorQueue.isEmpty();
    }

    public int getCurrentQueueLength() {
        return survivorQueue.size();
    }

    public String getServicePointName() {
        return servicePointName;
    }

    public int getTotalSurvivorsServed() {
        return totalSurvivorsServed;
    }

    public double getAverageServiceTime() {
        return totalSurvivorsServed > 0 ? cumulativeServiceTime / totalSurvivorsServed : 0;
    }

    public int getMaximumQueueLength() {
        return maximumQueueLength;
    }

    public double getCumulativeServiceTime() {
        return cumulativeServiceTime;
    }

    public double getCumulativeWaitingTime() {
        return cumulativeWaitingTime;
    }

    public int getTotalServed() {
        return totalSurvivorsServed;
    }

    public double getMaxWaitingTime() {
        return maxWaitingTimeObserved;
    }

    public double getLocalMaxWaitingTime() {
        return localMaxWaitingTime;
    }
}
