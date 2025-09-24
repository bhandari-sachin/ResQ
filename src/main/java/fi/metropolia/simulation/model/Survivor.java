package fi.metropolia.simulation.model;

import fi.metropolia.simulation.framework.Clock;

public class Survivor {
    public enum HealthCondition { HEALTHY, INJURED }
    public enum AgeCategory   { CHILD, ADULT }

    private static int nextSurvivorId = 1;
    private int survivorId;
    private int survivorAge;
    private HealthCondition healthCondition;
    private AgeCategory ageCategory;

    // Timing
    private double campArrivalTime;
    private double processingCompletionTime;
    private double totalWaitingTime = 0;

    // Service requirements (derived)
    private boolean requiresMedicalTreatment;
    private boolean requestsCommunicationService; // <-- children must be FALSE
    private boolean isFullyProcessed = false;

    // Service start timestamps
    private double medicalTreatmentStartTime;          // SC-4
    private double registrationStartTime;              // SC-1
    private double communicationServiceStartTime;      // SC-2
    private double suppliesDistributionStartTime;      // SC-3
    private double accommodationCenterStartTime;       // SC-5
    private double childShelterAssignmentStartTime;    // SC-6
    private double adultShelterAssignmentStartTime;    // SC-7

    public Survivor() {
        this.survivorId = nextSurvivorId++;
        this.campArrivalTime = Clock.getInstance().getClock();
        generateSurvivorAttributes();
    }

    private void generateSurvivorAttributes() {
        // Age 1–80
        this.survivorAge = (int) (Math.random() * 80) + 1;
        this.ageCategory = (survivorAge < 18) ? AgeCategory.CHILD : AgeCategory.ADULT;

        // Health: 20% injured (unchanged)
        this.healthCondition = (Math.random() < 0.2) ? HealthCondition.INJURED : HealthCondition.HEALTHY;

        // Medical need (derived)
        // - Children: require medical treatment
        // - Adults: require medical only if injured
        this.requiresMedicalTreatment =
                (ageCategory == AgeCategory.CHILD) || (healthCondition == HealthCondition.INJURED);

        // Communication need (FIX): children must NOT request it
        // Adults may request with 40% probability
        this.requestsCommunicationService =
                (ageCategory == AgeCategory.ADULT) && (Math.random() < 0.4);
    }

    public void setProcessingCompletionTime(double completionTime) {
        this.processingCompletionTime = completionTime;
        this.isFullyProcessed = true;
    }

    public double getTotalTimeInCamp() {
        if (isFullyProcessed) return processingCompletionTime - campArrivalTime;
        return Clock.getInstance().getClock() - campArrivalTime;
    }

    public void addWaitingTime(double waitTime) { this.totalWaitingTime += waitTime; }

    // Getters
    public int getSurvivorId() { return survivorId; }
    public int getSurvivorAge() { return survivorAge; }
    public HealthCondition getHealthCondition() { return healthCondition; }
    public AgeCategory getAgeCategory() { return ageCategory; }
    public double getCampArrivalTime() { return campArrivalTime; }
    public double getProcessingCompletionTime() { return processingCompletionTime; }
    public double getTotalWaitingTime() { return totalWaitingTime; }
    public boolean requiresMedicalTreatment() { return requiresMedicalTreatment; }
    public boolean requestsCommunicationService() { return requestsCommunicationService; }
    public boolean isFullyProcessed() { return isFullyProcessed; }

    // Service time setters
    public void setMedicalTreatmentStartTime(double t) { this.medicalTreatmentStartTime = t; }
    public void setRegistrationStartTime(double t) { this.registrationStartTime = t; }
    public void setCommunicationServiceStartTime(double t) { this.communicationServiceStartTime = t; }
    public void setSuppliesDistributionStartTime(double t) { this.suppliesDistributionStartTime = t; }
    public void setAccommodationCenterStartTime(double t) { this.accommodationCenterStartTime = t; }
    public void setChildShelterAssignmentStartTime(double t) { this.childShelterAssignmentStartTime = t; }
    public void setAdultShelterAssignmentStartTime(double t) { this.adultShelterAssignmentStartTime = t; }
}
