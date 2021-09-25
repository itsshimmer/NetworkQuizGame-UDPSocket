import java.net.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client {

	static final String host = "localhost";
	static final int serverPort = 9876;
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

		// while que controla o recebimento de resposta
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

	private static void sendAck() throws Exception {
		lastSentData = "ack";
		sendPacket();
	}

	private static void awaitAck() throws Exception {
		String receivedData;
		receivedData = receivePacket(3);
		if (!receivedData.equals("ack")) {
			System.out.println("Invalid data received, exiting...");
			System.exit(0);
		}
	}

	public static void main(String[] args) throws Exception {

		ipAddress = InetAddress.getByName(host);
		input = new Scanner(System.in);

		while (true) {
			clientSocket = new DatagramSocket();
			clientSocket.setSoTimeout(timeout);

			int userInput;

			System.out.println("Trying to validate the connection with the server");
			validateConnection();
			System.out.println("Starting game...");

			// Difficulty selection
			System.out.println("Please choose a difficulty level:");
			System.out.println("1 - Easy");
			System.out.println("2 - Medium");
			System.out.println("3 - Hard");
			userInput = readValidInteger(1, 3);
			lastSentData = "" + userInput;
			sendPacket();
			awaitAck();

			// Amount of questions selection
			System.out.println("How many questions should be asked this round(1-100)?");
			int numberOfQuestions = readValidInteger(1, 100);

			// Start asking questions
			for (int index = 0; index < numberOfQuestions; index++) {
				lastSentData = "q";
				sendPacket();
				awaitAck();
				String receivedData = receivePacket(100);
				sendAck();
				String[] question = receivedData.split(";");
				System.out.println("Question: " + question[0]);
				for (int answers = 1; answers < question.length; answers++) {
					System.out.println(answers + ": " + question[answers]);
				}
				userInput = readValidInteger(1, question.length - 1);
				lastSentData = "" + userInput;
				sendPacket();
				awaitAck();
				// Waits for server chosen response validation
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
			lastSentData = "e";
			sendPacket();
			awaitAck();

			String finalScore = receivePacket(3);
			sendAck();
			System.out.println("Finished match! Your score was: " + finalScore);
			System.out.println("Restarting client...");
		}
	}
}
