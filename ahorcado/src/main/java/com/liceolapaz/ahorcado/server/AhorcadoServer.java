package com.liceolapaz.ahorcado.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Clase principal del servidor para el juego del Ahorcado.
 * Se encarga de inicializar el servidor, recibir conexiones de jugadores
 * y lanzar una sesión de juego con uno o dos participantes.
 */
public class AhorcadoServer {
    private static final int PORT = 65000;

    /**
     * Método principal para iniciar el servidor.
     * Solicita el número de jugadores, espera sus conexiones y lanza la partida.
     * @param args Argumentos de la línea de comandos (no utilizados).
     */
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor escuchando en el puerto " + PORT);

            // Preguntar cuántos jugadores jugarán (1 o 2)
            Scanner sc = new Scanner(System.in);
            int numJugadores = 0;
            while (numJugadores != 1 && numJugadores != 2) {
                System.out.print("Elige el número de jugadores (1 o 2): ");
                try {
                    numJugadores = Integer.parseInt(sc.nextLine());
                } catch (NumberFormatException e) {
                    System.out.println("Entrada no válida. Introduce 1 o 2.");
                }
            }

            // Esperar conexiones
            List<Socket> sockets = new ArrayList<>();
            for (int i = 1; i <= numJugadores; i++) {
                System.out.println("Esperando conexión para el jugador " + i + "...");
                Socket socket = serverSocket.accept();
                System.out.println("Jugador " + i + " conectado desde " + socket.getInetAddress());
                sockets.add(socket);
            }

            // Crear sesión de juego y lanzarla en un hilo
            GameSession gameSession = new GameSession(sockets);
            new Thread(gameSession).start();

            System.out.println("GameSession iniciado con " + numJugadores + " jugador(es).");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
