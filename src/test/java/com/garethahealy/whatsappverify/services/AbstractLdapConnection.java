package com.garethahealy.whatsappverify.services;

import com.garethahealy.whatsappverify.factories.LdapConnectionFactory;

abstract class AbstractLdapConnection {

    private final LdapConnectionFactory ldapConnectionFactory;

    public AbstractLdapConnection(LdapConnectionFactory ldapConnectionFactory) {
        this.ldapConnectionFactory = ldapConnectionFactory;
    }

    protected boolean canConnectVpn() {
        return ldapConnectionFactory.canConnect();
    }
}
