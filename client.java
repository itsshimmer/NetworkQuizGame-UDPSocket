import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

public class client {

    static final String host = "localhost";
    static final int serverPort = 9877;
    static final int timeout = 5000;
    
    static DatagramSocket clientSocket;
    static InetAddress ipAddress;

    static String lastSentData;

    private static void sendPacket() throws Exception {
        byte[] sendData = lastSentData.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, serverPort);
        clientSocket.send(sendPacket);
    }

    private static String receivePacket() throws Exception {
        boolean received = false;
        byte[] receiveData = new byte[15000];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while(!received) {
            try {
                clientSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
               sendPacket();
               continue;
            }
        }
        return new String(receivePacket.getData());
    }
    public static void main(String[] args) throws Exception {

        // declara socket cliente
        clientSocket = new DatagramSocket();
        clientSocket.setSoTimeout(timeout);
        ipAddress = InetAddress.getByName(host);
        
        while (true) {

            // while para validar a conexao
            while(true) {
                lastSentData = "c";
                sendPacket();
                String response = receivePacket();
                System.out.println("got response:" + response);
                if(response.equals("k")) {
                    break;
                }
            }
            System.out.println("Conectado!");
            //send start game packet
            break;

        }

        clientSocket.close();
    }
}
