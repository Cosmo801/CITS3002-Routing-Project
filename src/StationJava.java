import java.io.IOException;
import java.util.ArrayList;

//By William Knight 21722128

public class StationJava {


    public static void main(String[] args) throws IOException {


 
        if(args.length < 4){
            System.out.println("Not enough arguments to start station server");
            return;
        }

        String stationName = args[0];
        int tcpPort = Integer.parseInt(args[1]);
        int udpPort = Integer.parseInt(args[2]);
        ArrayList<Integer> neighbours = new ArrayList<>();
        StationTimetable timetable;

        try {
            timetable = new StationTimetable(stationName);
        }
        catch (Exception ex){
            System.out.println("Could not read timetable file");
             return;
        }


        for (int i = 3; i <args.length ; i++) {

            try {
                neighbours.add(Integer.parseInt(args[i]));

            }
            catch (Exception ex){
                continue;
            }

        }

        if(neighbours.size() < 1) {
            System.out.println("Station has no neighbours");
        }

        System.out.println(neighbours);

        try {
            TCPServer server = new TCPServer(stationName, tcpPort, udpPort, neighbours, timetable);
            server.listenTCP();
        }
        catch (IOException ex){
            System.out.println("Error occured in station " + stationName);
        }


    }

    /**
     * Helper method for creating a datagram
     * @param requestId RequestID of the datagram
     * @param originalSenderPort Original sender port (ie the station that received the request)
     * @param originalSenderName Original sender station name
     * @param destination The destination station
     * @return
     */
    public static String createDatagram(String requestId, int originalSenderPort, String originalSenderName, String destination){
        return  "REQUESTID=" + requestId + "\n" +
                "RESOLVED=0\n" +
                "CURRENTSENDERNAME=" + originalSenderName + "\n"+
                "CURRENTSENDERPORT=" + originalSenderPort+ "\n"+
                "ORIGINALSENDERPORT=" + originalSenderPort + "\n"+
                "ORIGINALSENDERNAME=" + originalSenderName + "\n"+
                "DESTINATION=" + destination + "\n" +
                "JOURNEYCOUNT=0\n" +
                "END:";
    }

    public static String appendSenders(String datagram, int currentSenderPort, String currentSenderName, int complete){
        String[] split = datagram.split("\n");
        StringBuilder resultBuilder = new StringBuilder();

        for (int i = 0; i < split.length - 1; i++) {

            if(i == 1){
                resultBuilder.append("RESOLVED=").append(complete).append("\n");
                continue;
            }
            if(i == 2){
                resultBuilder.append("CURRENTSENDERNAME=").append(currentSenderName).append("\n");
                continue;
            }
            if (i == 3){
                resultBuilder.append("CURRENTSENDERPORT=").append(currentSenderPort).append("\n");
                continue;
            }


            resultBuilder.append(split[i]).append("\n");

        }
        resultBuilder.append("END:");
        return  resultBuilder.toString();
    }

    public static String appendJourney(String datagram, String journey){
        String[] split = datagram.split("\n");
        StringBuilder resultBuilder = new StringBuilder();

        for (int i = 0; i < 7; i++) {
            resultBuilder.append(split[i]).append("\n");
        }

        int journeyCount = Integer.parseInt(split[7].split("=")[1]);
        resultBuilder.append("JOURNEYCOUNT=").append(journeyCount + 1).append("\n");

        for (int i = 8; i < 8 + journeyCount; i++) {
            resultBuilder.append(split[i]).append("\n");
        }
        resultBuilder.append(journey).append("\n");
        resultBuilder.append("END:");

        return resultBuilder.toString();
    }




}
