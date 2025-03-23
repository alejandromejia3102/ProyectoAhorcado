package com.liceolapaz.ahorcado.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Servidor que arranca en modo 1 o 2 jugadores (Enfoque A).
 * Después crea un GameSession que maneja la lógica del ahorcado.
 */
public class AhorcadoServer {
    private static final int PORT = 65000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Servidor escuchando en el puerto " + PORT);

            // Preguntamos cuántos jugadores en consola
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

            // Esperamos conexiones
            List<Socket> sockets = new ArrayList<>();
            for (int i = 1; i <= numJugadores; i++) {
                System.out.println("Esperando conexión para el jugador " + i + "...");
                Socket socket = serverSocket.accept();
                System.out.println("Jugador " + i + " conectado desde " + socket.getInetAddress());
                sockets.add(socket);
            }

            // Crear la sesión de juego con esos sockets
            GameSession gameSession = new GameSession(sockets);
            // Lanzar en un hilo aparte
            new Thread(gameSession).start();

            System.out.println("GameSession iniciado con " + numJugadores + " jugador(es).");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
