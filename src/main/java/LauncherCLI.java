import fi.metropolia.simulation.model.SimulationEngine; // FIXED: correct package
import fi.metropolia.simulation.csv.CsvExporter;
import fi.metropolia.simulation.framework.Trace;
import fi.metropolia.simulation.model.Survivor;

import java.util.List;

/**
 * Main class to run the rescue camp simulation
 */
public class LauncherCLI {

    public static void main(String[] args) {
        // Configure trace level for monitoring simulation progress
        Trace.setTraceLevel(Trace.Level.INFO);

        // Create and configure rescue camp simulation engine
        SimulationEngine rescueCampSimulation = new SimulationEngine();

        // --- Set initial staffing ---
        rescueCampSimulation.setMedicalWorkers(6);
        rescueCampSimulation.setRegistrationWorkers(3);
        rescueCampSimulation.setCommunicationWorkers(2);
        rescueCampSimulation.setSuppliesWorkers(2);
        rescueCampSimulation.setAccommodationWorkers(2); // NEW: SC-5
        rescueCampSimulation.setChildShelterWorkers(2);
        rescueCampSimulation.setAdultShelterWorkers(2);
        // REMOVED: family shelter workers (no-family scenario)

        // Set simulation duration (in minutes)
        double simulationDurationMinutes = 480.0;
        rescueCampSimulation.setSimulationDuration(simulationDurationMinutes);

        System.out.println("=".repeat(60));
        System.out.println("RESCUE CAMP SIMULATION STARTING");
        System.out.println("=".repeat(60));
        System.out.println("Simulation duration: " + simulationDurationMinutes + " minutes (8 hours)");
        System.out.println("Trace monitoring level: " + Trace.Level.INFO);
        System.out.println("Simulating rescue camp operations...");
        System.out.println("-".repeat(60));

        // Execute the rescue camp simulation
        long simulationStartTime = System.currentTimeMillis();
        rescueCampSimulation.startSimulation();
        long simulationEndTime = System.currentTimeMillis();

        // Export ALL generated survivors to CSV
        List<Survivor> survivors = rescueCampSimulation.getAllSurvivors();
        CsvExporter.writeSurvivorsToCsv("survivors.csv", survivors);
        System.out.println("Exported " + survivors.size() + " survivors to survivors.csv");

        System.out.println("\nRescue camp simulation completed successfully!");
        System.out.println("Real-time execution duration: " + (simulationEndTime - simulationStartTime) + " milliseconds");
    }
}
