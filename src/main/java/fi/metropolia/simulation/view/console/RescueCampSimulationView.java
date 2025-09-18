package fi.metropolia.simulation.view.console;

import fi.metropolia.simulation.framework.Trace;
import fi.metropolia.simulation.model.*;

import java.util.List;

/**
 * VIEW: Display and user interface logic for rescue camp simulation
 * Handles all output formatting and presentation logic
 */
public class RescueCampSimulationView {

    public void displaySimulationStart() {
        System.out.println("=".repeat(80));
        System.out.println("RESCUE CAMP SIMULATION STARTING");
        System.out.println("=".repeat(80));
        Trace.out(Trace.Level.INFO, "=== Rescue Camp Simulation Starting ===");
    }

    public void displaySurvivorArrival(Survivor survivor) {
        String familyInfo = survivor.hasFamily() ? ", Family size: " + survivor.getFamilyMemberCount() : ", Individual survivor";
        String message = String.format("NEW ARRIVAL: Survivor #%d arrived at camp at %.2f minutes - Age: %d, Health: %s%s",
                survivor.getSurvivorId(),
                survivor.getCampArrivalTime(),
                survivor.getSurvivorAge(),
                survivor.getHealthCondition(),
                familyInfo);
        System.out.println(message);
        Trace.out(Trace.Level.INFO, message);
    }

    public void displayServiceAssignment(Survivor survivor, String serviceName) {
        String message = String.format("ROUTING: Survivor #%d assigned to %s", survivor.getSurvivorId(), serviceName);
        Trace.out(Trace.Level.INFO, message);
    }

    public void displayServiceStart(RescueCampServicePoint servicePoint) {
        String message = String.format("SERVICE STARTED: %s is now serving a survivor (queue length: %d)",
                servicePoint.getServicePointName(),
                servicePoint.getCurrentQueueLength());
        Trace.out(Trace.Level.INFO, message);
    }

    public void displaySurvivorProgress(Survivor survivor, String completedService) {
        String message = String.format("PROGRESS: Survivor #%d completed %s", survivor.getSurvivorId(), completedService);
        Trace.out(Trace.Level.INFO, message);
    }

    public void displaySurvivorCompletion(Survivor survivor) {
        System.out.println(String.format("*** SETTLEMENT COMPLETE: Survivor #%d successfully settled in rescue camp ***", survivor.getSurvivorId()));
        displaySurvivorDetailedReport(survivor);
    }

    private void displaySurvivorDetailedReport(Survivor survivor) {
        System.out.println("\n=== Survivor #" + survivor.getSurvivorId() + " Processing Complete ===");
        System.out.println("Camp arrival time: " + String.format("%.2f", survivor.getCampArrivalTime()) + " minutes");
        System.out.println("Processing completion time: " + String.format("%.2f", survivor.getProcessingCompletionTime()) + " minutes");
        System.out.println("Total time in camp: " + String.format("%.2f", survivor.getTotalTimeInCamp()) + " minutes");
        System.out.println("Total waiting time: " + String.format("%.2f", survivor.getTotalWaitingTime()) + " minutes");
        System.out.println("Age: " + survivor.getSurvivorAge() + " years (" + survivor.getAgeCategory() + ")");
        System.out.println("Health condition: " + survivor.getHealthCondition());
        System.out.println("Family status: " + (survivor.hasFamily() ? "Has family (size: " + survivor.getFamilyMemberCount() + ")" : "Individual"));
        System.out.println("Services required: Medical(" + survivor.requiresMedicalTreatment() + "), Communication(" + survivor.requestsCommunicationService() + ")");
        System.out.println("-".repeat(50));
    }

    public void displayServicePointStatistics(RescueCampServicePoint servicePoint) {
        double maxWaitingTime = servicePoint.getMaxWaitingTime(); // Local variable tracking max waiting
        System.out.println("\n=== Service Point: " + servicePoint.getServicePointName() + " Statistics ===");
        System.out.println("Total survivors served: " + servicePoint.getTotalServed());
        System.out.println("Current queue length: " + servicePoint.getCurrentQueueLength());
        System.out.println("Average service time: " + String.format("%.2f", servicePoint.getAverageServiceTime()) + " minutes");
        System.out.println("Maximum waiting time observed: " + String.format("%.2f", maxWaitingTime) + " minutes");
        System.out.println("-".repeat(50));
        Trace.out(Trace.Level.INFO, "Service statistics displayed for " + servicePoint.getServicePointName());
    }

    public void displayOverallStatistics(List<Survivor> survivors) {
        double totalTime = 0;
        double totalWaiting = 0;
        int totalSurvivors = survivors.size();

        for (Survivor s : survivors) {
            totalTime += s.getTotalTimeInCamp();
            totalWaiting += s.getTotalWaitingTime();
        }

        System.out.println("\n=== Overall Simulation Statistics ===");
        System.out.println("Total survivors processed: " + totalSurvivors);
        System.out.println("Average total time in camp: " + String.format("%.2f", totalTime / totalSurvivors) + " minutes");
        System.out.println("Average waiting time: " + String.format("%.2f", totalWaiting / totalSurvivors) + " minutes");
        System.out.println("-".repeat(50));
        Trace.out(Trace.Level.INFO, "Overall simulation statistics displayed");
    }

    public void displayFinalResults(double currentTime, int totalArrivals, int totalProcessed, List<Survivor> survivors, List<RescueCampServicePoint> servicePoints) {
        System.out.println("\n=== SIMULATION COMPLETE ===");
        System.out.println("Simulation time: " + String.format("%.2f", currentTime) + " minutes");
        System.out.println("Total survivors arrived: " + totalArrivals);
        System.out.println("Total survivors processed: " + totalProcessed);

        System.out.println("\n--- Individual Survivor Reports ---");
        for (Survivor s : survivors) {
            displaySurvivorDetailedReport(s);
        }

        System.out.println("\n--- Service Point Statistics ---");
        for (RescueCampServicePoint sp : servicePoints) {
            displayServicePointStatistics(sp);
        }

        displayOverallStatistics(survivors);
        Trace.out(Trace.Level.INFO, "Simulation final results displayed.");
    }
}
