package com.liceolapaz.ahorcado.server;

import com.liceolapaz.ahorcado.dao.JugadorAD;
import com.liceolapaz.ahorcado.dao.PalabraAD;
import com.liceolapaz.ahorcado.dao.PartidaAD;
import com.liceolapaz.ahorcado.model.Jugador;
import com.liceolapaz.ahorcado.model.Palabra;
import com.liceolapaz.ahorcado.model.Partida;

import java.io.*;
import java.net.Socket;
import java.util.*;

/**
 * Clase que representa una sesión de juego del servidor.
 * Gestiona la lógica de juego entre 1 o 2 jugadores conectados por socket.
 */
public class GameSession implements Runnable {
    private List<Socket> sockets;
    private ObjectOutputStream[] outputs;
    private ObjectInputStream[] inputs;
    private Jugador[] jugadores;
    private int numeroJugadores;

    private JugadorAD jugadorAD = new JugadorAD();
    private PalabraAD palabraAD = new PalabraAD();
    private PartidaAD partidaAD = new PartidaAD();

    private String palabraSecreta;
    private char[] estado;
    private int[] intentosRestantes;
    private boolean partidaActiva = true;

    /**
     * Constructor que crea una sesión de juego con los sockets de los jugadores.
     * @param sockets Lista de sockets de los jugadores.
     */
    public GameSession(List<Socket> sockets) {
        this.sockets = sockets;
        this.numeroJugadores = sockets.size();
        outputs = new ObjectOutputStream[numeroJugadores];
        inputs = new ObjectInputStream[numeroJugadores];
        jugadores = new Jugador[numeroJugadores];
        intentosRestantes = new int[numeroJugadores];
    }

    /**
     * Lógica principal de ejecución del hilo. Controla el flujo del juego.
     */
    @Override
    public void run() {
        try {
            // Inicializar streams
            for (int i = 0; i < numeroJugadores; i++) {
                outputs[i] = new ObjectOutputStream(sockets.get(i).getOutputStream());
                inputs[i] = new ObjectInputStream(sockets.get(i).getInputStream());
            }

            // Recibir nombres de los jugadores
            for (int i = 0; i < numeroJugadores; i++) {
                Object obj = inputs[i].readObject();
                if (obj instanceof String && ((String) obj).startsWith("NOMBRE:")) {
                    String nombre = ((String) obj).substring("NOMBRE:".length()).trim();
                    Jugador jug = jugadorAD.getByNombre(nombre);
                    if (jug == null) {
                        jug = new Jugador();
                        jug.setNombre(nombre);
                        jugadorAD.save(jug);
                    }
                    jugadores[i] = jug;
                    send(i, "Bienvenido, " + nombre + ". Esperando a que todos estén listos...");
                }
            }

            if (!iniciarPartida()) {
                partidaActiva = false;
                return;
            }

            if (numeroJugadores == 1) {
                jugarMonojugador(0);
            } else {
                jugarDosJugadores();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            for (Socket socket : sockets) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Inicializa una nueva partida seleccionando una palabra aleatoria.
     * @return true si se puede iniciar la partida, false si no hay palabras.
     */
    private boolean iniciarPartida() throws IOException {
        List<Palabra> lista = palabraAD.getAll();
        if (lista.isEmpty()) {
            broadcast("No hay palabras en la BD. No se puede jugar.", true);
            return false;
        }

        Palabra p = lista.get(new Random().nextInt(lista.size()));
        palabraSecreta = p.getPalabra().toUpperCase();
        estado = new char[palabraSecreta.length()];
        Arrays.fill(estado, '_');

        for (int i = 0; i < numeroJugadores; i++) {
            intentosRestantes[i] = Math.max(1, palabraSecreta.length() / 2);
        }

        broadcast("Comienza la partida. La palabra tiene " + palabraSecreta.length() + " letras.", false);
        return true;
    }

    /**
     * Lógica del modo de juego para un solo jugador.
     */
    private void jugarMonojugador(int idx) throws IOException, ClassNotFoundException {
        send(idx, "Partida monojugador iniciada. Tienes " + intentosRestantes[idx] + " fallos permitidos.");

        while (partidaActiva && intentosRestantes[idx] > 0 && !palabraAdivinada()) {
            Object obj = inputs[idx].readObject();
            if (!partidaActiva) break;

            if (obj instanceof Character) {
                char letra = (char) obj;
                boolean acierto = procesarLetra(letra, idx);
                if (!acierto) {
                    intentosRestantes[idx]--;
                    if (intentosRestantes[idx] <= 0) {
                        send(idx, "Has perdido. La palabra era: " + palabraSecreta);
                        registrarPartida(false, jugadores[idx]);
                        send(idx, "FIN_PARTIDA");
                        partidaActiva = false;
                        return;
                    } else {
                        send(idx, "No está la letra '" + letra + "'. Te quedan " + intentosRestantes[idx] + " intentos.");
                    }
                }
            } else if (obj instanceof String) {
                String cmd = ((String) obj).trim();

                if (cmd.equalsIgnoreCase("CANCELAR")) {
                    send(idx, "Has cancelado la partida. No se guarda puntuación.");
                    partidaActiva = false;
                    return;

                } else if (cmd.equalsIgnoreCase("PUNTUACION")) {
                    mostrarPuntuacion(idx);

                } else {
                    if (cmd.equalsIgnoreCase(palabraSecreta)) {
                        send(idx, "¡Has adivinado la palabra!: " + palabraSecreta);
                        registrarPartida(true, jugadores[idx], palabraSecreta.length() < 10 ? 1 : 2);
                        send(idx, "FIN_PARTIDA");
                        partidaActiva = false;
                        return;
                    } else {
                        intentosRestantes[idx]--;
                        if (intentosRestantes[idx] <= 0) {
                            send(idx, "Has perdido. La palabra era: " + palabraSecreta);
                            registrarPartida(false, jugadores[idx]);
                            send(idx, "FIN_PARTIDA");
                            partidaActiva = false;
                            return;
                        } else {
                            send(idx, "No era la palabra. Te quedan " + intentosRestantes[idx] + " intentos.");
                        }
                    }
                }
            }

            if (palabraAdivinada()) {
                send(idx, "¡Has adivinado la palabra!: " + palabraSecreta);
                registrarPartida(true, jugadores[idx], palabraSecreta.length() < 10 ? 1 : 2);
                send(idx, "FIN_PARTIDA");
                partidaActiva = false;
                return;
            }
        }
    }

    /**
     * Lógica del modo de juego para dos jugadores.
     */
    private void jugarDosJugadores() throws IOException, ClassNotFoundException {
        int turno = new Random().nextInt(2);
        broadcast("Modo 2 jugadores. Empieza: " + jugadores[turno].getNombre(), false);
        broadcast("Ahora juega: " + jugadores[turno].getNombre(), false);

        while (partidaActiva && !palabraAdivinada() && (intentosRestantes[0] > 0 || intentosRestantes[1] > 0)) {
            Object obj = inputs[turno].readObject();
            if (!partidaActiva) break;

            if (obj instanceof Character) {
                char letra = (char) obj;
                boolean acierto = procesarLetra(letra, turno);

                if (!acierto) {
                    intentosRestantes[turno]--;
                    if (intentosRestantes[turno] <= 0) {
                        send(turno, "Has perdido. No te quedan más intentos.");
                        int otro = (turno == 0) ? 1 : 0;
                        if (intentosRestantes[otro] > 0 && !palabraAdivinada()) {
                            broadcast("Ahora juega: " + jugadores[otro].getNombre(), false);
                            mostrarEstadoPalabra(otro);
                            turno = otro;
                        } else {
                            terminarPartida(false, -1);
                            break;
                        }
                    } else {
                        send(turno, "No está la letra '" + letra + "'. Te quedan " + intentosRestantes[turno] + " intentos.");
                        int otro = (turno == 0) ? 1 : 0;
                        broadcast("Fallo. Turno para " + jugadores[otro].getNombre(), false);
                        broadcast("Ahora juega: " + jugadores[otro].getNombre(), false);
                        mostrarEstadoPalabra(otro);
                        turno = otro;
                    }
                }
            } else if (obj instanceof String) {
                String cmd = ((String) obj).trim();

                if (cmd.equalsIgnoreCase("CANCELAR")) {
                    broadcast("Un jugador se ha retirado. Partida cancelada para ambos.", false);
                    partidaActiva = false;
                    break;

                } else if (cmd.equalsIgnoreCase("PUNTUACION")) {
                    mostrarPuntuacion(turno);

                } else {
                    if (cmd.equalsIgnoreCase(palabraSecreta)) {
                        send(turno, "¡Has adivinado la palabra!: " + palabraSecreta);
                        terminarPartida(true, turno);
                        break;
                    } else {
                        intentosRestantes[turno]--;
                        send(turno, "No era la palabra. Te quedan " + intentosRestantes[turno] + " intentos.");
                        int otro = (turno == 0) ? 1 : 0;
                        if (intentosRestantes[turno] <= 0) {
                            if (intentosRestantes[otro] > 0 && !palabraAdivinada()) {
                                broadcast("Ahora juega: " + jugadores[otro].getNombre(), false);
                                mostrarEstadoPalabra(otro);
                                turno = otro;
                            } else {
                                terminarPartida(false, -1);
                                break;
                            }
                        } else {
                            broadcast("Fallo. Turno para " + jugadores[otro].getNombre(), false);
                            broadcast("Ahora juega: " + jugadores[otro].getNombre(), false);
                            mostrarEstadoPalabra(otro);
                            turno = otro;
                        }
                    }
                }
            }

            if (palabraAdivinada()) {
                send(turno, "¡Has adivinado la palabra!: " + palabraSecreta);
                terminarPartida(true, turno);
                break;
            }
        }
    }

    /**
     * Procesa una letra introducida por un jugador y actualiza el estado de la palabra.
     * @param letra Letra enviada.
     * @param idxJugador Índice del jugador.
     * @return true si acertó alguna letra.
     */
    private boolean procesarLetra(char letra, int idxJugador) throws IOException {
        letra = Character.toUpperCase(letra);
        boolean acierto = false;

        for (int i = 0; i < palabraSecreta.length(); i++) {
            if (palabraSecreta.charAt(i) == letra && estado[i] != letra) {
                estado[i] = letra;
                acierto = true;
            }
        }

        if (acierto) {
            send(idxJugador, "Acertaste la letra '" + letra + "'. Estado: " + new String(estado));
        }

        return acierto;
    }

    /**
     * Verifica si ya no quedan letras por adivinar.
     * @return true si la palabra está completa.
     */
    private boolean palabraAdivinada() {
        return new String(estado).indexOf('_') == -1;
    }

    /**
     * Finaliza la partida, registra los resultados y avisa a los jugadores.
     */
    private void terminarPartida(boolean acertado, int idxGanador) throws IOException {
        partidaActiva = false;
        if (!acertado) {
            broadcast("La palabra era: " + palabraSecreta + ". Nadie la adivinó.", false);
            for (Jugador j : jugadores) registrarPartida(false, j);
        } else {
            int puntos = palabraSecreta.length() < 10 ? 1 : 2;
            registrarPartida(true, jugadores[idxGanador], puntos);
            for (int i = 0; i < numeroJugadores; i++) {
                if (i != idxGanador) registrarPartida(false, jugadores[i]);
            }
        }

        for (int i = 0; i < numeroJugadores; i++) {
            send(i, "FIN_PARTIDA");
        }
    }

    /**
     * Registra una partida sin puntuación.
     */
    private void registrarPartida(boolean acertado, Jugador j) {
        registrarPartida(acertado, j, 0);
    }

    /**
     * Registra una partida en la base de datos con puntuación.
     */
    private void registrarPartida(boolean acertado, Jugador j, int puntos) {
        try {
            Palabra palabraBD = palabraAD.findByPalabra(palabraSecreta);
            Partida partida = new Partida();
            partida.setJugador(j);
            partida.setPalabra(palabraBD);
            partida.setFechaHora(new Date());
            partida.setAcertado(acertado);
            partida.setPuntuacion(puntos);
            partidaAD.save(partida);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Muestra la puntuación total del jugador actual.
     */
    private void mostrarPuntuacion(int idxJugador) throws IOException {
        Jugador jug = jugadores[idxJugador];
        List<Partida> partidas = partidaAD.findByJugador(jug);
        int total = partidas.stream().mapToInt(Partida::getPuntuacion).sum();
        send(idxJugador, "Tu puntuación global es: " + total);
    }

    /**
     * Envía el estado actual de la palabra al jugador indicado.
     */
    private void mostrarEstadoPalabra(int idxJugador) throws IOException {
        send(idxJugador, "Estado actual de la palabra: " + new String(estado));
    }

    /**
     * Envía un mensaje a un jugador específico.
     */
    private void send(int idx, String msg) throws IOException {
        outputs[idx].writeObject(msg);
        outputs[idx].flush();
    }

    /**
     * Envía un mensaje a todos los jugadores.
     * @param msg Mensaje a enviar.
     * @param close Si se deben cerrar los sockets después de enviar.
     */
    private void broadcast(String msg, boolean close) throws IOException {
        for (int i = 0; i < numeroJugadores; i++) {
            outputs[i].writeObject(msg);
            outputs[i].flush();
            if (close) sockets.get(i).close();
        }
    }
}
