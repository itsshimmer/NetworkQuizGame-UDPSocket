import java.net.*;
import java.io.*;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class Server {

    static final int serverPort = 9876;
    static final int timeout = 5000;
    static final Random generator = new Random();

    static DatagramSocket serverSocket;
    static InetAddress clientIpAddress;
    static int clientPort;

    static String lastSentData;
    static String lastReceivedContent;

    static List<Question> questions;

    private static class Question {

        String question;
        String correctAnswer;
        String alternative1;
        String alternative2;
        String alternative3;

        public Question(String question, String correctAnswer, String alternative1, String alternative2,
                String alternative3) {
            this.question = question;
            this.correctAnswer = correctAnswer;
            this.alternative1 = alternative1;
            this.alternative2 = alternative2;
            this.alternative3 = alternative3;
        }

    }

    private static void buildQuestionsArray() throws IOException {
        questions = new ArrayList<>();
        Path file = Path.of("questions.csv");
        List<String> lines = Files.readAllLines(file);
        for (String string : lines) {
            String[] values = string.split(";");
            Question question = new Question(values[0], values[1], values[2], values[3], values[4]);
            questions.add(question);
        }
    }

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
            if (lastReceivedContent.equals("connect")) {
                lastSentData = "ack1";
                sendPacket();
                lastReceivedContent = receivePacketWithTimeout(4);
                if (lastReceivedContent.equals("ack2")) {
                    System.out.println("Connection validated successfully!");
                    break;
                }
            }
        }
    }

    private static void sendAck() throws Exception {
        lastSentData = "ack";
        sendPacket();
    }

    private static boolean isValidInteger(int value, int min, int max) {
        if(value < min || value > max) {
            return false;
        }
        return true;
    }

    public static void main(String[] args) throws Exception {

        // Reads csv and builds the questions array
        buildQuestionsArray();

        // Server run loop
        while (true) {
            // Creates the server socket at the chosen port
            serverSocket = new DatagramSocket(serverPort);
            // Waits for a valid connection
            System.out.println("Waiting for a valid connection");
            validateConnection();
            // After this point the connection is validated, if a unknown response is
            // received, the validation process must happen again

            // Starting game
            boolean validResponse = true;
            while (validResponse) {
                System.out.println("Waiting for difficulty selection 1-3");
                lastReceivedContent = receivePacketNoTimeout(1);
                System.out.println("Received a package");
                switch (lastReceivedContent) {
                    case "1":
                        System.out.println("Starting easy mode");
                        sendAck();
                        System.out.println("Waiting for amount of questions 1-9");
                        lastReceivedContent = receivePacketNoTimeout(1);
                        int numberOfQuestions;
                        try {

                        }
                        if(!isValidInteger(Integer.parseInt(lastReceivedContent), 1, 9)) {
                            System.out.println("Invalid response detected, ending connection...");
                            validResponse = false;
                            break;
                        }
                        

                        break;

                    case "2":
                        System.out.println("Starting normal mode");
                        sendAck();
                        break;

                    case "3":
                        System.out.println("Starting hard mode");
                        sendAck();
                        break;

                    default:
                        System.out.println("Invalid response detected, ending connection...");
                        validResponse = false;
                        break;
                }
            }
            serverSocket.close();
        }
    }
}
