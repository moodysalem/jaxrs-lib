package com.moodysalem.jaxrs.lib.resources;

import com.moodysalem.hibernate.model.BaseEntity;
import com.moodysalem.jaxrs.lib.resources.config.ErrorMessageConfig;
import com.moodysalem.jaxrs.lib.resources.config.PaginationParameterConfiguration;
import com.moodysalem.jaxrs.lib.resources.config.SortParameterConfiguration;

import javax.persistence.EntityManager;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.container.ContainerRequestContext;
import java.util.List;

/**
 * These are the abstract methods that are used to configure an entity resource
 * Pulled out into its own class for better organization
 *
 * @param <T> type of the entity
 */
abstract class EntityResourceConfig<T extends BaseEntity> {
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

    /**
     * Return the error message config object that contains the templates of the error messages
     *
     * @return default error message config
     */
    public ErrorMessageConfig getErrorMessageConfig() {
        return ErrorMessageConfig.DEFAULT;
    }

    // whether the user is authenticated
    public abstract boolean isLoggedIn();

    // whether the resource rqeuires login
    public abstract boolean requiresLogin();

    // whether the entity can be created
    public abstract boolean canSave(final T oldData, final T newData);

    // whether the entity can be deleted
    public abstract boolean canDelete(final T toDelete);

    // perform these actions before persisting the entity
    public abstract void beforeSave(final T oldData, final T newData);

    // get a list of query predicates for lists
    protected abstract void getPredicatesFromRequest(final List<Predicate> predicates,
                                                     final Root<T> root);

    // perform these actions after creating an entity
    public abstract void afterSave(final T entity);

    // used to perform transformations before sending back an entity in a response
    public abstract void beforeSend(final T entity);

    protected abstract ContainerRequestContext getContainerRequestContext();

    protected abstract EntityManager getEntityManager();
}
