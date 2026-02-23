package com.garethahealy.whatsappverify.commands;

import com.garethahealy.whatsappverify.config.PhoneNumberUtilConfig;
import com.garethahealy.whatsappverify.model.Member;
import com.garethahealy.whatsappverify.services.LdapGuessService;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import jakarta.inject.Inject;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.jboss.logging.Logger;
import picocli.CommandLine;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(
    name = "verify",
    description = "Verify phone numbers against LDAP",
    mixinStandardHelpOptions = true)
public class VerifyCommand implements Runnable {

    @CommandLine.Option(names = {"-p", "--phone-list"}, description = "List of comma separated phone numbers", required = true)
    String csvPhoneNumbers;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output to write CSV to", defaultValue = "whatsapp.csv")
    String output;

    @CommandLine.Option(names = {"-vpn", "--fail-if-no-vpn"}, description = "Throw an exception if can't connect to LDAP", defaultValue = "true")
    boolean failNoVpn;

    @Inject
    private Logger logger;

    @Inject
    private LdapGuessService ldapGuessService;

    @Inject
    private PhoneNumberUtilConfig phoneNumberUtilConfig;

    @Override
    public void run() {
        if (csvPhoneNumbers == null || csvPhoneNumbers.isBlank()) {
            logger.info("No phone numbers provided, nothing to verify");
            return;
        }

        try {
            Path outputPath = Path.of(output);
            if (!Files.exists(outputPath)) {
                Path parent = outputPath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Files.createFile(outputPath);
            }

            List<Phonenumber.PhoneNumber> phoneNumbers = parsePhoneNumbers(csvPhoneNumbers);
            List<Member> members = collectMembers(phoneNumbers);

            writeCsv(outputPath, members);
        } catch (IOException | LdapException | NumberParseException e) {
            throw new RuntimeException(e);
        }
    }

    private List<Phonenumber.PhoneNumber> parsePhoneNumbers(String csvPhoneNumbers) throws NumberParseException {
        List<Phonenumber.PhoneNumber> answer = new ArrayList<>();

        PhoneNumberUtil phoneUtil = phoneNumberUtilConfig.get();
        List<String> phoneNumbers = List.of(csvPhoneNumbers.split(","));
        List<String> trimmedPhoneNumbers = new ArrayList<>();

        for (String current : phoneNumbers) {
            String trimmed = current.trim();
            if (!trimmed.isEmpty()) {
                trimmedPhoneNumbers.add(trimmed);
            }
        }

        logger.infof("%s phone numbers to verify", trimmedPhoneNumbers.size());

        for (String trimmed : trimmedPhoneNumbers) {
            Phonenumber.PhoneNumber number = phoneUtil.parse(trimmed, null);
            number.setRawInput(trimmed);

            answer.add(number);
        }

        return answer;
    }

    private List<Member> collectMembers(List<Phonenumber.PhoneNumber> phoneNumbers) throws IOException, LdapException {
        List<Member> answer = new ArrayList<>();

        for (Phonenumber.PhoneNumber number : phoneNumbers) {
            Member found = ldapGuessService.attempt(number, failNoVpn);
            if (found == null) {
                found = Member.from(number, null);
            }

            answer.add(found);
        }

        return answer;
    }

    private void writeCsv(Path output, List<Member> members) throws IOException {
        if (members.isEmpty()) {
            logger.info("Members is empty, writing headers only");
        }

        CSVFormat csvFormat = CSVFormat.Builder.create(CSVFormat.DEFAULT)
            .setHeader(Member.Headers.class)
            .get();

        try (Writer writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            try (CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {
                for (Member current : members) {
                    csvPrinter.printRecord(current.toArray());
                }
            }
        }
    }
}
