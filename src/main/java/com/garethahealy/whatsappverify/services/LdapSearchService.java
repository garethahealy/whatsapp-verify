package com.garethahealy.whatsappverify.services;

import com.garethahealy.whatsappverify.factories.LdapConnectionFactory;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidSearchFilterException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.List;

/**
 * Handles searching Red Hat LDAP
 */
@ApplicationScoped
public class LdapSearchService {

    public static class AttributeKeys {
        public static final String PrimaryMail = "rhatPrimaryMail";
    }

    private final Logger logger;
    private final LdapConnectionFactory connectionFactory;

    @Inject
    public LdapSearchService(Logger logger, LdapConnectionFactory connectionFactory) {
        this.logger = logger;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Can connect to LDAP
     *
     * @return
     */
    public boolean canConnect() {
        return connectionFactory.canConnect();
    }

    /**
     * Create a LdapConnection
     *
     * @return
     */
    public LdapConnection open() {
        return connectionFactory.open();
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
        FilterBuilder filter = FilterBuilder.equal("mobile", number);
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
        FilterBuilder filter = FilterBuilder.equal("homePhone", number);
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
    private String searchAndGetPrimaryMail(LdapConnection connection, FilterBuilder filter) throws LdapException, IOException {
        String answer = "";

        try {
            List<Attribute> attributes = connectionFactory.search(connection, filter, AttributeKeys.PrimaryMail);

            for (Attribute found : attributes) {
                if (found.getId().equalsIgnoreCase(AttributeKeys.PrimaryMail)) {
                    logger.debugf("- returning %s == %s", AttributeKeys.PrimaryMail, found.get().toString());
                    answer = found.get().toString();
                    break;
                }
            }
        } catch (LdapInvalidSearchFilterException ex) {
            logger.error("Unable to search on " + filter, ex);
        }

        return answer;
    }
}
