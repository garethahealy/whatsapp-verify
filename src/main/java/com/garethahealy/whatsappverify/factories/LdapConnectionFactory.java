package com.garethahealy.whatsappverify.factories;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class LdapConnectionFactory {

    private final Logger logger;
    private final String ldapConnection;
    private final String ldapDn;
    private final String ldapWarmupUser;

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private volatile Dn systemDn;

    @Inject
    public LdapConnectionFactory(Logger logger, @ConfigProperty(name = "redhat.ldap.connection") String ldapConnection,
            @ConfigProperty(name = "redhat.ldap.dn") String ldapDn, @ConfigProperty(name = "redhat.ldap.warmup-user") String ldapWarmupUser) {
        this.logger = logger;
        this.ldapConnection = ldapConnection;
        this.ldapDn = ldapDn;
        this.ldapWarmupUser = ldapWarmupUser;
    }

    @PostConstruct
    void init() {
        ensureWarmedUp();
    }

    public boolean canConnect() {
        if (!warmedUp.get()) {
            ensureWarmedUp();
        }

        return warmedUp.get();
    }

    public LdapConnection open() {
        return new LdapNetworkConnection(ldapConnection);
    }

    public Dn getSystemDn() throws LdapException {
        if (systemDn == null) {
            systemDn = new Dn(ldapDn);
        }

        return systemDn;
    }

    private void ensureWarmedUp() {
        try {
            Dn dn = getSystemDn();
            try (LdapConnection connection = open()) {
                String filter = FilterBuilder.equal("uid", ldapWarmupUser).toString();
                try (EntryCursor cursor = connection.search(dn, filter, SearchScope.SUBTREE, "dn")) {
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

    public List<Attribute> search(LdapConnection connection, FilterBuilder filter, String... attributes) throws LdapException, IOException {
        List<Attribute> answer = new ArrayList<>();

        Dn systemDn = getSystemDn();
        try (EntryCursor cursor = connection.search(systemDn, filter.toString(), SearchScope.SUBTREE, attributes)) {
            for (Entry entry : cursor) {
                logger.debugf("Found %s attributes for %s", entry.getAttributes().size(), filter);

                answer.addAll(entry.getAttributes());
            }
        }

        return answer;
    }
}
