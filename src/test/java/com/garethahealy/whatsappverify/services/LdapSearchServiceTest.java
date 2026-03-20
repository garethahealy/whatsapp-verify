package com.garethahealy.whatsappverify.services;

import com.garethahealy.whatsappverify.factories.LdapConnectionLease;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class LdapSearchServiceTest extends AbstractLdapConnection {

    @Inject
    LdapSearchService service;

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnMobile() throws Exception {
        try (LdapConnectionLease lease = service.open()) {
            Optional<String> result = service.searchOnMobile(lease.connection(), "+44 7818511214");

            assertTrue(result.isPresent());
            assertEquals("gahealy@redhat.com", result.get());
        }
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnHomePhoneBuildsFilterAndReturnsEmail() throws Exception {
        try (LdapConnectionLease lease = service.open()) {
            Optional<String> result = service.searchOnHomePhone(lease.connection(), "+447725078585");

            assertTrue(result.isPresent());
            assertEquals("gahealy@redhat.com", result.get());
        }
    }
}
