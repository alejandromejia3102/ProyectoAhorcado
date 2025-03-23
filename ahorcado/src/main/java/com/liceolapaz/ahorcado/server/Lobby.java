package com.liceolapaz.ahorcado.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Lobby {
    private int numJugadores;
    private ServerSocket serverSocket;

    public Lobby(int numJugadores, ServerSocket serverSocket) {
        this.numJugadores = numJugadores;
        this.serverSocket = serverSocket;
    }

    public void startLobby() {
        try {
            List<Socket> sockets = new ArrayList<>();
            for (int i = 1; i <= numJugadores; i++) {
                System.out.println("Esperando conexión para el jugador " + i + " ...");
                Socket socket = serverSocket.accept();
                System.out.println("Jugador " + i + " conectado desde " + socket.getInetAddress());
                sockets.add(socket);
            }

            GameSession gameSession = new GameSession(sockets);
            new Thread(gameSession).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
