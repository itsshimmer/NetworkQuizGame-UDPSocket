import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class client {

    static final String host = "localhost";
    static final int serverPort = 9876;
    static final int timeout = 5000;
    
    static DatagramSocket clientSocket;
    static InetAddress ipAddress;

    static String lastSentData;

    private static void sendPacket() throws Exception {
        byte[] sendData = lastSentData.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, serverPort);
        clientSocket.send(sendPacket);
    }

    private static String receivePacket(int bytes) throws Exception {
        byte[] receiveData = new byte[bytes];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        // while que controla o recebimento de resposta
        while(true) {
            try {
                clientSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
               sendPacket();
               continue;
            }
            break;
        }
        return new String(receivePacket.getData());
    }

    private static void validateConnection() throws Exception {
        while(true) {
            lastSentData = "connect";
            sendPacket();
            String response = receivePacket(3);
            if(response.equals("ack")) {
                lastSentData = "ack";
                sendPacket();
                System.out.println("Connection validated successfully!");
                break;
            }
        }
    }
    public static void main(String[] args) throws Exception {

        // declara socket cliente
        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(timeout);
        ipAddress = InetAddress.getByName(host);
        
        while (true) {
            validateConnection();
            // Send start game packet
            break;

        }

        clientSocket.close();
    }
}
