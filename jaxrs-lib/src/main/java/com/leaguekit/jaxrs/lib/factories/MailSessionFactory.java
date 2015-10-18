package com.leaguekit.jaxrs.lib.factories;

import org.glassfish.hk2.api.Factory;

import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import java.util.Properties;
import java.util.logging.Logger;

public class MailSessionFactory implements Factory<Session> {
    private static final Logger LOG = Logger.getLogger(MailSessionFactory.class.getName());

    private Session session;

    public MailSessionFactory(String host, final String username, final String password, int port) {
        // Create a Properties object to contain connection configuration information.
        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        // Set properties indicating that we want to use STARTTLS to encrypt the connection.
        // The SMTP session will begin on an unencrypted connection, and then the client
        // will issue a STARTTLS command to upgrade to an encrypted connection.
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");

        LOG.info("Created mail session with the following properties: " + props.toString());

        // Get the Session object
        session = Session.getInstance(props,
            new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            });
    }

    @Override
    public Session provide() {
        LOG.info("Distributing mail session.");
        return session;
    }

    @Override
    public void dispose(Session session) {
    }
}
