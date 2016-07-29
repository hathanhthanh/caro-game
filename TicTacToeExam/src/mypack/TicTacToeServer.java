package mypack;

// This class maintains a game of Tic-Tac-Toe for two clients.
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Formatter;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Condition;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

// /*<applet code="TicTacToeServer" width=400 height=300></applet>*/

public class TicTacToeServer extends JFrame {
	// current location
	private int k;
	private int boardSize;
	private int winSize;
	private String[] board; // tic-tac-toe board
	private JTextArea outputArea;
	private JTextField inputSize;
	private JLabel label;
	private Container conTainer;
	private JButton ok;
	private Player[] players;
	private ServerSocket server;
	private int currentPlayer;
	private final static int PLAYER_X = 0;
	private final static int PLAYER_O = 1;
	private final static String[] MARKS = { "X", "O" };
	// Doi tuong executor quan li multil client
	private ExecutorService runGame;
	// A lock is a tool for controlling access to a shared resource by multiple
	// threads
	private Lock gameLock;
	private Condition otherPlayerConnected; // to wait for other player
	private Condition otherPlayerTurn; // to wait for other player's turn

	public TicTacToeServer() {
		super("Tic-Tac-Toe Server");

		// Quản lý client, 2 client= 2 thread, chỉ có 2 player
		conTainer = getContentPane();
		outputArea = new JTextArea(25, 25);
		inputSize = new JTextField(20);
		ok = new JButton("OK");
		label = new JLabel("input size of board: ", JLabel.LEFT);
		conTainer.add(label);
		conTainer.add(inputSize);
		conTainer.add(ok);
		JFrame parent = new JFrame();

		ok.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				// get Text input Size of board
				String temp1 = inputSize.getText();
				if (temp1.isEmpty()) {
					System.out.println("You clicked the button");
					JOptionPane.showMessageDialog(parent,
							"please input size of board");
				} else {
					try {
						int temp2 = Integer.parseInt(temp1);
						// set size board >=3 && size board<=20
						if (temp2 >= 3 && temp2 <= 20) {
							//winSize = temp2 >= 5 ? 5 : temp2;
							if(temp2>=5)
							{
								winSize=5;
							}
							else
							{
								winSize=temp2;
							}
							System.out.println("ok here");
							boardSize = temp2;
							runGame = Executors.newFixedThreadPool(2);
							
							gameLock = new ReentrantLock();
							
							otherPlayerConnected = gameLock.newCondition();
							otherPlayerTurn = gameLock.newCondition();
							
							board = new String[boardSize * boardSize];
							for (int i = 0; i < boardSize * boardSize; i++)
								board[i] = new String("");
							
							players = new Player[2];
							
							currentPlayer = PLAYER_X;
							try {
								// server duoc start, serverSocket
								server = new ServerSocket(12345, 2);
							} catch (IOException ioException) {
								ioException.printStackTrace();
								System.exit(1);
							}
							for (int i = 0; i < boardSize * boardSize; i++) {
								board[i] = i + "";
							}
							execute();
						} else {
							JOptionPane
									.showMessageDialog(parent,
											"Size board must greater than 3 and less than 20, please!");

						}
					} catch (Exception ex) {
						JOptionPane.showMessageDialog(parent,
								"Size board must be number, please!");
					}
				}
			}
		});

		conTainer.add(outputArea);
		outputArea.setText("Server awaiting connections\n");
		conTainer.setLayout(new FlowLayout());
		setSize(300, 300);
		setVisible(true);
	}

	//
	public void execute() {
		for (int i = 0; i < players.length; i++) {
			try {
				players[i] = new Player(server.accept(), i);
				runGame.execute(players[i]);
			} catch (IOException ioException) {
				ioException.printStackTrace();
				System.exit(1);
			}
		}
		gameLock.lock();
		try {
			players[PLAYER_X].setSuspended(false);
			otherPlayerConnected.signal();
		} finally {
			gameLock.unlock(); // unlock game after signalling player X
		}
	}

	private void displayMessage(final String messageToDisplay) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				outputArea.append(messageToDisplay);
			}
		});
	}

	// determine if move is valid
	public boolean validateAndMove(int location, int player) {
		// while not current player, must wait for turn
		while (player != currentPlayer) {
			gameLock.lock(); // lock game to wait for other player to go

			try {
				otherPlayerTurn.await(); // wait for player's turn
			} catch (InterruptedException exception) {
				exception.printStackTrace();
			} finally {
				gameLock.unlock(); // unlock game after waiting
			}
		}

		if (!isOccupied(location)) {
			if (isGameOver()) {
				return false;
			}
			board[location] = MARKS[currentPlayer];

			players[currentPlayer].playermov(location);

			currentPlayer = (currentPlayer + 1) % 2; // change player

			// let new current player know that move occurred
			players[currentPlayer].otherPlayerMoved(location);

			gameLock.lock(); // lock game to signal other player to go

			try {
				otherPlayerTurn.signal(); // signal other player to continue
			} finally {
				if (!isGameOver())
					gameLock.unlock(); // unlock game after signaling
				else

				{
					players[currentPlayer].output.format("Game over %s won\n",
							board[location]);
					k=location;
					players[currentPlayer].output.flush();
					gameLock.lock();

				}

			}
			return true;
		} else
			return false;
	}

	// determine whether location is occupied
	public boolean isOccupied(int location) {
		if (board[location].equals(MARKS[PLAYER_X])
				|| board[location].equals(MARKS[PLAYER_O]))
			return true; // location is occupied
		else
			return false;
	}
	// convert x,y to location in board[]
	int calcIndex(int x, int y) {
		return x * boardSize + y;
	}

	// các trường hợp win game
	public boolean isGameOver() {

		for (int x = 0; x < boardSize; x++) {
			for (int y = 0; y < boardSize; y++) {
				int i = 1;
				// hang ngang
				while (true) {
					if (i == winSize) {
						return true;
					} else {
						if (x == boardSize || y + i == boardSize) {
							break;
						}

						if (board[calcIndex(x, y)].equals(board[calcIndex(x, y
								+ i)])) {
							i++;
						} else {
							break;
						}
					}
				}

				i = 1;
				// hang cheo 1
				while (true) {
					if (i == winSize) {
						return true;
					} else {
						if (x + i == boardSize || y + i == boardSize) {
							break;
						}

						if (board[calcIndex(x, y)].equals(board[calcIndex(
								x + i, y + i)])) {
							i++;
						} else {
							break;
						}
					}
				}
				i = 1;
				// hang cheo 2
				while (true) {
					if (i == winSize) {
						return true;
					} else {
						if (x + i == boardSize||y-i<0) {
							break;
						}
						if (board[calcIndex(x, y)].equals(board[calcIndex(
								x + i, y - i)])) {
							i++;
						} else {
							break;
						}
					}
				}

				i = 1;
				// hang doc
				while (true) {
					if (i == winSize) {
						return true;
					} else {
						if (x + i == boardSize || y == boardSize) {
							break;
						}

						if (board[calcIndex(x, y)].equals(board[calcIndex(
								x + i, y)])) {
							i++;
						} else {
							break;
						}
					}
				}
			}
		}

		return false;
	}

	// mỗi player là một thread.
	// private inner class Player manages each Player as a runnable
	private class Player implements Runnable {
		private Socket connection;
		private Scanner input;
		private Formatter output;
		private int playerNumber;
		private String mark;
		private boolean suspended = true;

		public Player(Socket socket, int number) {
			playerNumber = number;
			mark = MARKS[playerNumber];
			connection = socket;
			try {
				input = new Scanner(connection.getInputStream());
				output = new Formatter(connection.getOutputStream());
				// Send board size to client
				output.format(boardSize + "\n");
				output.flush();
			} catch (IOException ioException) {
				ioException.printStackTrace();
				System.exit(1);
			}
		}

		public void playermov(int loc) {
			output.format("Your move is done" + "\n");
			output.format("%d\n", loc);
			output.flush();
		}

		// send message that other player moved
		public void otherPlayerMoved(int location) {
			output.format("Opponent moved\n");
			output.format("%d\n", location); // send location of move
			output.flush(); // flush output
		}

		public void run() {
			// send client its mark (X or O), process messages from client
			try {
				displayMessage("Player " + mark + " connected\n");
				output.format("%s\n", mark); // send player's mark
				output.flush(); // flush output

				if (playerNumber == PLAYER_X) {

					output.format("%s\n%s", "Player X connected",
							"Waiting for another player\n");
					output.flush(); // flush output

					gameLock.lock(); // lock game to wait for second player

					try {
						while (suspended) {
							otherPlayerConnected.await(); // wait for player O
						}
					} catch (InterruptedException exception) {
						exception.printStackTrace();
					} finally {
						gameLock.unlock(); // unlock game after second player
					}

					// send message that other player connected
					output.format("Other player connected. Your move.\n");
					output.flush(); // flush output
				} // end if
				else {
					output.format("Player O connected, please wait\n");
					output.flush();
				}

				// while game not over
				while (!isGameOver()) {
					int location = 0;

					if (input.hasNext())
						location = input.nextInt(); // get move location
					// check for valid move
					if (validateAndMove(location, playerNumber)) {
						displayMessage("\nlocation: " + location);
						output.format("Valid move in %d\n", location);
						output.flush();
					} else {
						output.format("Invalid move, try again\n");
						output.flush();
					}
					if (isGameOver()) {
						output.format("Game over Player  %s Won \n",board[k]);
						output.flush();
					}
				}
			} finally {
				try {
					connection.close(); // close connection to client
				} catch (IOException ioException) {
					ioException.printStackTrace();
					System.exit(1);
				}
			}
		}

		public void setSuspended(boolean status) {
			suspended = status;
		}
	}
}