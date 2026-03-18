package com.garethahealy.whatsappverify.services;

import com.garethahealy.whatsappverify.model.Member;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

import java.io.IOException;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@QuarkusTest
class LdapGuessServiceTest extends AbstractLdapConnection {

    @Inject
    PhoneNumberUtil phoneNumberUtil;

    @Inject
    LdapGuessService service;

    @Inject
    LdapGuessService serviceWithMockedLdapSearch;

    @Test
    @EnabledIf("canConnectVpn")
    void attempt() throws Exception {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");

        Member answer = service.attempt(phoneNumber, true);

        assertNotNull(answer);
        assertEquals("+44 7818 511214", answer.whatsAppNumber().getRawInput());
        assertEquals("gahealy@redhat.com", answer.redHatEmailAddress().get());
    }

    @Test
    @EnabledIf("canConnectVpn")
    void doNotRetryAttempt() {
        LdapSearchService mockedLdapSearchService = Mockito.mock(LdapSearchService.class);
        when(mockedLdapSearchService.canConnect()).thenReturn(false);

        serviceWithMockedLdapSearch.setLdapSearchService(mockedLdapSearchService);

        boolean exceptionThrown = false;
        try {
            serviceWithMockedLdapSearch.attempt(null, true);
        } catch (RuntimeException ex) {
            exceptionThrown = true;
        } catch (IOException | LdapException e) {
            fail();
        }

        assertTrue(exceptionThrown);

        Collection<Invocation> invocations = Mockito.mockingDetails(mockedLdapSearchService).getInvocations();
        assertEquals(1, invocations.size());
    }

    @Test
    @EnabledIf("canConnectVpn")
    void retryAttempt() throws IOException, LdapException {
        LdapSearchService mockedLdapSearchService = Mockito.mock(LdapSearchService.class);
        when(mockedLdapSearchService.canConnect()).thenReturn(true);
        when(mockedLdapSearchService.open()).thenReturn(Mockito.mock(LdapNetworkConnection.class));
        when(mockedLdapSearchService.searchOnMobile(any(), any())).thenThrow(new LdapException("Testing retry logic"));

        serviceWithMockedLdapSearch.setLdapSearchService(mockedLdapSearchService);

        try {
            Phonenumber.PhoneNumber phoneToGuess = phoneNumberUtil.parse("+447725078585", null);
            serviceWithMockedLdapSearch.attempt(phoneToGuess, true);
        } catch (Exception e) {
            assertEquals(LdapException.class, e.getClass());
            assertEquals("Testing retry logic", e.getMessage());
        }

        Collection<Invocation> invocations = Mockito.mockingDetails(mockedLdapSearchService).getInvocations();
        assertEquals(12, invocations.size());
    }
}
