package com.liceolapaz.ahorcado.client;

import java.io.IOException;
import java.net.Socket;

public class AhorcadoClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 65000);

            ClienteGUI gui = new ClienteGUI(socket);
            gui.setVisible(true);
        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
        }
    }
}