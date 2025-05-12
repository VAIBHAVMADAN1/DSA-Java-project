import java.io.*;
import java.util.*;

public class StudentManager {
    private static final String FILE_NAME = "students.csv";
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        boolean running = true;
        while (running) {
            printMenu();
            int choice = getIntInput("Choose an option: ");
            switch (choice) {
                case 1 -> addStudent();
                case 2 -> viewStudents();
                case 3 -> updateStudent();
                case 4 -> deleteStudent();
                case 5 -> clearConsole();
                case 6 -> running = false;
                default -> System.out.println("Invalid choice.");
            }
            if (choice != 5 && choice != 6) {
                System.out.println("\nPress Enter to continue...");
                scanner.nextLine();
            }
        }
        System.out.println("Goodbye!");
    }

    private static void addStudent() {
        System.out.print("First Name: ");
        String first = scanner.nextLine();
        System.out.print("Last Name: ");
        String last = scanner.nextLine();
        System.out.print("Roll Number: ");
        String roll = scanner.nextLine();
        System.out.print("Email ID: ");
        String email = scanner.nextLine();
        System.out.print("Course: ");
        String course = scanner.nextLine();
        int group = getIntInput("Group Number: ");
        System.out.print("Is Hosteller? (yes/no): ");
        boolean hosteller = scanner.nextLine().equalsIgnoreCase("yes");
        String hostel = "";
        if (hosteller) {
            System.out.print("Hostel Name: ");
            hostel = scanner.nextLine();
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            out.println(String.join(",", first, last, roll, email, course, String.valueOf(group),
                    String.valueOf(hosteller), hostel));
            System.out.println("Student added.");
        } catch (IOException e) {
            System.out.println("Error saving student: " + e.getMessage());
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows"))
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            else
                System.out.print("\033[H\033[2J");
            System.out.flush();
        } catch (Exception e) {
            System.out.println("Could not clear console.");
        }
    }

    private static void deleteStudent() {
        System.out.print("Enter roll number to delete: ");
        String roll = scanner.nextLine();
        List<String> newLines = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", -1);
                if (data.length >= 3 && data[2].equalsIgnoreCase(roll)) {
                    found = true;
                    continue;
                }
                newLines.add(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading file.");
            return;
        }

        if (!found) {
            System.out.println("Student not found.");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (String s : newLines)
                out.println(s);
            System.out.println("Student deleted.");
        } catch (IOException e) {
            System.out.println("Error saving file.");
        }
    }

    private static String formatStudent(String csv) {
        String[] data = csv.split(",", -1);
        if (data.length < 7)
            return "Invalid student data.";
        return data[0] + " " + data[1] + " | Roll: " + data[2] + " | Email: " + data[3] +
                " | Course: " + data[4] + " | Group: " + data[5] +
                " | " + (Boolean.parseBoolean(data[6]) ? "Hosteller (" + (data.length > 7 ? data[7] : "N/A") + ")"
                        : "Day Scholar");
    }

    private static int getIntInput(String prompt) {
        System.out.print(prompt);
        while (!scanner.hasNextInt()) {
            System.out.print("Invalid number. Try again: ");
            scanner.next();
        }
        int num = scanner.nextInt();
        scanner.nextLine(); // consume newline
        return num;
    }

    private static String inputOrDefault(String oldValue) {
        String input = scanner.nextLine().trim();
        return input.isEmpty() ? oldValue : input;
    }

    private static void printMenu() {
        System.out.println("\n=== Student Manager ===");
        System.out.println("1. Add Student");
        System.out.println("2. View Students");
        System.out.println("3. Update Student");
        System.out.println("4. Delete Student");
        System.out.println("5. Clear Console");
        System.out.println("6. Exit");
    }

    private static void updateStudent() {
        System.out.print("Enter roll number of student to update: ");
        String rollToUpdate = scanner.nextLine();
        List<String> updatedLines = new ArrayList<>();
        boolean found = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] data = line.split(",", -1);
                if (data.length >= 7 && data[2].equalsIgnoreCase(rollToUpdate)) {
                    found = true;

                    System.out.print("First Name (" + data[0] + "): ");
                    String first = inputOrDefault(data[0]);

                    System.out.print("Last Name (" + data[1] + "): ");
                    String last = inputOrDefault(data[1]);

                    System.out.print("Email ID (" + data[3] + "): ");
                    String email = inputOrDefault(data[3]);

                    System.out.print("Course (" + data[4] + "): ");
                    String course = inputOrDefault(data[4]);

                    System.out.print("Group Number (" + data[5] + "): ");
                    String groupStr = inputOrDefault(data[5]);

                    System.out.print("Is Hosteller? (" + data[6] + "): ");
                    String hostellerStr = inputOrDefault(data[6]);

                    String hostelName = "";
                    if (Boolean.parseBoolean(hostellerStr)) {
                        System.out.print("Hostel Name (" + (data.length > 7 ? data[7] : "") + "): ");
                        hostelName = inputOrDefault(data.length > 7 ? data[7] : "");
                    }

                    updatedLines.add(String.join(",", first, last, rollToUpdate, email, course, groupStr, hostellerStr,
                            hostelName));
                } else {
                    updatedLines.add(line);
                }
            }
        } catch (IOException e) {
            System.out.println("Error reading file.");
            return;
        }

        if (!found) {
            System.out.println("Student not found.");
            return;
        }

        try (PrintWriter out = new PrintWriter(new FileWriter(FILE_NAME))) {
            for (String s : updatedLines)
                out.println(s);
            System.out.println("Student updated successfully.\n");
            for (String s : updatedLines) {
                String[] data = s.split(",", -1);
                if (data.length >= 3 && data[2].equalsIgnoreCase(rollToUpdate)) {
                    System.out.println("Updated Record:\n" + formatStudent(s));
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error saving file.");
        }

    }

    private static void viewStudents() {
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            System.out.println("\n--- Students ---");
            while ((line = reader.readLine()) != null) {
                System.out.println(formatStudent(line));
            }
        } catch (IOException e) {
            System.out.println("No students found or file missing.");
        }
    }
    
}
