package com.liceolapaz.ahorcado.dao;

import com.liceolapaz.ahorcado.model.Jugador;
import com.liceolapaz.ahorcado.model.Partida;
import com.liceolapaz.ahorcado.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PartidaAD {

    public void save(Partida partida) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.save(partida);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public List<Partida> getAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery("FROM Partida", Partida.class).list();
        } finally {
            session.close();
        }
    }

    public Partida getById(int id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Partida.class, id);
        } finally {
            session.close();
        }
        
    }
    public List<Partida> findByJugador(Jugador jugador) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery(
                "FROM Partida p WHERE p.jugador.id = :idJugador", Partida.class)
                .setParameter("idJugador", jugador.getId())
                .list();
        }
    }

}