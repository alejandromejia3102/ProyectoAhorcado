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
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());

            // Enviar el nombre al servidor
            output.writeObject("NOMBRE:" + nombreJugador);
            output.flush();

            // Leer mensajes iniciales del servidor
            while (true) {
                Object mensaje = input.readObject();
                if (mensaje instanceof String) {
                    String texto = (String) mensaje;

                    // Si el servidor manda "FIN_PARTIDA" antes de empezar
                    if (texto.equalsIgnoreCase("FIN_PARTIDA")) {
                        outputArea.append("Fin de la partida.\n");
                        bloquearInterfaz();
                        break;
                    }

                    // Mostrar mensaje
                    outputArea.append(texto + "\n");

                    // Rompemos el bucle si vemos un indicio de que ya comenzó
                    if (texto.contains("Empieza el turno")
                            || texto.contains("Empieza a jugar")
                            || texto.contains("Partida monojugador")
                            || texto.contains("Modo 2 jugadores")) // <-- IMPORTANTE
                    {
                        break;
                    }
                } else {
                    break;
                }
            }

        } catch (IOException | ClassNotFoundException e) {
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

            // Esperar respuesta del servidor
            String respuesta = (String) input.readObject();
            outputArea.append(respuesta + "\n");

            // Si llega FIN_PARTIDA, bloqueamos
            if (respuesta.equals("FIN_PARTIDA")) {
                bloquearInterfaz();
            }

        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al enviar o recibir datos: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        inputField.setText("");
    }

    private void cancelarPartida() {
        try {
            output.writeObject("CANCELAR");
            output.flush();

            String respuesta = (String) input.readObject();
            outputArea.append(respuesta + "\n");
            bloquearInterfaz();

        } catch (IOException | ClassNotFoundException e) {
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

            String puntuacion = (String) input.readObject();
            outputArea.append("Tu puntuación global es: " + puntuacion + "\n");

        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this,
                    "Error al obtener la puntuación: " + e.getMessage(),
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
