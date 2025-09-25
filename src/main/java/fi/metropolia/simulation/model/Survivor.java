package fi.metropolia.simulation.model;

import fi.metropolia.simulation.framework.Clock;
import java.util.Locale;

public class Survivor {
    public enum HealthCondition { HEALTHY, INJURED }
    public enum AgeCategory   { CHILD, ADULT }

    // === NEW: temporary homes per spec (SC-6 / SC-7) ===
    public enum TempHomeChild {
        FAITHWORKS_CHILDCARE,
        EASTER_BROOK_FOUNDATION
    }
    public enum TempHomeAdult {
        CITY_OF_REFUGE_ORPHANAGE_HOME,
        LIFEPATH_CARE_HOME,
        EVERGREEN_CARE_CENTER
    }

    private static int nextSurvivorId = 1;
    private final int survivorId;
    private int survivorAge;
    private HealthCondition healthCondition;
    private AgeCategory ageCategory;

    // Timing
    private final double campArrivalTime;
    private double processingCompletionTime;
    private double totalWaitingTime = 0;

    // Service requirements (derived)
    private boolean requiresMedicalTreatment;
    private boolean requestsCommunicationService; // <-- children must be FALSE
    private boolean isFullyProcessed = false;

    // === NEW: assignment result + time ===
    private TempHomeChild assignedChildHome = null;
    private TempHomeAdult assignedAdultHome = null;
    private double assignmentTime = Double.NaN;

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
        this.requiresMedicalTreatment =
                (ageCategory == AgeCategory.CHILD) || (healthCondition == HealthCondition.INJURED);

        // Communication need: children must NOT request; adults 40%
        this.requestsCommunicationService =
                (ageCategory == AgeCategory.ADULT) && (Math.random() < 0.4);
    }

    // === NEW: perform assignment at SC-6/SC-7 ===
    /** Idempotent: safe to call once when SC-6/SC-7 starts. */
    public void assignTemporaryHome() {
        if (hasAssignment()) return;
        double r = Math.random();
        if (ageCategory == AgeCategory.CHILD) {
            // 50% Faithworks, 50% Easter Brook Foundation (SC-6)
            assignedChildHome = (r < 0.5)
                    ? TempHomeChild.FAITHWORKS_CHILDCARE
                    : TempHomeChild.EASTER_BROOK_FOUNDATION;
        } else {
            // 40% City of Refuge, 30% Lifepath, 30% Evergreen (SC-7)
            if (r < 0.40) {
                assignedAdultHome = TempHomeAdult.CITY_OF_REFUGE_ORPHANAGE_HOME;
            } else if (r < 0.70) {
                assignedAdultHome = TempHomeAdult.LIFEPATH_CARE_HOME;
            } else {
                assignedAdultHome = TempHomeAdult.EVERGREEN_CARE_CENTER;
            }
        }
        assignmentTime = Clock.getInstance().getClock();
    }

    public boolean hasAssignment() {
        return assignedChildHome != null || assignedAdultHome != null;
    }

    public String getAssignedHomeName() {
        if (assignedChildHome != null) {
            switch (assignedChildHome) {
                case FAITHWORKS_CHILDCARE:       return "Faithworks childcare";
                case EASTER_BROOK_FOUNDATION:    return "Easter Brook Foundation";
            }
        }
        if (assignedAdultHome != null) {
            switch (assignedAdultHome) {
                case CITY_OF_REFUGE_ORPHANAGE_HOME: return "City of Refuge Orphanage Home";
                case LIFEPATH_CARE_HOME:            return "Lifepath Care Home";
                case EVERGREEN_CARE_CENTER:         return "Evergreen Care center";
            }
        }
        return null;
    }

    // === NEW: CSV helpers (header + row for assignments) ===
    public static String csvHeader() {
        return String.join(",",
                "survivor_id",
                "age_category",
                "age",
                "health",
                "requires_medical",
                "requests_communication",
                "stage",                // SC-6 or SC-7
                "assigned_home",
                "assignment_time",
                "camp_arrival_time"
        );
    }

    /** Produce one CSV row after assignment has been made. */
    public String toCsvRow() {
        String stage = (ageCategory == AgeCategory.CHILD) ? "SC-6" : "SC-7";
        String home = getAssignedHomeName();
        return String.join(",",
                Integer.toString(survivorId),
                ageCategory.name(),
                Integer.toString(survivorAge),
                healthCondition.name(),
                Boolean.toString(requiresMedicalTreatment),
                Boolean.toString(requestsCommunicationService),
                stage,
                escapeCsv(home == null ? "" : home),
                formatTime(assignmentTime),
                formatTime(campArrivalTime)
        );
    }

    private String escapeCsv(String s) {
        if (s == null) return "";
        boolean needsQuotes = s.contains(",") || s.contains("\"") || s.contains("\n");
        String escaped = s.replace("\"", "\"\"");
        return needsQuotes ? "\"" + escaped + "\"" : escaped;
    }
    private String formatTime(double t) {
        if (Double.isNaN(t)) return "";
        return String.format(Locale.US, "%.4f", t);
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
    public void setMedicalTreatmentStartTime(double t) { // Service start timestamps
        // SC-4
    }
    public void setRegistrationStartTime(double t) { // SC-1
    }
    public void setCommunicationServiceStartTime(double t) { // SC-2
    }
    public void setSuppliesDistributionStartTime(double t) { // SC-3
    }
    public void setAccommodationCenterStartTime(double t) { // SC-5
    }
    public void setChildShelterAssignmentStartTime(double t) { // SC-6
    }
    public void setAdultShelterAssignmentStartTime(double t) { // SC-7
    }
}
