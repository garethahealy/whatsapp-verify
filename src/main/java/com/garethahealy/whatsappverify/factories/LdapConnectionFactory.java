package com.garethahealy.whatsappverify.factories;

import com.garethahealy.whatsappverify.config.LdapConfig;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.search.FilterBuilder;
import org.jboss.logging.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@ApplicationScoped
public class LdapConnectionFactory {

    private final Logger logger;
    private final LdapConfig ldapConfig;

    private final AtomicBoolean warmedUp = new AtomicBoolean(false);
    private Dn systemDn;

    public LdapConnectionFactory(Logger logger, LdapConfig ldapConfig) {
        this.logger = logger;
        this.ldapConfig = ldapConfig;
    }

    @PostConstruct
    void init() {
        setupSystemDn();
        ensureWarmedUp();
    }

    private void setupSystemDn() {
        try {
            systemDn = new Dn(ldapConfig.dn());
        } catch (LdapException ex) {
            logger.errorf(ex,"Invalid LDAP DN configuration");
        }
    }

    private void ensureWarmedUp() {
        try {
            Dn dn = getSystemDn();
            try (LdapConnection connection = open()) {
                String filter = FilterBuilder.equal("uid", ldapConfig.warmupUser()).toString();
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

    public Dn getSystemDn() throws LdapException {
        return systemDn;
    }

    public boolean canConnect() {
        if (!warmedUp.get()) {
            ensureWarmedUp();
        }

        return warmedUp.get();
    }

    public LdapConnection open() {
        return new LdapNetworkConnection(ldapConfig.connection());
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
