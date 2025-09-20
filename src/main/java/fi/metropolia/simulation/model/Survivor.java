package fi.metropolia.simulation.model;

import fi.metropolia.simulation.framework.Clock;

/**
 * MODEL: Survivor data model for the rescue camp simulation
 * Contains only data and business logic, no UI or control logic
 */
public class Survivor {
    public enum HealthCondition {
        HEALTHY, INJURED
    }

    public enum AgeCategory {
        CHILD, ADULT
    }

    // Identity and basic attributes
    private static int nextSurvivorId = 1;
    private int survivorId;
    private int survivorAge;
    private HealthCondition healthCondition;
    private AgeCategory ageCategory;
    private boolean hasFamily;
    private int familyMemberCount;
    private String familyGroupId;

    // Timing data
    private double campArrivalTime;
    private double processingCompletionTime;
    private double totalWaitingTime = 0;

    // Service requirements
    private boolean requiresMedicalTreatment;
    private boolean requestsCommunicationService;
    private boolean isFullyProcessed = false;

    // Service tracking times
    private double medicalTreatmentStartTime;
    private double registrationStartTime;
    private double communicationServiceStartTime;
    private double suppliesDistributionStartTime;
    private double shelterAssignmentStartTime;

    /**
     * Create a new survivor with randomly generated attributes
     */
    public Survivor() {
        this.survivorId = nextSurvivorId++;
        this.campArrivalTime = Clock.getInstance().getClock();
        generateSurvivorAttributes();
    }

    /**
     * Business Logic: Generate random attributes for the survivor
     */
    private void generateSurvivorAttributes() {
        // Generate age (1-80 years)
        this.survivorAge = (int) (Math.random() * 80) + 1;
        this.ageCategory = (survivorAge < 18) ? AgeCategory.CHILD : AgeCategory.ADULT;

        // Generate health condition (20% chance of being injured)
        this.healthCondition = (Math.random() < 0.2) ? HealthCondition.INJURED : HealthCondition.HEALTHY;
        this.requiresMedicalTreatment = (healthCondition == HealthCondition.INJURED);

        // Generate family status (70% chance of having family)
        this.hasFamily = (Math.random() < 0.7);
        if (hasFamily) {
            this.familyMemberCount = (int) (Math.random() * 5) + 2; // 2-6 family members
            this.familyGroupId = "FAMILY_" + survivorId; // Family group ID based on first member
        } else {
            this.familyMemberCount = 1;
            this.familyGroupId = null;
        }

        // Generate communication service need (40% chance)
        this.requestsCommunicationService = (Math.random() < 0.4);
    }

    /**
     * Business Logic: Mark when survivor completes the entire rescue camp process
     */
    public void setProcessingCompletionTime(double completionTime) {
        this.processingCompletionTime = completionTime;
        this.isFullyProcessed = true;
    }

    /**
     * Business Logic: Calculate total time spent in the rescue camp system
     */
    public double getTotalTimeInCamp() {
        if (isFullyProcessed) {
            return processingCompletionTime - campArrivalTime;
        }
        return Clock.getInstance().getClock() - campArrivalTime;
    }

    /**
     * Business Logic: Add waiting time to the total
     */
    public void addWaitingTime(double waitTime) {

        this.totalWaitingTime += waitTime;
    }

    // Data Access Methods (Getters)
    public int getSurvivorId() {

        return survivorId;
    }

    public int getSurvivorAge() {

        return survivorAge;
    }

    public HealthCondition getHealthCondition() {

        return healthCondition;
    }

    public AgeCategory getAgeCategory() {
        return ageCategory;
    }

    public boolean hasFamily() {

        return hasFamily;
    }

    public int getFamilyMemberCount() {

        return familyMemberCount;
    }

    public String getFamilyGroupId() {

        return familyGroupId;
    }

    public double getCampArrivalTime() {
        return campArrivalTime;
    }

    public double getProcessingCompletionTime() {
        return processingCompletionTime;
    }

    public double getTotalWaitingTime() {
        return totalWaitingTime;
    }

    public boolean requiresMedicalTreatment() {

        return requiresMedicalTreatment;
    }

    public boolean requestsCommunicationService() {
        return requestsCommunicationService;
    }

    public boolean isFullyProcessed() {
        return isFullyProcessed;
    }

    // Service time setters for tracking
    public void setMedicalTreatmentStartTime(double time) {
        this.medicalTreatmentStartTime = time;
    }

    public void setRegistrationStartTime(double time) {
        this.registrationStartTime = time;
    }

    public void setCommunicationServiceStartTime(double time) {
        this.communicationServiceStartTime = time;
    }

    public void setSuppliesDistributionStartTime(double time) {
        this.suppliesDistributionStartTime = time;
    }

    public void setShelterAssignmentStartTime(double time) {
        this.shelterAssignmentStartTime = time;
    }
}