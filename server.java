import java.net.*;
import java.io.*;

public class server {

    static final int serverPort = 9876;
    static final int timeout = 5000;
    static DatagramSocket serverSocket;

    static InetAddress clientIpAddress;
    static int clientPort;

    static String lastSentData;
    static String lastReceivedContent;

    private static void sendPacket() throws Exception {
        byte[] sendData = lastSentData.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientIpAddress, clientPort);
        serverSocket.send(sendPacket);
    }

    private static String receivePacketWithTimeout(int bytes) throws Exception {
        serverSocket.setSoTimeout(timeout);
        byte[] receiveData = new byte[bytes];
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

    private static String receivePacketNoTimeout(int bytes) throws Exception {
        serverSocket.setSoTimeout(0);
        byte[] receiveData = new byte[bytes];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        return new String(receivePacket.getData());
    }

    private static String receivePacketNoTimeoutUpdateClient(int bytes) throws Exception {
        serverSocket.setSoTimeout(0);
        byte[] receiveData = new byte[bytes];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
        serverSocket.receive(receivePacket);
        clientIpAddress = receivePacket.getAddress();
        clientPort = receivePacket.getPort();
        return new String(receivePacket.getData());
    }

    private static void validateConnection() throws Exception {
        while (true) {
            lastReceivedContent = receivePacketNoTimeoutUpdateClient(7);
            if(lastReceivedContent.equals("connect")) {
                lastSentData = "ack";
                sendPacket();
                lastReceivedContent = receivePacketWithTimeout(3);
                if(lastReceivedContent.equals("ack")) {
                    System.out.println("Connection validated successfully!");
                    break;
                }
            }
        }
    }
    public static void main(String[] args) throws Exception {
        // Path fileName = Path.of("/Users/joaobrentano/Documents/transfer.txt");
        // // Path fileName = Path.of("D://Eduardo//Documents//transfer.txt");
        // String data = Files.readString(fileName);

        // Creates the server socket at the chosen port 
        serverSocket = new DatagramSocket(serverPort);

        while (true) {
            // While to validate the connection
            validateConnection();
            // Awaits for game start packet
            lastReceivedContent = receivePacketNoTimeout(9);
            if(lastReceivedContent.equals("startGame")) {
                // Starts game
            }
            break;
        }
    }
}
