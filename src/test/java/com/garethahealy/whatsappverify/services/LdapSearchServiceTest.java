package com.garethahealy.whatsappverify.services;

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
        Optional<String> result = service.searchOnMobile(service.open(), "+44 7818511214");

        assertTrue(result.isPresent());
        assertEquals("gahealy@redhat.com", result.get());
    }

    @Test
    @EnabledIf("canConnectVpn")
    void searchOnHomePhoneBuildsFilterAndReturnsEmail() throws Exception {
        Optional<String> result = service.searchOnHomePhone(service.open(), "+447725078585");

        assertTrue(result.isPresent());
        assertEquals("gahealy@redhat.com", result.get());
    }
}
