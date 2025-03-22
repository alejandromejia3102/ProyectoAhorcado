package com.liceolapaz.ahorcado.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class AhorcadoServer {
    private static final int PORT = 65000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor escuchando en el puerto " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(new GameSession(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}