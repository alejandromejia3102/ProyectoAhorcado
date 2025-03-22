package com.liceolapaz.ahorcado;

import com.liceolapaz.ahorcado.dao.PalabraAD;
import com.liceolapaz.ahorcado.model.Palabra;
import com.liceolapaz.ahorcado.util.JsonImporter;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        JsonImporter.importarPalabrasDesdeJSON();

        PalabraAD palabraDAO = new PalabraAD();
        List<Palabra> palabras = palabraDAO.getAll();
        System.out.println("Lista de palabras después de la importación:");
        for (Palabra p : palabras) {
            System.out.println(p.getPalabra());
        }
    }
}