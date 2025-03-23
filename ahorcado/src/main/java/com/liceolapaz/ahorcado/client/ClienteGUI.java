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
    private String miNombreNormalizado;

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

                                // MODO MONOJUGADOR: desbloquear al inicio
                                if (texto.startsWith("Partida monojugador iniciada")) {
                                    desbloquearInterfaz();
                                }

                                // FIN PARTIDA
                                if (texto.equalsIgnoreCase("FIN_PARTIDA")) {
                                    bloquearInterfaz();
                                }

                                // MODO MULTIJUGADOR: activar o bloquear según el turno
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
            JOptionPane.showMessageDialog(this, "Error de conexión: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        sendButton.addActionListener((ActionEvent e) -> enviarLetra());
        cancelButton.addActionListener((ActionEvent e) -> cancelarPartida());
        scoreButton.addActionListener((ActionEvent e) -> mostrarPuntuacion());
    }

    private boolean esMiTurno(String texto) {
        return (texto.startsWith("Turno para") || texto.startsWith("Ahora juega:"))
                && texto.toLowerCase().contains(miNombreNormalizado);
    }

    private boolean esTurnoDeOtro(String texto) {
        return (texto.startsWith("Turno para") || texto.startsWith("Ahora juega:"))
                && !texto.toLowerCase().contains(miNombreNormalizado);
    }

    private void enviarLetra() {
        String entrada = inputField.getText().trim();
        if (entrada.isEmpty()) return;
        try {
            if (entrada.length() == 1 && Character.isLetter(entrada.charAt(0))) {
                output.writeObject(entrada.charAt(0));
            } else {
                output.writeObject(entrada);
            }
            output.flush();
            inputField.setText("");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al enviar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void cancelarPartida() {
        try {
            output.writeObject("CANCELAR");
            output.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al cancelar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarPuntuacion() {
        try {
            output.writeObject("PUNTUACION");
            output.flush();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al pedir puntuación: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void bloquearInterfaz() {
        inputField.setEnabled(false);
        sendButton.setEnabled(false);
    }

    private void desbloquearInterfaz() {
        inputField.setEnabled(true);
        sendButton.setEnabled(true);
    }
}
