import java.io.*;
import java.util.*;

public class DistanceCalculator {

    // Method to calculate distance using the Haversine formula
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the Earth in kilometers
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // Distance in kilometers
    }

    public static void main(String[] args) throws IOException {
        // Read locations from the file
        File file = new File("locations.txt");
        List<String> locationNames = new ArrayList<>();
        List<double[]> coordinates = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length == 3) {
                    locationNames.add(parts[0]);
                    double lat = Double.parseDouble(parts[1]);
                    double lon = Double.parseDouble(parts[2]);
                    coordinates.add(new double[] { lat, lon });
                }
            }
        }

        // Display all locations to the user
        System.out.println("Available Locations:");
        for (int i = 0; i < locationNames.size(); i++) {
            System.out.printf("%d. %s%n", i + 1, locationNames.get(i));
        }

        // Ask the user to input two numbers
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter the number of the first location: ");
        int loc1 = scanner.nextInt() - 1;
        System.out.print("Enter the number of the second location: ");
        int loc2 = scanner.nextInt() - 1;

        // Validate input
        if (loc1 < 0 || loc1 >= locationNames.size() || loc2 < 0 || loc2 >= locationNames.size()) {
            System.out.println("Invalid input. Please restart the program and try again.");
            return;
        }

        // Calculate and display the distance
        double[] coord1 = coordinates.get(loc1);
        double[] coord2 = coordinates.get(loc2);
        double distance = calculateDistance(coord1[0], coord1[1], coord2[0], coord2[1]);
        System.out.printf("The distance between '%s' and '%s' is %.2f kilometers.%n",
                locationNames.get(loc1), locationNames.get(loc2), distance);
    }
}