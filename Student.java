public class Student {
    public String firstName;
    public String lastName;
    public String rollNumber;
    public String emailId;
    public String course;
    public int groupNumber;
    public boolean isHosteller;
    public String hostelName;

    public Student(String firstName, String lastName, String rollNumber, String emailId,
            String course, int groupNumber, boolean isHosteller, String hostelName) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.rollNumber = rollNumber;
        this.emailId = emailId;
        this.course = course;
        this.groupNumber = groupNumber;
        this.isHosteller = isHosteller;
        this.hostelName = hostelName;
    }

    public Student(String firstName, String lastName, String rollNumber, String emailId,
            String course, int groupNumber, boolean isHosteller) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.rollNumber = rollNumber;
        this.emailId = emailId;
        this.course = course;
        this.groupNumber = groupNumber;
        this.isHosteller = isHosteller;
    }

    @Override
    public String toString() {
        return firstName + " " + lastName + " | Roll: " + rollNumber + " | Email: " + emailId +
                " | Course: " + course + " | Group: " + groupNumber +
                " | " + (isHosteller ? "Hosteller (" + hostelName + ")" : "Day Scholar");
    }
}
