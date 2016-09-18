package com.moodysalem.jaxrs.lib.resources.util;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import java.util.concurrent.Callable;

/**
 * Abstract class for helping with wrapping methods in transactions
 */
public abstract class TXHelper {
    /**
     * Do something without returning anything
     */
    public interface DoSomething {
        void call();
    }

    /**
     * Overload that does not return anything
     *
     * @param action to perform in transaction
     */
    public static void withinTransaction(final EntityManager em, final DoSomething action) throws Exception {
        withinTransaction(em, () -> {
            action.call();
            return null;
        });
    }

    /**
     * Helper method to open a transaction to contain some process
     *
     * @param action something to do
     * @param <V>    return type
     * @return return type of callable
     */
    public static <V> V withinTransaction(final EntityManager em, final Callable<V> action) throws Exception {
        final EntityTransaction etx = em.getTransaction();
        final V result;

        try {
            etx.begin();
            result = action.call();
            etx.commit();
        } finally {
            if (etx.isActive()) {
                etx.rollback();
            }
        }

        return result;
    }
}