package fi.metropolia.simulation.csv;



import fi.metropolia.simulation.model.Survivor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {
    public static void writeSurvivorsToCsv(String filePath, List<Survivor> survivors) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // ⬇️ ADD "AssignedHome" to include SC-6/SC-7 result
            writer.write("ID,Age,AgeCategory,HealthCondition,RequiresMedicalTreatment,RequestsCommunicationService,AssignedHome");
            writer.newLine();

            for (Survivor s : survivors) {
                // ⬇️ write assigned home (empty if not yet assigned)
                String assignedHome = s.getAssignedHomeName();
                writer.write(
                        s.getSurvivorId() + "," +
                                s.getSurvivorAge() + "," +
                                s.getAgeCategory() + "," +
                                s.getHealthCondition() + "," +
                                s.requiresMedicalTreatment() + "," +
                                s.requestsCommunicationService() + "," +
                                (assignedHome == null ? "" : assignedHome)
                );
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
