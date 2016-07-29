package mypack;

// Client that let a user play Tic-Tac-Toe with another across a network.

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.ImageObserver;
import java.net.Socket;
import java.net.InetAddress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import java.util.Formatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

/*<applet code="TicTacToeClient" width=400 height=300></applet>*/

public class TicTacToeClient extends JFrame implements Runnable {
	private JTextField idField;
	private JTextArea displayArea;
	private JPanel boardPanel;
	private JPanel panel2;
	private Square board[][];
	// Image  of PLAYER X
	public Image imgCross;
	//Image of PLAYER O
	public Image imgNought;
	public Image deFault;
	private Square currentSquare;
	private Socket connection;
	private Scanner input;
	private Formatter output;
	private String ticTacToeHost;
	private String myMark;
	
	public Image currentImg;
	
	private boolean myTurn;
	private final String X_MARK = "X";
	private final String O_MARK = "O";
	private final static int PLAYER_X = 0;
	private final static int PLAYER_O = 1;
	// Map for current Image
	private Map<String,Image> iconTable = 
		    new HashMap<String,Image>();
	public static int boardSize = 10;

	public TicTacToeClient(String host) {
		//Get and resize Image

		Toolkit t=Toolkit.getDefaultToolkit();
		deFault = reSizeImage(t.getImage("default.gif"), 28, 28);
		imgCross = reSizeImage(t.getImage("cross.gif"), 28, 28);
		imgNought = reSizeImage(t.getImage("nought.gif"), 28, 28);
		// Add image to MAP iconTable
		iconTable.put("X", imgCross);
		iconTable.put("O", imgNought);
		ticTacToeHost = host;
		displayArea = new JTextArea(4, 30);
		displayArea.setEditable(false);
		add(new JScrollPane(displayArea), BorderLayout.SOUTH);

		startClient();
	}

	public void startClient() {
		try {
			// make connection to server
			connection = new Socket(InetAddress.getLocalHost(), 12345);

			// get streams for input and output
			input = new Scanner(connection.getInputStream());
			output = new Formatter(connection.getOutputStream());
			//Image
			Toolkit t=Toolkit.getDefaultToolkit();
			deFault = reSizeImage(t.getImage("default.gif"), 28, 28);
			imgCross = reSizeImage(t.getImage("cross.gif"), 28, 28);
			imgNought = reSizeImage(t.getImage("nought.gif"), 28, 28);
			// boardSize
			boardSize = Integer.parseInt(input.nextLine());
			System.out.println(boardSize);
			boardPanel = new JPanel();
			boardPanel.setLayout(new GridLayout(boardSize, boardSize, 0, 0));
			boardPanel.setBackground(Color.green);
			board = new Square[boardSize][boardSize];

			for (int row = 0; row < board.length; row++) {
				for (int column = 0; column < board[row].length; column++) {

					board[row][column] = new Square(deFault, row * boardSize
							+ column);
					boardPanel.add(board[row][column]); // add square
				}
			}

			idField = new JTextField();
			idField.setEditable(false);
			add(idField, BorderLayout.NORTH);

			panel2 = new JPanel();
			panel2.setBackground(Color.GREEN);
			panel2.add(boardPanel, BorderLayout.CENTER);
			add(panel2, BorderLayout.CENTER);
			setSize(300 + (boardSize - 3) * 30, 225 + (boardSize - 3) * 30);
			setVisible(true);
		} catch (IOException ioException) {
			ioException.printStackTrace();
		}

		ExecutorService worker = Executors.newFixedThreadPool(1);
		worker.execute(this);
	}

	@SuppressWarnings("static-access")
	public Image reSizeImage(Image image, int width, int height) {
		image = new ImageIcon(image.getScaledInstance(width, height,
				imgCross.SCALE_SMOOTH)).getImage();
		return image;
	}

	public void run() {
		myMark = input.nextLine();

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				idField.setText("You are player \"" + myMark + "\"");
			}
		});
		// X_MARK = "X";
		myTurn = (myMark.equals(X_MARK));
		while (true) {
			if (input.hasNextLine())
				processMessage(input.nextLine());
		}
	}

	private void processMessage(String message) {

		if (message.equals("Game over %s won")) {
			displayMessage(message + "\n");
		}

		else if (message.equals("Your move is done")) {
			displayMessage(message + "\n");
			int i = input.nextInt();
			input.nextLine();
			

				System.out.println(myMark);
				setMark(board[i / boardSize][i % boardSize],iconTable.get(myMark));	

																	
		}		
			else if (message.equals("Valid move in "))
			displayMessage(message);

		else if (message.equals("Invalid move, try again")) {
			displayMessage(message + "\n"); // display invalid move
			myTurn = true; // still this client's turn
		} else if (message.equals("Opponent moved")) {
			int location = input.nextInt(); // get move location
			input.nextLine(); // skip newline after int location
			int row = location / boardSize; // calculate row
			int column = location % boardSize; // calculate column
			// Image

			setMark(board[row][column], (myMark.equals(X_MARK) ?imgNought 
					: imgCross));
			displayMessage("Opponent moved. Your turn.\n");
			myTurn = true;
		} else
			displayMessage(message + "\n");
	}

	private void displayMessage(final String messageToDisplay) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				displayArea.append(messageToDisplay);
			} // end method run
		});
	}

	private void setMark(final Square squareToMark, final Image mark) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				squareToMark.setMark(mark);
				// squareToMark.add(Image )
			}
		});
	}

	public void sendClickedSquare(int location) {

		if (myTurn) {
			output.format("%d\n", location);
			output.flush();
			myTurn = false;
		}
	}

	public void setCurrentSquare(Square square) {
		currentSquare = square;
	}

	private class Square extends JPanel {
		private Image mark;
		private int location;

		public Square(Image squareMark, int squareLocation) {
			mark = squareMark;
			location = squareLocation;

			addMouseListener(new MouseAdapter() {
				public void mouseReleased(MouseEvent e) {
					currentSquare = Square.this;

					sendClickedSquare(getSquareLocation());
				}
			});
		}

		public Dimension getPreferredSize() {
			return new Dimension(30, 30);
		}

		public Dimension getMinimumSize() {
			return getPreferredSize();
		}

		public void setMark(Image newMark) {
			mark = newMark;
			repaint();
		}

		public int getSquareLocation() {
			return location;
		}

		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			g.drawRect(0, 0, 28, 28);
			g.drawImage(mark,0,0,28, 28, this);
			// g.drawString( mark, 11, 20 );
		}
	}
}
