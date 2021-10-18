import java.net.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

	static final String host = "localhost";
	static int serverPort = 9876;
	static final int timeout = 5000;

	static DatagramSocket clientSocket;
	static InetAddress ipAddress;

	static String lastSentData;

	static Scanner input;

	private static void sendPacket() throws Exception {
		byte[] sendData = lastSentData.getBytes();
		DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, ipAddress, serverPort);
		clientSocket.send(sendPacket);
	}

	private static String receivePacket(int bytes) throws Exception {
		byte[] receiveData = new byte[bytes];
		DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

		while (true) {
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

	private static void sendAck() throws Exception {
		lastSentData = "ack";
		sendPacket();
	}

	private static void awaitAck() throws Exception {
		String receivedData;

		// Tries to receive ACK correctly
		while (true) {
			receivedData = receivePacket(3);
			if (receivedData.equals("ack")) {
				break;
			}
			System.out.println("Invalid data received, retrying to receive ack...");
		}
	}

	private static void validateConnection() throws Exception {
		while (true) {
			lastSentData = "connect";
			sendPacket();
			String response = receivePacket(4);
			if (response.equals("ack1")) {
				lastSentData = "ack2";
				sendPacket();
				System.out.println("Connection validated successfully!");
				break;
			}
		}
	}

	private static int readValidInteger(int min, int max) {
		while (true) {
			System.out.println("Please input an integer between " + min + " and " + max + ":");
			int integer;
			try {
				integer = input.nextInt();
			} catch (InputMismatchException exception) {
				continue;
			}
			if (integer < min || integer > max) {
				continue;
			}
			return integer;
		}
	}

	public static void main(String[] args) throws Exception {

		// Setting up the client
		ipAddress = InetAddress.getByName(host);
		input = new Scanner(System.in);

		// Main client loop
		while (true) {

			// Setting up a new game connection
			clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(timeout);
			int userInput;

			System.out.println("Starting game...");

			// Asks if the client is player 1 or 2
			System.out.println("Are you player 1 or 2?");
			userInput = readValidInteger(1, 2);
			if (userInput == 2) {
				serverPort = 9877;
				System.out.println("Trying to validate the connection with the server");
				validateConnection();
			} else {
				serverPort = 9876;
				System.out.println("Trying to validate the connection with the server");
				validateConnection();
				// Difficulty selection and sending to the server
				System.out.println("Please choose a difficulty level:");
				System.out.println("1 - Easy");
				System.out.println("2 - Medium");
				System.out.println("3 - Hard");
				userInput = readValidInteger(1, 3);
				lastSentData = "" + userInput;
				sendPacket();
				awaitAck();
			}

			// Start asking questions
			for (int index = 0; index < 10; index++) {

				// Tries to receive the question data correctly
				String receivedData;
				while (true) {
					receivedData = receivePacket(100);
					if (receivedData.length() > 10) {
						sendAck();
						break;
					}
				}

				// Prints out the received question
				String[] question = receivedData.split(";");
				System.out.println("Question: " + question[0]);
				for (int answers = 1; answers < question.length; answers++) {
					System.out.println(answers + ": " + question[answers]);
				}

				// Collects the user's answer and sends it back to the server
				userInput = readValidInteger(1, question.length - 1);
				lastSentData = "" + userInput;
				sendPacket();
				awaitAck();

				// Tries to receive the chosen response grading (correct or wrong) correctly
				while (true) {
					receivedData = receivePacket(1);
					if (receivedData.equals("t")) {
						System.out.println("Correct!");
						sendAck();
						break;
					} else if (receivedData.equals("f")) {
						System.out.println("Wrong!");
						sendAck();
						break;
					} else {
						System.out.println("Invalid data received, retrying receive...");
					}
				}

			}

			// Tries to receive the final score of the game correctly
			String finalScore;
			while (true) {
				finalScore = receivePacket(37);
				if (finalScore.length() == 37) {
					sendAck();
					break;
				}
			}

			// Prints out the score and restarts the client
			System.out.println("Finished match! " + finalScore);
			System.out.println("Restarting client...");
		}
	}
}
