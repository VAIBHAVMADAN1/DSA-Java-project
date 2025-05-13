import java.io.*;
import java.util.*;
import java.time.LocalDate;
import java.time.LocalTime;

public class EventManager {
    private static Map<String, Student> studentMap = new HashMap<>();
    private static Scanner scanner = new Scanner(System.in);
    private static final String STUDENTS_FILE = "students.csv";
    private static final String EVENTS_FILE = "events.csv";

    private static List<String> buildings = new ArrayList<>();

    public static void main(String[] args) {
        ensureEventsFileExists();
        loadBuildings();
        loadStudents();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = getIntInput("Choose an option: ");
            switch (choice) {
                case 1 -> addEvent();
                case 2 -> viewEvents();
                case 3 -> updateEvent();
                case 4 -> deleteEvent();
                case 5 -> manageParticipants();
                case 6 -> running = false;
                case 7 -> clearConsole();
                default -> System.out.println("Invalid choice. Try again.");
            }
            if (choice != 7 && choice != 6) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
        System.out.println("Exiting Event Manager. Goodbye!");
    }

    private static void addEvent() {
        System.out.print("Event Name: ");
        String name = scanner.nextLine();

        String building = chooseBuilding();

        System.out.print("Event Room: ");
        String room = scanner.nextLine();

        LocalDate date = getDateInput("Event Date (YYYY-MM-DD) ");
        LocalTime time = getTimeInput("Event Start Time (HH:MM) ");

        System.out.print("Organiser: ");
        String organiser = scanner.nextLine();

        System.out.print("Event Details: ");
        String details = scanner.nextLine();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(EVENTS_FILE, true))) {
            bw.write(String.join(",", name, building, room, date.toString(), time.toString(), organiser, details));
            bw.newLine();
            System.out.println("Event added successfully.");
        } catch (IOException e) {
            System.out.println("Error adding event: " + e.getMessage());
        }
    }

    private static void viewEvents() {
        try (BufferedReader br = new BufferedReader(new FileReader(EVENTS_FILE))) {
            String line;
            int id = 1;
            while ((line = br.readLine()) != null) {
                System.out.println("\nEvent ID: " + id++);
                System.out.println(formatEvent(line));
            }
            if (id == 1) {
                System.out.println("There are no events yet.");
            }
        } catch (IOException e) {
            System.out.println("Error reading events: " + e.getMessage());
        }
    }

    private static void updateEvent() {
        List<String> updatedEvents = new ArrayList<>();
        viewEvents();
        int idToUpdate = getIntInput("Enter Event ID to update: ");
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(EVENTS_FILE))) {
            String line;
            int currentId = 1;
            while ((line = br.readLine()) != null) {
                if (currentId == idToUpdate) {
                    found = true;
                    String[] data = line.split(",", -1);

                    System.out.print("New Event Name (" + data[0] + "): ");
                    String name = inputOrDefault(data[0]);

                    String building = chooseBuilding();

                    System.out.print("New Room (" + data[2] + "): ");
                    String room = inputOrDefault(data[2]);

                    LocalDate date = getDateInput("New Date (YYYY-MM-DD): ");
                    LocalTime time = getTimeInput("New Start Time (HH:MM): ");

                    System.out.print("New Organiser (" + data[5] + "): ");
                    String organiser = inputOrDefault(data[5]);

                    System.out.print("New Details: ");
                    String details = inputOrDefault(data[6]);

                    updatedEvents.add(String.join(",", name, building, room, date.toString(), time.toString(),
                            organiser, details));
                } else {
                    updatedEvents.add(line);
                }
                currentId++;
            }
        } catch (IOException e) {
            System.out.println("Error reading events: " + e.getMessage());
            return;
        }

        if (!found) {
            System.out.println("Invalid Event ID.");
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(EVENTS_FILE))) {
            for (String event : updatedEvents) {
                bw.write(event);
                bw.newLine();
            }
            System.out.println("Event updated successfully.");
        } catch (IOException e) {
            System.out.println("Error updating event: " + e.getMessage());
        }
    }

    private static void deleteEvent() {
        List<String> remainingEvents = new ArrayList<>();
        viewEvents();
        int idToDelete = getIntInput("Enter Event ID to delete: ");
        boolean found = false;

        try (BufferedReader br = new BufferedReader(new FileReader(EVENTS_FILE))) {
            String line;
            int currentId = 1;
            while ((line = br.readLine()) != null) {
                if (currentId == idToDelete) {
                    found = true;
                } else {
                    remainingEvents.add(line);
                }
                currentId++;
            }
        } catch (IOException e) {
            System.out.println("Error reading events: " + e.getMessage());
            return;
        }

        if (!found) {
            System.out.println("Invalid Event ID.");
            return;
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(EVENTS_FILE))) {
            for (String event : remainingEvents) {
                bw.write(event);
                bw.newLine();
            }
            System.out.println("Event deleted successfully.");
        } catch (IOException e) {
            System.out.println("Error deleting event: " + e.getMessage());
        }
    }

    private static String formatEvent(String csv) {
        String[] data = csv.split(",", -1);
        return "Event: " + data[0] + " | Building: " + data[1] + " | Room: " + data[2] +
                " | Date: " + data[3] + " | Time: " + data[4] +
                " | Organiser: " + data[5] + "\nDetails: " + data[6];
    }

    private static void ensureEventsFileExists() {
        File file = new File(EVENTS_FILE);
        try {
            if (file.createNewFile()) {
                System.out.println("Created new file: " + EVENTS_FILE);
            }
        } catch (IOException e) {
            System.out.println("Failed to create file: " + EVENTS_FILE);
        }
    }

    private static LocalDate getDateInput(String prompt) {
        System.out.print(prompt + " (Press Enter for today's date): ");
        String input = scanner.nextLine().trim();
        if (input.isEmpty()) {
            return LocalDate.now();
        }
        while (true) {
            try {
                return LocalDate.parse(input);
            } catch (Exception e) {
                System.out.print("Invalid date format. Enter again (YYYY-MM-DD): ");
                input = scanner.nextLine().trim();
                if (input.isEmpty())
                    return LocalDate.now();
            }
        }
    }

    private static LocalTime getTimeInput(String prompt) {
        System.out.print(prompt);
        while (true) {
            try {
                return LocalTime.parse(scanner.nextLine());
            } catch (Exception e) {
                System.out.print("Invalid time format. Try again (HH:MM): ");
            }
        }
    }

    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid input. Try again: ");
            scanner.next();
        }
        int value = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return value;
    }

    private static String inputOrDefault(String oldValue) {
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? oldValue : input;
    }

    private static void printMenu() {
        System.out.println("\n=== Campus Event Manager ===");
        System.out.println("1. Add Event");
        System.out.println("2. View All Events");
        System.out.println("3. Update Event");
        System.out.println("4. Delete Event");
        System.out.println("5. Manage Participants");
        System.out.println("6. Exit");
        System.out.println("7. Clear Console");
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            System.out.println("Could not clear console.");
        }
    }

    private static void loadStudents() {
        try (BufferedReader br = new BufferedReader(new FileReader(STUDENTS_FILE))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                Student student = new Student(data[0], data[1], data[2], data[3], data[4],
                        Integer.parseInt(data[5]), Boolean.parseBoolean(data[6]), data[7]);
                studentMap.put(data[2], student); // Roll number as key
            }
        } catch (IOException e) {
            System.out.println("Error loading students: " + e.getMessage());
        }
    }

    private static void loadBuildings() {
        try (BufferedReader br = new BufferedReader(new FileReader("locations.txt"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split("\t");
                buildings.add(parts[0]); // Add building name
            }
        } catch (IOException e) {
            System.out.println("Error loading buildings: " + e.getMessage());
        }
    }

    private static String chooseBuilding() {
        if (buildings.isEmpty()) {
            System.out.println("No buildings available.");
            return "Unknown";
        }

        System.out.println("Choose a building:");
        for (int i = 0; i < buildings.size(); i++) {
            System.out.println((i + 1) + ". " + buildings.get(i));
        }

        int choice = getIntInput("Enter the number corresponding to the building: ");
        while (choice < 1 || choice > buildings.size()) {
            System.out.print("Invalid choice. Try again: ");
            choice = getIntInput("Enter the number corresponding to the building: ");
        }

        return buildings.get(choice - 1);
    }

    private static void manageParticipants() {
        System.out.println("Enter comma-separated roll numbers of participants:");
        String input = scanner.nextLine();
        String[] rollNumbers = input.split(",");

        List<String> validRollNumbers = new ArrayList<>();
        List<String> invalidRollNumbers = new ArrayList<>();

        for (String rollNumber : rollNumbers) {
            rollNumber = rollNumber.trim();
            if (studentMap.containsKey(rollNumber)) {
                validRollNumbers.add(rollNumber);
            } else {
                invalidRollNumbers.add(rollNumber);
            }
        }

        System.out.println("\nValid Participants:");
        for (String rollNumber : validRollNumbers) {
            Student student = studentMap.get(rollNumber);
            System.out.println("Roll Number: " + rollNumber + ", Name: " + student.firstName + " " + student.lastName);
        }

        if (!invalidRollNumbers.isEmpty()) {
            System.out.println("\nInvalid Roll Numbers:");
            for (String rollNumber : invalidRollNumbers) {
                System.out.println(rollNumber);
            }
        }
    }

}