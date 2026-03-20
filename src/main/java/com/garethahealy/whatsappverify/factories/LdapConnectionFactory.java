package com.garethahealy.whatsappverify.factories;

import com.garethahealy.whatsappverify.config.LdapConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.*;
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
    private LdapConnectionPool connectionPool;

    public LdapConnectionFactory(Logger logger, LdapConfig ldapConfig) {
        this.logger = logger;
        this.ldapConfig = ldapConfig;
    }

    @PostConstruct
    void init() {
        setupSystemDn();
        this.connectionPool = createLdapConnectionPool();
        ensureWarmedUp();
    }

    @PreDestroy
    void destroy() {
        if (connectionPool == null) {
            return;
        }

        try {
            connectionPool.close();
            logger.debug("LDAP connection pool closed");
        } catch (Exception e) {
            logger.warnf(e, "Error closing LDAP connection pool");
        } finally {
            connectionPool = null;
        }
    }

    private void setupSystemDn() {
        try {
            systemDn = new Dn(ldapConfig.dn());
        } catch (LdapException ex) {
            throw new IllegalStateException("Invalid LDAP DN in redhat.ldap.dn: " + ldapConfig.dn(), ex);
        }
    }

    private LdapConnectionPool createLdapConnectionPool() {
        LdapConnectionConfig config = new LdapConnectionConfig();
        config.setLdapHost(ldapConfig.connection());
        config.setLdapPort(ldapConfig.port());

        DefaultLdapConnectionFactory factory = new DefaultLdapConnectionFactory(config);
        GenericObjectPoolConfig<LdapConnection> poolConfig = new GenericObjectPoolConfig<>();
        poolConfig.setMaxTotal(Runtime.getRuntime().availableProcessors());
        poolConfig.setMaxIdle(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

        return new LdapConnectionPool(new DefaultPoolableLdapConnectionFactory(factory), poolConfig);
    }

    private void ensureWarmedUp() {
        try {
            try (LdapConnectionLease lease = open()) {
                LdapConnection connection = lease.connection();

                String filter = FilterBuilder.equal("uid", ldapConfig.warmupUser()).toString();
                try (EntryCursor cursor = connection.search(systemDn, filter, SearchScope.SUBTREE, "dn")) {
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

    public boolean canConnect() {
        if (!warmedUp.get()) {
            ensureWarmedUp();
        }

        return warmedUp.get();
    }

    public LdapConnectionLease open() throws LdapException {
        return new LdapConnectionLease(connectionPool, connectionPool.getConnection());
    }

    public List<Attribute> search(LdapConnection connection, FilterBuilder filter, String... attributes) throws LdapException, IOException {
        List<Attribute> answer = new ArrayList<>();

        try (EntryCursor cursor = connection.search(systemDn, filter.toString(), SearchScope.SUBTREE, attributes)) {
            for (Entry entry : cursor) {
                logger.debugf("Found %s attributes for %s", entry.getAttributes().size(), filter);

                answer.addAll(entry.getAttributes());
            }
        }

        return answer;
    }
}
