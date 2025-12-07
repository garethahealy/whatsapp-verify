package com.garethahealy.whatsappverify.config;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PhoneNumberUtilConfigTest {

    @Inject
    PhoneNumberUtilConfig config;

    @Test
    void canResolve() {
        assertNotNull(config.get());
    }
}
