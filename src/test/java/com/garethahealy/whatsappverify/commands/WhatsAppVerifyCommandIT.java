package com.garethahealy.whatsappverify.commands;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusMainIntegrationTest
public class WhatsAppVerifyCommandIT {

    @Test
    @Launch("help")
    void test(LaunchResult result) {
        result.echoSystemOut();

        assertEquals(0, result.exitCode());
    }
}
