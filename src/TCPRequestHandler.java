import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TCPRequestHandler implements Runnable {

    private final UDPServer udpServer;
    private final String requestId;
    private final Socket connection;
    private final TCPServer tcpServer;


    public TCPRequestHandler(String requestId, Socket connection, UDPServer udpServer, TCPServer tcpServer){
        this.requestId = requestId;
        this.connection = connection;
        this.udpServer = udpServer;
        this.tcpServer = tcpServer;
    }

    //Handle the incoming TCP request
    private void handleRequest() throws IOException {

        BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder header = new StringBuilder();
        String currentLine;

        while (true){
            currentLine = inputStream.readLine();
            if(currentLine == null || currentLine.equals("")) break;
            header.append(currentLine).append("\n");
        }

        //Check the query string ie /?to=East_Station
        //If there is no query string ignore
        String[] headerLines = header.toString().split("\n", -1);
        String requestLine = headerLines[0];
        if(!requestLine.contains("/?to=")){
            connection.close();
            System.out.println("IGNORING REQUEST ON TCP " + tcpServer.tcpPort);
            System.out.println();
            return;
        }

        String[] split = requestLine.split(" ");
        String targetStation = split[1].split("=")[1];

        //If the HTTP request is for the current station we ignore the request
        if(targetStation.equals(tcpServer.stationName)){
            String httpResult = "HTTP/1.1 200 OK\r\n\r\n";
            httpResult += "Already at station";
            PrintWriter outputStream = new PrintWriter(connection.getOutputStream());
            outputStream.print(httpResult);
            outputStream.close();
            connection.close();
            return;

        }

        System.out.println("REQUEST FOR " + targetStation + " ON TCP " + tcpServer.tcpPort + "\n");
        //Start UDP Communication
        udpServer.getDestination(targetStation, requestId);
    }


    @Override
    public void run() {

        try {
            handleRequest();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
