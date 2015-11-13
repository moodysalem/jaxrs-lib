import com.leaguekit.hibernate.model.BaseEntity;
import com.leaguekit.jaxrs.lib.BaseApplication;
import com.leaguekit.jaxrs.lib.factories.JAXRSEntityManagerFactory;
import com.leaguekit.jaxrs.lib.resources.EntityResource;
import com.leaguekit.jaxrs.lib.test.BaseTest;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.validator.constraints.NotBlank;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertTrue;

public class EntityResourceTest extends BaseTest {

    public static final String X_TOTAL_COUNT = "X-Total-Count";
    public static final String X_START = "X-Start";
    public static final String X_COUNT = "X-Count";
    public static final String START = "start";
    public static final String COUNT = "count";
    public static final String SORT = "sort";

    @javax.persistence.Entity
    @Table(name = "MyEntity")
    public static class MyEntity extends BaseEntity {
        @NotBlank
        @Column(name = "hometown", nullable = false, unique = true)
        private String hometown;

        public String getHometown() {
            return hometown;
        }

        public void setHometown(String hometown) {
            this.hometown = hometown;
        }
    }

    @Path("myentity")
    public static class MyEntityResource extends EntityResource<MyEntity> {

        @Context
        ContainerRequestContext req;

        @Inject
        EntityManager em;

        @Override
        protected ContainerRequestContext getContainerRequestContext() {
            return req;
        }

        @Override
        protected EntityManager getEntityManager() {
            return em;
        }

        @Override
        public String getSortQueryParameterName() {
            return SORT;
        }

        @Override
        public String getSortInfoSeparator() {
            return "\\|";
        }

        @Override
        public String getSortPathSeparator() {
            return "\\.";
        }

        @Override
        public int getMaxNumberOfSorts() {
            return 5;
        }

        @Override
        public String getFirstRecordQueryParameterName() {
            return START;
        }

        @Override
        public String getCountQueryParameterName() {
            return COUNT;
        }

        @Override
        public int getMaxPerPage() {
            return 500;
        }

        @Override
        public int getDefaultRecordsPerPage() {
            return 20;
        }

        @Override
        protected int getMaxBatchDeleteSize() {
            return 100;
        }

        @Override
        public String getFirstRecordHeader() {
            return X_START;
        }

        @Override
        public String getCountHeader() {
            return X_COUNT;
        }

        @Override
        public String getTotalCountHeader() {
            return X_TOTAL_COUNT;
        }

        @Override
        public boolean isLoggedIn() {
            return false;
        }

        @Override
        public Class<MyEntity> getEntityClass() {
            return MyEntity.class;
        }

        @Override
        public boolean requiresLogin() {
            return false;
        }

        @Override
        public boolean canCreate(MyEntity entity) {
            return !"ABC".equals(entity.getHometown());
        }

        @Override
        public boolean canEdit(MyEntity entity) {
            return true;
        }

        @Override
        public boolean canDelete(MyEntity entity) {
            return true;
        }

        @Override
        protected void validateEntity(List<String> errors, MyEntity entity) {

        }

        @Override
        public void beforeCreate(MyEntity entity) {

        }

        @Override
        public void beforeEdit(MyEntity oldEntity, MyEntity entity) {

        }

        @Override
        protected void getPredicatesFromRequest(List<Predicate> predicates, Root<MyEntity> root) {

        }

        @Override
        public void afterCreate(MyEntity entity) {

        }

        @Override
        public void beforeSend(MyEntity entity) {

        }
    }

    @Override
    public ResourceConfig getResourceConfig() {
        ResourceConfig rc = new BaseApplication() {
            @Override
            public boolean forceHttps() {
                return true;
            }

            @Override
            public boolean allowCORS() {
                return true;
            }
        };
        // bind the factory
        rc.register(new AbstractBinder() {
            @Override
            protected void configure() {
                bindFactory(new JAXRSEntityManagerFactory("jdbc:h2:mem:tester;DB_CLOSE_DELAY=-1", "sa", "sa", "mpu", "ertest/schema.xml", true, null))
                    .to(EntityManager.class).in(RequestScoped.class);
            }
        });
        // register the resource
        rc.register(MyEntityResource.class);
        return rc;
    }


    @Test
    public void testForbiddenPost() {
        WebTarget wt = target("myentity");
        MyEntity me = new MyEntity();
        me.setHometown("ABC");
        Response r = wt.request().post(Entity.json(me));
        assertTrue(r.getStatus() == 403);
    }

    @Test
    public void testPut() {
        WebTarget wt = target("myentity");
        MyEntity me = new MyEntity();
        me.setHometown("Chicago");
        me = wt.request().post(Entity.json(me), MyEntity.class);

        me.setHometown("Austin");
        me = wt.path(Long.toString(me.getId())).request().put(Entity.json(me), MyEntity.class);
        assertTrue(me.getVersion() == 1);
    }

    @Test
    public void testDelete() {
        WebTarget wt = target("myentity");
        MyEntity me = new MyEntity();
        me.setHometown("to delete");
        me = wt.request().post(Entity.json(me), MyEntity.class);

        Response del = wt.path(Long.toString(me.getId())).request().delete();
        assertTrue(del.getStatus() == 204);
    }

    @Test
    public void testEntityResource() {
        WebTarget wt = target("myentity");
        assertTrue(wt.request().get().getStatus() == 200);

        int i = 0;
        while (i < 100) {
            MyEntity me = new MyEntity();
            me.setHometown("#" + i);
            me = wt.request().post(Entity.json(me), MyEntity.class);
            assertTrue(me.getId() > 0 && me.getHometown().equals("#" + i));
            i++;
        }

        // unique name
        MyEntity me = new MyEntity();
        me.setHometown("#1");
        Response constraintViolation = wt.request().post(Entity.json(me));
        assert constraintViolation.getStatus() != 200;

        // empty name
        MyEntity me2 = new MyEntity();
        me.setHometown("   ");
        Response emptyName = wt.request().post(Entity.json(me2));
        assert emptyName.getStatus() != 200;

        Response r = wt.queryParam(COUNT, 40).queryParam(START, 20)
            .request().get();
        assertTrue(r.readEntity(List.class).size() == 40);
        assertTrue(r.getStatus() == 200);
        assertTrue(r.getHeaderString(X_TOTAL_COUNT).equals("100"));
        assertTrue(r.getHeaderString(X_START).equals("20"));
        assertTrue(r.getHeaderString(X_COUNT).equals("40"));


        Response list = wt.queryParam(COUNT, 1000).queryParam(START, 20)
            .request().get();
        assertTrue(list.getHeaderString(X_COUNT).equals("500"));

        List<MyEntity> lme = list.readEntity(new GenericType<List<MyEntity>>() {
        });
        String ids = lme.stream().limit(100).map(MyEntity::getId).map((id) -> Long.toString(id)).collect(Collectors.joining(","));

        Response del = wt.path(ids).request().delete();
        assertTrue(del.getStatus() == 204);

    }

}
