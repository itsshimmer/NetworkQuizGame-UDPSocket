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
    static boolean invalidDataDetected;

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

    private static void awaitAck() throws Exception {
        String receivedData;
        receivedData = receivePacketWithTimeout(3);
        if (!receivedData.equals("ack")) {
            invalidDataDetected = true;
        }
    }

    private static int getIntegerValue(String string, int min, int max) {
        int value;
        try {
            value = Integer.parseInt(string);
        } catch (Exception e) {
            invalidDataDetected = true;
            return -1;
        }
        if (value < min || value > max) {
            invalidDataDetected = true;
            return -1;
        }
        return value;
    }

    private static String shuffleAlternatives(Question question) {
        // MARK: - TO DO
        String questionString = question.question + ";" + question.correctAnswer + ";" + question.alternative1 + ";"
                + question.alternative2 + ";" + question.alternative3;
        return questionString;
    }

    public static void main(String[] args) throws Exception {

        // Reads csv and builds the questions array
        buildQuestionsArray();

        // Server run loop
        while (true) {
            // Creates the server socket at the chosen port
            serverSocket = new DatagramSocket(serverPort);
            invalidDataDetected = false;
            // Waits for a valid connection
            System.out.println("Waiting for a valid connection");
            validateConnection();
            // After this point the connection is validated, if a unknown response is
            // received, the validation process must happen again

            int difficulty;

            // Gets difficulty
            System.out.println("Waiting for difficulty selection 1-3");
            lastReceivedContent = receivePacketNoTimeout(1);
            System.out.println("Received a package");
            difficulty = getIntegerValue(lastReceivedContent, 1, 3);

            if (invalidDataDetected) {
                System.out.println("Invalid data detected, disconnecting...");
                serverSocket.close();
                continue;
            }
            sendAck();

            // Loop for fetching questions
            int userPoints = 0;
            boolean shouldKeepReceiving = true;
            while (shouldKeepReceiving) {
                System.out.println("Waiting for question or ending request");
                lastReceivedContent = receivePacketNoTimeout(1);
                System.out.println("Received a package");
                switch (lastReceivedContent) {
                    case "q":
                        sendAck();
                        Question question = questions.get(generator.nextInt(questions.size()));
                        String questionString = shuffleAlternatives(question);
                        lastSentData = questionString;
                        sendPacket();
                        awaitAck();
                        System.out.println("Waiting for user response selection 1 - diffculty+1");
                        lastReceivedContent = receivePacketNoTimeout(1);
                        System.out.println("Received a package" + lastReceivedContent);
                        int userChosen = getIntegerValue(lastReceivedContent, 1, difficulty + 1);

                        if (invalidDataDetected) {
                            System.out.println("Invalid data detected, disconnecting...");
                            shouldKeepReceiving = false;
                            serverSocket.close();
                            continue;
                        }
                        sendAck();
                        lastSentData = "f";
                        // MARK: - TO DO Validate response
                        if (userChosen == 1) {
                            userPoints++;
                            lastSentData = "t";
                        }
                        sendPacket();
                        awaitAck();
                        break;
                    case "e":
                        sendAck();
                        shouldKeepReceiving = false;
                        break;
                    default:
                        System.out.println("Invalid data detected, disconnecting...");
                        invalidDataDetected = true;
                        shouldKeepReceiving = false;
                        break;
                }
            }
            lastSentData = "" + userPoints;
            sendPacket();
            awaitAck();
            System.out.println("Finished match. Ending cycle...");
            serverSocket.close();
        }
    }
}