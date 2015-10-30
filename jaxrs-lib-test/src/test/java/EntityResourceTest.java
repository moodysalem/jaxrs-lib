import com.leaguekit.hibernate.model.BaseEntity;
import com.leaguekit.jaxrs.lib.factories.JAXRSEntityManagerFactory;
import com.leaguekit.jaxrs.lib.resources.EntityResource;
import com.leaguekit.jaxrs.lib.test.BaseTest;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Table;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.List;

public class EntityResourceTest extends BaseTest {
    @Entity
    @Table(name = "MyEntity")
    public static class MyEntity extends BaseEntity {
        @Column(name = "hometown")
        private String hometown;

        public String getHometown() {
            return hometown;
        }

        public void setHometown(String hometown) {
            this.hometown = hometown;
        }
    }

    @Path("myentity")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
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
            return "sort";
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
            return "start";
        }

        @Override
        public String getCountQueryParameterName() {
            return "count";
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
        public String getFirstRecordHeader() {
            return "X-Start";
        }

        @Override
        public String getCountHeader() {
            return "X-Count";
        }

        @Override
        public String getTotalCountHeader() {
            return "X-Total-Count";
        }

        @Override
        public Class<MyEntity> getEntityClass() {
            return MyEntity.class;
        }

        @Override
        public boolean canCreate(MyEntity entity) {
            return true;
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
        ResourceConfig rc = new ResourceConfig();
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
    public void testEntityResource() {
        target("myentity").request().get();
    }
}
