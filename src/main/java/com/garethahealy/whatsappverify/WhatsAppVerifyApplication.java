package com.garethahealy.whatsappverify;

import com.garethahealy.whatsappverify.commands.VerifyCommand;
import com.garethahealy.whatsappverify.commands.WhatsAppVerifyCommand;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import jakarta.inject.Inject;
import picocli.CommandLine;

@QuarkusMain
public class WhatsAppVerifyApplication implements QuarkusApplication {

    @Inject
    VerifyCommand verifyCommand;

    public static void main(String[] args) {
        Quarkus.run(WhatsAppVerifyApplication.class, args);
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(new WhatsAppVerifyCommand())
                .addSubcommand(verifyCommand)
                .execute(args);
    }
}
