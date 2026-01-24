package com.garethahealy.whatsappverify.services;

import com.garethahealy.whatsappverify.factories.LdapConnectionFactory;
import com.garethahealy.whatsappverify.model.Member;
import com.google.i18n.phonenumbers.Phonenumber;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class DefaultLdapGuessServiceTest extends AbstractLdapConnection {

    @Inject
    DefaultLdapGuessService service;

    @Test
    @EnabledIf("canConnectVpn")
    void attempt() throws Exception {
        Phonenumber.PhoneNumber phoneNumber = new Phonenumber.PhoneNumber().setRawInput("+44 7818 511214");

        Member answer = service.attempt(phoneNumber, true);
        assertNotNull(answer);
        assertEquals("+44 7818 511214", answer.whatsAppNumber().getRawInput());
        assertEquals("gahealy@redhat.com", answer.redHatEmailAddress());
    }
}
