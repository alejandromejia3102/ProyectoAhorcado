package com.liceolapaz.ahorcado.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

/**
 * Clase que gestiona la conexión y lógica del cliente con interfaz gráfica.
 * Permite enviar letras o palabras al servidor y recibir mensajes del juego.
 */
public class ClienteGUI extends JFrame {
	private JTextField inputField;
	private JTextArea outputArea;
	private JButton sendButton, cancelButton, scoreButton;
	private Socket socket;
	private ObjectOutputStream output;
	private ObjectInputStream input;
	private String nombreJugador;
	private String miNombreNormalizado;

	/**
	 * Constructor del cliente con GUI. Configura la interfaz y la conexión al
	 * servidor.
	 * 
	 * @param socket Socket conectado al servidor.
	 */
	public ClienteGUI(Socket socket) {
		this.socket = socket;

		// PEDIR NOMBRE
		nombreJugador = JOptionPane.showInputDialog(this, "Introduce tu nombre:");
		if (nombreJugador == null || nombreJugador.trim().isEmpty()) {
			nombreJugador = "Invitado" + new Random().nextInt(1000);
		}
		miNombreNormalizado = nombreJugador.trim().toLowerCase();

		setTitle("Ahorcado - Jugador: " + nombreJugador);
		setSize(500, 350);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		inputField = new JTextField(20);
		outputArea = new JTextArea(12, 40);
		outputArea.setEditable(false);

		sendButton = new JButton("Enviar");
		cancelButton = new JButton("Cancelar");
		scoreButton = new JButton("Mostrar Puntuación");

		JPanel panel = new JPanel();
		panel.add(new JLabel("Letra o palabra:"));
		panel.add(inputField);
		panel.add(sendButton);
		panel.add(cancelButton);
		panel.add(scoreButton);

		add(new JScrollPane(outputArea), BorderLayout.CENTER);
		add(panel, BorderLayout.SOUTH);

		bloquearInterfaz();

		try {
			output = new ObjectOutputStream(socket.getOutputStream());
			input = new ObjectInputStream(socket.getInputStream());

			// Enviar nombre
			output.writeObject("NOMBRE:" + nombreJugador);
			output.flush();

			// HILO DE ESCUCHA
			new Thread(() -> {
				try {
					while (true) {
						Object mensaje = input.readObject();
						if (mensaje instanceof String) {
							String texto = (String) mensaje;

							SwingUtilities.invokeLater(() -> {
								outputArea.append(texto + "\n");

								if (texto.startsWith("Partida monojugador iniciada")) {
									desbloquearInterfaz();
								}

								if (texto.equalsIgnoreCase("FIN_PARTIDA")) {
									bloquearInterfaz();
								}

								if (esMiTurno(texto)) {
									desbloquearInterfaz();
								} else if (esTurnoDeOtro(texto)) {
									bloquearInterfaz();
								}
							});
						}
					}
				} catch (Exception ex) {
					System.err.println("Conexión finalizada: " + ex.getMessage());
				}
			}).start();

		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error de conexión: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}

		sendButton.addActionListener((ActionEvent e) -> enviarLetra());
		cancelButton.addActionListener((ActionEvent e) -> cancelarPartida());
		scoreButton.addActionListener((ActionEvent e) -> mostrarPuntuacion());
	}

	/**
	 * Determina si el mensaje recibido indica que es el turno del jugador.
	 * 
	 * @param texto Mensaje recibido.
	 * @return true si es el turno del jugador.
	 */
	private boolean esMiTurno(String texto) {
		return (texto.startsWith("Turno para") || texto.startsWith("Ahora juega:"))
				&& texto.toLowerCase().contains(miNombreNormalizado);
	}

	/**
	 * Determina si el turno es de otro jugador.
	 * 
	 * @param texto Mensaje recibido.
	 * @return true si no es el turno del jugador.
	 */
	private boolean esTurnoDeOtro(String texto) {
		return (texto.startsWith("Turno para") || texto.startsWith("Ahora juega:"))
				&& !texto.toLowerCase().contains(miNombreNormalizado);
	}

	/**
	 * Envía la letra o palabra ingresada al servidor.
	 */
	private void enviarLetra() {
		String entrada = inputField.getText().trim();
		if (entrada.isEmpty())
			return;
		try {
			if (entrada.length() == 1 && Character.isLetter(entrada.charAt(0))) {
				output.writeObject(entrada.charAt(0));
			} else {
				output.writeObject(entrada);
			}
			output.flush();
			inputField.setText("");
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error al enviar: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Envía un mensaje al servidor para cancelar la partida actual.
	 */
	private void cancelarPartida() {
		try {
			output.writeObject("CANCELAR");
			output.flush();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error al cancelar: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Envía una solicitud al servidor para mostrar la puntuación del jugador.
	 */
	private void mostrarPuntuacion() {
		try {
			output.writeObject("PUNTUACION");
			output.flush();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Error al pedir puntuación: " + e.getMessage(), "Error",
					JOptionPane.ERROR_MESSAGE);
		}
	}

	/**
	 * Desactiva los campos y botones de entrada.
	 */
	private void bloquearInterfaz() {
		inputField.setEnabled(false);
		sendButton.setEnabled(false);
	}

	/**
	 * Activa los campos y botones de entrada.
	 */
	private void desbloquearInterfaz() {
		inputField.setEnabled(true);
		sendButton.setEnabled(true);
	}
}
