import java.io.IOException;
import java.net.*;
import java.util.ArrayList;

public class UDPServer {

    public final int udpPort;
    public final ArrayList<Integer> neighbours;
    public final StationTimetable timetable;
    public final TCPServer tcpServer;
    private DatagramSocket socket;

    public ArrayList<String> seenUnresolved;
    public ArrayList<String> seenResolved;

    public UDPServer(int udpPort, ArrayList<Integer> neighbours, StationTimetable timetable, TCPServer tcpServer) throws IOException {
        this.udpPort = udpPort;
        this.neighbours = neighbours;
        this.timetable = timetable;
        this.tcpServer = tcpServer;

        seenResolved = new ArrayList<>();
        seenUnresolved = new ArrayList<>();

        Thread thread = new Thread(this::listenUDP);
        thread.start();
    }

    /**
     * Listen for UDP requests and handle each request on a new thread
     */
    public void listenUDP() {

        System.out.println("JAVA:LISTENING UDP ON " + udpPort);
        System.out.println();

        try {
            socket = new DatagramSocket(udpPort);

            while (true){
                byte[] buffer = new byte[2048];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                System.out.println("RECEIVED DATAGRAM ON UDP " + udpPort + "\n" + new String(packet.getData()) + "\n");

                Thread worker = new Thread(new UDPHandler(new String(packet.getData()), this));
                worker.start();
            }
        }
        catch (IOException ex){
            System.out.println("Error occurred in UDP");

        }




    }

    /**
     * Attempt to find the destination via UDP. If the destination is adjacent to the current station no UDP is performed
     * @param destination The destination station
     * @param requestId The unique requestId
     * @throws IOException
     */
    public void getDestination(String destination, String requestId) throws IOException {

        //No UDP is required because we are already connected to the destination
        ArrayList<String> journeys = timetable.getAllJourneysForDestination(destination);
        if(journeys.size() > 0){
            System.out.println("JAVA: NO UDP REQUIRED FOR " + destination + "\n");
            tcpServer.onRequestComplete(requestId, journeys, 0);
        }
        //UDP is required to find a path to the station
        //Create a datagram and send to neighbours
        else {
            System.out.println("JAVA: UDP STARTING AT " + udpPort + "\n");
            System.out.println();
            String datagram = StationJava.createDatagram(requestId, udpPort, tcpServer.stationName, destination);
            for (int neighbour:neighbours){
                System.out.println("JAVA: SENDING DATAGRAM FROM " + udpPort + " TO " + neighbour + "\n");
                sendUDP(datagram, neighbour);
            }

        }

    }

    /**
     * Send the Datagram via UDP to a port
     * @param datagram The unique datagram for the request
     * @param neighbour The port we are sending to
     * @throws IOException
     */
    public void sendUDP(String datagram, int neighbour) throws IOException {

        byte[] buffer = datagram.getBytes();

        //Send to "localhost"
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length, InetAddress.getLocalHost(), neighbour);
        socket.send(packet);
    }
}
