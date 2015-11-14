package com.moodysalem.jaxrs.lib.resources;

import com.moodysalem.hibernate.model.BaseEntity;
import com.moodysalem.jaxrs.lib.exceptions.RequestProcessingException;
import org.hibernate.exception.ConstraintViolationException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceException;
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

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public abstract class EntityResource<T extends BaseEntity> {

    private static final Logger LOG = Logger.getLogger(EntityResource.class.getName());

    protected abstract ContainerRequestContext getContainerRequestContext();

    protected abstract EntityManager getEntityManager();

    //////////////////////////////CONSTANTS//////////////////////////////////////////

    // sorting behavior
    public abstract String getSortQueryParameterName();

    public abstract String getSortInfoSeparator();

    public abstract String getSortPathSeparator();

    public abstract int getMaxNumberOfSorts();

    // paging behavior
    public abstract String getFirstRecordQueryParameterName();

    public abstract String getCountQueryParameterName();

    public abstract int getMaxPerPage();

    public abstract int getDefaultRecordsPerPage();

    // the number of records that can be deleted in a single request
    protected abstract int getMaxBatchDeleteSize();

    // name of the header that should indicate the first record returned
    public abstract String getFirstRecordHeader();

    // name of the header that should indicate the count of records returned
    public abstract String getCountHeader();

    // name of the header that should include the total count of records that fit the criteria
    public abstract String getTotalCountHeader();

    // whether the user is authenticated
    public abstract boolean isLoggedIn();

    // error messages
    public static final String NOT_FOUND = "%1$s with ID %2$s not found.";
    public static final String NOT_AUTHORIZED_TO_CREATE = "Not authorized to create %1$s.";
    public static final String NOT_AUTHORIZED_TO_EDIT = "Not authorized to edit %1$s with ID %2$s";
    public static final String ID_SHOULD_NOT_BE_INCLUDED_IN_A_POST = "ID should not be included in a post.";
    private static final String NOT_AUTHORIZED_TO_DELETE = "Not authorized to delete %1$s with ID %2$s.";
    public static final String VERSION_CONFLICT_ERROR = "%1$s with ID %2$s has since been edited.";

    ////////////////////////////////////GET/////////////////////////////////////////

    private void mustBeLoggedIn() {
        if (requiresLogin() && !isLoggedIn()) {
            throw new RequestProcessingException(Response.Status.UNAUTHORIZED, "You must be logged in to access this resource.");
        }
    }

    @GET
    @Path("{id}")
    public Response get(@PathParam("id") long id) {
        mustBeLoggedIn();

        T entity = getEntityWithId(id);
        if (entity == null) {
            throw new RequestProcessingException(Response.Status.NOT_FOUND,
                String.format(NOT_FOUND, getEntityName(), id));
        }
        beforeSend(entity);
        return Response.ok(entity).build();
    }


    private String getQueryParameter(String param) {
        List<String> params = getQueryParameters(param);
        return params != null && params.size() > 0 ? params.get(0) : null;
    }

    private List<String> getQueryParameters(String param) {
        return getContainerRequestContext().getUriInfo().getQueryParameters().get(param);
    }

    private int getStart() {
        String start = getQueryParameter(getFirstRecordQueryParameterName());
        if (start != null) {
            try {
                return Math.max(Integer.parseInt(start), 0);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Invalid start received", e);
            }
        }
        return 0;
    }

    private int getCount() {
        String count = getQueryParameter(getCountQueryParameterName());
        if (count != null) {
            try {
                return Math.max(Math.min(Integer.parseInt(count), getMaxPerPage()), 1);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Invalid count received", e);
            }
        }
        return getDefaultRecordsPerPage();
    }


    @GET
    public Response getList() {
        mustBeLoggedIn();

        int count = getCount();
        int start = getStart();

        // get the entities
        List<T> entities = getListOfEntities(count, start);
        entities.forEach(this::beforeSend);

        // count the total number of the results that would've been returned
        long totalCount = getTotalCountOfEntities();

        // return the filtered and mapped list of entities
        return Response.ok(entities)
            .header(getFirstRecordHeader(), start)
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
    protected T getEntityWithId(long id) {
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


    protected long getTotalCountOfEntities() {
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

    protected List<T> getListOfEntities(int numRecords, int firstRecord) {
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

        return em.createQuery(cq)
            .setMaxResults(numRecords)
            .setFirstResult(firstRecord)
            .getResultList();
    }


    protected List<Order> getOrderFromRequest(Root<T> from) {
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
    protected void getOrderFromRequest(Root<T> from, List<Order> orders) {
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

    // parse the request to return the predicates that should result from the parameters
    private List<Predicate> getPredicatesFromRequest(Root<T> root) {
        List<Predicate> predicates = new ArrayList<>();
        getPredicatesFromRequest(predicates, root);
        return predicates;
    }

    ////////////////////////////////////GET/////////////////////////////////////////
    ///////////////////////////////////POST/////////////////////////////////////////

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response post(T entity) {
        mustBeLoggedIn();

        notNull(entity, getEntityName());
        if (entity.getId() != 0) {
            throw new RequestProcessingException(Response.Status.BAD_REQUEST,
                ID_SHOULD_NOT_BE_INCLUDED_IN_A_POST);
        }
        if (!canCreate(entity)) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN,
                String.format(NOT_AUTHORIZED_TO_CREATE, getEntityName()));
        }
        beforeCreate(entity);
        List<String> errors = validateEntity(entity);
        if (errors.size() > 0) {
            throw new RequestProcessingException(Response.Status.BAD_REQUEST, errors);
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
        return Response.ok(entity).build();
    }

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

    // helper method to create a list so it's never null
    private List<String> validateEntity(T entity) {
        List<String> errors = new ArrayList<>();
        validateEntity(errors, entity);
        return errors;
    }


    ///////////////////////////////////POST/////////////////////////////////////////


    @PUT
    @Path("{id}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response put(@PathParam("id") long id, T entity) {
        mustBeLoggedIn();

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
            throw new RequestProcessingException(Response.Status.BAD_REQUEST, errors);
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

    @SuppressWarnings("unchecked")
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response putAll(List<T> entities) {
        mustBeLoggedIn();

        List<T> savedEntities = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        try {
            openTransaction();
            for (T entity : entities) {
                try {
                    if (entity.getId() != 0) {
                        savedEntities.add((T) put(entity.getId(), entity).getEntity());
                    } else {
                        savedEntities.add((T) post(entity).getEntity());
                    }
                } catch (Exception e) {
                    LOG.log(Level.SEVERE, "Failed to save entity in collection", e);
                    errors.add(translateExceptionToMessage(e));
                }
            }
            if (errors.size() == 0) {
                commit();
            } else {
                rollback();
            }
        } catch (Exception e) {
            rollback();
            LOG.log(Level.SEVERE, "Failed to save collection edits", e);
            errors.add(translateExceptionToMessage(e));
        }
        if (errors.size() > 0) {
            throw new RequestProcessingException(errors);
        }
        return Response.ok(savedEntities).build();
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") String csIds) {
        mustBeLoggedIn();

        String[] ids = csIds.split(Pattern.quote(","));
        Set<Long> idSet = new HashSet<>();
        for (String i : ids) {
            try {
                idSet.add(Long.parseLong(i));
            } catch (NumberFormatException e) {
                throw new RequestProcessingException(Response.Status.BAD_REQUEST, String.format("Invalid ID passed to delete: %s.", i));
            }
        }

        // verify they're not deleting too many
        if (idSet.size() > getMaxBatchDeleteSize()) {
            throw new RequestProcessingException(
                Response.Status.BAD_REQUEST,
                String.format("Number of IDs exceeds the maximum delete batch size of %d", getMaxBatchDeleteSize())
            );
        }

        List<String> errors = new ArrayList<>();

        Set<T> entitiesToDelete = new HashSet<>();
        for (Long i : idSet) {
            T entity = getEntityWithId(i);
            if (entity == null) {
                errors.add(String.format(NOT_FOUND, getEntityName(), i));
            } else {
                entitiesToDelete.add(entity);
            }
        }

        if (errors.size() > 0) {
            throw new RequestProcessingException(Response.Status.NOT_FOUND, errors);
        }

        for (T ent : entitiesToDelete) {
            if (!canDelete(ent)) {
                errors.add(String.format(NOT_AUTHORIZED_TO_DELETE, getEntityName(), ent.getId()));
            }
        }

        if (errors.size() > 0) {
            throw new RequestProcessingException(Response.Status.FORBIDDEN, errors);
        }

        try {
            openTransaction();
            for (T entity : entitiesToDelete) {
                try {
                    deleteEntity(entity);
                } catch (Exception e) {
                    errors.add(translateExceptionToMessage(e));
                }
            }
            // no errors encountered, do commit
            if (errors.size() == 0) {
                commit();
            } else {
                //otherwise rollback
                rollback();
            }
        } catch (Exception e) {
            rollback();
            LOG.log(Level.SEVERE, "Failed to delete resource", e);
            throw new RequestProcessingException(
                Response.Status.CONFLICT,
                translateExceptionToMessage(e)
            );
        }

        if (errors.size() > 0) {
            throw new RequestProcessingException(Response.Status.CONFLICT, errors);
        }

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    // override to delete by setting a field
    protected void deleteEntity(T entityToDelete) {
        getEntityManager().remove(entityToDelete);
    }

    ////////////////////////////TRANSACTION HELPERS////////////////////////////////////////////////////
    private EntityTransaction etx;

    protected void openTransaction() {
        if (etx != null) {
            throw new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR, "Transaction was opened twice.");
        }
        etx = getEntityManager().getTransaction();
        etx.begin();
    }

    protected boolean noTx() {
        return (etx == null || !etx.isActive());
    }

    protected void commit() {
        if (etx == null) {
            throw new RequestProcessingException(Response.Status.INTERNAL_SERVER_ERROR, "Transaction was closed while not open.");
        }
        getEntityManager().flush();
        etx.commit();
        etx = null;
    }

    protected void rollback() {
        if (etx == null) {
            return;
        }
        etx.rollback();
        etx = null;
    }
    ////////////////////////////TRANSACTION HELPERS////////////////////////////////////////////////////

    public abstract Class<T> getEntityClass();

    public abstract boolean requiresLogin();

    public abstract boolean canCreate(T entity);

    public abstract boolean canEdit(T entity);

    public abstract boolean canDelete(T entity);

    protected abstract void validateEntity(List<String> errors, T entity);

    public abstract void beforeCreate(T entity);

    public abstract void beforeEdit(T oldEntity, T entity);

    protected abstract void getPredicatesFromRequest(List<Predicate> predicates, Root<T> root);

    public abstract void afterCreate(T entity);

    public abstract void beforeSend(T entity);

    protected String getEntityName() {
        return getEntityClass().getSimpleName();
    }
}
