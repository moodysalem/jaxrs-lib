package com.moodysalem.jaxrs.lib.resources;

import com.moodysalem.hibernate.model.BaseEntity;
import com.moodysalem.hibernate.model.BaseEntity_;
import com.moodysalem.jaxrs.lib.exceptionmappers.RequestError;
import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;
import com.moodysalem.jaxrs.lib.resources.config.EntityResourceConfig;
import com.moodysalem.jaxrs.lib.resources.config.PaginationParameterConfiguration;
import com.moodysalem.jaxrs.lib.resources.config.SortParameterConfiguration;
import com.moodysalem.jaxrs.lib.resources.util.SortInfo;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.moodysalem.jaxrs.lib.resources.config.EntityResourceConfig.Action.*;
import static com.moodysalem.jaxrs.lib.resources.util.TXHelper.withinTransaction;
import static java.lang.String.format;

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
        checkAccess(LIST);

        final Integer count = getCount();
        final int start = getStart();

        // getSingle the entities
        final List<T> entities = getListOfEntities(count, start);
        beforeSend(entities);

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
    public Response getSingle(@PathParam("id") final UUID id) {
        checkAccess(GET_SINGLE);

        final T entity = getEntityWithId(id);
        if (entity == null) {
            idNotFound(id);
        }

        beforeSend(Collections.singletonList(entity));

        return Response.ok(entity).build();
    }

    /**
     * Get the entities with some set of IDs
     *
     * @param ids to get data for
     * @return map of ID to entity
     */
    private Map<UUID, T> getOldData(final Set<UUID> ids) {
        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        final CriteriaQuery<T> query = cb.createQuery(getEntityClass());
        final Root<T> from = query.from(getEntityClass());

        return getEntityManager().createQuery(
                query.select(from).where(from.get(BaseEntity_.id).in(ids))
        ).getResultList()
                .stream().collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    }

    /**
     * Save a set of updates for some entities
     *
     * @param list of updates
     * @return updated list
     */
    @POST
    public Response save(final List<T> list) {
        checkAccess(SAVE);

        if (list == null || list.isEmpty()) {
            throw new RequestProcessingException(Response.Status.BAD_REQUEST, "Empty post body");
        }

        // verify the list contains no ID more than once
        validateNoDuplicateIds(list);

        // collect all the old entities by ID
        final Map<UUID, T> oldData = getOldData(
                list.stream().filter(e -> e.getId() != null)
                        .map(BaseEntity::getId).collect(Collectors.toSet())
        );

        // verify that the user is authorized to save each of the posted entities
        verifyCanMergeData(list, oldData);

        // now start saving the entities
        final List<T> saved = new LinkedList<>();

        // now save each entity
        {
            try {
                withinTransaction(getEntityManager(), () ->
                        list.forEach(e -> {
                            beforeMerge(e.getId() != null ? oldData.get(e.getId()) : null, e);
                            final T merged = getEntityManager().merge(e);
                            saved.add(merged);
                            afterMerge(merged);
                        })
                );
            } catch (Exception e) {
                throw RequestProcessingException.from(e);
            }
        }

        beforeSend(saved);

        return Response.ok(saved).build();
    }

    /**
     * Verify that the user can save each of the items in the list of entities
     *
     * @param list    to verify
     * @param oldData map of old data IDs
     */
    protected void verifyCanMergeData(final List<T> list, final Map<UUID, T> oldData) {
        final List<RequestError> permissionRequestErrors = new LinkedList<>();

        for (int ix = 0; ix < list.size(); ix++) {
            final T entity = list.get(ix);

            // if there is an ID we need to find it
            final T old = entity.getId() != null ? oldData.get(entity.getId()) : null;

            if (!canMerge(old, entity)) {
                permissionRequestErrors.add(
                        new RequestError(
                                entity.getId(),
                                "id",
                                entity.getId() == null ?
                                        format("Cannot save %s #%s", getEntityName(), ix) :
                                        format("Cannot save %s with ID: %s", getEntityName(), entity.getId())
                        )
                );
            }
        }

        if (!permissionRequestErrors.isEmpty()) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    permissionRequestErrors.stream().toArray(RequestError[]::new));
        }
    }

    /**
     * Validate the list only contains one entity per ID
     *
     * @param list to check
     */
    private void validateNoDuplicateIds(final List<T> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        // count the number of times each ID occurs
        final Map<UUID, Long> countById = list.stream()
                .filter(be -> be.getId() != null)
                .collect(Collectors.groupingBy(BaseEntity::getId, Collectors.counting()));

        // return all the IDs that occur more than once
        final Set<UUID> duplicates = countById.keySet()
                .stream()
                .filter(id -> countById.get(id) > 1)
                .collect(Collectors.toSet());

        if (duplicates.size() > 0) {
            throw new RequestProcessingException(Response.Status.BAD_REQUEST,
                    format("The following IDs were found more than once in the request body: %s",
                            duplicates.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        }
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
        checkAccess(DELETE_SINGLE);

        final T entity = getEntityWithId(id);
        if (entity == null) {
            idNotFound(id);
        }

        if (!canDelete(entity)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    format("Not authorized to delete %s with ID %s",
                            getEntityName(), entity.getId()));
        }

        try {
            withinTransaction(getEntityManager(), () -> getEntityManager().remove(entity));
        } catch (Exception e) {
            throw RequestProcessingException.from(e);
        }

        return Response.noContent().build();
    }

    /**
     * Delete all the entities matching the query parameters
     *
     * @return empty response
     */
    @DELETE
    public Response deleteAll() {
        checkAccess(DELETE);

        final List<T> toDelete = getListOfEntities(null, 0);

        final Set<UUID> cannotDelete = toDelete.stream().filter((e) -> !canDelete(e)).map(BaseEntity::getId)
                .collect(Collectors.toSet());

        if (!cannotDelete.isEmpty()) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    format("Not authorized to delete %s with IDs: %s",
                            getEntityName(),
                            cannotDelete.stream().map(UUID::toString).collect(Collectors.joining(", "))));
        }

        try {
            withinTransaction(getEntityManager(), () -> toDelete.forEach(getEntityManager()::remove));
        } catch (Exception e) {
            throw RequestProcessingException.from(e);
        }

        return Response.noContent().build();
    }

    /**
     * Helper method to getSingle a single query parameter
     *
     * @param param name of the query parameter
     * @return the value assigned to the query parameter
     */
    private String getQueryParameter(final String param) {
        final List<String> params = getQueryParameters(param);
        return params != null && !params.isEmpty() ? params.get(0) : null;
    }

    /**
     * Helper method to getSingle a list of values associated with a query parameter
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
     * @param id of the entity to getSingle
     * @return entity of type T with ID id
     */
    private T getEntityWithId(final UUID id) {
        final EntityManager em = getEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();
        final CriteriaQuery<T> cq = cb.createQuery(getEntityClass());
        final Root<T> from = cq.from(getEntityClass());

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
        final Root<T> root = cq.from(getEntityClass());

        final Predicate[] predicates = getPredicatesFromRequest(root).stream().toArray(Predicate[]::new);

        final CriteriaQuery<Long> countQuery = cq.select(cb.count(root)).where(predicates);

        return em.createQuery(countQuery).getSingleResult();
    }

    /**
     * Get a list of entities with a maximum size of count, starting at start
     *
     * @param count max # of entities to getSingle
     * @param start which entity to start at
     * @return a list of type T from the database
     */
    private List<T> getListOfEntities(final Integer count, final int start) {
        if (count != null && count <= 0) {
            return Collections.emptyList();
        }

        final EntityManager em = getEntityManager();
        final CriteriaBuilder cb = em.getCriteriaBuilder();

        final CriteriaQuery<T> cq = cb.createQuery(getEntityClass());
        final Root<T> from = cq.from(getEntityClass());
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
        final List<SortInfo> sorts = SortInfo.from(
                getQueryParameters(sortConfig.getQueryParameterName()),
                sortConfig.getSortInfoSeparator(),
                sortConfig.getSortPathSeparator()
        );

        if (sorts == null || sorts.isEmpty()) {
            return;
        }

        final CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        for (final SortInfo sort : sorts) {
            if (orders.size() > sortConfig.getMaxSorts()) {
                return;
            }

            javax.persistence.criteria.Path sortBy;
            try {
                final LinkedList<String> sortAttributePieces = new LinkedList<>(Arrays.asList(sort.getPath()));

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

            if (sort.isAscending()) {
                orders.add(cb.asc(sortBy));
            } else {
                orders.add(cb.desc(sortBy));
            }
        }
    }

    /**
     * Parse the request to getSingle the query parameters to append to the GET requests
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
     * Throw an exception indicating the ID was not found
     *
     * @param id not found
     */
    private void idNotFound(final UUID id) {
        throw new RequestProcessingException(Response.Status.NOT_FOUND,
                format("%s with ID %s not found", getEntityName(), id));
    }

}

