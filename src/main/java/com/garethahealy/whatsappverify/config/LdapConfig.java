package com.garethahealy.whatsappverify.config;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "redhat.ldap")
public interface LdapConfig {

    String connection();

    int port();

    String dn();

    @WithName("warmup-user")
    String warmupUser();
}
