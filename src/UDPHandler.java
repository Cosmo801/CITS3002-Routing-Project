import java.io.IOException;
import java.util.ArrayList;

public class UDPHandler implements Runnable {


    private final String data;
    private final UDPServer udpServer;


    public UDPHandler(String data, UDPServer udpServer) {
        this.data = data;
        this.udpServer = udpServer;


    }

    //Handle UDP
    private void handleUDP() throws IOException {

        System.out.println("Arrived in worker thread");
        String[] split = data.split("\n");

        //Read the datagram
        String requestId = split[0].split("=")[1];
        int resolved = Integer.parseInt(split[1].split("=")[1]);
        int originalSenderPort = Integer.parseInt(split[4].split("=")[1]);
        String currentSenderName = split[2].split("=")[1];
        int currentSenderPort = Integer.parseInt(split[3].split("=")[1]);
        String destination = split[6].split("=")[1];
        int numJourneys = Integer.parseInt(split[7].split("=")[1]);

        //If the request is resolved, ie the destination has been found
        if(resolved == 1){
            //Only accept the first route, multiple routes may be discovered
            //Also stops circular layouts from continuing infinitely
            if(udpServer.seenResolved.contains(requestId)) return;
            udpServer.seenResolved.add(requestId);

            //If we have arrived back at the original sender we can finally send the Trips back as a result
            if(udpServer.udpPort == originalSenderPort){

                String toJourney;

                String time = (split[7 + numJourneys].split(",")[3]).substring(0, 5);
                String lastJourneyBefore = udpServer.timetable.getJourneyBefore(currentSenderName, time);
                if(lastJourneyBefore == null){
                    toJourney = udpServer.timetable.getAllJourneysForDestination(currentSenderName).get(0);
                }
                else {
                    toJourney = lastJourneyBefore;
                }


                ArrayList<String> allJourneys = new ArrayList<>();

                for (int i = 8; i < split.length -1; i++) {
                    allJourneys.add(split[i]);
                }
                allJourneys.add(toJourney);
                udpServer.tcpServer.onRequestComplete(requestId, allJourneys, 1);
            }
            //The destination has been found but we are not back at the original sender
            //Append a trip to the current sender to build the path
            //Send to neighbours so that the datagram can travel back to the original sender
            else {

                String time = (split[7 + numJourneys].split(",")[3]).substring(0, 5);
                String lastJourneyBefore = udpServer.timetable.getJourneyBefore(currentSenderName, time);
                String journey;
                if(lastJourneyBefore == null){
                    journey = udpServer.timetable.getAllJourneysForDestination(currentSenderName).get(0);
                }
                else {
                    journey = lastJourneyBefore;
                }

                String updatedGram = StationJava.appendJourney(data, journey);
                String updatedGram2 = StationJava.appendSenders(updatedGram, udpServer.udpPort, udpServer.tcpServer.stationName, 1);
                for (Integer neighbour:udpServer.neighbours){
                    //Do not send back to previous sender it is redundant
                    if(currentSenderPort == neighbour) continue;
                    udpServer.sendUDP(updatedGram2, neighbour);
                    return;
                }


            }
        }
        //If the request has not been resolved we need to keep looking
        if(resolved == 0){
            //If the request has already been seen, ignore it so an infinite loop does not occur
            if(udpServer.udpPort == originalSenderPort) return;
            if(udpServer.seenUnresolved.contains(requestId)) {
                return;
            }

            //Request has not been seen
            else {
                udpServer.seenUnresolved.add(requestId);
                ArrayList<String> toJourneys = udpServer.timetable.getAllJourneysForDestination(destination);
                //If the current station knows the path to the destination
                //Mark as complete and append the trip
                //Send back to previous sender
                //Append self as current sender so that the previous sender knows the path
                if(toJourneys.size() > 0){
                    String updatedGram = StationJava.appendJourney(data, toJourneys.get(toJourneys.size() - 1));
                    String updatedGram2 = StationJava.appendSenders(updatedGram, udpServer.udpPort, udpServer.tcpServer.stationName, 1);
                    udpServer.seenResolved.add(requestId);
                    udpServer.sendUDP(updatedGram2, currentSenderPort);
                    return;
                }
                //We still havent found the station, keep broadcasting to neighbours
                else {
                    String updatedGram = StationJava.appendSenders(data, udpServer.udpPort, udpServer.tcpServer.stationName, 0);
                    System.out.println(udpServer.neighbours.size());

                    for (int i = 0; i < udpServer.neighbours.size(); i++) {
                        if(udpServer.neighbours.get(i) == currentSenderPort) continue;
                        udpServer.sendUDP(updatedGram, udpServer.neighbours.get(i));

                    }


                }
            }



        }


    }

    @Override
    public void run() {
        try {
            handleUDP();
        } catch (IOException e) {
            System.out.println("Here");
            e.printStackTrace();
        }

    }
}
