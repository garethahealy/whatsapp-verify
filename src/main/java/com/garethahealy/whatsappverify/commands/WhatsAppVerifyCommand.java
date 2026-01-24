package com.garethahealy.whatsappverify.commands;

import io.quarkus.picocli.runtime.annotations.TopCommand;
import picocli.CommandLine;

@TopCommand
@CommandLine.Command(
    name = "whatsapp-verify",
    description = "WhatsApp phone number helper utility",
    mixinStandardHelpOptions = true,
    subcommands = {VerifyCommand.class, CommandLine.HelpCommand.class})
public class WhatsAppVerifyCommand {

}
