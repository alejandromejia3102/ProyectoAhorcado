package com.liceolapaz.ahorcado.util;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

/**
 * Clase utilitaria para gestionar la configuración y acceso a Hibernate.
 * Proporciona una única instancia de {@link SessionFactory} para toda la aplicación.
 */
public class HibernateUtil {
    // Instancia única de SessionFactory
    private static final SessionFactory sessionFactory = buildSessionFactory();

    /**
     * Crea la instancia de SessionFactory a partir del archivo de configuración de Hibernate.
     * @return una nueva instancia de SessionFactory.
     */
    private static SessionFactory buildSessionFactory() {
        try {
            return new Configuration().configure().buildSessionFactory();
        } catch (Throwable ex) {
            System.err.println("Error al crear SessionFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    /**
     * Devuelve la instancia única de SessionFactory para realizar operaciones con la base de datos.
     * @return instancia compartida de SessionFactory.
     */
    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Cierra la SessionFactory liberando todos los recursos asociados.
     */
    public static void shutdown() {
        getSessionFactory().close();
    }
}
