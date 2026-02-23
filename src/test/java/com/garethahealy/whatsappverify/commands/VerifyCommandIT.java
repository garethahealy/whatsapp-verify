package com.garethahealy.whatsappverify.commands;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusMainIntegrationTest
class VerifyCommandIT {

    @Test
    @Launch({"verify", "--phone-list=+44 7818 511214, +447725078585", "--output=target/whatsapp.csv"})
    void run(LaunchResult result) {
        result.echoSystemOut();

        assertEquals(0, result.exitCode());
        assertTrue(Files.exists(Path.of("target/whatsapp.csv")));
        assertTrue(result.getOutput().contains("-> gahealy@redhat.com found via searchOnRawMobile"));
        assertTrue(result.getOutput().contains("-> gahealy@redhat.com found via searchOnRawHomePhone"));
    }
}
