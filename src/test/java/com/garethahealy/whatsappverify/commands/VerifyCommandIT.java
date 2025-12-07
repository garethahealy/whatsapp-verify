package com.garethahealy.whatsappverify.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VerifyCommandIT extends BaseCommand {

    @Test
    @EnabledIf(value = "isRunnerSet")
    void run() throws IOException, InterruptedException, TimeoutException {
        ProcessExecutor executor = new ProcessExecutor()
                .command(getRunner(), "verify", "--phone-list=+44 7818 511214, +447725078585", "--output=target/whatsapp.csv")
                .redirectError(System.err)
                .redirectOutput(System.out);

        String command = String.join(" ", executor.getCommand());
        System.out.println("Executing \"" + command + "\"");

        ProcessResult result = executor.execute();

        assertEquals(0, result.getExitValue());
    }
}
