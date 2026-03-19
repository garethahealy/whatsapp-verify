package com.garethahealy.whatsappverify.services;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class LdapGuessServiceMockTest {

    @Inject
    PhoneNumberUtil phoneNumberUtil;

    @Inject
    LdapGuessService service;

    @InjectMock
    LdapSearchService ldapSearchService;

    @Test
    void doNotRetryAttempt() {
        when(ldapSearchService.canConnect()).thenReturn(false);

        assertThrows(RuntimeException.class, () -> service.attempt(null, true));

        Collection<Invocation> invocations = Mockito.mockingDetails(ldapSearchService).getInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    void retryAttempt() throws Exception {
        when(ldapSearchService.canConnect()).thenReturn(true);
        when(ldapSearchService.open()).thenReturn(Mockito.mock(LdapNetworkConnection.class));
        when(ldapSearchService.searchOnMobile(any(), any())).thenThrow(new LdapException("Testing retry logic"));

        Phonenumber.PhoneNumber phoneToGuess = phoneNumberUtil.parse("+447725078585", null);

        try {
            service.attempt(phoneToGuess, true);
            fail("Expected LdapException to be thrown");
        } catch (LdapException e) {
            assertEquals("Testing retry logic", e.getMessage());
        }

        Collection<Invocation> invocations = Mockito.mockingDetails(ldapSearchService).getInvocations();
        assertEquals(12, invocations.size());
    }
}
