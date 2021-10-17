import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Server {

	static final int clientOneServerPort = 9876;
	static final int clientTwoServerPort = 9877;
	static final int timeout = 5000;
	static final Random generator = new Random();

	static DatagramSocket clientOneServerSocket;
	static DatagramSocket clientTwoServerSocket;
	static InetAddress clientOneIpAddress;
	static InetAddress clientTwoIpAddress;
	static int clientOnePort;
	static int clientTwoPort;

	static String clientOneLastSentData;
	static String clientTwoLastSentData;
	static String clientOneLastReceivedContent;
	static String clientTwoLastReceivedContent;
	static boolean clientOneInvalidDataDetected;
	static boolean clientTwoInvalidDataDetected;

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

	private static String shuffleAlternatives(Question question) {
		// MARK: - TO DO
		String questionString = question.question + ";" + question.correctAnswer + ";" + question.alternative1 + ";"
				+ question.alternative2 + ";" + question.alternative3;
		return questionString;
	}

	private static int clientOneGetIntegerValue(String string, int min, int max) {
		int value;
		try {
			value = Integer.parseInt(string);
		} catch (Exception e) {
			clientOneInvalidDataDetected = true;
			return -1;
		}
		if (value < min || value > max) {
			clientOneInvalidDataDetected = true;
			return -1;
		}
		return value;
	}

	private static int clientTwoGetIntegerValue(String string, int min, int max) {
		int value;
		try {
			value = Integer.parseInt(string);
		} catch (Exception e) {
			clientTwoInvalidDataDetected = true;
			return -1;
		}
		if (value < min || value > max) {
			clientTwoInvalidDataDetected = true;
			return -1;
		}
		return value;
	}

	private static void clientOneSendPacket() throws Exception {
		byte[] sendData = clientOneLastSentData.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientOneIpAddress, clientOnePort);
		clientOneServerSocket.send(sendPacket);
	}

	private static void clientTwoSendPacket() throws Exception {
		byte[] sendData = clientTwoLastSentData.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, clientTwoIpAddress, clientTwoPort);
		clientTwoServerSocket.send(sendPacket);
	}

	private static String clientOneReceivePacketWithTimeout(int bytes) throws Exception {
		clientOneServerSocket.setSoTimeout(timeout);
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		while (true) {
			try {
				clientOneServerSocket.receive(receivePacket);
			} catch (SocketTimeoutException e) {
				clientOneSendPacket();
				continue;
			}
			break;
		}
		return new String(receivePacket.getData());
	}

	private static String clientTwoReceivePacketWithTimeout(int bytes) throws Exception {
		clientTwoServerSocket.setSoTimeout(timeout);
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		while (true) {
			try {
				clientTwoServerSocket.receive(receivePacket);
			} catch (SocketTimeoutException e) {
				clientTwoSendPacket();
				continue;
			}
			break;
		}
		return new String(receivePacket.getData());
	}

	private static String clientOneReceivePacketNoTimeout(int bytes) throws Exception {
		clientOneServerSocket.setSoTimeout(0);
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientOneServerSocket.receive(receivePacket);
		return new String(receivePacket.getData());
	}

	private static String clientTwoReceivePacketNoTimeout(int bytes) throws Exception {
		clientTwoServerSocket.setSoTimeout(0);
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientTwoServerSocket.receive(receivePacket);
		return new String(receivePacket.getData());
	}

	private static String clientOneReceivePacketNoTimeoutUpdateClient(int bytes) throws Exception {
		clientOneServerSocket.setSoTimeout(0);
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientOneServerSocket.receive(receivePacket);
		clientOneIpAddress = receivePacket.getAddress();
		clientOnePort = receivePacket.getPort();
		return new String(receivePacket.getData());
	}

	private static String clientTwoReceivePacketNoTimeoutUpdateClient(int bytes) throws Exception {
		clientTwoServerSocket.setSoTimeout(0);
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
		clientTwoServerSocket.receive(receivePacket);
		clientTwoIpAddress = receivePacket.getAddress();
		clientTwoPort = receivePacket.getPort();
		return new String(receivePacket.getData());
	}

	private static void clientOneSendAck() throws Exception {
		clientOneLastSentData = "ack";
		clientOneSendPacket();
	}

	private static void clientTwoSendAck() throws Exception {
		clientTwoLastSentData = "ack";
		clientTwoSendPacket();
	}

	private static void clientOneAwaitAck() throws Exception {
		String receivedData;

		// Tries to receive ACK correctly
		while (true) {
			receivedData = clientOneReceivePacketWithTimeout(3);
			if (receivedData.equals("ack")) {
				break;
			}
			System.out.println("Invalid data received, retrying to receive ack...");
		}
	}

	private static void clientTwoAwaitAck() throws Exception {
		String receivedData;

		// Tries to receive ACK correctly
		while (true) {
			receivedData = clientTwoReceivePacketWithTimeout(3);
			if (receivedData.equals("ack")) {
				break;
			}
			System.out.println("Invalid data received, retrying to receive ack...");
		}
	}

	private static void clientOneValidateConnection() throws Exception {
		while (true) {
			clientOneLastReceivedContent = clientOneReceivePacketNoTimeoutUpdateClient(7);
			if (clientOneLastReceivedContent.equals("connect")) {
				clientOneLastSentData = "ack1";
				clientOneSendPacket();
				clientOneLastReceivedContent = clientOneReceivePacketWithTimeout(4);
				if (clientOneLastReceivedContent.equals("ack2")) {
					System.out.println("Connection validated successfully!");
					break;
				}
			}
		}
	}

	private static void clientTwoValidateConnection() throws Exception {
		while (true) {
			clientTwoLastReceivedContent = clientTwoReceivePacketNoTimeoutUpdateClient(7);
			if (clientTwoLastReceivedContent.equals("connect")) {
				clientTwoLastSentData = "ack1";
				clientTwoSendPacket();
				clientTwoLastReceivedContent = clientTwoReceivePacketWithTimeout(4);
				if (clientTwoLastReceivedContent.equals("ack2")) {
					System.out.println("Connection validated successfully!");
					break;
				}
			}
		}
	}
	public static void main(String[] args) throws Exception {

		// Reads csv and builds the questions array
		buildQuestionsArray();

		// Server main loop
		while (true) {

			// Setting up
			int difficulty;
			int clientOnePoints = 0;
			int clientTwoPoints = 0;
			int userChosen;
			clientOneInvalidDataDetected = false;
			clientTwoInvalidDataDetected = false;

			// Creates the server socket at the chosen port
			clientOneServerSocket = new DatagramSocket(clientOneServerPort);
			clientTwoServerSocket = new DatagramSocket(clientTwoServerPort);

			// Waits for the first client connection
			System.out.println("Waiting for client one connection");
			clientOneValidateConnection();

			// Waits for the second client connection
			System.out.println("Waiting for client two connection");
			clientTwoValidateConnection();

			// Gets difficulty
			System.out.println("Waiting for difficulty selection 1-3");

			// Tries to receive the difficulty data correctly
			while (true) {
				clientOneLastReceivedContent = clientOneReceivePacketNoTimeout(1);
				difficulty = clientOneGetIntegerValue(clientOneLastReceivedContent, 1, 3);
				if (!clientOneInvalidDataDetected) {
					clientOneSendAck();
					break;
				}
				clientOneInvalidDataDetected = false;
			}

			// For loop to run the game's 10 questions 
			for(int i = 0; i < 10; i++) {

				System.out.println("New question");

				// Fetches a random question and builds the questionString
				Question question = questions.get(generator.nextInt(questions.size()));
				String questionString = shuffleAlternatives(question);

				// Sends question and waits for an ACK | client 1
				clientOneLastSentData = questionString;
				clientOneSendPacket();
				clientOneAwaitAck();
				// Sends question and waits for an ACK | client 2
				clientTwoLastSentData = questionString;
				clientTwoSendPacket();
				clientTwoAwaitAck();

				// Tries to receive the chosen response correctly | client 1
				System.out.println("Waiting for the client 1 chosen alternative, valid inputs: from 1 to " + (difficulty+1));
				while (true) {
					clientOneLastReceivedContent = clientOneReceivePacketNoTimeout(1);
					userChosen = clientOneGetIntegerValue(clientOneLastReceivedContent, 1, difficulty + 1);
					if (!clientOneInvalidDataDetected) {
						clientOneSendAck();
						break;
					}
					clientOneInvalidDataDetected = false;
				}
				System.out.println("Received the client 1 chosen alternative: " + userChosen);
				// Defaults to wrong alternative, if valid changes packet data to "t" and adds
				// to user points
				clientOneLastSentData = "f";
				// Currently the correct answer is always number one
				if (userChosen == 1) {
					clientOnePoints++;
					clientOneLastSentData = "t";
				}
				clientOneSendPacket();
				clientOneAwaitAck();

				// Tries to receive the chosen response correctly | client 2
				System.out.println("Waiting for the client 2 chosen alternative, valid inputs: from 1 to " + (difficulty+1));
				while (true) {
					clientTwoLastReceivedContent = clientTwoReceivePacketNoTimeout(1);
					userChosen = clientTwoGetIntegerValue(clientTwoLastReceivedContent, 1, difficulty + 1);
					if (!clientTwoInvalidDataDetected) {
						clientTwoSendAck();
						break;
					}
					clientTwoInvalidDataDetected = false;
				}
				System.out.println("Received the client 2 chosen alternative: " + userChosen);
				// Defaults to wrong alternative, if valid changes packet data to "t" and adds
				// to user points
				clientTwoLastSentData = "f";
				// Currently the correct answer is always number Two
				if (userChosen == 1) {
					clientTwoPoints++;
					clientTwoLastSentData = "t";
				}
				clientTwoSendPacket();
				clientTwoAwaitAck();
			}

			// Ends game by sending the scores to the clients
			// Client 1
			clientOneLastSentData = "Your points: " + clientOnePoints + ", player 2 points: " + clientTwoPoints;
			clientOneSendPacket();
			clientOneAwaitAck();
			// Client 2
			clientTwoLastSentData = "Your points: " + clientTwoPoints + ", player 1 points: " + clientOnePoints;
			clientTwoSendPacket();
			clientTwoAwaitAck();

			// Closes socket and restarts loop
			System.out.println("Finished match. Ending cycle...");
			clientOneServerSocket.close();
			clientTwoServerSocket.close();
		}
	}
}
