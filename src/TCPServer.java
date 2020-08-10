import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

public class TCPServer {

    public final String stationName;
    public final int tcpPort;
    private UDPServer udpServer;
    private ServerSocket serverSocket;
    private StationTimetable stationTimetable;
    public HashMap<String, Socket> requests;


    public TCPServer(String stationName, int tcpPort, int udpPort, ArrayList<Integer> neighbours, StationTimetable stationTimetable) throws IOException {
        this.stationName = stationName;
        this.tcpPort = tcpPort;
        this.stationTimetable = stationTimetable;

        try {
            this.udpServer = new UDPServer(udpPort, neighbours, stationTimetable, this);
        }
        catch (Exception ex){
            serverSocket.close();
        }

    }

    /**
     * Start listening fot TCP
     * @throws IOException
     */
    public void listenTCP() throws IOException {

        int requestCount= 0;


        serverSocket = new ServerSocket(tcpPort);
        requests = new HashMap<>();

        System.out.println("JAVA: LISTENING TCP ON " + tcpPort + "\n");

        while (true){

            String requestId = stationName + requestCount;

            Socket client = serverSocket.accept();
            System.out.println("FOUND CLIENT" + " ON TCP=" +tcpPort + "\n");
            stationTimetable.updateTimetable();

            requests.put(requestId, client);
            System.out.println("HANDLING REQUEST" + " ON TCP=" + tcpPort + "\n");
            Thread thread = new Thread(new TCPRequestHandler(requestId, client, udpServer, this));
            thread.start();

            requestCount++;
        }


    }

    /**
     * When a request is completed in the UDPServer it will call into this method to handle the HTTP request
     * @param requestId
     * @param result The calculated path to the station
     * @param responseType 0 if current station is adjacent to destination, 1 if not
     * @throws IOException
     */
    public void onRequestComplete(String requestId, ArrayList<String> result, int responseType) throws IOException {



        Socket client = requests.get(requestId);
        String destination = result.get(0).split(",")[4];

        StringBuilder httpResult = new StringBuilder();
        httpResult.append("HTTP/1.1 200 OK\r\n\r\n");

        httpResult.append("<html><body>");
        httpResult.append("<h1>From " + stationName + "</h1>");

        if(responseType == 0){
            for(String j: result){
                httpResult.append("<h3>Option </h3>");
                httpResult.append(j).append("<br>");
            }
        }
        if (responseType == 1){


            for (int i = result.size() - 1; i >= 0 ; i--) {
                httpResult.append(result.get(i)).append("<br>");
                if(i > 0){
                    httpResult.append("+ ");
                }

            }

        }

        httpResult.append("<h1>Arrive at " + destination + "</h1>");
        httpResult.append("</body></html>");

        PrintWriter outputStream = new PrintWriter(client.getOutputStream());
        outputStream.print(httpResult.toString());
        outputStream.close();
        client.close();

        //Why isnt this working
        requests.remove(requestId);



    }





}
