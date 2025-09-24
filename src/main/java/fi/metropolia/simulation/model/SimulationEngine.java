package fi.metropolia.simulation.model;

import eduni.distributions.*;
import fi.metropolia.simulation.framework.*;
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
    private RescueCampServicePoint medicalTreatmentStation;   // SC-4
    private RescueCampServicePoint registrationDesk;          // SC-1
    private RescueCampServicePoint communicationCenter;       // SC-2
    private RescueCampServicePoint suppliesDistributionPoint; // SC-3
    private RescueCampServicePoint accommodationCenter;       // SC-5
    private RescueCampServicePoint childShelterAssignment;    // SC-6
    private RescueCampServicePoint adultShelterAssignment;    // SC-7

    // ---- Distribution parameters (defaults preserved) ----
    private double arrivalMean = 20.0;   // Negexp mean
    private double regMin = 3.0,   regMax = 5.0;   // Uniform for Registration
    private double comMin = 3.0,   comMax = 6.0;   // Uniform for Communication
    private double supMin = 4.0,   supMax = 7.0;   // Uniform for Supplies
    private double medMin = 10.0,  medMax = 15.0;  // Uniform for Medical
    private double accMean = 6.0,  accSd  = 1.0;   // Normal for Accommodation
    private double childMean = 5.0, childSd = 1.0; // Normal for Child Shelter
    private double adultMean = 5.0, adultSd = 1.0; // Normal for Adult Shelter

    // Camp operation statistics
    private int totalSurvivorArrivals = 0;
    private int totalSurvivorsProcessed = 0;

    // Keep ALL survivors as they arrive (for CSV)
    private final List<Survivor> allSurvivors = new ArrayList<>();

    // Keep fully processed survivors
    private final List<Survivor> fullyProcessedSurvivors = new ArrayList<>();

    // ---- Constructors ----

    /** Default: uses the same parameters you had before */
    public SimulationEngine() {
        this.view = new RescueCampSimulationView();
        initializeCampServicePoints();
        initializeSurvivorArrivalProcess();
    }

    /** Parameterized: same distributions, caller may override parameters */
    public SimulationEngine(
            double arrivalMean,
            double regMin,   double regMax,
            double comMin,   double comMax,
            double supMin,   double supMax,
            double medMin,   double medMax,
            double accMean,  double accSd,
            double childMean, double childSd,
            double adultMean, double adultSd
    ) {
        this.view = new RescueCampSimulationView();

        // Assign overrides (no validation beyond basic assignment for minimal change)
        this.arrivalMean = arrivalMean;
        this.regMin = regMin;     this.regMax = regMax;
        this.comMin = comMin;     this.comMax = comMax;
        this.supMin = supMin;     this.supMax = supMax;
        this.medMin = medMin;     this.medMax = medMax;
        this.accMean = accMean;   this.accSd  = accSd;
        this.childMean = childMean; this.childSd = childSd;
        this.adultMean = adultMean; this.adultSd = adultSd;

        initializeCampServicePoints();
        initializeSurvivorArrivalProcess();
    }

    /**
     * Initialize all service points with proper distributions
     */
    private void initializeCampServicePoints() {
        medicalTreatmentStation = new RescueCampServicePoint(
                new Uniform(medMin, medMax), eventList,
                RescueCampEventType.MEDICAL_TREATMENT_COMPLETE, "Medical Treatment Station"); // SC-4

        registrationDesk = new RescueCampServicePoint(
                new Uniform(regMin, regMax), eventList,
                RescueCampEventType.REGISTRATION_COMPLETE, "Registration Desk"); // SC-1

        communicationCenter = new RescueCampServicePoint(
                new Uniform(comMin, comMax), eventList,
                RescueCampEventType.COMMUNICATION_SERVICE_COMPLETE, "Communication Center"); // SC-2

        suppliesDistributionPoint = new RescueCampServicePoint(
                new Uniform(supMin, supMax), eventList,
                RescueCampEventType.SUPPLIES_DISTRIBUTION_COMPLETE, "Supplies Distribution Point"); // SC-3

        accommodationCenter = new RescueCampServicePoint(
                new Normal(accMean, accSd), eventList,
                RescueCampEventType.ACCOMMODATION_CENTER_COMPLETE, "Accommodation Center"); // SC-5

        childShelterAssignment = new RescueCampServicePoint(
                new Normal(childMean, childSd), eventList,
                RescueCampEventType.CHILD_SHELTER_ASSIGNMENT_COMPLETE, "Child Shelter Assignment"); // SC-6

        adultShelterAssignment = new RescueCampServicePoint(
                new Normal(adultMean, adultSd), eventList,
                RescueCampEventType.ADULT_SHELTER_ASSIGNMENT_COMPLETE, "Adult Shelter Assignment"); // SC-7

        // === Initial staffing ===
        medicalTreatmentStation.setWorkers(5);
        registrationDesk.setWorkers(2);
        communicationCenter.setWorkers(2);
        suppliesDistributionPoint.setWorkers(2);
        accommodationCenter.setWorkers(2);
        childShelterAssignment.setWorkers(2);
        adultShelterAssignment.setWorkers(2);
    }

    /**
     * Initialize survivor arrival process with valid seed
     */
    private void initializeSurvivorArrivalProcess() {
        int seed = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        ContinuousGenerator survivorArrivalTimeGenerator = new Negexp(arrivalMean, seed);
        survivorArrivalProcess = new ArrivalProcess(
                survivorArrivalTimeGenerator, eventList, RescueCampEventType.SURVIVOR_ARRIVAL);
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

            case MEDICAL_TREATMENT_COMPLETE: // SC-4 -> SC-1
                survivor = medicalTreatmentStation.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Medical Treatment Complete");
                    registrationDesk.addSurvivorToQueue(survivor);
                }
                break;

            case REGISTRATION_COMPLETE: // SC-1 -> SC-2 (adults needing comms) or SC-3
                survivor = registrationDesk.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Registration Complete");
                    routeSurvivorAfterRegistration(survivor);
                }
                break;

            case COMMUNICATION_SERVICE_COMPLETE: // SC-2 -> SC-3
                survivor = communicationCenter.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Communication Service Complete");
                    suppliesDistributionPoint.addSurvivorToQueue(survivor);
                }
                break;

            case SUPPLIES_DISTRIBUTION_COMPLETE: // SC-3 -> SC-5
                survivor = suppliesDistributionPoint.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Supplies Distribution Complete");
                    accommodationCenter.addSurvivorToQueue(survivor);
                    view.displayServiceAssignment(survivor, "Accommodation Center");
                }
                break;

            case ACCOMMODATION_CENTER_COMPLETE: // SC-5 -> SC-6/SC-7 (by age)
                survivor = accommodationCenter.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Accommodation Center Complete");
                    if (survivor.getAgeCategory() == Survivor.AgeCategory.CHILD) {
                        childShelterAssignment.addSurvivorToQueue(survivor);
                        view.displayServiceAssignment(survivor, "Child Shelter Assignment");
                    } else {
                        adultShelterAssignment.addSurvivorToQueue(survivor);
                        view.displayServiceAssignment(survivor, "Adult Shelter Assignment");
                    }
                }
                break;

            case CHILD_SHELTER_ASSIGNMENT_COMPLETE: // SC-6 -> done
                survivor = childShelterAssignment.removeSurvivorFromQueue();
                if (survivor != null) completeSurvivorProcessing(survivor);
                break;

            case ADULT_SHELTER_ASSIGNMENT_COMPLETE: // SC-7 -> done
                survivor = adultShelterAssignment.removeSurvivorFromQueue();
                if (survivor != null) completeSurvivorProcessing(survivor);
                break;
        }
    }

    /**
     * Handle new survivor arrival
     */
    private void handleNewSurvivorArrival() {
        Survivor newSurvivor = new Survivor();
        totalSurvivorArrivals++;
        allSurvivors.add(newSurvivor); // record all generated survivors for CSV
        view.displaySurvivorArrival(newSurvivor);

        // Route based on requirement-derived medical need (children always true; adults if injured)
        if (newSurvivor.requiresMedicalTreatment()) {
            medicalTreatmentStation.addSurvivorToQueue(newSurvivor); // SC-4
            view.displayServiceAssignment(newSurvivor, "Medical Treatment Station");
        } else {
            registrationDesk.addSurvivorToQueue(newSurvivor); // SC-1
            view.displayServiceAssignment(newSurvivor, "Registration Desk");
        }

        survivorArrivalProcess.generateNextEvent(); // Schedule next arrival
    }

    /**
     * Route survivor after registration
     */
    private void routeSurvivorAfterRegistration(Survivor survivor) {
        // Only adults may visit the Communication Center (SC-2) if they request it; children go to Supplies (SC-3)
        if (survivor.getAgeCategory() == Survivor.AgeCategory.ADULT && survivor.requestsCommunicationService()) {
            communicationCenter.addSurvivorToQueue(survivor); // SC-2
            view.displayServiceAssignment(survivor, "Communication Center");
        } else {
            suppliesDistributionPoint.addSurvivorToQueue(survivor); // SC-3
            view.displayServiceAssignment(survivor, "Supplies Distribution Point");
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
                accommodationCenter, childShelterAssignment, adultShelterAssignment
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
                accommodationCenter, childShelterAssignment, adultShelterAssignment
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

    // ---- accessors for CSV export ----
    public List<Survivor> getAllSurvivors() { return Collections.unmodifiableList(allSurvivors); }
    public List<Survivor> getFullyProcessedSurvivors() { return Collections.unmodifiableList(fullyProcessedSurvivors); }

    // ---- worker controls ----
    public void setMedicalWorkers(int n)       { medicalTreatmentStation.setWorkers(n); }
    public void setRegistrationWorkers(int n)  { registrationDesk.setWorkers(n); }
    public void setCommunicationWorkers(int n) { communicationCenter.setWorkers(n); }
    public void setSuppliesWorkers(int n)      { suppliesDistributionPoint.setWorkers(n); }
    public void setAccommodationWorkers(int n) { accommodationCenter.setWorkers(n); }
    public void setChildShelterWorkers(int n)  { childShelterAssignment.setWorkers(n); }
    public void setAdultShelterWorkers(int n)  { adultShelterAssignment.setWorkers(n); }
}
