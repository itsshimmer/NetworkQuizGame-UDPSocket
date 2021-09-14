import java.net.*;
import java.io.*;

public class server {

    static final int serverPort = 9877;
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

    private static String receivePacket() throws Exception {
        boolean received = false;
        byte[] receiveData = new byte[1];
        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

        while (!received) {
            try {
                serverSocket.receive(receivePacket);
            } catch (SocketTimeoutException e) {
                sendPacket();
                continue;
            }
        }
        return new String(receivePacket.getData());
    }

    public static void main(String[] args) throws Exception {
        // Path fileName = Path.of("/Users/joaobrentano/Documents/transfer.txt");
        // // Path fileName = Path.of("D://Eduardo//Documents//transfer.txt");
        // String data = Files.readString(fileName);

        // Creates the server socket at the port 9877
        serverSocket = new DatagramSocket(serverPort);

        while (true) {

            // while para validar a conexao
            while (true) {
                byte[] receiveData = new byte[1];
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                serverSocket.receive(receivePacket);
                String content = new String(receivePacket.getData());
                if(content.equals("c")) {
                    clientIpAddress = receivePacket.getAddress();
                    clientPort = receivePacket.getPort();
                    serverSocket.setSoTimeout(timeout);
                    lastSentData = "k";
                    sendPacket();
                    break;
                }
            }

            // should await start game
            break;
        }
    }
}
