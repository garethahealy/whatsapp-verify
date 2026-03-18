package com.garethahealy.whatsappverify.commands;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainIntegrationTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusMainIntegrationTest
class VerifyCommandIT {

    @Test
    @Launch({"verify", "--phone-list=+44 7818 511214, +447725078585", "--output=target/outputs/runMobileAndHome.csv"})
    void runMobileAndHome(LaunchResult result) throws IOException {
        result.echoSystemOut();

        assertEquals(0, result.exitCode());

        Path output = Path.of("target/outputs/runMobileAndHome.csv");
        assertTrue(Files.exists(output));
        assertTrue(result.getOutput().contains("-> gahealy@redhat.com found via searchOnRawMobile"));
        assertTrue(result.getOutput().contains("-> gahealy@redhat.com found via searchOnRawHomePhone"));

        String content = Files.readString(output);
        assertTrue(content.contains("+447818511214,gahealy@redhat.com"));
        assertTrue(content.contains("+447725078585,gahealy@redhat.com"));
    }

    @Test
    @Launch({"verify", "--phone-list=+44 7480 842689", "--output=target/outputs/runLast.csv", "--last-output=test-data/last.csv"})
    void runLast(LaunchResult result) throws IOException {
        result.echoSystemOut();

        assertEquals(0, result.exitCode());

        Path output = Path.of("target/outputs/runLast.csv");
        assertTrue(Files.exists(output));
        assertTrue(result.getOutput().contains("-> mcroft@redhat.com (manual) found via mergeLastWithNow"));

        String content = Files.readString(output);
        assertTrue(content.contains("+447480842689,mcroft@redhat.com (manual)"));
    }
}
