package com.liceolapaz.ahorcado.dao;

import com.liceolapaz.ahorcado.model.Palabra;
import com.liceolapaz.ahorcado.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.List;

public class PalabraAD {

    public void save(Palabra palabra) {
        Transaction transaction = null;
        Session session = null;
        try {
            session = HibernateUtil.getSessionFactory().openSession();
            transaction = session.beginTransaction();
            session.save(palabra);
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

    public List<Palabra> getAll() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery("FROM Palabra", Palabra.class).list();
        } finally {
            session.close();
        }
    }

    public Palabra getById(int id) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.get(Palabra.class, id);
        } finally {
            session.close();
        }
    }

    public Palabra findByPalabra(String palabraTexto) {
        Session session = HibernateUtil.getSessionFactory().openSession();
        try {
            return session.createQuery("FROM Palabra WHERE palabra = :palabra", Palabra.class)
                    .setParameter("palabra", palabraTexto)
                    .uniqueResult();
        } finally {
            session.close();
        }
    }
}