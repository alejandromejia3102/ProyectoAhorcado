package com.liceolapaz.ahorcado.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Random;

public class ClienteGUI extends JFrame {
    private JTextField inputField;
    private JTextArea outputArea;
    private JButton sendButton, cancelButton, scoreButton;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private String nombreJugador;

    public ClienteGUI(Socket socket) {
        this.socket = socket;

        // PEDIR NOMBRE DEL JUGADOR
        nombreJugador = JOptionPane.showInputDialog(this, "Introduce tu nombre:");
        if (nombreJugador == null || nombreJugador.trim().isEmpty()) {
            nombreJugador = "Invitado" + new Random().nextInt(1000);
        }

        setTitle("Ahorcado - Jugador: " + nombreJugador);
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        inputField = new JTextField(20);
        outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);

        sendButton = new JButton("Enviar");
        cancelButton = new JButton("Cancelar");
        scoreButton = new JButton("Mostrar Puntuación");

        JPanel panel = new JPanel();
        panel.add(new JLabel("Introduce una letra o palabra:"));
        panel.add(inputField);
        panel.add(sendButton);
        panel.add(cancelButton);
        panel.add(scoreButton);

        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        try {
            // Streams
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Enviar el nombre al servidor
            output.writeObject("NOMBRE:" + nombreJugador);
            output.flush();

            // LANZAMOS UN HILO PARA LEER MENSAJES DE MANERA CONTINUA
            new Thread(() -> {
                try {
                    while (true) {
                        Object mensaje = input.readObject();
                        if (mensaje instanceof String) {
                            String texto = (String) mensaje;
                            // Mostramos el texto en la interfaz
                            outputArea.append(texto + "\n");

                            // Si el servidor manda FIN_PARTIDA, bloqueamos
                            if (texto.equalsIgnoreCase("FIN_PARTIDA")) {
                                bloquearInterfaz();
                            }
                        } 
                        // Si no es String, puedes manejarlo aquí si hiciese falta
                    }
                } catch (IOException | ClassNotFoundException ex) {
                    // Cuando se cierre la conexión saltará esta excepción
                    // Podemos simplemente salir del while
                    System.out.println("Lectura finalizada: " + ex.getMessage());
                }
            }).start();

        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al conectar con el servidor: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }

        // Acciones de botones
        sendButton.addActionListener((ActionEvent e) -> enviarLetra());
        cancelButton.addActionListener((ActionEvent e) -> cancelarPartida());
        scoreButton.addActionListener((ActionEvent e) -> mostrarPuntuacion());
    }

    private void enviarLetra() {
        String entrada = inputField.getText().trim();
        try {
            // Si es un único carácter, lo mandamos como Character
            if (entrada.length() == 1 && Character.isLetter(entrada.charAt(0))) {
                output.writeObject(entrada.charAt(0));
            } else {
                // En caso contrario, lo mandamos como String (posible palabra completa)
                output.writeObject(entrada);
            }
            output.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al enviar datos al servidor: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        inputField.setText("");
    }

    private void cancelarPartida() {
        try {
            output.writeObject("CANCELAR");
            output.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al cancelar la partida: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarPuntuacion() {
        try {
            output.writeObject("PUNTUACION");
            output.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al solicitar la puntuación: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bloquearInterfaz() {
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
        cancelButton.setEnabled(false);
    }
}
