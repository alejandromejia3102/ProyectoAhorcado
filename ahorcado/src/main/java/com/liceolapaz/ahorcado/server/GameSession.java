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
// Esto tambien creo
public class GameSession implements Runnable {
    private List<Socket> sockets;
    private ObjectOutputStream[] outputs;
    private ObjectInputStream[] inputs;
    private Jugador[] jugadores;
    private int numeroJugadores;

    // DAOs
    private JugadorAD jugadorAD = new JugadorAD();
    private PalabraAD palabraAD = new PalabraAD();
    private PartidaAD partidaAD = new PartidaAD();

    // Variables del juego
    private String palabraSecreta;
    private char[] estado;
    private int[] intentosRestantes;
    private boolean partidaActiva = true;

    public GameSession(List<Socket> sockets) {
        this.sockets = sockets;
        this.numeroJugadores = sockets.size(); // 1 o 2
        outputs = new ObjectOutputStream[numeroJugadores];
        inputs = new ObjectInputStream[numeroJugadores];
        jugadores = new Jugador[numeroJugadores];
        intentosRestantes = new int[numeroJugadores];
    }

    @Override
    public void run() {
        try {
            // 1) Inicializar streams
            for (int i = 0; i < numeroJugadores; i++) {
                outputs[i] = new ObjectOutputStream(sockets.get(i).getOutputStream());
                inputs[i] = new ObjectInputStream(sockets.get(i).getInputStream());
            }

            // 2) Recibir nombres
            for (int i = 0; i < numeroJugadores; i++) {
                Object obj = inputs[i].readObject();
                if (obj instanceof String) {
                    String msg = (String) obj;
                    if (msg.startsWith("NOMBRE:")) {
                        String nombre = msg.substring("NOMBRE:".length()).trim();
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
            }

            // 3) Iniciar partida
            if (!iniciarPartida()) {
                partidaActiva = false;
                return;
            }

            // 4) Monojugador o 2 jugadores
            if (numeroJugadores == 1) {
                jugarMonojugador(0);
            } else {
                jugarDosJugadores();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Cerrar sockets
            for (int i = 0; i < numeroJugadores; i++) {
                try {
                    sockets.get(i).close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

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
            // Al menos 1 intento
            intentosRestantes[i] = Math.max(1, palabraSecreta.length() / 2);
        }

        broadcast("Comienza la partida. La palabra tiene "
                + palabraSecreta.length() + " letras.", false);
        return true;
    }

    // -------------------------------------------------------------------------
    // MONOJUGADOR
    // -------------------------------------------------------------------------
    private void jugarMonojugador(int idx) throws IOException, ClassNotFoundException {
        send(idx, "Partida monojugador iniciada. Tienes "
                + intentosRestantes[idx] + " fallos permitidos.");

        while (partidaActiva && intentosRestantes[idx] > 0 && !palabraAdivinada()) {
            Object obj = inputs[idx].readObject();
            if (!partidaActiva) break;

            // ----------------------------------------------------------
            // JUGADA: LETRA
            // ----------------------------------------------------------
            if (obj instanceof Character) {
                char letra = (char) obj;
                boolean acierto = procesarLetra(letra, idx);
                if (!acierto) {
                    // Solo aquí mandamos el "No está la letra" (una sola vez)
                    intentosRestantes[idx]--;
                    if (intentosRestantes[idx] <= 0) {
                        send(idx, "Has perdido. La palabra era: " + palabraSecreta);
                        registrarPartida(false, jugadores[idx]);
                        send(idx, "FIN_PARTIDA");
                        partidaActiva = false;
                        return;
                    } else {
                        send(idx, "No está la letra '" + letra + "'. Te quedan "
                                + intentosRestantes[idx] + " intentos.");
                    }
                }
            }
            // ----------------------------------------------------------
            // JUGADA: TEXTO (CANCELAR, PUNTUACION, O ADIVINAR)
            // ----------------------------------------------------------
            else if (obj instanceof String) {
                String cmd = ((String) obj).trim();

                if (cmd.equalsIgnoreCase("CANCELAR")) {
                    send(idx, "Has cancelado la partida. No se guarda puntuación.");
                    partidaActiva = false;
                    return;

                } else if (cmd.equalsIgnoreCase("PUNTUACION")) {
                    mostrarPuntuacion(idx);

                } else {
                    // Intento de adivinar palabra entera
                    if (cmd.equalsIgnoreCase(palabraSecreta)) {
                        // ¡Acierta!
                        send(idx, "¡Has adivinado la palabra!: " + palabraSecreta);
                        int puntos = (palabraSecreta.length() < 10) ? 1 : 2;
                        registrarPartida(true, jugadores[idx], puntos);
                        send(idx, "FIN_PARTIDA");
                        partidaActiva = false;
                        return;
                    } else {
                        // Falla adivinando la palabra
                        intentosRestantes[idx]--;
                        if (intentosRestantes[idx] <= 0) {
                            send(idx, "Has perdido. La palabra era: " + palabraSecreta);
                            registrarPartida(false, jugadores[idx]);
                            send(idx, "FIN_PARTIDA");
                            partidaActiva = false;
                            return;
                        } else {
                            send(idx, "No era la palabra. Te quedan "
                                    + intentosRestantes[idx] + " intentos.");
                        }
                    }
                }
            }

            // Comprobamos si se ha adivinado por letras
            if (palabraAdivinada()) {
                send(idx, "¡Has adivinado la palabra!: " + palabraSecreta);
                int puntos = (palabraSecreta.length() < 10) ? 1 : 2;
                registrarPartida(true, jugadores[idx], puntos);
                send(idx, "FIN_PARTIDA");
                partidaActiva = false;
                return;
            }
        }
    }

    // -------------------------------------------------------------------------
    // 2 JUGADORES
    // -------------------------------------------------------------------------
    private void jugarDosJugadores() throws IOException, ClassNotFoundException {
        int turno = new Random().nextInt(2);
        broadcast("Modo 2 jugadores. Empieza: " + jugadores[turno].getNombre(), false);

        while (partidaActiva && !palabraAdivinada()
                && (intentosRestantes[0] > 0 || intentosRestantes[1] > 0)) {

            Object obj = inputs[turno].readObject();
            if (!partidaActiva) break;

            // ----------------------------------------------------------
            // JUGADA: LETRA
            // ----------------------------------------------------------
            if (obj instanceof Character) {
                char letra = (char) obj;
                boolean acierto = procesarLetra(letra, turno);

             // Después de fallo con letra
                if (!acierto) {
                    intentosRestantes[turno]--;
                    if (intentosRestantes[turno] <= 0) {
                        send(turno, "Has perdido. No te quedan más intentos.");
                        int otro = (turno == 0) ? 1 : 0;
                        if (intentosRestantes[otro] > 0 && !palabraAdivinada()) {
                            broadcast("Ahora juega: " + jugadores[otro].getNombre(), false);
                            mostrarEstadoPalabra(otro); // ← ¡AQUÍ!
                            turno = otro;
                        } else {
                            terminarPartida(false, -1);
                            break;
                        }
                    } else {
                        send(turno, "No está la letra '" + letra + "'. Te quedan " +
                                intentosRestantes[turno] + " intentos.");
                        int otro = (turno == 0) ? 1 : 0;
                        broadcast("Fallo. Turno para " + jugadores[otro].getNombre(), false);
                        mostrarEstadoPalabra(otro); // ← ¡AQUÍ TAMBIÉN!
                        turno = otro;
                    }
                }

            }
            // ----------------------------------------------------------
            // JUGADA: TEXTO (CANCELAR, PUNTUACION, O ADIVINAR PALABRA)
            // ----------------------------------------------------------
            else if (obj instanceof String) {
                String cmd = ((String) obj).trim();

                if (cmd.equalsIgnoreCase("CANCELAR")) {
                    broadcast("Un jugador se ha retirado. Partida cancelada para ambos.", false);
                    partidaActiva = false;
                    break;

                } else if (cmd.equalsIgnoreCase("PUNTUACION")) {
                    mostrarPuntuacion(turno);

                } else {
                    // intento de palabra completa
                    if (cmd.equalsIgnoreCase(palabraSecreta)) {
                        send(turno, "¡Has adivinado la palabra!: " + palabraSecreta);
                        terminarPartida(true, turno);
                        break;
                    } else {
                        intentosRestantes[turno]--;
                        send(turno, "No era la palabra. Te quedan " +
                            intentosRestantes[turno] + " intentos.");

                        if (intentosRestantes[turno] <= 0) {
                            int otro = (turno == 0) ? 1 : 0;
                            if (intentosRestantes[otro] > 0 && !palabraAdivinada()) {
                                broadcast("Ahora juega: " + jugadores[otro].getNombre(), false);
                                mostrarEstadoPalabra(otro); // << NUEVO
                                turno = otro;
                            } else {
                                terminarPartida(false, -1);
                                break;
                            }
                        } else {
                            int otro = (turno == 0) ? 1 : 0;
                            broadcast("Fallo. Turno para " + jugadores[otro].getNombre(), false);
                            mostrarEstadoPalabra(otro); // << NUEVO
                            turno = otro;
                        }
                    }
                }
            }


            // Comprobamos si la palabra está adivinada tras la jugada
            if (palabraAdivinada()) {
                send(turno, "¡Has adivinado la palabra!: " + palabraSecreta);
                terminarPartida(true, turno);
                break;
            }
        }
    }

    // -------------------------------------------------------------------------
    // PROCESAR LETRA (Sin mensaje de “No está la letra” para evitar duplicados)
    // -------------------------------------------------------------------------
    private boolean procesarLetra(char letra, int idxJugador) throws IOException {
        letra = Character.toUpperCase(letra);
        boolean acierto = false;

        for (int i = 0; i < palabraSecreta.length(); i++) {
            if (palabraSecreta.charAt(i) == letra && estado[i] != letra) {
                estado[i] = letra;
                acierto = true;
            }
        }

        // Si acierta, enviamos el mensaje aquí
        if (acierto) {
            send(idxJugador, "Acertaste la letra '" + letra
                    + "'. Estado: " + new String(estado));
        }
        // Si NO acierta, no decimos nada. 
        // El método llama a return false, y el mensaje de error se manda 
        // fuera (en jugarMonojugador / jugarDosJugadores).

        return acierto;
    }

    private boolean palabraAdivinada() {
        return new String(estado).indexOf('_') == -1;
    }

    // -------------------------------------------------------------------------
    // TERMINAR PARTIDA
    // -------------------------------------------------------------------------
    private void terminarPartida(boolean acertado, int idxGanador) throws IOException {
        partidaActiva = false;
        if (!acertado) {
            broadcast("La palabra era: " + palabraSecreta + ". Nadie la adivinó.", false);
            // Registrar 0 para todos
            for (int i = 0; i < numeroJugadores; i++) {
                registrarPartida(false, jugadores[i]);
            }
        } else {
            int puntos = (palabraSecreta.length() < 10) ? 1 : 2;
            registrarPartida(true, jugadores[idxGanador], puntos);
            // El resto sin puntos
            for (int i = 0; i < numeroJugadores; i++) {
                if (i != idxGanador) {
                    registrarPartida(false, jugadores[i]);
                }
            }
        }

        // Avisar a todos
        for (int i = 0; i < numeroJugadores; i++) {
            send(i, "FIN_PARTIDA");
        }
    }

    private void registrarPartida(boolean acertado, Jugador j) {
        registrarPartida(acertado, j, 0);
    }

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

    // -------------------------------------------------------------------------
    // MOSTRAR PUNTUACIÓN
    // -------------------------------------------------------------------------
    private void mostrarPuntuacion(int idxJugador) throws IOException {
        Jugador jug = jugadores[idxJugador];
        List<Partida> historial = jug.getPartidas();
        if (historial == null) {
            historial = new ArrayList<>();
        }

        int total = 0;
        for (Partida p : historial) {
            total += p.getPuntuacion();
        }

        send(idxJugador, "Tu puntuación global es: " + total);
    }
    
    // -------------------------------------------------------------------------
    // MOSTRAR ESTADO
    // -------------------------------------------------------------------------
    private void mostrarEstadoPalabra(int idxJugador) throws IOException {
        send(idxJugador, "Estado actual de la palabra: " + new String(estado));
    }


    // -------------------------------------------------------------------------
    // MÉTODOS AUXILIARES DE ENVÍO
    // -------------------------------------------------------------------------
    private void send(int idx, String msg) throws IOException {
        outputs[idx].writeObject(msg);
        outputs[idx].flush();
    }

    private void broadcast(String msg, boolean close) throws IOException {
        for (int i = 0; i < numeroJugadores; i++) {
            outputs[i].writeObject(msg);
            outputs[i].flush();
            if (close) {
                sockets.get(i).close();
            }
        }
    }
}
