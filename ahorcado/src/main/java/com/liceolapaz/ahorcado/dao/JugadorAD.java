package com.liceolapaz.ahorcado.dao;

import com.liceolapaz.ahorcado.model.Jugador;
import com.liceolapaz.ahorcado.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class JugadorAD {

    public void save(Jugador jugador) {
        Transaction transaction = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            transaction = session.beginTransaction();
            session.save(jugador);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
    }

    public List<Jugador> getAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Jugador", Jugador.class).list();
        }
    }

    public Jugador getById(int id) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.get(Jugador.class, id);
        }
    }

    public Jugador getByNombre(String nombre) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM Jugador WHERE nombre = :nombre", Jugador.class)
                    .setParameter("nombre", nombre)
                    .uniqueResult();
        }
    }
}