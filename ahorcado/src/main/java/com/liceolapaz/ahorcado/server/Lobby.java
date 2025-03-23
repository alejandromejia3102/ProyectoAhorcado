package com.liceolapaz.ahorcado.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase que representa un lobby de espera en el servidor.
 * Se encarga de esperar la conexión de los jugadores antes de iniciar una sesión de juego.
 */
public class Lobby {
    private int numJugadores;
    private ServerSocket serverSocket;

    /**
     * Constructor del lobby.
     * @param numJugadores Número de jugadores que se deben conectar antes de comenzar la partida.
     * @param serverSocket Socket del servidor que acepta las conexiones entrantes.
     */
    public Lobby(int numJugadores, ServerSocket serverSocket) {
        this.numJugadores = numJugadores;
        this.serverSocket = serverSocket;
    }

    /**
     * Inicia la espera de conexiones de jugadores.
     * Cuando todos los jugadores se han conectado, lanza una nueva GameSession en un hilo separado.
     */
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
