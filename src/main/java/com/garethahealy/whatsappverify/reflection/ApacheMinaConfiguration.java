package com.garethahealy.whatsappverify.reflection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.apache.commons.pool2.impl.DefaultEvictionPolicy;
import org.apache.directory.api.ldap.codec.standalone.StandaloneLdapApiService;
import org.apache.mina.transport.socket.nio.NioProcessor;

@RegisterForReflection(targets = {NioProcessor.class})
public class ApacheMinaConfiguration {
}
