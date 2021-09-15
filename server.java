import java.net.*;
import java.io.*;

public class server {

    static final int serverPort = 9876;
    static final int timeout = 5000;
    static DatagramSocket serverSocket;

    static InetAddress clientIpAddress;
    static int clientPort;

    static String lastSentData;

    private static void sendPacket() throws Exception {
        byte[] sendData = lastSentData.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIpAddress, clientPort);
        serverSocket.send(sendPacket);
    }

    private static String receivePacketWithTimeout() throws Exception {
        serverSocket.setSoTimeout(timeout);
        byte[] receiveData = new byte[1];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while (true) {
            try {
                serverSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                sendPacket();
                continue;
            }
            break;
        }
        return new String(receivePacket.getData());
    }

    private static String receivePacketNoTimeout() throws Exception {
        serverSocket.setSoTimeout(0);
        byte[] receiveData = new byte[1];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        return new String(receivePacket.getData());
    }

    private static String receivePacketNoTimeoutUpdateClient() throws Exception {
        serverSocket.setSoTimeout(0);
        byte[] receiveData = new byte[1];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        clientIpAddress = receivePacket.getAddress();
        clientPort = receivePacket.getPort();
        return new String(receivePacket.getData());
    }

    public static void main(String[] args) throws Exception {
        // Path fileName = Path.of("/Users/joaobrentano/Documents/transfer.txt");
        // // Path fileName = Path.of("D://Eduardo//Documents//transfer.txt");
        // String data = Files.readString(fileName);

        // Creates the server socket at the port 9877
        serverSocket = new DatagramSocket(serverPort);
        String content;

        while (true) {
            // While to validate the connection
            while (true) {
                content = receivePacketNoTimeoutUpdateClient();
                if(content.equals("c")) {
                    lastSentData = "k";
                    sendPacket();
                    content = receivePacketWithTimeout();
                    if(content.equals("k")) {
                        System.out.println("Connection validated successfully!");
                        break;
                    }
                }
            }
            // Awaits for game start packet
            content = receivePacketNoTimeout();
            if(content.equals("startGame")) {
                // Starts game
            }
            break;
        }
    }
}
