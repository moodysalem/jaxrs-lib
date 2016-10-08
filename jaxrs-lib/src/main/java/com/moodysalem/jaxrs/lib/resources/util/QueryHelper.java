package com.moodysalem.jaxrs.lib.resources.util;

import com.moodysalem.hibernate.model.BaseEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.*;
import java.util.List;

/**
 * Abstract class for helping with wrapping methods in transactions
 */
public abstract class QueryHelper {
    public interface Predicator<T> {
        Predicate from(Root<T> root);
    }

    public static <T extends BaseEntity> Subquery<T> all(final CriteriaBuilder cb,
                                                         final Class<T> clazz) {
        return subquery(cb, clazz, (root) -> null);
    }

    public static <T extends BaseEntity> Subquery<T> none(final CriteriaBuilder cb,
                                                          final Class<T> clazz) {
        return subquery(cb, clazz, (root) -> cb.or());
    }

    public static <T extends BaseEntity> Subquery<T> subquery(
            final CriteriaBuilder cb,
            final Class<T> clazz,
            final Predicator<T> mapper
    ) {
        final Subquery<T> subquery = cb.createQuery().subquery(clazz);
        final Root<T> root = subquery.from(clazz);
        final Predicate p = mapper.from(root);
        if (p != null) {
            subquery.where(p);
        }
        return subquery;
    }

    public static <T extends BaseEntity> List<T> query(
            final EntityManager em,
            final Class<T> clazz,
            final Predicator<T> mapper
    ) {
        return query(em, clazz, mapper, false);
    }

    public static <T extends BaseEntity> List<T> query(
            final EntityManager em,
            final Class<T> clazz,
            final Predicator<T> mapper,
            final boolean distinct
    ) {
        final CriteriaQuery<T> query = em.getCriteriaBuilder().createQuery(clazz);
        final Root<T> root = query.from(clazz);
        final Predicate predicate = mapper.from(root);

        if (predicate != null) {
            query.where(predicate);
        }
        if (distinct) {
            query.distinct(true);
        }

        return em.createQuery(query).getResultList();
    }

}