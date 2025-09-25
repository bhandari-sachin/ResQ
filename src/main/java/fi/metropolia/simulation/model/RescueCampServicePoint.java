package fi.metropolia.simulation.model;

import eduni.distributions.ContinuousGenerator;
import fi.metropolia.simulation.framework.Clock;
import fi.metropolia.simulation.framework.Event;
import fi.metropolia.simulation.framework.EventList;

import java.util.LinkedList;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class RescueCampServicePoint {
    private final LinkedList<Survivor> survivorQueue = new LinkedList<>();
    private final ContinuousGenerator serviceTimeGenerator;
    private final EventList campEventList;
    private final RescueCampEventType scheduledEventType;
    private boolean serviceInProgress = false;
    private final String servicePointName;

    private int workers = 1;

    // Stats
    private int totalSurvivorsServed = 0;
    private double cumulativeServiceTime = 0;
    private double cumulativeWaitingTime = 0;
    private int maximumQueueLength = 0;
    private double maxWaitingTimeObserved = 0;
    private double localMaxWaitingTime = 0;

    // CSV file path
    private static final Path ASSIGNMENT_CSV = Path.of("survivor_assignments.csv");

    public RescueCampServicePoint(ContinuousGenerator serviceTimeGenerator,
                                  EventList campEventList,
                                  RescueCampEventType scheduledEventType,
                                  String servicePointName) {
        this.serviceTimeGenerator = serviceTimeGenerator;
        this.campEventList = campEventList;
        this.scheduledEventType = scheduledEventType;
        this.servicePointName = servicePointName;
    }

    // Worker controls
    public void setWorkers(int n) { this.workers = Math.max(1, n); }
    public int getWorkers() { return workers; }

    public void addSurvivorToQueue(Survivor survivor) {
        survivorQueue.add(survivor);
        updateMaximumQueueLength();
    }

    public Survivor removeSurvivorFromQueue() {
        serviceInProgress = false;
        Survivor survivor = survivorQueue.poll();
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
        if (serviceInProgress || survivorQueue.isEmpty()) return;
        Survivor currentSurvivor = survivorQueue.peek();
        serviceInProgress = true;

        double baseServiceDuration = serviceTimeGenerator.sample();
        double actualServiceDuration = calculateActualServiceTime(currentSurvivor, baseServiceDuration);

        actualServiceDuration = actualServiceDuration / Math.max(1, workers);
        actualServiceDuration = Math.max(0.0001, actualServiceDuration);

        recordServiceStartTime(currentSurvivor);
        Event serviceCompletionEvent =
                new Event(scheduledEventType, Clock.getInstance().getClock() + actualServiceDuration);
        campEventList.add(serviceCompletionEvent);
    }

    private double calculateActualServiceTime(Survivor survivor, double baseDuration) {
        double serviceDuration = baseDuration;
        switch (scheduledEventType) {
            case CHILD_SHELTER_ASSIGNMENT_COMPLETE:
            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
                serviceDuration = 5;
                break;
            default:
                break;
        }
        return serviceDuration;
    }

    private void recordServiceStartTime(Survivor survivor) {
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
                survivor.assignTemporaryHome();   // assign 50/50
                appendAssignmentCsvRow(survivor);
                break;
            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
                survivor.setAdultShelterAssignmentStartTime(currentTime);
                survivor.assignTemporaryHome();   // assign 40/30/30
                appendAssignmentCsvRow(survivor);
                break;
            default:
                break;
        }
    }

    private void appendAssignmentCsvRow(Survivor s) {
        try {
            boolean exists = Files.exists(ASSIGNMENT_CSV);
            try (BufferedWriter out = Files.newBufferedWriter(
                    ASSIGNMENT_CSV, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

                if (!exists) {
                    out.write(Survivor.csvHeader());
                    out.newLine();
                }
                out.write(s.toCsvRow());
                out.newLine();
            }
        } catch (IOException e) {
            System.err.println("CSV write failed: " + e.getMessage());
        }
    }

    private void updateMaximumQueueLength() {
        if (survivorQueue.size() > maximumQueueLength) {
            maximumQueueLength = survivorQueue.size();
        }
    }

    // === FIXED methods ===
    public boolean isServiceInProgress() { return serviceInProgress; }
    public boolean hasSurvivorsInQueue() { return !survivorQueue.isEmpty(); }
    public int getCurrentQueueLength() { return survivorQueue.size(); }  // ✅ added

    // Getters
    public String getServicePointName() { return servicePointName; }
    public int getTotalSurvivorsServed() { return totalSurvivorsServed; }
    public double getAverageServiceTime() { return totalSurvivorsServed > 0 ? cumulativeServiceTime / totalSurvivorsServed : 0; }
    public int getMaximumQueueLength() { return maximumQueueLength; }
    public double getCumulativeServiceTime() { return cumulativeServiceTime; }
    public double getCumulativeWaitingTime() { return cumulativeWaitingTime; }
    public int getTotalServed() { return totalSurvivorsServed; }
    public double getMaxWaitingTime() { return maxWaitingTimeObserved; }
    public double getLocalMaxWaitingTime() { return localMaxWaitingTime; }
}
