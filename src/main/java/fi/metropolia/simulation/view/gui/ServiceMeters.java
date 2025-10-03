package fi.metropolia.simulation.view.gui;

import fi.metropolia.simulation.model.RescueCampServicePoint;
import fi.metropolia.simulation.model.SimulationEngine;

import java.lang.reflect.Field;

/**
 * Small helper DTO to pull max waiting time metrics from the SimulationEngine's
 * internal service-point fields using reflection.
 *
 * It expects the following field names in SimulationEngine:
 *  - medicalTreatmentStation
 *  - registrationDesk
 *  - communicationCenter
 *  - suppliesDistributionPoint
 *  - accommodationCenter
 *  - adultShelterAssignment
 *  - childShelterAssignment
 */
public final class ServiceMeters {

    public final double medicalMaxWait;
    public final double registrationMaxWait;
    public final double communicationMaxWait;
    public final double suppliesMaxWait;
    public final double accommodationMaxWait;
    public final double adultShelterMaxWait;
    public final double childShelterMaxWait;

    private ServiceMeters(double medicalMaxWait,
                          double registrationMaxWait,
                          double communicationMaxWait,
                          double suppliesMaxWait,
                          double accommodationMaxWait,
                          double adultShelterMaxWait,
                          double childShelterMaxWait) {
        this.medicalMaxWait = medicalMaxWait;
        this.registrationMaxWait = registrationMaxWait;
        this.communicationMaxWait = communicationMaxWait;
        this.suppliesMaxWait = suppliesMaxWait;
        this.accommodationMaxWait = accommodationMaxWait;
        this.adultShelterMaxWait = adultShelterMaxWait;
        this.childShelterMaxWait = childShelterMaxWait;
    }

    public static ServiceMeters fromEngine(SimulationEngine engine) {
        double medical         = maxWait(engine, "medicalTreatmentStation");
        double registration    = maxWait(engine, "registrationDesk");
        double communication   = maxWait(engine, "communicationCenter");
        double supplies        = maxWait(engine, "suppliesDistributionPoint");
        double accommodation   = maxWait(engine, "accommodationCenter");
        double adultShelter    = maxWait(engine, "adultShelterAssignment");
        double childShelter    = maxWait(engine, "childShelterAssignment");

        return new ServiceMeters(
                medical,
                registration,
                communication,
                supplies,
                accommodation,
                adultShelter,
                childShelter
        );
    }

    private static double maxWait(SimulationEngine engine, String fieldName) {
        try {
            Field f = engine.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            Object sp = f.get(engine);
            if (sp instanceof RescueCampServicePoint) {
                RescueCampServicePoint p = (RescueCampServicePoint) sp;
                // FIX: call the actual method available on the service point
                return p.getMaxWaitingTime();
            }
        } catch (ReflectiveOperationException ignored) {
            // fall through to return 0.0
        }
        return 0.0;
    }
}
