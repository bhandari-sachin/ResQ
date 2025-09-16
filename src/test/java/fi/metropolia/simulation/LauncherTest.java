package fi.metropolia.simulation;

import fi.metropolia.simulation.framework.Engine;
import fi.metropolia.simulation.framework.Trace;
import fi.metropolia.simulation.framework.Trace.Level;
import fi.metropolia.simulation.model.SimulationEngine;

/**
 * Command-line type User Interface
 *
 * With setTraceLevel() you can control the number of diagnostic messages printed to the console.
 */
public class LauncherTest {
    public static void main(String[] args) {
        Trace.setTraceLevel(Level.INFO);

        Engine m = new SimulationEngine();
        m.setSimulationTime(1000);
        m.run();
    }
}

