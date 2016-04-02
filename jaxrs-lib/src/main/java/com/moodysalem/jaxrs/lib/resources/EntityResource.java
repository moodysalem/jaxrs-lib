package com.moodysalem.jaxrs.lib.resources;

import com.moodysalem.hibernate.model.BaseEntity;
import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import javax.validation.ConstraintViolation;
import javax.ws.rs.*;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This resource implements a common REST interface for CRUD against a particular
 * entity type
 *
 * @param <T> entity type to allow CRUD
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class EntityResource<T extends BaseEntity> {

    private static final Logger LOG = Logger.getLogger(EntityResource.class.getName());
    private static final String S_WITH_ID_S_NOT_FOUND = "%s with ID %s not found";

    /**
     * Return the Class that this resource manages
     *
     * @return class that the resource manages
     */
    public abstract Class<T> getEntityClass();


    protected abstract ContainerRequestContext getContainerRequestContext();

    protected abstract EntityManager getEntityManager();

    //////////////////////////////CONSTANTS//////////////////////////////////////////

    // sorting behavior
    public abstract String getSortQueryParameterName();

    public abstract String getSortInfoSeparator();

    public abstract String getSortPathSeparator();

    public abstract int getMaxNumberOfSorts();

    // paging behavior
    public abstract String getStartQueryParameterName();

    public abstract String getCountQueryParameterName();

    public abstract Integer getMaxPerPage();

    // name of the header that should indicate the first record returned
    public abstract String getStartHeader();

    // name of the header that should indicate the count of records returned
    public abstract String getCountHeader();

    // name of the header that should include the total count of records that fit the criteria
    public abstract String getTotalCountHeader();

    // whether the user is authenticated
    public abstract boolean isLoggedIn();

    // whether the resource rqeuires login
    public abstract boolean requiresLogin();

    // whether the entity can be created
    public abstract boolean canCreate(T entity);

    // whether the entity can be edited
    public abstract boolean canEdit(T entity);

    // whether the entity can be deleted
    public abstract boolean canDelete(T entity);

    // fill errors array with any validation error strings
    protected abstract void validateEntity(List<String> errors, T entity);

    // perform these actions before persisting the entity
    public abstract void beforeCreate(T entity);

    // perform these actions before merging the entity changes
    public abstract void beforeEdit(T oldEntity, T entity);

    // get a list of query predicates for lists
    protected abstract void getPredicatesFromRequest(List<Predicate> predicates, Root<T> root);

    // perform these actions after creating an entity
    public abstract void afterCreate(T entity);

    // used to perform transformations before sending back an entity in a response
    public abstract void beforeSend(T entity);

    // error message templates
    private static final String NOT_FOUND = "%1$s with ID %2$s not found.";
    private static final String NOT_AUTHORIZED_TO_CREATE = "Not authorized to create %1$s.";
    private static final String NOT_AUTHORIZED_TO_EDIT = "Not authorized to edit %1$s with ID %2$s";
    private static final String NOT_AUTHORIZED_TO_DELETE = "Not authorized to delete %1$s with ID %2$s.";
    private static final String VERSION_CONFLICT_ERROR = "%1$s with ID %2$s has since been edited.";

    ////////////////////////////////////GET/////////////////////////////////////////

    private void checkLoggedIn() {
        if (requiresLogin() && !isLoggedIn()) {
            throw new RequestProcessingException(Response.Status.UNAUTHORIZED, "You must be logged in to access this resource.");
        }
    }

    /**
     * Get a single entity with an ID
     *
     * @param id of the entity
     * @return the entity corresponding to the ID
     */
    @GET
    @Path("{id}")
    public Response get(@PathParam("id") UUID id) {
        checkLoggedIn();

        T entity = getEntityWithId(id);
        if (entity == null) {
            throw new RequestProcessingException(Response.Status.NOT_FOUND,
                    String.format(NOT_FOUND, getEntityName(), id));
        }

        beforeSend(entity);
        return Response.ok(entity).build();
    }


    /**
     * Helper method to get a single query parameter
     *
     * @param param name of the query parameter
     * @return the value assigned to the query parameter
     */
    private String getQueryParameter(String param) {
        List<String> params = getQueryParameters(param);
        return params != null && params.size() > 0 ? params.get(0) : null;
    }

    /**
     * Helper method to get a list of values associated with a query parameter
     *
     * @param param name of the query parameter
     * @return list of values assigned to query parameter
     */
    private List<String> getQueryParameters(String param) {
        return getContainerRequestContext().getUriInfo().getQueryParameters().get(param);
    }

    /**
     * Get the first record that should be returned
     *
     * @return an int corresponding to the first record to return
     */
    private int getStart() {
        String start = getQueryParameter(getStartQueryParameterName());
        if (start != null) {
            try {
                return Math.max(Integer.parseInt(start), 0);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Invalid start received", e);
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
        String countString = getQueryParameter(getCountQueryParameterName());
        Integer maxCount = getMaxPerPage();
        Integer count = null;
        if (countString != null) {
            try {
                count = Integer.parseInt(countString);
            } catch (NumberFormatException e) {
                LOG.fine(String.format("Invalid count passed to GET: %s", countString));
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
     * Return a list of type T to the client, including headers about pagination
     *
     * @return response with entity list and headers corresponding to pagination details
     */
    @GET
    public Response getList() {
        checkLoggedIn();

        Integer count = getCount();
        int start = getStart();

        // get the entities
        List<T> entities = getListOfEntities(count, start);
        entities.forEach(this::beforeSend);

        // count the total number of the results that would've been returned
        long totalCount = getTotalCountOfEntities();

        // return the filtered and mapped list of entities
        return Response.ok(entities)
                .header(getStartHeader(), start)
                .header(getCountHeader(), count)
                .header(getTotalCountHeader(), totalCount)
                .build();
    }

    /**
     * Return the entity with the ID, filtered by the predicates associated with the request
     *
     * @param id of the entity to get
     * @return entity of type T with ID id
     */
    private T getEntityWithId(UUID id) {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<T> cq = cb.createQuery(this.getEntityClass());
        Root<T> from = cq.from(this.getEntityClass());

        List<Predicate> predicates = getPredicatesFromRequest(from);

        predicates.add(cb.equal(from.get("id"), id));

        Predicate[] pArray = new Predicate[predicates.size()];
        predicates.toArray(pArray);

        List<T> entity = em.createQuery(cq.select(from).where(pArray)).getResultList();

        return (entity.size() == 1 ? entity.get(0) : null);
    }

    /**
     * Get the total count of entities in the database that match the predicates
     *
     * @return the total count of entities that match the predicates
     */
    private long getTotalCountOfEntities() {
        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<T> root = cq.from(this.getEntityClass());

        List<Predicate> predicates = getPredicatesFromRequest(root);
        Predicate[] pArray = new Predicate[predicates.size()];
        predicates.toArray(pArray);

        CriteriaQuery<Long> countQuery = cq.select(cb.count(root)).where(pArray);

        return em.createQuery(countQuery).getSingleResult();
    }

    /**
     * Get a list of entities with a maximum size of count, starting at start
     *
     * @param count max # of entities to get
     * @param start which entity to start at
     * @return a list of type T from the database
     */
    private List<T> getListOfEntities(Integer count, int start) {
        if (count != null && count <= 0) {
            return Collections.emptyList();
        }

        EntityManager em = getEntityManager();
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<T> cq = cb.createQuery(this.getEntityClass());
        Root<T> from = cq.from(this.getEntityClass());
        cq.select(from);

        List<Predicate> predicates = getPredicatesFromRequest(from);
        if (predicates.size() > 0) {
            Predicate[] pArray = new Predicate[predicates.size()];
            predicates.toArray(pArray);
            cq.where(pArray);
        }

        // parse the request to return the orders that should be applied
        List<Order> orderBys = getOrderFromRequest(from);
        if (orderBys.size() > 0) {
            Order[] oArray = new Order[orderBys.size()];
            orderBys.toArray(oArray);
            cq.orderBy(orderBys);
        }

        TypedQuery<T> query = em.createQuery(cq)
                .setFirstResult(start);

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
    private List<Order> getOrderFromRequest(Root<T> from) {
        List<Order> orders = new ArrayList<>();
        getOrderFromRequest(from, orders);
        return orders;
    }

    // throw an error if the object is null
    private void notNull(Object shouldNotBeNull, String nameOfParameter) {
        if (shouldNotBeNull == null) {
            throw new RequestProcessingException(
                    Response.Status.BAD_REQUEST,
                    String.format("%1$s should not be null.", nameOfParameter)
            );
        }
    }

    /**
     * Resolves sorting orders from the request by matching them to attribute on the model
     *
     * @param from root of the query
     */
    private void getOrderFromRequest(Root<T> from, List<Order> orders) {
        List<String> sorts = getQueryParameters(Pattern.quote(getSortQueryParameterName()));
        if (sorts == null || sorts.size() == 0) {
            return;
        }

        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();

        for (String sortOrder : sorts) {
            String[] pieces = sortOrder.split(Pattern.quote(getSortInfoSeparator()));
            if (pieces.length <= 1) {
                continue;
            }

            boolean asc = (pieces[0].equals("A"));

            javax.persistence.criteria.Path sortBy;
            try {
                LinkedList<String> sortAttributePieces = new LinkedList<>(Arrays.asList(pieces[1].split(getSortPathSeparator())));
                if (sortAttributePieces.size() > 1) {
                    Join j = from.join(sortAttributePieces.pop(), JoinType.LEFT);
                    while (sortAttributePieces.size() > 1) {
                        String attribute = sortAttributePieces.pop();
                        j = j.join(attribute, JoinType.LEFT);
                    }
                    sortBy = j.get(sortAttributePieces.get(0));
                } else {
                    sortBy = from.get(sortAttributePieces.get(0));
                }
            } catch (IllegalArgumentException e) {
                LOG.log(Level.WARNING, "Failed to parse sort: " + sortOrder, e);
                continue;
            }

            if (asc) {
                orders.add(cb.asc(sortBy));
            } else {
                orders.add(cb.desc(sortBy));
            }
            if (orders.size() > getMaxNumberOfSorts()) {
                break;
            }
        }
    }

    /**
     * Parse the request to get the query parameters to append to the GET requests
     *
     * @param root the root of the query
     * @return a list of predicates to apply to the query
     */
    private List<Predicate> getPredicatesFromRequest(Root<T> root) {
        List<Predicate> predicates = new ArrayList<>();
        getPredicatesFromRequest(predicates, root);
        return predicates;
    }

    ////////////////////////////////////GET/////////////////////////////////////////
    ///////////////////////////////////POST/////////////////////////////////////////

    /**
     * Create an entity
     *
     * @param entity data to persist
     * @return a response containing the saved entity
     */
    @POST
    public Response post(T entity) {
        checkLoggedIn();

        notNull(entity, getEntityName());
        if (!canCreate(entity)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    String.format(NOT_AUTHORIZED_TO_CREATE, getEntityName()));
        }
        beforeCreate(entity);
        List<String> errors = validateEntity(entity);
        if (errors.size() > 0) {
            String[] errs = new String[errors.size()];
            errors.toArray(errs);
            throw new RequestProcessingException(Response.Status.BAD_REQUEST, errs);
        }
        boolean needsTx = noTx();
        try {
            if (needsTx) {
                openTransaction();
            }
            getEntityManager().persist(entity);
            if (needsTx) {
                commit();
            }
            afterCreate(entity);
        } catch (Exception e) {
            if (needsTx) {
                rollback();
            }
            LOG.log(Level.SEVERE, "Failed to create entity", e);
            throw new RequestProcessingException(
                    Response.Status.CONFLICT,
                    translateExceptionToMessage(e)
            );
        }

        return get(entity.getId());
    }

    /**
     * Translates an exception to a human readable message
     *
     * @param e exception to translate
     * @return a human readable message from the exception, if recognized
     */
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
     * Validate an entity and return a list of validation errors
     *
     * @param entity to validate
     * @return a list of errors
     */
    private List<String> validateEntity(T entity) {
        List<String> errors = new ArrayList<>();
        validateEntity(errors, entity);
        return errors;
    }

    ///////////////////////////////////POST/////////////////////////////////////////
    ///////////////////////////////////PUT/////////////////////////////////////////


    /**
     * Save an entity that already exists
     *
     * @param id     of the entity to save
     * @param entity data to merge
     * @return saved entity
     */
    @PUT
    @Path("{id}")
    public Response put(@PathParam("id") UUID id, T entity) {
        checkLoggedIn();

        notNull(entity, getEntityName());
        T entityToEdit = getEntityWithId(id);
        // check the entity with this ID truly exists and is visible to the user
        if (entityToEdit == null) {
            throw new RequestProcessingException(Response.Status.NOT_FOUND,
                    String.format(NOT_FOUND, getEntityName(), id));
        }
        // check for permission to edit
        if (!canEdit(entityToEdit)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    String.format(NOT_AUTHORIZED_TO_EDIT, getEntityName(), id));
        }
        // check for version conflicts so we can give a more useful message
        if (entityToEdit.getVersion() != entity.getVersion()) {
            throw new RequestProcessingException(Response.Status.CONFLICT,
                    String.format(VERSION_CONFLICT_ERROR, getEntityName(), id));
        }
        // make sure the path param matches the id of the entity they are putting
        entity.setId(id);
        beforeEdit(entityToEdit, entity);
        List<String> errors = validateEntity(entity);
        if (errors.size() > 0) {
            String[] errs = new String[errors.size()];
            errors.toArray(errs);
            throw new RequestProcessingException(Response.Status.BAD_REQUEST, errs);
        }
        boolean needsTx = noTx();
        try {
            if (needsTx) {
                openTransaction();
            }
            getEntityManager().merge(entity);
            if (needsTx) {
                commit();
            }
        } catch (Exception e) {
            if (needsTx) {
                rollback();
            }
            LOG.log(Level.SEVERE, "Failed to save changes to single entity", e);
            rollback();
            throw new RequestProcessingException(
                    Response.Status.INTERNAL_SERVER_ERROR,
                    translateExceptionToMessage(e)
            );
        }

        return get(id);
    }

    ///////////////////////////////////PUT/////////////////////////////////////////
    ///////////////////////////////////DELETE/////////////////////////////////////////

    /**
     * Delete a single entity
     *
     * @param id of the entity to delete
     * @return 204 if successful, otherwise error message
     */
    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") UUID id) {
        checkLoggedIn();

        T entity = getEntityWithId(id);
        if (entity == null) {
            throw new RequestProcessingException(Response.Status.NOT_FOUND,
                    String.format(S_WITH_ID_S_NOT_FOUND, getEntityName(), id));
        }

        if (!canDelete(entity)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                    String.format(NOT_AUTHORIZED_TO_DELETE, getEntityName(), entity.getId()));
        }

        try {
            openTransaction();
            try {
                deleteEntity(entity);
            } catch (Exception e) {
                LOG.log(Level.SEVERE, "failed to delete single entity", e);
                throw new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR,
                        "Failed to delete resource", translateExceptionToMessage(e));
            }
            commit();
        } catch (Exception e) {
            rollback();
            LOG.log(Level.SEVERE, "Failed to delete resource", e);
            throw new RequestProcessingException(
                    Response.Status.CONFLICT,
                    translateExceptionToMessage(e)
            );
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    @DELETE
    public Response deleteAll() {
        checkLoggedIn();

        List<T> toDelete = getListOfEntities(null, 0);

        if (!toDelete.stream().allMatch(this::canDelete)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN, "You are not permitted to delete all the entities matching your request.");
        }

        try {
            openTransaction();
            toDelete.stream().forEach(this::deleteEntity);
            commit();
        } catch (Exception e) {
            rollback();
            LOG.log(Level.SEVERE, "Failed to delete resources", e);
            throw new RequestProcessingException(Response.Status.CONFLICT, "Failed to delete resource",
                    translateExceptionToMessage(e));
        }

        return Response.noContent().build();
    }

    // override this method delete by e.g. setting a field
    private void deleteEntity(T entityToDelete) {
        getEntityManager().remove(entityToDelete);
    }

    ///////////////////////////////////DELETE END/////////////////////////////////////////
    ////////////////////////////TRANSACTION HELPERS////////////////////////////////////////////////////
    private EntityTransaction etx;

    public void openTransaction() {
        if (etx != null) {
            throw new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR, "Transaction was opened twice.");
        }
        etx = getEntityManager().getTransaction();
        etx.begin();
    }

    public boolean noTx() {
        return (etx == null || !etx.isActive());
    }

    public void commit() {
        if (etx == null) {
            throw new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR, "Transaction was closed while not open.");
        }
        getEntityManager().flush();
        etx.commit();
        etx = null;
    }

    public void rollback() {
        if (etx == null) {
            return;
        }
        etx.rollback();
        etx = null;
    }

    /**
     * Return the name of the entity class
     *
     * @return name of the entity class, used in error messages
     */
    private String getEntityName() {
        return getEntityClass().getSimpleName();
    }
}

