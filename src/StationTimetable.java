import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * Class that tracks a station timetable text file
 */
public class StationTimetable {

    private final String stationName;
    private ArrayList<String> stationTimetable;
    private long currentModTime;


    public StationTimetable(String stationName) throws IOException {
        this.stationName = stationName;
        stationTimetable = new ArrayList<>();

        readTimetableFile();
    }

    public ArrayList<String> getAllJourneys(){
        return  stationTimetable;
    }

 //Check if the timetable file needs to be updates
    public void updateTimetable() throws IOException {

        String timetableFileName = "tt-" + stationName;

        File file = new File("Timetables/" + timetableFileName);
        if(file.lastModified() > currentModTime){
            
            readTimetableFile();
        }
        

    }

    /**
     * Get all trips from the current station to the destination station
     * @param destination
     * @return
     */
    public ArrayList<String> getAllJourneysForDestination(String destination){

        ArrayList<String> result = new ArrayList<>();


        for (String s: stationTimetable){

            String[] split = s.split(",");
            String station = split[4];

            if(station.equals(destination)){
                result.add(s);
            }

        }
        return result;

    }

    public String getLatestJourneyTo(String destination){
        for (int i = stationTimetable.size() -1; i >= 0 ; i--) {

            String journeyDest = stationTimetable.get(i).split(",")[4];
            if(journeyDest.equals(destination)) return stationTimetable.get(i);

        }
        return null;
    }

    public String getJourneyBefore(String destination, String timeString){
        String[] timeSplit = timeString.split(":");
        int hours = Integer.parseInt(timeSplit[0]);
        int minutes = Integer.parseInt(timeSplit[1]);
        String currentJourney = null;

        for (String j: getAllJourneysForDestination(destination)
             ) {
            String[] tSplit = j.split(",")[3].split(":");

            int jHours = Integer.parseInt(tSplit[0]);
            int jMinutes = Integer.parseInt(tSplit[1]);
            
            if(jHours > hours) break;
            if(jHours == hours && jMinutes > minutes) break;

            currentJourney = j;
        }


        return currentJourney;

    }

    private void readTimetableFile() throws IOException {

        String timetableFileName = "tt-" + stationName;



        File file = new File("Timetables/" + timetableFileName);
        currentModTime = file.lastModified();

        BufferedReader reader = new BufferedReader(new FileReader(file));
        String currentLine = reader.readLine();


        while ((currentLine = reader.readLine()) != null) {
            stationTimetable.add(currentLine);
        }

    }
}
