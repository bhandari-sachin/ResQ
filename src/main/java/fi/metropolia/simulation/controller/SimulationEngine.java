package fi.metropolia.simulation.controller;

import eduni.distributions.*;
import fi.metropolia.simulation.framework.*;
import fi.metropolia.simulation.model.*;
import fi.metropolia.simulation.view.console.RescueCampSimulationView;

import java.util.*;

/**
 * CONTROLLER: Main simulation controller for rescue camp operations
 * Handles business logic, coordinates between Model and View, manages simulation flow
 */
public class SimulationEngine extends Engine {

    // View reference
    private RescueCampSimulationView view;

    // Survivor arrival process
    private ArrivalProcess survivorArrivalProcess;

    // Rescue camp service points (Models)
    private RescueCampServicePoint medicalTreatmentStation;
    private RescueCampServicePoint registrationDesk;
    private RescueCampServicePoint communicationCenter;
    private RescueCampServicePoint suppliesDistributionPoint;
    private RescueCampServicePoint childShelterAssignment;
    private RescueCampServicePoint adultShelterAssignment;
    private RescueCampServicePoint familyShelterAssignment;

    // Family tracking system
    private Map<String, List<Survivor>> familyGroupRegistry = new HashMap<>();

    // Camp operation statistics
    private int totalSurvivorArrivals = 0;
    private int totalSurvivorsProcessed = 0;

    // Keep ALL survivors as they arrive (for CSV)
    private final List<Survivor> allSurvivors = new ArrayList<>();

    // Keep fully processed survivors (you already had this)
    private final List<Survivor> fullyProcessedSurvivors = new ArrayList<>();

    public SimulationEngine() {
        this.view = new RescueCampSimulationView();
        initializeCampServicePoints();
        initializeSurvivorArrivalProcess();
    }

    /**
     * Initialize all service points with proper distributions
     */
    private void initializeCampServicePoints() {
        medicalTreatmentStation = new RescueCampServicePoint(
                new Uniform(10, 15), eventList, RescueCampEventType.MEDICAL_TREATMENT_COMPLETE, "Medical Treatment Station");
        registrationDesk = new RescueCampServicePoint(
                new Uniform(3, 5), eventList, RescueCampEventType.REGISTRATION_COMPLETE, "Registration Desk");
        communicationCenter = new RescueCampServicePoint(
                new Uniform(3, 6), eventList, RescueCampEventType.COMMUNICATION_SERVICE_COMPLETE, "Communication Center");
        suppliesDistributionPoint = new RescueCampServicePoint(
                new Uniform(4, 7), eventList, RescueCampEventType.SUPPLIES_DISTRIBUTION_COMPLETE, "Supplies Distribution Point");
        childShelterAssignment = new RescueCampServicePoint(
                new Normal(5, 1), eventList, RescueCampEventType.CHILD_SHELTER_ASSIGNMENT_COMPLETE, "Child Shelter Assignment");
        adultShelterAssignment = new RescueCampServicePoint(
                new Normal(5, 1), eventList, RescueCampEventType.ADULT_SHELTER_ASSIGNMENT_COMPLETE, "Adult Shelter Assignment");
        familyShelterAssignment = new RescueCampServicePoint(
                new Normal(8, 2), eventList, RescueCampEventType.FAMILY_SHELTER_ASSIGNMENT_COMPLETE, "Family Shelter Assignment");

        // === Initial staffing ===
        medicalTreatmentStation.setWorkers(5);
        registrationDesk.setWorkers(2);
        communicationCenter.setWorkers(2);
        suppliesDistributionPoint.setWorkers(2);
        childShelterAssignment.setWorkers(2);
        adultShelterAssignment.setWorkers(2);
        familyShelterAssignment.setWorkers(2);
    }

    /**
     * Initialize survivor arrival process with valid seed
     */
    private void initializeSurvivorArrivalProcess() {
        int seed = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        ContinuousGenerator survivorArrivalTimeGenerator = new Negexp(20, seed);
        survivorArrivalProcess = new ArrivalProcess(survivorArrivalTimeGenerator, eventList, RescueCampEventType.SURVIVOR_ARRIVAL);
    }

    @Override
    protected void initialize() {
        Clock.getInstance().reset(); // Ensure clock starts at 0
        view.displaySimulationStart();
        survivorArrivalProcess.generateNextEvent(); // Schedule first survivor
    }

    @Override
    protected void runEvent(Event campEvent) {
        Survivor survivor;

        switch ((RescueCampEventType) campEvent.getType()) {
            case SURVIVOR_ARRIVAL:
                handleNewSurvivorArrival();
                break;

            case MEDICAL_TREATMENT_COMPLETE:
                survivor = medicalTreatmentStation.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Medical Treatment Complete");
                    registrationDesk.addSurvivorToQueue(survivor);
                }
                break;

            case REGISTRATION_COMPLETE:
                survivor = registrationDesk.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Registration Complete");
                    routeSurvivorAfterRegistration(survivor);
                }
                break;

            case COMMUNICATION_SERVICE_COMPLETE:
                survivor = communicationCenter.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Communication Service Complete");
                    suppliesDistributionPoint.addSurvivorToQueue(survivor);
                }
                break;

            case SUPPLIES_DISTRIBUTION_COMPLETE:
                survivor = suppliesDistributionPoint.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Supplies Distribution Complete");
                    assignSurvivorToShelter(survivor);
                }
                break;

            case CHILD_SHELTER_ASSIGNMENT_COMPLETE:
                survivor = childShelterAssignment.removeSurvivorFromQueue();
                if (survivor != null) completeSurvivorProcessing(survivor);
                break;

            case ADULT_SHELTER_ASSIGNMENT_COMPLETE:
                survivor = adultShelterAssignment.removeSurvivorFromQueue();
                if (survivor != null) completeSurvivorProcessing(survivor);
                break;

            case FAMILY_SHELTER_ASSIGNMENT_COMPLETE:
                survivor = familyShelterAssignment.removeSurvivorFromQueue();
                if (survivor != null) handleFamilyShelterAssignmentCompletion(survivor);
                break;
        }
    }

    /**
     * Handle new survivor arrival
     */
    private void handleNewSurvivorArrival() {
        Survivor newSurvivor = new Survivor();
        totalSurvivorArrivals++;
        allSurvivors.add(newSurvivor); // <-- record all generated survivors for CSV
        view.displaySurvivorArrival(newSurvivor);

        // Register family groups
        if (newSurvivor.hasFamily()) {
            String familyId = newSurvivor.getFamilyGroupId();
            familyGroupRegistry.computeIfAbsent(familyId, k -> new ArrayList<>()).add(newSurvivor);
        }

        // Route based on health condition
        if (newSurvivor.requiresMedicalTreatment()) {
            medicalTreatmentStation.addSurvivorToQueue(newSurvivor);
            view.displayServiceAssignment(newSurvivor, "Medical Treatment Station");
        } else {
            registrationDesk.addSurvivorToQueue(newSurvivor);
            view.displayServiceAssignment(newSurvivor, "Registration Desk");
        }

        survivorArrivalProcess.generateNextEvent(); // Schedule next arrival
    }

    /**
     * Route survivor after registration
     */
    private void routeSurvivorAfterRegistration(Survivor survivor) {
        if (survivor.requestsCommunicationService()) {
            communicationCenter.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Communication Center");
        } else {
            suppliesDistributionPoint.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Supplies Distribution Point");
        }
    }

    /**
     * Assign survivor to shelter
     */
    private void assignSurvivorToShelter(Survivor survivor) {
        if (survivor.hasFamily()) {
            familyShelterAssignment.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Family Shelter Assignment");
        } else if (survivor.getAgeCategory() == Survivor.AgeCategory.CHILD) {
            childShelterAssignment.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Child Shelter Assignment");
        } else {
            adultShelterAssignment.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Adult Shelter Assignment");
        }
    }

    /**
     * Handle family shelter completion
     */
    private void handleFamilyShelterAssignmentCompletion(Survivor survivor) {
        if (survivor.hasFamily()) {
            String familyId = survivor.getFamilyGroupId();
            List<Survivor> familyMembers = familyGroupRegistry.get(familyId);
            if (familyMembers != null) {
                for (Survivor member : familyMembers) completeSurvivorProcessing(member);
                familyGroupRegistry.remove(familyId);
            }
        } else {
            completeSurvivorProcessing(survivor);
        }
    }

    /**
     * Complete survivor processing
     */
    private void completeSurvivorProcessing(Survivor survivor) {
        survivor.setProcessingCompletionTime(Clock.getInstance().getClock());
        totalSurvivorsProcessed++;
        fullyProcessedSurvivors.add(survivor);
        view.displaySurvivorCompletion(survivor);
    }

    @Override
    protected void tryCEvents() {
        RescueCampServicePoint[] allServicePoints = {
                medicalTreatmentStation, registrationDesk, communicationCenter, suppliesDistributionPoint,
                childShelterAssignment, adultShelterAssignment, familyShelterAssignment
        };

        for (RescueCampServicePoint sp : allServicePoints) {
            if (!sp.isServiceInProgress() && sp.hasSurvivorsInQueue()) {
                sp.beginServiceForSurvivor();
                view.displayServiceStart(sp);
            }
        }
    }

    @Override
    protected void results() {
        List<RescueCampServicePoint> allServicePoints = Arrays.asList(
                medicalTreatmentStation, registrationDesk, communicationCenter, suppliesDistributionPoint,
                childShelterAssignment, adultShelterAssignment, familyShelterAssignment
        );

        view.displayFinalResults(
                Clock.getInstance().getClock(),
                totalSurvivorArrivals,
                totalSurvivorsProcessed,
                fullyProcessedSurvivors,
                allServicePoints
        );
    }

    // ---- External control methods ----
    public void setSimulationDuration(double minutes) { setSimulationTime(minutes); }
    public void startSimulation() { run(); }

    // ---- NEW: accessors for CSV export ----
    public List<Survivor> getAllSurvivors() {
        return Collections.unmodifiableList(allSurvivors);
    }

    public List<Survivor> getFullyProcessedSurvivors() {
        return Collections.unmodifiableList(fullyProcessedSurvivors);
    }

    // ---- worker controls ----
    public void setMedicalWorkers(int n)       { medicalTreatmentStation.setWorkers(n); }
    public void setRegistrationWorkers(int n)  { registrationDesk.setWorkers(n); }
    public void setCommunicationWorkers(int n) { communicationCenter.setWorkers(n); }
    public void setSuppliesWorkers(int n)      { suppliesDistributionPoint.setWorkers(n); }
    public void setChildShelterWorkers(int n)  { childShelterAssignment.setWorkers(n); }
    public void setAdultShelterWorkers(int n)  { adultShelterAssignment.setWorkers(n); }
    public void setFamilyShelterWorkers(int n) { familyShelterAssignment.setWorkers(n); }
}
