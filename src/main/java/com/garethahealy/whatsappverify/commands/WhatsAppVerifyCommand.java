package com.garethahealy.whatsappverify.commands;

import picocli.CommandLine;

@CommandLine.Command(
    name = "whatsapp-verify",
    description = "WhatsApp phone number helper utility",
    subcommands = {CommandLine.HelpCommand.class})
public class WhatsAppVerifyCommand {
}
