package com.garethahealy.whatsappverify.config;

import com.google.i18n.phonenumbers.PhoneNumberUtil;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class PhoneNumberUtilConfigTest {

    @Inject
    PhoneNumberUtilConfig config;

    @Inject
    PhoneNumberUtil phoneNumberUtil;

    @Test
    void canResolveConfig() {
        assertNotNull(config.get());
    }

    @Test
    void canResolve() {
        assertNotNull(phoneNumberUtil);
    }
}
