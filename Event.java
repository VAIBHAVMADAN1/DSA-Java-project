import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Event {
    public String eventName;
    public String eventBuilding;
    public String eventRoom;
    public LocalDate eventDate;
    public LocalTime eventStartTime;
    public String eventOrganiser;
    public String eventDetails;
    public List<Student> participants;

    public Event(String eventName, String eventBuilding, String eventRoom, LocalDate eventDate, LocalTime eventStartTime,
            String eventOrganiser, String eventDetails) {
        this.eventName = eventName;
        this.eventBuilding = eventBuilding;
        this.eventRoom = eventRoom;
        this.eventDate = eventDate;
        this.eventStartTime = eventStartTime;
        this.eventOrganiser = eventOrganiser;
        this.eventDetails = eventDetails;
        this.participants = new ArrayList<>();
    }

    public void addParticipant(Student s) {
        participants.add(s);
    }
    public void removeParticipant(String rollNumber) {
        participants.removeIf(s -> s.rollNumber.equals(rollNumber));
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }
    public void setEventBuilding(String eventBuilding) {
        this.eventBuilding = eventBuilding;
    }
    
    public void setEventRoom(String eventRoom) {
        this.eventRoom = eventRoom;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }
    public void setEventStartTime(LocalTime eventStartTime) {
        this.eventStartTime = eventStartTime;
    }
    public void setEventOrganiser(String eventOrganiser) {
        this.eventOrganiser = eventOrganiser;
    }
    public void setEventDetails(String eventDetails) {
        this.eventDetails = eventDetails;
    }

    @Override
    public String toString() {
        return "Event: " + eventName + " | Building: " + eventBuilding + " | Room: " + eventRoom +
                " | Date: " + eventDate + " | Time: " + eventStartTime +
                " | Organiser: " + eventOrganiser + "\nDetails: " + eventDetails +
                "\nParticipants: " + participants.size();
    }
}
