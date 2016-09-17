package com.moodysalem.jaxrs.lib.resources;

import com.moodysalem.hibernate.model.BaseEntity;
import com.moodysalem.hibernate.model.BaseEntity_;
import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;
import com.moodysalem.jaxrs.lib.resources.config.PaginationParameterConfiguration;
import com.moodysalem.jaxrs.lib.resources.config.SortParameterConfiguration;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This resource implements a common REST interface for CRUD against a particular
 * entity type
 *
 * @param <T> entity type to allow CRUD
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class EntityResource<T extends BaseEntity> extends EntityResourceConfig<T> {
    private static final Logger LOG = Logger.getLogger(EntityResource.class.getName());

    /**
     * Return a list of type T to the client, including headers about pagination
     *
     * @return response with entity list and headers corresponding to pagination details
     */
    @GET
    public Response getList() {
        checkLoggedIn();

        final Integer count = getCount();
        final int start = getStart();

        // get the entities
        final List<T> entities = getListOfEntities(count, start);
        entities.forEach(this::beforeSend);

        // count the total number of the results that would've been returned
        final long totalCount = getTotalCountOfEntities();

        final PaginationParameterConfiguration paginationConfig = getPaginationConfiguration();

        // return the filtered and mapped list of entities
        return Response.ok(entities)
                .header(paginationConfig.getStartHeader(), start)
                .header(paginationConfig.getCountHeader(), count)
                .header(paginationConfig.getTotalCountHeader(), totalCount)
                .build();
    }

    /**
     * Get a single entity with an ID
     *
     * @param id of the entity
     * @return the entity corresponding to the ID
     */
    @GET
    @Path("{id}")
    public Response get(@PathParam("id") final UUID id) {
        checkLoggedIn();

        final T entity = getEntityWithId(id);
        if (entity == null) {
            idNotFound(id);
        }

        beforeSend(entity);

        return Response.ok(entity).build();
    }

    /**
     * Save a set of updates for some entities
     *
     * @param list of updates
     * @return updated list
     */
    @POST
    public Response save(final List<T> list) {
        if (list == null || list.isEmpty()) {
            throw new RequestProcessingException(Response.Status.BAD_REQUEST, "Empty post body");
        }

        // validate that each ID only shows up once in the POST to prevent strange behavior when saving
        {
            final Map<UUID, Long> countById = list.stream()
                    .filter(be -> be.getId() != null)
                    .collect(Collectors.groupingBy(BaseEntity::getId, Collectors.counting()));

            final Set<UUID> duplicates = new HashSet<>();
            for (final UUID id : countById.keySet()) {
                if (countById.get(id) > 1) {
                    duplicates.add(id);
                }
            }

            if (duplicates.size() > 0) {
                throw new RequestProcessingException(Response.Status.BAD_REQUEST,
                        String.format("The following IDs were found more than once in the request body: %s",
                                duplicates.stream().map(UUID::toString).collect(Collectors.joining(", "))));
            }
        }

        // collect all the old entities with the same IDs
        final Map<UUID, T> oldData = list.stream()
                .filter(e -> e.getId() != null)
                .map(id -> getEntityManager().find(getEntityClass(), id))
                .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));

        // verify that the user can save each of the posted entities
        {
            final List<String> permissionErrors = new LinkedList<>();

            for (int ix = 0; ix < list.size(); ix++) {
                final T entity = list.get(ix);

                // if there is an ID we need to find it
                final T old = entity.getId() != null ? oldData.get(entity.getId()) : null;

                if (!canSave(old, entity)) {
                    permissionErrors.add(entity.getId() == null ?
                            String.format("Cannot save %s #%s", getEntityName(), ix) :
                            String.format("Cannot save %s with ID: %s", getEntityName(), entity.getId())
                    );
                }
            }

            if (!permissionErrors.isEmpty()) {
                throw new RequestProcessingException(Response.Status.FORBIDDEN,
                        permissionErrors.stream().toArray(String[]::new));
            }
        }

        final List<T> saved = new LinkedList<>();

        // now save each entity
        {
            withinTransaction(() ->
                    list.forEach(e -> {
                        beforeSave(e.getId() != null ? oldData.get(e.getId()) : null, e);
                        final T merged = getEntityManager().merge(e);
                        saved.add(merged);
                        afterSave(merged);
                    })
            );
        }

        return Response.ok(saved).build();
    }

    /**
     * Delete a single entity
     *
     * @param id of the entity to delete
     * @return 204 if successful, otherwise error message
     */
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") final UUID id) {
        checkLoggedIn();

        final T entity = getEntityWithId(id);
        if (entity == null) {
            idNotFound(id);
        }

        if (!canDelete(entity)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    String.format(getErrorMessageConfig().getUnauthorizedDelete(), getEntityName(), entity.getId()));
        }

        withinTransaction(() -> getEntityManager().remove(entity));

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    /**
     * Delete all the entities matching the query parameters
     *
     * @return empty response
     */
    @DELETE
    public Response deleteAll() {
        checkLoggedIn();

        final List<T> toDelete = getListOfEntities(null, 0);

        if (!toDelete.stream().allMatch(this::canDelete)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    getErrorMessageConfig().getUnauthorizedDelete());
        }

        withinTransaction(() -> toDelete.forEach(getEntityManager()::remove));

        return Response.noContent().build();
    }

    /**
     * Helper method to get a single query parameter
     *
     * @param param name of the query parameter
     * @return the value assigned to the query parameter
     */
    private String getQueryParameter(final String param) {
        final List<String> params = getQueryParameters(param);
        return params != null && !params.isEmpty() ? params.get(0) : null;
    }

    /**
     * Helper method to get a list of values associated with a query parameter
     *
     * @param param name of the query parameter
     * @return list of values assigned to query parameter
     */
    private List<String> getQueryParameters(final String param) {
        return getContainerRequestContext().getUriInfo().getQueryParameters().get(param);
    }

    /**
     * Get the first record that should be returned
     *
     * @return an int corresponding to the first record to return
     */
    private int getStart() {
        final String start = getQueryParameter(getPaginationConfiguration().getStartQueryParameterName());
        if (start != null) {
            try {
                return Math.max(Integer.parseInt(start), 0);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Invalid start query parameter received", e);
            }
        }
        return 0;
    }

    /**
     * Get the # of records that should be returned
     *
     * @return the # of records, or null if all should be returned
     */
    private Integer getCount() {
        final String countString = getQueryParameter(getPaginationConfiguration().getCountQueryParameterName());
        final Integer maxCount = getPaginationConfiguration().getMaxPerPage();

        Integer count = null;
        if (countString != null) {
            try {
                count = Integer.parseInt(countString);
            } catch (NumberFormatException e) {
                LOG.log(Level.WARNING, "Invalid count query parameter received", e);
            }
        }

        if (count != null) {
            if (maxCount != null) {
                return Math.max(Math.min(count, maxCount), 0);
            } else {
                return Math.max(count, 0);
            }
        }
        return maxCount;
    }

    /**
     * Return the entity with the ID, filtered by the predicates associated with the request
     *
     * @param id of the entity to get
     * @return entity of type T with ID id
     */
    private T getEntityWithId(final UUID id) {
        final EntityManager em = getEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<T> cq = cb.createQuery(this.getEntityClass());
        final Root<T> from = cq.from(this.getEntityClass());

        final Predicate[] predicates = getPredicatesFromRequest(from).stream().toArray(Predicate[]::new);

        final List<T> entity = em.createQuery(cq.select(from)
                .where(
                        cb.equal(from.get(BaseEntity_.id), id),
                        cb.and(predicates)
                )
        ).getResultList();

        return (entity.size() == 1 ? entity.get(0) : null);
    }

    /**
     * Get the total count of entities in the database that match the predicates
     *
     * @return the total count of entities that match the predicates
     */
    private long getTotalCountOfEntities() {
        final EntityManager em = getEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        final Root<T> root = cq.from(this.getEntityClass());

        final Predicate[] predicates = getPredicatesFromRequest(root).stream().toArray(Predicate[]::new);

        final CriteriaQuery<Long> countQuery = cq.select(cb.count(root)).where(predicates);

        return em.createQuery(countQuery).getSingleResult();
    }

    /**
     * Get a list of entities with a maximum size of count, starting at start
     *
     * @param count max # of entities to get
     * @param start which entity to start at
     * @return a list of type T from the database
     */
    private List<T> getListOfEntities(final Integer count, final int start) {
        if (count != null && count <= 0) {
            return Collections.emptyList();
        }

        final EntityManager em = getEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();

        final CriteriaQuery<T> cq = cb.createQuery(this.getEntityClass());
        final Root<T> from = cq.from(this.getEntityClass());
        cq.select(from).distinct(true);

        final Predicate[] predicates = getPredicatesFromRequest(from).stream().toArray(Predicate[]::new);

        if (predicates.length > 0) {
            cq.where(predicates);
        }

        // parse the request to return the orders that should be applied
        final Order[] orderBys = getOrderFromRequest(from).stream().toArray(Order[]::new);
        if (orderBys.length > 0) {
            cq.orderBy(orderBys);
        }

        final TypedQuery<T> query = em.createQuery(cq).setFirstResult(start);

        if (count != null) {
            query.setMaxResults(count);
        }

        return query.getResultList();
    }

    /**
     * Get a list of Orders from the request
     *
     * @param from the root of the query
     * @return a list of orders
     */
    private List<Order> getOrderFromRequest(final Root<T> from) {
        final List<Order> orders = new LinkedList<>();
        getOrderFromRequest(from, orders);
        return orders;
    }

    /**
     * Resolves sorting orders from the request by matching them to attribute on the model
     *
     * @param from root of the query
     */
    // TODO: refactor sort query parameter parsing
    private void getOrderFromRequest(final Root<T> from, final List<Order> orders) {
        final SortParameterConfiguration sortConfig = getSortConfiguration();
        final List<String> sorts = getQueryParameters(Pattern.quote(sortConfig.getQueryParameterName()));

        if (sorts == null || sorts.isEmpty()) {
            return;
        }

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        for (final String sortOrder : sorts) {
            if (orders.size() > sortConfig.getMaxSorts()) {
                return;
            }

            final String[] pieces = sortOrder.split(Pattern.quote(sortConfig.getSortInfoSeparator()));
            if (pieces.length <= 1) {
                continue;
            }

            boolean asc = (pieces[0].equalsIgnoreCase("A"));

            javax.persistence.criteria.Path sortBy;
            try {
                final LinkedList<String> sortAttributePieces = new LinkedList<>(Arrays.asList(
                        pieces[1].split(Pattern.quote(sortConfig.getSortPathSeparator()))));
                if (sortAttributePieces.size() > 1) {
                    Join j = from.join(sortAttributePieces.pop(), JoinType.LEFT);
                    while (sortAttributePieces.size() > 1) {
                        final String attribute = sortAttributePieces.pop();
                        j = j.join(attribute, JoinType.LEFT);
                    }
                    sortBy = j.get(sortAttributePieces.get(0));
                } else {
                    sortBy = from.get(sortAttributePieces.get(0));
                }
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Failed to parse sort", e);
                continue;
            }

            if (asc) {
                orders.add(cb.asc(sortBy));
            } else {
                orders.add(cb.desc(sortBy));
            }
        }
    }

    /**
     * Parse the request to get the query parameters to append to the GET requests
     *
     * @param root the root of the query
     * @return a list of predicates to apply to the query
     */
    private List<Predicate> getPredicatesFromRequest(final Root<T> root) {
        final List<Predicate> predicates = new LinkedList<>();
        getPredicatesFromRequest(predicates, root);
        return predicates;
    }

    /**
     * Translates an exception to a human readable message
     *
     * @param e exception to translate
     * @return a human readable message from the exception, if recognized
     */
    // TODO: yank this out - probably a static method for RequestProcessingException from(Exception e)
    private String translateExceptionToMessage(Exception e) {
        if (e == null) {
            return null;
        }
        String msg = e.getMessage();
        if (e instanceof PersistenceException) {
            PersistenceException pe = (PersistenceException) e;
            if (e.getCause() instanceof ConstraintViolationException) {
                e = (Exception) pe.getCause();
            }
        }
        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException cve = (ConstraintViolationException) e;
            SQLException se = cve.getSQLException();
            StringBuilder sb = new StringBuilder();

            while (se != null) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(se.getMessage());
                se = se.getNextException();
            }

            msg = sb.toString();
        }
        if (e instanceof javax.validation.ConstraintViolationException) {
            StringBuilder sb = new StringBuilder();

            javax.validation.ConstraintViolationException cve = (javax.validation.ConstraintViolationException) e;
            if (cve.getConstraintViolations() != null) {
                for (ConstraintViolation cv : cve.getConstraintViolations()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    String prop = cv.getPropertyPath() != null ? cv.getPropertyPath().toString() : getEntityName();
                    String error = cv.getMessage() != null ? cv.getMessage() : "unknown error";
                    sb.append(String.format("Invalid %s: %s", prop, error));
                }
            }

            msg = sb.toString();
        }
        return msg;
    }

    /**
     * Throw an exception indicating the ID was not found
     *
     * @param id not found
     */
    private void idNotFound(final UUID id) {
        throw new RequestProcessingException(Response.Status.NOT_FOUND,
                String.format(getErrorMessageConfig().getIdNotFound(), getEntityName(), id));
    }

    // used in withinTransaction
    private interface DoSomething {
        void call();
    }

    /**
     * Overload that does not return anything
     *
     * @param action to perform in transaction
     */
    private void withinTransaction(final DoSomething action) {
        withinTransaction(() -> {
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
    private <V> V withinTransaction(final Callable<V> action) {
        final EntityTransaction etx = getEntityManager().getTransaction();
        final V result;

        try {
            etx.begin();
            result = action.call();
            etx.commit();
        } catch (Exception e) {
            throw new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR, translateExceptionToMessage(e));
        } finally {
            if (etx.isActive()) {
                etx.rollback();
            }
        }

        return result;
    }

    /**
     * Helper method checks logged in
     */
    private void checkLoggedIn() {
        if (requiresLogin() && !isLoggedIn()) {
            throw new RequestProcessingException(Response.Status.UNAUTHORIZED,
                    "You must be logged in to access this resource.");
        }
    }
}

