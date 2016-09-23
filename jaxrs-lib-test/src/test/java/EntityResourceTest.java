import com.moodysalem.hibernate.model.VersionedEntity;
import com.moodysalem.jaxrs.lib.BaseApplication;
import com.moodysalem.jaxrs.lib.exceptionmappers.ErrorResponse;
import com.moodysalem.jaxrs.lib.factories.JAXRSEntityManagerFactory;
import com.moodysalem.jaxrs.lib.resources.VersionedEntityResource;
import com.moodysalem.jaxrs.lib.resources.config.PaginationParameterConfiguration;
import com.moodysalem.jaxrs.lib.test.BaseTest;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.validator.constraints.NotBlank;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.*;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.testng.AssertJUnit.assertTrue;

public class EntityResourceTest extends BaseTest {

    public static final String X_TOTAL_COUNT = "X-Total-Count";
    public static final String X_START = "X-Start";
    public static final String X_COUNT = "X-Count";
    public static final String START = "start";
    public static final String COUNT = "count";

    private static final PaginationParameterConfiguration paginationConfig =
            new PaginationParameterConfiguration(START, COUNT, X_START, X_COUNT, X_TOTAL_COUNT, 500);

    @javax.persistence.Entity
    @Table(name = "MyEntity")
    public static class MyEntity extends VersionedEntity {
        @NotBlank
        @Column(name = "hometown", nullable = false, unique = true)
        private String hometown;

        @Column(name = "string")
        @ElementCollection
        @CollectionTable(name = "EntityStrings", joinColumns = {
                @JoinColumn(name = "entityId")
        })
        private Set<String> strings;

        public String getHometown() {
            return hometown;
        }

        public void setHometown(String hometown) {
            this.hometown = hometown;
        }

        public Set<String> getStrings() {
            return strings;
        }

        public void setStrings(Set<String> strings) {
            this.strings = strings;
        }
    }


    @Path("myentity")
    public static class MyEntityResource extends VersionedEntityResource<MyEntity> {
        @Context
        private ContainerRequestContext req;

        @Inject
        private EntityManager em;

        @Override
        public PaginationParameterConfiguration getPaginationConfiguration() {
            return paginationConfig;
        }

        @Override
        public boolean canDelete(MyEntity entity) {
            return true;
        }

        @Override
        public void beforeMerge(MyEntity oldData, MyEntity newData) {

        }

        @Override
        public void getPredicatesFromRequest(List<Predicate> predicates, Root<MyEntity> root) {

        }

        @Override
        public void beforeSend(List<MyEntity> entity) {

        }

        @Override
        public void afterMerge(MyEntity entity) {

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
        public boolean canMerge(MyEntity oldData, MyEntity newData) {
            return !"ABC".equals(newData.getHometown());
        }

        @Override
        public ContainerRequestContext getContainerRequestContext() {
            return req;
        }

        @Override
        public EntityManager getEntityManager() {
            return em;
        }
    }

    @Override
    public ResourceConfig getResourceConfig() {
        ResourceConfig rc = new BaseApplication() {
            @Override
            public boolean forceLoadBalancerHTTPS() {
                return false;
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
                bindFactory(JAXRSEntityManagerFactory.builder("my em")
                        .withUrl("jdbc:h2:mem:tester;DB_CLOSE_DELAY=-1")
                        .withUser("sa").withPassword("sa")
                        .withPersistenceUnit("mpu")
                        .withChangelogFile("ertest/schema.xml")
                        .withShowSql(true)
                        .build())
                        .to(EntityManager.class).in(RequestScoped.class).proxy(true);
            }
        });
        // register the resource
        rc.register(MyEntityResource.class);
        return rc;
    }


    @Test
    public void testForbiddenPost() {
        final WebTarget wt = target("myentity");
        final MyEntity me = new MyEntity();
        me.setHometown("ABC");
        Response r = wt.request().post(Entity.json(Arrays.asList(me)));
        assertTrue(r.getStatus() == 403);

        final ErrorResponse error = r.readEntity(ErrorResponse.class);
        assertTrue(error.getStatusCode() == 403);
        assertTrue(error.getRequestErrors().size() == 1);
        assertTrue(error.getNumErrors() == error.getRequestErrors().size());
        assertTrue(error.getRequestErrors().iterator().next().getMessage() != null);
    }

    @Test
    public void testPut() {
        final WebTarget wt = target("myentity");
        MyEntity me = new MyEntity();
        me.setHometown("Chicago");
        me.setStrings(new HashSet<>());
        me.getStrings().add("hello");
        me.getStrings().add("world");
        me = wt.request().post(Entity.json(Arrays.asList(me)), new GenericType<List<MyEntity>>() {
        }).get(0);

        me.setHometown("Austin");
        assertTrue("PUT is no longer supported",
                wt.path(me.getId().toString()).request().put(Entity.json(Arrays.asList(me))).getStatus() == 405);
    }

    @Test
    public void testDelete() {
        final WebTarget wt = target("myentity");
        MyEntity me = new MyEntity();
        me.setHometown("to delete");
        me = wt.request().post(Entity.json(Collections.singletonList(me)), new GenericType<List<MyEntity>>() {
        }).get(0);

        Response del = wt.path(me.getId().toString()).request().delete();
        assertTrue(del.getStatus() == 204);
    }

    @Test
    public void testEntityResource() {
        final WebTarget wt = target("myentity");
        assertTrue(wt.request().get().getStatus() == 200);

        // create a list and fill it with entities
        List<MyEntity> list = new ArrayList<>(100);
        for (int i = 0; i < 100; i++) {
            final MyEntity me = new MyEntity();
            me.setHometown("#" + i);
            list.add(i, me);

        }
        list = wt.request().post(Entity.json(list), new GenericType<List<MyEntity>>() {
        });

        // unique name
        MyEntity me = new MyEntity();
        me.setHometown("#1");
        Response constraintViolation = wt.request().post(Entity.json(Collections.singletonList(me)));
        assert constraintViolation.getStatus() != 200;

        // empty name
        MyEntity me2 = new MyEntity();
        me.setHometown("   ");
        Response emptyName = wt.request().post(Entity.json(Collections.singletonList(me2)));
        assert emptyName.getStatus() != 200;

        Response r = wt.queryParam(COUNT, 40).queryParam(START, 20)
                .request().get();
        final List<MyEntity> get = r.readEntity(new GenericType<List<MyEntity>>() {
        });
        assertTrue(get.size() == 40);
        assertTrue(r.getStatus() == 200);
        assertTrue(r.getHeaderString(X_TOTAL_COUNT).equals("100"));
        assertTrue(r.getHeaderString(X_START).equals("20"));
        assertTrue(r.getHeaderString(X_COUNT).equals("40"));

        // do a big request that exceeds the max count
        final Response bigRequest = wt.queryParam(COUNT, 1000).queryParam(START, 20)
                .request().get();
        assertTrue(bigRequest.getHeaderString(X_COUNT).equals("500"));
        assertTrue(bigRequest.getHeaderString(X_START).equals("20"));

        // when count isn't specified, make sure the max page size is enforced
        final Response noCount = wt.queryParam(START, 20)
                .request().get();
        assertTrue(noCount.getHeaderString(X_COUNT).equals("500"));
        assertTrue(noCount.getHeaderString(X_START).equals("20"));

        // delete them all
        Response del = wt.request().delete();
        assertTrue(del.getStatus() == 204);

        List<MyEntity> afterdelete = wt.request().get()
                .readEntity(new GenericType<List<MyEntity>>() {
                });
        assertTrue(afterdelete.size() == 0);
    }

}
