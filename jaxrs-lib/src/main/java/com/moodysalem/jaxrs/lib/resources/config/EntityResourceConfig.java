package com.moodysalem.jaxrs.lib.resources.config;

import com.moodysalem.hibernate.model.BaseEntity;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.List;

/**
 * This is the configuration of an entity resource - usually only created once per entity resource+
 *
 * @param <T> type of the entity
 */
public abstract class EntityResourceConfig<T extends BaseEntity> {
    /**
     * Return the entity Class that this resource manages
     *
     * @return class that the resource manages
     */
    public abstract Class<T> getEntityClass();

    /**
     * Return the user friendly name of the entity class
     *
     * @return name of the entity class, used in error messages
     */
    public String getEntityName() {
        return getEntityClass().getSimpleName();
    }

    /**
     * Return the sorting configuration for the resource
     *
     * @return sort configuration
     */
    public SortParameterConfiguration getSortConfiguration() {
        return SortParameterConfiguration.DEFAULT;
    }

    /**
     * Return the pagination configuration for the resource
     *
     * @return the pagination configuration
     */
    public PaginationParameterConfiguration getPaginationConfiguration() {
        return PaginationParameterConfiguration.DEFAULT;
    }

    // whether the entity can be created
    public abstract boolean canMerge(final T oldData, final T newData);

    // perform these actions before persisting the entity
    public abstract void beforeMerge(final T oldData, final T newData);

    // perform these actions after creating an entity
    public abstract void afterMerge(final T entity);

    // whether the entity can be deleted
    public abstract boolean canDelete(final T toDelete);

    // getSingle a list of query predicates for lists
    public abstract void getPredicatesFromRequest(final List<Predicate> predicates, final Root<T> root);

    // used to perform transformations before sending back an entity in a response
    public abstract void beforeSend(final List<T> entity);

    // get the container request context used for finding query parameters
    public abstract ContainerRequestContext getContainerRequestContext();

    // get the entity manager used for querying the database
    public abstract EntityManager getEntityManager();

    /**
     * Helper method checks logged in
     */
    public abstract void checkAccess(Action action);

    public enum Action {
        LIST,
        GET_SINGLE,
        SAVE,
        DELETE_SINGLE,
        DELETE
    }
}
