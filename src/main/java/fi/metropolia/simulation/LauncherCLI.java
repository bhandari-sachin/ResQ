package fi.metropolia.simulation;

import fi.metropolia.simulation.controller.SimulationEngine;
import fi.metropolia.simulation.framework.Trace;

/**
 * Main class to run the rescue camp simulation
 */
public class LauncherCLI {

    public static void main(String[] args) {
        // Configure trace level for monitoring simulation progress
        Trace.setTraceLevel(Trace.Level.INFO);

        // Create and configure rescue camp simulation engine
        SimulationEngine rescueCampSimulation = new SimulationEngine();

        // Set simulation duration (in minutes) - simulate for 8 hours (480 minutes)
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

        System.out.println("\nRescue camp simulation completed successfully!");
        System.out.println("Real-time execution duration: " + (simulationEndTime - simulationStartTime) + " milliseconds");
    }
}
