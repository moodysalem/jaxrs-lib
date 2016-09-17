package com.moodysalem.jaxrs.lib.factories;

import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.LiquibaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.glassfish.hk2.api.Factory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a hibernate entity manager that is used for persisting classes to/from the database
 * <p>
 * Also runs migrations against the database when instantiated
 */
public class JAXRSEntityManagerFactory implements Factory<EntityManager> {
    // STATIC STUFF
    private static final Logger LOG = Logger.getLogger(JAXRSEntityManagerFactory.class.getName());
    private static final String[] DRIVERS = new String[]{
            "com.mysql.jdbc.Driver",
            "org.postgresql.Driver",
            "oracle.jdbc.driver.OracleDriver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    };

    /**
     * Load all the JDBC drivers that are typically used in a hibernate application
     */
    private static void loadDrivers() {
        for (final String driverName : DRIVERS) {
            try {
                Class.forName(driverName);
                LOG.info("JDBC Driver loaded: " + driverName);
            } catch (ClassNotFoundException e) {
                LOG.warning("JDBC Driver not found in classpath. This is benign if the driver is not needed: " + driverName);
            }
        }
    }

    static {
        // We need to do this only once so that all the JDBC drivers are on the classpath
        loadDrivers();
    }

    // the only way to use this is to first call the static builder method and then call build from the static builder
    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static class Builder {
        private String name, url, user, persistenceUnit, changelogFile, context, password = "";
        private Properties additionalProperties;
        private boolean showSql;

        public JAXRSEntityManagerFactory build() {
            return new JAXRSEntityManagerFactory(
                    name, url, user, password, persistenceUnit, changelogFile, showSql,
                    context, additionalProperties
            );
        }

        private Builder(String name) {
            if (name == null) {
                throw new NullPointerException("Name is required");
            }
            this.name = name;
        }

        public Builder withUrl(String url) {
            this.url = url;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public Builder withPersistenceUnit(String persistenceUnit) {
            this.persistenceUnit = persistenceUnit;
            return this;
        }

        public Builder withChangelogFile(String changelogFile) {
            this.changelogFile = changelogFile;
            return this;
        }

        public Builder withShowSql(boolean showSql) {
            this.showSql = showSql;
            return this;
        }

        public Builder withContext(String context) {
            this.context = context;
            return this;
        }

        public Builder withAdditionalProperties(Properties additionalProperties) {
            this.additionalProperties = additionalProperties;
            return this;
        }

        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }
    }


    /**
     * Create an entity manager factory which is used to provide entity managers to the requests
     */
    private static EntityManagerFactory createEMF(String url,
                                                  String user,
                                                  String password,
                                                  String persistenceUnit,
                                                  boolean showSql,
                                                  Properties additionalProperties) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", url);
        properties.setProperty("hibernate.connection.user", user);
        properties.setProperty("hibernate.connection.password", password);
        properties.setProperty("hibernate.connection.useUnicode", "true");

        if (showSql) {
            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");
        }

        // database connection pool
        properties.setProperty("hibernate.c3p0.min_size", "1");
        properties.setProperty("hibernate.c3p0.max_size", "100");
        properties.setProperty("hibernate.c3p0.idle_test_period", "1000");
        properties.setProperty("hibernate.c3p0.timeout", "100");
        properties.setProperty("hibernate.c3p0.max_statements", "50");
        properties.setProperty("hibernate.default_batch_fetch_size", "32");

        if (additionalProperties != null) {
            properties.putAll(additionalProperties);
        }

        return Persistence.createEntityManagerFactory(persistenceUnit, properties);
    }

    private final String name;
    private final EntityManagerFactory _emf;

    private JAXRSEntityManagerFactory(String name, String url, String user, String password, String persistenceUnit,
                                      String changelogFile, boolean showSql, String context,
                                      Properties additionalProperties) {
        this.name = name;
        runMigrations(changelogFile, url, user, password, context);
        _emf = createEMF(url, user, password, persistenceUnit, showSql, additionalProperties);
    }

    /**
     * Run the migrations in the changelog associated with this entity manager
     */
    private void runMigrations(String changelogFile, String url, String user, String password, String context) {
        if (changelogFile != null) {
            try (Connection c = DriverManager.getConnection(url, user, password)) {
                LOG.info("Running Migrations");
                // first run the liquibase migrations against the database
                Liquibase lb = new Liquibase(changelogFile, new ClassLoaderResourceAccessor(), new JdbcConnection(c));
                lb.update(context);
            } catch (LiquibaseException e) {
                LOG.log(Level.SEVERE, "Liquibase exception thrown while trying to run migrations", e);
            } catch (SQLException e) {
                LOG.log(Level.SEVERE, "SQL Exception thrown while trying to open a connection", e);
            }
        } else {
            LOG.info("No changelog file specified, not running migrations.");
        }
    }

    // used for counting the entity managers so we can make sure they're all closed
    private final AtomicLong COUNTER = new AtomicLong(0);
    private final Map<EntityManager, Long> ENTITY_MANAGER_LONG_MAP = new HashMap<>();

    @Override
    public EntityManager provide() {
        final long next = COUNTER.incrementAndGet();
        LOG.fine(String.format("%s: Providing an entity manager: %s", name, next));
        final EntityManager toReturn = _emf.createEntityManager();
        ENTITY_MANAGER_LONG_MAP.put(toReturn, next);
        return toReturn;
    }

    @Override
    public void dispose(EntityManager entityManager) {
        final Long next = ENTITY_MANAGER_LONG_MAP.remove(entityManager);
        LOG.fine(String.format("%s: Disposing an entity manager: #%s", name, next));
        entityManager.close();
    }
}
