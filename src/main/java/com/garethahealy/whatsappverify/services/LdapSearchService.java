package com.garethahealy.whatsappverify.services;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles searching Red Hat LDAP
 */
@ApplicationScoped
public class LdapSearchService {

    public static class AttributeKeys {
        public static final String PrimaryMail = "rhatPrimaryMail";
    }

    @Inject
    Logger logger;

    @ConfigProperty(name = "redhat.ldap.dn")
    String ldapDn;

    @ConfigProperty(name = "redhat.ldap.connection")
    String ldapConnection;

    @ConfigProperty(name = "redhat.ldap.warmup-user")
    String ldapWarmupUser;

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private Dn systemDn;

    public LdapSearchService() {
    }

    /**
     * Attempts to connect to LDAP
     */
    @PostConstruct
    void init() {
        try {
            systemDn = new Dn(ldapDn);
            try (LdapConnection connection = open()) {
                try (EntryCursor cursor = connection.search(systemDn, "(uid=" + ldapWarmupUser + ")", SearchScope.SUBTREE, "dn")) {
                    for (Entry entry : cursor) {
                        logger.infof("Warmup found %s", entry.getDn());
                        warmedUp.set(true);
                        break;
                    }
                }
            }
        } catch (IOException | LdapException e) {
            logger.error("Failed to open connection to LDAP", e);
        }
    }

    /**
     * Can connect to LDAP
     *
     * @return
     */
    public boolean canConnect() {
        if (!warmedUp.get()) {
            init();
        }

        return warmedUp.get();
    }

    /**
     * Create a LdapConnection
     *
     * @return
     */
    public LdapConnection open() {
        return new LdapNetworkConnection(ldapConnection);
    }

    /**
     * Search based on mobile=${param}
     *
     * @param connection
     * @param number
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnMobile(LdapConnection connection, String number) throws LdapException, IOException {
        String filter = "(mobile=" + number + ")";
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on homePhone=${param}
     *
     * @param connection
     * @param number
     * @return
     * @throws LdapException
     * @throws IOException
     */
    public String searchOnHomePhone(LdapConnection connection, String number) throws LdapException, IOException {
        String filter = "(homePhone=" + number + ")";
        return searchAndGetPrimaryMail(connection, filter);
    }

    /**
     * Search based on a filter and return the PrimaryMail
     *
     * @param connection
     * @param filter
     * @return
     * @throws LdapException
     * @throws IOException
     */
    private String searchAndGetPrimaryMail(LdapConnection connection, String filter) throws LdapException, IOException {
        String answer = "";

        try (EntryCursor cursor = connection.search(systemDn, filter, SearchScope.SUBTREE, AttributeKeys.PrimaryMail)) {
            int count = 0;
            for (Entry entry : cursor) {
                logger.debugf("Found %s", filter);

                for (Attribute found : entry.getAttributes()) {
                    if (found.getId().equalsIgnoreCase(AttributeKeys.PrimaryMail)) {
                        logger.debugf("- returning %s == %s", AttributeKeys.PrimaryMail, found.get().toString());
                        answer = found.get().toString();
                    }
                }

                count++;
                if (count >= 2) {
                    throw new LdapException("cursor returned multiple entries for: " + filter);
                }
            }
        } catch (LdapInvalidSearchFilterException ex) {
            logger.error("Unable to search on " + filter, ex);
        }

        return answer;
    }
}
