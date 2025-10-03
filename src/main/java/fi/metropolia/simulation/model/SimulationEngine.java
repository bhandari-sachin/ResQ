package fi.metropolia.simulation.model;

import eduni.distributions.*;
import fi.metropolia.simulation.csv.CsvExporter;
import fi.metropolia.simulation.framework.*;
import fi.metropolia.simulation.view.console.RescueCampSimulationView;

import java.util.*;

/** Main simulation controller for rescue camp operations. */
public class SimulationEngine extends Engine {

    private RescueCampSimulationView view;
    private ArrivalProcess survivorArrivalProcess;

    private RescueCampServicePoint medicalTreatmentStation;   // SC-4
    private RescueCampServicePoint registrationDesk;          // SC-1
    private RescueCampServicePoint communicationCenter;       // SC-2
    private RescueCampServicePoint suppliesDistributionPoint; // SC-3
    private RescueCampServicePoint accommodationCenter;       // SC-5
    private RescueCampServicePoint childShelterAssignment;    // SC-6
    private RescueCampServicePoint adultShelterAssignment;    // SC-7

    // distributions (defaults)
    private double arrivalMean = 20.0;
    private double regMin = 3.0,   regMax = 5.0;
    private double comMin = 3.0,   comMax = 6.0;
    private double supMin = 4.0,   supMax = 7.0;
    private double medMin = 10.0,  medMax = 15.0;
    private double accMean = 6.0,  accSd  = 1.0;
    private double childMean = 5.0, childSd = 1.0;
    private double adultMean = 5.0, adultSd = 1.0;

    // stats
    private int totalSurvivorArrivals = 0;
    private int totalSurvivorsProcessed = 0;
    private final List<Survivor> allSurvivors = new ArrayList<>();
    private final List<Survivor> fullyProcessedSurvivors = new ArrayList<>();

    // pacing
    private volatile double animationSpeedMultiplier = 1.0;
    private static final double SPEED_MIN = 0.25, SPEED_MAX = 8.0;
    private volatile long baseDelayMs = 200;

    /** The file written at results(); for UI message. */
    private java.io.File autoExportFile;
    public java.io.File getAutoExportFile() { return autoExportFile; }

    public double getAnimationSpeedMultiplier() { return animationSpeedMultiplier; }
    public void setAnimationSpeedMultiplier(double m) {
        animationSpeedMultiplier = Math.max(SPEED_MIN, Math.min(SPEED_MAX, m));
    }
    public void setBaseDelayMs(long ms) { baseDelayMs = Math.max(0, ms); }
    private void slowDownTick() {
        long delay = baseDelayMs;
        double mult = animationSpeedMultiplier;
        long sleep = (long) Math.max(0, delay / mult);
        if (sleep <= 0) return;
        try { Thread.sleep(sleep); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
    }

    // ---- constructors ----
    public SimulationEngine() {
        this.view = new RescueCampSimulationView();
        initializeCampServicePoints();
        initializeSurvivorArrivalProcess();
    }
    public SimulationEngine(double arrivalMeanMinutes) {
        this.arrivalMean = arrivalMeanMinutes;
        this.view = new RescueCampSimulationView();
        initializeCampServicePoints();
        initializeSurvivorArrivalProcess();
    }
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
        this.arrivalMean = arrivalMean;
        this.regMin = regMin;   this.regMax = regMax;
        this.comMin = comMin;   this.comMax = comMax;
        this.supMin = supMin;   this.supMax = supMax;
        this.medMin = medMin;   this.medMax = medMax;
        this.accMean = accMean; this.accSd  = accSd;
        this.childMean = childMean; this.childSd = childSd;
        this.adultMean = adultMean; this.adultSd = adultSd;
        initializeCampServicePoints();
        initializeSurvivorArrivalProcess();
    }

    private void initializeCampServicePoints() {
        medicalTreatmentStation = new RescueCampServicePoint(
                new Uniform(medMin, medMax), eventList,
                RescueCampEventType.MEDICAL_TREATMENT_COMPLETE, "Medical Treatment Station");
        registrationDesk = new RescueCampServicePoint(
                new Uniform(regMin, regMax), eventList,
                RescueCampEventType.REGISTRATION_COMPLETE, "Registration Desk");
        communicationCenter = new RescueCampServicePoint(
                new Uniform(comMin, comMax), eventList,
                RescueCampEventType.COMMUNICATION_SERVICE_COMPLETE, "Communication Center");
        suppliesDistributionPoint = new RescueCampServicePoint(
                new Uniform(supMin, supMax), eventList,
                RescueCampEventType.SUPPLIES_DISTRIBUTION_COMPLETE, "Supplies Distribution Point");
        accommodationCenter = new RescueCampServicePoint(
                new Normal(accMean, accSd), eventList,
                RescueCampEventType.ACCOMMODATION_CENTER_COMPLETE, "Accommodation Center");
        childShelterAssignment = new RescueCampServicePoint(
                new Normal(childMean, childSd), eventList,
                RescueCampEventType.CHILD_SHELTER_ASSIGNMENT_COMPLETE, "Child Shelter Assignment");
        adultShelterAssignment = new RescueCampServicePoint(
                new Normal(adultMean, adultSd), eventList,
                RescueCampEventType.ADULT_SHELTER_ASSIGNMENT_COMPLETE, "Adult Shelter Assignment");

        medicalTreatmentStation.setWorkers(5);
        registrationDesk.setWorkers(2);
        communicationCenter.setWorkers(2);
        suppliesDistributionPoint.setWorkers(2);
        accommodationCenter.setWorkers(2);
        childShelterAssignment.setWorkers(2);
        adultShelterAssignment.setWorkers(2);
    }

    private void initializeSurvivorArrivalProcess() {
        int seed = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
        ContinuousGenerator survivorArrivalTimeGenerator = new Negexp(arrivalMean, seed);
        survivorArrivalProcess = new ArrivalProcess(
                survivorArrivalTimeGenerator, eventList, RescueCampEventType.SURVIVOR_ARRIVAL);
    }

    @Override
    protected void initialize() {
        Clock.getInstance().reset();
        view.displaySimulationStart();
        survivorArrivalProcess.generateNextEvent();
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
                    accommodationCenter.addSurvivorToQueue(survivor);
                    view.displayServiceAssignment(survivor, "Accommodation Center");
                }
                break;

            case ACCOMMODATION_CENTER_COMPLETE:
                survivor = accommodationCenter.removeSurvivorFromQueue();
                if (survivor != null) {
                    view.displaySurvivorProgress(survivor, "Accommodation Center Complete");
                    if (survivor.getAgeCategory() == Survivor.AgeCategory.CHILD) {
                        // Assign home & time (SC-6) then queue
                        survivor.assignTemporaryHome();
                        childShelterAssignment.addSurvivorToQueue(survivor);
                        view.displayServiceAssignment(survivor, "Child Shelter Assignment");
                    } else {
                        // Assign home & time (SC-7) then queue
                        survivor.assignTemporaryHome();
                        adultShelterAssignment.addSurvivorToQueue(survivor);
                        view.displayServiceAssignment(survivor, "Adult Shelter Assignment");
                    }
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
        }

        slowDownTick();
    }

    private void handleNewSurvivorArrival() {
        Survivor newSurvivor = new Survivor();
        totalSurvivorArrivals++;
        allSurvivors.add(newSurvivor);
        view.displaySurvivorArrival(newSurvivor);

        if (newSurvivor.requiresMedicalTreatment()) {
            medicalTreatmentStation.addSurvivorToQueue(newSurvivor);
            view.displayServiceAssignment(newSurvivor, "Medical Treatment Station");
        } else {
            registrationDesk.addSurvivorToQueue(newSurvivor);
            view.displayServiceAssignment(newSurvivor, "Registration Desk");
        }

        survivorArrivalProcess.generateNextEvent();
    }

    private void routeSurvivorAfterRegistration(Survivor survivor) {
        if (survivor.getAgeCategory() == Survivor.AgeCategory.ADULT && survivor.requestsCommunicationService()) {
            communicationCenter.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Communication Center");
        } else {
            suppliesDistributionPoint.addSurvivorToQueue(survivor);
            view.displayServiceAssignment(survivor, "Supplies Distribution Point");
        }
    }

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

        // Auto-save: delete previous CSVs and write ONLY the current data + 2-decimal stats section
        autoExportFile = CsvExporter.writeOnlyCurrentWithStats(allSurvivors, fullyProcessedSurvivors);
    }

    // control
    public void setSimulationDuration(double minutes) { setSimulationTime(minutes); }
    public void startSimulation() { run(); }

    public List<Survivor> getAllSurvivors() { return Collections.unmodifiableList(allSurvivors); }
    public List<Survivor> getFullyProcessedSurvivors() { return Collections.unmodifiableList(fullyProcessedSurvivors); }

    public void setMedicalWorkers(int n)       { medicalTreatmentStation.setWorkers(n); }
    public void setRegistrationWorkers(int n)  { registrationDesk.setWorkers(n); }
    public void setCommunicationWorkers(int n) { communicationCenter.setWorkers(n); }
    public void setSuppliesWorkers(int n)      { suppliesDistributionPoint.setWorkers(n); }
    public void setAccommodationWorkers(int n) { accommodationCenter.setWorkers(n); }
    public void setChildShelterWorkers(int n)  { childShelterAssignment.setWorkers(n); }
    public void setAdultShelterWorkers(int n)  { adultShelterAssignment.setWorkers(n); }
}
