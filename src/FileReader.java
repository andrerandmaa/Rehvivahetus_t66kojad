import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class FileReader {

    public Map<String, List<List<String>>> readFile(String filePath) {
        // Saves information about workshop. Name - 1. address, 2. serviceable vehicles, 3. api url
        Map<String, List<List<String>>> fileContents = new HashMap<>();

        int rowNumber = 1;
        String location = "";
        boolean faultyLine = false; // For Address line
        List<List<String>> allValues = new ArrayList<>();

        try (BufferedReader reader = Files.newBufferedReader(Paths.get(filePath))) {
            String line;
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String[] splittedLine = line.split(",");
                // Finds address
                if (rowNumber % 3 == 1) {
                    allValues = new ArrayList<>();
                    // Checks, if file line is in format: Street address, City (must have comma)
                    if (splittedLine.length != 1) {
                        location = splittedLine[splittedLine.length - 1].trim();
                    } else {
                        faultyLine = true;
                        break;
                    }
                    allValues.add(Collections.singletonList(line));
                }
                // Adds all serviceable vehicle types
                else if (rowNumber % 3 == 2) {
                    List<String> vehicleTypes = new ArrayList<>();
                    for (String vehicleType : splittedLine) {
                        vehicleTypes.add(vehicleType.trim());
                    }
                    allValues.add(vehicleTypes);
                }
                // Adds api url
                else if (rowNumber % 3 == 0) {
                    allValues.add(Collections.singletonList(line));
                    fileContents.put(location, allValues);
                }
                rowNumber++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (rowNumber % 3 == 0 || rowNumber % 3 == 2 || faultyLine) {
            throw new RuntimeException("tireWorkshops.txt hasn't been configured correctly." +
                    "Format is following: 1.line - location (format: <address, town>), " +
                    "2.line - vehicle types (separated my commas), 3.line - REST API url");
        }

        return fileContents;
    }
}
