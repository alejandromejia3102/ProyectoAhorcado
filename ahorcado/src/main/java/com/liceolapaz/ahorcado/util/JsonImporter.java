package com.liceolapaz.ahorcado.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.liceolapaz.ahorcado.dao.PalabraAD;
import com.liceolapaz.ahorcado.model.Palabra;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class JsonImporter {

    // Ruta del archivo JSON (directamente en resources)
    private static final String RUTA_JSON = "src/main/resources/palabras.json";

    // Método para importar palabras desde un archivo JSON
    public static void importarPalabrasDesdeJSON() {
        PalabraAD palabraAD = new PalabraAD();
        ObjectMapper mapper = new ObjectMapper();

        try {
            // Leer el archivo JSON
            List<Palabra> palabras = mapper.readValue(new File(RUTA_JSON), new TypeReference<List<Palabra>>() {});

            // Verificar si la primera palabra ya está en la base de datos
            if (palabras.isEmpty()) {
                System.out.println("El archivo JSON está vacío.");
                return;
            }

            String primeraPalabraTexto = palabras.get(0).getPalabra();
            if (palabraAD.findByPalabra(primeraPalabraTexto) != null) {
                System.out.println("La primera palabra ya existe en la base de datos. No se importarán palabras.");
                return;
            }

            // Si la primera palabra no existe, importar todas las palabras
            for (Palabra palabra : palabras) {
                palabraAD.save(palabra);
                System.out.println("Palabra insertada: " + palabra.getPalabra());
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo JSON: " + e.getMessage());
        }
    }
}