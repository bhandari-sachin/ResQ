package fi.metropolia.simulation.model;

import eduni.distributions.ContinuousGenerator;
import fi.metropolia.simulation.framework.*;

import java.util.LinkedList;

public class RescueCampServicePoint {
    private LinkedList<Survivor> survivorQueue = new LinkedList<>();
    private ContinuousGenerator serviceTimeGenerator;
    private EventList campEventList;
    private RescueCampEventType scheduledEventType;
    private boolean serviceInProgress = false;
    private String servicePointName;

    // Performance statistics data
    private int totalSurvivorsServed = 0;
    private double cumulativeServiceTime = 0;
    private double cumulativeWaitingTime = 0;
    private int maximumQueueLength = 0;
    private double maxWaitingTimeObserved = 0;

    // Local variable to track max waiting time during service
    private double localMaxWaitingTime = 0;

    public RescueCampServicePoint(ContinuousGenerator serviceTimeGenerator, EventList campEventList, RescueCampEventType scheduledEventType, String servicePointName) {
        this.serviceTimeGenerator = serviceTimeGenerator;
        this.campEventList = campEventList;
        this.scheduledEventType = scheduledEventType;
        this.servicePointName = servicePointName;
    }

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
        if (survivorQueue.isEmpty()) return;
        Survivor currentSurvivor = survivorQueue.peek();
        serviceInProgress = true;
        double baseServiceDuration = serviceTimeGenerator.sample();
        double actualServiceDuration = calculateActualServiceTime(currentSurvivor, baseServiceDuration);
        recordServiceStartTime(currentSurvivor);
        Event serviceCompletionEvent = new Event(scheduledEventType, Clock.getInstance().getClock() + actualServiceDuration);
        campEventList.add(serviceCompletionEvent);
    }

    private double calculateActualServiceTime(Survivor survivor, double baseDuration) {
        double serviceDuration = baseDuration;
        switch (scheduledEventType) {
            case SUPPLIES_DISTRIBUTION_COMPLETE:
                if (survivor.hasFamily()) serviceDuration += survivor.getFamilyMemberCount() - 1;
                break;
            case FAMILY_SHELTER_ASSIGNMENT_COMPLETE:
                serviceDuration = survivor.hasFamily() ? 8 + survivor.getFamilyMemberCount() : 8;
                break;
            case CHILD_SHELTER_ASSIGNMENT_COMPLETE:
            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
                serviceDuration = 5;
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
            case CHILD_SHELTER_ASSIGNMENT_COMPLETE:
            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
            case FAMILY_SHELTER_ASSIGNMENT_COMPLETE:
                survivor.setShelterAssignmentStartTime(currentTime);
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
