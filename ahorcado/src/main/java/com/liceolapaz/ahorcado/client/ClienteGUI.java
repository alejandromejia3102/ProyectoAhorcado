package com.liceolapaz.ahorcado.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClienteGUI extends JFrame {
    private JTextField inputField;
    private JTextArea outputArea;
    private JButton sendButton, cancelButton, scoreButton;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;

    public ClienteGUI(Socket socket) {
        this.socket = socket;

        setTitle("Ahorcado");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        inputField = new JTextField(20);
        outputArea = new JTextArea(10, 30);
        outputArea.setEditable(false);

        sendButton = new JButton("Enviar");
        cancelButton = new JButton("Cancelar");
        scoreButton = new JButton("Mostrar Puntuación");

        JPanel panel = new JPanel();
        panel.add(new JLabel("Introduce una letra:"));
        panel.add(inputField);
        panel.add(sendButton);
        panel.add(cancelButton);
        panel.add(scoreButton);

        add(new JScrollPane(outputArea), BorderLayout.CENTER);
        add(panel, BorderLayout.SOUTH);

        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error al inicializar streams: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enviarLetra();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelarPartida();
            }
        });

        scoreButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarPuntuacion();
            }
        });
    }

    private void enviarLetra() {
        String letra = inputField.getText().trim();
        if (letra.length() == 1) {
            try {
                output.writeObject(letra.charAt(0));
                output.flush();

                String respuesta = (String) input.readObject();
                outputArea.append(respuesta + "\n");
            } catch (IOException | ClassNotFoundException e) {
                JOptionPane.showMessageDialog(this, "Error al enviar o recibir datos: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, "Introduce una sola letra.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        inputField.setText("");
    }

    private void cancelarPartida() {
        try {
            output.writeObject("CANCELAR");
            output.flush();

            String respuesta = (String) input.readObject();
            outputArea.append(respuesta + "\n");
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error al cancelar la partida: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void mostrarPuntuacion() {
        try {
            output.writeObject("PUNTUACION");
            output.flush();

            String puntuacion = (String) input.readObject();
            outputArea.append("Tu puntuación global es: " + puntuacion + "\n");
        } catch (IOException | ClassNotFoundException e) {
            JOptionPane.showMessageDialog(this, "Error al obtener la puntuación: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}