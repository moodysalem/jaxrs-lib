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
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides a hibernate entity manager that is used for persisting classes to/from the database
 * <p>
 * Also runs migrations against the database when instantiated
 */
public class JAXRSEntityManagerFactory implements Factory<EntityManager> {

    private String url;
    private String user;
    private String password;
    private String persistenceUnit;
    private String changelogFile;
    private String context;
    private boolean showSql;
    private boolean validate;

    public JAXRSEntityManagerFactory(String url, String user, String password, String persistenceUnit,
                                     String changelogFile, boolean showSql, boolean validate, String context, Properties additionalProperties) {
        this.url = url;
        this.user = user;
        this.password = password;
        this.persistenceUnit = persistenceUnit;
        this.showSql = showSql;
        this.changelogFile = changelogFile;
        this.context = context;
        this.validate = validate;
        runMigrations();
        _emf = createEMF(additionalProperties);
    }

    private static final Logger LOG = Logger.getLogger(JAXRSEntityManagerFactory.class.getName());
    private static final String[] DRIVERS = new String[]{
            "com.mysql.jdbc.Driver",
            "org.postgresql.Driver",
            "oracle.jdbc.driver.OracleDriver",
            "com.microsoft.sqlserver.jdbc.SQLServerDriver"
    };

    private EntityManagerFactory _emf;

    static {
        // We need to do this only once so that all the JDBC drivers are on the classpath
        loadDrivers();
    }

    /**
     * Load all the JDBC drivers that are typically used in a hibernate application
     */
    private static void loadDrivers() {
        for (String driverName : DRIVERS) {
            try {
                Class.forName(driverName);
                LOG.info("JDBC Driver loaded: " + driverName);
            } catch (ClassNotFoundException e) {
                LOG.warning("JDBC Driver not found in classpath. This is benign if the driver is not needed: " + driverName);
            }
        }
    }

    /**
     * Create an entity manager factory which is used to provide entity managers to the requests
     */
    private EntityManagerFactory createEMF(Properties additionalProperties) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.connection.url", url);
        properties.setProperty("hibernate.connection.user", user);
        properties.setProperty("hibernate.connection.password", password);
        properties.setProperty("hibernate.connection.useUnicode", "true");

        if (showSql) {
            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");
        }

        if (validate) {
            properties.setProperty("hibernate.hbm2ddl.auto", "validate");
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

    /**
     * Run the migrations in the changelog associated with this entity manager
     */
    private void runMigrations() {
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

    @Override
    public EntityManager provide() {
        LOG.fine("Providing an entity manager");
        return _emf.createEntityManager();
    }

    @Override
    public void dispose(EntityManager entityManager) {
        LOG.fine("Disposing an entity manager");
        entityManager.close();
    }
}
