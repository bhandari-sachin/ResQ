package fi.metropolia.simulation.csv;

import fi.metropolia.simulation.model.Survivor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class CsvExporter {
    public static void writeSurvivorsToCsv(String filePath, List<Survivor> survivors) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // header row (family fields removed)
            writer.write("ID,Age,AgeCategory,HealthCondition,RequiresMedicalTreatment,RequestsCommunicationService");
            writer.newLine();

            // data rows
            for (Survivor s : survivors) {
                writer.write(
                        s.getSurvivorId() + "," +
                                s.getSurvivorAge() + "," +
                                s.getAgeCategory() + "," +
                                s.getHealthCondition() + "," +
                                s.requiresMedicalTreatment() + "," +
                                s.requestsCommunicationService()
                );
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
