package com.liceolapaz.ahorcado.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liceolapaz.ahorcado.dao.PalabraAD;
import com.liceolapaz.ahorcado.model.Palabra;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Clase utilitaria encargada de importar palabras desde un archivo JSON
 * y guardarlas en la base de datos si no existen previamente.
 */
public class JsonImporter {

    /**
     * Ruta del archivo JSON que contiene las palabras a importar.
     */
    private static final String RUTA_JSON = "src/main/resources/palabras.json";

    /**
     * Método para importar palabras desde un archivo JSON.
     * Verifica si ya existe la primera palabra en la base de datos para evitar duplicados.
     * Si no existe, importa todas las palabras listadas en el archivo.
     */
    public static void importarPalabrasDesdeJSON() {
        PalabraAD palabraAD = new PalabraAD();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Leer el archivo JSON como lista de objetos Palabra
            List<Palabra> palabras = mapper.readValue(new File(RUTA_JSON), new TypeReference<List<Palabra>>() {});

            if (palabras.isEmpty()) {
                System.out.println("El archivo JSON está vacío.");
                return;
            }

            // Verificación de existencia de la primera palabra
            String primeraPalabraTexto = palabras.get(0).getPalabra();
            if (palabraAD.findByPalabra(primeraPalabraTexto) != null) {
                System.out.println("La primera palabra ya existe en la base de datos. No se importarán palabras.");
                return;
            }

            // Guardar cada palabra en la base de datos
            for (Palabra palabra : palabras) {
                palabraAD.save(palabra);
                System.out.println("Palabra insertada: " + palabra.getPalabra());
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo JSON: " + e.getMessage());
        }
    }
}
